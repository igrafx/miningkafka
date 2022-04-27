package com.logpickr.ksql.functions.prediction.adapters.api.dtos

import com.logpickr.ksql.functions.prediction.domain.entities.PredictionResponse

import java.util.UUID

protected[api] final case class PredictionResponseDto(
    predictionId: UUID,
    predictions: Iterable[CasePredictionDto]
)

protected[api] object PredictionResponseDto {
  def fromPredictionResponse(predictionResponse: PredictionResponse): PredictionResponseDto = {
    PredictionResponseDto(
      predictionResponse.predictionId,
      predictionResponse.predictions.map(casePrediction => CasePredictionDto.fromCasePrediction(casePrediction))
    )
  }
}
