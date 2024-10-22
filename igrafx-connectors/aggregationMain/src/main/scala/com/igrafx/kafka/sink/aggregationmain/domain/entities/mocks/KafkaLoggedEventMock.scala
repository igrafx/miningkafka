package com.igrafx.kafka.sink.aggregationmain.domain.entities.mocks

import com.igrafx.core.Mock
import com.igrafx.kafka.sink.aggregationmain.domain.entities.{KafkaEventPayload, KafkaLoggedEvent}

class KafkaLoggedEventMock extends Mock[KafkaLoggedEvent] {
  private var eventType: String = "pushFile"
  private var igrafxProject: String = "igrafx_project_example"
  private var eventDate: Long = 451435151
  private var eventSequenceId: String = "connectorName_topic_partition_offset"
  private var payload: KafkaEventPayload = new KafkaEventPayloadPushFileMock().setDate(451435151).build()

  def setEventType(eventType: String): KafkaLoggedEventMock = {
    this.eventType = eventType
    this
  }

  def setIGrafxProject(igrafxProject: String): KafkaLoggedEventMock = {
    this.igrafxProject = igrafxProject
    this
  }

  def setEventDate(eventDate: Long): KafkaLoggedEventMock = {
    this.eventDate = eventDate
    this
  }

  def setEventSequenceId(eventSequenceId: String): KafkaLoggedEventMock = {
    this.eventSequenceId = eventSequenceId
    this
  }

  def setPayload(payload: KafkaEventPayload): KafkaLoggedEventMock = {
    this.payload = payload
    this
  }

  override def build(): KafkaLoggedEvent = {
    KafkaLoggedEvent(
      eventType = eventType,
      igrafxProject = igrafxProject,
      eventDate = eventDate,
      eventSequenceId = eventSequenceId,
      payload = payload
    )
  }
}
