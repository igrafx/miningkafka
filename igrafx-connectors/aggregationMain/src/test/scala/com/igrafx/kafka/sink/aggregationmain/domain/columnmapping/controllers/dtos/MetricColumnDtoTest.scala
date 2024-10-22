package com.igrafx.kafka.sink.aggregationmain.domain.columnmapping.controllers.dtos

import com.igrafx.kafka.sink.aggregationmain.domain.columnmapping.controllers.dtos.enums.MetricAggregationDto
import com.igrafx.kafka.sink.aggregationmain.domain.columnmapping.domain.entities.{
  IndexMock,
  MetricAggregationInformationMock,
  ValidMetricColumnMock
}
import com.igrafx.kafka.sink.aggregationmain.domain.entities.ColumnsNumber
import com.igrafx.kafka.sink.aggregationmain.domain.entities.mocks.NonEmptyStringMock
import com.igrafx.kafka.sink.aggregationmain.domain.exceptions.{
  IndexException,
  InvalidPropertyValueException,
  NonEmptyStringException,
  WrongAggregationException
}
import core.UnitTestSpec
import org.mockito.ArgumentMatchers
import org.mockito.Mockito.{times, verify}
import org.slf4j.Logger

class MetricColumnDtoTest extends UnitTestSpec {
  describe("toEntity") {
    it("should return a ValidMetricColumn") {
      val columnsNumber = ColumnsNumber(4)
      implicit val log: Logger = mock[Logger]

      val columnIndex = 3
      val name = "testMetric"
      val unit = None
      val isCaseScope = false
      val aggregation = Some(MetricAggregationDto.FIRST)
      val groupedTasksAggregation = Some(MetricAggregationDto.LAST)
      val metricColumnDto = MetricColumnDto(
        columnIndex = columnIndex,
        name = name,
        unit = unit,
        isCaseScope = isCaseScope,
        aggregation = aggregation,
        groupedTasksAggregation = groupedTasksAggregation
      )

      val expectedResult =
        new ValidMetricColumnMock()
          .setColumnIndex(new IndexMock().setIndex(columnIndex).build())
          .setName(new NonEmptyStringMock().setStringValue(name).build())
          .setUnit(unit)
          .setAggregationInformation(
            new MetricAggregationInformationMock()
              .setIsCaseScope(isCaseScope)
              .setAggregation(aggregation.map(_.toEntity))
              .build()
          )
          .setGroupedTasksAggregation(groupedTasksAggregation.map(_.toEntity))
          .build()

      val res = metricColumnDto.toEntity(columnsNumber)

      assert(res == expectedResult)
    }
    it("should throw an InvalidPropertyValueException if IndexException is received") {
      val columnsNumber = ColumnsNumber(4)
      implicit val log: Logger = mock[Logger]

      val columnIndex = 5
      val name = "testMetric"
      val unit = None
      val isCaseScope = false
      val aggregation = None
      val groupedTasksAggregation = None
      val metricColumnDto = MetricColumnDto(
        columnIndex = columnIndex,
        name = name,
        unit = unit,
        isCaseScope = isCaseScope,
        aggregation = aggregation,
        groupedTasksAggregation = groupedTasksAggregation
      )

      assertThrows[InvalidPropertyValueException] {
        metricColumnDto.toEntity(columnsNumber)
      }.map { assert =>
        verify(log, times(1))
          .error(ArgumentMatchers.any[String], ArgumentMatchers.any[IndexException])

        assert
      }
    }
    it("should throw an InvalidPropertyValueException if NonEmptyStringException is received") {
      val columnsNumber = ColumnsNumber(4)
      implicit val log: Logger = mock[Logger]

      val columnIndex = 3
      val name = ""
      val unit = None
      val isCaseScope = false
      val aggregation = None
      val groupedTasksAggregation = None
      val metricColumnDto = MetricColumnDto(
        columnIndex = columnIndex,
        name = name,
        unit = unit,
        isCaseScope = isCaseScope,
        aggregation = aggregation,
        groupedTasksAggregation = groupedTasksAggregation
      )

      assertThrows[InvalidPropertyValueException] {
        metricColumnDto.toEntity(columnsNumber)
      }.map { assert =>
        verify(log, times(1))
          .error(ArgumentMatchers.any[String], ArgumentMatchers.any[NonEmptyStringException])

        assert
      }
    }
    it("should throw an InvalidPropertyValueException if WrongAggregationException is received") {
      val columnsNumber = ColumnsNumber(4)
      implicit val log: Logger = mock[Logger]

      val columnIndex = 3
      val name = "testMetric"
      val unit = None
      val isCaseScope = true
      val aggregation = None
      val groupedTasksAggregation = None
      val metricColumnDto = MetricColumnDto(
        columnIndex = columnIndex,
        name = name,
        unit = unit,
        isCaseScope = isCaseScope,
        aggregation = aggregation,
        groupedTasksAggregation = groupedTasksAggregation
      )

      assertThrows[InvalidPropertyValueException] {
        metricColumnDto.toEntity(columnsNumber)
      }.map { assert =>
        verify(log, times(1))
          .error(ArgumentMatchers.any[String], ArgumentMatchers.any[WrongAggregationException])

        assert
      }
    }
  }
}
