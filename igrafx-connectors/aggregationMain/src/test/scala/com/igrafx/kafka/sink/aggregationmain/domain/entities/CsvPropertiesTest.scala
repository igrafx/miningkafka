package com.igrafx.kafka.sink.aggregationmain.domain.entities

import com.igrafx.kafka.sink.aggregationmain.domain.entities.mocks.{ColumnsNumberMock, CsvPropertiesMock}
import core.UnitTestSpec

class CsvPropertiesTest extends UnitTestSpec {
  describe("CsvProperties") {
    it("should throw an exception if csvFieldsNumber is not >= 3") {
      assertThrows[IllegalArgumentException] {
        new CsvPropertiesMock().setCsvFieldsNumber(new ColumnsNumberMock().setNumber(2).build()).build()
      }
    }
    it("should throw an exception if csvSeparator.length != 1") {
      assertThrows[IllegalArgumentException] {
        new CsvPropertiesMock().setCsvSeparator("").build()
      }
      assertThrows[IllegalArgumentException] {
        new CsvPropertiesMock().setCsvSeparator(",,").build()
      }
    }
    it("should throw an exception if csvQuote.length != 1") {
      assertThrows[IllegalArgumentException] {
        new CsvPropertiesMock().setCsvQuote("").build()
      }
      assertThrows[IllegalArgumentException] {
        new CsvPropertiesMock().setCsvQuote("\"\"").build()
      }
    }
  }
}
