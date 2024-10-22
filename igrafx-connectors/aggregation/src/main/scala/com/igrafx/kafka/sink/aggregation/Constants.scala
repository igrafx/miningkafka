package com.igrafx.kafka.sink.aggregation

object Constants {
  val connectorNameProperty = "name"
  val topicsProperty = "topics"
  val schemaRegistryUrlProperty = "value.converter.schema.registry.url"
  val schemaRegistryNoSchemaForTopicErrorCode = 40401
  val maxMessageBytesConfigurationName = "max.message.bytes"
  val retentionConfigurationName = "retention.ms"
  val awaitedElementNumberInKafkaMessageMarginRatio = 0.95
}
