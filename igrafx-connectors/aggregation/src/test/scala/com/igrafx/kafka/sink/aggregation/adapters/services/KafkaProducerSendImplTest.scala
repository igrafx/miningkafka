package com.igrafx.kafka.sink.aggregation.adapters.services

import com.igrafx.kafka.sink.aggregation.domain.entities.mocks.DebugInformationMock
import com.igrafx.kafka.sink.aggregation.domain.exceptions.MaxMessageBytesException
import core.UnitTestSpec
import org.apache.avro.{Schema, SchemaBuilder}
import org.apache.avro.generic.{GenericData, GenericRecord}
import org.apache.kafka.clients.producer.ProducerRecord
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito
import org.mockito.Mockito.{spy, times, verify}
import org.slf4j.LoggerFactory

class KafkaProducerSendImplTest extends UnitTestSpec {
  private val schema: Schema = SchemaBuilder.record("test").fields().endRecord()

  describe("sendAggregationRec") {
    it("should call sendCorrectRecord if the size of the record is inferior than the limit") {
      val limit = 1000000
      val recordSize = 900000
      val topicOut = "topicOut"
      val nameIndex = "topic_0"
      val debugInformation = new DebugInformationMock().build()

      val kafkaProducerSendImplSpy: KafkaProducerSendImpl =
        spy(new KafkaProducerSendImpl(LoggerFactory.getLogger(getClass)))

      val avroRecord = new GenericData.Record(schema)
      val record = new ProducerRecord[String, GenericRecord](topicOut, nameIndex, avroRecord)
      Mockito
        .doAnswer(_ => avroRecord)
        .when(kafkaProducerSendImplSpy)
        .createAvroRecordForAggregation(any(), any(), any(), any(), any())

      Mockito
        .doAnswer(_ => recordSize)
        .when(kafkaProducerSendImplSpy)
        .getRecordSize(any(), any(), any())

      Mockito
        .doAnswer(_ => ())
        .when(kafkaProducerSendImplSpy)
        .validateRecordToSend(any(), any(), any(), any())

      Mockito
        .doAnswer(_ => Seq.empty)
        .when(kafkaProducerSendImplSpy)
        .sendCorrectRecord(any(), any(), any(), any(), any(), any(), any())

      kafkaProducerSendImplSpy.sendAggregationRec(
        aggregationSeq = Seq("test"),
        numberOfElements = 1,
        schema = schema,
        nameIndex = "topic_0",
        topicOut = "topicOut",
        aggregationColumnName = "LINEAG",
        bootstrapServers = "broker",
        schemaRegistryUrl = "schema",
        maxMessageBytes = limit,
        debugInformation = debugInformation
      )

      verify(kafkaProducerSendImplSpy, times(1))
        .sendCorrectRecord(
          record = record,
          topicOut = topicOut,
          bootstrapServers = "broker",
          schemaRegistryUrl = "schema",
          maxMessageBytes = limit,
          recordSize = recordSize,
          debugInformation = debugInformation
        )

      verify(kafkaProducerSendImplSpy, times(0))
        .sendHugeRecord(any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any())

      assert(true)
    }

    it("should call sendHugeRecord if the size of the record is superior than the limit") {
      val limit = 1000000
      val recordSize = 1100000
      val topicOut = "topicOut"
      val nameIndex = "topic_0"
      val debugInformation = new DebugInformationMock().build()
      val kafkaProducerSendImplSpy: KafkaProducerSendImpl =
        spy(new KafkaProducerSendImpl(LoggerFactory.getLogger(getClass)))

      val avroRecord = new GenericData.Record(schema)
      Mockito
        .doAnswer(_ => avroRecord)
        .when(kafkaProducerSendImplSpy)
        .createAvroRecordForAggregation(any(), any(), any(), any(), any())

      Mockito
        .doAnswer(_ => recordSize)
        .when(kafkaProducerSendImplSpy)
        .getRecordSize(any(), any(), any())

      Mockito
        .doAnswer(_ => ())
        .when(kafkaProducerSendImplSpy)
        .validateRecordToSend(any(), any(), any(), any())

      Mockito
        .doAnswer(_ => Seq.empty)
        .when(kafkaProducerSendImplSpy)
        .sendHugeRecord(any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any())

      kafkaProducerSendImplSpy.sendHugeRecord(
        aggregationSeq = Seq("test"),
        numberOfElements = 1,
        schema = schema,
        nameIndex = nameIndex,
        topicOut = topicOut,
        aggregationColumnName = "LINEAG",
        bootstrapServers = "broker",
        schemaRegistryUrl = "schema",
        maxMessageBytes = limit,
        recordSize = recordSize,
        debugInformation = debugInformation
      )

      verify(kafkaProducerSendImplSpy, times(0))
        .sendCorrectRecord(any(), any(), any(), any(), any(), any(), any())

      verify(kafkaProducerSendImplSpy, times(1))
        .sendHugeRecord(
          aggregationSeq = Seq("test"),
          numberOfElements = 1,
          schema = schema,
          nameIndex = "topic_0",
          topicOut = "topicOut",
          aggregationColumnName = "LINEAG",
          bootstrapServers = "broker",
          schemaRegistryUrl = "schema",
          maxMessageBytes = limit,
          recordSize = recordSize,
          debugInformation = debugInformation
        )

      assert(true)
    }
  }

