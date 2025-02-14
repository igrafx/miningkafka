package com.igrafx.ksql.functions.prediction.adapters.services.dtos

import com.igrafx.ksql.functions.prediction.domain.entities.CasePrediction

protected[services] final case class CasePredictionDto(
    caseId: String,
    finalProcessKeyPredictions: Iterable[FinalProcessKeyPredictionDto]
) {
  def toCasePrediction: CasePrediction = {
    CasePrediction(
      caseId,
      finalProcessKeyPredictions.map(_.toFinalProcessKeyPrediction)
    )
  }
}
