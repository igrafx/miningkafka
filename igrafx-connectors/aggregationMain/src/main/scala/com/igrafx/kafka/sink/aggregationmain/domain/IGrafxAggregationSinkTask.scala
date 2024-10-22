package com.igrafx.kafka.sink.aggregationmain.domain

import com.igrafx.kafka.sink.aggregationmain.Constants
import com.igrafx.kafka.sink.aggregationmain.domain.dtos.EncodingEnumDto
import com.igrafx.kafka.sink.aggregationmain.domain.dtos.EncodingEnumDto._
import com.igrafx.kafka.sink.aggregationmain.domain.entities._
import com.igrafx.kafka.sink.aggregationmain.domain.enums.{ConnectorPropertiesEnum, EncodingEnum}
import com.igrafx.kafka.sink.aggregationmain.domain.exceptions.{AggregationException, SendAggregationResultException}
import com.igrafx.kafka.sink.aggregationmain.domain.usecases.TaskAggregationUseCases
import org.apache.kafka.clients.consumer.OffsetAndMetadata
import org.apache.kafka.common.config.ConfigException
import org.apache.kafka.common.{KafkaException, TopicPartition}
import org.apache.kafka.connect.errors.{ConnectException, DataException}
import org.apache.kafka.connect.sink.{SinkRecord, SinkTask}
import org.slf4j.{Logger, LoggerFactory}

import java.util
import java.util.UUID
import scala.collection.immutable.HashMap
import scala.jdk.CollectionConverters._
import scala.util.{Failure, Success, Try}

class IGrafxAggregationSinkTask extends SinkTask {
  private val log: Logger = LoggerFactory.getLogger(getClass)

  private[aggregationmain] val taskAggregationUseCases: TaskAggregationUseCases = TaskAggregationUseCases.instance

  private var properties: Option[Properties] = None
  private var csvProperties: Option[CsvProperties] = None
  private var kafkaLoggingEventsProperties: Option[KafkaLoggingEventsProperties] = None

  private var partitionTrackerMap: Map[String, PartitionTracker] = new HashMap[String, PartitionTracker]()

