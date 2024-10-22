package com.igrafx.kafka.sink.aggregationmain.domain.entities.mocks

import com.igrafx.core.Mock
import com.igrafx.kafka.sink.aggregationmain.domain.columnmapping.domain.entities.NonEmptyString

final class NonEmptyStringMock extends Mock[NonEmptyString] {
  private var stringValue: String = "propertyValue"

  def setStringValue(stringValue: String): NonEmptyStringMock = {
    this.stringValue = stringValue
    this
  }

  override def build(): NonEmptyString = {
    new NonEmptyString(stringValue = stringValue)
  }
}
