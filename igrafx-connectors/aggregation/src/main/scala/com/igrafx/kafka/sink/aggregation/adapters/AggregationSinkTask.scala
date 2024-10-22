package com.igrafx.kafka.sink.aggregation.adapters

import com.igrafx.kafka.sink.aggregation.Constants
import com.igrafx.kafka.sink.aggregation.domain.entities.{
  CommitOffsetMaps,
  DebugInformation,
  PartitionTracker,
  TaskProperties
}
import com.igrafx.kafka.sink.aggregation.domain.enums.ConnectorPropertiesEnum
import com.igrafx.kafka.sink.aggregation.domain.exceptions.{
  AggregationException,
  MaxMessageBytesException,
  SendRecordException
}
import com.igrafx.kafka.sink.aggregation.domain.usecases.TaskUseCases
import org.apache.avro.AvroRuntimeException
import org.apache.kafka.clients.consumer.OffsetAndMetadata
import org.apache.kafka.common.{KafkaException, TopicPartition}
import org.apache.kafka.common.config.ConfigException
import org.apache.kafka.common.errors.{InterruptException, InvalidConfigurationException, SerializationException}
import org.apache.kafka.connect.errors.{ConnectException, DataException}
import org.apache.kafka.connect.sink.{SinkRecord, SinkTask}
import org.slf4j.{Logger, LoggerFactory}

import java.util
import scala.collection.immutable.HashMap
import scala.jdk.CollectionConverters._
import scala.util.{Failure, Success, Try}

class AggregationSinkTask extends SinkTask {
  private val log: Logger = LoggerFactory.getLogger(getClass)

  private var propertiesOption: Option[TaskProperties] = None
  private var partitionTrackerMap: Map[String, PartitionTracker] = new HashMap[String, PartitionTracker]()

  private[adapters] val taskUseCases: TaskUseCases = new TaskUseCases(log)

  /** Method called when a Task is started, performs configuration parsing
    * Warning : the "propertiesOption" are modified in this method
    *
    * @param map Corresponds to one the the Maps in the List returned by AggregationSinkConnector.taskConfigs. The following keys need to be defined :
    * - Constants.connectorNameProperty
    * - ConnectorPropertiesEnum.aggregationColumnNameProperty
    * - ConnectorPropertiesEnum.topicOutProperty
    * - ConnectorPropertiesEnum.elementNumberThresholdProperty
    * - ConnectorPropertiesEnum.valuePatternThresholdProperty
    * - ConnectorPropertiesEnum.timeoutInSecondsThresholdProperty
    * - ConnectorPropertiesEnum.bootstrapServersProperty
    * - Constants.schemaRegistryUrlProperty
    * - Constants.maxMessageBytesConfigurationName
    *
    * @throws ConnectException at least one property is not valid, either missing or with a null value
    * @throws IllegalArgumentException A requirement in Properties is not verified. Requirements are : topicOut.nonEmpty && elementNumberThreshold > 0 && timeoutInSecondsThreshold > 0
    */
  override def start(map: util.Map[String, String]): Unit = {
    log.debug("[START TASK]")
    log.info(s"[AggregationSinkTask.start] Starting Kafka Sink Task ${this.getClass.getName}")
    val connectorName =
      checkProperty(property = map.get(Constants.connectorNameProperty), propertyName = Constants.connectorNameProperty)
    val topicOut = checkProperty(
      property = map.get(ConnectorPropertiesEnum.topicOutProperty.toStringDescription),
      propertyName = ConnectorPropertiesEnum.topicOutProperty.toStringDescription
    )
    val aggregationColumnName = checkProperty(
      property = map.get(ConnectorPropertiesEnum.aggregationColumnNameProperty.toStringDescription),
      propertyName = ConnectorPropertiesEnum.aggregationColumnNameProperty.toStringDescription
    )
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
    val maxMessageBytes = checkProperty(
      property = map.get(Constants.maxMessageBytesConfigurationName),
      propertyName = Constants.maxMessageBytesConfigurationName
    ).toInt

    log.debug(
      s"[START TASK] properties : connectorName = $connectorName, topicOut = $topicOut, elementNumberThresholdProperty = $elementNumberThresholdProperty, timeoutInSecondsThresholdProperty = $timeoutInSecondsThresholdProperty, bootstrapServers = $bootstrapServers, schemaRegistryUrl = $schemaRegistryUrl, maxMessageBytes = $maxMessageBytes"
        .replaceAll("[\r\n]", "")
    )

    Try {
      propertiesOption = Some(
        TaskProperties(
          connectorName = connectorName,
          topicOut = topicOut,
          aggregationColumnName = aggregationColumnName,
          elementNumberThreshold = elementNumberThresholdProperty,
          valuePatternThreshold = valuePatternThresholdProperty,
          timeoutInSecondsThreshold = timeoutInSecondsThresholdProperty,
          bootstrapServers = bootstrapServers,
          schemaRegistryUrl = schemaRegistryUrl,
          maxMessageBytes = maxMessageBytes
        )
      )
    } match {
      case Success(_) => ()
      case Failure(exception) =>
        log.error("Issue with the values of the properties given to Tasks", exception)
        throw exception
    }
    log.debug("[START TASK END]")
  }