  /**  Method called when a Task is started
    * Warning : the "properties" and "csvProperties" class attribute are modified in this method
    *
    * @param map Corresponds to one of the Maps in the List returned by IGrafxSinkConnector.taskConfigs. The following keys need to be defined :
    * - name
    * - ConnectorPropertiesEnum.apiUrlProperty
    * - ConnectorPropertiesEnum.authUrlProperty
    * - ConnectorPropertiesEnum.workGroupIdProperty
    * - ConnectorPropertiesEnum.workGroupKeyProperty
    * - ConnectorPropertiesEnum.projectIdProperty, value needs to follow the UUID format
    * - ConnectorPropertiesEnum.csvEncodingProperty, value needs to match either UTF-8 or ASCII or ISO-8859-1
    * - ConnectorPropertiesEnum.csvSeparatorProperty, value.length needs to be equal to 1
    * - ConnectorPropertiesEnum.csvQuoteProperty, value.length needs to be equal to 1
    * - ConnectorPropertiesEnum.csvFieldsNumberProperty, value needs to be >= 3
    * - ConnectorPropertiesEnum.csvHeaderProperty
    * - ConnectorPropertiesEnum.csvDefaultTextValueProperty
    * - ConnectorPropertiesEnum.retentionTimeInDayProperty, value needs to be > 0
    * - ConnectorPropertiesEnum.kafkaLoggingEventsIsLoggingProperty
    * - ConnectorPropertiesEnum.elementNumberThresholdProperty, value needs to be >= 1
    * - ConnectorPropertiesEnum.valuePatternThresholdProperty
    * - ConnectorPropertiesEnum.timeoutInSecondsThresholdProperty, value needs to be >= 1
    * - ConnectorPropertiesEnum.bootstrapServersProperty
    * - Constants.schemaRegistryUrlProperty
    *
    * If ConnectorPropertiesEnum.csvHeaderProperty is true then Constants.headerValueProperty needs to be defined
    *
    * If ConnectorPropertiesEnum.kafkaLoggingEventsIsLoggingProperty is true then the following property needs to be defined :
    *
    * - ConnectorPropertiesEnum.kafkaLoggingEventsTopicProperty, value can't be empty
    *
    * @throws ConfigException at least one property is not valid, either missing or with a null value
    * @throws IllegalArgumentException The projectId property defined by the user does not suit UUID or a requirement in CsvProperties is not verified. Requirements are : csvFieldsNumber >= 3 && csvSeparator.length == 1 && csvQuote.length == 1
    */
  override def start(map: util.Map[String, String]): Unit = {
    log.debug(s"[IGrafxSinkTask.start] Starting Kafka Sink Task ${this.getClass.getName}")
    val connectorName = checkProperty(map.get("name"), "name")
    val projectId =
      checkProperty(
        map.get(ConnectorPropertiesEnum.projectIdProperty.toStringDescription),
        ConnectorPropertiesEnum.projectIdProperty.toStringDescription
      )
    val csvEncoding =
      checkProperty(
        map.get(ConnectorPropertiesEnum.csvEncodingProperty.toStringDescription),
        ConnectorPropertiesEnum.csvEncodingProperty.toStringDescription
      )
    val csvSeparator =
      checkProperty(
        map.get(ConnectorPropertiesEnum.csvSeparatorProperty.toStringDescription),
        ConnectorPropertiesEnum.csvSeparatorProperty.toStringDescription
      )
    val csvQuote =
      checkProperty(
        map.get(ConnectorPropertiesEnum.csvQuoteProperty.toStringDescription),
        ConnectorPropertiesEnum.csvQuoteProperty.toStringDescription
      )
    val csvFieldsNumberAsInt =
      checkProperty(
        map.get(ConnectorPropertiesEnum.csvFieldsNumberProperty.toStringDescription),
        ConnectorPropertiesEnum.csvFieldsNumberProperty.toStringDescription
      ).toInt
    val csvFieldsNumber = Try {
      ColumnsNumber(csvFieldsNumberAsInt)
    } match {
      case Success(columnsNumber: ColumnsNumber) => columnsNumber
      case Failure(exception) =>
        log.error(
          s"Issue with the ${ConnectorPropertiesEnum.csvFieldsNumberProperty.toStringDescription} property. The value is not greater or equals than ${Constants.minimumColumnsNumber}",
          exception
        )
        throw new ConfigException(
          s"Issue with the ${ConnectorPropertiesEnum.csvFieldsNumberProperty.toStringDescription} property. The value is not greater or equals than ${Constants.minimumColumnsNumber}",
          exception
        )
    }
    val csvHeader = checkProperty(
      map.get(ConnectorPropertiesEnum.csvHeaderProperty.toStringDescription),
      ConnectorPropertiesEnum.csvHeaderProperty.toStringDescription
    ).toBoolean
    val csvDefaultTextValue =
      checkProperty(
        map.get(ConnectorPropertiesEnum.csvDefaultTextValueProperty.toStringDescription),
        ConnectorPropertiesEnum.csvDefaultTextValueProperty.toStringDescription
      )
    val csvHeaderValue = if (csvHeader) {
      Some(
        checkProperty(
          map.get(Constants.headerValueProperty),
          Constants.headerValueProperty
        )
      )
    } else {
      Option.empty[String]
    }

    val encoding: EncodingEnum = EncodingEnumDto.getEncoding(csvEncoding, log).toEncodingEnum
    val projectUuid = Try {
      UUID.fromString(projectId)
    } match {
      case Success(projectUuid) => projectUuid
      case Failure(exception: IllegalArgumentException) =>
        log.error(
          s"Issue with the projectId format, not a UUID",
          exception
        )
        throw new IllegalArgumentException(
          "Issue with the projectId format, not a UUID",
          exception
        )
      case Failure(exception: Throwable) =>
        log.error("Unexpected exception while transforming the projectId from String to UUID", exception)
        throw exception
    }

    Try {
      csvProperties = Some(
        CsvProperties(
          connectorName,
          projectUuid,
          encoding,
          csvSeparator,
          csvQuote,
          csvFieldsNumber,
          csvHeader,
          csvDefaultTextValue,
          csvHeaderValue
        )
      )
    } match {
      case Success(_) => ()
      case Failure(exception) =>
        log.error(
          "Issue the CSV properties given to Tasks",
          exception
        )
        throw exception
    }

    val apiUrl = checkProperty(
      map.get(ConnectorPropertiesEnum.apiUrlProperty.toStringDescription),
      ConnectorPropertiesEnum.apiUrlProperty.toStringDescription
    )
    val authUrl = checkProperty(
      map.get(ConnectorPropertiesEnum.authUrlProperty.toStringDescription),
      ConnectorPropertiesEnum.authUrlProperty.toStringDescription
    )
    val workgroupId = checkProperty(
      map.get(ConnectorPropertiesEnum.workGroupIdProperty.toStringDescription),
      ConnectorPropertiesEnum.workGroupIdProperty.toStringDescription
    )
    val workgroupKey = checkProperty(
      map.get(ConnectorPropertiesEnum.workGroupKeyProperty.toStringDescription),
      ConnectorPropertiesEnum.workGroupKeyProperty.toStringDescription
    )
    val retentionTimeInDay = checkProperty(
      map.get(ConnectorPropertiesEnum.retentionTimeInDayProperty.toStringDescription),
      ConnectorPropertiesEnum.retentionTimeInDayProperty.toStringDescription
    ).toInt
    val isLogging = checkProperty(
      map.get(ConnectorPropertiesEnum.kafkaLoggingEventsIsLoggingProperty.toStringDescription),
      ConnectorPropertiesEnum.kafkaLoggingEventsIsLoggingProperty.toStringDescription
    ).toBoolean
    val elementNumberThresholdProperty = checkProperty(
      property = map.get(ConnectorPropertiesEnum.elementNumberThresholdProperty.toStringDescription),
      propertyName = ConnectorPropertiesEnum.elementNumberThresholdProperty.toStringDescription
    ).toInt
    val valuePatternThresholdProperty = checkProperty(
      property = map.get(ConnectorPropertiesEnum.valuePatternThresholdProperty.toStringDescription),
      propertyName = ConnectorPropertiesEnum.valuePatternThresholdProperty.toStringDescription
    )
    val timeoutInSecondsThresholdProperty = checkProperty(
      property = map.get(ConnectorPropertiesEnum.timeoutInSecondsThresholdProperty.toStringDescription),
      propertyName = ConnectorPropertiesEnum.timeoutInSecondsThresholdProperty.toStringDescription
    ).toInt
    val bootstrapServers = checkProperty(
      property = map.get(ConnectorPropertiesEnum.bootstrapServersProperty.toStringDescription),
      propertyName = ConnectorPropertiesEnum.bootstrapServersProperty.toStringDescription
    )
    val schemaRegistryUrl = checkProperty(
      property = map.get(Constants.schemaRegistryUrlProperty),
      propertyName = Constants.schemaRegistryUrlProperty
    )

    Try {
      properties = Some(
        Properties(
          connectorName,
          apiUrl,
          authUrl,
          workgroupId,
          workgroupKey,
          projectUuid,
          encoding,
          csvSeparator,
          csvQuote,
          csvFieldsNumber,
          csvHeader,
          csvDefaultTextValue,
          retentionTimeInDay,
          isLogging,
          elementNumberThresholdProperty,
          valuePatternThresholdProperty,
          timeoutInSecondsThresholdProperty,
          bootstrapServers,
          schemaRegistryUrl
        )
      )
    } match {
      case Success(_) => ()
      case Failure(exception) =>
        log.error(
          "Issue the properties given to Tasks",
          exception
        )
        throw exception
    }

    if (isLogging) {
      Try {
        val kafkaLoggingEventsTopic = checkProperty(
          map.get(ConnectorPropertiesEnum.kafkaLoggingEventsTopicProperty.toStringDescription),
          ConnectorPropertiesEnum.kafkaLoggingEventsTopicProperty.toStringDescription
        )
        kafkaLoggingEventsProperties = Some(
          KafkaLoggingEventsProperties(
            kafkaLoggingEventsTopic,
            bootstrapServers,
            schemaRegistryUrl
          )
        )
      } match {
        case Success(_) => ()
        case Failure(exception) =>
          log.error(
            "Issue the Kafka Logging Events properties given to Tasks",
            exception
          )
          throw exception
      }
    }
  }

