package com.igrafx.kafka.sink.aggregation.domain.entities

import com.igrafx.kafka.sink.aggregation.domain.entities.mocks.TaskPropertiesMock
import core.UnitTestSpec

class TaskPropertiesTest extends UnitTestSpec {
  describe("TaskProperties") {
    it("should throw an exception if topicOut is empty") {
      assertThrows[IllegalArgumentException] {
        new TaskPropertiesMock().setTopicOut("").build()
      }
    }
    it("should throw an exception if elementNumberThreshold is not > 0") {
      assertThrows[IllegalArgumentException] {
        new TaskPropertiesMock().setElementNumberThreshold(0).build()
      }
    }
    it("should throw an exception if timeoutInSecondsThreshold is not > 0") {
      assertThrows[IllegalArgumentException] {
        new TaskPropertiesMock().setTimeoutInSecondsThreshold(0).build()
      }
    }
    it("should throw an exception if maxMessageBytes is not > 0") {
      assertThrows[IllegalArgumentException] {
        new TaskPropertiesMock().setMaxMessageBytes(0).build()
      }
    }
  }
}
