package com.igrafx.kafka.sink.aggregationmain.domain.interfaces

import com.igrafx.kafka.sink.aggregationmain.domain.entities.{KafkaLoggedEvent, KafkaLoggingEventsProperties}
import org.apache.avro.AvroRuntimeException
import org.apache.kafka.common.KafkaException
import org.apache.kafka.common.errors.InterruptException

import scala.concurrent.Future

trait MainKafkaLoggingEvents {

  @throws[AvroRuntimeException]
  @throws[InterruptException]
  @throws[IllegalStateException]
  @throws[KafkaException]
  def sendEventToKafka(
      kafkaLoggingEventsProperties: KafkaLoggingEventsProperties,
      kafkaLoggedEvent: KafkaLoggedEvent
  ): Future[Unit]
}
