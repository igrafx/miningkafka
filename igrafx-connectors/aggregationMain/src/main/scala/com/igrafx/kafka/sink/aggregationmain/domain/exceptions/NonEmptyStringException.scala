package com.igrafx.kafka.sink.aggregationmain.domain.exceptions

import com.igrafx.core.exceptions.SafeException

abstract class NonEmptyStringException(message: String) extends SafeException(message)

final case class IncoherentNonEmptyStringException(message: String) extends NonEmptyStringException(message)
final case class DefaultNonEmptyStringException(message: String) extends NonEmptyStringException(message)
