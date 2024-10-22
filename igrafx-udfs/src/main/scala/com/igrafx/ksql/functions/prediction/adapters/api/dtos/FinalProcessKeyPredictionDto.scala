package com.igrafx.ksql.functions.prediction.adapters.api.dtos

import com.igrafx.ksql.functions.prediction.domain.entities.FinalProcessKeyPrediction

protected[api] final case class FinalProcessKeyPredictionDto(
    finalProcessKey: String,
    predictions: Iterable[PredictionDto]
)

protected[api] object FinalProcessKeyPredictionDto {
  def fromFinalProcessKeyPrediction(
      finalProcessKeyPrediction: FinalProcessKeyPrediction
  ): FinalProcessKeyPredictionDto = {
    FinalProcessKeyPredictionDto(
      finalProcessKeyPrediction.finalProcessKey,
      finalProcessKeyPrediction.predictions.map(prediction => PredictionDto.fromPrediction(prediction))
    )
  }
}
