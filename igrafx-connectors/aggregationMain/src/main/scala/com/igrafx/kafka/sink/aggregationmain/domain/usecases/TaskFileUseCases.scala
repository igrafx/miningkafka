package com.igrafx.kafka.sink.aggregationmain.domain.usecases

import com.igrafx.kafka.sink.aggregationmain.domain.entities.{
  CsvProperties,
  DebugInformation,
  Event,
  KafkaLoggingEventsProperties,
  Properties
}

import scala.concurrent.Future

trait TaskFileUseCases {
  def initializeDeleteAndSendNewDataToIGrafx(
      properties: Properties,
      csvProperties: CsvProperties,
      kafkaLoggingEventsPropertiesOpt: Option[KafkaLoggingEventsProperties],
      lines: Iterable[Event],
      aggregationInformation: DebugInformation
  ): Future[Unit]

}

object TaskFileUseCases {
  lazy val instance: TaskFileUseCases = new TaskFileUseCasesImpl
}
