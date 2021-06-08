package com.logpickr.ksql.functions.prediction.domain.entities

final case class FinalProcessKeyPrediction(
    finalProcessKey: String,
    predictions: Iterable[Prediction]
)
