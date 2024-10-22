package com.igrafx.kafka.sink.aggregationmain.domain.usecases

import com.igrafx.kafka.sink.aggregationmain.domain.entities.PartitionTracker
import com.igrafx.kafka.sink.aggregationmain.domain.entities.mocks._
import com.igrafx.kafka.sink.aggregationmain.domain.exceptions._
import com.igrafx.kafka.sink.aggregationmain.domain.interfaces.KafkaTopicGetConfiguration
import core.UnitTestSpec
import org.apache.kafka.common.config.ConfigException
import org.apache.kafka.common.record.TimestampType
import org.apache.kafka.connect.sink.SinkRecord
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito._
import org.mockito.{ArgumentMatchers, Mockito}

import java.util
import scala.collection.immutable.HashMap
import scala.concurrent.Future

class TaskAggregationUseCasesImplTest extends UnitTestSpec {
  private val taskFileUseCasesMock = mock[TaskFileUseCasesImpl]
  private val kafkaTopicGetConfigurationMock = mock[KafkaTopicGetConfiguration]

  class TestTaskAggregationUseCasesImpl extends TaskAggregationUseCasesImpl {
    override val taskFileUseCases: TaskFileUseCasesImpl = taskFileUseCasesMock
    override val kafkaTopicGetConfiguration: KafkaTopicGetConfiguration = kafkaTopicGetConfigurationMock
  }

  private val taskAggregationUseCases: TestTaskAggregationUseCasesImpl = new TestTaskAggregationUseCasesImpl

  describe("aggregateAndSendCollection") {
    it("should not call external methods if the collection of SinkRecords is empty") {
      val taskUseCasesSpy = spy(taskAggregationUseCases)
      Mockito
        .doAnswer(_ => new PartitionTrackerMock().build())
        .when(taskUseCasesSpy)
        .processPartition(any(), any())
      Mockito
        .doAnswer(_ => new PartitionTrackerMock().build())
        .when(taskUseCasesSpy)
        .flushPartition(any(), any(), any(), any(), any())

      taskUseCasesSpy.aggregateAndSendCollection(
        new util.ArrayList[SinkRecord](),
        new PropertiesMock().build(),
        new CsvPropertiesMock().build(),
        None,
        new HashMap[String, PartitionTracker].empty
      )
      verify(taskUseCasesSpy, times(0)).processPartition(any(), any())
      verify(taskUseCasesSpy, times(0)).flushPartition(any(), any(), any(), any(), any())
      assert(true)
    }
  }

