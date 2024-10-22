package com.igrafx.kafka.sink.aggregationmain.domain.columnmapping.controllers.dtos

import com.igrafx.kafka.sink.aggregationmain.domain.columnmapping.domain.entities.{IndexMock, ValidCaseIdColumnMock}
import com.igrafx.kafka.sink.aggregationmain.domain.entities.ColumnsNumber
import com.igrafx.kafka.sink.aggregationmain.domain.enums.ConnectorPropertiesEnum
import com.igrafx.kafka.sink.aggregationmain.domain.exceptions.{IndexException, InvalidPropertyValueException}
import core.UnitTestSpec
import org.apache.kafka.common.config.{AbstractConfig, ConfigValue}
import org.mockito.ArgumentMatchers
import org.mockito.Mockito.{doAnswer, times, verify}
import org.slf4j.Logger

final class CaseIdColumnDtoTest extends UnitTestSpec {
  describe("toEntity") {
    it("should return a ValidCaseIdColumn") {
      val columnIndex = 1
      val caseIdColumnDto = CaseIdColumnDto(columnIndex = columnIndex)
      val columnsNumber = ColumnsNumber(3)
      implicit val log: Logger = mock[Logger]

      val expectedResult =
        new ValidCaseIdColumnMock().setColumnIndex(new IndexMock().setIndex(columnIndex).build()).build()

      val res = caseIdColumnDto.toEntity(columnsNumber)

      assert(res == expectedResult)
    }
    it("should throw an InvalidPropertyValueException if IndexException is received") {
      val caseIdColumnDto = CaseIdColumnDto(columnIndex = 5)
      val columnsNumber = ColumnsNumber(3)
      implicit val log: Logger = mock[Logger]

      assertThrows[InvalidPropertyValueException] {
        caseIdColumnDto.toEntity(columnsNumber)
      }.map { assert =>
        verify(log, times(1))
          .error(ArgumentMatchers.any[String], ArgumentMatchers.any[IndexException])

        assert
      }
    }
  }

  describe("fromConnectorConfig") {
    it("should return an CaseIdColumnDto") {
      val connectorConfigMock = mock[AbstractConfig]

      val caseIdColumnPropertyValue = 1
      doAnswer(_ => caseIdColumnPropertyValue)
        .when(connectorConfigMock)
        .getInt(ConnectorPropertiesEnum.columnMappingCaseIdColumnProperty.toStringDescription)

      val expectedResult = CaseIdColumnDto(caseIdColumnPropertyValue)

      val res = CaseIdColumnDto.fromConnectorConfig(connectorConfigMock)

      verify(connectorConfigMock, times(1)).getInt(
        ConnectorPropertiesEnum.columnMappingCaseIdColumnProperty.toStringDescription
      )

      assert(res == expectedResult)
    }
  }

  describe("fromCaseIdConfig") {
    it("should return an CaseIdColumnDto") {
      val columnMappingCaseIdColumnCfgMock = mock[ConfigValue]

      val caseIdColumnPropertyValueAsString = "1"
      doAnswer(_ => caseIdColumnPropertyValueAsString).when(columnMappingCaseIdColumnCfgMock).value()

      val expectedResult = CaseIdColumnDto(caseIdColumnPropertyValueAsString.toInt)

      val res = CaseIdColumnDto.fromCaseIdConfig(columnMappingCaseIdColumnCfgMock)

      verify(columnMappingCaseIdColumnCfgMock, times(1)).value()

      assert(res == expectedResult)
    }
    it("should throw an InvalidPropertyValueException if property value is not parsable to int") {
      val columnMappingCaseIdColumnCfgMock = mock[ConfigValue]

      val caseIdColumnPropertyValueAsString = "test"
      doAnswer(_ => caseIdColumnPropertyValueAsString).when(columnMappingCaseIdColumnCfgMock).value()

      assertThrows[InvalidPropertyValueException] {
        CaseIdColumnDto.fromCaseIdConfig(columnMappingCaseIdColumnCfgMock)
      }.map { assert =>
        verify(columnMappingCaseIdColumnCfgMock, times(1)).value()

        assert
      }
    }
  }
}
