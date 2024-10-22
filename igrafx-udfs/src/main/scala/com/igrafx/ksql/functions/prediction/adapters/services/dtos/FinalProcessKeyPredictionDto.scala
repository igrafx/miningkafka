package com.igrafx.ksql.functions.prediction.adapters.services.dtos

import com.igrafx.ksql.functions.prediction.domain.entities.FinalProcessKeyPrediction

protected[services] final case class FinalProcessKeyPredictionDto(
    finalProcessKey: String,
    predictions: Iterable[PredictionDto]
) {
  def toFinalProcessKeyPrediction: FinalProcessKeyPrediction = {
    FinalProcessKeyPrediction(
      finalProcessKey,
      predictions.map(_.toPrediction)
    )
  }
}
