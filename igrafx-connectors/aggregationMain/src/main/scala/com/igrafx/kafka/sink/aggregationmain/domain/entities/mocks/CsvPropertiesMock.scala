package com.igrafx.kafka.sink.aggregationmain.domain.entities.mocks

import com.igrafx.core.Mock
import com.igrafx.kafka.sink.aggregationmain.domain.entities.{ColumnsNumber, CsvProperties}
import com.igrafx.kafka.sink.aggregationmain.domain.enums.EncodingEnum

import java.util.UUID

class CsvPropertiesMock extends Mock[CsvProperties] {
  private var connectorName: String = "connectorName"
  private var projectId: UUID = UUID.fromString("0f61e0f3-68c8-4c7f-bc1d-a4aabf7b74e5")
  private var csvEncoding: EncodingEnum = EncodingEnum.UTF_8
  private var csvSeparator: String = ","
  private var csvQuote: String = "\""
  private var csvFieldsNumber: ColumnsNumber = new ColumnsNumberMock().setNumber(4).build()
  private var csvHeader: Boolean = false
  private var csvDefaultTextValue: String = "null"
  private var csvHeaderValue: Option[String] = None

  def setConnectorName(connectorName: String): CsvPropertiesMock = {
    this.connectorName = connectorName
    this
  }

  def setProjectId(projectId: UUID): CsvPropertiesMock = {
    this.projectId = projectId
    this
  }

  def setCsvEncoding(csvEncoding: EncodingEnum): CsvPropertiesMock = {
    this.csvEncoding = csvEncoding
    this
  }

  def setCsvSeparator(csvSeparator: String): CsvPropertiesMock = {
    this.csvSeparator = csvSeparator
    this
  }

  def setCsvQuote(csvQuote: String): CsvPropertiesMock = {
    this.csvQuote = csvQuote
    this
  }

  def setCsvFieldsNumber(csvFieldsNumber: ColumnsNumber): CsvPropertiesMock = {
    this.csvFieldsNumber = csvFieldsNumber
    this
  }

  def setCsvHeader(csvHeader: Boolean): CsvPropertiesMock = {
    this.csvHeader = csvHeader
    this
  }

  def setCsvDefaultTextValue(csvDefaultTextValue: String): CsvPropertiesMock = {
    this.csvDefaultTextValue = csvDefaultTextValue
    this
  }

  def setCsvHeaderValue(csvHeaderValue: Option[String]): CsvPropertiesMock = {
    this.csvHeaderValue = csvHeaderValue
    this
  }

  override def build(): CsvProperties = {
    CsvProperties(
      connectorName = connectorName,
      projectId = projectId,
      csvEncoding = csvEncoding,
      csvSeparator = csvSeparator,
      csvQuote = csvQuote,
      csvFieldsNumber = csvFieldsNumber,
      csvHeader = csvHeader,
      csvDefaultTextValue = csvDefaultTextValue,
      csvHeaderValue = csvHeaderValue
    )
  }
}
