package com.igrafx.kafka.sink.aggregation.adapters

import com.igrafx.kafka.sink.aggregation.Constants
import com.igrafx.kafka.sink.aggregation.adapters.dtos.PropertiesDto
import com.igrafx.kafka.sink.aggregation.domain.entities.Properties
import com.igrafx.kafka.sink.aggregation.domain.enums.ConnectorPropertiesEnum
import com.igrafx.kafka.sink.aggregation.domain.usecases.ConnectorUseCases
import org.apache.kafka.common.KafkaException
import org.apache.kafka.common.config._
import org.apache.kafka.connect.connector.Task
import org.apache.kafka.connect.sink.SinkConnector
import org.slf4j.{Logger, LoggerFactory}

import java.util
import scala.annotation.tailrec
import scala.jdk.CollectionConverters._
import scala.util.{Failure, Success, Try}

class AggregationSinkConnector extends SinkConnector {
  private val log: Logger = LoggerFactory.getLogger(getClass)

  private[adapters] val connectorUseCases: ConnectorUseCases = new ConnectorUseCases(log)

  private var propertiesOpt: Option[Properties] = None
  private var kafkaMaxMessageBytesOpt: Option[Int] = None

  /** Method called at the start of the Connector, used here to retrieve configuration
    *
    * @param map java.util.Map[String, String] with the Connector properties, the key corresponds to the name of the property and the value corresponds to its value. The following keys need to be defined :
    * - name
    * - topics
    * - ConnectorPropertiesEnum.topicOutProperty, value can't be empty
    * - ConnectorPropertiesEnum.elementNumberThresholdProperty, value needs to be >= 1
    * - ConnectorPropertiesEnum.valuePatternThresholdProperty
    * - ConnectorPropertiesEnum.timeoutInSecondsThresholdProperty, value needs to be >= 1
    * - ConnectorPropertiesEnum.bootstrapServersProperty
    * - Constants.schemaRegistryUrlProperty
    *
    * @throws ConfigException from startConfig : At least one of the mandatory properties is missing
    * @throws IllegalArgumentException from startConfig : A requirement in Properties is not verified. Requirements are : topicOut.nonEmpty && elementNumberThreshold > 0 && timeoutInSecondsThreshold > 0
    */
  override def start(map: util.Map[String, String]): Unit = {
    log.info(s"[AggregationSinkConnector] Starting Kafka Sink Connector ${this.getClass.getName}")
    val connectorConfig = new AbstractConfig(ConnectorPropertiesEnum.configDef, map)
    val immutableMap = map.asScala.toMap
    startConfig(map = immutableMap, connectorConfig = connectorConfig)
  }

  /** Method called to retrieve and check the properties of the connector
    * Warning : the "propertiesOpt" and "maxMessageBytesOpt" class attributes are modified in this method
    *
    * @param map scala.collection.immutable.Map[String, String] with the Connector properties
    * @param connectorConfig The AbstractConfig corresponding to the connector's properties
    *
    * @throws ConfigException At least one of the mandatory properties is missing
    * @throws IllegalArgumentException A requirement in Properties is not verified. Requirements are : topicOut.nonEmpty && elementNumberThreshold > 0 && timeoutInSecondsThreshold > 0
    */
  private[aggregation] def startConfig(map: Map[String, String], connectorConfig: AbstractConfig): Unit = {
    val connectorName: String = map.get(Constants.connectorNameProperty) match {
      case Some(name) => name
      case None =>
        log.error(
          s"[AggregationSinkConnector.startConfig] ConfigException : The property '${Constants.connectorNameProperty}' needs to be defined"
        )
        throw new ConfigException(
          s"ConfigException : The property '${Constants.connectorNameProperty}' needs to be defined"
        )
    }
    val topics: String = map.get(Constants.topicsProperty) match {
      case Some(topics) => topics
      case None =>
        log.error(
          s"[AggregationSinkConnector.startConfig] ConfigException : The property '${Constants.topicsProperty}' needs to be defined"
        )
        throw new ConfigException(s"ConfigException : The property '${Constants.topicsProperty}' needs to be defined")
    }

    val schemaRegistryUrl: String = map.get(Constants.schemaRegistryUrlProperty) match {
      case Some(schemaRegistryUrl) => schemaRegistryUrl
      case None =>
        log.error(
          s"[AggregationSinkConnector.startConfig] ConfigException : The property '${Constants.schemaRegistryUrlProperty}' needs to be defined"
        )
        throw new ConfigException(
          s"ConfigException : The property '${Constants.schemaRegistryUrlProperty}' needs to be defined"
        )
    }

    Try {
      val propertiesFromConnectorConfig: Properties =
        PropertiesDto.fromConnectorConfig(connectorConfig, connectorName, schemaRegistryUrl).toProperties
      propertiesOpt = Some(propertiesFromConnectorConfig)

      val kafkaMaxMessageBytes =
        connectorUseCases.getTopicMaxMessageBytes(
          topicOut = propertiesFromConnectorConfig.topicOut,
          bootstrapServers = propertiesFromConnectorConfig.bootstrapServers
        )
      kafkaMaxMessageBytesOpt = Some(kafkaMaxMessageBytes)

      debugPrints(propertiesOpt, topics)
    } match {
      case Success(_) => ()
      case Failure(exception: NumberFormatException) => throw exception
      case Failure(exception: NoSuchElementException) => throw exception
      case Failure(exception: KafkaException) => throw exception
      case Failure(exception: Throwable) =>
        log.error("Issue with at least one of the connector's properties", exception)
        throw exception
    }
  }

