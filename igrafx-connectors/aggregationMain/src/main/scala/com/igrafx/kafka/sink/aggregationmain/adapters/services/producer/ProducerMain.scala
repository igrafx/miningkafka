package com.igrafx.kafka.sink.aggregationmain.adapters.services.producer

import org.apache.avro.generic.GenericRecord
import org.apache.kafka.clients.producer.{KafkaProducer, Producer, ProducerConfig}
import org.apache.kafka.common.KafkaException

import java.util.Properties

object ProducerMain {
  @throws[KafkaException](cause = "Failed to construct kafka producer")
  def createProducer(bootstrapServers: String, schemaRegistryUrl: String): Producer[String, GenericRecord] = {
    val props: Properties = new Properties()
    props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers)
    props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, "org.apache.kafka.common.serialization.StringSerializer")
    props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, classOf[io.confluent.kafka.serializers.KafkaAvroSerializer])
    props.put("schema.registry.url", schemaRegistryUrl)
    new KafkaProducer[String, GenericRecord](props)
  }
}
