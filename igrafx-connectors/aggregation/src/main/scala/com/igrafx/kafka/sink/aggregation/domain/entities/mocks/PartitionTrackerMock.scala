package com.igrafx.kafka.sink.aggregation.domain.entities.mocks

import com.igrafx.core.Mock
import com.igrafx.kafka.sink.aggregation.domain.entities.{DebugInformation, PartitionTracker}

class PartitionTrackerMock extends Mock[PartitionTracker] {
  private var nameIndex: String = "testNameIndex"
  private var processedOffset: Long = 2
  private var flushedOffset: Long = 1
  private var committedOffset: Long = 0
  private var pendingValue: Seq[AnyRef] = Seq("testValue")
  private var previousFlushTimeStamp: Long = System.currentTimeMillis()
  private var earliestRecordTimestamp: Long = Long.MaxValue
  private var retentionTime: Long = 604800000
  private var debugInformation: DebugInformation = new DebugInformationMock().build()

  def setNameIndex(nameIndex: String): PartitionTrackerMock = {
    this.nameIndex = nameIndex
    this
  }

  def setProcessedOffset(processedOffset: Int): PartitionTrackerMock = {
    this.processedOffset = processedOffset
    this
  }

  def setFlushedOffset(flushedOffset: Int): PartitionTrackerMock = {
    this.flushedOffset = flushedOffset
    this
  }

  def setCommittedOffset(committedOffset: Int): PartitionTrackerMock = {
    this.committedOffset = committedOffset
    this
  }

  def setPendingValue(pendingValue: Seq[AnyRef]): PartitionTrackerMock = {
    this.pendingValue = pendingValue
    this
  }

  def setPreviousFlushTimeStamp(previousFlushTimeStamp: Long): PartitionTrackerMock = {
    this.previousFlushTimeStamp = previousFlushTimeStamp
    this
  }

  def setEarliestRecordTimestamp(earliestRecordTimestamp: Long): PartitionTrackerMock = {
    this.earliestRecordTimestamp = earliestRecordTimestamp
    this
  }

  def setRetentionTime(retentionTime: Long): PartitionTrackerMock = {
    this.retentionTime = retentionTime
    this
  }

  def setDebugInformation(debugInformation: DebugInformation): PartitionTrackerMock = {
    this.debugInformation = debugInformation
    this
  }

  override def build(): PartitionTracker = {
    PartitionTracker(
      nameIndex = nameIndex,
      processedOffset = processedOffset,
      flushedOffset = flushedOffset,
      committedOffset = committedOffset,
      pendingValue = pendingValue,
      previousFlushTimeStamp = previousFlushTimeStamp,
      earliestRecordTimestamp = earliestRecordTimestamp,
      retentionTime = retentionTime,
      debugInformation = debugInformation
    )
  }
}
