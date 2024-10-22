package com.igrafx.kafka.sink.aggregationmain.domain.columnmapping.domain.entities

import com.igrafx.kafka.sink.aggregationmain.Constants
import com.igrafx.kafka.sink.aggregationmain.domain.exceptions.{
  DefaultNonEmptyStringException,
  IncoherentNonEmptyStringException
}

final case class NonEmptyString(stringValue: String) {
  require(stringValue.nonEmpty)
}

object NonEmptyString {
  @throws[DefaultNonEmptyStringException]
  @throws[IncoherentNonEmptyStringException]
  def apply(stringValue: String, isOnlyStringProperty: Boolean): NonEmptyString = {
    stringValue match {
      case stringValue if (stringValue == Constants.columnMappingStringDefaultValue) && isOnlyStringProperty =>
        throw DefaultNonEmptyStringException("Property is not defined")
      case stringValue if stringValue.isEmpty =>
        throw IncoherentNonEmptyStringException(
          s"The value '$stringValue' is not correct for a column mapping non empty character, String can't be empty"
        )
      case _ => new NonEmptyString(stringValue)
    }
  }
}
