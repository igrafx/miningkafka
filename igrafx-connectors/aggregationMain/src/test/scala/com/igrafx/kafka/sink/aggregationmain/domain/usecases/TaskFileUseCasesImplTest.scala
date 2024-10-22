package com.igrafx.kafka.sink.aggregationmain.domain.usecases

import com.igrafx.kafka.sink.aggregationmain.domain.entities.mocks._
import com.igrafx.kafka.sink.aggregationmain.domain.entities._
import com.igrafx.kafka.sink.aggregationmain.domain.enums.ConnectorPropertiesEnum
import com.igrafx.kafka.sink.aggregationmain.domain.exceptions._
import com.igrafx.kafka.sink.aggregationmain.domain.interfaces.{MainApi, MainKafkaLoggingEvents, MainSystem}
import core.UnitTestSpec
import org.apache.kafka.common.KafkaException
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.{spy, times, verify}
import org.mockito.{ArgumentMatchers, Mockito}

import java.nio.charset.StandardCharsets
import java.nio.file.Paths
import java.util.UUID
import scala.concurrent.Future

class TaskFileUseCasesImplTest extends UnitTestSpec {
  private val mainSystemMock = mock[MainSystem]
  private val mainApiMock = mock[MainApi]
  private val mainKafkaLoggingEventsMock = mock[MainKafkaLoggingEvents]

  class TestTaskFileUseCasesImpl extends TaskFileUseCasesImpl {
    override val mainSystem: MainSystem = mainSystemMock
    override val mainApi: MainApi = mainApiMock
    override val mainKafkaLoggingEvents: MainKafkaLoggingEvents = mainKafkaLoggingEventsMock
  }

  private val TaskFileUseCases: TestTaskFileUseCasesImpl = new TestTaskFileUseCasesImpl

  describe("createPushFileEvent") {
    it("should create a correct KafkaLoggedEvent for the pushFile event") {
      val connectorName = "connectorNameTest"
      val projectId = UUID.randomUUID()
      val filePath = Paths.get("filename")
      val topic = "testTopic"
      val partition = 1
      val offsetFrom = 0
      val offsetTo = 1
      val aggregationInformation: DebugInformation = new DebugInformationMock()
        .setTopic(topic)
        .setPartition(partition)
        .setOffsetFrom(offsetFrom)
        .setOffsetTo(offsetTo)
        .build()
      val lines: Iterable[Event] = Seq(new EventMock().build(), new EventMock().build())
      val lineNumber: Int = 1
      val properties: Properties =
        new PropertiesMock().setCsvHeader(false).setConnectorName(connectorName).setProjectId(projectId).build()
      val awaitedSequenceId = TaskFileUseCases.getEventSequenceId(aggregationInformation, properties)

      val resultKafkaLoggedEvent =
        TaskFileUseCases.createPushFileEvent(filePath, lines, lineNumber, properties, aggregationInformation)

      val awaitedKafkaEventPayloadPushFile = new KafkaEventPayloadPushFileMock()
        .setFilePath(filePath)
        .setDate(resultKafkaLoggedEvent.payload.date)
        .setLineNumber(lineNumber)
        .build()

      val awaitedKafkaLoggedEvent = new KafkaLoggedEventMock()
        .setEventType("pushFile")
        .setIGrafxProject(projectId.toString)
        .setEventDate(resultKafkaLoggedEvent.eventDate)
        .setEventSequenceId(awaitedSequenceId)
        .setPayload(awaitedKafkaEventPayloadPushFile)
        .build()

      assert(awaitedKafkaLoggedEvent == resultKafkaLoggedEvent)
    }
  }

