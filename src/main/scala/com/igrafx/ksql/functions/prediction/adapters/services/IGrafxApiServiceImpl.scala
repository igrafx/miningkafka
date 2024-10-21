package com.igrafx.ksql.functions.prediction.adapters.services

import com.igrafx.ksql.functions.prediction.PredictionConstants
import com.igrafx.ksql.functions.prediction.adapters.services.dtos.PredictionResponseDto
import com.igrafx.ksql.functions.prediction.domain.entities.PredictionResponse
import com.igrafx.ksql.functions.prediction.domain.entities.exceptions.{InvalidTokenException, PredictionException}
import com.igrafx.ksql.functions.prediction.domain.usecases.interfaces.IGrafxApiService
import com.igrafx.utils.Json4sUtils
import org.json4s.Formats
import org.json4s.native.JsonMethods.parse
import org.slf4j.{Logger, LoggerFactory}
import scalaj.http.{Http, HttpOptions}

import java.nio.charset.StandardCharsets
import java.util.UUID
import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global

class IGrafxApiServiceImpl extends IGrafxApiService {
  private val log: Logger = LoggerFactory.getLogger(getClass)
  implicit private val formats: Formats = Json4sUtils.defaultFormats

  @throws[InvalidTokenException]
  def sendAuthentication(
      authUrl: String,
      workGroupId: String,
      workGroupKey: String,
      predictionDescription: String
  ): Future[String] = {
    val form: Seq[(String, String)] = Seq(
      ("grant_type", "client_credentials"),
      ("client_id", workGroupId),
      ("client_secret", workGroupKey)
    )

    Future {
      Http(s"$authUrl/realms/igrafxdev/protocol/openid-connect/token")
        .postForm(form)
        .header("Content-Type", "application/json")
        .header("Charset", StandardCharsets.UTF_8.name())
        .option(HttpOptions.readTimeout(PredictionConstants.timeoutApiCallValueInMilliSeconds))
        .asString
    }.map { resultLogin =>
      resultLogin.code match {
        case 200 =>
          val token = (parse(resultLogin.body) \\ "access_token").extract[String]
          log.debug(
            s"Token for the $predictionDescription has been successfully retrieved : $token".replaceAll("[\r\n]", "")
          )
          token
        case _ =>
          throw InvalidTokenException(
            s"Couldn't retrieve a token for this Http Request Authentication : ${resultLogin.code}, ${resultLogin.body}, can't retrieve the $predictionDescription"
          )
      }
    }.recover {
      case tokenException: InvalidTokenException =>
        log.error(
          s"InvalidTokenException : Issue with the Http Request for authentication, can't retrieve the $predictionDescription"
            .replaceAll("[\r\n]", ""),
          tokenException
        )
        throw tokenException
      case exception =>
        log.error(
          s"Unexpected exception while trying to retrieve a token via the iGrafx authentication API, hence can't retrieve either the $predictionDescription"
            .replaceAll("[\r\n]", ""),
          exception
        )
        throw InvalidTokenException(exception.getMessage)
    }
  }

  @throws[PredictionException]
  def sendPrediction(
      apiUrl: String,
      caseIds: Iterable[String],
      projectId: UUID,
      token: String,
      predictionDescription: String
  ): Future[String] = {
    Future {
      Http(s"$apiUrl/pub/project/$projectId/prediction?${setCaseIdsForQuery(caseIds)}")
        .header(PredictionConstants.tokenHeader, s"Bearer $token")
        .header("content-type", "application/json")
        .postForm
        .option(HttpOptions.readTimeout(PredictionConstants.timeoutApiCallValueInMilliSeconds))
        .asString
    }.map { resultSendPrediction =>
      resultSendPrediction.code match {
        case 202 =>
          log.debug(s"The $predictionDescription has been performed successfully !".replaceAll("[\r\n]", ""))
          (parse(resultSendPrediction.body) \\ "message").extract[String]
        case 404 =>
          throw PredictionException(
            s"Project of metrics not found while trying to launch the $predictionDescription, Http Response : ${resultSendPrediction.code}, ${resultSendPrediction.body}"
          )
        case 424 =>
          throw PredictionException(
            s"Failure in prediction launch for the $predictionDescription, Http Response : ${resultSendPrediction.code}, ${resultSendPrediction.body}"
          )
        case _ =>
          throw PredictionException(
            s"Problem with Http Request, can't launch the $predictionDescription : ${resultSendPrediction.code}, ${resultSendPrediction.body}"
          )
      }
    }.recover {
      case predictionException: PredictionException =>
        log.error(
          s"Issue with Http Request launching a $predictionDescription".replaceAll("[\r\n]", ""),
          predictionException
        )
        throw predictionException
      case exception =>
        log.error(
          s"Unexpected exception with the Http Request launching a $predictionDescription".replaceAll("[\r\n]", ""),
          exception
        )
        throw PredictionException(exception.getMessage)
    }
  }

  private[services] def setCaseIdsForQuery(caseIds: Iterable[String]): String = {
    caseIds.foldLeft("") { (acc: String, caseId: String) =>
      if (acc.nonEmpty) {
        s"$acc,$caseId"
      } else {
        s"caseIds[]=$caseId"
      }
    }
  }

  @throws[PredictionException]
  def getPredictionResult(
      predictionUrl: String,
      token: String,
      predictionDescription: String
  ): Future[Option[PredictionResponse]] = {
    Future {
      Http(predictionUrl)
        .header(PredictionConstants.tokenHeader, s"Bearer $token")
        .header("content-type", "application/json")
        .option(HttpOptions.readTimeout(PredictionConstants.timeoutApiCallValueInMilliSeconds))
        .asString
    }.map { resultPrediction =>
      resultPrediction.code match {
        case 200 =>
          log.info(s"The $predictionDescription has been successfully retrieved !".replaceAll("[\r\n]", ""))
          Some(parse(resultPrediction.body).extract[PredictionResponseDto].toPredictionResponse)
        case 202 =>
          log.debug(
            s"$predictionDescription not ready, Http Response : ${resultPrediction.code}, ${resultPrediction.body}"
              .replaceAll("[\r\n]", "")
          )
          None
        case 404 =>
          throw PredictionException(
            s"No existing $predictionDescription, Http Response : ${resultPrediction.code}, ${resultPrediction.body}"
          )
        case _ =>
          throw PredictionException(
            s"Problem with Http Request for the $predictionDescription : ${resultPrediction.code}, ${resultPrediction.body}"
          )
      }
    }.recover {
      case exception: PredictionException =>
        log.error(
          s"Issue while retrieving from the iGrafx API a $predictionDescription".replaceAll("[\r\n]", ""),
          exception
        )
        throw exception
      case exception =>
        log.error(
          s"Unexpected exception with the Http Request retrieving prediction information from the iGrafx API for the $predictionDescription"
            .replaceAll("[\r\n]", ""),
          exception
        )
        throw PredictionException(exception.getMessage)
    }
  }
}