  describe("processPartition") {
    val taskUseCasesSpy = spy(taskAggregationUseCases)
    val eventResult = new EventMock().build()
    Mockito
      .doAnswer(_ => eventResult)
      .when(taskUseCasesSpy)
      .getParams(
        ArgumentMatchers.any()
      )

    it(
      "should return a correct PartitionTracker if the current processedOffset equals -1 (just after initialization of the PartitionTracker) and the Record's offset equals 0"
    ) {
      val inputPartitionTracker = new PartitionTrackerMock()
        .setNameIndex("topic_1")
        .setProcessedOffset(-1)
        .setFlushedOffset(-1)
        .setCommittedOffset(-1)
        .setPendingValue(Seq.empty)
        .setEarliestRecordTimestamp(Long.MaxValue)
        .setRetentionTime(1000)
        .setDebugInformation(new DebugInformationMock().setOffsetFrom(-1).setOffsetTo(-1).build())
        .setPreviousFlushTimeStamp(System.currentTimeMillis() - 100)
        .build()

      val inputRecord = new SinkRecord("topic", 1, null, "", null, "addedValue", 0, 999, TimestampType.CREATE_TIME)
      val expectedPartitionTracker = new PartitionTrackerMock()
        .setNameIndex("topic_1")
        .setProcessedOffset(0)
        .setFlushedOffset(-1)
        .setCommittedOffset(-1)
        .setPendingValue(Seq(eventResult))
        .setEarliestRecordTimestamp(inputRecord.timestamp())
        .setRetentionTime(1000)
        .setDebugInformation(new DebugInformationMock().setOffsetFrom(0).setOffsetTo(0).build())
        .setPreviousFlushTimeStamp(100)
        .build()

      val resultPartitionTracker = taskUseCasesSpy.processPartition(inputPartitionTracker, inputRecord)
      assert(
        (resultPartitionTracker.nameIndex == expectedPartitionTracker.nameIndex) && (resultPartitionTracker.processedOffset == expectedPartitionTracker.processedOffset) && (resultPartitionTracker.flushedOffset == expectedPartitionTracker.flushedOffset) && (resultPartitionTracker.committedOffset == expectedPartitionTracker.committedOffset) && (resultPartitionTracker.pendingValue == expectedPartitionTracker.pendingValue) && (resultPartitionTracker.previousFlushTimeStamp != inputPartitionTracker.previousFlushTimeStamp) && (resultPartitionTracker.earliestRecordTimestamp == expectedPartitionTracker.earliestRecordTimestamp) && (resultPartitionTracker.retentionTime == expectedPartitionTracker.retentionTime) && (resultPartitionTracker.debugInformation == expectedPartitionTracker.debugInformation)
      )
    }

    it(
      "should return a correct PartitionTracker if the current processedOffset equals -1 (just after initialization of the PartitionTracker) and the Record's offset equals 20"
    ) {
      val inputPartitionTracker = new PartitionTrackerMock()
        .setNameIndex("topic_1")
        .setProcessedOffset(-1)
        .setFlushedOffset(-1)
        .setCommittedOffset(-1)
        .setPendingValue(Seq.empty)
        .setEarliestRecordTimestamp(Long.MaxValue)
        .setRetentionTime(1000)
        .setDebugInformation(new DebugInformationMock().setOffsetFrom(-1).setOffsetTo(-1).build())
        .setPreviousFlushTimeStamp(System.currentTimeMillis() - 100)
        .build()

      val inputRecord = new SinkRecord("topic", 1, null, "", null, "addedValue", 20, 999, TimestampType.CREATE_TIME)
      val expectedPartitionTracker = new PartitionTrackerMock()
        .setNameIndex("topic_1")
        .setProcessedOffset(20)
        .setFlushedOffset(19)
        .setCommittedOffset(19)
        .setPendingValue(Seq(eventResult))
        .setEarliestRecordTimestamp(inputRecord.timestamp())
        .setRetentionTime(1000)
        .setDebugInformation(new DebugInformationMock().setOffsetFrom(20).setOffsetTo(20).build())
        .setPreviousFlushTimeStamp(100)
        .build()

      val resultPartitionTracker = taskUseCasesSpy.processPartition(inputPartitionTracker, inputRecord)
      assert(
        (resultPartitionTracker.nameIndex == expectedPartitionTracker.nameIndex) && (resultPartitionTracker.processedOffset == expectedPartitionTracker.processedOffset) && (resultPartitionTracker.flushedOffset == expectedPartitionTracker.flushedOffset) && (resultPartitionTracker.committedOffset == expectedPartitionTracker.committedOffset) && (resultPartitionTracker.pendingValue == expectedPartitionTracker.pendingValue) && (resultPartitionTracker.previousFlushTimeStamp != inputPartitionTracker.previousFlushTimeStamp && (resultPartitionTracker.earliestRecordTimestamp == expectedPartitionTracker.earliestRecordTimestamp) && (resultPartitionTracker.retentionTime == expectedPartitionTracker.retentionTime) && (resultPartitionTracker.debugInformation == expectedPartitionTracker.debugInformation))
      )
    }

    it("should return a correct PartitionTracker if the current processedOffset is different than -1") {
      val event1 = new EventMock().build()
      val event2 = new EventMock().build()

      val inputPartitionTracker = new PartitionTrackerMock()
        .setNameIndex("topic_1")
        .setProcessedOffset(20)
        .setFlushedOffset(16)
        .setCommittedOffset(14)
        .setPendingValue(Seq(event1, event2))
        .setPreviousFlushTimeStamp(100)
        .setEarliestRecordTimestamp(1000)
        .setRetentionTime(1000)
        .setDebugInformation(new DebugInformationMock().setOffsetFrom(17).setOffsetTo(20).build())
        .build()

      val inputRecord = new SinkRecord("topic", 1, null, "", null, "addedValue", 21, 999, TimestampType.CREATE_TIME)
      val expectedPartitionTracker = new PartitionTrackerMock()
        .setNameIndex("topic_1")
        .setProcessedOffset(21)
        .setFlushedOffset(16)
        .setCommittedOffset(14)
        .setPendingValue(Seq(event1, event2, eventResult))
        .setPreviousFlushTimeStamp(100)
        .setEarliestRecordTimestamp(inputRecord.timestamp())
        .setRetentionTime(1000)
        .setDebugInformation(new DebugInformationMock().setOffsetFrom(17).setOffsetTo(21).build())
        .build()

      assert(expectedPartitionTracker == taskUseCasesSpy.processPartition(inputPartitionTracker, inputRecord))
    }
  }

