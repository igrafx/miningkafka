package com.igrafx.kafka.sink.aggregationmain.domain.exceptions

import com.igrafx.kafka.sink.aggregationmain.domain.entities.PartitionTracker

final case class AggregationException(cause: Throwable, partitionTrackerMap: Map[String, PartitionTracker])
    extends Exception(cause)
