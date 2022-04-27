package com.logpickr.ksql.functions.prediction.domain.entities.exceptions

import com.logpickr.core.exceptions.SafeException

final case class InvalidTokenException(message: String) extends SafeException(message)
