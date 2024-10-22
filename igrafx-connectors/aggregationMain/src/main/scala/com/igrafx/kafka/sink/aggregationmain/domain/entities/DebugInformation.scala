package com.igrafx.kafka.sink.aggregationmain.domain.entities

final case class DebugInformation(topic: String, partition: Int, offsetFrom: Long, offsetTo: Long)