  describe("createPushFileEvent") {
    it("should create a correct KafkaLoggedEvent for the issuePushFile") {
      val connectorName = "connectorNameTest"
      val projectId = UUID.randomUUID()
      val filePath = Paths.get("filename")
      val topic = "testTopic"
      val partition = 1
      val offsetFrom = 0
      val offsetTo = 1
      val aggregationInformation: DebugInformation = new DebugInformationMock()
        .setTopic(topic)
        .setPartition(partition)
        .setOffsetFrom(offsetFrom)
        .setOffsetTo(offsetTo)
        .build()
      val lines: Iterable[Event] = Seq(new EventMock().build(), new EventMock().build())
      val exception = SendFileException(message = "test", canRetry = true)
      val properties: Properties =
        new PropertiesMock().setCsvHeader(false).setConnectorName(connectorName).setProjectId(projectId).build()
      val awaitedSequenceId = TaskFileUseCases.getEventSequenceId(aggregationInformation, properties)

      val resultKafkaLoggedEvent =
        TaskFileUseCases.createIssuePushFileEvent(filePath, lines, exception, properties, aggregationInformation)

      val awaitedKafkaEventPayloadIssuePushFile =
        new KafkaEventPayloadIssuePushFileMock()
          .setFilePath(filePath)
          .setDate(resultKafkaLoggedEvent.payload.date)
          .setExceptionType(exception.getClass.getCanonicalName)
          .build()

      val awaitedKafkaLoggedEvent = new KafkaLoggedEventMock()
        .setEventType("issuePushFile")
        .setIGrafxProject(projectId.toString)
        .setEventDate(resultKafkaLoggedEvent.eventDate)
        .setEventSequenceId(awaitedSequenceId)
        .setPayload(awaitedKafkaEventPayloadIssuePushFile)
        .build()

      assert(awaitedKafkaLoggedEvent == resultKafkaLoggedEvent)
    }
  }

  describe("getEventSequenceId") {
    it("should return a correct sequenceEventId") {
      val connectorName = "connectorNameTest"
      val topic = "testTopic"
      val partition = 1
      val offsetFrom = 0
      val offsetTo = 10

      val aggregationInformation = new DebugInformationMock()
        .setTopic(topic)
        .setPartition(partition)
        .setOffsetFrom(offsetFrom)
        .setOffsetTo(offsetTo)
        .build()

      val properties: Properties =
        new PropertiesMock().setConnectorName(connectorName).build()

      val awaitedSequenceEventId = s"${topic}_${partition}_from_${offsetFrom}_to_${offsetTo}_$connectorName"
      val awaitedSequenceEventIdHash =
        UUID.nameUUIDFromBytes(awaitedSequenceEventId.getBytes(StandardCharsets.UTF_8)).toString

      assert(TaskFileUseCases.getEventSequenceId(aggregationInformation, properties) == awaitedSequenceEventIdHash)
    }
  }

