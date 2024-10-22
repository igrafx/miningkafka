package com.igrafx.kafka.sink.aggregationmain.domain.entities

final case class PartitionTracker(
    nameIndex: String,
    processedOffset: Long,
    flushedOffset: Long,
    committedOffset: Long,
    pendingValue: Seq[Event],
    previousFlushTimeStamp: Long,
    earliestRecordTimestamp: Long,
    retentionTime: Long,
    debugInformation: DebugInformation
)
