package com.igrafx.kafka.sink.aggregationmain.domain.entities.mocks

import com.igrafx.core.Mock
import com.igrafx.kafka.sink.aggregationmain.domain.entities.{ColumnsNumber, Properties}
import com.igrafx.kafka.sink.aggregationmain.domain.enums.EncodingEnum

import java.util.UUID

class PropertiesMock extends Mock[Properties] {
  private var connectorName: String = "connectorName"
  private var apiUrl: String = "apiUrl"
  private var authUrl: String = "authUrl"
  private var workGroupId: String = "workGroupId"
  private var workGroupKey: String = "workGroupKey"
  private var projectId: UUID = UUID.fromString("0f61e0f3-68c8-4c7f-bc1d-a4aabf7b74e5")
  private var csvEncoding: EncodingEnum = EncodingEnum.UTF_8
  private var csvSeparator: String = ","
  private var csvQuote: String = "\""
  private var csvFieldsNumber: ColumnsNumber = new ColumnsNumberMock().setNumber(4).build()
  private var csvHeader: Boolean = true
  private var csvDefaultTextValue: String = "null"
  private var retentionTimeInDay: Int = 100
  private var isLogging: Boolean = false
  private var elementNumberThreshold: Int = 10
  private var valuePatternThreshold: String = ""
  private var timeoutInSecondsThreshold: Int = 60
  private var bootstrapServers: String = "broker:29092"
  private var schemaRegistryUrl: String = "http://schema-registry:8081"

  def setConnectorName(connectorName: String): PropertiesMock = {
    this.connectorName = connectorName
    this
  }

  def setApiUrl(apiUrl: String): PropertiesMock = {
    this.apiUrl = apiUrl
    this
  }

  def setAuthUrl(authUrl: String): PropertiesMock = {
    this.authUrl = authUrl
    this
  }

  def setWorkGroupId(workGroupId: String): PropertiesMock = {
    this.workGroupId = workGroupId
    this
  }

  def setWorkGroupKey(workGroupKey: String): PropertiesMock = {
    this.workGroupKey = workGroupKey
    this
  }

  def setProjectId(projectId: UUID): PropertiesMock = {
    this.projectId = projectId
    this
  }

  def setCsvEncoding(csvEncoding: EncodingEnum): PropertiesMock = {
    this.csvEncoding = csvEncoding
    this
  }

  def setCsvSeparator(csvSeparator: String): PropertiesMock = {
    this.csvSeparator = csvSeparator
    this
  }

  def setCsvQuote(csvQuote: String): PropertiesMock = {
    this.csvQuote = csvQuote
    this
  }

  def setCsvFieldsNumber(csvFieldsNumber: ColumnsNumber): PropertiesMock = {
    this.csvFieldsNumber = csvFieldsNumber
    this
  }

  def setCsvHeader(csvHeader: Boolean): PropertiesMock = {
    this.csvHeader = csvHeader
    this
  }

  def setCsvDefaultTextValue(csvDefaultTextValue: String): PropertiesMock = {
    this.csvDefaultTextValue = csvDefaultTextValue
    this
  }

  def setRetentionTimeInDay(retentionTimeInDay: Int): PropertiesMock = {
    this.retentionTimeInDay = retentionTimeInDay
    this
  }

  def setIsLogging(isLogging: Boolean): PropertiesMock = {
    this.isLogging = isLogging
    this
  }

  def setElementNumberThreshold(elementNumberThreshold: Int): PropertiesMock = {
    this.elementNumberThreshold = elementNumberThreshold
    this
  }

  def setValuePatternThreshold(valuePatternThreshold: String): PropertiesMock = {
    this.valuePatternThreshold = valuePatternThreshold
    this
  }

  def setTimeoutInSecondsThreshold(timeoutInSecondsThreshold: Int): PropertiesMock = {
    this.timeoutInSecondsThreshold = timeoutInSecondsThreshold
    this
  }

  def setBootstrapServers(bootstrapServers: String): PropertiesMock = {
    this.bootstrapServers = bootstrapServers
    this
  }

  def setSchemaRegistryUrl(schemaRegistryUrl: String): PropertiesMock = {
    this.schemaRegistryUrl = schemaRegistryUrl
    this
  }

  override def build(): Properties = {
    Properties(
      connectorName = connectorName,
      apiUrl = apiUrl,
      authUrl = authUrl,
      workGroupId = workGroupId,
      workGroupKey = workGroupKey,
      projectId = projectId,
      csvEncoding = csvEncoding,
      csvSeparator = csvSeparator,
      csvQuote = csvQuote,
      csvFieldsNumber = csvFieldsNumber,
      csvHeader = csvHeader,
      csvDefaultTextValue = csvDefaultTextValue,
      retentionTimeInDay = retentionTimeInDay,
      isLogging = isLogging,
      elementNumberThreshold = elementNumberThreshold,
      valuePatternThreshold = valuePatternThreshold,
      timeoutInSecondsThreshold = timeoutInSecondsThreshold,
      bootstrapServers = bootstrapServers,
      schemaRegistryUrl = schemaRegistryUrl
    )
  }
}
