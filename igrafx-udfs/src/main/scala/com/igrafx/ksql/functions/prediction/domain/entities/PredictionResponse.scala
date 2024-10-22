package com.igrafx.ksql.functions.prediction.domain.entities

import java.util.UUID

final case class PredictionResponse(
    predictionId: UUID,
    predictions: Iterable[CasePrediction]
)
