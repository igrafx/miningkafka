package com.igrafx.kafka.sink.aggregationmain.domain.entities.mocks

import com.igrafx.core.Mock
import com.igrafx.kafka.sink.aggregationmain.domain.entities.DebugInformation

class DebugInformationMock extends Mock[DebugInformation] {
  private var topic: String = "topicExample"
  private var partition: Int = 0
  private var offsetFrom: Long = -1
  private var offsetTo: Long = -1

  def setTopic(topic: String): DebugInformationMock = {
    this.topic = topic
    this
  }

  def setPartition(partition: Int): DebugInformationMock = {
    this.partition = partition
    this
  }

  def setOffsetFrom(offsetFrom: Long): DebugInformationMock = {
    this.offsetFrom = offsetFrom
    this
  }

  def setOffsetTo(offsetTo: Long): DebugInformationMock = {
    this.offsetTo = offsetTo
    this
  }

  override def build(): DebugInformation = {
    DebugInformation(
      topic = topic,
      partition = partition,
      offsetFrom = offsetFrom,
      offsetTo = offsetTo
    )
  }
}
