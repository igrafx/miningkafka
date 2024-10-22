package com.igrafx.kafka.sink.aggregationmain.domain.entities

import com.igrafx.kafka.sink.aggregationmain.domain.entities.mocks.KafkaLoggingEventsPropertiesMock
import core.UnitTestSpec

class KafkaLoggingEventsPropertiesTest extends UnitTestSpec {
  describe("KafkaLoggingEventsProperties") {
    it("should throw an exception if topic is empty") {
      assertThrows[IllegalArgumentException] {
        new KafkaLoggingEventsPropertiesMock().setTopic("").build()
      }
    }
    it("should throw an exception if bootstrapServers is empty") {
      assertThrows[IllegalArgumentException] {
        new KafkaLoggingEventsPropertiesMock().setBootstrapServers("").build()
      }
    }
    it("should throw an exception if schemaRegistryUrl is empty") {
      assertThrows[IllegalArgumentException] {
        new KafkaLoggingEventsPropertiesMock().setSchemaRegistryUrl("").build()
      }
    }
  }
}
