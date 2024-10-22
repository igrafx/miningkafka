package com.igrafx.utils

import io.confluent.connect.avro.AvroData
import org.apache.kafka.connect.errors.DataException
import org.apache.kafka.connect.sink.SinkRecord
import org.slf4j.Logger

import scala.util.{Failure, Success, Try}

object AvroUtils {
  private val avroData = new AvroData(10000)

  /** Method used to transform a Connect data (the record's value) to an Avro Data
    *
    * @param record The record whose value we want to convert from Connect to Avro
    * @param log The Logger to use
    *
    * @throws DataException If there is a problem between the record's value and its schema
    */
  def fromConnectToAvro(
      record: SinkRecord,
      log: Logger
  ): AnyRef = {
    Try {
      avroData.fromConnectData(record.valueSchema(), record.value())
    } match {
      case Success(value) => value
      case Failure(exception: DataException) =>
        log.error(
          s"[fromConnectToAvro] Issue between the value and its schema, can't transform the Connect data to an Avro Data. Kafka record information : topic ${record.topic}, partition ${record
            .kafkaPartition()}, offset ${record.kafkaOffset()}".replaceAll("[\r\n]", ""),
          exception
        )
        throw exception
      case Failure(exception: Throwable) =>
        log.error(
          s"Unexpected exception, can't transform the Connect data to an Avro Data. Kafka record information : topic ${record.topic}, partition ${record
            .kafkaPartition()}, offset ${record.kafkaOffset()}".replaceAll("[\r\n]", ""),
          exception
        )
        throw exception
    }
  }
}
