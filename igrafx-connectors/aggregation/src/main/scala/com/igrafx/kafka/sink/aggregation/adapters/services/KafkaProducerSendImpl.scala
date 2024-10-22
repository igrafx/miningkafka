package com.igrafx.kafka.sink.aggregation.adapters.services

import com.igrafx.kafka.sink.aggregation.Constants
import com.igrafx.kafka.sink.aggregation.adapters.services.producer.ProducerAggregation
import com.igrafx.kafka.sink.aggregation.domain.entities.{DebugInformation, PartitionTracker}
import com.igrafx.kafka.sink.aggregation.domain.exceptions.MaxMessageBytesException
import com.igrafx.kafka.sink.aggregation.domain.usecases.interfaces.KafkaProducerSend
import io.confluent.kafka.schemaregistry.client.CachedSchemaRegistryClient
import io.confluent.kafka.schemaregistry.client.rest.exceptions.RestClientException
import org.apache.avro.{AvroRuntimeException, JsonProperties, Schema}
import org.apache.avro.generic.{GenericData, GenericEnumSymbol, GenericFixed, GenericRecord, IndexedRecord}
import org.apache.kafka.clients.ApiVersions
import org.apache.kafka.clients.producer.{Producer, ProducerRecord}
import org.apache.kafka.common.KafkaException
import org.apache.kafka.common.errors.{InterruptException, InvalidConfigurationException, SerializationException}
import org.apache.kafka.common.record.{AbstractRecords, CompressionType}
import org.apache.kafka.common.serialization.StringSerializer
import org.slf4j.Logger

import java.io.IOException
import java.nio.ByteBuffer
import java.{lang, util}
import scala.util.{Failure, Success, Try, Using}
import scala.jdk.CollectionConverters._
import io.confluent.kafka.serializers.KafkaAvroSerializer

import scala.annotation.tailrec
import scala.collection.immutable.HashMap

class KafkaProducerSendImpl(private val log: Logger) extends KafkaProducerSend {

  /** Method sending the current aggregated values for a partition to Kafka
    *
    * @param partitionTracker The PartitionTracker containing the aggregated values to flush
    * @param topicOut The Kafka topic to which the aggregation result is sent
    * @param aggregationColumnName The name of the Column storing the aggregation result in ksqlDB
    * @param bootstrapServers The List of Kafka brokers
    * @param schemaRegistryUrl The url of the Kafka Schema Registry
    * @param maxMessageBytes The maximum size in bytes of a message sent to the topicOut Kafka topic, according to its configuration
    *
    * @throws MaxMessageBytesException Impossible to even send a message to Kafka with a single value
    * @throws InterruptException Issue while sending a record to Kafka or while flushing the producer
    * @throws IllegalStateException send method : if a transactional id has been configured and no transaction has been started, or when send is invoked after producer has been closed.
    * @throws KafkaException Problem while sending the record to Kafka or with the construction or the closing of the Kafka Producer
    * @throws SerializationException Issue while Serializing the record to retrieve its size
    * @throws InvalidConfigurationException Issue during serialization made to get the size of the record
    * @throws AvroRuntimeException Problem related to the schema
    */
  def sendRecord(
      partitionTracker: PartitionTracker,
      topicOut: String,
      aggregationColumnName: String,
      bootstrapServers: String,
      schemaRegistryUrl: String,
      maxMessageBytes: Int
  ): Unit = {
    val schema = getTopicSchema(
      topic = topicOut,
      schemaRegistryUrl = schemaRegistryUrl,
      debugInformation = partitionTracker.debugInformation
    )
    log.debug(
      s"[FLUSH PARTITION {topic : ${partitionTracker.debugInformation.topic}, partition : ${partitionTracker.debugInformation.partition}, offset(s) ${partitionTracker.debugInformation.offsetFrom} to ${partitionTracker.debugInformation.offsetTo}}] $topicOut Kafka topic's schema used to send the current aggregation : $schema"
        .replaceAll("[\r\n]", "")
    )

    Try {
      sendRecordRec(
        aggregationSeq = partitionTracker.pendingValue,
        schema = schema,
        nameIndex = partitionTracker.nameIndex,
        topicOut = topicOut,
        aggregationColumnName = aggregationColumnName,
        bootstrapServers = bootstrapServers,
        schemaRegistryUrl = schemaRegistryUrl,
        maxMessageBytes = maxMessageBytes,
        debugInformation = DebugInformation(
          topic = partitionTracker.debugInformation.topic,
          partition = partitionTracker.debugInformation.partition,
          offsetFrom = partitionTracker.processedOffset - partitionTracker.pendingValue.size + 1,
          offsetTo = partitionTracker.processedOffset
        )
      )
    } match {
      case Success(_) => ()
      case Failure(exception: MaxMessageBytesException) => throw exception
      case Failure(exception: SerializationException) => throw exception
      case Failure(exception: InvalidConfigurationException) => throw exception
      case Failure(exception: InterruptException) => throw exception
      case Failure(exception: IllegalStateException) => throw exception
      case Failure(exception: KafkaException) => throw exception
      case Failure(exception: AvroRuntimeException) =>
        log.error(
          s"[FLUSH PARTITION {topic : ${partitionTracker.debugInformation.topic}, partition : ${partitionTracker.debugInformation.partition}, offset(s) ${partitionTracker.debugInformation.offsetFrom} to ${partitionTracker.debugInformation.offsetTo}}] Issue related to a Schema problem, can't send the record to the $topicOut Kafka topic"
            .replaceAll("[\r\n]", ""),
          exception
        )
        throw exception
      case Failure(exception: Throwable) =>
        log.error(
          s"[FLUSH PARTITION {topic : ${partitionTracker.debugInformation.topic}, partition : ${partitionTracker.debugInformation.partition}, offset(s) ${partitionTracker.debugInformation.offsetFrom} to ${partitionTracker.debugInformation.offsetTo}}] Unexpected exception while trying to flush data to the $topicOut Kafka Topic"
            .replaceAll("[\r\n]", ""),
          exception
        )
        throw exception
    }
  }

