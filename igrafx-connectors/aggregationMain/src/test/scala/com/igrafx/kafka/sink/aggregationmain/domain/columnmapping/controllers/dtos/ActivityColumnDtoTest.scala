package com.igrafx.kafka.sink.aggregationmain.domain.columnmapping.controllers.dtos

import com.igrafx.kafka.sink.aggregationmain.domain.columnmapping.domain.entities.{IndexMock, ValidActivityColumnMock}
import com.igrafx.kafka.sink.aggregationmain.domain.entities.ColumnsNumber
import com.igrafx.kafka.sink.aggregationmain.domain.enums.ConnectorPropertiesEnum
import com.igrafx.kafka.sink.aggregationmain.domain.exceptions.{IndexException, InvalidPropertyValueException}
import core.UnitTestSpec
import org.apache.kafka.common.config.{AbstractConfig, ConfigValue}
import org.mockito.ArgumentMatchers
import org.mockito.Mockito.{doAnswer, times, verify}
import org.slf4j.Logger

final class ActivityColumnDtoTest extends UnitTestSpec {
  describe("toEntity") {
    it("should return a ValidActivityColumn") {
      val columnIndex = 1
      val activityColumnDto = ActivityColumnDto(columnIndex = columnIndex)
      val columnsNumber = ColumnsNumber(3)
      implicit val log: Logger = mock[Logger]

      val expectedResult =
        new ValidActivityColumnMock().setColumnIndex(new IndexMock().setIndex(columnIndex).build()).build()

      val res = activityColumnDto.toEntity(columnsNumber)

      assert(res == expectedResult)
    }
    it("should throw an InvalidPropertyValueException if IndexException is received") {
      val activityColumnDto = ActivityColumnDto(columnIndex = 5)
      val columnsNumber = ColumnsNumber(3)
      implicit val log: Logger = mock[Logger]

      assertThrows[InvalidPropertyValueException] {
        activityColumnDto.toEntity(columnsNumber)
      }.map { assert =>
        verify(log, times(1))
          .error(ArgumentMatchers.any[String], ArgumentMatchers.any[IndexException])

        assert
      }
    }
  }

  describe("fromConnectorConfig") {
    it("should return an ActivityColumnDto") {
      val connectorConfigMock = mock[AbstractConfig]

      val activityColumnPropertyValue = 1
      doAnswer(_ => activityColumnPropertyValue)
        .when(connectorConfigMock)
        .getInt(ConnectorPropertiesEnum.columnMappingActivityColumnProperty.toStringDescription)

      val expectedResult = ActivityColumnDto(activityColumnPropertyValue)

      val res = ActivityColumnDto.fromConnectorConfig(connectorConfigMock)

      verify(connectorConfigMock, times(1)).getInt(
        ConnectorPropertiesEnum.columnMappingActivityColumnProperty.toStringDescription
      )

      assert(res == expectedResult)
    }
  }

  describe("fromActivityConfig") {
    it("should return an ActivityColumnDto") {
      val columnMappingActivityColumnCfgMock = mock[ConfigValue]

      val activityColumnPropertyValueAsString = "1"
      doAnswer(_ => activityColumnPropertyValueAsString).when(columnMappingActivityColumnCfgMock).value()

      val expectedResult = ActivityColumnDto(activityColumnPropertyValueAsString.toInt)

      val res = ActivityColumnDto.fromActivityConfig(columnMappingActivityColumnCfgMock)

      verify(columnMappingActivityColumnCfgMock, times(1)).value()

      assert(res == expectedResult)
    }
    it("should throw an InvalidPropertyValueException if property value is not parsable to int") {
      val columnMappingActivityColumnCfgMock = mock[ConfigValue]

      val activityColumnPropertyValueAsString = "test"
      doAnswer(_ => activityColumnPropertyValueAsString).when(columnMappingActivityColumnCfgMock).value()

      assertThrows[InvalidPropertyValueException] {
        ActivityColumnDto.fromActivityConfig(columnMappingActivityColumnCfgMock)
      }.map { assert =>
        verify(columnMappingActivityColumnCfgMock, times(1)).value()

        assert
      }
    }
  }
}
