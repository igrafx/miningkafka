package com.igrafx.kafka.sink.aggregationmain.domain.entities

final case class KafkaRecordInformation(topic: String, partition: String, offset: String)
