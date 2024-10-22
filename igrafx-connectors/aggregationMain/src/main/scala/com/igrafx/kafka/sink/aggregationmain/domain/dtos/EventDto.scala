package com.igrafx.kafka.sink.aggregationmain.domain.dtos

import com.igrafx.kafka.sink.aggregationmain.domain.entities.{Event, Param}

final case class EventDto(DATAARRAY: Option[List[Option[ParamDto]]]) {
  @throws[IllegalArgumentException](
    cause =
      "Options can't be empty" // Option is only used to match the ksqlDB generated schema, but it shouldn't be None
  )
  def toEvent: Event = {
    val event: List[Param] = DATAARRAY match {
      case None => throw new IllegalArgumentException("The entire DATAARRAY (one event) is null")
      case Some(line) =>
        line.map {
          case None => throw new IllegalArgumentException("At least one event column is null")
          case Some(param) => param.toParam
        }
    }

    Event(event = event)
  }
}