  /** Method used to check that a property is defined
    *
    * @param property value of the property
    * @param propertyName name of the property
    *
    * @throws ConfigException if the property is not defined
    */
  private[domain] def checkProperty(property: String, propertyName: String): String = {
    Option(property) match {
      case Some(propertyValue) => propertyValue
      case None =>
        log.error(
          s"[IGrafxSinkTask.start] ConfigException : Property $propertyName is not defined for IGrafxSinkTask"
            .replaceAll("[\r\n]", "")
        )
        throw new ConfigException(
          s"Property $propertyName is not defined for IGrafxSinkTask"
        )
    }
  }

  /** Method called on Task startup or during Task rebalance
    *
    * @param partitions The list of all partitions that are now assigned to the task
    */
  override def open(partitions: util.Collection[TopicPartition]): Unit = {
    val properties: Properties = getProperties
    log.debug("[OPEN START]")
    Try {
      partitions.asScala.toSeq.foldLeft(Map[String, Long]()) {
        case (acc: Map[String, Long], partition: TopicPartition) =>
          val nameIndex =
            taskAggregationUseCases.getNameIndex(topic = partition.topic(), partition = partition.partition())
          val retentionTime = {
            if (acc.contains(partition.topic())) {
              acc.apply(partition.topic())
            } else {
              taskAggregationUseCases.getTopicRetentionMs(partition.topic(), properties.bootstrapServers)
            }
          }
          val debugInformation = DebugInformation(
            topic = partition.topic(),
            partition = partition.partition(),
            offsetFrom = -1,
            offsetTo = -1
          )

          partitionTrackerMap += (nameIndex -> PartitionTracker(
            nameIndex = nameIndex,
            processedOffset = -1,
            flushedOffset = -1,
            committedOffset = -1,
            pendingValue = Seq.empty,
            previousFlushTimeStamp = System.currentTimeMillis(),
            earliestRecordTimestamp = Long.MaxValue,
            retentionTime = retentionTime,
            debugInformation = debugInformation
          ))
          log.debug(s"[OPEN] New map key for $nameIndex".replaceAll("[\r\n]", ""))

          acc + (partition.topic() -> retentionTime)
      }
    } match {
      case Success(_) => ()
      case Failure(exception: ConfigException) => throw exception
      case Failure(exception: NumberFormatException) => throw exception
      case Failure(exception: NoSuchElementException) => throw exception
      case Failure(exception: KafkaException) => throw exception
      case Failure(exception: IllegalArgumentException) =>
        log.error("[OPEN] A requirement at the initialization of a PartitionTracker is not satisfied", exception)
        throw exception
      case Failure(exception: Throwable) =>
        log.error("[OPEN] Unexpected issue during the Task open function", exception)
        throw exception
    }
    log.debug("[OPEN END]")
  }

