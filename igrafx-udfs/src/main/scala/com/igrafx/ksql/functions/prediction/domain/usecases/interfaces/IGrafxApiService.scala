package com.igrafx.ksql.functions.prediction.domain.usecases.interfaces

import com.igrafx.ksql.functions.prediction.domain.entities.PredictionResponse
import com.igrafx.ksql.functions.prediction.domain.entities.exceptions.{InvalidTokenException, PredictionException}

import java.util.UUID
import scala.concurrent.Future

trait IGrafxApiService {
  @throws[InvalidTokenException]
  def sendAuthentication(
      authUrl: String,
      workGroupId: String,
      workGroupKey: String,
      predictionDescription: String
  ): Future[String]

  @throws[PredictionException]
  def sendPrediction(
      apiUrl: String,
      caseIds: Iterable[String],
      projectId: UUID,
      token: String,
      predictionDescription: String
  ): Future[String]

  @throws[PredictionException]
  def getPredictionResult(
      predictionUrl: String,
      token: String,
      predictionDescription: String
  ): Future[Option[PredictionResponse]]
}
