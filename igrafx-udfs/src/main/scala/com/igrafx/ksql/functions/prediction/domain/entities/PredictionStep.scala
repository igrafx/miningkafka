package com.igrafx.ksql.functions.prediction.domain.entities

import org.joda.time.DateTime

final case class PredictionStep(
    name: String,
    start: DateTime,
    startConfidenceInterval: Option[PredictionConfidenceInterval],
    end: DateTime,
    endConfidenceInterval: Option[PredictionConfidenceInterval]
)