  describe("sendHugeRecord") {
    it("should throw a MaxMessageBytesException if a single element already has a size too high to be sent") {
      val kafkaProducerSendImpl: KafkaProducerSendImpl = new KafkaProducerSendImpl(LoggerFactory.getLogger(getClass))
      val limit = 1000
      val recordSize = 1100

      assertThrows[MaxMessageBytesException] {
        kafkaProducerSendImpl.sendHugeRecord(
          Seq("test"),
          1,
          schema,
          "topic_0",
          "topicOut",
          "LINEAG",
          "broker",
          "schema",
          limit,
          recordSize,
          new DebugInformationMock().build()
        )
      }
    }

    it("should cut the aggregation to send smaller messages") {
      val kafkaProducerSendImplSpy: KafkaProducerSendImpl =
        spy(new KafkaProducerSendImpl(LoggerFactory.getLogger(getClass)))
      val limit = 1000
      val recordSize = 1250
      val elementSize = 600
      val cutRecordSize = elementSize
      val emptyRecordSize = 50
      val elementNumber = 2
      val inputSeq = Seq("value1", "value2")
      val topicOut = "topicOut"
      val nameIndex = "topic_0"
      val aggregationColumnName = "LINEAG"
      val bootstrapServers = "broker"
      val schemaRegistryUrl = "schema"
      val debugInformation = new DebugInformationMock().setOffsetFrom(0).setOffsetTo(1).build()
      val updatedDebugInformation = new DebugInformationMock().setOffsetFrom(0).setOffsetTo(0).build()

      Mockito
        .doAnswer(_ => emptyRecordSize)
        .when(kafkaProducerSendImplSpy)
        .getEmptyRecordSize(schema, topicOut, aggregationColumnName, schemaRegistryUrl, debugInformation)

      Mockito
        .doAnswer(_ => 1)
        .when(kafkaProducerSendImplSpy)
        .getAwaitedNumberOfElementsWithMargin(
          maxMessageBytes = limit,
          emptyRecordSize = emptyRecordSize,
          averageElementSize = elementSize,
          numberOfElements = elementNumber
        )

      Mockito
        .doAnswer(_ => new GenericData.Record(schema))
        .when(kafkaProducerSendImplSpy)
        .createAvroRecordForAggregation(any(), any(), any(), any(), any())

      Mockito
        .doAnswer(_ => cutRecordSize)
        .when(kafkaProducerSendImplSpy)
        .getRecordSize(any(), any(), any())

      Mockito
        .doAnswer(_ => ())
        .when(kafkaProducerSendImplSpy)
        .validateRecordToSend(any(), any(), any(), any())

      Mockito
        .doAnswer(_ => Seq.empty)
        .when(kafkaProducerSendImplSpy)
        .sendCorrectRecord(any(), any(), any(), any(), any(), any(), any())

      val res = kafkaProducerSendImplSpy.sendHugeRecord(
        aggregationSeq = inputSeq,
        numberOfElements = 2,
        schema = schema,
        nameIndex = nameIndex,
        topicOut = topicOut,
        aggregationColumnName = aggregationColumnName,
        bootstrapServers = bootstrapServers,
        schemaRegistryUrl = schemaRegistryUrl,
        maxMessageBytes = limit,
        recordSize = recordSize,
        debugInformation = debugInformation
      )

      verify(kafkaProducerSendImplSpy, times(1))
        .sendAggregationRec(
          aggregationSeq = Seq("value1"),
          numberOfElements = 1,
          schema = schema,
          nameIndex = nameIndex,
          topicOut = topicOut,
          aggregationColumnName = aggregationColumnName,
          bootstrapServers = bootstrapServers,
          schemaRegistryUrl = schemaRegistryUrl,
          maxMessageBytes = limit,
          debugInformation = updatedDebugInformation
        )

      assert(res == Seq("value2"))
    }
  }
}
