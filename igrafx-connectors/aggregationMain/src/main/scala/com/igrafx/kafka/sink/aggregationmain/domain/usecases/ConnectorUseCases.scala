package com.igrafx.kafka.sink.aggregationmain.domain.usecases

import com.igrafx.kafka.sink.aggregationmain.domain.entities.{KafkaLoggingEventsProperties, Properties}

trait ConnectorUseCases {
  def createConfigsRec(
      nbTasks: Int,
      configs: Seq[Properties],
      properties: Properties
  ): Seq[Properties]

  def createKafkaLoggingEventsConfigsRec(
      nbTasks: Int,
      configs: Seq[KafkaLoggingEventsProperties],
      kafkaLoggingEventsProperties: KafkaLoggingEventsProperties
  ): Seq[KafkaLoggingEventsProperties]
}

object ConnectorUseCases {
  lazy val instance: ConnectorUseCases = new ConnectorUseCasesImpl
}
