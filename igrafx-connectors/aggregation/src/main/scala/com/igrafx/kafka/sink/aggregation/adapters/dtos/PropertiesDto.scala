package com.igrafx.kafka.sink.aggregation.adapters.dtos

import com.igrafx.kafka.sink.aggregation.Constants
import com.igrafx.kafka.sink.aggregation.domain.entities.Properties
import com.igrafx.kafka.sink.aggregation.domain.enums.ConnectorPropertiesEnum
import org.apache.kafka.common.config.{AbstractConfig, ConfigException}

import scala.collection.immutable.HashMap

protected[adapters] final case class PropertiesDto(
    connectorName: String,
    topicOut: String,
    aggregationColumnName: String,
    elementNumberThreshold: Int,
    valuePatternThreshold: String,
    timeoutInSecondsThreshold: Int,
    bootstrapServers: String,
    schemaRegistryUrl: String
) {
  @throws[IllegalArgumentException](cause = "A requirement in Properties is not verified")
  def toProperties: Properties = {
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

protected[adapters] object PropertiesDto {
  implicit class PropertiesDtoToMapClass(propertiesDto: PropertiesDto) {
    def toMap: Map[String, String] = {
      propertiesDtoToMap(propertiesDto)
    }
  }

  private def propertiesDtoToMap(propertiesDto: PropertiesDto): Map[String, String] = {
    HashMap(
      Constants.connectorNameProperty -> propertiesDto.connectorName,
      ConnectorPropertiesEnum.topicOutProperty.toStringDescription -> propertiesDto.topicOut,
      ConnectorPropertiesEnum.aggregationColumnNameProperty.toStringDescription -> propertiesDto.aggregationColumnName,
      ConnectorPropertiesEnum.elementNumberThresholdProperty.toStringDescription -> propertiesDto.elementNumberThreshold.toString,
      ConnectorPropertiesEnum.valuePatternThresholdProperty.toStringDescription -> propertiesDto.valuePatternThreshold,
      ConnectorPropertiesEnum.timeoutInSecondsThresholdProperty.toStringDescription -> propertiesDto.timeoutInSecondsThreshold.toString,
      ConnectorPropertiesEnum.bootstrapServersProperty.toStringDescription -> propertiesDto.bootstrapServers,
      Constants.schemaRegistryUrlProperty -> propertiesDto.schemaRegistryUrl
    )
  }

  def fromProperties(properties: Properties): PropertiesDto = {
    PropertiesDto(
      connectorName = properties.connectorName,
      topicOut = properties.topicOut,
      aggregationColumnName = properties.aggregationColumnName,
      elementNumberThreshold = properties.elementNumberThreshold,
      valuePatternThreshold = properties.valuePatternThreshold,
      timeoutInSecondsThreshold = properties.timeoutInSecondsThreshold,
      bootstrapServers = properties.bootstrapServers,
      schemaRegistryUrl = properties.schemaRegistryUrl
    )
  }

  @throws[ConfigException](cause = "Issue in retrieving a connector's property")
  def fromConnectorConfig(
      connectorConfig: AbstractConfig,
      connectorName: String,
      schemaRegistryUrl: String
  ): PropertiesDto = {
    val topicOut: String = connectorConfig.getString(ConnectorPropertiesEnum.topicOutProperty.toStringDescription)
    val aggregationColumnName: String =
      connectorConfig.getString(ConnectorPropertiesEnum.aggregationColumnNameProperty.toStringDescription)
    val elementNumberThreshold: Int =
      connectorConfig.getInt(ConnectorPropertiesEnum.elementNumberThresholdProperty.toStringDescription)
    val valuePatternThreshold: String =
      connectorConfig.getString(ConnectorPropertiesEnum.valuePatternThresholdProperty.toStringDescription)
    val timeoutInSecondsThreshold: Int =
      connectorConfig.getInt(ConnectorPropertiesEnum.timeoutInSecondsThresholdProperty.toStringDescription)
    val bootstrapServers: String =
      connectorConfig.getString(ConnectorPropertiesEnum.bootstrapServersProperty.toStringDescription)

    PropertiesDto(
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
