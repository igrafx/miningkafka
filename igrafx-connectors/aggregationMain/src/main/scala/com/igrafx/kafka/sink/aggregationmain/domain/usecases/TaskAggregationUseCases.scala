package com.igrafx.kafka.sink.aggregationmain.domain.usecases

import com.igrafx.kafka.sink.aggregationmain.domain.entities.{
  CommitOffsetMaps,
  CsvProperties,
  Event,
  KafkaLoggingEventsProperties,
  PartitionTracker,
  Properties
}
import org.apache.kafka.clients.consumer.OffsetAndMetadata
import org.apache.kafka.common.TopicPartition
import org.apache.kafka.connect.sink.SinkRecord

import java.util

trait TaskAggregationUseCases {
  def getTopicRetentionMs(topic: String, bootstrapServers: String): Long

  def getNameIndex(topic: String, partition: Int): String

  def aggregateAndSendCollection(
      collection: util.Collection[SinkRecord],
      properties: Properties,
      csvProperties: CsvProperties,
      kafkaLoggingEventsPropertiesOpt: Option[KafkaLoggingEventsProperties],
      partitionTrackerMap: Map[String, PartitionTracker]
  ): Map[String, PartitionTracker]

  def checkPartitionsForTimeoutOrRetention(
      properties: Properties,
      csvProperties: CsvProperties,
      kafkaLoggingEventsPropertiesOpt: Option[KafkaLoggingEventsProperties],
      partitionTrackerMap: Map[String, PartitionTracker]
  ): Map[String, PartitionTracker]

  def commitOffsets(
      currentOffsets: util.Map[TopicPartition, OffsetAndMetadata],
      partitionTrackerMap: Map[String, PartitionTracker]
  ): CommitOffsetMaps

  def getParams(record: SinkRecord): Event
}

object TaskAggregationUseCases {
  lazy val instance: TaskAggregationUseCases = new TaskAggregationUseCasesImpl
}
