package com.igrafx.kafka.sink.aggregationmain.domain.exceptions

final case class FileCreationException(cause: Throwable) extends Exception(cause) with LogEventException
