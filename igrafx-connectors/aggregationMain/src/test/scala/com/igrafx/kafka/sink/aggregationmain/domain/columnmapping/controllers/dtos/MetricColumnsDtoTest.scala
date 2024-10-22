package com.igrafx.kafka.sink.aggregationmain.domain.columnmapping.controllers.dtos

import com.igrafx.kafka.sink.aggregationmain.Constants
import com.igrafx.kafka.sink.aggregationmain.domain.columnmapping.controllers.dtos.enums.MetricAggregationDto
import com.igrafx.kafka.sink.aggregationmain.domain.enums.ConnectorPropertiesEnum
import core.UnitTestSpec
import org.apache.kafka.common.config.{AbstractConfig, ConfigValue}
import org.json4s.ParserUtil.ParseException
import org.mockito.Mockito.{doAnswer, times, verify}

final class MetricColumnsDtoTest extends UnitTestSpec {
  describe("fromConnectorConfig") {
    it("should return a Set[MetricColumnDto]") {
      val connectorConfigMock = mock[AbstractConfig]

      val metric1Index = 4
      val metric1Name = "Price"
      val metric1Unit = "Euros"
      val metric1IsCaseScope = true
      val metric1Aggregation = MetricAggregationDto.MIN
      val metric1GroupedTasksAggregation = MetricAggregationDto.MAX
      val metric2Index = 5
      val metric2Name = "Region"
      val metric2IsCaseScope = false
      val metric2GroupedTasksAggregation = MetricAggregationDto.MIN
      val metricPropertyValue =
        s"[{\"columnIndex\": $metric1Index, \"name\": \"$metric1Name\", \"unit\": \"$metric1Unit\", \"isCaseScope\": $metric1IsCaseScope, \"aggregation\": \"$metric1Aggregation\", \"groupedTasksAggregation\": \"$metric1GroupedTasksAggregation\"},{\"columnIndex\": $metric2Index, \"name\": \"$metric2Name\", \"isCaseScope\": $metric2IsCaseScope, \"groupedTasksAggregation\": \"$metric2GroupedTasksAggregation\"}]"
      doAnswer(_ => metricPropertyValue)
        .when(connectorConfigMock)
        .getString(ConnectorPropertiesEnum.columnMappingMetricColumnsProperty.toStringDescription)

      val expectedResult = Set(MetricColumnDto(metric1Index, metric1Name, Some(metric1Unit),metric1IsCaseScope, Some(metric1Aggregation), Some(metric1GroupedTasksAggregation)), MetricColumnDto(metric2Index, metric2Name, None, metric2IsCaseScope, None, Some(metric2GroupedTasksAggregation)))

      val res = MetricColumnsDto.fromConnectorConfig(connectorConfigMock)

      verify(connectorConfigMock, times(1)).getString(
        ConnectorPropertiesEnum.columnMappingMetricColumnsProperty.toStringDescription
      )

      assert(res == expectedResult)
    }
  }

  describe("fromMetricConfig") {
    it("should return a Set[MetricColumnDto]") {
      val columnMappingMetricCfgMock = mock[ConfigValue]

      val metric1Index = 4
      val metric1Name = "Country"
      val metric1Unit = "Euros"
      val metric1IsCaseScope = true
      val metric1Aggregation = MetricAggregationDto.MIN
      val metric1GroupedTasksAggregation = MetricAggregationDto.MAX
      val metric2Index = 5
      val metric2Name = "Region"
      val metric2IsCaseScope = false
      val metric2GroupedTasksAggregation = MetricAggregationDto.MIN
      val metricPropertyValue =
        s"[{\"columnIndex\": $metric1Index, \"name\": \"$metric1Name\", \"unit\": \"$metric1Unit\", \"isCaseScope\": $metric1IsCaseScope, \"aggregation\": \"$metric1Aggregation\", \"groupedTasksAggregation\": \"$metric1GroupedTasksAggregation\"},{\"columnIndex\": $metric2Index, \"name\": \"$metric2Name\", \"isCaseScope\": $metric2IsCaseScope, \"groupedTasksAggregation\": \"$metric2GroupedTasksAggregation\"}]"
      doAnswer(_ => metricPropertyValue).when(columnMappingMetricCfgMock).value()

      val expectedResult = Set(MetricColumnDto(metric1Index, metric1Name, Some(metric1Unit),metric1IsCaseScope, Some(metric1Aggregation), Some(metric1GroupedTasksAggregation)), MetricColumnDto(metric2Index, metric2Name, None, metric2IsCaseScope, None, Some(metric2GroupedTasksAggregation)))

      val res = MetricColumnsDto.fromMetricConfig(columnMappingMetricCfgMock)

      verify(columnMappingMetricCfgMock, times(1)).value()

      assert(res == expectedResult)
    }
  }

  describe("getMetricColumnsDto") {
    it("should return an empty set if property is not defined") {
      val metricPropertyValue = Constants.columnMappingStringDefaultValue

      val expectedResult = Set.empty[MetricColumnDto]
      val res = MetricColumnsDto.getMetricColumnsDto(metricPropertyValue)

      assert(res == expectedResult)
    }
    it("should return a Set[MetricColumnDto]") {
      val metric1Index = 4
      val metric1Name = "Country"
      val metric1Unit = "Euros"
      val metric1IsCaseScope = true
      val metric1Aggregation = MetricAggregationDto.MIN
      val metric1GroupedTasksAggregation = MetricAggregationDto.MAX
      val metric2Index = 5
      val metric2Name = "Region"
      val metric2IsCaseScope = false
      val metric2GroupedTasksAggregation = MetricAggregationDto.MIN
      val metricPropertyValue =
        s"[{\"columnIndex\": $metric1Index, \"name\": \"$metric1Name\", \"unit\": \"$metric1Unit\", \"isCaseScope\": $metric1IsCaseScope, \"aggregation\": \"$metric1Aggregation\", \"groupedTasksAggregation\": \"$metric1GroupedTasksAggregation\"},{\"columnIndex\": $metric2Index, \"name\": \"$metric2Name\", \"isCaseScope\": $metric2IsCaseScope, \"groupedTasksAggregation\": \"$metric2GroupedTasksAggregation\"}]"

      val expectedResult = Set(MetricColumnDto(metric1Index, metric1Name, Some(metric1Unit),metric1IsCaseScope, Some(metric1Aggregation), Some(metric1GroupedTasksAggregation)), MetricColumnDto(metric2Index, metric2Name, None, metric2IsCaseScope, None, Some(metric2GroupedTasksAggregation)))

      val res = MetricColumnsDto.getMetricColumnsDto(metricPropertyValue)

      assert(res == expectedResult)
    }
    it("should throw a ParseException is the json property value is not parsable") {
      val metric1Index = 4
      val metric1Name = "Country"
      val metric1Unit = "Euros"
      val metric1IsCaseScope = true
      val metric1Aggregation = MetricAggregationDto.MIN
      val metric1GroupedTasksAggregation = MetricAggregationDto.MAX
      val metricPropertyValue =
        s"({\"columnIndex\": $metric1Index, \"name\": \"$metric1Name\", \"unit\": \"$metric1Unit\", \"isCaseScope\": $metric1IsCaseScope, \"aggregation\": \"$metric1Aggregation\", \"groupedTasksAggregation\": \"$metric1GroupedTasksAggregation\"})"

      assertThrows[ParseException](MetricColumnsDto.getMetricColumnsDto(metricPropertyValue))
    }
  }
}