  describe("initializeDeleteAndSendNewDataToIGrafx") {
    val properties: Properties = new PropertiesMock().setIsLogging(true).build()
    val csvProperties: CsvProperties = new CsvPropertiesMock().build()
    val kafkaLoggingEventsProperties: KafkaLoggingEventsProperties = new KafkaLoggingEventsPropertiesMock().build()
    val aggregationInformation: DebugInformation = new DebugInformationMock().build()
    val filePath = Paths.get("filename")

    it(
      s"should call the logging pushFile event external method if the ${ConnectorPropertiesEnum.kafkaLoggingEventsIsLoggingProperty.toStringDescription} property is true and the file was created an sent successfully"
    ) {
      val testTaskUseCasesSpy = spy(new TestTaskFileUseCasesImpl)

      val lines = Seq(new EventMock().build())

      Mockito
        .doAnswer(_ => Future.successful(()))
        .when(testTaskUseCasesSpy)
        .initializeProjectPathAndDeleteOldArchivedFiles(any())

      Mockito.doAnswer(_ => filePath).when(testTaskUseCasesSpy).generateFileNameAndPath(any())

      Mockito
        .doAnswer(_ => Future.successful(1))
        .when(testTaskUseCasesSpy)
        .handleCreateAndSendCsvToIGrafx(
          ArgumentMatchers.eq(properties),
          ArgumentMatchers.eq(csvProperties),
          ArgumentMatchers.eq(Some(kafkaLoggingEventsProperties)),
          ArgumentMatchers.eq(lines),
          ArgumentMatchers.eq(filePath),
          ArgumentMatchers.eq(aggregationInformation)
        )

      Mockito
        .doAnswer(_ => Future.successful(()))
        .when(testTaskUseCasesSpy)
        .sendPushFileEventToKafka(
          any(),
          ArgumentMatchers.eq(lines),
          any(),
          ArgumentMatchers.eq(properties),
          ArgumentMatchers.eq(kafkaLoggingEventsProperties),
          ArgumentMatchers.eq(aggregationInformation)
        )

      testTaskUseCasesSpy
        .initializeDeleteAndSendNewDataToIGrafx(
          properties,
          csvProperties,
          Some(kafkaLoggingEventsProperties),
          lines,
          aggregationInformation
        )
        .map { _ =>
          verify(testTaskUseCasesSpy, times(1)).checkAndSendPushFileEventIfLogging(
            any(),
            any(),
            any(),
            any(),
            any(),
            any()
          )
          verify(testTaskUseCasesSpy, times(1)).sendPushFileEventToKafka(any(), any(), any(), any(), any(), any())
          verify(testTaskUseCasesSpy, times(0))
            .checkAndSendIssuePushFileEventIfLogging(any(), any(), any(), any(), any(), any())
          verify(testTaskUseCasesSpy, times(0)).sendIssuePushFileEventToKafka(any(), any(), any(), any(), any(), any())

          assert(true)
        }
        .recover { case _ => assert(false) }
    }

    it(
      s"should call the logging issuePushFile event external method if the ${ConnectorPropertiesEnum.kafkaLoggingEventsIsLoggingProperty.toStringDescription} property is true and the file encountered an issue during its creation or sending"
    ) {
      val testTaskUseCasesSpy = spy(new TestTaskFileUseCasesImpl)

      Mockito
        .doAnswer(_ => Future.successful(()))
        .when(testTaskUseCasesSpy)
        .initializeProjectPathAndDeleteOldArchivedFiles(any())

      Mockito
        .doAnswer(_ => Future.failed(FileCreationException(cause = new Exception)))
        .when(testTaskUseCasesSpy)
        .createAndSendCsvToIGrafx(any(), any(), any(), any())

      Mockito.doAnswer(_ => filePath).when(testTaskUseCasesSpy).generateFileNameAndPath(any())

      Mockito
        .doAnswer(_ => Future.successful(()))
        .when(testTaskUseCasesSpy)
        .sendIssuePushFileEventToKafka(any(), any(), any(), any(), any(), any())

      val lines = Seq(new EventMock().build())

      recoverToSucceededIf[Throwable] {
        testTaskUseCasesSpy.initializeDeleteAndSendNewDataToIGrafx(
          properties,
          csvProperties,
          Some(kafkaLoggingEventsProperties),
          lines,
          aggregationInformation
        )
      }.map { assertion =>
        verify(testTaskUseCasesSpy, times(1)).checkAndSendIssuePushFileEventIfLogging(
          any(),
          any(),
          any(),
          any(),
          any(),
          any()
        )
        verify(testTaskUseCasesSpy, times(1)).sendIssuePushFileEventToKafka(any(), any(), any(), any(), any(), any())
        verify(testTaskUseCasesSpy, times(0)).checkAndSendPushFileEventIfLogging(
          any(),
          any(),
          any(),
          any(),
          any(),
          any()
        )
        verify(testTaskUseCasesSpy, times(0)).sendPushFileEventToKafka(any(), any(), any(), any(), any(), any())

        assertion
      }
    }

    it(
      "should throw an Exception if a FileCreationException is thrown by createAndSendCsvToIGrafx"
    ) {
      val testTaskUseCasesSpy = spy(new TestTaskFileUseCasesImpl)

      Mockito
        .doAnswer(_ => Future.successful(()))
        .when(testTaskUseCasesSpy)
        .initializeProjectPathAndDeleteOldArchivedFiles(any())

      Mockito.doAnswer(_ => Paths.get("filename")).when(testTaskUseCasesSpy).generateFileNameAndPath(any())

      Mockito
        .doAnswer(_ => Future.failed(FileCreationException(new Exception)))
        .when(testTaskUseCasesSpy)
        .createAndSendCsvToIGrafx(any(), any(), any(), any())

      Mockito
        .doAnswer(_ => Future.failed(new KafkaException))
        .when(testTaskUseCasesSpy)
        .sendIssuePushFileEventToKafka(any(), any(), any(), any(), any(), any())

      val lines = Seq(new EventMock().build())

      recoverToSucceededIf[FileCreationException] {
        testTaskUseCasesSpy.initializeDeleteAndSendNewDataToIGrafx(
          properties,
          csvProperties,
          Some(kafkaLoggingEventsProperties),
          lines,
          aggregationInformation
        )
      }
    }

    it(
      "should throw an exception if an InvalidTokenException is thrown by createAndSendCsvToIGrafx"
    ) {
      val testTaskUseCasesSpy = spy(new TestTaskFileUseCasesImpl)

      Mockito
        .doAnswer(_ => Future.successful(()))
        .when(testTaskUseCasesSpy)
        .initializeProjectPathAndDeleteOldArchivedFiles(any())

      Mockito.doAnswer(_ => Paths.get("filename")).when(testTaskUseCasesSpy).generateFileNameAndPath(any())

      Mockito
        .doAnswer(_ => Future.failed(InvalidTokenException("test", canRetry = true)))
        .when(testTaskUseCasesSpy)
        .createAndSendCsvToIGrafx(any(), any(), any(), any())

      Mockito
        .doAnswer(_ => Future.failed(new KafkaException))
        .when(testTaskUseCasesSpy)
        .sendIssuePushFileEventToKafka(any(), any(), any(), any(), any(), any())

      val lines = Seq(new EventMock().build())

      recoverToSucceededIf[InvalidTokenException] {
        testTaskUseCasesSpy.initializeDeleteAndSendNewDataToIGrafx(
          properties,
          csvProperties,
          Some(kafkaLoggingEventsProperties),
          lines,
          aggregationInformation
        )
      }
    }

    it(
      "should throw an exception if a SendFileException is thrown by createAndSendCsvToIGrafx"
    ) {
      val testTaskUseCasesSpy = spy(new TestTaskFileUseCasesImpl)

      Mockito
        .doAnswer(_ => Future.successful(()))
        .when(testTaskUseCasesSpy)
        .initializeProjectPathAndDeleteOldArchivedFiles(any())

      Mockito.doAnswer(_ => Paths.get("filename")).when(testTaskUseCasesSpy).generateFileNameAndPath(any())

      Mockito
        .doAnswer(_ => Future.failed(SendFileException("test", canRetry = true)))
        .when(testTaskUseCasesSpy)
        .createAndSendCsvToIGrafx(any(), any(), any(), any())

      Mockito
        .doAnswer(_ => Future.failed(new KafkaException))
        .when(testTaskUseCasesSpy)
        .sendIssuePushFileEventToKafka(any(), any(), any(), any(), any(), any())

      val lines = Seq(new EventMock().build())

      recoverToSucceededIf[SendFileException] {
        testTaskUseCasesSpy.initializeDeleteAndSendNewDataToIGrafx(
          properties,
          csvProperties,
          Some(kafkaLoggingEventsProperties),
          lines,
          aggregationInformation
        )
      }
    }

    it(
      "should throw an exception if a ProjectPathInitializationException is thrown by initializeProjectPathAndDeleteOldArchivedFiles"
    ) {
      val testTaskUseCasesSpy = spy(new TestTaskFileUseCasesImpl)

      Mockito
        .doAnswer(_ => Future.failed(ProjectPathInitializationException(new Exception)))
        .when(testTaskUseCasesSpy)
        .initializeProjectPathAndDeleteOldArchivedFiles(any())

      Mockito.doAnswer(_ => Paths.get("filename")).when(testTaskUseCasesSpy).generateFileNameAndPath(any())

      Mockito
        .doAnswer(_ => Future.successful(1))
        .when(testTaskUseCasesSpy)
        .createAndSendCsvToIGrafx(any(), any(), any(), any())

      Mockito
        .doAnswer(_ => Future.successful(()))
        .when(testTaskUseCasesSpy)
        .sendPushFileEventToKafka(any(), any(), any(), any(), any(), any())

      val lines = Seq(new EventMock().build())

      recoverToSucceededIf[ProjectPathInitializationException] {
        testTaskUseCasesSpy.initializeDeleteAndSendNewDataToIGrafx(
          properties,
          csvProperties,
          Some(kafkaLoggingEventsProperties),
          lines,
          aggregationInformation
        )
      }
    }

    it(
      "should throw an Exception if an OldArchivedFilesDeletionException is thrown by deleteOldArchivedFiles"
    ) {
      val testTaskUseCasesSpy = spy(new TestTaskFileUseCasesImpl)

      Mockito
        .doAnswer(_ => Future.failed(OldArchivedFilesDeletionException(new Exception)))
        .when(testTaskUseCasesSpy)
        .initializeProjectPathAndDeleteOldArchivedFiles(any())

      Mockito.doAnswer(_ => Paths.get("filename")).when(testTaskUseCasesSpy).generateFileNameAndPath(any())

      Mockito
        .doAnswer(_ => Future.successful(1))
        .when(testTaskUseCasesSpy)
        .createAndSendCsvToIGrafx(any(), any(), any(), any())

      Mockito
        .doAnswer(_ => Future.successful(()))
        .when(testTaskUseCasesSpy)
        .sendPushFileEventToKafka(any(), any(), any(), any(), any(), any())

      val lines = Seq(new EventMock().build())

      recoverToSucceededIf[OldArchivedFilesDeletionException] {
        testTaskUseCasesSpy.initializeDeleteAndSendNewDataToIGrafx(
          properties,
          csvProperties,
          Some(kafkaLoggingEventsProperties),
          lines,
          aggregationInformation
        )
      }
    }

    it(
      "should throw an Exception if the sending of the file goes well but the event logging to Kafka encounters an error"
    ) {
      val testTaskUseCasesSpy = spy(new TestTaskFileUseCasesImpl)

      Mockito
        .doAnswer(_ => Future.successful(()))
        .when(testTaskUseCasesSpy)
        .initializeProjectPathAndDeleteOldArchivedFiles(any())

      Mockito.doAnswer(_ => Paths.get("filename")).when(testTaskUseCasesSpy).generateFileNameAndPath(any())

      Mockito
        .doAnswer(_ => Future.successful(1))
        .when(testTaskUseCasesSpy)
        .createAndSendCsvToIGrafx(any(), any(), any(), any())

      Mockito
        .doAnswer(_ => Future.failed(new KafkaException))
        .when(testTaskUseCasesSpy)
        .sendPushFileEventToKafka(any(), any(), any(), any(), any(), any())

      val lines = Seq(new EventMock().build())

      recoverToSucceededIf[KafkaException] {
        testTaskUseCasesSpy.initializeDeleteAndSendNewDataToIGrafx(
          properties,
          csvProperties,
          Some(kafkaLoggingEventsProperties),
          lines,
          aggregationInformation
        )
      }
    }
  }
}
