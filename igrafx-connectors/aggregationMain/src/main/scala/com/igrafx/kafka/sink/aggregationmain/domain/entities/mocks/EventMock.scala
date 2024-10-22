package com.igrafx.kafka.sink.aggregationmain.domain.entities.mocks

import com.igrafx.core.Mock
import com.igrafx.kafka.sink.aggregationmain.domain.entities.{Event, Param}

class EventMock extends Mock[Event] {
  private var event: List[Param] = List(new ParamMock().build())

  def setEvent(event: List[Param]): EventMock = {
    this.event = event
    this
  }

  override def build(): Event = {
    Event(event = event)
  }
}
