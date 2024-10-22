package com.igrafx.kafka.sink.aggregationmain.domain.columnmapping.controllers.dtos

import com.igrafx.kafka.sink.aggregationmain.Constants
import com.igrafx.kafka.sink.aggregationmain.domain.enums.ConnectorPropertiesEnum
import com.igrafx.kafka.sink.aggregationmain.domain.exceptions.InvalidPropertyValueException
import org.apache.kafka.common.config.{AbstractConfig, ConfigValue}

object TimeColumnsDto {
  @throws[InvalidPropertyValueException]
  def fromConnectorConfig(
      connectorConfig: AbstractConfig
  ): Set[TimeColumnDto] = {
    val timePropertyValue =
      connectorConfig.getString(
        ConnectorPropertiesEnum.columnMappingTimeColumnsProperty.toStringDescription
      )

    getTimeColumnsDto(timePropertyValue)
  }

  @throws[InvalidPropertyValueException]
  def fromTimeConfig(columnMappingTimeCfg: ConfigValue): Set[TimeColumnDto] = {
    val timePropertyValue = columnMappingTimeCfg.value().toString

    getTimeColumnsDto(timePropertyValue)
  }

  @throws[InvalidPropertyValueException]
  private[dtos] def getTimeColumnsDto(timePropertyValue: String): Set[TimeColumnDto] = {
    timePropertyValue match {
      case Constants.columnMappingStringDefaultValue =>
        throw InvalidPropertyValueException(
          s"The ${ConnectorPropertiesEnum.columnMappingTimeColumnsProperty} property is not defined !"
        )
      case columnTimeAsStringNonDefault =>
        val tabTime = columnTimeAsStringNonDefault.split(Constants.delimiterForPropertiesWithListValue).toSeq
        val tabSize = tabTime.size
        if ((tabSize < 1) || (tabSize > 2)) {
          throw InvalidPropertyValueException(
            s"Issue with the ${ConnectorPropertiesEnum.columnMappingTimeColumnsProperty} property. " +
              s"The columns description '$timePropertyValue' has $tabSize columns, and should only have a number " +
              s"of columns between 1 and 2"
          )
        } else {
          tabTime.map { timeColumnAsString =>
            TimeColumnDto(timeColumnAsString)
          }.toSet
        }
    }
  }
}
