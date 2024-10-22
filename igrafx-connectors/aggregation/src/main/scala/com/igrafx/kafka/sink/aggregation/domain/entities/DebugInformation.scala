package com.igrafx.kafka.sink.aggregation.domain.entities

final case class DebugInformation(topic: String, partition: Int, offsetFrom: Long, offsetTo: Long)
