package com.igrafx.kafka.sink.aggregationmain.domain.enums

import com.igrafx.kafka.sink.aggregationmain.Constants
import com.igrafx.kafka.sink.aggregationmain.domain.recommenders.{ColumnMappingPropertyVisibilityRecommender, CsvEncodingRecommender, KafkaLoggingEventsVisibilityRecommender}
import com.igrafx.kafka.sink.aggregationmain.domain.validators.{BooleanValidator, CsvCharactersValidator, CsvEncodingValidator, ProjectIdValidator}
import org.apache.kafka.common.config.ConfigDef

import java.util

sealed trait ConnectorPropertiesEnum {
  val toStringDescription: String
}

object ConnectorPropertiesEnum {
  final case object apiUrlProperty extends ConnectorPropertiesEnum {
    val toStringDescription: String = "api.url"
  }
  final case object authUrlProperty extends ConnectorPropertiesEnum {
    val toStringDescription: String = "api.authUrl"
  }
  final case object workGroupIdProperty extends ConnectorPropertiesEnum {
    val toStringDescription: String = "workGroupId"
  }
  final case object workGroupKeyProperty extends ConnectorPropertiesEnum {
    val toStringDescription: String = "workGroupKey"
  }
  final case object projectIdProperty extends ConnectorPropertiesEnum {
    val toStringDescription: String = "projectId"
  }
  final case object csvEncodingProperty extends ConnectorPropertiesEnum {
    val toStringDescription: String = "csv.encoding"
  }
  final case object csvSeparatorProperty extends ConnectorPropertiesEnum {
    val toStringDescription: String = "csv.separator"
  }
  final case object csvQuoteProperty extends ConnectorPropertiesEnum {
    val toStringDescription: String = "csv.quote"
  }
  final case object csvHeaderProperty extends ConnectorPropertiesEnum {
    val toStringDescription: String = "csv.header"
  }
  final case object csvFieldsNumberProperty extends ConnectorPropertiesEnum {
    val toStringDescription: String = "csv.fieldsNumber"
  }
  final case object csvDefaultTextValueProperty extends ConnectorPropertiesEnum {
    val toStringDescription: String = "csv.defaultTextValue"
  }
  final case object retentionTimeInDayProperty extends ConnectorPropertiesEnum {
    val toStringDescription: String = "retentionTimeInDay"
  }
  final case object columnMappingCreateProperty extends ConnectorPropertiesEnum {
    val toStringDescription: String = "columnMapping.create"
  }
  final case object columnMappingCaseIdColumnProperty extends ConnectorPropertiesEnum {
    val toStringDescription: String = "columnMapping.caseIdColumnIndex"
  }
  final case object columnMappingActivityColumnProperty extends ConnectorPropertiesEnum {
    val toStringDescription: String = "columnMapping.activityColumnIndex"
  }
  final case object columnMappingTimeColumnsProperty extends ConnectorPropertiesEnum {
    val toStringDescription: String = "columnMapping.timeInformationList"
  }
  final case object columnMappingDimensionColumnsProperty extends ConnectorPropertiesEnum {
    val toStringDescription: String = "columnMapping.dimensionsInformationList"
  }
  final case object columnMappingMetricColumnsProperty extends ConnectorPropertiesEnum {
    val toStringDescription: String = "columnMapping.metricsInformationList"
  }
  final case object columnMappingGroupedTasksColumnsProperty extends ConnectorPropertiesEnum {
    val toStringDescription: String = "columnMapping.groupedTasksColumns"
  }
  final case object csvEndOfLineProperty extends ConnectorPropertiesEnum {
    val toStringDescription: String = "csv.endOfLine"
  }
  final case object csvEscapeProperty extends ConnectorPropertiesEnum {
    val toStringDescription: String = "csv.escape"
  }
  final case object csvCommentProperty extends ConnectorPropertiesEnum {
    val toStringDescription: String = "csv.comment"
  }
  final case object kafkaLoggingEventsIsLoggingProperty extends ConnectorPropertiesEnum {
    val toStringDescription: String = "kafkaLoggingEvents.isLogging"
  }
  final case object kafkaLoggingEventsTopicProperty extends ConnectorPropertiesEnum {
    val toStringDescription: String = "kafkaLoggingEvents.topic"
  }
  final case object elementNumberThresholdProperty extends ConnectorPropertiesEnum {
    val toStringDescription: String = "threshold.elementNumber"
  }
  final case object valuePatternThresholdProperty extends ConnectorPropertiesEnum {
    val toStringDescription: String = "threshold.valuePattern"
  }
  final case object timeoutInSecondsThresholdProperty extends ConnectorPropertiesEnum {
    val toStringDescription: String = "threshold.timeoutInSeconds"
  }
  final case object bootstrapServersProperty extends ConnectorPropertiesEnum {
    val toStringDescription: String = "bootstrap.servers"
  }

