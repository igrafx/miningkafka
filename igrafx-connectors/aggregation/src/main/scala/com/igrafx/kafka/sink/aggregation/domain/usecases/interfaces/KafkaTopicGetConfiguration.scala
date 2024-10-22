package com.igrafx.kafka.sink.aggregation.domain.usecases.interfaces

import org.apache.kafka.common.KafkaException
import org.slf4j.Logger

trait KafkaTopicGetConfiguration {
  @throws[NumberFormatException]
  @throws[NoSuchElementException]
  @throws[KafkaException]
  def getTopicMaxMessageBytes(topic: String, bootstrapServers: String, log: Logger): Int

  @throws[NumberFormatException]
  @throws[NoSuchElementException]
  @throws[KafkaException]
  def getTopicRetentionMs(topic: String, bootstrapServers: String, log: Logger): Long
}
