package com.igrafx.kafka.sink.aggregation.domain.usecases.interfaces

import com.igrafx.kafka.sink.aggregation.domain.entities.PartitionTracker
import com.igrafx.kafka.sink.aggregation.domain.exceptions.MaxMessageBytesException
import io.confluent.kafka.schemaregistry.client.rest.exceptions.RestClientException
import org.apache.avro.{AvroRuntimeException, Schema}
import org.apache.kafka.common.KafkaException
import org.apache.kafka.common.errors.{InterruptException, InvalidConfigurationException, SerializationException}

import java.io.IOException

trait KafkaProducerSend {

  @throws[IOException]
  @throws[RestClientException]
  @throws[AvroRuntimeException]
  @throws[InterruptException]
  @throws[IllegalStateException]
  @throws[KafkaException]
  @throws[MaxMessageBytesException]
  @throws[SerializationException]
  @throws[InvalidConfigurationException]
  def sendRecord(
      partitionTracker: PartitionTracker,
      topicOut: String,
      aggregationColumnName: String,
      bootstrapServers: String,
      schemaRegistryUrl: String,
      maxMessageBytes: Int
  ): Unit
}