  /** Recursive method used to send (in multiple messages if needed) the current aggregation for the partition
    *
    * @param aggregationSeq The Seq containing the aggregated values to flush
    * @param schema: The value schema of the topicOut Kafka topic
    * @param nameIndex: The name index related to the partition being flushed
    * @param topicOut The Kafka topic to which the aggregation result is sent
    * @param aggregationColumnName The name of the Column storing the aggregation result in ksqlDB
    * @param bootstrapServers The List of Kafka brokers
    * @param schemaRegistryUrl The url of the Kafka Schema Registry
    * @param maxMessageBytes The maximum size in bytes of a message sent to the topicOut Kafka topic, according to its configuration
    * @throws MaxMessageBytesException Impossible to even send a message to Kafka with a single value
    * @throws AvroRuntimeException Problem related to the schema
    * @throws InterruptException Issue while sending a record to Kafka or while flushing the producer
    * @throws IllegalStateException send method : if a transactional id has been configured and no transaction has been started, or when send is invoked after producer has been closed.
    * @throws KafkaException Problem while sending the record to Kafka or with the construction or the closing of the Kafka Producer
    * @throws SerializationException Issue while Serializing the record to retrieve its size
    * @throws InvalidConfigurationException Issue during serialization made to get the size of the record
    */
  @tailrec
  private def sendRecordRec(
      aggregationSeq: Seq[AnyRef],
      schema: Schema,
      nameIndex: String,
      topicOut: String,
      aggregationColumnName: String,
      bootstrapServers: String,
      schemaRegistryUrl: String,
      maxMessageBytes: Int,
      debugInformation: DebugInformation
  ): Unit = {
    if (aggregationSeq.isEmpty) {
      ()
    } else {
      val aggregationExtraSeq: Seq[AnyRef] = sendAggregationRec(
        aggregationSeq = aggregationSeq,
        numberOfElements = aggregationSeq.size,
        schema = schema,
        nameIndex = nameIndex,
        topicOut = topicOut,
        aggregationColumnName = aggregationColumnName,
        bootstrapServers = bootstrapServers,
        schemaRegistryUrl = schemaRegistryUrl,
        maxMessageBytes = maxMessageBytes,
        debugInformation = DebugInformation(
          topic = debugInformation.topic,
          partition = debugInformation.partition,
          offsetFrom = debugInformation.offsetTo - aggregationSeq.size + 1,
          offsetTo = debugInformation.offsetTo
        )
      )

      sendRecordRec(
        aggregationSeq = aggregationExtraSeq,
        schema = schema,
        nameIndex = nameIndex,
        topicOut = topicOut,
        aggregationColumnName = aggregationColumnName,
        bootstrapServers = bootstrapServers,
        schemaRegistryUrl = schemaRegistryUrl,
        maxMessageBytes = maxMessageBytes,
        debugInformation = debugInformation
      )
    }
  }

  /** Method sending the avro record containing the aggregation result to Kafka
    *
    * @param aggregationSeq The Seq containing the aggregated values to flush
    * @param numberOfElements: The number of aggregated elements in the aggregationSeq Seq
    * @param schema: The value schema of the topicOut Kafka topic
    * @param nameIndex: The name index related to the partition being flushed
    * @param topicOut The Kafka topic to which the aggregation result is sent
    * @param aggregationColumnName The name of the Column storing the aggregation result in ksqlDB
    * @param bootstrapServers The List of Kafka brokers
    * @param schemaRegistryUrl The url of the Kafka Schema Registry
    * @param maxMessageBytes The maximum size in bytes of a message sent to the topicOut Kafka topic, according to its configuration
    *
    * @return a Seq containing the elements that couldn't be sent because of the message size limit
    *
    * @throws AvroRuntimeException Problem related to the schema
    * @throws InterruptException Issue while sending a record to Kafka or while flushing the producer
    * @throws IllegalStateException send method : if a transactional id has been configured and no transaction has been started, or when send is invoked after producer has been closed.
    * @throws KafkaException Problem while sending the record to Kafka or with the construction or the closing of the Kafka Producer
    * @throws SerializationException Issue while Serializing the record to retrieve its size
    * @throws InvalidConfigurationException Issue during serialization made to get the size of the record
    */
  private[services] def sendAggregationRec(
      aggregationSeq: Seq[AnyRef],
      numberOfElements: Int,
      schema: Schema,
      nameIndex: String,
      topicOut: String,
      aggregationColumnName: String,
      bootstrapServers: String,
      schemaRegistryUrl: String,
      maxMessageBytes: Int,
      debugInformation: DebugInformation
  ): Seq[AnyRef] = {
    val avroRecord: GenericData.Record =
      createAvroRecordForAggregation(aggregationSeq, schema, topicOut, aggregationColumnName, debugInformation)

    val record: ProducerRecord[String, GenericRecord] =
      new ProducerRecord[String, GenericRecord](topicOut, nameIndex, avroRecord)

    val recordSize: Int = getRecordSize(record, schemaRegistryUrl, debugInformation)

    if (recordSize > maxMessageBytes) { // record too big to be sent
      sendHugeRecord(
        aggregationSeq,
        numberOfElements,
        schema,
        nameIndex,
        topicOut,
        aggregationColumnName,
        bootstrapServers,
        schemaRegistryUrl,
        maxMessageBytes,
        recordSize,
        debugInformation
      )
    } else {
      validateRecordToSend(avroRecord, schema, topicOut, debugInformation)

      sendCorrectRecord(
        record,
        topicOut,
        bootstrapServers,
        schemaRegistryUrl,
        maxMessageBytes,
        recordSize,
        debugInformation
      )
    }
  }

