package com.igrafx.kafka.sink.aggregation.domain.entities

final case class PartitionTracker(
    nameIndex: String,
    processedOffset: Long,
    flushedOffset: Long,
    committedOffset: Long,
    pendingValue: Seq[AnyRef],
    previousFlushTimeStamp: Long,
    earliestRecordTimestamp: Long,
    retentionTime: Long,
    debugInformation: DebugInformation
)
