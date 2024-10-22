package com.igrafx.kafka.sink.aggregation.domain.exceptions

import com.igrafx.core.exceptions.SafeException

final case class MaxMessageBytesException(message: String) extends SafeException(message)
