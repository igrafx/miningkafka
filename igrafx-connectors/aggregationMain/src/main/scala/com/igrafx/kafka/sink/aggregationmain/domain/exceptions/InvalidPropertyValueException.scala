package com.igrafx.kafka.sink.aggregationmain.domain.exceptions

import com.igrafx.core.exceptions.SafeException

final case class InvalidPropertyValueException(message: String) extends SafeException(message)
