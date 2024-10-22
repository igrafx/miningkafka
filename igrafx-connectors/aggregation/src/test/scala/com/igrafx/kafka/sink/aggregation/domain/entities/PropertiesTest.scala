package com.igrafx.kafka.sink.aggregation.domain.entities

import com.igrafx.kafka.sink.aggregation.domain.entities.mocks.PropertiesMock
import core.UnitTestSpec

class PropertiesTest extends UnitTestSpec {
  describe("Properties") {
    it("should throw an exception if topicOut is empty") {
      assertThrows[IllegalArgumentException] {
        new PropertiesMock().setTopicOut("").build()
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
