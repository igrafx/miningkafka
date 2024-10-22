package com.igrafx.kafka.sink.aggregation.domain.enums

import org.apache.kafka.common.config.ConfigDef

sealed trait ConnectorPropertiesEnum {
  val toStringDescription: String
}

object ConnectorPropertiesEnum {
  case object topicOutProperty extends ConnectorPropertiesEnum {
    val toStringDescription: String = "topicOut"
  }
  case object aggregationColumnNameProperty extends ConnectorPropertiesEnum {
    val toStringDescription: String = "aggregationColumnName"
  }
  case object elementNumberThresholdProperty extends ConnectorPropertiesEnum {
    val toStringDescription: String = "threshold.elementNumber"
  }
  case object valuePatternThresholdProperty extends ConnectorPropertiesEnum {
    val toStringDescription: String = "threshold.valuePattern"
  }
  case object timeoutInSecondsThresholdProperty extends ConnectorPropertiesEnum {
    val toStringDescription: String = "threshold.timeoutInSeconds"
  }
  case object bootstrapServersProperty extends ConnectorPropertiesEnum {
    val toStringDescription: String = "bootstrap.servers"
  }

  // Added Connector Configuration Properties
  val configDef: ConfigDef = new ConfigDef()
    .define(
      ConnectorPropertiesEnum.topicOutProperty.toStringDescription,
      ConfigDef.Type.STRING,
      ConfigDef.NO_DEFAULT_VALUE,
      ConfigDef.Importance.HIGH,
      "The destination topic for aggregated data",
      "AGGREGATION",
      1,
      ConfigDef.Width.NONE,
      ConnectorPropertiesEnum.topicOutProperty.toStringDescription
    )
    .define(
      ConnectorPropertiesEnum.aggregationColumnNameProperty.toStringDescription,
      ConfigDef.Type.STRING,
      ConfigDef.NO_DEFAULT_VALUE,
      ConfigDef.Importance.HIGH,
      "The name of the column storing the aggregation in ksqlDB",
      "AGGREGATION",
      2,
      ConfigDef.Width.NONE,
      ConnectorPropertiesEnum.aggregationColumnNameProperty.toStringDescription
    )
    .define(
      ConnectorPropertiesEnum.elementNumberThresholdProperty.toStringDescription,
      ConfigDef.Type.INT,
      ConfigDef.NO_DEFAULT_VALUE,
      ConfigDef.Range.atLeast(1),
      ConfigDef.Importance.HIGH,
      "The number of elements necessary to send the aggregation to Kafka",
      "AGGREGATION",
      3,
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
      4,
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
      5,
      ConfigDef.Width.NONE,
      ConnectorPropertiesEnum.timeoutInSecondsThresholdProperty.toStringDescription
    )
    .define(
      ConnectorPropertiesEnum.bootstrapServersProperty.toStringDescription,
      ConfigDef.Type.STRING,
      ConfigDef.NO_DEFAULT_VALUE,
      ConfigDef.Importance.HIGH,
      "The Kafka Bootstrap Servers (example : broker:29092)",
      "AGGREGATION",
      6,
      ConfigDef.Width.NONE,
      ConnectorPropertiesEnum.bootstrapServersProperty.toStringDescription
    )
}
