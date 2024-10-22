package com.igrafx.kafka.sink.aggregationmain.domain.columnmapping.controllers.dtos

import com.igrafx.kafka.sink.aggregationmain.domain.columnmapping.controllers.dtos.enums.{
  DimensionAggregationDto,
  GroupedTasksDimensionAggregationDto
}
import com.igrafx.kafka.sink.aggregationmain.domain.columnmapping.domain.entities.{
  DimensionAggregationInformationMock,
  IndexMock,
  ValidDimensionColumnMock
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

final class DimensionColumnDtoTest extends UnitTestSpec {
  describe("toEntity") {
    it("should return a ValidDimensionColumn") {
      val columnsNumber = ColumnsNumber(4)
      implicit val log: Logger = mock[Logger]

      val columnIndex = 3
      val name = "testDimension"
      val isCaseScope = false
      val aggregation = Some(DimensionAggregationDto.FIRST)
      val groupedTasksAggregation = Some(GroupedTasksDimensionAggregationDto.LAST)
      val dimensionColumnDto = DimensionColumnDto(
        columnIndex = columnIndex,
        name = name,
        isCaseScope = isCaseScope,
        aggregation = aggregation,
        groupedTasksAggregation = groupedTasksAggregation
      )

      val expectedResult =
        new ValidDimensionColumnMock()
          .setColumnIndex(new IndexMock().setIndex(columnIndex).build())
          .setName(new NonEmptyStringMock().setStringValue(name).build())
          .setAggregationInformation(
            new DimensionAggregationInformationMock()
              .setIsCaseScope(isCaseScope)
              .setAggregation(aggregation.map(_.toEntity))
              .build()
          )
          .setGroupedTasksAggregation(groupedTasksAggregation.map(_.toEntity))
          .build()

      val res = dimensionColumnDto.toEntity(columnsNumber)

      assert(res == expectedResult)
    }
    it("should throw an InvalidPropertyValueException if IndexException is received") {
      val columnsNumber = ColumnsNumber(4)
      implicit val log: Logger = mock[Logger]

      val columnIndex = 5
      val name = "testDimension"
      val isCaseScope = false
      val aggregation = None
      val groupedTasksAggregation = None
      val dimensionColumnDto = DimensionColumnDto(
        columnIndex = columnIndex,
        name = name,
        isCaseScope = isCaseScope,
        aggregation = aggregation,
        groupedTasksAggregation = groupedTasksAggregation
      )

      assertThrows[InvalidPropertyValueException] {
        dimensionColumnDto.toEntity(columnsNumber)
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
      val isCaseScope = false
      val aggregation = None
      val groupedTasksAggregation = None
      val dimensionColumnDto = DimensionColumnDto(
        columnIndex = columnIndex,
        name = name,
        isCaseScope = isCaseScope,
        aggregation = aggregation,
        groupedTasksAggregation = groupedTasksAggregation
      )

      assertThrows[InvalidPropertyValueException] {
        dimensionColumnDto.toEntity(columnsNumber)
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
      val name = "testDimension"
      val isCaseScope = true
      val aggregation = None
      val groupedTasksAggregation = None
      val dimensionColumnDto = DimensionColumnDto(
        columnIndex = columnIndex,
        name = name,
        isCaseScope = isCaseScope,
        aggregation = aggregation,
        groupedTasksAggregation = groupedTasksAggregation
      )

      assertThrows[InvalidPropertyValueException] {
        dimensionColumnDto.toEntity(columnsNumber)
      }.map { assert =>
        verify(log, times(1))
          .error(ArgumentMatchers.any[String], ArgumentMatchers.any[WrongAggregationException])

        assert
      }
    }
  }
}
