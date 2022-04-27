package com.logpickr.ksql.functions.prediction.domain.usecases

import com.logpickr.ksql.functions.prediction.PredictionConstants
import com.logpickr.ksql.functions.prediction.domain.entities.PredictionResponse
import com.logpickr.ksql.functions.prediction.domain.entities.exceptions.{
  InvalidTokenException,
  NoMoreTryException,
  PredictionException
}
import com.logpickr.ksql.functions.prediction.domain.usecases.interfaces.LogpickrApiService
import com.logpickr.utils.Json4sUtils
import org.json4s.Formats
import org.slf4j.{Logger, LoggerFactory}

import java.lang.Thread.sleep
import java.util.UUID
import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global

class PredictionUseCases(private val logpickrApiService: LogpickrApiService) {
  private val log: Logger = LoggerFactory.getLogger(getClass)
  implicit private val formats: Formats = Json4sUtils.defaultFormats

  @throws[NoMoreTryException]
  @throws[InvalidTokenException]
  @throws[PredictionException]
  def getPrediction(
      caseIds: Iterable[String],
      projectId: UUID,
      workgroupId: String,
      workgroupKey: String,
      apiUrl: String,
      authUrl: String,
      tryNumber: Int
  ): Future[PredictionResponse] = {
    val predictionDescription =
      s"prediction on the $projectId Logpickr Project for the following Case Ids : $caseIds"
    (for {
      token <- logpickrApiService.sendAuthentication(authUrl, workgroupId, workgroupKey, predictionDescription)
      predictionUrl <- logpickrApiService.sendPrediction(apiUrl, caseIds, projectId, token, predictionDescription)
      predictions <- pullPredictionResult(
        authUrl,
        predictionUrl,
        workgroupId,
        workgroupKey,
        tryNumber,
        predictionDescription
      )
    } yield {
      predictions
    }).recover {
      case exception: NoMoreTryException =>
        throw exception
      case exception: InvalidTokenException =>
        throw exception
      case exception: PredictionException =>
        throw exception
      case exception: Throwable =>
        log.error(
          s"Unexpected exception while trying to send or retrieve a $predictionDescription".replaceAll("[\r\n]", ""),
          exception
        )
        throw exception
    }
  }

  @throws[NoMoreTryException]
  @throws[InvalidTokenException]
  @throws[PredictionException]
  private def pullPredictionResult(
      authUrl: String,
      predictionUrl: String,
      workgroupId: String,
      workgroupKey: String,
      tryNumber: Int,
      predictionDescription: String
  ): Future[PredictionResponse] = {
    if (tryNumber <= 0) {
      log.error(
        s"No more try, all prediction receptions have failed for the $predictionDescription".replaceAll("[\r\n]", "")
      )
      throw NoMoreTryException(s"No more try to retrieve the $predictionDescription")
    }
    sleep(PredictionConstants.tryIntervalInMilliseconds)
    (for {
      token <- logpickrApiService.sendAuthentication(authUrl, workgroupId, workgroupKey, predictionDescription)
      predictionsOpt <- logpickrApiService.getPredictionResult(predictionUrl, token, predictionDescription)
      prediction <- predictionsOpt match {
        case Some(predictions) => Future.successful(predictions)
        case None =>
          log.debug(
            s"Failed to retrieve the $predictionDescription, ${tryNumber - 1} tries left".replaceAll("[\r\n]", "")
          )
          pullPredictionResult(
            authUrl,
            predictionUrl,
            workgroupId,
            workgroupKey,
            tryNumber - 1,
            predictionDescription
          )
      }
    } yield prediction).recover {
      case exception: NoMoreTryException =>
        throw exception
      case exception: InvalidTokenException =>
        throw exception
      case exception: PredictionException =>
        throw exception
      case exception: Throwable =>
        log.error(
          s"Unexpected exception while trying to retrieve a prediction for the $predictionDescription"
            .replaceAll("[\r\n]", ""),
          exception
        )
        throw exception
    }
  }
}
