package com.igrafx.kafka.sink.aggregationmain.domain.exceptions

import com.igrafx.core.exceptions.SafeException

final case class InvalidColumnMappingException(message: String) extends SafeException(message)
