package com.igrafx.kafka.sink.aggregation.domain.usecases

import com.igrafx.kafka.sink.aggregation.adapters.services.{KafkaProducerSendImpl, KafkaTopicGetConfigurationImpl}
import com.igrafx.kafka.sink.aggregation.domain.entities.{
  CommitOffsetMaps,
  DebugInformation,
  PartitionTracker,
  TaskProperties
}
import com.igrafx.kafka.sink.aggregation.domain.exceptions.{AggregationException, SendRecordException}
import com.igrafx.kafka.sink.aggregation.domain.usecases.interfaces.{KafkaProducerSend, KafkaTopicGetConfiguration}
import com.igrafx.utils.AvroUtils
import org.apache.kafka.clients.consumer.OffsetAndMetadata
import org.apache.kafka.common.{KafkaException, TopicPartition}
import org.apache.kafka.connect.errors.{ConnectException, DataException}
import org.apache.kafka.connect.sink.SinkRecord
import org.slf4j.Logger

import java.util
import scala.jdk.CollectionConverters._
import scala.util.{Failure, Success, Try}

protected[aggregation] class TaskUseCases(private val log: Logger) {
  private[usecases] val kafkaProducerSend: KafkaProducerSend = new KafkaProducerSendImpl(log)
  private[usecases] val kafkaTopicGetConfiguration: KafkaTopicGetConfiguration =
    new KafkaTopicGetConfigurationImpl

  /** Method used to retrieve the value of the retention.ms configuration of the Kafka topic
    *
    * @param topic The name of the Kafka topic
    * @param bootstrapServers The List of Kafka brokers
    *
    * @throws NumberFormatException If the value retrieved for retention.ms couldn't be parsed to Long
    * @throws NoSuchElementException Either impossible to retrieve any configuration or the topic or impossible to retrieve the retention.ms configuration
    * @throws KafkaException Issue with the KafkaAdminClient
    */
  def getTopicRetentionMs(topic: String, bootstrapServers: String): Long = {
    kafkaTopicGetConfiguration.getTopicRetentionMs(topic, bootstrapServers, log)
  }

  /** Method used to determine a name for a given partition from a given topic
    *
    * @param topic The Kafka topic
    * @param partition The Kafka partition related to the topic
    */
  def getNameIndex(topic: String, partition: Int): String = {
    s"${topic}_$partition"
  }

  /** This method processes the new data coming from kafka and, if needed, flushes the current aggregation for one or more partition
    *
    * @param collection Collection of SinkRecord, each SinkRecord corresponds to one message coming from a Kafka topic/partition
    * @param properties The properties of the AggregationSinkTask
    * @param partitionTrackerMap The current partitionTrackerMap maintaining for each partition the latest processed, flushed and committed offset, along with the current aggregation, the value of the previous flush time of the partition and information about the retention time of the partition and the earliest timestamp belonging to an aggregated value    *
    * @return The updated partitionTrackerMap
    *
    * @throws DataException If there is an issue between the value and its schema
    * @throws ConnectException Partition hasn't been initialized in the open function of the Sink Task
    * @throws SendRecordException Issue while trying to flush a record to Kafka
    */
  def aggregateAndSendCollection(
      collection: util.Collection[SinkRecord],
      properties: TaskProperties,
      partitionTrackerMap: Map[String, PartitionTracker]
  ): Map[String, PartitionTracker] = {
    if (!collection.isEmpty) {
      log.debug(s"[PUT] New data to aggregate")
      collection.asScala.toSeq.foldLeft(partitionTrackerMap) {
        case (partitionTrackerMapAcc: Map[String, PartitionTracker], record: SinkRecord) =>
          Try {
            val nameIndex = getNameIndex(topic = record.topic(), partition = record.kafkaPartition())

            val currentPartitionTracker = getPartitionTrackerFromNameIndex(
              nameIndex = nameIndex,
              partitionTrackerMap = partitionTrackerMapAcc,
              topic = record.topic(),
              partition = record.kafkaPartition()
            )

            val processedPartition = processPartition(partitionTracker = currentPartitionTracker, record = record)
            val partitionTrackerMapProcessUpdate: Map[String, PartitionTracker] =
              partitionTrackerMapAcc + (nameIndex -> processedPartition)

            debugPartitionTrackerPrints(partitionTracker = processedPartition, tag = "[PROCESSED]")

            flushPartitionIfValuePatternOrElementNumberThresholdVerified(
              processedPartition = processedPartition,
              partitionTrackerMap = partitionTrackerMapProcessUpdate,
              properties = properties,
              record = record
            )
          } match {
            case Success(updatedPartitionTrackerMap) => updatedPartitionTrackerMap
            case Failure(exception) => throw AggregationException(exception, partitionTrackerMapAcc)
          }
      }
    } else {
      partitionTrackerMap
    }
  }

  /** Method used to check for each partition if the timeout threshold or the retention time is reached
    *
    * @param properties The properties of the AggregationSinkTask
    * @param partitionTrackerMap The current partitionTrackerMap maintaining for each partition the latest processed, flushed and committed offset, along with the current aggregation, the value of the previous flush time of the partition and information about the retention time of the partition and the earliest timestamp belonging to an aggregated value
    *
    * @return The updated partitionTrackerMap
    *
    * @throws AggregationException Issue while trying to flush a partition
    */
  def checkPartitionsForTimeoutOrRetention(
      properties: TaskProperties,
      partitionTrackerMap: Map[String, PartitionTracker]
  ): Map[String, PartitionTracker] = {
    // check all PartitionTracker to see if the timeout threshold or the retention time is reached for one (ore more) of them
    partitionTrackerMap.values.foldLeft(partitionTrackerMap) {
      case (partitionTrackerMapAcc: Map[String, PartitionTracker], partitionTracker: PartitionTracker) =>
        Try {
          flushPartitionIfTimeoutThresholdOrRetentionVerified(partitionTracker, partitionTrackerMapAcc, properties)
        } match {
          case Success(updatedPartitionTrackerMap) => updatedPartitionTrackerMap
          case Failure(exception) => throw AggregationException(exception, partitionTrackerMapAcc)
        }
    }
  }

  /** Method used to commit the offsets of the newly flushed data
    *
    * @param currentOffsets The current map of offsets as of the last call to AggregationSinkTask.put
    * @param partitionTrackerMap The current partitionTrackerMap
    *
    * @return a CommitOffsetMaps containing a map with the offsets to commit for each concerned partition, and an updated partitionTrackerMap
    */
  def commitOffsets(
      currentOffsets: util.Map[TopicPartition, OffsetAndMetadata],
      partitionTrackerMap: Map[String, PartitionTracker]
  ): CommitOffsetMaps = {
    currentOffsets.asScala.toMap.foldLeft(
      CommitOffsetMaps(
        updatedOffsetsMap = Map[TopicPartition, OffsetAndMetadata](),
        partitionTrackerMap = partitionTrackerMap
      )
    ) {
      case (acc: CommitOffsetMaps, (topicPartition: TopicPartition, _: OffsetAndMetadata)) =>
        val nameIndex = getNameIndex(topic = topicPartition.topic(), partition = topicPartition.partition())
        val partitionTracker: PartitionTracker = acc.partitionTrackerMap.get(nameIndex) match {
          case Some(partitionTracker) => partitionTracker
          case None =>
            log.error(
              s"[COMMIT] No entry for the requested partition ${topicPartition.partition()} of the ${topicPartition.topic()} topic, should have been initialized by OPEN function"
                .replaceAll("[\r\n]", "")
            )
            throw new ConnectException(
              s"No entry for the requested partition ${topicPartition.partition()} of the ${topicPartition.topic()} topic, should have been initialized by OPEN function"
                .replaceAll("[\r\n]", "")
            )
        }
        val committedPartitionOption = commitPartition(partitionTracker)
        committedPartitionOption match {
          case Some(committedPartition) =>
            val partitionTrackerMapCommitUpdate =
              acc.partitionTrackerMap + (committedPartition.nameIndex -> committedPartition)
            debugPartitionTrackerPrints(partitionTracker = committedPartition, tag = "[PRECOMMIT]")
            val updatedOffsetsMap =
              acc.updatedOffsetsMap + (topicPartition -> new OffsetAndMetadata(committedPartition.committedOffset + 1))
            //For the consumer, it looks like we need to commit the offset for the next message it will read, and not
            //the offset of the last processed message, for a given partition

            CommitOffsetMaps(
              updatedOffsetsMap = updatedOffsetsMap,
              partitionTrackerMap = partitionTrackerMapCommitUpdate
            )
          case None =>
            acc
        }
    }
  }

  // ---------------- CHECK THRESHOLDS ----------------

  /** Method used to check if the value pattern threshold or the element number threshold are verified. If it is the case, the newly processed partition is flushed
    *
    * @param processedPartition The newly processed partition
    * @param partitionTrackerMap The current partitionTrackerMap
    * @param properties The properties of the AggregationSinkTask
    * @param record The current record received from Kafka
    *
    * @return The updated partitionTrackerMap
    *
    * @throws SendRecordException Issue while trying to flush a record to Kafka
    */
  private[usecases] def flushPartitionIfValuePatternOrElementNumberThresholdVerified(
      processedPartition: PartitionTracker,
      partitionTrackerMap: Map[String, PartitionTracker],
      properties: TaskProperties,
      record: SinkRecord
  ): Map[String, PartitionTracker] = {
    // check if the value pattern threshold is reached
    if (
      properties.valuePatternThreshold.nonEmpty && record.value().toString.matches(properties.valuePatternThreshold)
    ) {
      log.debug(
        s"[Value Pattern Threshold {topic : ${processedPartition.debugInformation.topic}, partition : ${processedPartition.debugInformation.partition}, offset(s) ${processedPartition.debugInformation.offsetFrom} to ${processedPartition.debugInformation.offsetTo}}] value Pattern threshold verified !"
          .replaceAll("[\r\n]", "")
      )
      flushPartition(
        partitionTracker = processedPartition,
        topicOut = properties.topicOut,
        aggregationColumnName = properties.aggregationColumnName,
        bootstrapServers = properties.bootstrapServers,
        schemaRegistryUrl = properties.schemaRegistryUrl,
        maxMessageBytes = properties.maxMessageBytes,
        partitionTrackerMap = partitionTrackerMap
      )
    }
    // check if the element number threshold is reached
    else if (
      processedPartition.processedOffset - processedPartition.flushedOffset >= properties.elementNumberThreshold
    ) {
      log.debug(
        s"[Number of Elements Threshold {topic : ${processedPartition.debugInformation.topic}, partition : ${processedPartition.debugInformation.partition}, offset(s) ${processedPartition.debugInformation.offsetFrom} to ${processedPartition.debugInformation.offsetTo}}] Number of element threshold reached !"
          .replaceAll("[\r\n]", "")
      )
      flushPartition(
        partitionTracker = processedPartition,
        topicOut = properties.topicOut,
        aggregationColumnName = properties.aggregationColumnName,
        bootstrapServers = properties.bootstrapServers,
        schemaRegistryUrl = properties.schemaRegistryUrl,
        maxMessageBytes = properties.maxMessageBytes,
        partitionTrackerMap = partitionTrackerMap
      )
    } else {
      partitionTrackerMap
    }
  }

  /** Method used to check if the timeout threshold or the retention duration of one record is verified. If it is the case, the newly processed partition is flushed
    *
    * @param partitionTracker The partition to check
    * @param partitionTrackerMap The current partitionTrackerMap
    * @param properties The properties of the AggregationSinkTask
    *
    * @return The updated partitionTrackerMap
    *
    * @throws SendRecordException Issue while trying to flush a record to Kafka
    */
  private[usecases] def flushPartitionIfTimeoutThresholdOrRetentionVerified(
      partitionTracker: PartitionTracker,
      partitionTrackerMap: Map[String, PartitionTracker],
      properties: TaskProperties
  ): Map[String, PartitionTracker] = {
    if (shouldFlushPartition(partitionTracker = partitionTracker, flushTime = properties.timeoutInSecondsThreshold)) { // if the timeout threshold is reached
      log.debug(
        s"[Timeout Threshold {topic : ${partitionTracker.debugInformation.topic}, partition : ${partitionTracker.debugInformation.partition}, offset(s) ${partitionTracker.debugInformation.offsetFrom} to ${partitionTracker.debugInformation.offsetTo}}] Timeout threshold reached for ${partitionTracker.nameIndex} !"
          .replaceAll("[\r\n]", "")
      )
      // Time based flushing
      flushPartition(
        partitionTracker = partitionTracker,
        topicOut = properties.topicOut,
        aggregationColumnName = properties.aggregationColumnName,
        bootstrapServers = properties.bootstrapServers,
        schemaRegistryUrl = properties.schemaRegistryUrl,
        maxMessageBytes = properties.maxMessageBytes,
        partitionTrackerMap = partitionTrackerMap
      )
    } else if (
      shouldFlushPartitionBecauseOfRetention(partitionTracker.earliestRecordTimestamp, partitionTracker.retentionTime)
    ) { // If the retention time of an aggregated record becomes too close
      log.info(
        s"[Retention {topic : ${partitionTracker.debugInformation.topic}, partition : ${partitionTracker.debugInformation.partition}, offset(s) ${partitionTracker.debugInformation.offsetFrom} to ${partitionTracker.debugInformation.offsetTo}}] One of the data being aggregated has a timestamp of ${partitionTracker.earliestRecordTimestamp}. Its current life duration in the topic is getting too close from the retention time in its Kafka topic. hence, the current aggregation of the partition is sent to prevent a loss of data in case of a crash of the connector"
          .replaceAll("[\r\n]", "")
      )
      // retention based flushing
      flushPartition(
        partitionTracker = partitionTracker,
        topicOut = properties.topicOut,
        aggregationColumnName = properties.aggregationColumnName,
        bootstrapServers = properties.bootstrapServers,
        schemaRegistryUrl = properties.schemaRegistryUrl,
        maxMessageBytes = properties.maxMessageBytes,
        partitionTrackerMap = partitionTrackerMap
      )
    } else {
      partitionTrackerMap
    }
  }

  /** Method checking if the timeout threshold is reached or not
    *
    * @param partitionTracker The PartitionTracker we want to check
    * @param flushTime If the duration between the current time and the previous flush for the given partitionTracker is greater than the flushTime (seconds), then a new flush has to happen (timeout threshold)
    */
  private[usecases] def shouldFlushPartition(partitionTracker: PartitionTracker, flushTime: Long): Boolean = {
    (System.currentTimeMillis() - partitionTracker.previousFlushTimeStamp) >= (flushTime * 1000)
  }

  /** Method checking if the retention duration approaches for the record with the earliest timestamp of a partition
    *
    * @param earliestTimestamp The value of the earliest timestamp
    * @param retentionDuration The value of the Kafka topic's retention duration
    */
  private[usecases] def shouldFlushPartitionBecauseOfRetention(
      earliestTimestamp: Long,
      retentionDuration: Long
  ): Boolean = {
    (System.currentTimeMillis() - earliestTimestamp) >= (retentionDuration * 0.8).toLong
  }

  // ---------------- PROCESS ----------------

  /** Method used to process the sink record coming from Kafka and update the related partitionTracker
    *
    * @param partitionTracker The PartitionTracker to update
    * @param record The Sink Record coming from a Kafka topic
    *
    * @return a new PartitionTracker up to date after a record process
    *
    * @throws DataException If there is an issue between the value and its schema
    */
  private[aggregation] def processPartition(
      partitionTracker: PartitionTracker,
      record: SinkRecord
  ): PartitionTracker = {
    val avroValue: AnyRef = fromConnectToAvro(record)
    val newEarliestTimestamp: Long = Math.min(partitionTracker.earliestRecordTimestamp, record.timestamp)

    if (partitionTracker.processedOffset == -1) {
      val previousOffset = record.kafkaOffset() - 1
      PartitionTracker(
        nameIndex = partitionTracker.nameIndex,
        processedOffset = record.kafkaOffset(),
        flushedOffset = previousOffset,
        committedOffset = previousOffset,
        pendingValue = partitionTracker.pendingValue :+ avroValue,
        previousFlushTimeStamp = System.currentTimeMillis(),
        earliestRecordTimestamp = newEarliestTimestamp,
        retentionTime = partitionTracker.retentionTime,
        debugInformation = DebugInformation(
          topic = partitionTracker.debugInformation.topic,
          partition = partitionTracker.debugInformation.partition,
          offsetFrom = record.kafkaOffset(),
          offsetTo = record.kafkaOffset()
        )
      )
    } else {
      PartitionTracker(
        nameIndex = partitionTracker.nameIndex,
        processedOffset = record.kafkaOffset(),
        flushedOffset = partitionTracker.flushedOffset,
        committedOffset = partitionTracker.committedOffset,
        pendingValue = partitionTracker.pendingValue :+ avroValue,
        previousFlushTimeStamp = partitionTracker.previousFlushTimeStamp,
        earliestRecordTimestamp = newEarliestTimestamp,
        retentionTime = partitionTracker.retentionTime,
        debugInformation = DebugInformation(
          topic = partitionTracker.debugInformation.topic,
          partition = partitionTracker.debugInformation.partition,
          offsetFrom = partitionTracker.flushedOffset + 1,
          offsetTo = record.kafkaOffset()
        )
      )
    }
  }

  // ---------------- FLUSH ----------------

  /** Method used to flush messages to Kafka
    *
    * @param partitionTracker The PartitionTracker containing the values to flush
    * @param topicOut The Kafka topic to which the aggregation result is sent
    * @param aggregationColumnName The name of the Column storing the aggregation result in ksqlDB
    * @param bootstrapServers The List of Kafka brokers
    * @param schemaRegistryUrl The url of the Kafka Schema Registry
    * @param partitionTrackerMap The current partitionTrackerMap
    *
    * @return The updated partitionTrackerMap
    *
    * @throws SendRecordException Issue while trying to flush a record to Kafka
    */
  private[usecases] def flushPartition(
      partitionTracker: PartitionTracker,
      topicOut: String,
      aggregationColumnName: String,
      bootstrapServers: String,
      schemaRegistryUrl: String,
      maxMessageBytes: Int,
      partitionTrackerMap: Map[String, PartitionTracker]
  ): Map[String, PartitionTracker] = {
    val flushedPartition = flushPartition(
      partitionTracker,
      topicOut,
      aggregationColumnName,
      bootstrapServers,
      schemaRegistryUrl,
      maxMessageBytes
    )
    val partitionTrackerMapFlushUpdate = partitionTrackerMap + (flushedPartition.nameIndex -> flushedPartition)
    debugPartitionTrackerPrints(partitionTracker = flushedPartition, tag = "[FLUSHED]")

    partitionTrackerMapFlushUpdate
  }

  /** Method used to flush the current aggregated messages to Kafka
    *
    * @param partitionTracker The PartitionTracker containing the values to flush
    * @param topicOut The Kafka topic to which the aggregation result is sent
    * @param aggregationColumnName The name of the Column storing the aggregation result in ksqlDB
    * @param bootstrapServers The List of Kafka brokers
    * @param schemaRegistryUrl The url of the Kafka Schema Registry
    *
    * @return a new PartitionTracker up to date after a flush
    *
    * @throws SendRecordException Issue while trying to flush a record to Kafka
    */
  private[aggregation] def flushPartition(
      partitionTracker: PartitionTracker,
      topicOut: String,
      aggregationColumnName: String,
      bootstrapServers: String,
      schemaRegistryUrl: String,
      maxMessageBytes: Int
  ): PartitionTracker = {
    log.debug("[FLUSH PARTITION START]")
    val newPartitionTracker: PartitionTracker =
      if (partitionTracker.processedOffset > partitionTracker.flushedOffset) {
        log.debug(
          s"[FLUSH PARTITION] Flushing the new offsets from offset ${partitionTracker.debugInformation.offsetFrom} to offset ${partitionTracker.debugInformation.offsetTo} for the partition ${partitionTracker.debugInformation.partition} from the ${partitionTracker.debugInformation.topic} Kafka topic"
            .replaceAll("[\r\n]", "")
        )

        Try {
          kafkaProducerSend.sendRecord(
            partitionTracker,
            topicOut,
            aggregationColumnName,
            bootstrapServers,
            schemaRegistryUrl,
            maxMessageBytes
          )
        } match {
          case Success(_) => ()
          case Failure(exception: Throwable) =>
            log.error(
              s"[FLUSH PARTITION {topic : ${partitionTracker.debugInformation.topic}, partition : ${partitionTracker.debugInformation.partition}, offset(s) ${partitionTracker.debugInformation.offsetFrom} to ${partitionTracker.debugInformation.offsetTo}}] Couldn't send the current aggregation to the $topicOut Kafka topic"
                .replaceAll("[\r\n]", ""),
              exception
            )
            throw SendRecordException(exception)
        }

        PartitionTracker(
          nameIndex = partitionTracker.nameIndex,
          processedOffset = partitionTracker.processedOffset,
          flushedOffset = partitionTracker.processedOffset,
          committedOffset = partitionTracker.committedOffset,
          pendingValue = Seq.empty,
          previousFlushTimeStamp = System.currentTimeMillis(),
          earliestRecordTimestamp = Long.MaxValue,
          retentionTime = partitionTracker.retentionTime,
          debugInformation = DebugInformation(
            topic = partitionTracker.debugInformation.topic,
            partition = partitionTracker.debugInformation.partition,
            offsetFrom = -1,
            offsetTo = -1
          )
        )
      } else {
        log.debug(
          s"[FLUSH PARTITION] Timeout threshold reached but there is no new offset for the partition ${partitionTracker.debugInformation.partition} from the ${partitionTracker.debugInformation.topic} Kafka topic"
            .replaceAll("[\r\n]", "")
        )
        PartitionTracker(
          nameIndex = partitionTracker.nameIndex,
          processedOffset = partitionTracker.processedOffset,
          flushedOffset = partitionTracker.flushedOffset,
          committedOffset = partitionTracker.committedOffset,
          pendingValue = partitionTracker.pendingValue,
          previousFlushTimeStamp = System.currentTimeMillis(),
          earliestRecordTimestamp = partitionTracker.earliestRecordTimestamp,
          retentionTime = partitionTracker.retentionTime,
          debugInformation = partitionTracker.debugInformation
        )
      }
    log.debug("[FLUSH PARTITION END]")

    newPartitionTracker
  }

  // ---------------- COMMIT ----------------

  /** Method used to check if a commit can be done
    *
    * @param partitionTracker The PartitionTracker whose offsets we want to check
    *
    * @return a new PartitionTracker up to date with a commit of its offsets
    */
  private[aggregation] def commitPartition(partitionTracker: PartitionTracker): Option[PartitionTracker] = {
    val oldCommittedOffset = partitionTracker.committedOffset
    val newCommittedOffset = partitionTracker.flushedOffset
    if (newCommittedOffset > oldCommittedOffset) {
      log.debug(s"[PRECOMMIT] New offset(s) to commit for ${partitionTracker.nameIndex} !".replaceAll("[\r\n]", ""))
      log.debug(
        s"[PRECOMMIT] oldCommittedOffset = $oldCommittedOffset and newCommittedOffset = $newCommittedOffset"
          .replaceAll("[\r\n]", "")
      )

      Some(
        PartitionTracker(
          nameIndex = partitionTracker.nameIndex,
          processedOffset = partitionTracker.processedOffset,
          flushedOffset = partitionTracker.flushedOffset,
          committedOffset = newCommittedOffset,
          pendingValue = partitionTracker.pendingValue,
          previousFlushTimeStamp = partitionTracker.previousFlushTimeStamp,
          earliestRecordTimestamp = partitionTracker.earliestRecordTimestamp,
          retentionTime = partitionTracker.retentionTime,
          debugInformation = partitionTracker.debugInformation
        )
      )
    } else {
      log.debug(s"[PRECOMMIT] No new offset to commit for ${partitionTracker.nameIndex}".replaceAll("[\r\n]", ""))
      None
    }
  }

  // ---------------- OTHER ----------------

  /** Method used to transform a Connect data (the record's value) to an avro Data
    *
    * @param record The record coming from Kafka
    *
    * @throws DataException If there is an issue between the record's value and its schema
    */
  private[usecases] def fromConnectToAvro(record: SinkRecord): AnyRef = {
    AvroUtils.fromConnectToAvro(record, log)
  }

  /** Method used to retrieve a PartitionTracker from a Map
    *
    * @param nameIndex The nameIndex corresponding to the key of the partition in the Map
    * @param partitionTrackerMap The Map containing for each partition its PartitionTracker
    * @param topic The name of the Kafka topic
    * @param partition The number of the partition
    *
    * @return The PartitionTracker related to the partition
    *
    * @throws ConnectException Partition hasn't been initialized in the open function of the Sink Task
    */
  private def getPartitionTrackerFromNameIndex(
      nameIndex: String,
      partitionTrackerMap: Map[String, PartitionTracker],
      topic: String,
      partition: Int
  ): PartitionTracker = {
    partitionTrackerMap.get(nameIndex) match {
      case Some(partitionTracker) => partitionTracker
      case None =>
        log.error(
          s"[AggregationSinkTask.aggregateAndSendCollection] Topic: $topic Partition: $partition hasn't been initialized by OPEN function"
            .replaceAll("[\r\n]", "")
        )
        throw new ConnectException(
          s"Topic: $topic Partition: $partition hasn't been initialized by OPEN function".replaceAll("[\r\n]", "")
        )
    }
  }

  private def debugPartitionTrackerPrints(partitionTracker: PartitionTracker, tag: String): Unit = {
    // uncomment this to have information about any PartitionTracker changes, this will add a lot of text in the console
    /*log.debug(s"""
         | $tag PartitionTracker.nameIndex : ${partitionTracker.nameIndex.replaceAll("[\r\n]", "")}
         | $tag PartitionTracker.processedOffset = ${partitionTracker.processedOffset}
         | $tag PartitionTracker.flushedOffset = ${partitionTracker.flushedOffset}
         | $tag PartitionTracker.committedOffset = ${partitionTracker.committedOffset}
         | $tag PartitionTracker.previousFlushTimeStamp = ${partitionTracker.previousFlushTimeStamp}
         | $tag PartitionTracker.earliestRecordTimestamp = ${partitionTracker.earliestRecordTimestamp}
         | $tag PartitionTracker.retentionTime = ${partitionTracker.retentionTime}
         |""".stripMargin)*/
  }
}
