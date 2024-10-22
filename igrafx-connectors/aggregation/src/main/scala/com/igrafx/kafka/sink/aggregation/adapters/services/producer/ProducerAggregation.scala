package com.igrafx.kafka.sink.aggregation.adapters.services.producer

import org.apache.kafka.clients.producer.{KafkaProducer, Producer, ProducerConfig}

import java.util.Properties
import org.apache.avro.generic.GenericRecord
import org.apache.kafka.common.KafkaException

object ProducerAggregation {
  @throws[KafkaException](cause = "Failed to construct kafka producer")
  def createProducer(
      bootstrapServers: String,
      schemaRegistryUrl: String,
      maxMessageBytes: Int
  ): Producer[String, GenericRecord] = {
    val props: Properties = new Properties()
    props.put(ProducerConfig.MAX_REQUEST_SIZE_CONFIG, maxMessageBytes)
    props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers)
    props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, "org.apache.kafka.common.serialization.StringSerializer")
    props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, classOf[io.confluent.kafka.serializers.KafkaAvroSerializer])
    props.put("schema.registry.url", schemaRegistryUrl)
    new KafkaProducer[String, GenericRecord](props)
  }
}
