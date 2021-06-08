package com.logpickr.ksql.functions.prediction.domain.entities.exceptions

final case class PredictionException(message: String) extends Exception(message)