  /** Method used to deal with records having a size too high to be sent in one single message to Kafka
    *
    * @param aggregationSeq The Seq containing the aggregated values to flush
    * @param numberOfElements: The number of aggregated elements in the aggregationSeq Seq
    * @param schema: The value schema of the topicOut Kafka topic
    * @param nameIndex: The name index related to the partition being flushed
    * @param topicOut The Kafka topic to which the aggregation result is sent
    * @param aggregationColumnName The name of the Column storing the aggregation result in ksqlDB
    * @param bootstrapServers The List of Kafka brokers
    * @param schemaRegistryUrl The url of the Kafka Schema Registry
    * @param maxMessageBytes The maximum size in bytes of a message sent to the topicOut Kafka topic, according to its configuration
    * @param recordSize The size of the record
    *
    * @return a Seq containing the elements that couldn't be sent because of the message size limit
    *
    * @throws MaxMessageBytesException If it's not even possible to send an aggregation of one element
    * @throws AvroRuntimeException Problem related to the schema
    * @throws InterruptException Issue while sending a record to Kafka or while flushing the producer
    * @throws IllegalStateException send method : if a transactional id has been configured and no transaction has been started, or when send is invoked after producer has been closed.
    * @throws KafkaException Problem while sending the record to Kafka or with the construction or the closing of the Kafka Producer
    * @throws SerializationException Issue while Serializing the record to retrieve its size
    * @throws InvalidConfigurationException Issue during serialization made to get the size of the record
    */
  private[services] def sendHugeRecord(
      aggregationSeq: Seq[AnyRef],
      numberOfElements: Int,
      schema: Schema,
      nameIndex: String,
      topicOut: String,
      aggregationColumnName: String,
      bootstrapServers: String,
      schemaRegistryUrl: String,
      maxMessageBytes: Int,
      recordSize: Int,
      debugInformation: DebugInformation
  ): Seq[AnyRef] = {
    if (numberOfElements == 1) { // can't even send an aggregation of one element
      log.error(
        s"[sendHugeRecord {topic : ${debugInformation.topic}, partition : ${debugInformation.partition}, offset(s) ${debugInformation.offsetFrom} to ${debugInformation.offsetTo}}] According to the $topicOut Kafka topic max.message.bytes configuration, the maximum size of a message is $maxMessageBytes bytes. However, a message with a single aggregated value has a size of $recordSize bytes which is already superior than the maximum size allowed. Consider either lowering the size of a single message coming from a Kafka topic or improving the max.message.bytes configuration"
          .replaceAll("[\r\n]", "")
      )
      throw MaxMessageBytesException(
        s"According to the $topicOut Kafka topic max.message.bytes configuration, the maximum size of a message is $maxMessageBytes bytes. However, a message with a single aggregated value has a size of $recordSize bytes which is already superior than the maximum size allowed. Consider either lowering the size of a single message coming from a Kafka topic or improving the max.message.bytes configuration"
      )
    }

    val extraBytes: Int = recordSize - maxMessageBytes
    val emptyRecordSize: Int =
      getEmptyRecordSize(schema, topicOut, aggregationColumnName, schemaRegistryUrl, debugInformation)

    val averageElementSize: Double = (recordSize - emptyRecordSize).toDouble / numberOfElements.toDouble

    val awaitedNumberOfElementsWithMargin: Int =
      getAwaitedNumberOfElementsWithMargin(maxMessageBytes, emptyRecordSize, averageElementSize, numberOfElements)

    val numberOfExtraElements: Int = numberOfElements - awaitedNumberOfElementsWithMargin

    val (newAggregationSeq, extraElementsSeq): (Seq[AnyRef], Seq[AnyRef]) =
      aggregationSeq.splitAt(awaitedNumberOfElementsWithMargin)

    log.debug(
      s"""\n[sendHugeRecord {topic : ${debugInformation.topic
        .replaceAll("[\r\n]", "")}, partition : ${debugInformation.partition}, offset(s) ${debugInformation.offsetFrom} to ${debugInformation.offsetTo}}] Huge Record information :
         | ----------------------------------------------------------------------------
         | Record size : $recordSize bytes
         | Maximum size : $maxMessageBytes bytes
         | Number of extra bytes : $extraBytes bytes
         | Average element size : ${averageElementSize.toInt} bytes
         | Empty record size : $emptyRecordSize bytes
         | Number of elements : $numberOfElements 
         | New number of elements : $awaitedNumberOfElementsWithMargin (with margin)
         | Number of extra elements : $numberOfExtraElements 
         | New Aggregation number : ${newAggregationSeq.size}
         | Extra Aggregation number : ${extraElementsSeq.size}
         | ----------------------------------------------------------------------------
         |""".stripMargin
    )

    val updatedDebugInformation: DebugInformation = DebugInformation(
      topic = debugInformation.topic,
      partition = debugInformation.partition,
      offsetFrom = debugInformation.offsetFrom,
      offsetTo = debugInformation.offsetTo - numberOfExtraElements
    )

    val sendRecordExtraElementsSeq = sendAggregationRec(
      aggregationSeq = newAggregationSeq,
      numberOfElements = awaitedNumberOfElementsWithMargin,
      schema = schema,
      nameIndex = nameIndex,
      topicOut = topicOut,
      aggregationColumnName = aggregationColumnName,
      bootstrapServers = bootstrapServers,
      schemaRegistryUrl = schemaRegistryUrl,
      maxMessageBytes = maxMessageBytes,
      debugInformation = updatedDebugInformation
    )

    sendRecordExtraElementsSeq :++ extraElementsSeq
  }

