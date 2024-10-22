package com.igrafx.kafka.sink.aggregation.domain.entities

import org.apache.kafka.clients.consumer.OffsetAndMetadata
import org.apache.kafka.common.TopicPartition

final case class CommitOffsetMaps(
    updatedOffsetsMap: Map[TopicPartition, OffsetAndMetadata],
    partitionTrackerMap: Map[String, PartitionTracker]
)
