package com.igrafx.kafka.sink.aggregationmain.domain.entities

import java.nio.file.Path

sealed trait KafkaEventPayload {
  def filePath: Path
  def date: Long
}

final case class KafkaEventPayloadPushFile(filePath: Path, date: Long, lineNumber: Int) extends KafkaEventPayload

final case class KafkaEventPayloadIssuePushFile(
    filePath: Path,
    date: Long,
    exceptionType: String
) extends KafkaEventPayload