  /** Method used to retrieve the expected number of elements in the record to send to Kafka, with a margin, and according to the average size of an element and to the maximum size in bytes of a Kafka message
    *
    * @param maxMessageBytes The maximum size in bytes of a message sent to the topicOut Kafka topic, according to its configuration
    * @param emptyRecordSize The size in bytes of an empty record (empty Kafka message)
    * @param averageElementSize The average size of an element
    * @param numberOfElements The current number of elements to send to Kafka
    */
  private[services] def getAwaitedNumberOfElementsWithMargin(
      maxMessageBytes: Int,
      emptyRecordSize: Int,
      averageElementSize: Double,
      numberOfElements: Int
  ): Int = {
    val awaitedNumberOfElementsWithMargin: Int = Math
      .floor(
        ((maxMessageBytes - emptyRecordSize).toDouble / averageElementSize) * Constants.awaitedElementNumberInKafkaMessageMarginRatio
      )
      .toInt

    Math.max(1, Math.min(awaitedNumberOfElementsWithMargin, numberOfElements - 1))
  }

  /** Method used to send the records having a size allowing them to be sent in one single message to Kafka
    *
    * @param record The record containing the aggregation to send to Kafka
    * @param topicOut The Kafka topic to which the aggregation result is sent
    * @param bootstrapServers The List of Kafka brokers
    * @param schemaRegistryUrl The url of the Kafka Schema Registry
    * @param maxMessageBytes The maximum size in bytes of a message sent to the topicOut Kafka topic, according to its configuration
    * @param recordSize The size of the record
    *
    * @return an empty Seq as there is no extra data
    *
    * @throws InterruptException Issue while sending a record to Kafka or while flushing the producer
    * @throws IllegalStateException send method : if a transactional id has been configured and no transaction has been started, or when send is invoked after producer has been closed.
    * @throws KafkaException Problem while sending the record to Kafka or with the construction or the closing of the Kafka Producer
    */
  private[services] def sendCorrectRecord(
      record: ProducerRecord[String, GenericRecord],
      topicOut: String,
      bootstrapServers: String,
      schemaRegistryUrl: String,
      maxMessageBytes: Int,
      recordSize: Int,
      debugInformation: DebugInformation
  ): Seq[AnyRef] = {

    Using(ProducerAggregation.createProducer(bootstrapServers, schemaRegistryUrl, maxMessageBytes)) {
      producer: Producer[String, GenericRecord] =>
        producer.send(record).get()
        log.info(
          s"[FLUSH PARTITION {topic : ${debugInformation.topic}, partition : ${debugInformation.partition}, offset(s) ${debugInformation.offsetFrom} to ${debugInformation.offsetTo}}] Record sent to Kafka topic $topicOut with a size of $recordSize bytes"
            .replaceAll("[\r\n]", "") + "\n"
        )
        producer.flush()
        Seq.empty[AnyRef]
    } match {
      case Success(emptySeq) => emptySeq
      case Failure(exception: InterruptException) =>
        log.error(
          s"[FLUSH PARTITION {topic : ${debugInformation.topic}, partition : ${debugInformation.partition}, offset(s) ${debugInformation.offsetFrom} to ${debugInformation.offsetTo}}] Issue while sending a record to Kafka or while flushing the producer"
            .replaceAll("[\r\n]", ""),
          exception
        )
        throw exception
      case Failure(exception: IllegalStateException) =>
        log.error(
          s"[FLUSH PARTITION {topic : ${debugInformation.topic}, partition : ${debugInformation.partition}, offset(s) ${debugInformation.offsetFrom} to ${debugInformation.offsetTo}}] Issue while trying to send a record to the $topicOut Kafka Topic"
            .replaceAll("[\r\n]", ""),
          exception
        )
        throw exception
      case Failure(exception: KafkaException) =>
        log.error(
          s"[FLUSH PARTITION {topic : ${debugInformation.topic}, partition : ${debugInformation.partition}, offset(s) ${debugInformation.offsetFrom} to ${debugInformation.offsetTo}}] Issue while sending a record to Kafka or failed to construct or close the Kafka Producer used to send data to the $topicOut Kafka Topic"
            .replaceAll("[\r\n]", ""),
          exception
        )
        throw exception
      case Failure(exception: Throwable) =>
        log.error(
          s"[FLUSH PARTITION {topic : ${debugInformation.topic}, partition : ${debugInformation.partition}, offset(s) ${debugInformation.offsetFrom} to ${debugInformation.offsetTo}}] Unexpected exception while trying to send an aggregation record to the $topicOut Kafka Topic"
            .replaceAll("[\r\n]", ""),
          exception
        )
        throw exception
    }
  }

