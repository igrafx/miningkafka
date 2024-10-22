package com.igrafx.kafka.sink.aggregationmain.domain.columnmapping.controllers.dtos

import com.igrafx.kafka.sink.aggregationmain.domain.columnmapping.domain.entities.{
  ColumnMappingProperties,
  NonEmptyString
}
import com.igrafx.kafka.sink.aggregationmain.domain.entities.{Character, ColumnsNumber, Properties}
import com.igrafx.kafka.sink.aggregationmain.domain.enums.ConnectorPropertiesEnum
import com.igrafx.kafka.sink.aggregationmain.domain.exceptions.{
  InvalidColumnMappingException,
  InvalidPropertyValueException
}
import org.apache.kafka.common.config.AbstractConfig
import org.json4s.ParserUtil.ParseException
import org.slf4j.Logger

final case class ColumnMappingPropertiesDto(
    configurationProperties: ColumnMappingConfigurationPropertiesDto,
    columnMapping: ColumnMappingDto,
    fileStructure: FileStructureDto,
    csvFieldsNumber: ColumnsNumber
) {
  @throws[IllegalArgumentException]
  @throws[InvalidPropertyValueException]
  @throws[InvalidColumnMappingException]
  def toEntity(implicit log: Logger): ColumnMappingProperties = {
    ColumnMappingProperties(
      configurationProperties = configurationProperties.toEntity,
      columnMapping = columnMapping.toEntity(csvFieldsNumber),
      fileStructure = fileStructure.toEntity
    )
  }
}

object ColumnMappingPropertiesDto {
  @throws[InvalidPropertyValueException]
  @throws[ParseException]
  def fromConnectorConfig(
      connectorConfig: AbstractConfig,
      mandatoryProperties: Properties
  )(implicit log: Logger): ColumnMappingPropertiesDto = {
    val columnMapping = ColumnMappingDto.fromConnectorConfig(connectorConfig)

    val csvEndOfLineAsString =
      connectorConfig.getString(ConnectorPropertiesEnum.csvEndOfLineProperty.toStringDescription)
    val csvEscapeAsString = connectorConfig.getString(ConnectorPropertiesEnum.csvEscapeProperty.toStringDescription)
    val csvCommentAsString = connectorConfig.getString(ConnectorPropertiesEnum.csvCommentProperty.toStringDescription)
    val csvEndOfLine = NonEmptyString(csvEndOfLineAsString)
    val csvEscape = new Character(csvEscapeAsString)
    val csvComment = new Character(csvCommentAsString)
    val fileStructure = FileStructureDto(
      csvEncoding = mandatoryProperties.csvEncoding,
      csvSeparator = mandatoryProperties.csvSeparator,
      csvQuote = mandatoryProperties.csvQuote,
      csvEscape = csvEscape,
      csvEndOfLine = csvEndOfLine,
      csvHeader = mandatoryProperties.csvHeader,
      csvComment = csvComment
    )

    val configurationProperties = ColumnMappingConfigurationPropertiesDto(
      apiUrl = mandatoryProperties.apiUrl,
      authUrl = mandatoryProperties.authUrl,
      workGroupId = mandatoryProperties.workGroupId,
      workGroupKey = mandatoryProperties.workGroupKey,
      projectId = mandatoryProperties.projectId
    )

    ColumnMappingPropertiesDto(
      configurationProperties = configurationProperties,
      columnMapping = columnMapping,
      fileStructure = fileStructure,
      csvFieldsNumber = mandatoryProperties.csvFieldsNumber
    )
  }
}
