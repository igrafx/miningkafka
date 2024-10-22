package com.igrafx.kafka.sink.aggregationmain.domain.entities

import com.igrafx.kafka.sink.aggregationmain.domain.entities.mocks.{ColumnsNumberMock, PropertiesMock}
import core.UnitTestSpec

class PropertiesTest extends UnitTestSpec {
  describe("Properties") {
    it("should throw an exception if csvFieldsNumber is not >= 3") {
      assertThrows[IllegalArgumentException] {
        new PropertiesMock().setCsvFieldsNumber(new ColumnsNumberMock().setNumber(2).build()).build()
      }
    }
    it("should throw an exception if retentionTimeInDay is not > 0") {
      assertThrows[IllegalArgumentException] {
        new PropertiesMock().setRetentionTimeInDay(0).build()
      }
    }
    it("should throw an exception if csvSeparator.length != 1") {
      assertThrows[IllegalArgumentException] {
        new PropertiesMock().setCsvSeparator("").build()
      }
      assertThrows[IllegalArgumentException] {
        new PropertiesMock().setCsvSeparator(",,").build()
      }
    }
    it("should throw an exception if csvQuote.length != 1") {
      assertThrows[IllegalArgumentException] {
        new PropertiesMock().setCsvQuote("").build()
      }
      assertThrows[IllegalArgumentException] {
        new PropertiesMock().setCsvQuote("\"\"").build()
      }
    }
    it("should throw an exception if elementNumberThreshold is not > 0") {
      assertThrows[IllegalArgumentException] {
        new PropertiesMock().setElementNumberThreshold(0).build()
      }
    }
    it("should throw an exception if timeoutInSecondsThreshold is not > 0") {
      assertThrows[IllegalArgumentException] {
        new PropertiesMock().setTimeoutInSecondsThreshold(0).build()
      }
    }
  }
}