  /** Method called on Task shutdown or during Task rebalance
    *
    * @param partitions The list of all partitions that were assigned to the task
    */
  override def close(partitions: util.Collection[TopicPartition]): Unit = {
    log.debug("[CLOSE START]")
    log.debug("[CLOSE END]")
  }

  /**  put is a Method called repeatedly by the Task
    *
    * Warning : Method not pure as there is a dependency towards the mutable class attributes "properties" and "csvProperties"
    *
    * An exception thrown in this function will cause the Task to stop
    *
    * @param collection : Collection of SinkRecord, each SinkRecord corresponds to one message coming from the Kafka topic
    */
  override def put(collection: util.Collection[SinkRecord]): Unit = {
    log.debug("[IGrafxSinkTask.put] Put")
    val propertiesPut: Properties = getProperties
    val csvPropertiesPut: CsvProperties = getCsvProperties

    Try {
      partitionTrackerMap = taskAggregationUseCases.aggregateAndSendCollection(
        collection,
        propertiesPut,
        csvPropertiesPut,
        kafkaLoggingEventsProperties,
        partitionTrackerMap
      )
      partitionTrackerMap = taskAggregationUseCases.checkPartitionsForTimeoutOrRetention(
        propertiesPut,
        csvPropertiesPut,
        kafkaLoggingEventsProperties,
        partitionTrackerMap
      )
    } match {
      case Success(_) => ()
      case Failure(exception: AggregationException) =>
        partitionTrackerMap = exception.partitionTrackerMap
        exception.cause match {
          case exception: DataException => throw exception
          case exception: ConnectException => throw exception
          case exception: SendAggregationResultException => throw exception.cause
          case exception: Throwable =>
            log.error("[PUT] Unexpected exception while dealing with data to aggregate", exception)
            throw exception
        }
      case Failure(exception: Throwable) =>
        log.error("[PUT] Unexpected exception while dealing with data to aggregate", exception)
        throw exception
    }
  }

