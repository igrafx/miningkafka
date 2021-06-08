package com.logpickr.ksql.functions.prediction.domain.entities

import org.joda.time.DateTime

final case class Prediction(
    finalProcessKeyConfidence: Double,
    nextStep: Option[PredictionStep],
    estimatedEndOfCase: Option[DateTime],
    endOfCaseConfidenceInterval: Option[PredictionConfidenceInterval]
)
