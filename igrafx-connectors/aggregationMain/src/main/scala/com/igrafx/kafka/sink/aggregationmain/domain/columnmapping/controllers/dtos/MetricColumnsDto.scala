package com.igrafx.kafka.sink.aggregationmain.domain.columnmapping.controllers.dtos

import com.igrafx.kafka.sink.aggregationmain.Constants
import com.igrafx.kafka.sink.aggregationmain.domain.columnmapping.controllers.Json4sSupport
import com.igrafx.kafka.sink.aggregationmain.domain.enums.ConnectorPropertiesEnum
import org.apache.kafka.common.config.{AbstractConfig, ConfigValue}
import org.json4s.ParserUtil.ParseException
import org.json4s.native.JsonMethods.parse

object MetricColumnsDto extends Json4sSupport {
  @throws[ParseException]
  def fromConnectorConfig(
      connectorConfig: AbstractConfig
  ): Set[MetricColumnDto] = {
    val metricPropertyValue =
      connectorConfig.getString(
        ConnectorPropertiesEnum.columnMappingMetricColumnsProperty.toStringDescription
      )

    getMetricColumnsDto(metricPropertyValue)
  }

  @throws[ParseException]
  def fromMetricConfig(
      columnMappingMetricCfg: ConfigValue
  ): Set[MetricColumnDto] = {
    val metricPropertyValue = columnMappingMetricCfg.value().toString

    getMetricColumnsDto(metricPropertyValue)
  }

  private[dtos] def getMetricColumnsDto(metricPropertyValue: String): Set[MetricColumnDto] = {
    metricPropertyValue match {
      case Constants.columnMappingStringDefaultValue => Set.empty[MetricColumnDto]
      case metricPropertyValue => parse(metricPropertyValue).extract[Set[MetricColumnDto]]
    }
  }
}
