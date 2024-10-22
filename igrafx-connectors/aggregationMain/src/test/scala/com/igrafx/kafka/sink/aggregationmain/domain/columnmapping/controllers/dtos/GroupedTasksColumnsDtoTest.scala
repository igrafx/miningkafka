package com.igrafx.kafka.sink.aggregationmain.domain.columnmapping.controllers.dtos

import com.igrafx.kafka.sink.aggregationmain.Constants
import com.igrafx.kafka.sink.aggregationmain.domain.columnmapping.domain.entities.{
  IndexMock,
  ValidGroupedTasksColumnsMock
}
import com.igrafx.kafka.sink.aggregationmain.domain.entities.ColumnsNumber
import com.igrafx.kafka.sink.aggregationmain.domain.enums.ConnectorPropertiesEnum
import com.igrafx.kafka.sink.aggregationmain.domain.exceptions.{IndexException, InvalidPropertyValueException}
import core.UnitTestSpec
import org.apache.kafka.common.config.{AbstractConfig, ConfigValue}
import org.json4s.ParserUtil.ParseException
import org.mockito.ArgumentMatchers
import org.mockito.Mockito.{doAnswer, times, verify}
import org.slf4j.Logger

final class GroupedTasksColumnsDtoTest extends UnitTestSpec {
  describe("toEntity") {
    it("should return a ValidGroupedTasksColumns") {
      val index1 = 1
      val index2 = 2
      val index3 = 3
      val groupedTasksColumns = Set(index1, index2, index3)
      val groupedTasksColumnsDto = GroupedTasksColumnsDto(groupedTasksColumns)
      val columnsNumber = ColumnsNumber(5)
      implicit val log: Logger = mock[Logger]

      val expectedResult =
        new ValidGroupedTasksColumnsMock()
          .setGroupedTasksColumns(
            Set(
              new IndexMock().setIndex(index1).build(),
              new IndexMock().setIndex(index2).build(),
              new IndexMock().setIndex(index3).build()
            )
          )
          .build()

      val res = groupedTasksColumnsDto.toEntity(columnsNumber)

      assert(res == expectedResult)
    }
    it("should throw an InvalidPropertyValueException if IndexException is received") {
      val index1 = 1
      val index2 = 2
      val index3 = 4
      val groupedTasksColumns = Set(index1, index2, index3)
      val groupedTasksColumnsDto = GroupedTasksColumnsDto(groupedTasksColumns)
      val columnsNumber = ColumnsNumber(3)
      implicit val log: Logger = mock[Logger]

      assertThrows[InvalidPropertyValueException] {
        groupedTasksColumnsDto.toEntity(columnsNumber)
      }.map { assert =>
        verify(log, times(1))
          .error(ArgumentMatchers.any[String], ArgumentMatchers.any[IndexException])

        assert
      }
    }
  }

  describe("fromConnectorConfig") {
    it("should return an Option[GroupedTasksColumnsDto]") {
      val connectorConfigMock = mock[AbstractConfig]

      val index1 = 1
      val index2 = 2
      val index3 = 3
      val groupedTasksColumnPropertyValue = s"[$index1, $index2, $index3]"
      doAnswer(_ => groupedTasksColumnPropertyValue)
        .when(connectorConfigMock)
        .getString(ConnectorPropertiesEnum.columnMappingGroupedTasksColumnsProperty.toStringDescription)

      val expectedResult = Some(GroupedTasksColumnsDto(Set(index1, index2, index3)))

      val res = GroupedTasksColumnsDto.fromConnectorConfig(connectorConfigMock)

      verify(connectorConfigMock, times(1)).getString(
        ConnectorPropertiesEnum.columnMappingGroupedTasksColumnsProperty.toStringDescription
      )

      assert(res == expectedResult)
    }
  }

  describe("fromGroupedTasksColumnsConfig") {
    it("should return an Option[GroupedTasksColumnsDto]") {
      val columnMappingGroupedTasksColumnsCfg = mock[ConfigValue]

      val index1 = 1
      val index2 = 2
      val index3 = 3
      val groupedTasksColumnPropertyValue = s"[$index1, $index2, $index3]"
      doAnswer(_ => groupedTasksColumnPropertyValue).when(columnMappingGroupedTasksColumnsCfg).value()

      val expectedResult = Some(GroupedTasksColumnsDto(Set(index1, index2, index3)))

      val res = GroupedTasksColumnsDto.fromGroupedTasksColumnsConfig(columnMappingGroupedTasksColumnsCfg)

      verify(columnMappingGroupedTasksColumnsCfg, times(1)).value()

      assert(res == expectedResult)
    }
  }

  describe("getGroupedTasksColumnsDto") {
    it("should return None if property is not defined") {
      val groupedTasksColumnsPropertyValue = Constants.columnMappingStringDefaultValue

      val expectedResult = None
      val res = GroupedTasksColumnsDto.getGroupedTasksColumnsDto(groupedTasksColumnsPropertyValue)

      assert(res == expectedResult)
    }
    it("should return an Option[GroupedTasksColumnsDto]") {
      val index1 = 1
      val index2 = 2
      val index3 = 3
      val groupedTasksColumnPropertyValue = s"[$index1, $index2, $index3]"

      val expectedResult = Some(GroupedTasksColumnsDto(Set(index1, index2, index3)))

      val res = GroupedTasksColumnsDto.getGroupedTasksColumnsDto(groupedTasksColumnPropertyValue)

      assert(res == expectedResult)
    }
    it("should throw a ParseException is the json property value is not parsable") {
      val index1 = 1
      val index2 = 2
      val index3 = 3
      val groupedTasksColumnPropertyValue = s"($index1, $index2, $index3)"

      assertThrows[ParseException](GroupedTasksColumnsDto.getGroupedTasksColumnsDto(groupedTasksColumnPropertyValue))
    }
  }
}
