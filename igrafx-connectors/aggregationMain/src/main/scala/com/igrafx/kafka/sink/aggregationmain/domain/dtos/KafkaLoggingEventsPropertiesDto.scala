package com.igrafx.kafka.sink.aggregationmain.domain.dtos

import com.igrafx.kafka.sink.aggregationmain.Constants
import com.igrafx.kafka.sink.aggregationmain.domain.entities.KafkaLoggingEventsProperties
import com.igrafx.kafka.sink.aggregationmain.domain.enums.ConnectorPropertiesEnum
import org.apache.kafka.common.config.{AbstractConfig, ConfigException}

import scala.collection.immutable.HashMap

final case class KafkaLoggingEventsPropertiesDto(
    topic: String,
    bootstrapServers: String,
    schemaRegistryUrl: String
) {
  @throws[IllegalArgumentException](
    cause =
      "At least one of the connector's Kafka Logging Events properties doesn't verify the KafkaLoggingEventsProperties requirements"
  )
  def toKafkaLoggingEventsProperties: KafkaLoggingEventsProperties = {
    KafkaLoggingEventsProperties(
      topic,
      bootstrapServers,
      schemaRegistryUrl
    )
  }
}

object KafkaLoggingEventsPropertiesDto {
  implicit class KafkaLoggingEventsPropertiesDtoToMapClass(
      kafkaLoggingEventsPropertiesDto: KafkaLoggingEventsPropertiesDto
  ) {
    def toMap: Map[String, String] = {
      kafkaLoggingEventsPropertiesDtoToMap(kafkaLoggingEventsPropertiesDto)
    }
  }

  private def kafkaLoggingEventsPropertiesDtoToMap(
      kafkaLoggingEventsPropertiesDto: KafkaLoggingEventsPropertiesDto
  ): Map[String, String] = {
    HashMap(
      ConnectorPropertiesEnum.kafkaLoggingEventsTopicProperty.toStringDescription -> kafkaLoggingEventsPropertiesDto.topic,
      ConnectorPropertiesEnum.bootstrapServersProperty.toStringDescription -> kafkaLoggingEventsPropertiesDto.bootstrapServers,
      Constants.schemaRegistryUrlProperty -> kafkaLoggingEventsPropertiesDto.schemaRegistryUrl
    )
  }

  def fromKafkaLoggingEventsProperties(
      kafkaLoggingEventsProperties: KafkaLoggingEventsProperties
  ): KafkaLoggingEventsPropertiesDto = {
    KafkaLoggingEventsPropertiesDto(
      kafkaLoggingEventsProperties.topic,
      kafkaLoggingEventsProperties.bootstrapServers,
      kafkaLoggingEventsProperties.schemaRegistryUrl
    )
  }

  @throws[ConfigException](cause = "Issue in retrieving a Kafka Logging Events connector's property")
  def fromConnectorConfig(
      connectorConfig: AbstractConfig,
      schemaRegistryUrl: String
  ): KafkaLoggingEventsPropertiesDto = {
    val topic = connectorConfig.getString(ConnectorPropertiesEnum.kafkaLoggingEventsTopicProperty.toStringDescription)
    val bootstrapServers =
      connectorConfig.getString(ConnectorPropertiesEnum.bootstrapServersProperty.toStringDescription)

    KafkaLoggingEventsPropertiesDto(
      topic = topic,
      bootstrapServers = bootstrapServers,
      schemaRegistryUrl = schemaRegistryUrl
    )
  }
}
