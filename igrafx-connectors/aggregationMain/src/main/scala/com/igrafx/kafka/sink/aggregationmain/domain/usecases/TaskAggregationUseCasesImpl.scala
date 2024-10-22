package com.igrafx.kafka.sink.aggregationmain.domain.usecases

import com.igrafx.kafka.sink.aggregationmain.Constants
import com.igrafx.kafka.sink.aggregationmain.adapters.services.KafkaTopicGetConfigurationImpl
import com.igrafx.kafka.sink.aggregationmain.domain.dtos.EventDto
import com.igrafx.kafka.sink.aggregationmain.domain.entities._
import com.igrafx.kafka.sink.aggregationmain.domain.exceptions.{AggregationException, SendAggregationResultException}
import com.igrafx.kafka.sink.aggregationmain.domain.interfaces.KafkaTopicGetConfiguration
import com.igrafx.utils.AvroUtils
import com.sksamuel.avro4s.{Avro4sDecodingException, RecordFormat}
import org.apache.avro.generic.GenericData
import org.apache.avro.{AvroRuntimeException, Schema, SchemaBuilder}
import org.apache.kafka.clients.consumer.OffsetAndMetadata
import org.apache.kafka.common.{KafkaException, TopicPartition}
import org.apache.kafka.connect.errors.{ConnectException, DataException}
import org.apache.kafka.connect.sink.SinkRecord
import org.slf4j.{Logger, LoggerFactory}

import java.util
import scala.concurrent.Await
import scala.concurrent.duration.DurationInt
import scala.jdk.CollectionConverters._
import scala.util.{Failure, Success, Try}

