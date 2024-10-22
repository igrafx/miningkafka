package com.igrafx.kafka.sink.aggregationmain.domain.columnmapping.controllers.dtos

import com.igrafx.kafka.sink.aggregationmain.Constants
import com.igrafx.kafka.sink.aggregationmain.domain.columnmapping.controllers.Json4sSupport
import com.igrafx.kafka.sink.aggregationmain.domain.columnmapping.domain.entities.{Index, ValidGroupedTasksColumns}
import com.igrafx.kafka.sink.aggregationmain.domain.entities.ColumnsNumber
import com.igrafx.kafka.sink.aggregationmain.domain.enums.ConnectorPropertiesEnum
import com.igrafx.kafka.sink.aggregationmain.domain.exceptions.{IndexException, InvalidPropertyValueException}
import org.apache.kafka.common.config.{AbstractConfig, ConfigValue}
import org.json4s.ParserUtil.ParseException
import org.json4s.native.JsonMethods.parse
import org.slf4j.Logger

import scala.util.{Failure, Success, Try}

final case class GroupedTasksColumnsDto(groupedTasksColumns: Set[Int]) {
  @throws[InvalidPropertyValueException]
  def toEntity(columnsNumber: ColumnsNumber)(implicit log: Logger): ValidGroupedTasksColumns = {
    Try {
      ValidGroupedTasksColumns(
        groupedTasksColumns = groupedTasksColumns.map(index => Index(index, columnsNumber, isOnlyIndexProperty = false))
      )
    } match {
      case Success(validGroupedTasksColumns) => validGroupedTasksColumns
      case Failure(exception: IndexException) =>
        val message =
          s"Issue with the ${ConnectorPropertiesEnum.columnMappingGroupedTasksColumnsProperty} property. " +
            s"At least one index is wrong : ${exception.getMessage}"
        log.error(message.replaceAll("[\r\n]", ""), exception)
        throw InvalidPropertyValueException(message)
      case Failure(exception) =>
        val message =
          s"Unexpected issue with the ${ConnectorPropertiesEnum.columnMappingGroupedTasksColumnsProperty} " +
            s"property. Please check the logs of the connector for more information."
        log.error(message.replaceAll("[\r\n]", ""), exception)
        throw InvalidPropertyValueException(message)
    }
  }
}

object GroupedTasksColumnsDto extends Json4sSupport {
  @throws[ParseException]
  def fromConnectorConfig(connectorConfig: AbstractConfig): Option[GroupedTasksColumnsDto] = {
    val groupedTasksColumnsPropertyValue =
      connectorConfig.getString(ConnectorPropertiesEnum.columnMappingGroupedTasksColumnsProperty.toStringDescription)

    getGroupedTasksColumnsDto(groupedTasksColumnsPropertyValue)
  }

  @throws[ParseException]
  def fromGroupedTasksColumnsConfig(
      columnMappingGroupedTasksColumnsCfg: ConfigValue
  ): Option[GroupedTasksColumnsDto] = {
    val groupedTasksColumnsPropertyValue = columnMappingGroupedTasksColumnsCfg.value().toString

    getGroupedTasksColumnsDto(groupedTasksColumnsPropertyValue)
  }

  @throws[ParseException]
  private[dtos] def getGroupedTasksColumnsDto(
      groupedTasksColumnsPropertyValue: String
  ): Option[GroupedTasksColumnsDto] = {
    groupedTasksColumnsPropertyValue match {
      case Constants.columnMappingStringDefaultValue => None
      case groupedTasksColumnsPropertyValue =>
        val groupedTasksColumns = parse(groupedTasksColumnsPropertyValue).extract[Set[Int]]
        Some(GroupedTasksColumnsDto(groupedTasksColumns))
    }
  }
}
