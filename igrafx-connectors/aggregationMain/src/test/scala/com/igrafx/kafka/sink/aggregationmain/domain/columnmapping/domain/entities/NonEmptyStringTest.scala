package com.igrafx.kafka.sink.aggregationmain.domain.columnmapping.domain.entities

import com.igrafx.kafka.sink.aggregationmain.Constants
import com.igrafx.kafka.sink.aggregationmain.domain.exceptions.{
  DefaultNonEmptyStringException,
  IncoherentNonEmptyStringException
}
import core.UnitTestSpec

final class NonEmptyStringTest extends UnitTestSpec {
  describe("NonEmptyString.apply(stringValue, isOnlyStringProperty)") {
    it("should return a NonEmptyString if stringValue is valid and isOnlyIndexProperty is false") {
      val stringValue = "test"
      val isOnlyIndexProperty = false

      val res = NonEmptyString(stringValue, isOnlyIndexProperty)

      assert(res.stringValue == stringValue)
    }
    it("should return a NonEmptyString if stringValue is valid and isOnlyIndexProperty is true") {
      val stringValue = "test"
      val isOnlyIndexProperty = true

      val res = NonEmptyString(stringValue, isOnlyIndexProperty)

      assert(res.stringValue == stringValue)
    }
    it(
      "should throw a DefaultNonEmptyStringException if isOnlyStringProperty is true and stringValue has a default value"
    ) {
      val stringValue = Constants.columnMappingStringDefaultValue
      val isOnlyIndexProperty = true

      assertThrows[DefaultNonEmptyStringException](NonEmptyString(stringValue, isOnlyIndexProperty))
    }
    it("should throw an IncoherentNonEmptyStringException if stringValue is empty") {
      val stringValue = ""
      val isOnlyIndexProperty = false

      assertThrows[IncoherentNonEmptyStringException](NonEmptyString(stringValue, isOnlyIndexProperty))
    }
  }
}
