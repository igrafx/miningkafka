package com.igrafx.kafka.sink.aggregationmain.domain.entities.mocks

import com.igrafx.core.Mock
import com.igrafx.kafka.sink.aggregationmain.domain.entities.KafkaEventPayloadPushFile

import java.nio.file.{Path, Paths}

final class KafkaEventPayloadPushFileMock extends Mock[KafkaEventPayloadPushFile] {
  private var filePath: Path = Paths.get("filename")
  private var date: Long = System.currentTimeMillis()
  private var lineNumber: Int = 100

  def setFilePath(filePath: Path): KafkaEventPayloadPushFileMock = {
    this.filePath = filePath
    this
  }

  def setDate(date: Long): KafkaEventPayloadPushFileMock = {
    this.date = date
    this
  }

  def setLineNumber(lineNumber: Int): KafkaEventPayloadPushFileMock = {
    this.lineNumber = lineNumber
    this
  }

  override def build(): KafkaEventPayloadPushFile = {
    KafkaEventPayloadPushFile(
      filePath = filePath,
      date = date,
      lineNumber = lineNumber
    )
  }
}
