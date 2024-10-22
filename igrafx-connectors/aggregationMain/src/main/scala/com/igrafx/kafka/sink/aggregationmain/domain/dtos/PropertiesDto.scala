package com.igrafx.kafka.sink.aggregationmain.domain.dtos

import com.igrafx.kafka.sink.aggregationmain.Constants
import com.igrafx.kafka.sink.aggregationmain.domain.dtos.EncodingEnumDto._
import com.igrafx.kafka.sink.aggregationmain.domain.entities.{ColumnsNumber, Properties}
import com.igrafx.kafka.sink.aggregationmain.domain.enums.{ConnectorPropertiesEnum, EncodingEnum}
import org.apache.kafka.common.config.{AbstractConfig, ConfigException}
import org.slf4j.Logger

import java.util.UUID
import scala.collection.immutable.HashMap
import scala.util.{Failure, Success, Try}

final case class PropertiesDto(
    connectorName: String,
    apiUrl: String,
    authUrl: String,
    workGroupId: String,
    workGroupKey: String,
    projectId: UUID,
    csvEncoding: EncodingEnum,
    csvSeparator: String,
    csvQuote: String,
    csvFieldsNumber: ColumnsNumber,
    csvHeader: Boolean,
    csvDefaultTextValue: String,
    retentionTimeInDay: Int,
    isLogging: Boolean,
    elementNumberThreshold: Int,
    valuePatternThreshold: String,
    timeoutInSecondsThreshold: Int,
    bootstrapServers: String,
    schemaRegistryUrl: String
) {
  @throws[IllegalArgumentException](
    cause = "At least one of the connector's property doesn't verify the Properties requirements"
  )
  def toProperties: Properties = {
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

object PropertiesDto {
  implicit class PropertiesDtoToMapClass(propertiesDto: PropertiesDto) {
    def toMap: Map[String, String] = {
      propertiesDtoToMap(propertiesDto)
    }
  }

  private def propertiesDtoToMap(propertiesDto: PropertiesDto): Map[String, String] = {
    HashMap(
      "name" -> propertiesDto.connectorName,
      ConnectorPropertiesEnum.apiUrlProperty.toStringDescription -> propertiesDto.apiUrl,
      ConnectorPropertiesEnum.authUrlProperty.toStringDescription -> propertiesDto.authUrl,
      ConnectorPropertiesEnum.workGroupIdProperty.toStringDescription -> propertiesDto.workGroupId,
      ConnectorPropertiesEnum.workGroupKeyProperty.toStringDescription -> propertiesDto.workGroupKey,
      ConnectorPropertiesEnum.projectIdProperty.toStringDescription -> propertiesDto.projectId.toString,
      ConnectorPropertiesEnum.csvEncodingProperty.toStringDescription -> propertiesDto.csvEncoding.toStringDescription,
      ConnectorPropertiesEnum.csvSeparatorProperty.toStringDescription -> propertiesDto.csvSeparator,
      ConnectorPropertiesEnum.csvQuoteProperty.toStringDescription -> propertiesDto.csvQuote,
      ConnectorPropertiesEnum.csvFieldsNumberProperty.toStringDescription -> propertiesDto.csvFieldsNumber.number.toString,
      ConnectorPropertiesEnum.csvHeaderProperty.toStringDescription -> propertiesDto.csvHeader.toString,
      ConnectorPropertiesEnum.csvDefaultTextValueProperty.toStringDescription -> propertiesDto.csvDefaultTextValue,
      ConnectorPropertiesEnum.retentionTimeInDayProperty.toStringDescription -> propertiesDto.retentionTimeInDay.toString,
      ConnectorPropertiesEnum.kafkaLoggingEventsIsLoggingProperty.toStringDescription -> propertiesDto.isLogging.toString,
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
      apiUrl = properties.apiUrl,
      authUrl = properties.authUrl,
      workGroupId = properties.workGroupId,
      workGroupKey = properties.workGroupKey,
      projectId = properties.projectId,
      csvEncoding = properties.csvEncoding,
      csvSeparator = properties.csvSeparator,
      csvQuote = properties.csvQuote,
      csvFieldsNumber = properties.csvFieldsNumber,
      csvHeader = properties.csvHeader,
      csvDefaultTextValue = properties.csvDefaultTextValue,
      retentionTimeInDay = properties.retentionTimeInDay,
      isLogging = properties.isLogging,
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
      schemaRegistryUrl: String,
      log: Logger
  ): PropertiesDto = {
    val apiUrl: String = connectorConfig.getString(ConnectorPropertiesEnum.apiUrlProperty.toStringDescription)
    val authUrl: String = connectorConfig.getString(ConnectorPropertiesEnum.authUrlProperty.toStringDescription)
    val wId: String = connectorConfig.getString(ConnectorPropertiesEnum.workGroupIdProperty.toStringDescription)
    val wKey: String = connectorConfig.getString(ConnectorPropertiesEnum.workGroupKeyProperty.toStringDescription)
    val projectUuid =
      UUID.fromString(connectorConfig.getString(ConnectorPropertiesEnum.projectIdProperty.toStringDescription))
    val csvEncoding: String =
      connectorConfig.getString(ConnectorPropertiesEnum.csvEncodingProperty.toStringDescription)
    val csvSeparator: String =
      connectorConfig.getString(ConnectorPropertiesEnum.csvSeparatorProperty.toStringDescription)
    val csvQuote: String = connectorConfig.getString(ConnectorPropertiesEnum.csvQuoteProperty.toStringDescription)
    val csvFieldsNumberAsInt: Int =
      connectorConfig.getInt(ConnectorPropertiesEnum.csvFieldsNumberProperty.toStringDescription)
    val csvFieldsNumber = Try {
      ColumnsNumber(csvFieldsNumberAsInt)
    } match {
      case Success(columnsNumber: ColumnsNumber) => columnsNumber
      case Failure(exception) =>
        throw new ConfigException(
          s"Issue with the ${ConnectorPropertiesEnum.csvFieldsNumberProperty.toStringDescription} property. The value is not greater or equals than ${Constants.minimumColumnsNumber}",
          exception
        )
    }
    val csvHeader: Boolean = connectorConfig.getBoolean(ConnectorPropertiesEnum.csvHeaderProperty.toStringDescription)
    val csvDefaultTextValue =
      connectorConfig.getString(ConnectorPropertiesEnum.csvDefaultTextValueProperty.toStringDescription)
    val retentionTimeInDay: Int =
      connectorConfig.getInt(ConnectorPropertiesEnum.retentionTimeInDayProperty.toStringDescription)
    val isLogging =
      connectorConfig.getBoolean(ConnectorPropertiesEnum.kafkaLoggingEventsIsLoggingProperty.toStringDescription)
    val elementNumberThreshold: Int =
      connectorConfig.getInt(ConnectorPropertiesEnum.elementNumberThresholdProperty.toStringDescription)
    val valuePatternThreshold: String =
      connectorConfig.getString(ConnectorPropertiesEnum.valuePatternThresholdProperty.toStringDescription)
    val timeoutInSecondsThreshold: Int =
      connectorConfig.getInt(ConnectorPropertiesEnum.timeoutInSecondsThresholdProperty.toStringDescription)
    val bootstrapServers: String =
      connectorConfig.getString(ConnectorPropertiesEnum.bootstrapServersProperty.toStringDescription)

    val encoding: EncodingEnum = EncodingEnumDto.getEncoding(csvEncoding, log).toEncodingEnum

    PropertiesDto(
      connectorName = connectorName,
      apiUrl = apiUrl,
      authUrl = authUrl,
      workGroupId = wId,
      workGroupKey = wKey,
      projectId = projectUuid,
      csvEncoding = encoding,
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
