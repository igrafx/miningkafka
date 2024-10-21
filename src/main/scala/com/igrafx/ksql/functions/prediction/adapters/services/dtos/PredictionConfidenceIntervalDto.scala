package com.igrafx.ksql.functions.prediction.adapters.services.dtos

import com.igrafx.ksql.functions.prediction.domain.entities.PredictionConfidenceInterval
import org.joda.time.DateTime

protected[services] final case class PredictionConfidenceIntervalDto(
    startInterval: DateTime,
    endInterval: DateTime,
    intervalProbability: Double
) {
  def toPredictionConfidenceInterval: PredictionConfidenceInterval = {
    PredictionConfidenceInterval(
      startInterval,
      endInterval,
      intervalProbability
    )
  }
}
