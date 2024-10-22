package com.igrafx.kafka.sink.aggregationmain.domain.usecases

import com.igrafx.kafka.sink.aggregationmain.adapters.api.MainApiImpl
import com.igrafx.kafka.sink.aggregationmain.adapters.services.MainKafkaLoggingEventsImpl
import com.igrafx.kafka.sink.aggregationmain.adapters.system.MainSystemImpl
import com.igrafx.kafka.sink.aggregationmain.config.MainConfig
import com.igrafx.kafka.sink.aggregationmain.domain.entities._
import com.igrafx.kafka.sink.aggregationmain.domain.exceptions._
import com.igrafx.kafka.sink.aggregationmain.domain.interfaces.{MainApi, MainKafkaLoggingEvents, MainSystem}
import com.igrafx.utils.FileUtils
import org.apache.avro.AvroRuntimeException
import org.apache.kafka.common.KafkaException
import org.apache.kafka.common.config.ConfigException
import org.apache.kafka.common.errors.InterruptException
import org.slf4j.{Logger, LoggerFactory}

import java.nio.charset.StandardCharsets
import java.nio.file.{Path, Paths}
import java.util.UUID
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

protected[usecases] class TaskFileUseCasesImpl extends TaskFileUseCases {
  private implicit val log: Logger = LoggerFactory.getLogger(classOf[TaskFileUseCasesImpl])

  private[aggregationmain] val mainSystem: MainSystem = new MainSystemImpl
  private[aggregationmain] val mainApi: MainApi = new MainApiImpl
  private[aggregationmain] val mainKafkaLoggingEvents: MainKafkaLoggingEvents = new MainKafkaLoggingEventsImpl(log)

  /** Method used to initialize the project path, to deletthe old archive files, and to write and send a CSV file containing the new data coming from Kafka
    *
    * @param properties The Task Properties
    * @param csvProperties The Task CsvProperties
    * @param kafkaLoggingEventsPropertiesOpt Option of the Task Kafka Logging properties
    * @param lines The Collection of SinkRecord added to the file
    *
    * @throws ProjectPathInitializationException If there is an issue during the initialization of the project path
    * @throws OldArchivedFilesDeletionException If there is an issue during the deletion of old archived files
    * @throws FileCreationException Issue during the creation or the writing of the file storing data
    * @throws InvalidTokenException Couldn't retrieve a Token from iGrafx because of an Authentication or Server Issue
    * @throws SendFileException Couldn't send the file to the iGrafx Mining API
    * @throws ConfigException If the Task KafkaLoggingEventsProperties are missing
    */
  def initializeDeleteAndSendNewDataToIGrafx(
      properties: Properties,
      csvProperties: CsvProperties,
      kafkaLoggingEventsPropertiesOpt: Option[KafkaLoggingEventsProperties],
      lines: Iterable[Event],
      aggregationInformation: DebugInformation
  ): Future[Unit] = {
    val filePath = generateFileNameAndPath(csvProperties)
    for {
      _ <- initializeProjectPathAndDeleteOldArchivedFiles(properties)
      lineNumber <- handleCreateAndSendCsvToIGrafx(
        properties,
        csvProperties,
        kafkaLoggingEventsPropertiesOpt,
        lines,
        filePath,
        aggregationInformation
      )
      _ <- checkAndSendPushFileEventIfLogging(
        filePath = filePath,
        lines = lines,
        lineNumber = lineNumber,
        properties = properties,
        kafkaLoggingEventsPropertiesOpt = kafkaLoggingEventsPropertiesOpt,
        aggregationInformation = aggregationInformation
      )
    } yield ()
  }

  /** Method used to create, write and send the CSV file containing the data coming from Kafka
    *
    * @param properties The Task Properties
    * @param csvProperties The Task CsvProperties
    * @param kafkaLoggingEventsPropertiesOpt Option of the Task Kafka Logging properties
    * @param lines The Collection of SinkRecord added to the file
    * @param filename The name of the file
    *
    * @return an Int corresponding to the number of lines written in the file
    *
    * @throws FileCreationException Issue during the creation or the writing of the file storing data
    * @throws InvalidTokenException Couldn't retrieve a Token from iGrafx because of an Authentication or Server Issue
    * @throws SendFileException Couldn't send the file to the iGrafx Mining API
    * @throws ConfigException If the Task KafkaLoggingEventsProperties are missing
    */
  private[usecases] def handleCreateAndSendCsvToIGrafx(
      properties: Properties,
      csvProperties: CsvProperties,
      kafkaLoggingEventsPropertiesOpt: Option[KafkaLoggingEventsProperties],
      lines: Iterable[Event],
      filePath: Path,
      aggregationInformation: DebugInformation
  ): Future[Int] = {
    log.info(
      s"Creation of the file $filePath to send to iGrafx data coming from Kafka, from offset ${aggregationInformation.offsetFrom} to offset ${aggregationInformation.offsetTo} for the partition ${aggregationInformation.partition} from the ${aggregationInformation.topic} Kafka topic"
        .replaceAll("[\r\n]", "")
    )
    createAndSendCsvToIGrafx(
      properties = properties,
      csvProperties = csvProperties,
      filePath = filePath,
      lines = lines
    ).recoverWith {
      case exception: LogEventException =>
        checkAndSendIssuePushFileEventIfLogging(
          filePath = filePath,
          lines = lines,
          exception = exception,
          properties = properties,
          kafkaLoggingEventsPropertiesOpt = kafkaLoggingEventsPropertiesOpt,
          aggregationInformation = aggregationInformation
        ).map { _ =>
          throw exception
        }
      case exception: Throwable =>
        log.error(
          s"[IGrafxSinkTask.put] Unexpected exception while trying to create, fill and send the file $filePath to the iGrafx API"
            .replaceAll("[\r\n]", ""),
          exception
        )
        checkAndSendIssuePushFileEventIfLogging(
          filePath = filePath,
          lines = lines,
          exception = exception,
          properties = properties,
          kafkaLoggingEventsPropertiesOpt = kafkaLoggingEventsPropertiesOpt,
          aggregationInformation = aggregationInformation
        ).map(throw exception)
    }
  }

  /** Method used to both initialized the project paths and delete the old archived files
    *
    * @param properties The Task Properties
    *
    * @throws ProjectPathInitializationException If there is an issue during the initialization of the project path
    * @throws OldArchivedFilesDeletionException If there is an issue during the deletion of old archived files
    */
  private[usecases] def initializeProjectPathAndDeleteOldArchivedFiles(properties: Properties): Future[Unit] = {
    (for {
      _ <- initProjectPaths(properties.projectId, properties.connectorName)
      _ <- deleteOldArchivedFiles(
        properties.projectId,
        properties.connectorName,
        properties.retentionTimeInDay
      )
    } yield ()) recover {
      case exception: ProjectPathInitializationException =>
        throw exception
      case exception: OldArchivedFilesDeletionException =>
        throw exception
      case exception: Throwable =>
        log.error(
          "[IGrafxSinkTask.put] Unexpected exception during the initialization of the project paths or during the deletion of old archived files",
          exception
        )
        throw exception
    }
  }

  /** Method used to both fill a new file with the values coming from Kafka and send the file to the iGrafx Mining API
    *
    * @param properties The Task Properties
    * @param csvProperties The Task CsvProperties
    * @param filename The name of the file
    * @param lines The Collection of SinkRecord added to the file
    *
    * @return The number of lines written in the file
    *
    * @throws FileCreationException Issue during the creation or the writing of the file storing data
    * @throws InvalidTokenException Couldn't retrieve a Token from iGrafx because of an Authentication or Server Issue
    * @throws SendFileException Couldn't send the file to the iGrafx Mining API
    */
  private[usecases] def createAndSendCsvToIGrafx(
      properties: Properties,
      csvProperties: CsvProperties,
      filePath: Path,
      lines: Iterable[Event]
  ): Future[Int] = {
    (for {
      lineNumber <- appendToCsv(csvProperties, filePath, lines)
      _ <- sendCsvToIGrafx(properties, filePath)
    } yield lineNumber) recover {
      case exception: FileCreationException =>
        log.error(
          "Because of an issue during the creation of the file storing data, Task is going to stop, please restart it when the problem is solved"
        )
        throw exception
      case exception: InvalidTokenException =>
        if (exception.canRetry) {
          log.error(
            "Couldn't send the file because of a Server issue during Authentication. Task is going to stop, please restart it when the problem is solved"
          )
        } else {
          log.error(
            "Couldn't send the file because of an Authentication issue, Task is going to stop, please restart it when the problem is solved or change the connector configuration"
          )
        }
        throw exception
      case exception: SendFileException =>
        if (exception.canRetry) {
          log.error(
            "Couldn't send the file to the iGrafx Mining API because of a Server issue. Task is going to stop, please restart it when the problem is solved"
          )
        } else {
          log.error(
            "Couldn't send the file to the iGrafx Mining API. Task is going to stop, please restart it when the problem is solved"
          )
        }
        throw exception
      case exception =>
        log.error(
          "Issue in the sequence of operations to send the file to the iGrafx Mining API. Task is going to stop, please restart it when the problem is solved",
          exception
        )
        throw exception
    }
  }

  /** Method used to create a pushFile event and to send it to Kafka
    *
    * @param filename The name of the file
    * @param lines The Collection of SinkRecord added to the file
    * @param lineNumber The number of lines written in the file
    * @param properties The Task Properties
    * @param kafkaLoggingEventsProperties The Task KafkaLoggingEventsProperties
    *
    * @throws AvroRuntimeException If there is an issue between the avro Schema to use and the value of the record we want to send
    * @throws InterruptException Issue while sending a record to Kafka or while flushing the producer
    * @throws IllegalStateException send method : if a transactional id has been configured and no transaction has been started, or when send is invoked after producer has been closed.
    * @throws KafkaException If the creation or the closing of the KafkaProducer failed
    */
  private[usecases] def sendPushFileEventToKafka(
      filePath: Path,
      lines: Iterable[Event],
      lineNumber: Int,
      properties: Properties,
      kafkaLoggingEventsProperties: KafkaLoggingEventsProperties,
      aggregationInformation: DebugInformation
  ): Future[Unit] = {
    val newFileSentEvent =
      createPushFileEvent(
        filePath = filePath,
        lines = lines,
        lineNumber = lineNumber,
        properties = properties,
        aggregationInformation = aggregationInformation
      )
    mainKafkaLoggingEvents.sendEventToKafka(
      kafkaLoggingEventsProperties = kafkaLoggingEventsProperties,
      kafkaLoggedEvent = newFileSentEvent
    )
  }

  /** Method used to create an issuePushFile event and to send it to Kafka
    *
    * @param filePath The path to the file
    * @param lines The Collection of SinkRecord added to the file
    * @param exception The exception threw while trying to create or send the file
    * @param properties The Task Properties
    * @param kafkaLoggingEventsProperties The Task KafkaLoggingEventsProperties
    *
    * @throws AvroRuntimeException If there is an issue between the avro Schema to use and the value of the record we want to send
    * @throws InterruptException Issue while sending a record to Kafka or while flushing the producer
    * @throws IllegalStateException send method : if a transactional id has been configured and no transaction has been started, or when send is invoked after producer has been closed.
    * @throws KafkaException If the creation or the closing of the KafkaProducer failed
    */
  private[usecases] def sendIssuePushFileEventToKafka(
      filePath: Path,
      lines: Iterable[Event],
      exception: Throwable,
      properties: Properties,
      kafkaLoggingEventsProperties: KafkaLoggingEventsProperties,
      aggregationInformation: DebugInformation
  ): Future[Unit] = {
    val newIssueFileSentEvent =
      createIssuePushFileEvent(
        filePath = filePath,
        lines = lines,
        exception = exception,
        properties = properties,
        aggregationInformation = aggregationInformation
      )
    mainKafkaLoggingEvents.sendEventToKafka(
      kafkaLoggingEventsProperties = kafkaLoggingEventsProperties,
      kafkaLoggedEvent = newIssueFileSentEvent
    )
  }

  /** Method used to generate a new file name
    *
    * @param csvProperties The Task CsvProperties
    *
    * @return The new filename
    */
  private[usecases] def generateFileNameAndPath(csvProperties: CsvProperties): Path = {
    val uuid: UUID = UUID.randomUUID()
    val timestamp: String = (System.currentTimeMillis() / 1000).toString
    val sanitizedFileName = FileUtils.sanitizeFileName(csvProperties.connectorName) match {
      case Left(exception) => throw exception
      case Right(value) => value
    }
    Paths.get(
      s"${MainConfig.csvPath}/${csvProperties.projectId}/$sanitizedFileName/archive/data_${uuid}_$timestamp.csv"
    )
  }

  // --------------------- Utility Methods ---------------------

  @throws[FileCreationException]
  private def appendToCsv(csvProperties: CsvProperties, filePath: Path, lines: Iterable[Event]): Future[Int] = {
    mainSystem.appendToCsv(csvProperties, filePath, lines)
  }

  @throws[InvalidTokenException]
  @throws[SendFileException]
  private def sendCsvToIGrafx(properties: Properties, filePath: Path): Future[Unit] = {
    mainApi.sendCsvToIGrafx(properties = properties, archivedFile = filePath.toFile)
  }

  @throws[ProjectPathInitializationException]
  private def initProjectPaths(projectId: UUID, connectorName: String): Future[Unit] = {
    mainSystem.initProjectPaths(projectId, connectorName)
  }

  @throws[OldArchivedFilesDeletionException]
  private def deleteOldArchivedFiles(projectId: UUID, connectorName: String, retentionTimeInDay: Int): Future[Unit] = {
    mainSystem.deleteOldArchivedFiles(
      projectId = projectId,
      connectorName = connectorName,
      retentionTime = retentionTimeInDay
    )
  }

  /** Method used to create a new pushFile event
    *
    * @param filePath The path to the file
    * @param lines The Collection of SinkRecord added to the file
    * @param lineNumber The number of lines written in the file
    * @param properties the Task Properties
    *
    * @return The created KafkaLoggedEvent corresponding to a pushFile event
    */
  private[usecases] def createPushFileEvent(
      filePath: Path,
      lines: Iterable[Event],
      lineNumber: Int,
      properties: Properties,
      aggregationInformation: DebugInformation
  ): KafkaLoggedEvent = {
    val sendDate = System.currentTimeMillis()

    KafkaLoggedEvent(
      eventType = "pushFile",
      igrafxProject = properties.projectId.toString,
      eventDate = sendDate,
      eventSequenceId = getEventSequenceId(aggregationInformation = aggregationInformation, properties = properties),
      payload = KafkaEventPayloadPushFile(
        filePath = filePath,
        date = sendDate,
        lineNumber = lineNumber
      )
    )
  }

  /** Method used to create a new issuePushFile event
    *
    * @param filePath The path to the file
    * @param lines The Collection of SinkRecord added to the file
    * @param properties the Task Properties
    *
    * @return The created KafkaLoggedEvent corresponding to a pushFile event
    */
  private[usecases] def createIssuePushFileEvent(
      filePath: Path,
      lines: Iterable[Event],
      exception: Throwable,
      properties: Properties,
      aggregationInformation: DebugInformation
  ): KafkaLoggedEvent = {
    val issueDate = System.currentTimeMillis()

    KafkaLoggedEvent(
      eventType = "issuePushFile",
      igrafxProject = properties.projectId.toString,
      eventDate = issueDate,
      eventSequenceId = getEventSequenceId(aggregationInformation = aggregationInformation, properties = properties),
      payload = KafkaEventPayloadIssuePushFile(
        filePath = filePath,
        date = issueDate,
        exceptionType = exception.getClass.getCanonicalName
      )
    )
  }

  /** Method used to get an event sequence ID according to the data's topic/partition/offset and to the connector's name
    *
    * @param aggregationInformation The information about the current aggregation of a partition
    * @param properties The Task Properties
    *
    * @return The event sequence ID
    */
  private[usecases] def getEventSequenceId(aggregationInformation: DebugInformation, properties: Properties): String = {
    val sequenceId =
      s"${aggregationInformation.topic}_${aggregationInformation.partition}_from_${aggregationInformation.offsetFrom}_to_${aggregationInformation.offsetTo}_${properties.connectorName}"
    val finalSequenceId = UUID.nameUUIDFromBytes(sequenceId.getBytes(StandardCharsets.UTF_8)).toString
    log.debug(
      s"[KAFKA LOG EVENT] New event with an eventSequenceId equal to $finalSequenceId for the following data : $sequenceId"
        .replaceAll("[\r\n]", "")
    )

    finalSequenceId
  }

  /** Method used to send an issuePushFile event to The Kafka event logging topic if the logging is enabled for the connector
    * Important : This method doesn't throw its exceptions as it is called when the connector is already going to an error state because of an exception that occurred during th creation or the sending of the file
    *
    * @param filePath The path to the file
    * @param lines The Collection of SinkRecord added to the file
    * @param exception The exception threw while trying to create or send the file
    * @param properties The Task Properties
    * @param kafkaLoggingEventsPropertiesOpt Option of the Task Kafka Logging properties
    *
    * @throws ConfigException If the Task KafkaLoggingEventsProperties are missing
    */
  private[usecases] def checkAndSendIssuePushFileEventIfLogging(
      filePath: Path,
      lines: Iterable[Event],
      exception: Throwable,
      properties: Properties,
      kafkaLoggingEventsPropertiesOpt: Option[KafkaLoggingEventsProperties],
      aggregationInformation: DebugInformation
  ): Future[Unit] = {
    if (properties.isLogging) {
      val kafkaLoggingEventsProperties: KafkaLoggingEventsProperties = getKafkaLoggingEventsProperties(
        kafkaLoggingEventsPropertiesOpt
      )
      (for {
        _ <- sendIssuePushFileEventToKafka(
          filePath = filePath,
          lines = lines,
          exception = exception,
          properties = properties,
          kafkaLoggingEventsProperties = kafkaLoggingEventsProperties,
          aggregationInformation = aggregationInformation
        )
      } yield ()) recover {
        case _: AvroRuntimeException => ()
        case _: InterruptException => ()
        case _: IllegalStateException => ()
        case _: KafkaException => ()
        case exception =>
          log.error(
            s"Unexpected issue with the external call logging an issuePushFile event to the ${kafkaLoggingEventsProperties.topic} Kafka topic"
              .replaceAll("[\r\n]", ""),
            exception
          )
      }
    } else {
      Future.successful(())
    }
  }

  /** Method used to send a pushFile event to The Kafka event logging topic if the logging is enabled for the connector
    *
    * @param filePath The path to the file
    * @param lines The Collection of SinkRecord added to the file
    * @param lineNumber The number of lines written in the file
    * @param properties The Task Properties
    * @param kafkaLoggingEventsPropertiesOpt Option of the Task Kafka Logging properties
    *
    * @throws AvroRuntimeException If there is an issue between the avro Schema to use and the value of the record we want to send
    * @throws InterruptException Issue while sending a record to Kafka or while flushing the producer
    * @throws IllegalStateException send method : if a transactional id has been configured and no transaction has been started, or when send is invoked after producer has been closed.
    * @throws KafkaException If the creation or the closing of the KafkaProducer failed
    * @throws ConfigException If the Task KafkaLoggingEventsProperties are missing
    */
  private[usecases] def checkAndSendPushFileEventIfLogging(
      filePath: Path,
      lines: Iterable[Event],
      lineNumber: Int,
      properties: Properties,
      kafkaLoggingEventsPropertiesOpt: Option[KafkaLoggingEventsProperties],
      aggregationInformation: DebugInformation
  ): Future[Unit] = {
    if (properties.isLogging) {
      val kafkaLoggingEventsProperties: KafkaLoggingEventsProperties = getKafkaLoggingEventsProperties(
        kafkaLoggingEventsPropertiesOpt
      )
      (for {
        _ <- {
          sendPushFileEventToKafka(
            filePath = filePath,
            lines = lines,
            lineNumber = lineNumber,
            properties = properties,
            kafkaLoggingEventsProperties = kafkaLoggingEventsProperties,
            aggregationInformation = aggregationInformation
          )
        }
      } yield ()).recover {
        case exception: AvroRuntimeException => throw exception
        case exception: InterruptException => throw exception
        case exception: IllegalStateException => throw exception
        case exception: KafkaException => throw exception
        case exception =>
          log.error(
            s"Unexpected issue with the external call logging a pushFile event to the ${kafkaLoggingEventsProperties.topic} Kafka topic"
              .replaceAll("[\r\n]", ""),
            exception
          )
          throw exception
      }
    } else {
      Future.successful(())
    }
  }

  /** Method used to retrieve the Task KafkaLoggingEventsProperties
    *
    * @param kafkaLoggingEventsPropertiesOpt Option of the Task Kafka Logging properties
    *
    * @throws ConfigException If the Task KafkaLoggingEventsProperties are missing
    */
  private def getKafkaLoggingEventsProperties(
      kafkaLoggingEventsPropertiesOpt: Option[KafkaLoggingEventsProperties]
  ): KafkaLoggingEventsProperties = {
    kafkaLoggingEventsPropertiesOpt match {
      case Some(kafkaLoggingEventsProperties) => kafkaLoggingEventsProperties
      case None =>
        log.error("[IGrafxSinkTask.put] ConfigException : Missing Task's Kafka events logging properties")
        throw new ConfigException("Missing Task's Kafka events logging properties")
    }
  }
}
