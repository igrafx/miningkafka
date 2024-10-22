package com.igrafx.kafka.sink.aggregationmain.domain.entities.mocks

import com.igrafx.core.Mock
import com.igrafx.kafka.sink.aggregationmain.domain.entities.KafkaLoggingEventsProperties

class KafkaLoggingEventsPropertiesMock extends Mock[KafkaLoggingEventsProperties] {
  private var topic: String = "topicLoggingEvents"
  private var bootstrapServers: String = "broker:29092"
  private var schemaRegistryUrl: String = "http://schema-registry:8081"

  def setTopic(topic: String): KafkaLoggingEventsPropertiesMock = {
    this.topic = topic
    this
  }

  def setBootstrapServers(bootstrapServers: String): KafkaLoggingEventsPropertiesMock = {
    this.bootstrapServers = bootstrapServers
    this
  }

  def setSchemaRegistryUrl(schemaRegistryUrl: String): KafkaLoggingEventsPropertiesMock = {
    this.schemaRegistryUrl = schemaRegistryUrl
    this
  }

  override def build(): KafkaLoggingEventsProperties = {
    KafkaLoggingEventsProperties(
      topic = topic,
      bootstrapServers = bootstrapServers,
      schemaRegistryUrl = schemaRegistryUrl
    )
  }
}