  /** Method used to retrieve the current schema of a Kafka topic
    *
    * @param topic The name of the Kafka topic
    * @param schemaRegistryUrl The url of the Kafka Schema Registry
    *
    * @throws IOException Problem with the Http Request used to retrieve the schema of the output topic
    * @throws RestClientException Either the output Kafka topic does not exists or has no schema, or an other problem with the Http Request used to retrieve the schema of the output topic
    */
  private def getTopicSchema(topic: String, schemaRegistryUrl: String, debugInformation: DebugInformation): Schema = {
    Try {
      val client: CachedSchemaRegistryClient = new CachedSchemaRegistryClient(schemaRegistryUrl, 1000)
      val schemaMetadata = client.getLatestSchemaMetadata(s"$topic-value")
      val schemaString = schemaMetadata.getSchema
      val parser: Schema.Parser = new Schema.Parser()

      parser.parse(schemaString)
    } match {
      case Success(schema) => schema
      case Failure(exception: IOException) =>
        log.error(
          s"[getTopicSchema {topic : ${debugInformation.topic}, partition : ${debugInformation.partition}, offset(s) ${debugInformation.offsetFrom} to ${debugInformation.offsetTo}}] Problem with the Http Request used to retrieve the schema for the $topic Kafka topic"
            .replaceAll("[\r\n]", ""),
          exception
        )
        throw exception
      case Failure(exception: RestClientException) =>
        if (exception.getErrorCode == Constants.schemaRegistryNoSchemaForTopicErrorCode) {
          log.error(
            s"[getTopicSchema {topic : ${debugInformation.topic}, partition : ${debugInformation.partition}, offset(s) ${debugInformation.offsetFrom} to ${debugInformation.offsetTo}}] The $topic Kafka topic does not exist or doesn't have a schema"
              .replaceAll("[\r\n]", ""),
            exception
          )
        } else {
          log.error(
            s"[getTopicSchema {topic : ${debugInformation.topic}, partition : ${debugInformation.partition}, offset(s) ${debugInformation.offsetFrom} to ${debugInformation.offsetTo}}] Problem with the Http Request used to retrieve the schema for the $topic Kafka topic"
              .replaceAll("[\r\n]", ""),
            exception
          )
        }
        throw exception
      case Failure(exception: Throwable) =>
        log.error(
          s"[getTopicSchema {topic : ${debugInformation.topic}, partition : ${debugInformation.partition}, offset(s) ${debugInformation.offsetFrom} to ${debugInformation.offsetTo}}] Unexpected issue while trying to retrieve the schema of the $topic Kafka topic"
            .replaceAll("[\r\n]", ""),
          exception
        )
        throw exception
    }
  }

  /** Method used to get the size in bytes of a ProducerRecord
    *
    * @param record The Producer Record
    * @param schemaRegistryUrl The url of the Kafka Schema Registry
    *
    * @return The size in bytes of the record
    *
    * @throws SerializationException Issue while Serializing the record to retrieve its size
    * @throws InvalidConfigurationException Issue during serialization made to get the size of the record
    */
  private[services] def getRecordSize(
      record: ProducerRecord[String, GenericRecord],
      schemaRegistryUrl: String,
      debugInformation: DebugInformation
  ): Int = {
    Try[Int] {
      val serializedKey = new StringSerializer().serialize(record.topic(), record.headers(), record.key())

      val props = HashMap("schema.registry.url" -> schemaRegistryUrl).asJava
      val serializer = new KafkaAvroSerializer
      serializer.configure(props, false)
      val serializedValue = serializer.serialize(record.topic(), record.headers(), record.value())

      AbstractRecords.estimateSizeInBytesUpperBound(
        new ApiVersions().maxUsableProduceMagic(),
        CompressionType.forName("none"),
        serializedKey,
        serializedValue,
        record.headers().toArray
      )
    } match {
      case Success(size) => size
      case Failure(exception: SerializationException) =>
        log.error(
          s"[getRecordSize {topic : ${debugInformation.topic}, partition : ${debugInformation.partition}, offset(s) ${debugInformation.offsetFrom} to ${debugInformation.offsetTo}}] Issue while serializing the record"
            .replaceAll("[\r\n]", ""),
          exception
        )
        throw exception
      case Failure(exception: InvalidConfigurationException) =>
        log.error(
          s"[getRecordSize {topic : ${debugInformation.topic}, partition : ${debugInformation.partition}, offset(s) ${debugInformation.offsetFrom} to ${debugInformation.offsetTo}}] Invalid configuration for serialization"
            .replaceAll("[\r\n]", ""),
          exception
        )
        throw exception
      case Failure(exception: Throwable) =>
        log.error(
          s"[getRecordSize {topic : ${debugInformation.topic}, partition : ${debugInformation.partition}, offset(s) ${debugInformation.offsetFrom} to ${debugInformation.offsetTo}}] Unexpected issue while trying to retrieve the size of a record"
            .replaceAll("[\r\n]", ""),
          exception
        )
        throw exception
    }

  }

