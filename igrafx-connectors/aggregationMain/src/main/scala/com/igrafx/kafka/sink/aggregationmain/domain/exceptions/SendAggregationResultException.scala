package com.igrafx.kafka.sink.aggregationmain.domain.exceptions

final case class SendAggregationResultException(cause: Throwable) extends Exception(cause)
