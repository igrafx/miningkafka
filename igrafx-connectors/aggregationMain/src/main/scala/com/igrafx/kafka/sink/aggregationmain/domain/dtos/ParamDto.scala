package com.igrafx.kafka.sink.aggregationmain.domain.dtos

import com.igrafx.kafka.sink.aggregationmain.domain.entities.Param

case class ParamDto(QUOTE: Option[Boolean], TEXT: Option[String], COLUMNID: Option[Int]) {
  @throws[IllegalArgumentException](
    cause =
      "Options can't be empty" // Option is only used to match the ksqlDB generated schema, but it shouldn't be None
  )
  def toParam: Param = {
    val paramQuote = QUOTE match {
      case None => throw new IllegalArgumentException("One of the QUOTE parameter of the event has a null value")
      case Some(paramDtoQuote) => paramDtoQuote
    }
    val paramText = TEXT match {
      case None =>
        throw new IllegalArgumentException(
          "One of the TEXT parameter of the event has a null value"
        )
      case Some(paramDtoText) => paramDtoText
    }
    val paramColumnId = COLUMNID match {
      case None => throw new IllegalArgumentException("One of the COLUMNID parameter of the event has a null value")
      case Some(paramDtoColumnId) => paramDtoColumnId
    }
    Param(
      QUOTE = paramQuote,
      TEXT = paramText,
      COLUMNID = paramColumnId
    )
  }
}
