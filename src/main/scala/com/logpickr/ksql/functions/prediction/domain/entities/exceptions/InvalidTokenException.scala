package com.logpickr.ksql.functions.prediction.domain.entities.exceptions

final case class InvalidTokenException(message: String) extends Exception(message)
