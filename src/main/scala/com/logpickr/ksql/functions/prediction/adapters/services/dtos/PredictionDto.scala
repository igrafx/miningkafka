package com.logpickr.ksql.functions.prediction.adapters.services.dtos

import com.logpickr.ksql.functions.prediction.domain.entities.Prediction
import org.joda.time.DateTime

protected[services] final case class PredictionDto(
    finalProcessKeyConfidence: Double,
    nextStep: Option[PredictionStepDto],
    estimatedEndOfCase: Option[DateTime],
    endOfCaseConfidenceInterval: Option[PredictionConfidenceIntervalDto]
) {
  def toPrediction: Prediction = {
    Prediction(
      finalProcessKeyConfidence,
      nextStep.map(_.toPredictionStep),
      estimatedEndOfCase,
      endOfCaseConfidenceInterval.map(_.toPredictionConfidenceInterval)
    )
  }
}
