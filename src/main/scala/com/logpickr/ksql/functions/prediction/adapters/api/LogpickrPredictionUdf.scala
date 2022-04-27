package com.logpickr.ksql.functions.prediction.adapters.api

import com.logpickr.ksql.functions.prediction.PredictionConstants
import com.logpickr.ksql.functions.prediction.adapters.api.dtos._
import com.logpickr.ksql.functions.prediction.adapters.services.LogpickrApiServiceImpl
import com.logpickr.ksql.functions.prediction.domain.entities.exceptions.{
  InvalidTokenException,
  NoMoreTryException,
  PredictionException
}
import com.logpickr.ksql.functions.prediction.domain.usecases.PredictionUseCases
import io.confluent.ksql.function.udf.{Udf, UdfDescription, UdfParameter}
import org.apache.kafka.connect.data.Struct
import org.apache.kafka.connect.errors.DataException
import org.slf4j.{Logger, LoggerFactory}

import java.util
import java.util.UUID
import scala.concurrent.Await
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.DurationInt
import scala.jdk.CollectionConverters._
import scala.util.{Failure, Success, Try}

@UdfDescription(
  name = "logpickr_prediction",
  author = "Logpickr",
  description = "Udf allowing to retrieve prediction information on given caseIds belonging to a Logpickr project"
)
class LogpickrPredictionUdf {
  private val log: Logger = LoggerFactory.getLogger(getClass)

  private val predictionUseCases: PredictionUseCases = new PredictionUseCases(new LogpickrApiServiceImpl)

  @Udf(
    description = "Retrieves prediction information for caseIds related to a Logpickr Project",
    schema = PredictionStructs.STRUCT_OUTPUT_SCHEMA_DESCRIPTOR
  )
  def logpickrPrediction(
      @UdfParameter(
        value = "caseIds",
        description = "The caseIds for which we want to predict next events"
      ) caseIds: util.List[String],
      @UdfParameter(
        value = "projectId",
        description = "The id of the Logpickr project containing the information"
      ) projectId: String,
      @UdfParameter(
        value = "workgroupId",
        description = "The id of the Logpickr workgroup related to the project"
      ) workgroupId: String,
      @UdfParameter(
        value = "workgroupKey",
        description = "The key of the Logpickr workgroup related to the project"
      ) workgroupKey: String,
      @UdfParameter(
        value = "apiUrl",
        description = "The URL corresponding to the Logpickr API (for instance : https://dev-api.logpickr.com)"
      ) apiUrl: String,
      @UdfParameter(
        value = "authUrl",
        description =
          "The URL corresponding to the Logpickr Authentication API (for instance : https://dev-auth.logpickr.com)"
      ) authUrl: String,
      @UdfParameter(
        value = "tryNumber",
        description =
          "Number of tries to perform while the prediction in not yet ready, once the number of tries is reached, if the prediction is not ready an error is thrown"
      ) tryNumber: Int
  ): Struct = {
    val projectUuid = Try {
      UUID.fromString(projectId)
    } match {
      case Success(value) => value
      case Failure(exception: IllegalArgumentException) =>
        log.error(
          s"The projectId argument does not correspond to UUID format",
          exception
        )
        throw exception
      case Failure(exception: Throwable) =>
        log.error(
          s"Unexpected exception. Can't retrieve prediction information for the caseIds $caseIds because of their projectId argument"
            .replaceAll("[\r\n]", ""),
          exception
        )
        throw exception
    }
    Try {
      val predictionsFuture =
        predictionUseCases
          .getPrediction(caseIds.asScala, projectUuid, workgroupId, workgroupKey, apiUrl, authUrl, tryNumber)
          .recover {
            case exception: NoMoreTryException => throw exception
            case exception: InvalidTokenException => throw exception
            case exception: PredictionException => throw exception
            case exception: Throwable =>
              log.error("Unexpected Exception while trying to retrieve Predictions from the Logpickr API", exception)
              throw exception
          }
      val predictions =
        Await.result(predictionsFuture, PredictionConstants.timeoutFutureValueInSeconds seconds)
      // Transform the PredictionResponse into a Struct that can be returned by the UDF
      generatePredictionResponseStruct(PredictionResponseDto.fromPredictionResponse(predictions))
    } match {
      case Success(predictions) => predictions
      case Failure(exception: NoMoreTryException) => throw exception
      case Failure(exception: InvalidTokenException) => throw exception
      case Failure(exception: PredictionException) => throw exception
      case Failure(exception: concurrent.TimeoutException) =>
        log.error("Timeout exceeded for the logpickr prediction", exception)
        throw exception
      case Failure(exception: InterruptedException) =>
        log.error(
          "Interrupted while waiting for the end of the external call retrieving prediction",
          exception
        )
        throw exception
      case Failure(exception: DataException) =>
        log.error("Problem with a field while trying to construct the final Struct", exception)
        throw exception
      case Failure(exception: Throwable) =>
        log.error("Unexpected issue with the Logpickr Prediction UDF", exception)
        throw exception
    }
  }

