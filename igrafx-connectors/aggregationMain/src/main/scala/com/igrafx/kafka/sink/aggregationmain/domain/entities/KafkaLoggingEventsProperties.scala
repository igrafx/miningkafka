package com.igrafx.kafka.sink.aggregationmain.domain.entities

final case class KafkaLoggingEventsProperties(
    topic: String,
    bootstrapServers: String,
    schemaRegistryUrl: String
) {
  require(
    topic.nonEmpty
      && bootstrapServers.nonEmpty
      && schemaRegistryUrl.nonEmpty
  )
}
