package com.igrafx.ksql.functions.prediction.domain.entities.exceptions

import com.igrafx.core.exceptions.SafeException

final case class InvalidTokenException(message: String) extends SafeException(message)