  /**  Method called when the Task stops */
  override def stop(): Unit = {
    log.debug(s"[IGrafxSinkTask.stop] Stopping Kafka Sink Task ${this.getClass.getName}")
  }

  override def version(): String = {
    new IGrafxAggregationSinkConnector().version()
  }

  /** Method used to commit the offset of the records that have been flushed
    *
    * @param currentOffsets The current map of offsets as of the last call to put
    *
    * @return a map with the new offsets to commit for the concerned TopicPartitions
    *
    * @throws ConnectException The partition related to the SinkRecord doesn't correspond to a initialized PartitionTask
    */
  override def preCommit(
      currentOffsets: util.Map[TopicPartition, OffsetAndMetadata]
  ): util.Map[TopicPartition, OffsetAndMetadata] = {
    log.debug("[PRECOMMIT START]")
    val commitOffsetMaps: CommitOffsetMaps = taskAggregationUseCases.commitOffsets(currentOffsets, partitionTrackerMap)
    partitionTrackerMap = commitOffsetMaps.partitionTrackerMap
    val committedOffsets: Map[TopicPartition, OffsetAndMetadata] = commitOffsetMaps.updatedOffsetsMap
    log.debug("[PRECOMMIT END]")

    committedOffsets.asJava
  }

  /** Method called right before committing the offsets of the last records managed by poll */
  override def flush(map: util.Map[TopicPartition, OffsetAndMetadata]): Unit = {}

  /** Method used to retrieve the Task Properties
    *
    * @throws ConfigException If the Task Properties are missing
    */
  private def getProperties: Properties = {
    properties match {
      case Some(properties) => properties
      case None =>
        log.error("[IGrafxSinkTask.put] ConfigException : Missing Task's properties")
        throw new ConfigException("Missing Task's properties")
    }
  }

  /** Method used to retrieve the Task CsvProperties
    *
    * @throws ConfigException If the Task CsvProperties are missing
    */
  private def getCsvProperties: CsvProperties = {
    csvProperties match {
      case Some(csvProperties) => csvProperties
      case None =>
        log.error("[IGrafxSinkTask.put] ConfigException : Missing Task's CSV properties")
        throw new ConfigException("Missing Task's CSV properties")
    }
  }
}
