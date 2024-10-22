package com.igrafx.kafka.sink.aggregation.domain.exceptions

import com.igrafx.kafka.sink.aggregation.domain.entities.PartitionTracker

final case class AggregationException(cause: Throwable, partitionTrackerMap: Map[String, PartitionTracker])
    extends Exception(cause)
