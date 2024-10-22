package com.igrafx.kafka.sink.aggregationmain.domain.columnmapping.domain.entities

import com.igrafx.kafka.sink.aggregationmain.Constants
import com.igrafx.kafka.sink.aggregationmain.domain.entities.ColumnsNumber
import com.igrafx.kafka.sink.aggregationmain.domain.enums.ConnectorPropertiesEnum
import com.igrafx.kafka.sink.aggregationmain.domain.exceptions.{
  DefaultIndexException,
  IncoherentIndexException,
  NegativeIndexException,
  ParsableIndexException
}

import scala.util.{Failure, Success, Try}

final case class Index(index: Int)

object Index {
  private def apply(index: Int): Index = {
    new Index(index)
  }

  @throws[DefaultIndexException]
  @throws[NegativeIndexException]
  @throws[IncoherentIndexException]
  def apply(
      index: Int,
      columnsNumber: ColumnsNumber,
      isOnlyIndexProperty: Boolean
  ): Index = {
    if (isOnlyIndexProperty && (index == Constants.columnIndexDefaultValue)) {
      throw DefaultIndexException("Property is not defined")
    } else if (index < 0) {
      throw NegativeIndexException(
        s"The value '$index' is negative. An index should be between 0 and the value of " +
          s"the ${ConnectorPropertiesEnum.csvFieldsNumberProperty} property"
      )
    } else if (index >= columnsNumber.number) {
      throw IncoherentIndexException(
        s"The value '$index' is greater or equal than the value '${columnsNumber.number}' of " +
          s"the ${ConnectorPropertiesEnum.csvFieldsNumberProperty} property. An index should be between 0 and " +
          s"the value of the ${ConnectorPropertiesEnum.csvFieldsNumberProperty} property"
      )
    } else {
      new Index(index)
    }
  }

  @throws[ParsableIndexException]
  @throws[DefaultIndexException]
  @throws[NegativeIndexException]
  @throws[IncoherentIndexException]
  def apply(
      indexAsString: String,
      columnsNumber: ColumnsNumber,
      isOnlyIndexProperty: Boolean
  ): Index = {
    Try {
      indexAsString.toInt
    } match {
      case Failure(_) =>
        throw ParsableIndexException(
          s"The value '$indexAsString' is not parsable to Int (you should try with an index between 0 and the value " +
            s"of the ${ConnectorPropertiesEnum.csvFieldsNumberProperty} property)"
        )
      case Success(index) => Index(index, columnsNumber, isOnlyIndexProperty)
    }
  }
}
