package com.logpickr.ksql.functions.prediction.domain.entities.exceptions

final case class NoMoreTryException(message: String) extends Exception(message)
