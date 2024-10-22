package com.igrafx.kafka.sink.aggregationmain.domain.exceptions

import com.igrafx.core.exceptions.SafeException

abstract class IndexException(message: String) extends SafeException(message)

final case class DefaultIndexException(message: String) extends IndexException(message)
final case class NegativeIndexException(message: String) extends IndexException(message)
final case class IncoherentIndexException(message: String) extends IndexException(message)
final case class ParsableIndexException(message: String) extends IndexException(message)
