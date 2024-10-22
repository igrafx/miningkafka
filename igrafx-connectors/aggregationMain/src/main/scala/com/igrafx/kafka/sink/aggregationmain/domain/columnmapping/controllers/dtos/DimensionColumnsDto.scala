package com.igrafx.kafka.sink.aggregationmain.domain.columnmapping.controllers.dtos

import com.igrafx.kafka.sink.aggregationmain.Constants
import com.igrafx.kafka.sink.aggregationmain.domain.columnmapping.controllers.Json4sSupport
import com.igrafx.kafka.sink.aggregationmain.domain.enums.ConnectorPropertiesEnum
import org.apache.kafka.common.config.{AbstractConfig, ConfigValue}
import org.json4s.ParserUtil.ParseException
import org.json4s.native.JsonMethods.parse

object DimensionColumnsDto extends Json4sSupport {
  @throws[ParseException]
  def fromConnectorConfig(
      connectorConfig: AbstractConfig
  ): Set[DimensionColumnDto] = {
    val dimensionPropertyValue =
      connectorConfig.getString(
        ConnectorPropertiesEnum.columnMappingDimensionColumnsProperty.toStringDescription
      )

    getDimensionColumnsDto(dimensionPropertyValue)
  }

  @throws[ParseException]
  def fromDimensionConfig(
      columnMappingDimensionCfg: ConfigValue
  ): Set[DimensionColumnDto] = {
    val dimensionPropertyValue = columnMappingDimensionCfg.value().toString

    getDimensionColumnsDto(dimensionPropertyValue)
  }

  @throws[ParseException]
  private[dtos] def getDimensionColumnsDto(dimensionPropertyValue: String): Set[DimensionColumnDto] = {
    dimensionPropertyValue match {
      case Constants.columnMappingStringDefaultValue => Set.empty[DimensionColumnDto]
      case dimensionPropertyValue => parse(dimensionPropertyValue).extract[Set[DimensionColumnDto]]
    }
  }
}
