package com.logpickr.ksql.functions.prediction.domain.entities

import org.joda.time.DateTime

final case class PredictionConfidenceInterval(
    startInterval: DateTime,
    endInterval: DateTime,
    intervalProbability: Double
)
