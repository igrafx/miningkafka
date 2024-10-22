package com.igrafx.kafka.sink.aggregationmain.domain.recommenders

import com.igrafx.kafka.sink.aggregationmain.domain.enums.ConnectorPropertiesEnum
import org.apache.kafka.common.config.ConfigDef

import java.util
import scala.jdk.CollectionConverters._
import scala.util.{Success, Try}

class KafkaLoggingEventsVisibilityRecommender extends ConfigDef.Recommender {
  override def validValues(name: String, parsedConfig: util.Map[String, AnyRef]): util.List[AnyRef] = {
    Seq.empty[AnyRef].asJava
  }

  override def visible(name: String, parsedConfig: util.Map[String, AnyRef]): Boolean = {
    val kafkaLoggingEventsSendInformationValue =
      parsedConfig
        .getOrDefault(ConnectorPropertiesEnum.kafkaLoggingEventsIsLoggingProperty.toStringDescription, "false")
        .toString

    Try {
      kafkaLoggingEventsSendInformationValue.toBoolean
    } match {
      case Success(kafkaLoggingEventsSendInformationBoolean) => kafkaLoggingEventsSendInformationBoolean
      case _ => false
    }
  }
}
