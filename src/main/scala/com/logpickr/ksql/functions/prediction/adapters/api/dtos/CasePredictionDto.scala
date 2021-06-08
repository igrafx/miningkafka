package com.logpickr.ksql.functions.prediction.adapters.api.dtos

import com.logpickr.ksql.functions.prediction.domain.entities.CasePrediction

protected[api] final case class CasePredictionDto(
    caseId: String,
    finalProcessKeyPredictions: Iterable[FinalProcessKeyPredictionDto]
)

protected[api] object CasePredictionDto {
  def fromCasePrediction(casePrediction: CasePrediction): CasePredictionDto = {
    CasePredictionDto(
      casePrediction.caseId,
      casePrediction.finalProcessKeyPredictions.map(finalProcessKeyPrediction =>
        FinalProcessKeyPredictionDto.fromFinalProcessKeyPrediction(finalProcessKeyPrediction)
      )
    )
  }
}