  /** Method called on Task startup or during Task rebalance
    *
    * @param partitions The list of all partitions that are now assigned to the task
    */
  override def open(partitions: util.Collection[TopicPartition]): Unit = {
    val properties: TaskProperties = getProperties
    log.debug("[OPEN START]")
    Try {
      partitions.asScala.toSeq.foldLeft(Map[String, Long]()) {
        case (acc: Map[String, Long], partition: TopicPartition) =>
          val nameIndex = taskUseCases.getNameIndex(topic = partition.topic(), partition = partition.partition())
          val retentionTime = {
            if (acc.contains(partition.topic())) {
              acc.apply(partition.topic())
            } else {
              taskUseCases.getTopicRetentionMs(partition.topic(), properties.bootstrapServers)
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

  /**  aggregateAndSendCollection is a Method called repeatedly by the Task
    *
    * Warning : Method not pure as there is a dependency towards the mutable class attribute "propertiesOption"
    *
    * An exception thrown in this function will cause the Task to stop
    *
    * @param collection : Collection of SinkRecord, each SinkRecord corresponds to one message coming from a Kafka topic/partition
    *
    * @throws ConfigException if the Task's properties are missing
    * @throws DataException If there is an issue between the value and its schema
    * @throws ConnectException Partition hasn't been initialized in the open function of the Sink Task
    * @throws MaxMessageBytesException Impossible to even send a message to Kafka with a single value
    * @throws InterruptException Issue while sending a record to Kafka or while flushing the producer
    * @throws IllegalStateException send method : if a transactional id has been configured and no transaction has been started, or when send is invoked after producer has been closed.
    * @throws KafkaException Problem while sending the record to Kafka or with the construction or the closing of the Kafka Producer
    * @throws SerializationException Issue while Serializing the record to retrieve its size
    * @throws InvalidConfigurationException Issue during serialization made to get the size of the record
    * @throws AvroRuntimeException Problem related to the schema
    */
  override def put(collection: util.Collection[SinkRecord]): Unit = {
    log.debug("[PUT START]")
    val properties: TaskProperties = getProperties

    Try {
      partitionTrackerMap = taskUseCases.aggregateAndSendCollection(collection, properties, partitionTrackerMap)
      partitionTrackerMap = taskUseCases.checkPartitionsForTimeoutOrRetention(properties, partitionTrackerMap)
    } match {
      case Success(_) => ()
      case Failure(exception: AggregationException) =>
        partitionTrackerMap = exception.partitionTrackerMap
        exception.cause match {
          case exception: DataException => throw exception
          case exception: ConnectException => throw exception
          case exception: SendRecordException => throw exception.cause
          case exception: Throwable =>
            log.error("[PUT] Unexpected exception while dealing with data to aggregate", exception)
            throw exception
        }
      case Failure(exception: Throwable) =>
        log.error("[PUT] Unexpected exception while dealing with data to aggregate", exception)
        throw exception
    }

    log.debug("[AggregationSinkTask PUT END]")
  }

  /**  Method called when the Task stops */
  override def stop(): Unit = {
    log.info(s"[AggregationSinkTask.stop] Stopping Kafka Sink Task ${this.getClass.getName}")
  }

  override def version(): String = {
    new AggregationSinkConnector().version()
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
    val commitOffsetMaps: CommitOffsetMaps = taskUseCases.commitOffsets(currentOffsets, partitionTrackerMap)
    partitionTrackerMap = commitOffsetMaps.partitionTrackerMap
    val committedOffsets: Map[TopicPartition, OffsetAndMetadata] = commitOffsetMaps.updatedOffsetsMap
    log.debug("[PRECOMMIT END]")

    committedOffsets.asJava
  }

  /** Method used to check that a property is defined
    *
    * @param property value of the property
    * @param propertyName name of the property
    *
    * @throws ConfigException if the property is not defined
    */
  private def checkProperty(property: String, propertyName: String): String = {
    Option(property) match {
      case Some(propertyValue) => propertyValue
      case None =>
        log.error(
          s"[AggregationSinkTask.start] ConfigException : Property $propertyName is not defined for AggregationSinkTask"
            .replaceAll("[\r\n]", "")
        )
        throw new ConfigException(
          s"Property $propertyName is not defined for AggregationSinkTask".replaceAll("[\r\n]", "")
        )
    }
  }

  /** Method used to retrieve the Task properties
    *
    * @throws ConfigException Missing Task's properties
    */
  private def getProperties: TaskProperties = {
    propertiesOption match {
      case Some(properties) => properties
      case None =>
        log.error("[IGrafxSinkTask.getProperties] ConfigException : Missing Task's properties")
        throw new ConfigException("Missing Task's properties")
    }
  }
}
