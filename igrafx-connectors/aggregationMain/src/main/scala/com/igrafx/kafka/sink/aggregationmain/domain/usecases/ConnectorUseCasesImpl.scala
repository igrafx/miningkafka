package com.igrafx.kafka.sink.aggregationmain.domain.usecases

import com.igrafx.kafka.sink.aggregationmain.domain.entities.{KafkaLoggingEventsProperties, Properties}
import org.slf4j.{Logger, LoggerFactory}

import scala.annotation.tailrec

protected[usecases] class ConnectorUseCasesImpl extends ConnectorUseCases {
  private implicit val log: Logger = LoggerFactory.getLogger(classOf[ConnectorUseCasesImpl])

  /** Method used to create recursively the list with the configurations we associate to tasks
    *
    * @param nbTasks must be >= 0
    * @param configs Seq in construction with the configuration properties needed by Tasks, each Properties is associated with a Task
    * @param properties corresponds to the connector's properties
    *
    * @return Seq with the configuration properties needed by Tasks, each Properties is associated with a Task
    *
    * @throws IllegalArgumentException if nbTasks < 0
    */
  @tailrec
  final def createConfigsRec(
      nbTasks: Int,
      configs: Seq[Properties],
      properties: Properties
  ): Seq[Properties] = {
    nbTasks match {
      case value if value > 0 =>
        createConfigsRec(
          nbTasks - 1,
          configs :+ properties,
          properties
        )
      case 0 => configs //Cas d'arrêt de la récursion
      case value =>
        log.error(
          s"[IGrafxSinkConnector.createConfigRec] IllegalArgumentException : Issue with the number of desired tasks : $value is inferior than 0 and the number of desired tasks ($value) can't be inferior than 0"
        )
        throw new IllegalArgumentException(
          s"The number of desired tasks ($value) can't be inferior than 0"
        ) //Cas d'erreur

    }
  }

  /** Method used to create recursively the list with the Kafka Logging Events configurations we associate to tasks
    *
    * @param nbTasks must be >= 0
    * @param configs Seq in construction with the configuration properties needed by Tasks, each KafkaLoggingEventsProperties is associated with a Task
    * @param kafkaLoggingEventsProperties corresponds to the Kafka Logging Events connector's properties
    *
    * @return Seq with the configuration properties needed by Tasks, each KafkaLoggingEventsProperties is associated with a Task
    *
    * @throws IllegalArgumentException if nbTasks < 0
    */
  @tailrec
  final def createKafkaLoggingEventsConfigsRec(
      nbTasks: Int,
      configs: Seq[KafkaLoggingEventsProperties],
      kafkaLoggingEventsProperties: KafkaLoggingEventsProperties
  ): Seq[KafkaLoggingEventsProperties] = {
    nbTasks match {
      case value if value > 0 =>
        createKafkaLoggingEventsConfigsRec(
          nbTasks - 1,
          configs :+ kafkaLoggingEventsProperties,
          kafkaLoggingEventsProperties
        )
      case 0 => configs //Cas d'arrêt de la récursion
      case value =>
        log.error(
          s"[IGrafxSinkConnector.createKafkaLoggingEventsConfigsRec] IllegalArgumentException : Issue with the number of desired tasks : $value is inferior than 0 and the number of desired tasks ($value) can't be inferior than 0"
        )
        throw new IllegalArgumentException(
          s"The number of desired tasks ($value) can't be inferior than 0"
        ) //Cas d'erreur
    }
  }
}
