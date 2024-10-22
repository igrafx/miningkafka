package com.igrafx.kafka.sink.aggregation.domain.entities

final case class TaskProperties(
    connectorName: String,
    topicOut: String,
    aggregationColumnName: String,
    elementNumberThreshold: Int,
    valuePatternThreshold: String,
    timeoutInSecondsThreshold: Int,
    bootstrapServers: String,
    schemaRegistryUrl: String,
    maxMessageBytes: Int
) {
  require(
    topicOut.nonEmpty
      && elementNumberThreshold > 0
      && timeoutInSecondsThreshold > 0
      && maxMessageBytes > 0
  )
}