  describe("flushPartition") {
    val properties = new PropertiesMock().build()
    val csvProperties = new CsvPropertiesMock().build()
    val kafkaLoggingEventsPropertiesOpt = None

    it(
      "should only change the previousFlushTimeStamp in the PartitionTracker if the current processedOffset is not superior than the current flushedOffset"
    ) {
      val inputPartitionTracker = new PartitionTrackerMock()
        .setNameIndex("topic_1")
        .setProcessedOffset(20)
        .setFlushedOffset(20)
        .setCommittedOffset(20)
        .setPendingValue(Seq.empty)
        .setPreviousFlushTimeStamp(System.currentTimeMillis() - 100)
        .build()

      val result = taskAggregationUseCases.flushPartition(
        inputPartitionTracker,
        properties,
        csvProperties,
        kafkaLoggingEventsPropertiesOpt
      )
      assert(
        (result.nameIndex == inputPartitionTracker.nameIndex) && (result.processedOffset == inputPartitionTracker.processedOffset) && (result.flushedOffset == inputPartitionTracker.flushedOffset) && (result.committedOffset == inputPartitionTracker.committedOffset) && (result.pendingValue == inputPartitionTracker.pendingValue) && (result.previousFlushTimeStamp != inputPartitionTracker.previousFlushTimeStamp)
      )
    }
    it(
      "should return a correct PartitionTracker if the current processedOffset is superior than the current flushedOffset"
    ) {
      val inputPartitionTracker = new PartitionTrackerMock()
        .setNameIndex("topic_1")
        .setProcessedOffset(23)
        .setFlushedOffset(20)
        .setCommittedOffset(20)
        .setPendingValue(Seq(new EventMock().build(), new EventMock().build(), new EventMock().build()))
        .setPreviousFlushTimeStamp(System.currentTimeMillis() - 100)
        .build()

      when(
        taskFileUseCasesMock.initializeDeleteAndSendNewDataToIGrafx(
          properties,
          csvProperties,
          kafkaLoggingEventsPropertiesOpt,
          inputPartitionTracker.pendingValue,
          inputPartitionTracker.debugInformation
        )
      ).thenReturn(Future.successful(()))

      val expectedPartition = new PartitionTrackerMock()
        .setNameIndex("topic_1")
        .setProcessedOffset(23)
        .setFlushedOffset(23)
        .setCommittedOffset(20)
        .setPendingValue(Seq.empty)
        .setPreviousFlushTimeStamp(System.currentTimeMillis())
        .build()

      val result = taskAggregationUseCases.flushPartition(
        inputPartitionTracker,
        properties,
        csvProperties,
        kafkaLoggingEventsPropertiesOpt
      )

      assert(
        (result.nameIndex == expectedPartition.nameIndex) && (result.processedOffset == expectedPartition.processedOffset) && (result.flushedOffset == expectedPartition.flushedOffset) && (result.committedOffset == expectedPartition.committedOffset) && (result.pendingValue == expectedPartition.pendingValue) && (result.previousFlushTimeStamp != inputPartitionTracker.previousFlushTimeStamp)
      )
    }

    it("should throw an exception if the initializeDeleteAndSendNewDataToIGrafx method throws an exception") {
      val inputPartitionTracker = new PartitionTrackerMock().build()

      when(
        taskFileUseCasesMock.initializeDeleteAndSendNewDataToIGrafx(
          properties,
          csvProperties,
          kafkaLoggingEventsPropertiesOpt,
          inputPartitionTracker.pendingValue,
          inputPartitionTracker.debugInformation
        )
      ).thenReturn(Future.failed(ProjectPathInitializationException(cause = new Exception)))

      assertThrows[SendAggregationResultException] {
        taskAggregationUseCases.flushPartition(
          inputPartitionTracker,
          properties,
          csvProperties,
          kafkaLoggingEventsPropertiesOpt
        )
      }

      when(
        taskFileUseCasesMock.initializeDeleteAndSendNewDataToIGrafx(
          properties,
          csvProperties,
          kafkaLoggingEventsPropertiesOpt,
          inputPartitionTracker.pendingValue,
          inputPartitionTracker.debugInformation
        )
      ).thenReturn(Future.failed(OldArchivedFilesDeletionException(cause = new Exception)))

      assertThrows[SendAggregationResultException] {
        taskAggregationUseCases.flushPartition(
          inputPartitionTracker,
          properties,
          csvProperties,
          kafkaLoggingEventsPropertiesOpt
        )
      }

      when(
        taskFileUseCasesMock.initializeDeleteAndSendNewDataToIGrafx(
          properties,
          csvProperties,
          kafkaLoggingEventsPropertiesOpt,
          inputPartitionTracker.pendingValue,
          inputPartitionTracker.debugInformation
        )
      ).thenReturn(Future.failed(FileCreationException(cause = new Exception)))

      assertThrows[SendAggregationResultException] {
        taskAggregationUseCases.flushPartition(
          inputPartitionTracker,
          properties,
          csvProperties,
          kafkaLoggingEventsPropertiesOpt
        )
      }

      when(
        taskFileUseCasesMock.initializeDeleteAndSendNewDataToIGrafx(
          properties,
          csvProperties,
          kafkaLoggingEventsPropertiesOpt,
          inputPartitionTracker.pendingValue,
          inputPartitionTracker.debugInformation
        )
      ).thenReturn(Future.failed(InvalidTokenException(message = "", canRetry = false)))

      assertThrows[SendAggregationResultException] {
        taskAggregationUseCases.flushPartition(
          inputPartitionTracker,
          properties,
          csvProperties,
          kafkaLoggingEventsPropertiesOpt
        )
      }

      when(
        taskFileUseCasesMock.initializeDeleteAndSendNewDataToIGrafx(
          properties,
          csvProperties,
          kafkaLoggingEventsPropertiesOpt,
          inputPartitionTracker.pendingValue,
          inputPartitionTracker.debugInformation
        )
      ).thenReturn(Future.failed(SendFileException(message = "", canRetry = false)))

      assertThrows[SendAggregationResultException] {
        taskAggregationUseCases.flushPartition(
          inputPartitionTracker,
          properties,
          csvProperties,
          kafkaLoggingEventsPropertiesOpt
        )
      }

      when(
        taskFileUseCasesMock.initializeDeleteAndSendNewDataToIGrafx(
          properties,
          csvProperties,
          kafkaLoggingEventsPropertiesOpt,
          inputPartitionTracker.pendingValue,
          inputPartitionTracker.debugInformation
        )
      ).thenReturn(Future.failed(new ConfigException("")))

      assertThrows[SendAggregationResultException] {
        taskAggregationUseCases.flushPartition(
          inputPartitionTracker,
          properties,
          csvProperties,
          kafkaLoggingEventsPropertiesOpt
        )
      }
    }
  }