  override def taskClass(): Class[_ <: Task] = {
    classOf[AggregationSinkTask]
  }

  /** Method used to specify the number of Tasks created for this Connector and to send the Configuration Properties to each Task
    *
    * Here the size of the list returned by the method corresponds to the number of Tasks that will be created and each Map
    * in this list corresponds to the Configuration Properties given to its associated Task
    *
    * Warning : Method not pure as there is a dependency towards the mutable class attribute "properties"
    *
    * @param maxTasks maximum number of Tasks created for this Connector
    *
    * @throws ConfigException Issue with the connector properties
    */
  override def taskConfigs(maxTasks: Int): util.List[util.Map[String, String]] = {
    propertiesOpt match {
      case None =>
        log.error("[AggregationSinkConnector.taskConfigs] Connector properties are not valid")
        throw new ConfigException("Connector properties are not valid")
      case Some(properties) =>
        kafkaMaxMessageBytesOpt match {
          case None =>
            log.error(
              "[AggregationSinkConnector.taskConfigs] No information about the max message bytes we can send to Kafka"
            )
            throw new ConfigException(
              "No information about the max message bytes we can send to Kafka"
            )
          case Some(maxMessageBytes) =>
            createConfigsRec(nbTasks = maxTasks, configs = Seq.empty[Properties], properties = properties)
              .map(
                PropertiesDto
                  .fromProperties(_)
                  .toMap
                  .+(Constants.maxMessageBytesConfigurationName -> maxMessageBytes.toString)
                  .asJava
              )
              .asJava
        }
    }
  }

  /** Method called when we stop the Connector */
  override def stop(): Unit = {
    log.info(s"[AggregationSinkConnector] Stopping Kafka Sink Connector ${this.getClass.getName}")
  }

  override def config(): ConfigDef = {
    ConnectorPropertiesEnum.configDef
  }

  override def version(): String = {
    "1.0.0"
  }

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
  ): Seq[Properties] =
    nbTasks match {
      case 0 => configs
      case value if value < 0 =>
        log.error(
          s"[IGrafxSinkConnector.createConfigRec] IllegalArgumentException : Issue with the number of desired tasks : $value is inferior than 0 and the number of desired tasks ($value) can't be inferior than 0"
        )
        throw new IllegalArgumentException(s"The number of desired tasks ($value) can't be inferior than 0")
      case _ =>
        createConfigsRec(nbTasks = nbTasks - 1, configs = configs :+ properties, properties = properties)
    }

  private def debugPrints(
      properties: Option[Properties],
      topics: String
  ): Unit = {
    properties match {
      case Some(properties) =>
        log.debug(s"""
           | Property connectorName : ${properties.connectorName.replaceAll("[\r\n]", "")}
           | Property topics : ${topics.replaceAll("[\r\n]", "")}
           | Property topicOut : ${properties.topicOut.replaceAll("[\r\n]", "")}
           | Property aggregationColumnName : ${properties.aggregationColumnName.replaceAll("[\r\n]", "")}
           | Property elementNumberThreshold : ${properties.elementNumberThreshold}
           | Property valuePatternThreshold : ${properties.valuePatternThreshold.replaceAll("[\r\n]", "")}
           | Property timeoutInSecondsThreshold : ${properties.timeoutInSecondsThreshold}
           | Property bootstrapServers : ${properties.bootstrapServers.replaceAll("[\r\n]", "")}
           | Property schemaRegistryUrl : ${properties.schemaRegistryUrl.replaceAll("[\r\n]", "")}
           |""".stripMargin)
      case None => log.debug("Problem with the connector's properties")
    }
  }
}
