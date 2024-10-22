package com.igrafx.kafka.sink.aggregationmain.domain.exceptions

trait RetryException {
  val canRetry: Boolean
}
