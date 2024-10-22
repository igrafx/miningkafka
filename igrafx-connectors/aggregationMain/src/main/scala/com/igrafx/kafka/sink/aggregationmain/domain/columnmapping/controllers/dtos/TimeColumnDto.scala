package com.igrafx.kafka.sink.aggregationmain.domain.columnmapping.controllers.dtos

import com.igrafx.kafka.sink.aggregationmain.Constants
import com.igrafx.kafka.sink.aggregationmain.domain.columnmapping.domain.entities.{Index, ValidTimeColumn}
import com.igrafx.kafka.sink.aggregationmain.domain.entities.ColumnsNumber
import com.igrafx.kafka.sink.aggregationmain.domain.enums.ConnectorPropertiesEnum
import com.igrafx.kafka.sink.aggregationmain.domain.exceptions.{IndexException, InvalidPropertyValueException}
import org.slf4j.Logger

import scala.util.{Failure, Success, Try}

final case class TimeColumnDto(columnIndex: String, format: String) {
  @throws[InvalidPropertyValueException]
  def toEntity(columnsNumber: ColumnsNumber)(implicit log: Logger): ValidTimeColumn = {
    Try {
      ValidTimeColumn(
        columnIndex = Index(columnIndex, columnsNumber, isOnlyIndexProperty = false),
        format = format
      )
    } match {
      case Success(validTimeColumn) => validTimeColumn
      case Failure(exception: IndexException) =>
        val message =
          s"Issue with the ${ConnectorPropertiesEnum.columnMappingTimeColumnsProperty} property. " +
            s"At least one of the columns has a wrong index : ${exception.getMessage}"
        log.error(message.replaceAll("[\r\n]", ""), exception)
        throw InvalidPropertyValueException(message)
      case Failure(exception) =>
        val message =
          s"Unexpected issue with the ${ConnectorPropertiesEnum.columnMappingTimeColumnsProperty} " +
            s"property. Please check the logs of the connector for more information."
        log.error(message.replaceAll("[\r\n]", ""), exception)
        throw InvalidPropertyValueException(message)
    }
  }
}

object TimeColumnDto {
  @throws[InvalidPropertyValueException]
  def apply(timeColumnAsString: String): TimeColumnDto = {
    if (
      (timeColumnAsString.charAt(0) != Constants.characterForElementStartInPropertyValue) ||
      (timeColumnAsString.charAt(timeColumnAsString.length - 1) != Constants.characterForElementEndInPropertyValue)
    ) {
      throw InvalidPropertyValueException(
        s"Issue with the ${ConnectorPropertiesEnum.columnMappingTimeColumnsProperty} property. " +
          s"The column description '$timeColumnAsString' has a wrong format " +
          s"(missing '${Constants.characterForElementStartInPropertyValue}' " +
          s"or '${Constants.characterForElementEndInPropertyValue}')"
      )
    } else {
      Try {
        timeColumnAsString.slice(1, timeColumnAsString.length - 1)
      } match {
        case Failure(_) =>
          throw InvalidPropertyValueException(
            s"Issue with the ${ConnectorPropertiesEnum.columnMappingTimeColumnsProperty} property. " +
              s"The column description '$timeColumnAsString' has a wrong format"
          )
        case Success(columnInfoWithoutBraces) =>
          val tabColumnInfo = columnInfoWithoutBraces.split(Constants.delimiterForElementInPropertyValue).toSeq
          if (tabColumnInfo.size != 2) {
            throw InvalidPropertyValueException(
              s"Issue with the ${ConnectorPropertiesEnum.columnMappingTimeColumnsProperty} property. " +
                s"The column description '$timeColumnAsString' has a wrong delimiter or a wrong number of information"
            )
          } else {
            tabColumnInfo.headOption match {
              case None =>
                throw InvalidPropertyValueException(
                  s"Issue with the ${ConnectorPropertiesEnum.columnMappingTimeColumnsProperty} property. " +
                    s"The column description '$timeColumnAsString' doesn't have an index"
                )
              case Some(indexAsString) =>
                tabColumnInfo.lift(1) match {
                  case None =>
                    throw InvalidPropertyValueException(
                      s"Issue with the ${ConnectorPropertiesEnum.columnMappingTimeColumnsProperty} property. " +
                        s"The column description '$timeColumnAsString' doesn't have a date format"
                    )
                  case Some(dateFormat) => new TimeColumnDto(indexAsString, dateFormat)
                }
            }
          }
      }
    }
  }
}
