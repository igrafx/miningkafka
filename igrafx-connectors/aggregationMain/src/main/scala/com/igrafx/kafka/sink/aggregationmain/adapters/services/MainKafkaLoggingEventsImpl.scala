package com.igrafx.kafka.sink.aggregationmain.adapters.services

import com.igrafx.kafka.sink.aggregationmain.Constants
import com.igrafx.kafka.sink.aggregationmain.adapters.serializers.PathSerializer
import com.igrafx.kafka.sink.aggregationmain.adapters.services.producer.ProducerMain
import com.igrafx.kafka.sink.aggregationmain.domain.entities.{KafkaLoggedEvent, KafkaLoggingEventsProperties}
import com.igrafx.kafka.sink.aggregationmain.domain.interfaces.MainKafkaLoggingEvents
import org.apache.avro.generic.{GenericData, GenericRecord}
import org.apache.avro.{AvroRuntimeException, Schema, SchemaBuilder}
import org.apache.kafka.clients.producer.{Producer, ProducerRecord}
import org.apache.kafka.common.KafkaException
import org.apache.kafka.common.errors.InterruptException
import org.json4s.{DefaultFormats, Formats}
import org.json4s.jackson.Serialization
import org.slf4j.Logger

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.util.{Failure, Success, Using}

class MainKafkaLoggingEventsImpl(private val log: Logger) extends MainKafkaLoggingEvents {
  private val schema = getKafkaLoggingEventsSchema
  private implicit val formats: DefaultFormats.type = DefaultFormats

  /** Method used to send a connector event to the Kafka Event Logging Topic
    *
    * @param kafkaLoggingEventsProperties The Kafka events logging related properties
    *
    * @throws AvroRuntimeException If there is an issue between the avro Schema to use and the value of the record we want to send
    * @throws InterruptException Issue while sending a record to Kafka or while flushing the producer
    * @throws IllegalStateException send method : if a transactional id has been configured and no transaction has been started, or when send is invoked after producer has been closed.
    * @throws KafkaException If the creation or the closing of the KafkaProducer failed
    */
  def sendEventToKafka(
      kafkaLoggingEventsProperties: KafkaLoggingEventsProperties,
      kafkaLoggedEvent: KafkaLoggedEvent
  ): Future[Unit] = {
    Future {
      implicit val formats: Formats = DefaultFormats + new PathSerializer
      val avroRecord: GenericRecord = new GenericData.Record(schema)
      avroRecord.put(Constants.kafkaLoggingEventTypeColumnName, kafkaLoggedEvent.eventType)
      avroRecord.put(Constants.kafkaLoggingIGrafxProjectColumnName, kafkaLoggedEvent.igrafxProject)
      avroRecord.put(Constants.kafkaLoggingEventDateColumnName, kafkaLoggedEvent.eventDate)
      avroRecord.put(Constants.kafkaLoggingEventSequenceIdColumnName, kafkaLoggedEvent.eventSequenceId)
      avroRecord.put(
        Constants.kafkaLoggingPayloadColumnName,
        Serialization.write(kafkaLoggedEvent.payload)
      )

      val record: ProducerRecord[String, GenericRecord] =
        new ProducerRecord[String, GenericRecord](
          kafkaLoggingEventsProperties.topic,
          avroRecord
        )

      log.debug(
        s"[KAFKA LOG EVENT] New event Record of eventSequenceId ${kafkaLoggedEvent.eventSequenceId} to send to the Kafka event logging ${kafkaLoggingEventsProperties.topic} topic : \n$record"
          .replaceAll("[\r\n]", "")
      )

      Using(
        ProducerMain.createProducer(
          kafkaLoggingEventsProperties.bootstrapServers,
          kafkaLoggingEventsProperties.schemaRegistryUrl
        )
      ) { producer: Producer[String, GenericRecord] =>
        producer.send(record).get
        log.info(
          s"[KAFKA LOG EVENT] Event record of eventSequenceId ${kafkaLoggedEvent.eventSequenceId} sent to the ${kafkaLoggingEventsProperties.topic} Kafka topic !"
            .replaceAll("[\r\n]", "")
        )
        producer.flush()
      } match {
        case Success(_) => ()
        case Failure(exception) => throw exception
      }

      ()
    }.recover {
      case exception: AvroRuntimeException =>
        log.error(
          s"[KAFKA LOG EVENT] There is an issue between the record we want to send to the ${kafkaLoggingEventsProperties.topic} Kafka topic and the Schema to use for Logging : $schema"
            .replaceAll("[\r\n]", ""),
          exception
        )
        throw exception
      case exception: InterruptException =>
        log.error(
          s"[KAFKA LOG EVENT] Issue while sending a record to the ${kafkaLoggingEventsProperties.topic} Kafka topic or while flushing the producer"
            .replaceAll("[\r\n]", ""),
          exception
        )
        throw exception
      case exception: IllegalStateException =>
        log.error(
          s"[KAFKA LOG EVENT] Issue while trying to send a record to the ${kafkaLoggingEventsProperties.topic} Kafka Topic"
            .replaceAll("[\r\n]", ""),
          exception
        )
        throw exception
      case exception: KafkaException =>
        log.error(
          s"[KAFKA LOG EVENT] Issue while sending a record to the ${kafkaLoggingEventsProperties.topic} Kafka topic or failed to construct or close the Kafka Producer"
            .replaceAll("[\r\n]", ""),
          exception
        )
        throw exception
      case exception: Throwable =>
        log.error(
          s"[KAFKA LOG EVENT] Unexpected exception threw while trying to send an event to the ${kafkaLoggingEventsProperties.topic} Kafka topic logging the connector's events"
            .replaceAll("[\r\n]", ""),
          exception
        )
        throw exception
    }
  }

  /** Method used to create the schema of the data sent to the Kafka events logging topic
    *
    * @return The schema related to the Kafka events logging topic
    */
  private def getKafkaLoggingEventsSchema: Schema = {
    //format: off
    val schema: Schema = SchemaBuilder
      .record("igrafxKafkaLoggingEventsSchema")
      .namespace("com.igrafx")
      .fields()
        .name(Constants.kafkaLoggingEventTypeColumnName)
          .`type`().unionOf().nullType().and().stringType().endUnion()
          .nullDefault()
        .name(Constants.kafkaLoggingIGrafxProjectColumnName)
          .`type`().unionOf().nullType().and().stringType().endUnion()
          .nullDefault()
        .name(Constants.kafkaLoggingEventDateColumnName)
          .`type`().unionOf().nullType().and().longType().endUnion()
          .nullDefault()
        .name(Constants.kafkaLoggingEventSequenceIdColumnName)
          .`type`().unionOf().nullType().and().stringType().endUnion()
          .nullDefault()
       .name(Constants.kafkaLoggingPayloadColumnName)
          .`type`().unionOf().nullType().and().stringType().endUnion()
          .nullDefault()
      .endRecord()
    //format: on

    log.debug(
      s"[KAFKA LOG EVENT] Avro Schema used to send data to the Kafka topic logging connector's events : $schema"
        .replaceAll("[\r\n]", "")
    )

    schema
  }
}
