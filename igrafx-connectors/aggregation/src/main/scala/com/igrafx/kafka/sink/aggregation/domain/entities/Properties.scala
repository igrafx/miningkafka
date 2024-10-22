package com.igrafx.kafka.sink.aggregation.domain.entities

final case class Properties(
    connectorName: String,
    topicOut: String,
    aggregationColumnName: String,
    elementNumberThreshold: Int,
    valuePatternThreshold: String,
    timeoutInSecondsThreshold: Int,
    bootstrapServers: String,
    schemaRegistryUrl: String
) {
  require(
    topicOut.nonEmpty
      && elementNumberThreshold > 0
      && timeoutInSecondsThreshold > 0
  )
}