  @throws[DataException]
  private[prediction] def generatePredictionResponseStruct(predictionResponse: PredictionResponseDto): Struct = {
    val result = new Struct(PredictionStructs.PREDICTION_RESPONSE)
    result.put(PredictionConstants.predictionId, predictionResponse.predictionId.toString)
    result.put(
      PredictionConstants.predictionsPredictionResponse,
      generateCasePredictionStruct(predictionResponse.predictions).toList.asJava
    )
    result
  }

  @throws[DataException]
  private[prediction] def generateCasePredictionStruct(
      casePredictions: Iterable[CasePredictionDto]
  ): Iterable[Struct] = {
    casePredictions.map { casePrediction: CasePredictionDto =>
      val result = new Struct(PredictionStructs.CASE_PREDICTION)
      result.put(PredictionConstants.caseId, casePrediction.caseId)
      result.put(
        PredictionConstants.finalProcessKeyPredictions,
        generateFinalProcessKeyPredictionsStruct(casePrediction.finalProcessKeyPredictions).toList.asJava
      )
      result
    }
  }

  @throws[DataException]
  private[prediction] def generateFinalProcessKeyPredictionsStruct(
      finalProcessKeyPredictions: Iterable[FinalProcessKeyPredictionDto]
  ): Iterable[Struct] = {
    finalProcessKeyPredictions.map { finalProcessKeyPrediction: FinalProcessKeyPredictionDto =>
      val result = new Struct(PredictionStructs.FINAL_PROCESS_KEY_PREDICTION)
      result.put(PredictionConstants.finalProcessKey, finalProcessKeyPrediction.finalProcessKey)
      result.put(
        PredictionConstants.predictionsFinalProcessKeyPrediction,
        generatePredictionsStruct(finalProcessKeyPrediction.predictions).toList.asJava
      )
      result
    }
  }

  @throws[DataException]
  private[prediction] def generatePredictionsStruct(
      predictions: Iterable[PredictionDto]
  ): Iterable[Struct] = {
    predictions.map { prediction: PredictionDto =>
      val result = new Struct(PredictionStructs.PREDICTION)
      result.put(PredictionConstants.finalProcessKeyConfidence, prediction.finalProcessKeyConfidence)
      prediction.nextStep match {
        case None => ()
        case Some(nextStep) => result.put(PredictionConstants.nextStep, generatePredictionStepStruct(nextStep))
      }
      prediction.estimatedEndOfCase match {
        case None => ()
        case Some(estimatedEndOfCase) => result.put(PredictionConstants.estimatedEndOfCase, estimatedEndOfCase.toString)
      }
      prediction.endOfCaseConfidenceInterval match {
        case None => ()
        case Some(endOfCaseConfidenceInterval) =>
          result.put(
            PredictionConstants.endOfCaseConfidenceInterval,
            generatePredictionConfidenceIntervalStruct(endOfCaseConfidenceInterval)
          )
      }
      result
    }
  }

  @throws[DataException]
  private[prediction] def generatePredictionStepStruct(predictionStep: PredictionStepDto): Struct = {
    val result = new Struct(PredictionStructs.PREDICTION_STEP)
    result.put(PredictionConstants.name, predictionStep.name)
    result.put(PredictionConstants.start, predictionStep.start.toString)
    predictionStep.startConfidenceInterval match {
      case None => ()
      case Some(startConfidenceInterval) =>
        result.put(
          PredictionConstants.startConfidenceInterval,
          generatePredictionConfidenceIntervalStruct(startConfidenceInterval)
        )
    }
    result.put(PredictionConstants.end, predictionStep.end.toString)
    predictionStep.endConfidenceInterval match {
      case None => ()
      case Some(endConfidenceInterval) =>
        result.put(
          PredictionConstants.endConfidenceInterval,
          generatePredictionConfidenceIntervalStruct(endConfidenceInterval)
        )
    }
    result
  }

  @throws[DataException]
  private[prediction] def generatePredictionConfidenceIntervalStruct(
      predictionConfidenceInterval: PredictionConfidenceIntervalDto
  ): Struct = {
    val result = new Struct(PredictionStructs.PREDICTION_CONFIDENCE_INTERVAL)
    result.put(PredictionConstants.startInterval, predictionConfidenceInterval.startInterval.toString)
    result.put(PredictionConstants.endInterval, predictionConfidenceInterval.endInterval.toString)
    result.put(PredictionConstants.intervalProbability, predictionConfidenceInterval.intervalProbability)
    result
  }
}
