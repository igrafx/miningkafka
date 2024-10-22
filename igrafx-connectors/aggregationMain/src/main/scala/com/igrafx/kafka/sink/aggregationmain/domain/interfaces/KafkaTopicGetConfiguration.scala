package com.igrafx.kafka.sink.aggregationmain.domain.interfaces

import org.apache.kafka.common.KafkaException
import org.slf4j.Logger

trait KafkaTopicGetConfiguration {
  @throws[NumberFormatException]
  @throws[NoSuchElementException]
  @throws[KafkaException]
  def getTopicRetentionMs(topic: String, bootstrapServers: String, log: Logger): Long
}
