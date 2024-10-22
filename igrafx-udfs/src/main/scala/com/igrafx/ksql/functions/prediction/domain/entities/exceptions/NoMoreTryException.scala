package com.igrafx.ksql.functions.prediction.domain.entities.exceptions

import com.igrafx.core.exceptions.SafeException

final case class NoMoreTryException(message: String) extends SafeException(message)
