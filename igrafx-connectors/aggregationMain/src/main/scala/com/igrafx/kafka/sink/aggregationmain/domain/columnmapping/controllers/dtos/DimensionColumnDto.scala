package com.igrafx.kafka.sink.aggregationmain.domain.columnmapping.controllers.dtos

import com.igrafx.kafka.sink.aggregationmain.domain.columnmapping.controllers.dtos.enums.{
  DimensionAggregationDto,
  GroupedTasksDimensionAggregationDto
}
import com.igrafx.kafka.sink.aggregationmain.domain.columnmapping.domain.entities.{
  DimensionAggregationInformation,
  Index,
  NonEmptyString,
  ValidDimensionColumn
}
import com.igrafx.kafka.sink.aggregationmain.domain.entities.ColumnsNumber
import com.igrafx.kafka.sink.aggregationmain.domain.enums.ConnectorPropertiesEnum
import com.igrafx.kafka.sink.aggregationmain.domain.exceptions.{
  IndexException,
  InvalidPropertyValueException,
  NonEmptyStringException,
  WrongAggregationException
}
import org.slf4j.Logger

import scala.util.{Failure, Success, Try}

final case class DimensionColumnDto(
    columnIndex: Int,
    name: String,
    isCaseScope: Boolean,
    aggregation: Option[DimensionAggregationDto],
    groupedTasksAggregation: Option[GroupedTasksDimensionAggregationDto]
) {
  @throws[InvalidPropertyValueException]
  def toEntity(columnsNumber: ColumnsNumber)(implicit log: Logger): ValidDimensionColumn = {
    Try {
      ValidDimensionColumn(
        columnIndex = Index(columnIndex, columnsNumber, isOnlyIndexProperty = false),
        name = NonEmptyString(name, isOnlyStringProperty = false),
        aggregationInformation = DimensionAggregationInformation(aggregation.map(_.toEntity), isCaseScope),
        groupedTasksAggregation = groupedTasksAggregation.map(_.toEntity)
      )
    } match {
      case Success(validDimensionColumn) => validDimensionColumn
      case Failure(exception: IndexException) =>
        val message =
          s"Issue with the ${ConnectorPropertiesEnum.columnMappingDimensionColumnsProperty} property. " +
            s"At least one of the columns has a wrong index : ${exception.getMessage}"
        log.error(message.replaceAll("[\r\n]", ""), exception)
        throw InvalidPropertyValueException(message)
      case Failure(exception: NonEmptyStringException) =>
        val message =
          s"Issue with the ${ConnectorPropertiesEnum.columnMappingDimensionColumnsProperty} property. " +
            s"At least one of the columns has a wrong name : ${exception.getMessage}"
        log.error(message.replaceAll("[\r\n]", ""), exception)
        throw InvalidPropertyValueException(message)
      case Failure(exception: WrongAggregationException) =>
        val message =
          s"Issue with the ${ConnectorPropertiesEnum.columnMappingDimensionColumnsProperty} property. " +
            s"At least one of the columns has an isCaseScope true but no valid aggregation type (if isCaseScope is " +
            s"true you need to provide a valid aggregation type). Please refer to the documentation to find " +
            s"the valid aggregation types"
        log.error(message.replaceAll("[\r\n]", ""), exception)
        throw InvalidPropertyValueException(message)
      case Failure(exception) =>
        val message =
          s"Unexpected issue with the ${ConnectorPropertiesEnum.columnMappingDimensionColumnsProperty} " +
            s"property. Please check the logs of the connector for more information."
        log.error(message.replaceAll("[\r\n]", ""), exception)
        throw InvalidPropertyValueException(message)
    }
  }
}
