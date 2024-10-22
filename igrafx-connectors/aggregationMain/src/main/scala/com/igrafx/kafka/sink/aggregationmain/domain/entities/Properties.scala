package com.igrafx.kafka.sink.aggregationmain.domain.entities

import com.igrafx.kafka.sink.aggregationmain.domain.enums.EncodingEnum

import java.util.UUID

final case class Properties(
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
  require(
    csvFieldsNumber.number >= 3
      && retentionTimeInDay > 0
      && csvSeparator.length == 1
      && csvQuote.length == 1
      && elementNumberThreshold > 0
      && timeoutInSecondsThreshold > 0
  )
}
