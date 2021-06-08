package com.logpickr.ksql.functions.prediction.adapters.api.dtos

import com.logpickr.ksql.functions.prediction.domain.entities.PredictionStep
import org.joda.time.DateTime

protected[api] final case class PredictionStepDto(
    name: String,
    start: DateTime,
    startConfidenceInterval: Option[PredictionConfidenceIntervalDto],
    end: DateTime,
    endConfidenceInterval: Option[PredictionConfidenceIntervalDto]
)

protected[api] object PredictionStepDto {
  def fromPredictionStep(predictionStep: PredictionStep): PredictionStepDto = {
    PredictionStepDto(
      predictionStep.name,
      predictionStep.start,
      predictionStep.startConfidenceInterval.map(predictionConfidenceInterval =>
        PredictionConfidenceIntervalDto.fromPredictionConfidenceInterval(predictionConfidenceInterval)
      ),
      predictionStep.end,
      predictionStep.endConfidenceInterval.map(predictionConfidenceInterval =>
        PredictionConfidenceIntervalDto.fromPredictionConfidenceInterval(predictionConfidenceInterval)
      )
    )
  }
}
