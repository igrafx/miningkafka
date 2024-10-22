package com.igrafx.kafka.sink.aggregationmain.domain.entities.mocks

import com.igrafx.core.Mock
import com.igrafx.kafka.sink.aggregationmain.domain.entities.KafkaEventPayloadIssuePushFile

import java.nio.file.{Path, Paths}

final class KafkaEventPayloadIssuePushFileMock extends Mock[KafkaEventPayloadIssuePushFile] {
  private var filePath: Path = Paths.get("filename")
  private var date: Long = System.currentTimeMillis()
  private var exceptionType: String = new Exception().getClass.getCanonicalName

  def setFilePath(filePath: Path): KafkaEventPayloadIssuePushFileMock = {
    this.filePath = filePath
    this
  }

  def setDate(date: Long): KafkaEventPayloadIssuePushFileMock = {
    this.date = date
    this
  }

  def setExceptionType(exceptionType: String): KafkaEventPayloadIssuePushFileMock = {
    this.exceptionType = exceptionType
    this
  }

  override def build(): KafkaEventPayloadIssuePushFile = {
    KafkaEventPayloadIssuePushFile(
      filePath = filePath,
      date = date,
      exceptionType = exceptionType
    )
  }
}
