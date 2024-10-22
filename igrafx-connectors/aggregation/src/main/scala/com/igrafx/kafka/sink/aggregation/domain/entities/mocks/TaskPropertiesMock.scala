package com.igrafx.kafka.sink.aggregation.domain.entities.mocks

import com.igrafx.core.Mock
import com.igrafx.kafka.sink.aggregation.domain.entities.TaskProperties

class TaskPropertiesMock extends Mock[TaskProperties] {
  private var connectorName: String = "connectorName"
  private var topicOut: String = "topicOut"
  private var aggregationColumnName = "LINEAG"
  private var elementNumberThreshold: Int = 10
  private var valuePatternThreshold: String = ""
  private var timeoutInSecondsThreshold: Int = 60
  private var bootstrapServers: String = "broker:29092"
  private var schemaRegistryUrl: String = "http://schema-registry:8081"
  private var maxMessageBytes: Int = 1048588

  def setConnectorName(connectorName: String): TaskPropertiesMock = {
    this.connectorName = connectorName
    this
  }

  def setTopicOut(topicOut: String): TaskPropertiesMock = {
    this.topicOut = topicOut
    this
  }

  def setAggregationColumnName(aggregationColumnName: String): TaskPropertiesMock = {
    this.aggregationColumnName = aggregationColumnName
    this
  }

  def setElementNumberThreshold(elementNumberThreshold: Int): TaskPropertiesMock = {
    this.elementNumberThreshold = elementNumberThreshold
    this
  }

  def setValuePatternThreshold(valuePatternThreshold: String): TaskPropertiesMock = {
    this.valuePatternThreshold = valuePatternThreshold
    this
  }

  def setTimeoutInSecondsThreshold(timeoutInSecondsThreshold: Int): TaskPropertiesMock = {
    this.timeoutInSecondsThreshold = timeoutInSecondsThreshold
    this
  }

  def setBootstrapServers(bootstrapServers: String): TaskPropertiesMock = {
    this.bootstrapServers = bootstrapServers
    this
  }

  def setSchemaRegistryUrl(schemaRegistryUrl: String): TaskPropertiesMock = {
    this.schemaRegistryUrl = schemaRegistryUrl
    this
  }

  def setMaxMessageBytes(maxMessageBytes: Int): TaskPropertiesMock = {
    this.maxMessageBytes = maxMessageBytes
    this
  }

  override def build(): TaskProperties = {
    TaskProperties(
      connectorName = connectorName,
      topicOut = topicOut,
      aggregationColumnName = aggregationColumnName,
      elementNumberThreshold = elementNumberThreshold,
      valuePatternThreshold = valuePatternThreshold,
      timeoutInSecondsThreshold = timeoutInSecondsThreshold,
      bootstrapServers = bootstrapServers,
      schemaRegistryUrl = schemaRegistryUrl,
      maxMessageBytes = maxMessageBytes
    )
  }
}