  // Added Connector Configuration Properties
  val configDef: ConfigDef = new ConfigDef()
    .define(
      ConnectorPropertiesEnum.apiUrlProperty.toStringDescription,
      ConfigDef.Type.STRING,
      ConfigDef.NO_DEFAULT_VALUE,
      ConfigDef.Importance.HIGH,
      "The url to query for API calls",
      "API",
      1,
      ConfigDef.Width.MEDIUM,
      ConnectorPropertiesEnum.apiUrlProperty.toStringDescription
    )
    .define(
      ConnectorPropertiesEnum.authUrlProperty.toStringDescription,
      ConfigDef.Type.STRING,
      ConfigDef.NO_DEFAULT_VALUE,
      ConfigDef.Importance.HIGH,
      "The url to query for authentication calls",
      "API",
      2,
      ConfigDef.Width.MEDIUM,
      ConnectorPropertiesEnum.authUrlProperty.toStringDescription
    )
    .define(
      ConnectorPropertiesEnum.workGroupIdProperty.toStringDescription,
      ConfigDef.Type.STRING,
      ConfigDef.NO_DEFAULT_VALUE,
      ConfigDef.Importance.HIGH,
      "The workgroup ID",
      "API",
      3,
      ConfigDef.Width.MEDIUM,
      ConnectorPropertiesEnum.workGroupIdProperty.toStringDescription
    )
    .define(
      ConnectorPropertiesEnum.workGroupKeyProperty.toStringDescription,
      ConfigDef.Type.STRING,
      ConfigDef.NO_DEFAULT_VALUE,
      ConfigDef.Importance.HIGH,
      "The workgroup secret key",
      "API",
      4,
      ConfigDef.Width.MEDIUM,
      ConnectorPropertiesEnum.workGroupKeyProperty.toStringDescription
    )
    .define(
      ConnectorPropertiesEnum.projectIdProperty.toStringDescription,
      ConfigDef.Type.STRING,
      ConfigDef.NO_DEFAULT_VALUE,
      new ProjectIdValidator(),
      ConfigDef.Importance.HIGH,
      "The project ID",
      "API",
      5,
      ConfigDef.Width.MEDIUM,
      ConnectorPropertiesEnum.projectIdProperty.toStringDescription
    )
    .define(
      ConnectorPropertiesEnum.csvEncodingProperty.toStringDescription,
      ConfigDef.Type.STRING,
      ConfigDef.NO_DEFAULT_VALUE,
      new CsvEncodingValidator(),
      ConfigDef.Importance.HIGH,
      "The encoding of the csv file (UTF-8/ASCII/ISO-8859-1)",
      "File Structure",
      1,
      ConfigDef.Width.MEDIUM,
      ConnectorPropertiesEnum.csvEncodingProperty.toStringDescription,
      new CsvEncodingRecommender()
    )
    .define(
      ConnectorPropertiesEnum.csvSeparatorProperty.toStringDescription,
      ConfigDef.Type.STRING,
      ConfigDef.NO_DEFAULT_VALUE,
      new CsvCharactersValidator(),
      ConfigDef.Importance.HIGH,
      "The separator character between two fields of the csv file",
      "File Structure",
      2,
      ConfigDef.Width.SHORT,
      ConnectorPropertiesEnum.csvSeparatorProperty.toStringDescription
    )
    .define(
      ConnectorPropertiesEnum.csvQuoteProperty.toStringDescription,
      ConfigDef.Type.STRING,
      ConfigDef.NO_DEFAULT_VALUE,
      new CsvCharactersValidator(),
      ConfigDef.Importance.HIGH,
      "The character used to quote in the csv file",
      "File Structure",
      3,
      ConfigDef.Width.SHORT,
      ConnectorPropertiesEnum.csvQuoteProperty.toStringDescription
    )
    .define(
      ConnectorPropertiesEnum.csvFieldsNumberProperty.toStringDescription,
      ConfigDef.Type.INT,
      ConfigDef.NO_DEFAULT_VALUE,
      ConfigDef.Range.atLeast(Constants.minimumColumnsNumber),
      ConfigDef.Importance.HIGH,
      s"The number of fields in a csv line, should be at least ${Constants.minimumColumnsNumber} (caseId, activity, date)",
      "File Structure",
      4,
      ConfigDef.Width.SHORT,
      ConnectorPropertiesEnum.csvFieldsNumberProperty.toStringDescription
    )
    .define(
      ConnectorPropertiesEnum.csvHeaderProperty.toStringDescription,
      ConfigDef.Type.BOOLEAN,
      ConfigDef.NO_DEFAULT_VALUE,
      ConfigDef.Importance.HIGH,
      "Boolean telling whether the csv file has a header or not (true/false)",
      "File Structure",
      5,
      ConfigDef.Width.MEDIUM,
      ConnectorPropertiesEnum.csvHeaderProperty.toStringDescription
    )
    .define(
      ConnectorPropertiesEnum.csvDefaultTextValueProperty.toStringDescription,
      ConfigDef.Type.STRING,
      ConfigDef.NO_DEFAULT_VALUE,
      ConfigDef.Importance.HIGH,
      "Text to use in csv file for a missing column",
      "File Structure",
      6,
      ConfigDef.Width.MEDIUM,
      ConnectorPropertiesEnum.csvDefaultTextValueProperty.toStringDescription
    )
    .define(
      ConnectorPropertiesEnum.retentionTimeInDayProperty.toStringDescription,
      ConfigDef.Type.INT,
      ConfigDef.NO_DEFAULT_VALUE,
      ConfigDef.Range.atLeast(1),
      ConfigDef.Importance.HIGH,
      "Retention time of an archived file in Days",
      "Other",
      4,
      ConfigDef.Width.MEDIUM,
      ConnectorPropertiesEnum.retentionTimeInDayProperty.toStringDescription
    )
    .define(
      ConnectorPropertiesEnum.columnMappingCreateProperty.toStringDescription,
      ConfigDef.Type.BOOLEAN,
      false,
      new BooleanValidator(),
      ConfigDef.Importance.HIGH,
      "Boolean indicating if the connector will create a Column Mapping for the project",
      "Column Mapping",
      1,
      ConfigDef.Width.MEDIUM,
      ConnectorPropertiesEnum.columnMappingCreateProperty.toStringDescription
    )
    .define(
      ConnectorPropertiesEnum.columnMappingCaseIdColumnProperty.toStringDescription,
      ConfigDef.Type.INT,
      Constants.columnIndexDefaultValue,
      ConfigDef.Importance.HIGH,
      "Index of the CaseId column in csv files",
      "Column Mapping",
      2,
      ConfigDef.Width.MEDIUM,
      ConnectorPropertiesEnum.columnMappingCaseIdColumnProperty.toStringDescription,
      util.Arrays.asList(
        ConnectorPropertiesEnum.columnMappingCreateProperty.toStringDescription,
        ConnectorPropertiesEnum.csvFieldsNumberProperty.toStringDescription
      ),
      new ColumnMappingPropertyVisibilityRecommender()
    )
    .define(
      ConnectorPropertiesEnum.columnMappingActivityColumnProperty.toStringDescription,
      ConfigDef.Type.INT,
      Constants.columnIndexDefaultValue,
      ConfigDef.Importance.HIGH,
      s"Index of the Activity column in csv files",
      "Column Mapping",
      3,
      ConfigDef.Width.MEDIUM,
      ConnectorPropertiesEnum.columnMappingActivityColumnProperty.toStringDescription,
      util.Arrays.asList(
        ConnectorPropertiesEnum.columnMappingCreateProperty.toStringDescription,
        ConnectorPropertiesEnum.csvFieldsNumberProperty.toStringDescription
      ),
      new ColumnMappingPropertyVisibilityRecommender()
    )
    .define(
      ConnectorPropertiesEnum.columnMappingTimeColumnsProperty.toStringDescription,
      ConfigDef.Type.STRING,
      Constants.columnMappingStringDefaultValue,
      ConfigDef.Importance.HIGH,
      s"List with information about Time columns following the format : ${Constants.characterForElementStartInPropertyValue}columnIndex${Constants.delimiterForElementInPropertyValue}dateFormat${Constants.characterForElementEndInPropertyValue} and separated by a '${Constants.delimiterForPropertiesWithListValue}' if there are two Time columns. For instance : ${Constants.characterForElementStartInPropertyValue}2${Constants.delimiterForElementInPropertyValue}dd/MM/yy HH:mm${Constants.characterForElementEndInPropertyValue}${Constants.delimiterForPropertiesWithListValue}${Constants.characterForElementStartInPropertyValue}3${Constants.delimiterForElementInPropertyValue}dd/MM/yy HH:mm${Constants.characterForElementEndInPropertyValue}",
      "Column Mapping",
      4,
      ConfigDef.Width.MEDIUM,
      ConnectorPropertiesEnum.columnMappingTimeColumnsProperty.toStringDescription,
      util.Arrays
        .asList(
          ConnectorPropertiesEnum.columnMappingCreateProperty.toStringDescription,
          ConnectorPropertiesEnum.csvFieldsNumberProperty.toStringDescription
        ),
      new ColumnMappingPropertyVisibilityRecommender()
    )
    .define(
      ConnectorPropertiesEnum.columnMappingDimensionColumnsProperty.toStringDescription,
      ConfigDef.Type.STRING,
      Constants.columnMappingStringDefaultValue,
      ConfigDef.Importance.HIGH,
      s"Information about Dimension columns using the JSON format. For instance : [{\"columnIndex\": 4, \"name\": \"Country\", \"isCaseScope\": true, \"aggregation\": \"FIRST\"}, {\"columnIndex\": 5, \"name\": \"Region\", \"isCaseScope\": false}]",
      "Column Mapping",
      5,
      ConfigDef.Width.MEDIUM,
      ConnectorPropertiesEnum.columnMappingDimensionColumnsProperty.toStringDescription,
      util.Arrays
        .asList(
          ConnectorPropertiesEnum.columnMappingCreateProperty.toStringDescription,
          ConnectorPropertiesEnum.csvFieldsNumberProperty.toStringDescription
        ),
      new ColumnMappingPropertyVisibilityRecommender()
    )
    .define(
      ConnectorPropertiesEnum.columnMappingMetricColumnsProperty.toStringDescription,
      ConfigDef.Type.STRING,
      Constants.columnMappingStringDefaultValue,
      ConfigDef.Importance.HIGH,
      s"Information about Metric columns using the JSON format. For instance : [{\"columnIndex\": 6, \"name\": \"Price\", \"unit\": \"Euros\", \"isCaseScope\": true, \"aggregation\": \"MIN\"}, {\"columnIndex\": 7, \"name\": \"DepartmentNumber\", \"isCaseScope\": false}]",
      "Column Mapping",
      6,
      ConfigDef.Width.MEDIUM,
      ConnectorPropertiesEnum.columnMappingMetricColumnsProperty.toStringDescription,
      util.Arrays
        .asList(
          ConnectorPropertiesEnum.columnMappingCreateProperty.toStringDescription,
          ConnectorPropertiesEnum.csvFieldsNumberProperty.toStringDescription
        ),
      new ColumnMappingPropertyVisibilityRecommender()
    )
    .define(ConnectorPropertiesEnum.columnMappingGroupedTasksColumnsProperty.toStringDescription,
      ConfigDef.Type.STRING,
      Constants.columnMappingStringDefaultValue,
      ConfigDef.Importance.HIGH,
      s"Information about grouped tasks columns indexes using the JSON format. For instance : [1, 3]",
      "Column Mapping",
      7,
      ConfigDef.Width.MEDIUM,
      ConnectorPropertiesEnum.columnMappingGroupedTasksColumnsProperty.toStringDescription,
      util.Arrays
        .asList(
          ConnectorPropertiesEnum.columnMappingCreateProperty.toStringDescription,
          ConnectorPropertiesEnum.csvFieldsNumberProperty.toStringDescription
        ),
      new ColumnMappingPropertyVisibilityRecommender()
    )
    .define(
      ConnectorPropertiesEnum.csvEndOfLineProperty.toStringDescription,
      ConfigDef.Type.STRING,
      Constants.columnMappingStringDefaultValue,
      ConfigDef.Importance.HIGH,
      "The character used to specify the end of a line in the csv file",
      "File Structure",
      7,
      ConfigDef.Width.MEDIUM,
      ConnectorPropertiesEnum.csvEndOfLineProperty.toStringDescription,
      util.Arrays.asList(ConnectorPropertiesEnum.columnMappingCreateProperty.toStringDescription),
      new ColumnMappingPropertyVisibilityRecommender()
    )
    .define(
      ConnectorPropertiesEnum.csvEscapeProperty.toStringDescription,
      ConfigDef.Type.STRING,
      Constants.columnMappingStringDefaultValue,
      ConfigDef.Importance.HIGH,
      "The character used to escape an other character in the csv file",
      "File Structure",
      8,
      ConfigDef.Width.MEDIUM,
      ConnectorPropertiesEnum.csvEscapeProperty.toStringDescription,
      util.Arrays.asList(ConnectorPropertiesEnum.columnMappingCreateProperty.toStringDescription),
      new ColumnMappingPropertyVisibilityRecommender()
    )
    .define(
      ConnectorPropertiesEnum.csvCommentProperty.toStringDescription,
      ConfigDef.Type.STRING,
      Constants.columnMappingStringDefaultValue,
      ConfigDef.Importance.HIGH,
      "The character used to specify the end of a line in the csv file",
      "File Structure",
      9,
      ConfigDef.Width.MEDIUM,
      ConnectorPropertiesEnum.csvCommentProperty.toStringDescription,
      util.Arrays.asList(ConnectorPropertiesEnum.columnMappingCreateProperty.toStringDescription),
      new ColumnMappingPropertyVisibilityRecommender()
    )
    .define(
      ConnectorPropertiesEnum.kafkaLoggingEventsIsLoggingProperty.toStringDescription,
      ConfigDef.Type.BOOLEAN,
      false,
      new BooleanValidator(),
      ConfigDef.Importance.HIGH,
      "Boolean indicating if the connector sends events information to a logging kafka topic or not",
      "Kafka Logging",
      1,
      ConfigDef.Width.MEDIUM,
      ConnectorPropertiesEnum.kafkaLoggingEventsIsLoggingProperty.toStringDescription
    )
    .define(
      ConnectorPropertiesEnum.kafkaLoggingEventsTopicProperty.toStringDescription,
      ConfigDef.Type.STRING,
      "",
      ConfigDef.Importance.HIGH,
      "Name of the Kafka topic logging the events information",
      "Kafka Logging",
      2,
      ConfigDef.Width.MEDIUM,
      ConnectorPropertiesEnum.kafkaLoggingEventsTopicProperty.toStringDescription,
      util.Arrays.asList(
        ConnectorPropertiesEnum.kafkaLoggingEventsIsLoggingProperty.toStringDescription
      ),
      new KafkaLoggingEventsVisibilityRecommender()
    )
    .define(
      ConnectorPropertiesEnum.elementNumberThresholdProperty.toStringDescription,
      ConfigDef.Type.INT,
      ConfigDef.NO_DEFAULT_VALUE,
      ConfigDef.Range.atLeast(1),
      ConfigDef.Importance.HIGH,
      "The number of elements necessary to send the aggregation to Kafka",
      "AGGREGATION",
      1,
      ConfigDef.Width.NONE,
      ConnectorPropertiesEnum.elementNumberThresholdProperty.toStringDescription
    )
    .define(
      ConnectorPropertiesEnum.valuePatternThresholdProperty.toStringDescription,
      ConfigDef.Type.STRING,
      "",
      ConfigDef.Importance.HIGH,
      "The Regex Pattern causing a flush of the current aggregation if the value received matches it",
      "AGGREGATION",
      2,
      ConfigDef.Width.NONE,
      ConnectorPropertiesEnum.valuePatternThresholdProperty.toStringDescription
    )
    .define(
      ConnectorPropertiesEnum.timeoutInSecondsThresholdProperty.toStringDescription,
      ConfigDef.Type.INT,
      ConfigDef.NO_DEFAULT_VALUE,
      ConfigDef.Range.atLeast(1),
      ConfigDef.Importance.HIGH,
      "The maximum time between the start of a new aggregation and its sending to Kafka",
      "AGGREGATION",
      3,
      ConfigDef.Width.NONE,
      ConnectorPropertiesEnum.timeoutInSecondsThresholdProperty.toStringDescription
    )
    .define(
      ConnectorPropertiesEnum.bootstrapServersProperty.toStringDescription,
      ConfigDef.Type.STRING,
      ConfigDef.NO_DEFAULT_VALUE,
      ConfigDef.Importance.HIGH,
      "The Kafka Bootstrap Servers (example : broker:29092)",
      "MANDATORY",
      1,
      ConfigDef.Width.NONE,
      ConnectorPropertiesEnum.bootstrapServersProperty.toStringDescription
    )
}
