package com.igrafx.kafka.sink.aggregation.domain.exceptions

final case class SendRecordException(cause: Throwable) extends Exception(cause)
