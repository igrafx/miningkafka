package com.logpickr.ksql.functions.prediction.adapters.services.dtos

import com.logpickr.ksql.functions.prediction.domain.entities.PredictionStep
import org.joda.time.DateTime

protected[services] final case class PredictionStepDto(
    name: String,
    start: DateTime,
    startConfidenceInterval: Option[PredictionConfidenceIntervalDto],
    end: DateTime,
    endConfidenceInterval: Option[PredictionConfidenceIntervalDto]
) {
  def toPredictionStep: PredictionStep = {
    PredictionStep(
      name,
      start,
      startConfidenceInterval.map(_.toPredictionConfidenceInterval),
      end,
      endConfidenceInterval.map(_.toPredictionConfidenceInterval)
    )
  }
}
