package com.igrafx.kafka.sink.aggregationmain.domain.exceptions

import com.igrafx.core.exceptions.SafeException

final case class ColumnMappingCreationException(message: String, canRetry: Boolean)
    extends SafeException(message)
    with RetryException
