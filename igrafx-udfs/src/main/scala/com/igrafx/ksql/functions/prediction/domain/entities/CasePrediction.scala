package com.igrafx.ksql.functions.prediction.domain.entities

final case class CasePrediction(
    caseId: String,
    finalProcessKeyPredictions: Iterable[FinalProcessKeyPrediction]
)
