package com.igrafx.kafka.sink.aggregationmain.domain.exceptions

final case class OldArchivedFilesDeletionException(cause: Throwable) extends Exception(cause)