  /** Method used to create the avro Record containing the aggregation result
    *
    * @param aggregationSeq The Seq containing the aggregated values
    * @param schema: The value schema of the topicOut Kafka topic
    * @param topicOut The Kafka topic to which the aggregation result is sent
    * @param aggregationColumnName The name of the Column storing the aggregation result in ksqlDB
    *
    * @return The created avro Record containing the aggregation result
    *
    * @throws AvroRuntimeException If the Column Name defined in the aggregationColumnName connector's property does not exist in the Kafka output topic's schema or if there is an issue between the aggregated values and the schema
    */
  private[services] def createAvroRecordForAggregation(
      aggregationSeq: Seq[AnyRef],
      schema: Schema,
      topicOut: String,
      aggregationColumnName: String,
      debugInformation: DebugInformation
  ): GenericData.Record = {
    val aggregationField: Schema.Field =
      getAggregationFieldInSchema(aggregationColumnName, schema, topicOut, debugInformation)

    val aggregationAvroRecord: AnyRef =
      updateValueWithSchema(
        value = aggregationSeq.asJava,
        schema = aggregationField.schema(),
        debugInformation = debugInformation
      )

    Try[GenericData.Record] {
      val avroRecord: GenericData.Record = new GenericData.Record(schema)
      avroRecord.put(
        aggregationColumnName,
        aggregationAvroRecord
      )

      avroRecord
    } match {
      case Success(avroRecord) => avroRecord
      case Failure(exception: AvroRuntimeException) =>
        log.error(
          s"[createAvroRecordForAggregation {topic : ${debugInformation.topic}, partition : ${debugInformation.partition}, offset(s) ${debugInformation.offsetFrom} to ${debugInformation.offsetTo}}] Issue while creating the avro Record containing the aggregated values"
            .replaceAll("[\r\n]", ""),
          exception
        )
        throw exception
      case Failure(exception: Throwable) =>
        log.error(
          s"[createAvroRecordForAggregation {topic : ${debugInformation.topic}, partition : ${debugInformation.partition}, offset(s) ${debugInformation.offsetFrom} to ${debugInformation.offsetTo}}] Unexpected issue while creating the avro Record containing the aggregated values"
            .replaceAll("[\r\n]", ""),
          exception
        )
        throw exception
    }
  }

  /** Method used to get the size of an empty ProducerRecord (without any aggregated element)
    *
    * @param topicOut The name of the Kafka topic
    * @param aggregationColumnName The name of the Column storing the aggregation result in ksqlDB
    * @param schemaRegistryUrl The url of the Kafka Schema Registry
    *
    * @return The size of an empty ProducerRecord made for aggregation
    *
    * @throws AvroRuntimeException If the Column Name defined in the aggregationColumnName connector's property does not exist in the Kafka output topic's schema or if there is an issue between the aggregated values and the schema
    * @throws SerializationException Issue while Serializing the record to retrieve its size
    * @throws InvalidConfigurationException Issue during serialization made to get the size of the record
    */
  private[services] def getEmptyRecordSize(
      schema: Schema,
      topicOut: String,
      aggregationColumnName: String,
      schemaRegistryUrl: String,
      debugInformation: DebugInformation
  ): Int = {
    val emptyAvroRecord: GenericData.Record = createAvroRecordForAggregation(
      aggregationSeq = Seq.empty[AnyRef],
      schema = schema,
      topicOut = topicOut,
      aggregationColumnName = aggregationColumnName,
      debugInformation = debugInformation
    )

    val emptyRecord: ProducerRecord[String, GenericRecord] =
      new ProducerRecord[String, GenericRecord](topicOut, topicOut, emptyAvroRecord)

    getRecordSize(emptyRecord, schemaRegistryUrl, debugInformation)
  }

  /** Update the value that came from Kafka with its new schema
    *
    * @param value The value to update, warning : value can be null
    * @param schema The new schema for the value
    *
    * @throws AvroRuntimeException The type of the value is not in the union or, from getValueType : If the value parameter has an unknown type
    */
  private def updateValueWithSchema(value: AnyRef, schema: Schema, debugInformation: DebugInformation): AnyRef = {
    val currentType = schema.getType

    currentType match {
      case Schema.Type.RECORD =>
        val recordValue = checkRecordType(value, debugInformation)
        val avroRecord: GenericData.Record = new GenericData.Record(schema)
        schema.getFields.forEach { field: Schema.Field =>
          avroRecord.put(
            field.name(),
            updateValueWithSchema(
              value = recordValue.get(field.name()),
              schema = field.schema(),
              debugInformation = debugInformation
            )
          )
        }
        avroRecord
      case Schema.Type.ARRAY =>
        val arrayValues: util.List[AnyRef] = checkArrayType(value, debugInformation)
        val avroArray: GenericData.Array[AnyRef] = new GenericData.Array[AnyRef](arrayValues.size(), schema)
        arrayValues.asScala.toSeq.foreach { arrayValue =>
          avroArray.add(
            updateValueWithSchema(
              value = arrayValue,
              schema = schema.getElementType,
              debugInformation = debugInformation
            )
          )
        }
        avroArray
      case Schema.Type.MAP =>
        val mapValues: util.Map[AnyRef, AnyRef] = checkMapType(value, debugInformation)
        val avroMap: util.Map[AnyRef, AnyRef] = new util.HashMap[AnyRef, AnyRef](mapValues.size())
        mapValues.entrySet().asScala.toSet.foreach { entry: util.Map.Entry[AnyRef, AnyRef] =>
          avroMap.put(
            updateValueWithSchema(
              value = entry.getKey,
              schema = Schema.create(Schema.Type.STRING),
              debugInformation = debugInformation
            ),
            updateValueWithSchema(
              value = entry.getValue,
              schema = schema.getValueType,
              debugInformation = debugInformation
            )
          )
        }
        avroMap
      case Schema.Type.UNION =>
        val valueType = getValueType(value, debugInformation)
        val valueSchema = schema.getTypes.asScala.toSeq.find { schema: Schema =>
          schema.getType.equals(valueType)
        } match {
          case Some(schema) => schema
          case None =>
            log.error(
              s"[updateValueWithSchema {topic : ${debugInformation.topic}, partition : ${debugInformation.partition}, offset(s) ${debugInformation.offsetFrom} to ${debugInformation.offsetTo}}] The type of the value $valueType is different than the types in the union : $schema"
                .replaceAll("[\r\n]", "")
            )
            throw new AvroRuntimeException(
              s"The type of the value $value ($valueType) is different than the types in the union : ${schema.getTypes}"
                .replaceAll("[\r\n]", "")
            )
        }
        updateValueWithSchema(value = value, schema = valueSchema, debugInformation = debugInformation)
      case Schema.Type.BOOLEAN | Schema.Type.BYTES | Schema.Type.DOUBLE | Schema.Type.ENUM | Schema.Type.FIXED |
          Schema.Type.FLOAT | Schema.Type.INT | Schema.Type.LONG | Schema.Type.STRING =>
        checkValueType(value = value, awaitedType = currentType, debugInformation = debugInformation)
        value
      case Schema.Type.NULL =>
        checkValueType(value = value, awaitedType = currentType, debugInformation = debugInformation)
        value // value is null
      case _ =>
        log.error(
          s"[updateValueWithSchema {topic : ${debugInformation.topic}, partition : ${debugInformation.partition}, offset(s) ${debugInformation.offsetFrom} to ${debugInformation.offsetTo}}] Unexpected exception, unknown avro type $currentType"
            .replaceAll("[\r\n]", "")
        )
        throw new AvroRuntimeException(s"Unexpected exception, unknown avro type $currentType".replaceAll("[\r\n]", ""))
    }
  }