protected[usecases] class TaskAggregationUseCasesImpl extends TaskAggregationUseCases {
  private implicit val log: Logger = LoggerFactory.getLogger(classOf[TaskAggregationUseCasesImpl])

  private[usecases] val taskFileUseCases: TaskFileUseCases = TaskFileUseCases.instance
  private[usecases] val kafkaTopicGetConfiguration: KafkaTopicGetConfiguration =
    new KafkaTopicGetConfigurationImpl

  private val validateSchema: Schema = getValidSchema

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
    * @throws SendAggregationResultException Issue while trying to flush a record to Kafka
    */
  def aggregateAndSendCollection(
      collection: util.Collection[SinkRecord],
      properties: Properties,
      csvProperties: CsvProperties,
      kafkaLoggingEventsPropertiesOpt: Option[KafkaLoggingEventsProperties],
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
              csvProperties = csvProperties,
              kafkaLoggingEventsPropertiesOpt = kafkaLoggingEventsPropertiesOpt,
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
      properties: Properties,
      csvProperties: CsvProperties,
      kafkaLoggingEventsPropertiesOpt: Option[KafkaLoggingEventsProperties],
      partitionTrackerMap: Map[String, PartitionTracker]
  ): Map[String, PartitionTracker] = {
    // check all PartitionTracker to see if the timeout threshold or the retention time is reached for one (ore more) of them
    partitionTrackerMap.values.foldLeft(partitionTrackerMap) {
      case (partitionTrackerMapAcc: Map[String, PartitionTracker], partitionTracker: PartitionTracker) =>
        Try {
          flushPartitionIfTimeoutThresholdOrRetentionVerified(
            partitionTracker,
            partitionTrackerMapAcc,
            properties,
            csvProperties,
            kafkaLoggingEventsPropertiesOpt
          )
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
    * @throws SendAggregationResultException Issue while trying to flush a record to Kafka
    */
  private[usecases] def flushPartitionIfValuePatternOrElementNumberThresholdVerified(
      processedPartition: PartitionTracker,
      partitionTrackerMap: Map[String, PartitionTracker],
      properties: Properties,
      csvProperties: CsvProperties,
      kafkaLoggingEventsPropertiesOpt: Option[KafkaLoggingEventsProperties],
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
        partitionTrackerMap = partitionTrackerMap,
        properties = properties,
        csvProperties = csvProperties,
        kafkaLoggingEventsPropertiesOpt = kafkaLoggingEventsPropertiesOpt
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
        partitionTrackerMap = partitionTrackerMap,
        properties = properties,
        csvProperties = csvProperties,
        kafkaLoggingEventsPropertiesOpt = kafkaLoggingEventsPropertiesOpt
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
    */
  private[usecases] def flushPartitionIfTimeoutThresholdOrRetentionVerified(
      partitionTracker: PartitionTracker,
      partitionTrackerMap: Map[String, PartitionTracker],
      properties: Properties,
      csvProperties: CsvProperties,
      kafkaLoggingEventsPropertiesOpt: Option[KafkaLoggingEventsProperties]
  ): Map[String, PartitionTracker] = {
    if (shouldFlushPartition(partitionTracker = partitionTracker, flushTime = properties.timeoutInSecondsThreshold)) { // if the timeout threshold is reached
      log.debug(
        s"[Timeout Threshold {topic : ${partitionTracker.debugInformation.topic}, partition : ${partitionTracker.debugInformation.partition}, offset(s) ${partitionTracker.debugInformation.offsetFrom} to ${partitionTracker.debugInformation.offsetTo}}] Timeout threshold reached for ${partitionTracker.nameIndex} !"
          .replaceAll("[\r\n]", "")
      )
      // Time based flushing
      flushPartition(
        partitionTracker = partitionTracker,
        partitionTrackerMap = partitionTrackerMap,
        properties = properties,
        csvProperties = csvProperties,
        kafkaLoggingEventsPropertiesOpt = kafkaLoggingEventsPropertiesOpt
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
        partitionTrackerMap = partitionTrackerMap,
        properties = properties,
        csvProperties = csvProperties,
        kafkaLoggingEventsPropertiesOpt = kafkaLoggingEventsPropertiesOpt
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
    * @throws DataException if there is a problem between the record's value and its schema
    * @throws AvroRuntimeException if the avro record is incoherent with the awaited avro schema
    * @throws Avro4sDecodingException if it's not possible to transform the avro record to a EventDto case class
    * @throws IllegalArgumentException if it's not possible to transform an EventDto to an Event because of an empty Option */
  private[aggregationmain] def processPartition(
      partitionTracker: PartitionTracker,
      record: SinkRecord
  ): PartitionTracker = {
    val event: Event = getParams(record)
    val newEarliestTimestamp: Long = Math.min(partitionTracker.earliestRecordTimestamp, record.timestamp)

    log.debug(
      s"New record from ${record.topic()} partition ${record.kafkaPartition()} offset ${record.kafkaOffset()} added to Partition Tracker"
        .replaceAll("[\r\n]", "")
    )
    if (partitionTracker.processedOffset == -1) {
      val previousOffset = record.kafkaOffset() - 1
      PartitionTracker(
        nameIndex = partitionTracker.nameIndex,
        processedOffset = record.kafkaOffset(),
        flushedOffset = previousOffset,
        committedOffset = previousOffset,
        pendingValue = partitionTracker.pendingValue :+ event,
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
        pendingValue = partitionTracker.pendingValue :+ event,
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

  /** Method used to retrieve an event (one line with caseId, activity, time..., here represented by a List[Param]) from an avro record coming from a Kafka topic
    *
    * Example :
    *
    * If in input we have this kind of avro record (represented here in JSON) :
    * "{"DATAARRAY":
    *       [{"QUOTE":true, "TEXT":"activity1", "COLUMNID":1},
    *        {"QUOTE":false, "TEXT":"caseId1", "COLUMNID":0},
    *        {"QUOTE":false, "TEXT":"endDate1", "COLUMNID":3}]
    *   }"
    *
    *   The method will create the following List[Param] (which is an Event) :
    *
    *   List(Param(QUOTE = true, TEXT = "activity1", COLUMNID = 1), Param(QUOTE = false, TEXT = "caseId1", COLUMNID = 0), Param(QUOTE = false, TEXT = "endDate1", COLUMNID = 3))
    *
    * @param record The avro record coming from a kafka topic and containing the list of events
    *
    * @throws DataException from the fromConnectToAvro method, if there is a problem between the record's value and its schema
    * @throws AvroRuntimeException from the validateRecord method, if the avro record is incoherent with the awaited avro schema
    * @throws Avro4sDecodingException from the RecordFormat.from method, if it's not possible to transform the avro record to an EventDto case class
    * @throws IllegalArgumentException from the RecordParamDto.toRecordParam method, if it's not possible to transform an EventDto to an Event because of an empty Option
    */
  def getParams(record: SinkRecord): Event = {
    val kafkaRecordInformation = KafkaRecordInformation(
      topic = record.topic(),
      partition = record.kafkaPartition().toString,
      offset = record.kafkaOffset().toString
    )

    val event: Event = Try[Event] {
      val avroRecord: GenericData.Record = AvroUtils
        .fromConnectToAvro(record, log)
        .asInstanceOf[GenericData.Record]

      validateRecord(avroRecord = avroRecord, schema = validateSchema, kafkaRecordInformation = kafkaRecordInformation)

      val format = RecordFormat[EventDto]
      val eventDto = format.from(record = avroRecord)
      eventDto.toEvent
    } match {
      case Success(event) => event
      case Failure(exception: DataException) => throw exception
      case Failure(exception: AvroRuntimeException) => throw exception
      case Failure(exception: Avro4sDecodingException) =>
        log.error(
          s"Issue while transforming the avro record coming from Kafka to the EventDto case class. Kafka record information : topic ${kafkaRecordInformation.topic}, partition ${kafkaRecordInformation.partition}, offset ${kafkaRecordInformation.offset}"
            .replaceAll("[\r\n]", ""),
          exception
        )
        throw exception
      case Failure(exception: IllegalArgumentException) =>
        log.error(
          s"Issue while transforming an EventDto to an Event. The connector doesn't accept empty options. Kafka record information : topic ${kafkaRecordInformation.topic}, partition ${kafkaRecordInformation.partition}, offset ${kafkaRecordInformation.offset}"
            .replaceAll("[\r\n]", ""),
          exception
        )
        throw exception
      case Failure(exception: Throwable) =>
        log.error(
          s"Unexpected exception, Kafka record information : topic ${kafkaRecordInformation.topic}, partition ${kafkaRecordInformation.partition}, offset ${kafkaRecordInformation.offset}"
            .replaceAll("[\r\n]", ""),
          exception
        )
        throw exception
    }

    event
  }

  /** Method validating that the avroRecord coming from Kafka is correct according to the validating schema
    *
    * @param avroRecord The avro record to check
    * @param schema The awaited schema
    *
    * @throws AvroRuntimeException If the avro record is incoherent with the awaited schema
    */
  private def validateRecord(
      avroRecord: GenericData.Record,
      schema: Schema,
      kafkaRecordInformation: KafkaRecordInformation
  ): Unit = {
    val genericData = new GenericData()
    if (!genericData.validate(schema, avroRecord)) {
      log.error(
        s"[validateRecord] The awaited schema is incoherent with the avro record coming from the ${kafkaRecordInformation.topic} Kafka topic, partition : ${kafkaRecordInformation.partition}, offset : ${kafkaRecordInformation.offset}\n Awaited schema : $schema\n Record schema : ${avroRecord.getSchema}"
          .replaceAll("[\r\n]", "")
      )
      throw new AvroRuntimeException(
        s"The awaited schema is incoherent with the avro record coming from the ${kafkaRecordInformation.topic} Kafka topic, partition : ${kafkaRecordInformation.partition}, offset : ${kafkaRecordInformation.offset}"
          .replaceAll("[\r\n]", "")
      )
    }
  }

  /** Method creating the schema used to validate that the incoming records are valid according to what is awaited by the connector
    *
    * @return The schema used for validation
    */
  private def getValidSchema: Schema = {
    //format: off
    SchemaBuilder
      .record("validate_schema").fields()
        .name("DATAARRAY").`type`()
          .unionOf().nullType().and().array().items()
            .record("KsqlDataSourceSchema_DATAARRAY").fields()
              .name("COLUMNID").`type`().unionOf().nullType().and().intType().endUnion().nullDefault()
              .name("TEXT").`type`().unionOf().nullType().and().stringType().endUnion().nullDefault()
              .name("QUOTE").`type`().unionOf().nullType().and().booleanType().endUnion().nullDefault()
            .endRecord()
          .endUnion()
        .nullDefault()
      .endRecord()
    //format: on
  }

  // ---------------- FLUSH ----------------

  /** Method used to flush messages to Kafka
    *
    * @param partitionTracker The PartitionTracker containing the values to flush
    * @param partitionTrackerMap The current partitionTrackerMap
    *
    * @return The updated partitionTrackerMap
    */
  private[usecases] def flushPartition(
      partitionTracker: PartitionTracker,
      partitionTrackerMap: Map[String, PartitionTracker],
      properties: Properties,
      csvProperties: CsvProperties,
      kafkaLoggingEventsPropertiesOpt: Option[KafkaLoggingEventsProperties]
  ): Map[String, PartitionTracker] = {
    val flushedPartition = flushPartition(partitionTracker, properties, csvProperties, kafkaLoggingEventsPropertiesOpt)
    val partitionTrackerMapFlushUpdate = partitionTrackerMap + (flushedPartition.nameIndex -> flushedPartition)
    debugPartitionTrackerPrints(partitionTracker = flushedPartition, tag = "[FLUSHED]")

    partitionTrackerMapFlushUpdate
  }

  /** Method used to flush the current aggregated messages to Kafka
    *
    * @param partitionTracker The PartitionTracker containing the values to flush
    *
    * @return a new PartitionTracker up to date after a flush
    *
    * @throws SendAggregationResultException If the current aggregation for a partition couldn't be sent to iGrafx
    */
  private[aggregationmain] def flushPartition(
      partitionTracker: PartitionTracker,
      properties: Properties,
      csvProperties: CsvProperties,
      kafkaLoggingEventsPropertiesOpt: Option[KafkaLoggingEventsProperties]
  ): PartitionTracker = {
    log.debug("[FLUSH PARTITION START]")
    val newPartitionTracker: PartitionTracker =
      if (partitionTracker.processedOffset > partitionTracker.flushedOffset) {
        log.debug(
          s"[FLUSH PARTITION] Flushing the new offsets (sending the aggregation to iGrafx Mining API) from offset ${partitionTracker.debugInformation.offsetFrom} to offset ${partitionTracker.debugInformation.offsetTo} for the partition ${partitionTracker.debugInformation.partition} from the ${partitionTracker.debugInformation.topic} Kafka topic"
            .replaceAll("[\r\n]", "")
        )

        val sendToIGrafxFuture = taskFileUseCases
          .initializeDeleteAndSendNewDataToIGrafx(
            lines = partitionTracker.pendingValue,
            properties = properties,
            csvProperties = csvProperties,
            kafkaLoggingEventsPropertiesOpt = kafkaLoggingEventsPropertiesOpt,
            aggregationInformation = partitionTracker.debugInformation
          )

        Try { Await.result(sendToIGrafxFuture, Constants.timeoutFutureValueInSeconds seconds) } match {
          case Success(_) => ()
          case Failure(exception: concurrent.TimeoutException) =>
            log.error(
              s"[FLUSH PARTITION {topic : ${partitionTracker.debugInformation.topic}, partition : ${partitionTracker.debugInformation.partition}, offset(s) ${partitionTracker.debugInformation.offsetFrom} to ${partitionTracker.debugInformation.offsetTo}}] Timeout exceeded while waiting for the end of the external calls. Couldn't send the aggregation to the iGrafx Mining API"
                .replaceAll("[\r\n]", ""),
              exception
            )
            throw SendAggregationResultException(exception)
          case Failure(exception: InterruptedException) =>
            log.error(
              s"[FLUSH PARTITION {topic : ${partitionTracker.debugInformation.topic}, partition : ${partitionTracker.debugInformation.partition}, offset(s) ${partitionTracker.debugInformation.offsetFrom} to ${partitionTracker.debugInformation.offsetTo}}] Interrupted while waiting for the end of the external calls.  Couldn't send the aggregation to the iGrafx Mining API"
                .replaceAll("[\r\n]", ""),
              exception
            )
            throw SendAggregationResultException(exception)
          case Failure(exception) =>
            log.error(
              s"[FLUSH PARTITION {topic : ${partitionTracker.debugInformation.topic}, partition : ${partitionTracker.debugInformation.partition}, offset(s) ${partitionTracker.debugInformation.offsetFrom} to ${partitionTracker.debugInformation.offsetTo}}] Task is going to stop because of the following ${exception.getClass.getCanonicalName} exception : ${exception.getMessage}\n Couldn't send the aggregation to the iGrafx Mining API"
                .replaceAll("[\r\n]", "")
            )
            throw SendAggregationResultException(exception)
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
  private[aggregationmain] def commitPartition(partitionTracker: PartitionTracker): Option[PartitionTracker] = {
    val oldCommittedOffset = partitionTracker.committedOffset
    val newCommittedOffset = partitionTracker.flushedOffset
    if (newCommittedOffset > oldCommittedOffset) {
      log.debug(s"[PRECOMMIT] New offset(s) to commit for ${partitionTracker.nameIndex} !".replaceAll("[\r\n]", ""))
      log.debug(s"[PRECOMMIT] oldCommittedOffset = $oldCommittedOffset and newCommittedOffset = $newCommittedOffset")

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