  describe("commitPartition") {
    it("should return None if there are no new offset to commit") {
      val inputPartitionTracker = new PartitionTrackerMock()
        .setNameIndex("topic_1")
        .setProcessedOffset(20)
        .setFlushedOffset(20)
        .setCommittedOffset(20)
        .setPendingValue(Seq.empty)
        .setPreviousFlushTimeStamp(System.currentTimeMillis())
        .build()

      assert(taskAggregationUseCases.commitPartition(inputPartitionTracker).isEmpty)
    }

    it(
      "should return a Some with the updated partitionTracker if the current flushedOffset is superior than the current committedOffset"
    ) {
      val inputPartitionTracker = new PartitionTrackerMock()
        .setNameIndex("topic_1")
        .setProcessedOffset(20)
        .setFlushedOffset(24)
        .setCommittedOffset(20)
        .setPendingValue(Seq.empty)
        .setPreviousFlushTimeStamp(100)
        .build()

      val expectedPartitionTrackerOption = Some(
        new PartitionTrackerMock()
          .setNameIndex("topic_1")
          .setProcessedOffset(20)
          .setFlushedOffset(24)
          .setCommittedOffset(24)
          .setPendingValue(Seq.empty)
          .setPreviousFlushTimeStamp(100)
          .build()
      )
      assert(taskAggregationUseCases.commitPartition(inputPartitionTracker) == expectedPartitionTrackerOption)
    }
  }

