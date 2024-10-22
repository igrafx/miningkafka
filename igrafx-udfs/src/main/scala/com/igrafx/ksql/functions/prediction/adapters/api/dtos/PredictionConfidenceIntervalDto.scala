package com.igrafx.ksql.functions.prediction.adapters.api.dtos

import com.igrafx.ksql.functions.prediction.domain.entities.PredictionConfidenceInterval
import org.joda.time.DateTime

protected[api] final case class PredictionConfidenceIntervalDto(
    startInterval: DateTime,
    endInterval: DateTime,
    intervalProbability: Double
)

protected[api] object PredictionConfidenceIntervalDto {
  def fromPredictionConfidenceInterval(
      predictionConfidenceInterval: PredictionConfidenceInterval
  ): PredictionConfidenceIntervalDto = {
    PredictionConfidenceIntervalDto(
      predictionConfidenceInterval.startInterval,
      predictionConfidenceInterval.endInterval,
      predictionConfidenceInterval.intervalProbability
    )
  }
}
