package com.igrafx.kafka.sink.aggregationmain.domain.usecases

import com.igrafx.kafka.sink.aggregationmain.domain.columnmapping.domain.entities.ColumnMappingProperties
import com.igrafx.kafka.sink.aggregationmain.domain.entities.ColumnsNumber
import com.igrafx.kafka.sink.aggregationmain.domain.enums.ConnectorPropertiesEnum
import com.igrafx.kafka.sink.aggregationmain.domain.exceptions.{ColumnMappingCreationException, InvalidTokenException}
import com.igrafx.kafka.sink.aggregationmain.domain.interfaces.MainApi
import org.apache.kafka.common.config.ConfigValue

import scala.concurrent.Future

trait ColumnMappingUseCases {

  // ---------- Column Mapping Verification Methods ----------
  def checkColumnMappingProperties(
      columnMappingCreateCfg: ConfigValue,
      configValues: Iterable[ConfigValue]
  ): Unit

  def checkNonEmptyStringProperty(
      propertyConfig: ConfigValue,
      propertyName: ConnectorPropertiesEnum
  ): Unit

  // ---------- Column Mapping Creation Methods ----------
  @throws[InvalidTokenException]
  @throws[ColumnMappingCreationException]
  def createColumnMapping(
      columnMappingProperties: ColumnMappingProperties,
      mainApi: MainApi
  ): Future[Unit]

  def generateCsvHeaderFromColumnMapping(
      columnMappingProperties: ColumnMappingProperties,
      csvFieldsNumber: ColumnsNumber,
      csvSeparator: String
  ): String

  def generateCsvHeaderWithoutColumnMapping(csvFieldsNumber: ColumnsNumber, csvSeparator: String): String
}

object ColumnMappingUseCases {
  lazy val instance: ColumnMappingUseCases = new ColumnMappingUseCasesImpl
}
