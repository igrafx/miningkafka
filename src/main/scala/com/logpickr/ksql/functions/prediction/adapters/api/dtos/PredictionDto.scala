package com.logpickr.ksql.functions.prediction.adapters.api.dtos

import com.logpickr.ksql.functions.prediction.domain.entities.Prediction
import org.joda.time.DateTime

protected[api] final case class PredictionDto(
    finalProcessKeyConfidence: Double,
    nextStep: Option[PredictionStepDto],
    estimatedEndOfCase: Option[DateTime],
    endOfCaseConfidenceInterval: Option[PredictionConfidenceIntervalDto]
)

protected[api] object PredictionDto {
  def fromPrediction(prediction: Prediction): PredictionDto = {
    PredictionDto(
      prediction.finalProcessKeyConfidence,
      prediction.nextStep.map(predictionStep => PredictionStepDto.fromPredictionStep(predictionStep)),
      prediction.estimatedEndOfCase,
      prediction.endOfCaseConfidenceInterval.map(predictionConfidenceInterval =>
        PredictionConfidenceIntervalDto.fromPredictionConfidenceInterval(predictionConfidenceInterval)
      )
    )
  }
}
