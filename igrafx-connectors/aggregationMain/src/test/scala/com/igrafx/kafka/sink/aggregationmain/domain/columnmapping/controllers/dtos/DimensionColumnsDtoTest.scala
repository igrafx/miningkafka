package com.igrafx.kafka.sink.aggregationmain.domain.columnmapping.controllers.dtos

import com.igrafx.kafka.sink.aggregationmain.Constants
import com.igrafx.kafka.sink.aggregationmain.domain.columnmapping.controllers.dtos.enums.{DimensionAggregationDto, GroupedTasksDimensionAggregationDto}
import com.igrafx.kafka.sink.aggregationmain.domain.enums.ConnectorPropertiesEnum
import core.UnitTestSpec
import org.apache.kafka.common.config.{AbstractConfig, ConfigValue}
import org.json4s.ParserUtil.ParseException
import org.mockito.Mockito.{doAnswer, times, verify}

final class DimensionColumnsDtoTest extends UnitTestSpec {
  describe("fromConnectorConfig") {
    it("should return a Set[DimensionColumnDto]") {
      val connectorConfigMock = mock[AbstractConfig]

      val dimension1Index = 4
      val dimension1Name = "Country"
      val dimension1IsCaseScope = true
      val dimension1Aggregation = DimensionAggregationDto.FIRST
      val dimension1GroupedTasksAggregation = GroupedTasksDimensionAggregationDto.FIRST
      val dimension2Index = 5
      val dimension2Name = "Region"
      val dimension2IsCaseScope = false
      val dimension2GroupedTasksAggregation = GroupedTasksDimensionAggregationDto.LAST
      val dimensionPropertyValue =
        s"[{\"columnIndex\": $dimension1Index, \"name\": \"$dimension1Name\", \"isCaseScope\": $dimension1IsCaseScope, \"aggregation\": \"$dimension1Aggregation\", \"groupedTasksAggregation\": \"$dimension1GroupedTasksAggregation\"},{\"columnIndex\": $dimension2Index, \"name\": \"$dimension2Name\", \"isCaseScope\": $dimension2IsCaseScope, \"groupedTasksAggregation\": \"$dimension2GroupedTasksAggregation\"}]"
      doAnswer(_ => dimensionPropertyValue)
        .when(connectorConfigMock)
        .getString(ConnectorPropertiesEnum.columnMappingDimensionColumnsProperty.toStringDescription)

      val expectedResult = Set(DimensionColumnDto(dimension1Index, dimension1Name, dimension1IsCaseScope, Some(dimension1Aggregation), Some(dimension1GroupedTasksAggregation)), DimensionColumnDto(dimension2Index, dimension2Name, dimension2IsCaseScope, None, Some(dimension2GroupedTasksAggregation)))

      val res = DimensionColumnsDto.fromConnectorConfig(connectorConfigMock)

      verify(connectorConfigMock, times(1)).getString(
        ConnectorPropertiesEnum.columnMappingDimensionColumnsProperty.toStringDescription
      )

      assert(res == expectedResult)
    }
  }

  describe("fromDimensionConfig") {
    it("should return a Set[DimensionColumnDto]") {
      val columnMappingDimensionCfgMock = mock[ConfigValue]

      val dimension1Index = 4
      val dimension1Name = "Country"
      val dimension1IsCaseScope = true
      val dimension1Aggregation = DimensionAggregationDto.FIRST
      val dimension1GroupedTasksAggregation = GroupedTasksDimensionAggregationDto.FIRST
      val dimension2Index = 5
      val dimension2Name = "Region"
      val dimension2IsCaseScope = false
      val dimension2GroupedTasksAggregation = GroupedTasksDimensionAggregationDto.LAST
      val dimensionPropertyValue =
        s"[{\"columnIndex\": $dimension1Index, \"name\": \"$dimension1Name\", \"isCaseScope\": $dimension1IsCaseScope, \"aggregation\": \"$dimension1Aggregation\", \"groupedTasksAggregation\": \"$dimension1GroupedTasksAggregation\"},{\"columnIndex\": $dimension2Index, \"name\": \"$dimension2Name\", \"isCaseScope\": $dimension2IsCaseScope, \"groupedTasksAggregation\": \"$dimension2GroupedTasksAggregation\"}]"
      doAnswer(_ => dimensionPropertyValue).when(columnMappingDimensionCfgMock).value()

      val expectedResult = Set(DimensionColumnDto(dimension1Index, dimension1Name, dimension1IsCaseScope, Some(dimension1Aggregation), Some(dimension1GroupedTasksAggregation)), DimensionColumnDto(dimension2Index, dimension2Name, dimension2IsCaseScope, None, Some(dimension2GroupedTasksAggregation)))

      val res = DimensionColumnsDto.fromDimensionConfig(columnMappingDimensionCfgMock)

      verify(columnMappingDimensionCfgMock, times(1)).value()

      assert(res == expectedResult)
    }
  }

  describe("getDimensionColumnsDto") {
    it("should return an empty set if property is not defined") {
      val dimensionPropertyValue = Constants.columnMappingStringDefaultValue

      val expectedResult = Set.empty[DimensionColumnDto]
      val res = DimensionColumnsDto.getDimensionColumnsDto(dimensionPropertyValue)

      assert(res == expectedResult)
    }
    it("should return a Set[DimensionColumnDto]") {
      val dimension1Index = 4
      val dimension1Name = "Country"
      val dimension1IsCaseScope = true
      val dimension1Aggregation = DimensionAggregationDto.FIRST
      val dimension1GroupedTasksAggregation = GroupedTasksDimensionAggregationDto.FIRST
      val dimension2Index = 5
      val dimension2Name = "Region"
      val dimension2IsCaseScope = false
      val dimension2GroupedTasksAggregation = GroupedTasksDimensionAggregationDto.LAST
      val dimensionPropertyValue =
        s"[{\"columnIndex\": $dimension1Index, \"name\": \"$dimension1Name\", \"isCaseScope\": $dimension1IsCaseScope, \"aggregation\": \"$dimension1Aggregation\", \"groupedTasksAggregation\": \"$dimension1GroupedTasksAggregation\"},{\"columnIndex\": $dimension2Index, \"name\": \"$dimension2Name\", \"isCaseScope\": $dimension2IsCaseScope, \"groupedTasksAggregation\": \"$dimension2GroupedTasksAggregation\"}]"

      val expectedResult = Set(DimensionColumnDto(dimension1Index, dimension1Name, dimension1IsCaseScope, Some(dimension1Aggregation), Some(dimension1GroupedTasksAggregation)), DimensionColumnDto(dimension2Index, dimension2Name, dimension2IsCaseScope, None, Some(dimension2GroupedTasksAggregation)))

      val res = DimensionColumnsDto.getDimensionColumnsDto(dimensionPropertyValue)

      assert(res == expectedResult)
    }
    it("should throw a ParseException is the json property value is not parsable") {
      val dimension1Index = 4
      val dimension1Name = "Country"
      val dimension1IsCaseScope = true
      val dimension1Aggregation = DimensionAggregationDto.FIRST
      val dimension1GroupedTasksAggregation = GroupedTasksDimensionAggregationDto.FIRST
      val dimensionPropertyValue =
        s"({\"columnIndex\": $dimension1Index, \"name\": \"$dimension1Name\", \"isCaseScope\": $dimension1IsCaseScope, \"aggregation\": \"$dimension1Aggregation\", \"groupedTasksAggregation\": \"$dimension1GroupedTasksAggregation\"})"

      assertThrows[ParseException](DimensionColumnsDto.getDimensionColumnsDto(dimensionPropertyValue))
    }
  }
}
