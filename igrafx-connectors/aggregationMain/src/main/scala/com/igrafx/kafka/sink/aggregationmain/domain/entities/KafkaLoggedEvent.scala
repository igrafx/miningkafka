package com.igrafx.kafka.sink.aggregationmain.domain.entities

final case class KafkaLoggedEvent(
    eventType: String,
    igrafxProject: String,
    eventDate: Long,
    eventSequenceId: String,
    payload: KafkaEventPayload
)