  /** Method used to check that a value is of type Record
    *
    * @param value The value to check, warning : value can be null
    *
    * @return a GenericData.Record value
    *
    * @throws AvroRuntimeException If the value doesn't correspond to a GenericData.Record
    */
  private def checkRecordType(
      value: AnyRef,
      debugInformation: DebugInformation
  ): GenericData.Record = {
    checkValueType(value = value, awaitedType = Schema.Type.RECORD, debugInformation = debugInformation)
    value match {
      case recordValue: GenericData.Record => recordValue
      case _ =>
        log.error(
          s"[checkRecordType {topic : ${debugInformation.topic}, partition : ${debugInformation.partition}, offset(s) ${debugInformation.offsetFrom} to ${debugInformation.offsetTo}}] The following value : $value should be of type ${Schema.Type.RECORD} but this is not the case"
            .replaceAll("[\r\n]", "")
        )
        throw new AvroRuntimeException(
          s"The following value : $value should be of type ${Schema.Type.RECORD} but this is not the case"
            .replaceAll("[\r\n]", "")
        )
    }
  }

  /** Method used to check that a value is of type Array
    *
    * @param value The value to check, warning : value can be null
    *
    * @return a util.List[AnyRef] value
    *
    * @throws AvroRuntimeException If the value doesn't correspond to an Array
    */
  private def checkArrayType(
      value: AnyRef,
      debugInformation: DebugInformation
  ): util.List[AnyRef] = {
    checkValueType(value = value, awaitedType = Schema.Type.ARRAY, debugInformation = debugInformation)
    value match {
      case arrayValue: util.List[AnyRef] => arrayValue
      case _ =>
        log.error(
          s"[checkRecordType {topic : ${debugInformation.topic}, partition : ${debugInformation.partition}, offset(s) ${debugInformation.offsetFrom} to ${debugInformation.offsetTo}}] The following value : $value should be of type ${Schema.Type.ARRAY} but this is not the case"
            .replaceAll("[\r\n]", "")
        )
        throw new AvroRuntimeException(
          s"The following value : $value should be of type ${Schema.Type.ARRAY} but this is not the case"
            .replaceAll("[\r\n]", "")
        )
    }
  }

  /** Method used to check that a value is of type Map
    *
    * @param value The value to check, warning : value can be null
    *
    * @return a util.Map[AnyRef, AnyRef]value
    *
    * @throws AvroRuntimeException If the value doesn't correspond to an Map
    */
  private def checkMapType(
      value: AnyRef,
      debugInformation: DebugInformation
  ): util.Map[AnyRef, AnyRef] = {
    checkValueType(value = value, awaitedType = Schema.Type.MAP, debugInformation = debugInformation)
    value match {
      case mapValue: util.Map[AnyRef, AnyRef] => mapValue
      case _ =>
        log.error(
          s"[checkRecordType {topic : ${debugInformation.topic}, partition : ${debugInformation.partition}, offset(s) ${debugInformation.offsetFrom} to ${debugInformation.offsetTo}}] The following value : $value should be of type ${Schema.Type.MAP} but this is not the case"
            .replaceAll("[\r\n]", "")
        )
        throw new AvroRuntimeException(
          s"The following value : $value should be of type ${Schema.Type.MAP} but this is not the case"
            .replaceAll("[\r\n]", "")
        )
    }
  }

