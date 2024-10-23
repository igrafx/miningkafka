package com.igrafx.kafka.sink.aggregationmain.adapters.services

import com.igrafx.kafka.sink.aggregationmain.Constants
import com.igrafx.kafka.sink.aggregationmain.domain.interfaces.KafkaTopicGetConfiguration
import org.apache.kafka.clients.admin.{AdminClient, AdminClientConfig, Config, ConfigEntry}
import org.apache.kafka.common.KafkaException
import org.apache.kafka.common.config.ConfigResource
import org.slf4j.Logger

import java.util
import java.util.{Collections, Properties}
import scala.jdk.CollectionConverters._
import scala.util.{Failure, Success, Try, Using}

class KafkaTopicGetConfigurationImpl extends KafkaTopicGetConfiguration {
  @throws[NumberFormatException]
  @throws[NoSuchElementException]
  @throws[KafkaException]
  def getTopicRetentionMs(topic: String, bootstrapServers: String, log: Logger): Long = {
    val configs = getTopicConfigurations(
      topic = topic,
      bootstrapServers = bootstrapServers,
      wantedConfiguration = Constants.retentionConfigurationName,
      log = log
    )

    Try[Long] {
      val retentionEntryOption: Option[ConfigEntry] =
        configs.head.entries().asScala.toSeq.find { entry: ConfigEntry =>
          entry.name() == Constants.retentionConfigurationName
        }

      val retention = retentionEntryOption match {
        case Some(retentionEntry) =>
          val retention = retentionEntry.value().toLong
          log.info(
            s"[CONFIGURATION] In accordance with the ${Constants.retentionConfigurationName} configuration of the $topic Kafka topic, the connector will send the data before their retention time of $retention milliseconds is reached"
              .replaceAll("[\r\n]", "")
          )
          retention
        case None =>
          log.error(
            s"The ${Constants.retentionConfigurationName} configuration is not defined for the $topic Kafka topic"
              .replaceAll("[\r\n]", "")
          )
          throw new NoSuchElementException(
            s"The ${Constants.retentionConfigurationName} configuration is not defined for the $topic Kafka topic"
              .replaceAll("[\r\n]", "")
          )
      }

      retention
    } match {
      case Success(retention) =>
        retention
      case Failure(exception: NumberFormatException) =>
        log.error(
          s"The value of the ${Constants.retentionConfigurationName} configuration of the $topic Kafka topic is not parsable to Long"
            .replaceAll("[\r\n]", ""),
          exception
        )
        throw exception
      case Failure(exception: NoSuchElementException) =>
        log.error(
          s"Either impossible to retrieve any configuration or the ${Constants.retentionConfigurationName} configuration is not defined for the $topic Kafka topic"
            .replaceAll("[\r\n]", ""),
          exception
        )
        throw exception
      case Failure(exception: Throwable) =>
        log.error(
          s"Unexpected exception while trying to retrieve the value of the ${Constants.retentionConfigurationName} configuration of the $topic Kafka topic"
            .replaceAll("[\r\n]", ""),
          exception
        )
        throw exception
    }
  }

  @throws[KafkaException]
  private def getTopicConfigurations(
      topic: String,
      bootstrapServers: String,
      wantedConfiguration: String,
      log: Logger
  ): Iterable[Config] = {
    val properties = new Properties()
    properties.put(AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers)
    Using(AdminClient.create(properties)) { adminClient =>
      val configResource = Collections.singleton(new ConfigResource(ConfigResource.Type.TOPIC, topic))
      val configResult = adminClient.describeConfigs(configResource)
      val configs: util.Collection[Config] = configResult.all().get().values()
      configs.asScala
    } match {
      case Success(configs) => configs
      case Failure(exception: KafkaException) =>
        log.error(
          s"Issue with the KafkaAdminClient while trying to retrieve the $topic Kafka topic configurations in order to obtain the value of the $wantedConfiguration configuration"
            .replaceAll("[\r\n]", ""),
          exception
        )
        throw exception
      case Failure(exception: Throwable) =>
        log.error(
          s"Unexpected exception while trying to retrieve the $topic Kafka topic configurations in order to obtain the value of the $wantedConfiguration configuration"
            .replaceAll("[\r\n]", ""),
          exception
        )
        throw exception
    }
  }
}