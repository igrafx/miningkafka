package com.igrafx.kafka.sink.aggregationmain

object Constants {
  val delimiterForPropertiesWithListValue = ","
  val delimiterForElementInPropertyValue = ";"
  val characterForElementStartInPropertyValue = '{'
  val characterForElementEndInPropertyValue = '}'
  val timeoutFutureValueInSeconds = 3600
  val timeoutApiCallValueInMilliSeconds = 300000

  val headerValueProperty = "header.value"
  val caseIdColumnName = "caseId"
  val activityColumnName = "activity"
  val timestampColumnName = "timestamp"
  val columnIndexDefaultValue: Int = -1
  val columnMappingStringDefaultValue: String = ""
  val minimumColumnsNumber = 3 // (caseId, activity, date)

  val kafkaLoggingEventTypeColumnName = "EVENT_TYPE"
  val kafkaLoggingIGrafxProjectColumnName = "IGRAFX_PROJECT"
  val kafkaLoggingEventDateColumnName = "EVENT_DATE"
  val kafkaLoggingEventSequenceIdColumnName = "EVENT_SEQUENCE_ID"
  val kafkaLoggingPayloadColumnName = "PAYLOAD"

  val connectorNameProperty = "name"
  val topicsProperty = "topics"
  val retentionConfigurationName = "retention.ms"
  val schemaRegistryUrlProperty = "value.converter.schema.registry.url"
}
