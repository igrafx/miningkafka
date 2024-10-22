package com.igrafx.kafka.sink.aggregationmain.domain.columnmapping.controllers.dtos

import com.igrafx.kafka.sink.aggregationmain.domain.columnmapping.domain.entities.{Index, ValidActivityColumn}
import com.igrafx.kafka.sink.aggregationmain.domain.entities.ColumnsNumber
import com.igrafx.kafka.sink.aggregationmain.domain.enums.ConnectorPropertiesEnum
import com.igrafx.kafka.sink.aggregationmain.domain.exceptions.{IndexException, InvalidPropertyValueException}
import org.apache.kafka.common.config.{AbstractConfig, ConfigValue}
import org.slf4j.Logger

import scala.util.{Failure, Success, Try}

final case class ActivityColumnDto(columnIndex: Int) {
  @throws[InvalidPropertyValueException]
  def toEntity(columnsNumber: ColumnsNumber)(implicit log: Logger): ValidActivityColumn = {
    Try {
      ValidActivityColumn(columnIndex = Index(columnIndex, columnsNumber, isOnlyIndexProperty = true))
    } match {
      case Success(validActivityColumn) => validActivityColumn
      case Failure(
            exception: IndexException
          ) =>
        val message =
          s"Issue with the ${ConnectorPropertiesEnum.columnMappingActivityColumnProperty} property. " +
            s"The column has a wrong index : ${exception.getMessage}"
        log.error(message.replaceAll("[\r\n]", ""), exception)
        throw InvalidPropertyValueException(message)
      case Failure(exception) =>
        val message =
          s"Unexpected issue with the ${ConnectorPropertiesEnum.columnMappingActivityColumnProperty} " +
            s"property. Please check the logs of the connector for more information."
        log.error(message.replaceAll("[\r\n]", ""), exception)
        throw InvalidPropertyValueException(message)
    }
  }
}

object ActivityColumnDto {
  def fromConnectorConfig(connectorConfig: AbstractConfig): ActivityColumnDto = {
    val activityColumnPropertyValue =
      connectorConfig.getInt(ConnectorPropertiesEnum.columnMappingActivityColumnProperty.toStringDescription)

    ActivityColumnDto(activityColumnPropertyValue)
  }

  @throws[InvalidPropertyValueException]
  def fromActivityConfig(
      columnMappingActivityCfg: ConfigValue
  ): ActivityColumnDto = {
    val activityColumnPropertyValueAsString = columnMappingActivityCfg.value().toString
    Try {
      activityColumnPropertyValueAsString.toInt
    } match {
      case Failure(_) =>
        throw InvalidPropertyValueException(
          s"Issue with the ${ConnectorPropertiesEnum.columnMappingActivityColumnProperty} property. The value " +
            s"'$activityColumnPropertyValueAsString' is not parsable to Int (you should try with an index between 0 " +
            s"and the value of the ${ConnectorPropertiesEnum.csvFieldsNumberProperty} property)"
        )
      case Success(activityColumnPropertyValue) => ActivityColumnDto(activityColumnPropertyValue)
    }
  }
}
