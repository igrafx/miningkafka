package com.igrafx.ksql.functions.prediction.adapters.services.dtos

import com.igrafx.ksql.functions.prediction.domain.entities.PredictionResponse

import java.util.UUID

protected[services] final case class PredictionResponseDto(
    predictionId: UUID,
    predictions: Iterable[CasePredictionDto]
) {
  def toPredictionResponse: PredictionResponse = {
    PredictionResponse(predictionId, predictions.map(_.toCasePrediction))
  }
}
