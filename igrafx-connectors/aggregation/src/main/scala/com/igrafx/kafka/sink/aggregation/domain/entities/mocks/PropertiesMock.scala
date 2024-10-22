package com.igrafx.kafka.sink.aggregation.domain.entities.mocks

import com.igrafx.core.Mock
import com.igrafx.kafka.sink.aggregation.domain.entities.Properties

class PropertiesMock extends Mock[Properties] {
  private var connectorName: String = "connectorName"
  private var topicOut: String = "topicOut"
  private var aggregationColumnName = "LINEAG"
  private var elementNumberThreshold: Int = 10
  private var valuePatternThreshold: String = ""
  private var timeoutInSecondsThreshold: Int = 60
  private var bootstrapServers: String = "broker:29092"
  private var schemaRegistryUrl: String = "http://schema-registry:8081"

  def setConnectorName(connectorName: String): PropertiesMock = {
    this.connectorName = connectorName
    this
  }

  def setTopicOut(topicOut: String): PropertiesMock = {
    this.topicOut = topicOut
    this
  }

  def setAggregationColumnName(aggregationColumnName: String): PropertiesMock = {
    this.aggregationColumnName = aggregationColumnName
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
      topicOut = topicOut,
      aggregationColumnName = aggregationColumnName,
      elementNumberThreshold = elementNumberThreshold,
      valuePatternThreshold = valuePatternThreshold,
      timeoutInSecondsThreshold = timeoutInSecondsThreshold,
      bootstrapServers = bootstrapServers,
      schemaRegistryUrl = schemaRegistryUrl
    )
  }
}