  /** Method used to check the type of the current value
    *
    * @param value The value to check, warning : value can be null
    * @param awaitedType The awaited type according to the schema
    *
    * @throws AvroRuntimeException If the value doesn't have the type awaited by the schema
    */
  private def checkValueType(value: AnyRef, awaitedType: Schema.Type, debugInformation: DebugInformation): Unit = {
    val realType = getValueType(value, debugInformation)
    if (realType != awaitedType) {
      log.error(
        s"[checkValueType {topic : ${debugInformation.topic}, partition : ${debugInformation.partition}, offset(s) ${debugInformation.offsetFrom} to ${debugInformation.offsetTo}}] According to the schema of the output Kafka topic the type of $value should be $awaitedType but is $realType"
          .replaceAll("[\r\n]", "")
      )
      throw new AvroRuntimeException(
        s"According to the schema of the output Kafka topic the type of $value should be $awaitedType but is $realType"
          .replaceAll("[\r\n]", "")
      )
    }
  }

  /** Method used to get the type of an AnyRef value
    *
    * @param value value AnyRef from which we want to get the type, warning : value can be null
    *
    * @throws AvroRuntimeException If the value parameter has an unknown type
    */
  private def getValueType(value: AnyRef, debugInformation: DebugInformation): Schema.Type = {
    if (value == null || value == JsonProperties.NULL_VALUE) {
      Schema.Type.NULL
    } else
      value match {
        case _: IndexedRecord =>
          Schema.Type.RECORD
        case _: GenericEnumSymbol[AnyRef] =>
          Schema.Type.ENUM
        case _: util.Collection[AnyRef] =>
          Schema.Type.ARRAY
        case _: util.Map[AnyRef, AnyRef] =>
          Schema.Type.MAP
        case _: GenericFixed =>
          Schema.Type.FIXED
        case _: CharSequence =>
          Schema.Type.STRING
        case _: ByteBuffer =>
          Schema.Type.BYTES
        case _: Integer =>
          Schema.Type.INT
        case _: lang.Long =>
          Schema.Type.LONG
        case _: lang.Float =>
          Schema.Type.FLOAT
        case _: lang.Double =>
          Schema.Type.DOUBLE
        case _: lang.Boolean =>
          Schema.Type.BOOLEAN
        case _ =>
          log.error(
            s"[getValueType {topic : ${debugInformation.topic}, partition : ${debugInformation.partition}, offset(s) ${debugInformation.offsetFrom} to ${debugInformation.offsetTo}}] Unknown datum type ${value.getClass.getName}: $value"
              .replaceAll("[\r\n]", "")
          )
          throw new AvroRuntimeException(
            s"Unknown datum type ${value.getClass.getName}: $value".replaceAll("[\r\n]", "")
          )
      }
  }

  /** Method used to check and get the field representing the aggregation in the schema, according to the column name defined in the aggregationColumnName connector's property
    *
    * @param aggregationColumnName The name of the Column storing the aggregation result in ksqlDB
    * @param schema The schema of the Kafka output topic
    * @param topicOut The name of the Kafka output topic
    *
    * @throws AvroRuntimeException If the Column Name defined in the aggregationColumnName connector's property does not exist in the Kafka output topic's schema
    */
  private def getAggregationFieldInSchema(
      aggregationColumnName: String,
      schema: Schema,
      topicOut: String,
      debugInformation: DebugInformation
  ): Schema.Field = {
    Option(schema.getField(aggregationColumnName)) match {
      case Some(field) => field
      case None =>
        log.error(
          s"[getAggregationFieldInSchema {topic : ${debugInformation.topic}, partition : ${debugInformation.partition}, offset(s) ${debugInformation.offsetFrom} to ${debugInformation.offsetTo}}] The Column Name defined in the aggregationColumnName connector's property does not exist in the $topicOut Kafka topic's schema (remember that ksqlDB stores name of fields in upper case letters)"
            .replaceAll("[\r\n]", "")
        )
        throw new AvroRuntimeException(
          s"The Column Name defined in the aggregationColumnName connector's property does not exist in the $topicOut Kafka topic's schema (remember that ksqlDB stores name of fields in upper case letters)"
            .replaceAll("[\r\n]", "")
        )
    }
  }

  /** Method validating that the avroRecord is correct according to the schema of the output topic
    *
    * @param avroRecord The avro record to check and to send to Kafka
    * @param schema The schema of the Kafka topic represented by topicOut
    * @param topicOut The name of the output Kafka topic
    *
    * @throws AvroRuntimeException If the Kafka topic's schema is incoherent with at least one value in the aggregated result we want to send
    */
  private[services] def validateRecordToSend(
      avroRecord: GenericData.Record,
      schema: Schema,
      topicOut: String,
      debugInformation: DebugInformation
  ): Unit = {
    val genericData = new GenericData()
    if (!genericData.validate(schema, avroRecord)) {
      log.error(
        s"[validateRecordToSend {topic : ${debugInformation.topic
          .replaceAll("[\r\n]", "")}, partition : ${debugInformation.partition}, offset(s) ${debugInformation.offsetFrom} to ${debugInformation.offsetTo}}] The ${topicOut
          .replaceAll("[\r\n]", "")} Kafka topic's schema is incoherent with at least one value in the aggregated result we want to send\n Topic schema : ${schema.toString
          .replaceAll("[\r\n]", "")}\n (it can be also an incoherence between one deep value and its schema)"
      )
      throw new AvroRuntimeException(
        s"The $topicOut Kafka topic's schema is incoherent with at least one value in the aggregated result we want to send"
          .replaceAll("[\r\n]", "")
      )
    }
    log.debug(
      s"[validateRecordToSend {topic : ${debugInformation.topic}, partition : ${debugInformation.partition}, offset(s) ${debugInformation.offsetFrom} to ${debugInformation.offsetTo}}] The record to send to the $topicOut Kafka topic is valid according to the $topicOut topic's schema."
        .replaceAll("[\r\n]", "")
    )
  }
}