  describe("getNameIndex") {
    it("should return a correct nameIndex for a given topic and a given partition") {
      assert(taskAggregationUseCases.getNameIndex("topic", 1) == "topic_1")
    }
  }

  describe("flushPartitionIfValuePatternOrElementNumberThresholdVerified") {
    val csvProperties = new CsvPropertiesMock().build()
    val kafkaLoggingEventsPropertiesOpt = None

    it("should flush the partition if the Element Number Threshold is reached") {
      val taskUseCasesSpy = spy(taskAggregationUseCases)
      val previousFlushTimestamp = System.currentTimeMillis()

      val processedOffsetFlush = 5
      val processedOffsetNoFlush = 4
      val flushedOffset = 2

      val valuePattern = ""
      val processedValue1 = "value2"
      val processedEvent1 = new EventMock().setEvent(List(new ParamMock().setText(processedValue1).build())).build()
      val processedValue2 = "value3"
      val processedEvent2 = new EventMock().setEvent(List(new ParamMock().setText(processedValue1).build())).build()

      require(!processedValue1.matches(valuePattern) && !processedValue2.matches(valuePattern))
      require(processedOffsetFlush > processedOffsetNoFlush)

      val properties = new PropertiesMock()
        .setElementNumberThreshold(processedOffsetFlush - flushedOffset)
        .setValuePatternThreshold(valuePattern)
        .build()

      // Element Number Threshold not reached

      val inputPartitionNoFlush = new PartitionTrackerMock()
        .setNameIndex("topic_1")
        .setProcessedOffset(processedOffsetNoFlush)
        .setFlushedOffset(flushedOffset)
        .setPendingValue(Seq(new EventMock().build(), processedEvent1))
        .setCommittedOffset(2)
        .setPreviousFlushTimeStamp(previousFlushTimestamp)
        .build()
      val inputRecordNoFlush = new SinkRecord("topic", 1, null, "", null, processedValue1, processedOffsetNoFlush)
      val inputPartitionTrackerMapNoFlush = HashMap[String, PartitionTracker]("topic_1" -> inputPartitionNoFlush)

      Mockito
        .doAnswer(_ => throw new Exception("Should not call flushPartition"))
        .when(taskUseCasesSpy)
        .flushPartition(any(), any(), any(), any())

      val resultPartitionTrackerMapNoFlush =
        taskUseCasesSpy.flushPartitionIfValuePatternOrElementNumberThresholdVerified(
          inputPartitionNoFlush,
          inputPartitionTrackerMapNoFlush,
          properties,
          csvProperties,
          kafkaLoggingEventsPropertiesOpt,
          inputRecordNoFlush
        )

      verify(taskUseCasesSpy, times(0))
        .flushPartition(any(), any(), any(), any())
      assert(resultPartitionTrackerMapNoFlush == inputPartitionTrackerMapNoFlush)

      // Element Number Threshold reached

      val inputPartition = new PartitionTrackerMock()
        .setNameIndex("topic_1")
        .setProcessedOffset(processedOffsetFlush)
        .setFlushedOffset(flushedOffset)
        .setPendingValue(Seq(new EventMock().build(), new EventMock().build(), processedEvent2))
        .setCommittedOffset(2)
        .setPreviousFlushTimeStamp(previousFlushTimestamp)
        .build()
      val inputRecord = new SinkRecord("topic", 1, null, "", null, processedValue2, processedOffsetFlush)
      val inputPartitionTrackerMap = HashMap[String, PartitionTracker]("topic_1" -> inputPartition)

      val updatedPartition = new PartitionTrackerMock()
        .setNameIndex("topic_1")
        .setProcessedOffset(processedOffsetFlush)
        .setFlushedOffset(processedOffsetFlush)
        .setCommittedOffset(2)
        .setPendingValue(Seq.empty)
        .setPreviousFlushTimeStamp(previousFlushTimestamp + 100)
        .build()

      Mockito
        .doAnswer(_ => updatedPartition)
        .when(taskUseCasesSpy)
        .flushPartition(inputPartition, properties, csvProperties, kafkaLoggingEventsPropertiesOpt)

      val resultPartitionTrackerMap = taskUseCasesSpy.flushPartitionIfValuePatternOrElementNumberThresholdVerified(
        inputPartition,
        inputPartitionTrackerMap,
        properties,
        csvProperties,
        kafkaLoggingEventsPropertiesOpt,
        inputRecord
      )

      verify(taskUseCasesSpy, times(1))
        .flushPartition(inputPartition, properties, csvProperties, kafkaLoggingEventsPropertiesOpt)

      assert(resultPartitionTrackerMap.apply("topic_1") == updatedPartition)
    }

    it("should flush the partition if the Value Pattern Threshold is verified") {
      val taskUseCasesSpy = spy(taskAggregationUseCases)
      val previousFlushTimestamp = System.currentTimeMillis()

      val processedOffsetFlush = 5
      val processedOffsetNoFlush = 4
      val flushedOffset = 2
      val elementNumberThreshold = processedOffsetFlush - flushedOffset + 1

      val valuePattern = "value3"
      val processedValueNoFlush = "value2"
      val processedEventNoFlush =
        new EventMock().setEvent(List(new ParamMock().setText(processedValueNoFlush).build())).build()
      val processedValueFlush = "value3"
      val processedEventFlush =
        new EventMock().setEvent(List(new ParamMock().setText(processedValueFlush).build())).build()

      require(processedValueFlush.matches(valuePattern) && !processedValueNoFlush.matches(valuePattern))
      require(elementNumberThreshold > processedOffsetFlush - flushedOffset)

      val properties = new PropertiesMock()
        .setElementNumberThreshold(elementNumberThreshold)
        .setValuePatternThreshold(valuePattern)
        .build()

      // Value Pattern Threshold not verified

      val inputPartitionNoFlush = new PartitionTrackerMock()
        .setNameIndex("topic_1")
        .setProcessedOffset(processedOffsetNoFlush)
        .setFlushedOffset(flushedOffset)
        .setPendingValue(Seq(new EventMock().build(), processedEventNoFlush))
        .setCommittedOffset(2)
        .setPreviousFlushTimeStamp(previousFlushTimestamp)
        .build()
      val inputRecordNoFlush = new SinkRecord("topic", 1, null, "", null, processedValueNoFlush, processedOffsetNoFlush)
      val inputPartitionTrackerMapNoFlush = HashMap[String, PartitionTracker]("topic_1" -> inputPartitionNoFlush)

      Mockito
        .doAnswer(_ => throw new Exception("Should not call flushPartition"))
        .when(taskUseCasesSpy)
        .flushPartition(any(), any(), any(), any())

      val resultPartitionTrackerMapNoFlush =
        taskUseCasesSpy.flushPartitionIfValuePatternOrElementNumberThresholdVerified(
          inputPartitionNoFlush,
          inputPartitionTrackerMapNoFlush,
          properties,
          csvProperties,
          kafkaLoggingEventsPropertiesOpt,
          inputRecordNoFlush
        )

      verify(taskUseCasesSpy, times(0))
        .flushPartition(any(), any(), any(), any())
      assert(resultPartitionTrackerMapNoFlush == inputPartitionTrackerMapNoFlush)

      // Value Pattern Threshold verified

      val inputPartition = new PartitionTrackerMock()
        .setNameIndex("topic_1")
        .setProcessedOffset(processedOffsetFlush)
        .setFlushedOffset(flushedOffset)
        .setPendingValue(Seq(new EventMock().build(), new EventMock().build(), processedEventFlush))
        .setCommittedOffset(2)
        .setPreviousFlushTimeStamp(previousFlushTimestamp)
        .build()
      val inputRecord = new SinkRecord("topic", 1, null, "", null, processedValueFlush, processedOffsetFlush)
      val inputPartitionTrackerMap = HashMap[String, PartitionTracker]("topic_1" -> inputPartition)

      val updatedPartition = new PartitionTrackerMock()
        .setNameIndex("topic_1")
        .setProcessedOffset(processedOffsetFlush)
        .setFlushedOffset(processedOffsetFlush)
        .setCommittedOffset(2)
        .setPendingValue(Seq.empty)
        .setPreviousFlushTimeStamp(previousFlushTimestamp + 100)
        .build()

      Mockito
        .doAnswer(_ => updatedPartition)
        .when(taskUseCasesSpy)
        .flushPartition(inputPartition, properties, csvProperties, kafkaLoggingEventsPropertiesOpt)

      val resultPartitionTrackerMap = taskUseCasesSpy.flushPartitionIfValuePatternOrElementNumberThresholdVerified(
        inputPartition,
        inputPartitionTrackerMap,
        properties,
        csvProperties,
        kafkaLoggingEventsPropertiesOpt,
        inputRecord
      )

      verify(taskUseCasesSpy, times(1))
        .flushPartition(inputPartition, properties, csvProperties, kafkaLoggingEventsPropertiesOpt)

      assert(resultPartitionTrackerMap.apply("topic_1") == updatedPartition)
    }
  }

