package com.igrafx.kafka.sink.aggregation.domain.usecases

import com.igrafx.kafka.sink.aggregation.adapters.services.KafkaTopicGetConfigurationImpl
import com.igrafx.kafka.sink.aggregation.domain.usecases.interfaces.KafkaTopicGetConfiguration
import org.apache.kafka.common.KafkaException
import org.slf4j.Logger

protected[aggregation] class ConnectorUseCases(private val log: Logger) {
  private[usecases] val kafkaTopicGetConfiguration: KafkaTopicGetConfiguration =
    new KafkaTopicGetConfigurationImpl

  /** Method used to retrieve the value of the max.message.bytes configuration of the Kafka topic
    *
    * @param topicOut The name of the Kafka topic
    * @param bootstrapServers The List of Kafka brokers
    *
    * @throws NumberFormatException If the value retrieved for max.message.bytes couldn't be parsed to Int
    * @throws NoSuchElementException Either impossible to retrieve any configuration or the topic or impossible to retrieve the max.message.bytes configuration
    * @throws KafkaException Issue with the KafkaAdminClient
    */
  def getTopicMaxMessageBytes(topicOut: String, bootstrapServers: String): Int = {
    kafkaTopicGetConfiguration.getTopicMaxMessageBytes(topicOut, bootstrapServers, log)
  }
}
