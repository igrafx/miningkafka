package com.igrafx.kafka.sink.aggregationmain.domain.exceptions

final case class ProjectPathInitializationException(cause: Throwable) extends Exception(cause)
