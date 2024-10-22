package com.igrafx.kafka.sink.aggregationmain.domain.columnmapping.controllers.dtos

import com.igrafx.kafka.sink.aggregationmain.domain.columnmapping.domain.entities.{Index, ValidCaseIdColumn}
import com.igrafx.kafka.sink.aggregationmain.domain.entities.ColumnsNumber
import com.igrafx.kafka.sink.aggregationmain.domain.enums.ConnectorPropertiesEnum
import com.igrafx.kafka.sink.aggregationmain.domain.exceptions.{IndexException, InvalidPropertyValueException}
import org.apache.kafka.common.config.{AbstractConfig, ConfigValue}
import org.slf4j.Logger

import scala.util.{Failure, Success, Try}

final case class CaseIdColumnDto(columnIndex: Int) {
  @throws[InvalidPropertyValueException]
  def toEntity(columnsNumber: ColumnsNumber)(implicit log: Logger): ValidCaseIdColumn = {
    Try {
      ValidCaseIdColumn(columnIndex = Index(columnIndex, columnsNumber, isOnlyIndexProperty = true))
    } match {
      case Success(validCaseIdColumn) => validCaseIdColumn
      case Failure(exception: IndexException) =>
        val message =
          s"Issue with the ${ConnectorPropertiesEnum.columnMappingCaseIdColumnProperty} property. " +
            s"The column has a wrong index : ${exception.getMessage}"
        log.error(message.replaceAll("[\r\n]", ""), exception)
        throw InvalidPropertyValueException(message)
      case Failure(exception) =>
        val message =
          s"Unexpected issue with the ${ConnectorPropertiesEnum.columnMappingCaseIdColumnProperty} " +
            s"property. Please check the logs of the connector for more information."
        log.error(message.replaceAll("[\r\n]", ""), exception)
        throw InvalidPropertyValueException(message)
    }
  }
}

object CaseIdColumnDto {
  def fromConnectorConfig(connectorConfig: AbstractConfig): CaseIdColumnDto = {
    val caseIdColumnPropertyValue =
      connectorConfig.getInt(ConnectorPropertiesEnum.columnMappingCaseIdColumnProperty.toStringDescription)

    CaseIdColumnDto(caseIdColumnPropertyValue)
  }

  @throws[InvalidPropertyValueException]
  def fromCaseIdConfig(
      columnMappingCaseIdCfg: ConfigValue
  ): CaseIdColumnDto = {
    val caseIdColumnPropertyValueAsString = columnMappingCaseIdCfg.value().toString
    Try {
      caseIdColumnPropertyValueAsString.toInt
    } match {
      case Failure(_) =>
        throw InvalidPropertyValueException(
          s"Issue with the ${ConnectorPropertiesEnum.columnMappingCaseIdColumnProperty} property. The value " +
            s"'$caseIdColumnPropertyValueAsString' is not parsable to Int (you should try with an index between 0 " +
            s"and the value of the ${ConnectorPropertiesEnum.csvFieldsNumberProperty} property)"
        )
      case Success(caseIdColumnPropertyValue) => CaseIdColumnDto(caseIdColumnPropertyValue)
    }
  }
}