  describe("flushPartitionIfTimeoutThresholdVerified") {
    val csvProperties = new CsvPropertiesMock().build()
    val kafkaLoggingEventsPropertiesOpt = None

    it("should flush the partition if the Timeout Threshold is reached") {
      val taskUseCasesSpy = spy(taskAggregationUseCases)

      val properties = new PropertiesMock().build()

      // Timeout Threshold not reached

      val inputPartitionNoFlush = new PartitionTrackerMock()
        .setNameIndex("topic_1")
        .setProcessedOffset(2)
        .setFlushedOffset(1)
        .setPendingValue(Seq(new EventMock().build()))
        .setCommittedOffset(1)
        .setPreviousFlushTimeStamp(System.currentTimeMillis())
        .build()
      val inputPartitionTrackerMapNoFlush = HashMap[String, PartitionTracker]("topic_1" -> inputPartitionNoFlush)

      Mockito
        .doAnswer(_ => false)
        .when(taskUseCasesSpy)
        .shouldFlushPartition(any(), any())

      Mockito
        .doAnswer(_ => false)
        .when(taskUseCasesSpy)
        .shouldFlushPartitionBecauseOfRetention(any(), any())

      val resultPartitionTrackerMapNoFlush =
        taskUseCasesSpy.flushPartitionIfTimeoutThresholdOrRetentionVerified(
          inputPartitionNoFlush,
          inputPartitionTrackerMapNoFlush,
          properties,
          csvProperties,
          kafkaLoggingEventsPropertiesOpt
        )

      verify(taskUseCasesSpy, times(0))
        .flushPartition(any(), any(), any(), any(), any())
      assert(resultPartitionTrackerMapNoFlush == inputPartitionTrackerMapNoFlush)

      // Timeout Threshold reached

      val previousFlushTimestamp = System.currentTimeMillis()
      val inputPartition = new PartitionTrackerMock()
        .setNameIndex("topic_1")
        .setProcessedOffset(2)
        .setFlushedOffset(1)
        .setPendingValue(Seq(new EventMock().build()))
        .setCommittedOffset(1)
        .setPreviousFlushTimeStamp(previousFlushTimestamp)
        .build()
      val inputPartitionTrackerMap = HashMap[String, PartitionTracker]("topic_1" -> inputPartition)

      Mockito
        .doAnswer(_ => true)
        .when(taskUseCasesSpy)
        .shouldFlushPartition(any(), any())

      val updatedPartition = new PartitionTrackerMock()
        .setNameIndex("topic_1")
        .setProcessedOffset(2)
        .setFlushedOffset(2)
        .setCommittedOffset(1)
        .setPendingValue(Seq.empty)
        .setPreviousFlushTimeStamp(previousFlushTimestamp + 100)
        .build()

      Mockito
        .doAnswer(_ => updatedPartition)
        .when(taskUseCasesSpy)
        .flushPartition(inputPartition, properties, csvProperties, kafkaLoggingEventsPropertiesOpt)

      val resultPartitionTrackerMap = taskUseCasesSpy.flushPartitionIfTimeoutThresholdOrRetentionVerified(
        inputPartition,
        inputPartitionTrackerMap,
        properties,
        csvProperties,
        kafkaLoggingEventsPropertiesOpt
      )

      verify(taskUseCasesSpy, times(1))
        .flushPartition(inputPartition, properties, csvProperties, kafkaLoggingEventsPropertiesOpt)

      assert(resultPartitionTrackerMap.apply("topic_1") == updatedPartition)
    }

    it("should flush the partition if the Retention Threshold is reached") {
      val taskUseCasesSpy = spy(taskAggregationUseCases)

      val properties = new PropertiesMock().build()

      // Retention Threshold not reached

      val inputPartitionNoFlush = new PartitionTrackerMock()
        .setNameIndex("topic_1")
        .setProcessedOffset(2)
        .setFlushedOffset(1)
        .setPendingValue(Seq(new EventMock().build()))
        .setCommittedOffset(1)
        .setPreviousFlushTimeStamp(System.currentTimeMillis())
        .build()
      val inputPartitionTrackerMapNoFlush = HashMap[String, PartitionTracker]("topic_1" -> inputPartitionNoFlush)

      Mockito
        .doAnswer(_ => false)
        .when(taskUseCasesSpy)
        .shouldFlushPartition(any(), any())

      Mockito
        .doAnswer(_ => false)
        .when(taskUseCasesSpy)
        .shouldFlushPartitionBecauseOfRetention(any(), any())

      val resultPartitionTrackerMapNoFlush =
        taskUseCasesSpy.flushPartitionIfTimeoutThresholdOrRetentionVerified(
          inputPartitionNoFlush,
          inputPartitionTrackerMapNoFlush,
          properties,
          csvProperties,
          kafkaLoggingEventsPropertiesOpt
        )

      verify(taskUseCasesSpy, times(0))
        .flushPartition(any(), any(), any(), any(), any())
      assert(resultPartitionTrackerMapNoFlush == inputPartitionTrackerMapNoFlush)

      // Timeout Threshold reached

      val previousFlushTimestamp = System.currentTimeMillis()
      val inputPartition = new PartitionTrackerMock()
        .setNameIndex("topic_1")
        .setProcessedOffset(2)
        .setFlushedOffset(1)
        .setPendingValue(Seq(new EventMock().build()))
        .setCommittedOffset(1)
        .setPreviousFlushTimeStamp(previousFlushTimestamp)
        .build()
      val inputPartitionTrackerMap = HashMap[String, PartitionTracker]("topic_1" -> inputPartition)

      Mockito
        .doAnswer(_ => true)
        .when(taskUseCasesSpy)
        .shouldFlushPartitionBecauseOfRetention(any(), any())

      val updatedPartition = new PartitionTrackerMock()
        .setNameIndex("topic_1")
        .setProcessedOffset(2)
        .setFlushedOffset(2)
        .setCommittedOffset(1)
        .setPendingValue(Seq.empty)
        .setPreviousFlushTimeStamp(previousFlushTimestamp + 100)
        .setEarliestRecordTimestamp(Long.MaxValue)
        .setDebugInformation(new DebugInformationMock().setOffsetFrom(-1).setOffsetTo(-1).build())
        .build()

      Mockito
        .doAnswer(_ => updatedPartition)
        .when(taskUseCasesSpy)
        .flushPartition(inputPartition, properties, csvProperties, kafkaLoggingEventsPropertiesOpt)

      val resultPartitionTrackerMap = taskUseCasesSpy.flushPartitionIfTimeoutThresholdOrRetentionVerified(
        inputPartition,
        inputPartitionTrackerMap,
        properties,
        csvProperties,
        kafkaLoggingEventsPropertiesOpt
      )

      verify(taskUseCasesSpy, times(1))
        .flushPartition(inputPartition, properties, csvProperties, kafkaLoggingEventsPropertiesOpt)

      assert(resultPartitionTrackerMap.apply("topic_1") == updatedPartition)
    }
  }
}
