package com.igrafx.kafka.sink.aggregationmain.domain

import com.igrafx.kafka.sink.aggregationmain.Constants
import com.igrafx.kafka.sink.aggregationmain.adapters.api.MainApiImpl
import com.igrafx.kafka.sink.aggregationmain.domain.columnmapping.controllers.dtos.ColumnMappingPropertiesDto
import com.igrafx.kafka.sink.aggregationmain.domain.columnmapping.domain.entities.{
  ColumnMappingProperties,
  ValidDimensionColumn,
  ValidMetricColumn,
  ValidTimeColumn
}
import com.igrafx.kafka.sink.aggregationmain.domain.dtos.{KafkaLoggingEventsPropertiesDto, PropertiesDto}
import com.igrafx.kafka.sink.aggregationmain.domain.entities._
import com.igrafx.kafka.sink.aggregationmain.domain.enums.ConnectorPropertiesEnum
import com.igrafx.kafka.sink.aggregationmain.domain.exceptions.InvalidPropertyValueException
import com.igrafx.kafka.sink.aggregationmain.domain.interfaces.MainApi
import com.igrafx.kafka.sink.aggregationmain.domain.usecases.{ColumnMappingUseCases, ConnectorUseCases}
import com.igrafx.utils.ConfigUtils
import org.apache.kafka.common.config._
import org.apache.kafka.connect.connector.Task
import org.apache.kafka.connect.sink.SinkConnector
import org.slf4j.{Logger, LoggerFactory}

import java.util
import scala.concurrent.duration.DurationInt
import scala.concurrent.{Await, Future}
import scala.jdk.CollectionConverters._
import scala.util.{Failure, Success, Try}

class IGrafxAggregationSinkConnector extends SinkConnector with ConfigUtils {
  private val log: Logger = LoggerFactory.getLogger(getClass)

  private[aggregationmain] val mainApi: MainApi = new MainApiImpl

  private[aggregationmain] val columnMappingUseCases: ColumnMappingUseCases = ColumnMappingUseCases.instance
  private val connectorUseCases: ConnectorUseCases = ConnectorUseCases.instance

  private var properties: Option[Properties] = None
  private var csvHeaderOpt: Option[String] = None
  private var kafkaLoggingEventsProperties: Option[KafkaLoggingEventsProperties] = None

  /** Method used to validate the connector's properties and check the value of the ColumnMapping related properties if the ConnectorPropertiesEnum.columnMappingCreateProperty property is true
    *
    * @param connectorConfigs java.util.Map[String, String] with the Connector properties, the key corresponds to the name of the property and the value corresponds to its value. The following keys need to be defined :
    * - name
    * - topics
    * - ConnectorPropertiesEnum.apiUrlProperty
    * - ConnectorPropertiesEnum.authUrlProperty
    * - ConnectorPropertiesEnum.workGroupIdProperty
    * - ConnectorPropertiesEnum.workGroupKeyProperty
    * - ConnectorPropertiesEnum.projectIdProperty, value needs to follow the UUID format
    * - ConnectorPropertiesEnum.csvEncodingProperty, value needs to match either UTF-8 or ASCII or ISO-8859-1
    * - ConnectorPropertiesEnum.csvSeparatorProperty, value.length needs to be equal to 1
    * - ConnectorPropertiesEnum.csvQuoteProperty, value.length needs to be equal to 1
    * - ConnectorPropertiesEnum.csvFieldsNumberProperty, value needs to be >= 3
    * - ConnectorPropertiesEnum.csvHeaderProperty
    * - ConnectorPropertiesEnum.csvDefaultTextValueProperty
    * - ConnectorPropertiesEnum.retentionTimeInDayProperty, value needs to be > 0
    * - ConnectorPropertiesEnum.elementNumberThresholdProperty, value needs to be >= 1
    * - ConnectorPropertiesEnum.valuePatternThresholdProperty
    * - ConnectorPropertiesEnum.timeoutInSecondsThresholdProperty, value needs to be >= 1
    * - ConnectorPropertiesEnum.bootstrapServersProperty
    * - Constants.schemaRegistryUrlProperty
    *
    * The next keys only need to be defined if the connector is supposed to create the Column Mapping of the iGrafx project :
    *
    * - ConnectorPropertiesEnum.columnMappingCreateProperty
    * - ConnectorPropertiesEnum.columnMappingCaseIdColumnIndexProperty, value needs to be between 0 and the value of ConnectorPropertiesEnum.csvFieldsNumberProperty-1
    * - ConnectorPropertiesEnum.columnMappingActivityColumnIndexProperty, value needs to be between 0 and the value of ConnectorPropertiesEnum.csvFieldsNumberProperty-1
    * - ConnectorPropertiesEnum.columnMappingTimeInformationListProperty, value needs to have between 1 and 2 elements, each element should have an index between 0 and the value of ConnectorPropertiesEnum.csvFieldsNumberProperty-1
    * - ConnectorPropertiesEnum.columnMappingDimensionsInformationListProperty, each element should have an index between 0 and the value of ConnectorPropertiesEnum.csvFieldsNumberProperty-1
    * - ConnectorPropertiesEnum.columnMappingMetricsInformationListProperty, each element should have an index between 0 and the value of ConnectorPropertiesEnum.csvFieldsNumberProperty-1
    * - ConnectorPropertiesEnum.csvEndOfLineProperty, value can't be empty
    * - ConnectorPropertiesEnum.csvEscapeProperty, value needs a length equal to 1
    * - ConnectorPropertiesEnum.csvCommentProperty, value needs a length equal to 1
    *
    * The next keys only need to be defined if the connector is supposed to log its file related events in a Kafka topic :
    *
    * - ConnectorPropertiesEnum.kafkaLoggingEventsIsLoggingProperty
    * - ConnectorPropertiesEnum.kafkaLoggingEventsTopicProperty, value can't be empty
    */
  override def validate(connectorConfigs: util.Map[String, String]): Config = {
    log.info("[IGrafxAggregationSinkConnector.validate] Starting validation")
    val config: Config = super.validate(connectorConfigs)
    val configValues = config.configValues().asScala

    val columnMappingCreateCfg: ConfigValue =
      getConfigValue(configValues, ConnectorPropertiesEnum.columnMappingCreateProperty.toStringDescription)
    val columnMappingCreate: Boolean = Try { columnMappingCreateCfg.value().toString.toBoolean } match {
      case Success(columnMappingCreate) => columnMappingCreate
      case Failure(exception: IllegalArgumentException) =>
        log.error(
          s"Error during Validation, value of ${ConnectorPropertiesEnum.columnMappingCreateProperty} property is not parsable to Boolean",
          exception
        )
        columnMappingCreateCfg.addErrorMessage(
          s"Value of ${ConnectorPropertiesEnum.columnMappingCreateProperty} property is not parsable to Boolean"
        )
        false
      case Failure(exception) =>
        log.error(
          s"Unexpected Error during Validation of ${ConnectorPropertiesEnum.columnMappingCreateProperty} property",
          exception
        )
        columnMappingCreateCfg.addErrorMessage(exception.getMessage)
        false
    }

    if (columnMappingCreate) {

      log.debug("Creation of Column Mapping")

      Try {
        columnMappingUseCases.checkColumnMappingProperties(columnMappingCreateCfg, configValues)
      } match {
        case Success(_) => ()
        case Failure(exception: InvalidPropertyValueException) =>
          log.error(exception.getMessage)
        case Failure(exception) =>
          log.error("Unexpected error during Validation", exception)
          columnMappingCreateCfg.addErrorMessage(
            "Unexpected error during Validation, please look at the logs for more information"
          )
      }
    } else {
      log.debug("No creation of Column Mapping")
    }

    val kafkaLoggingEventsSendInformationCfg: ConfigValue =
      getConfigValue(
        configValues,
        ConnectorPropertiesEnum.kafkaLoggingEventsIsLoggingProperty.toStringDescription
      )
    val kafkaLoggingEventsSendInformation: Boolean = Try {
      kafkaLoggingEventsSendInformationCfg.value().toString.toBoolean
    } match {
      case Success(kafkaLoggingEventsSendInformation) => kafkaLoggingEventsSendInformation
      case Failure(exception: IllegalArgumentException) =>
        log.error(
          s"Error during Validation, value of ${ConnectorPropertiesEnum.kafkaLoggingEventsIsLoggingProperty} property is not parsable to Boolean",
          exception
        )
        kafkaLoggingEventsSendInformationCfg.addErrorMessage(
          s"Value of ${ConnectorPropertiesEnum.kafkaLoggingEventsIsLoggingProperty} property is not parsable to Boolean"
        )
        false
      case Failure(exception) =>
        log.error(
          s"Unexpected Error during Validation of ${ConnectorPropertiesEnum.kafkaLoggingEventsIsLoggingProperty} property",
          exception
        )
        kafkaLoggingEventsSendInformationCfg.addErrorMessage(exception.getMessage)
        false
    }

    if (kafkaLoggingEventsSendInformation) {
      log.debug("Connector's events will be logged in a Kafka Topic")

      Try {
        val kafkaLoggingEventsTopicCfg: ConfigValue =
          getConfigValue(configValues, ConnectorPropertiesEnum.kafkaLoggingEventsTopicProperty.toStringDescription)
        columnMappingUseCases.checkNonEmptyStringProperty(
          kafkaLoggingEventsTopicCfg,
          ConnectorPropertiesEnum.kafkaLoggingEventsTopicProperty
        )
      } match {
        case Success(_) => ()
        case Failure(exception: InvalidPropertyValueException) =>
          log.error(exception.getMessage)
        case Failure(exception) =>
          log.error("Unexpected error during Validation", exception)
          kafkaLoggingEventsSendInformationCfg.addErrorMessage(
            "Unexpected error during Validation, please look at the logs for more information"
          )
      }
    } else {
      log.debug("Connector's events not logged in a Kafka Topic")
    }

    log.debug(s"Validation information : ${config.configValues().toString}".replaceAll("[\r\n]", ""))
    log.info("[IGrafxAggregationSinkConnector.validate] End of validation")

    config
  }

  /** Method called when we start the Connector, here it's used to retrieve the configuration
    * Warning : Method not pure as there is a dependency towards the mutable class attribute "properties"
    *
    * @param map java.util.Map[String, String] with the Connector properties, the key corresponds to the name of the property and the value corresponds to its value. The following keys need to be defined :
    * - name
    * - topics
    * - ConnectorPropertiesEnum.apiUrlProperty
    * - ConnectorPropertiesEnum.authUrlProperty
    * - ConnectorPropertiesEnum.workGroupIdProperty
    * - ConnectorPropertiesEnum.workGroupKeyProperty
    * - ConnectorPropertiesEnum.projectIdProperty, value needs to follow the UUID format
    * - ConnectorPropertiesEnum.csvEncodingProperty, value needs to match either UTF-8 or ASCII or ISO-8859-1
    * - ConnectorPropertiesEnum.csvSeparatorProperty, value.length needs to be equal to 1
    * - ConnectorPropertiesEnum.csvQuoteProperty, value.length needs to be equal to 1
    * - ConnectorPropertiesEnum.csvFieldsNumberProperty, value needs to be >= 3
    * - ConnectorPropertiesEnum.csvHeaderProperty
    * - ConnectorPropertiesEnum.csvDefaultTextValueProperty
    * - ConnectorPropertiesEnum.retentionTimeInDayProperty, value needs to be > 0
    * - ConnectorPropertiesEnum.elementNumberThresholdProperty, value needs to be >= 1
    * - ConnectorPropertiesEnum.valuePatternThresholdProperty
    * - ConnectorPropertiesEnum.timeoutInSecondsThresholdProperty, value needs to be >= 1
    * - ConnectorPropertiesEnum.bootstrapServersProperty
    * - Constants.schemaRegistryUrlProperty
    *
    * The next keys only need to be defined if the connector is supposed to create the Column Mapping of the iGrafx project :
    *
    * - ConnectorPropertiesEnum.columnMappingCreateProperty
    * - ConnectorPropertiesEnum.columnMappingCaseIdColumnIndexProperty, value needs to be between 0 and the value of ConnectorPropertiesEnum.csvFieldsNumberProperty-1
    * - ConnectorPropertiesEnum.columnMappingActivityColumnIndexProperty, value needs to be between 0 and the value of ConnectorPropertiesEnum.csvFieldsNumberProperty-1
    * - ConnectorPropertiesEnum.columnMappingTimeInformationListProperty, value needs to have between 1 and 2 elements, each element should have an index between 0 and the value of ConnectorPropertiesEnum.csvFieldsNumberProperty-1
    * - ConnectorPropertiesEnum.columnMappingDimensionsInformationListProperty, each element should have an index between 0 and the value of ConnectorPropertiesEnum.csvFieldsNumberProperty-1
    * - ConnectorPropertiesEnum.columnMappingMetricsInformationListProperty, each element should have an index between 0 and the value of ConnectorPropertiesEnum.csvFieldsNumberProperty-1
    * - ConnectorPropertiesEnum.csvEndOfLineProperty, value can't be empty
    * - ConnectorPropertiesEnum.csvEscapeProperty, value needs a length equal to 1
    * - ConnectorPropertiesEnum.csvCommentProperty, value needs a length equal to 1
    *
    * The next keys only need to be defined if the connector is supposed to log its file related events in a Kafka topic :
    *
    * - ConnectorPropertiesEnum.kafkaLoggingEventsIsLoggingProperty
    * - ConnectorPropertiesEnum.kafkaLoggingEventsTopicProperty, value can't be empty
    *
    * @throws ConfigException Issue with the connector properties
    * @throws IllegalArgumentException The projectId property defined by the user does not suit UUID or a requirement in Properties is not verified. Requirements are : csvFieldsNumber >= 3 && syncToApiPeriodInMinute > 0 && apiTryNumber > 0 && retentionTimeInDay > 0 && csvSeparator.length == 1 && csvQuote.length == 1
    * @throws concurrent.TimeoutException Timeout exceeded for startConfig
    * @throws InterruptedException If the current thread is interrupted while waiting for startConfig to finish
    */
  override def start(map: util.Map[String, String]): Unit = {
    log.info(s"[IGrafxAggregationSinkConnector] Starting Kafka Sink Connector ${this.getClass.getName}")
    val connectorConfig = new AbstractConfig(ConnectorPropertiesEnum.configDef, map)
    val immutableMap = map.asScala.toMap

    def startConfigFuture = startConfig(immutableMap, connectorConfig)

    //Don't start the connector (which will send files to the iGrafx Mining API) until the Column Mapping is created
    Try {
      Await.result(startConfigFuture, Constants.timeoutFutureValueInSeconds seconds)
    } match {
      case Success(_) => ()
      case Failure(exception: concurrent.TimeoutException) =>
        log.error("Timeout exceeded for startConfig during the starting phase of the connector", exception)
        throw exception
      case Failure(exception: InterruptedException) =>
        log.error(
          "Interrupted while waiting for startConfig to finish during the starting phase of the connector",
          exception
        )
        throw exception
      case Failure(exception) =>
        log.error(
          "Unexpected issue while waiting for startConfig to finish during the starting phase of the connector",
          exception
        )
        throw exception
    }
  }

  /** Method called to retrieve and check the properties of the connector
    * Warning : the "properties" class attribute is modified in this method
    *
    * @param map scala.collection.immutable.Map[String, String] with the Connector properties, the key corresponds to the name of the property and the value corresponds to its value. The following keys need to be defined :
    * - name
    * - topics
    * - ConnectorPropertiesEnum.apiUrlProperty
    * - ConnectorPropertiesEnum.authUrlProperty
    * - ConnectorPropertiesEnum.workGroupIdProperty
    * - ConnectorPropertiesEnum.workGroupKeyProperty
    * - ConnectorPropertiesEnum.projectIdProperty, value needs to follow the UUID format
    * - ConnectorPropertiesEnum.csvEncodingProperty, value needs to match either UTF-8 or ASCII or ISO-8859-1
    * - ConnectorPropertiesEnum.csvSeparatorProperty, value.length needs to be equal to 1
    * - ConnectorPropertiesEnum.csvQuoteProperty, value.length needs to be equal to 1
    * - ConnectorPropertiesEnum.csvFieldsNumberProperty, value needs to be >= 3
    * - ConnectorPropertiesEnum.csvHeaderProperty
    * - ConnectorPropertiesEnum.csvDefaultTextValueProperty
    * - ConnectorPropertiesEnum.retentionTimeInDayProperty, value needs to be > 0
    * - ConnectorPropertiesEnum.elementNumberThresholdProperty, value needs to be >= 1
    * - ConnectorPropertiesEnum.valuePatternThresholdProperty
    * - ConnectorPropertiesEnum.timeoutInSecondsThresholdProperty, value needs to be >= 1
    * - ConnectorPropertiesEnum.bootstrapServersProperty
    * - Constants.schemaRegistryUrlProperty
    *
    * The next keys only need to be defined if the connector is supposed to create the Column Mapping of the iGrafx project :
    *
    * - ConnectorPropertiesEnum.columnMappingCreateProperty
    * - ConnectorPropertiesEnum.columnMappingCaseIdColumnIndexProperty, value needs to be between 0 and the value of ConnectorPropertiesEnum.csvFieldsNumberProperty-1
    * - ConnectorPropertiesEnum.columnMappingActivityColumnIndexProperty, value needs to be between 0 and the value of ConnectorPropertiesEnum.csvFieldsNumberProperty-1
    * - ConnectorPropertiesEnum.columnMappingTimeInformationListProperty, value needs to have between 1 and 2 elements, each element should have an index between 0 and the value of ConnectorPropertiesEnum.csvFieldsNumberProperty-1
    * - ConnectorPropertiesEnum.columnMappingDimensionsInformationListProperty, each element should have an index between 0 and the value of ConnectorPropertiesEnum.csvFieldsNumberProperty-1
    * - ConnectorPropertiesEnum.columnMappingMetricsInformationListProperty, each element should have an index between 0 and the value of ConnectorPropertiesEnum.csvFieldsNumberProperty-1
    * - ConnectorPropertiesEnum.csvEndOfLineProperty, value can't be empty
    * - ConnectorPropertiesEnum.csvEscapeProperty, value needs a length equal to 1
    * - ConnectorPropertiesEnum.csvCommentProperty, value needs a length equal to 1
    *
    * The next keys only need to be defined if the connector is supposed to log its file related events in a Kafka topic :
    *
    * - ConnectorPropertiesEnum.kafkaLoggingEventsIsLoggingProperty
    * - ConnectorPropertiesEnum.kafkaLoggingEventsTopicProperty, value can't be empty
    *
    * @throws IllegalArgumentException The projectId property defined by the user does not suit UUID or a requirement in Properties is not verified. Requirements are : csvFieldsNumber >= 3 && syncToApiPeriodInMinute > 0 && apiTryNumber > 0 && retentionTimeInDay > 0 && csvSeparator.length == 1 && csvQuote.length == 1
    * @throws ConfigException At least one of the mandatory properties is missing
    */
  private[aggregationmain] def startConfig(map: Map[String, String], connectorConfig: AbstractConfig): Future[Unit] = {
    val connectorName: String = map.get("name") match {
      case Some(name) => name
      case None =>
        log.error(
          "[IGrafxAggregationSinkConnector.startConfig] ConfigException : The property 'name' needs to be defined"
        )
        throw new ConfigException(
          "ConfigException : The property 'name' needs to be defined"
        )
    }
    val topics: String = map.get("topics") match {
      case Some(topics) => topics
      case None =>
        log.error(
          "[IGrafxAggregationSinkConnector.startConfig] ConfigException : The property 'topics' needs to be defined"
        )
        throw new ConfigException(
          "ConfigException : The property 'topics' needs to be defined"
        )
    }
    val schemaRegistryUrl: String = map.get(Constants.schemaRegistryUrlProperty) match {
      case Some(schemaRegistryUrl) => schemaRegistryUrl
      case None =>
        log.error(
          s"[AggregationSinkConnector.startConfig] ConfigException : The property " +
            s"'${Constants.schemaRegistryUrlProperty}' needs to be defined"
              .replaceAll("[\r\n]", "")
        )
        throw new ConfigException(
          s"ConfigException : The property '${Constants.schemaRegistryUrlProperty}' needs to be defined"
            .replaceAll("[\r\n]", "")
        )
    }

    setPropertiesAndCreateColumnMapping(connectorConfig, connectorName, schemaRegistryUrl, topics)
  }

  private def setPropertiesAndCreateColumnMapping(
      connectorConfig: AbstractConfig,
      connectorName: String,
      schemaRegistryUrl: String,
      topics: String
  ): Future[Unit] = {
    Try[Option[ColumnMappingProperties]] {
      val propertiesFromConnector =
        PropertiesDto.fromConnectorConfig(connectorConfig, connectorName, schemaRegistryUrl, log).toProperties
      properties = Some(propertiesFromConnector)

      debugPrints(properties, topics)

      if (propertiesFromConnector.isLogging) {
        kafkaLoggingEventsProperties = Some(
          KafkaLoggingEventsPropertiesDto
            .fromConnectorConfig(connectorConfig, schemaRegistryUrl)
            .toKafkaLoggingEventsProperties
        )

        debugKafkaLoggingEventsPrints(
          kafkaLoggingEventsProperties
        )
      }

      getColumnMappingProperties(connectorConfig, propertiesFromConnector)
    } match {
      case Success(columnMappingPropertiesOption) =>
        columnMappingPropertiesOption match {
          case Some(columnMappingProperties) =>
            columnMappingUseCases.createColumnMapping(columnMappingProperties, mainApi)
          case None => Future.successful(())
        }
      case Failure(exception) =>
        log.error(
          "Issue with at least one of the connector's properties",
          exception
        )
        throw exception
    }
  }

  private def getColumnMappingProperties(
      connectorConfig: AbstractConfig,
      propertiesFromConnector: Properties
  ): Option[ColumnMappingProperties] = {
    val columnMappingCreate: Boolean =
      connectorConfig.getBoolean(ConnectorPropertiesEnum.columnMappingCreateProperty.toStringDescription)
    if (columnMappingCreate) {
      val mandatoryProperties = properties.getOrElse(
        throw new ConfigException("Mandatory properties did not initialize properly")
      )
      val columnMappingProperties =
        ColumnMappingPropertiesDto.fromConnectorConfig(connectorConfig, mandatoryProperties)(log).toEntity(log)

      debugColumnMappingPrint(columnMappingProperties)

      if (propertiesFromConnector.csvHeader) {
        csvHeaderOpt = Some(
          columnMappingUseCases.generateCsvHeaderFromColumnMapping(
            columnMappingProperties,
            propertiesFromConnector.csvFieldsNumber,
            propertiesFromConnector.csvSeparator
          )
        )
      }

      Some(columnMappingProperties)
    } else {
      if (propertiesFromConnector.csvHeader) {
        csvHeaderOpt = Some(
          columnMappingUseCases.generateCsvHeaderWithoutColumnMapping(
            propertiesFromConnector.csvFieldsNumber,
            propertiesFromConnector.csvSeparator
          )
        )
      }

      Option.empty[ColumnMappingProperties]
    }
  }

  override def taskClass(): Class[_ <: Task] = {
    classOf[IGrafxAggregationSinkTask]
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
    * @throws ConfigException Issue with the connector header or properties
    */
  override def taskConfigs(maxTasks: Int): util.List[util.Map[String, String]] = {
    val propertiesValue = properties match {
      case Some(properties) => properties
      case None =>
        log.error("[IGrafxAggregationSinkConnector.taskConfigs] Connector properties are not valid")
        throw new ConfigException("Connector properties are not valid")
    }
    val propertiesConfig: Seq[Map[String, String]] = csvHeaderOpt match {
      case Some(csvHeaderValue) =>
        connectorUseCases
          .createConfigsRec(maxTasks, Seq.empty[Properties], propertiesValue)
          .map(PropertiesDto.fromProperties(_).toMap + (Constants.headerValueProperty -> csvHeaderValue))
      case None =>
        connectorUseCases
          .createConfigsRec(maxTasks, Seq.empty[Properties], propertiesValue)
          .map(PropertiesDto.fromProperties(_).toMap)
    }

    val taskConfig: util.List[util.Map[String, String]] =
      (if (propertiesValue.isLogging) {
         val kafkaLoggingEventsPropertiesValue = kafkaLoggingEventsProperties match {
           case Some(kafkaLoggingEventsPropertiesValue) => kafkaLoggingEventsPropertiesValue
           case None =>
             log.error(
               "[IGrafxAggregationSinkConnector.taskConfigs] Connector Kafka Logging Events properties are not valid"
             )
             throw new ConfigException("Connector Kafka Logging Events properties are not valid")
         }
         val kafkaLoggingEventsPropertiesConfig = connectorUseCases
           .createKafkaLoggingEventsConfigsRec(
             maxTasks,
             Seq.empty[KafkaLoggingEventsProperties],
             kafkaLoggingEventsPropertiesValue
           )
           .map(KafkaLoggingEventsPropertiesDto.fromKafkaLoggingEventsProperties(_).toMap)
         propertiesConfig
           .zip(kafkaLoggingEventsPropertiesConfig)
           .map {
             case (propertiesMap: Map[String, String], kafkaLoggingEventsPropertiesMap: Map[String, String]) =>
               (propertiesMap ++ kafkaLoggingEventsPropertiesMap).asJava
           }
       } else {
         propertiesConfig.map(_.asJava)
       }).asJava

    taskConfig
  }

  /** Method called when we stop the Connector */
  override def stop(): Unit = {
    log.info(s"[IGrafxAggregationSinkConnector] Stopping Kafka Sink Connector ${this.getClass.getName}")
  }

  override def config(): ConfigDef = {
    ConnectorPropertiesEnum.configDef
  }

  override def version(): String = {
    "2.35.0"
  }

  def debugPrints(
      properties: Option[Properties],
      topics: String
  ): Unit = {
    properties match {
      case Some(properties) =>
        log.debug(s"""
            | Property connectorName : ${properties.connectorName.replaceAll("[\r\n]", "")}
            | Property apiUrl : ${properties.apiUrl.replaceAll("[\r\n]", "")}
            | Property authUrl : ${properties.authUrl.replaceAll("[\r\n]", "")}
            | Property workGroupId : ${properties.workGroupId.replaceAll("[\r\n]", "")}
            | Property workGroupKey : ${properties.workGroupKey.replaceAll("[\r\n]", "")}
            | Property projectId : ${properties.projectId}
            | Property topics : ${topics.replaceAll("[\r\n]", "")}
            | Property csvEncoding : ${properties.csvEncoding}
            | Property csvSeparator : ${properties.csvSeparator.replaceAll("[\r\n]", "")}
            | Property csvQuote : ${properties.csvQuote.replaceAll("[\r\n]", "")}
            | Property csvFieldsNumber : ${properties.csvFieldsNumber}
            | Property csvHeader : ${properties.csvHeader}
            | Property csvDefaultTextValue : ${properties.csvDefaultTextValue.replaceAll("[\r\n]", "")}
            | Property retentionTimeInDay : ${properties.retentionTimeInDay}
            | Property isLogging : ${properties.isLogging}
            | Property elementNumberThreshold : ${properties.elementNumberThreshold}
            | Property valuePatternThreshold : ${properties.valuePatternThreshold.replaceAll("[\r\n]", "")}
            | Property timeoutInSecondsThreshold : ${properties.timeoutInSecondsThreshold}
            | Property bootstrapServers : ${properties.bootstrapServers.replaceAll("[\r\n]", "")}
            | Property schemaRegistryUrl : ${properties.schemaRegistryUrl.replaceAll("[\r\n]", "")}
            |""".stripMargin)
      case None => log.debug("Problem with the connector's properties")
    }
  }

  def debugColumnMappingPrint(columnMappingProperties: ColumnMappingProperties): Unit = {
    log.debug(s"Property caseId index : ${columnMappingProperties.columnMapping.caseId}")
    log.debug(s"Property activity index : ${columnMappingProperties.columnMapping.activity}")
    columnMappingProperties.columnMapping.time.foreach { timeColumn: ValidTimeColumn =>
      log.debug(
        s"Property time : index = ${timeColumn.columnIndex}, date format = ${timeColumn.format}"
          .replaceAll("[\r\n]", "")
      )
    }
    columnMappingProperties.columnMapping.dimension.foreach { dimensionColumn: ValidDimensionColumn =>
      log.debug(
        s"Property dimension : index = ${dimensionColumn.columnIndex}, column name = ${dimensionColumn.name.stringValue}"
          .replaceAll("[\r\n]", "")
      )
    }

    columnMappingProperties.columnMapping.metric.foreach { metricColumn: ValidMetricColumn =>
      log.debug(
        s"Property dimension : index = ${metricColumn.columnIndex}, column name = ${metricColumn.name.stringValue}, unit = ${metricColumn.unit
          .getOrElse("empty")}".replaceAll("[\r\n]", "")
      )
    }
    log.debug(
      s"Property csvEndOfLine : ${columnMappingProperties.fileStructure.csvEndOfLine.stringValue}"
        .replaceAll("[\r\n]", "")
    )
    log.debug(s"Property csvEscape : ${columnMappingProperties.fileStructure.csvEscape}".replaceAll("[\r\n]", ""))
    log.debug(s"Property csvComment : ${columnMappingProperties.fileStructure.csvComment}".replaceAll("[\r\n]", ""))
  }

  def debugKafkaLoggingEventsPrints(kafkaLoggingEventsProperties: Option[KafkaLoggingEventsProperties]): Unit = {
    kafkaLoggingEventsProperties match {
      case Some(kafkaLoggingEventsProperties) =>
        log.debug(s"""
          | [Kafka Logging Events] Property topic : ${kafkaLoggingEventsProperties.topic.replaceAll("[\r\n]", "")}
          | [Kafka Logging Events] Property bootstrapServers : ${kafkaLoggingEventsProperties.bootstrapServers
          .replaceAll("[\r\n]", "")}
          | [Kafka Logging Events] Property schemaRegistryUrl : ${kafkaLoggingEventsProperties.schemaRegistryUrl
          .replaceAll("[\r\n]", "")}
          |""".stripMargin)
      case None => log.debug("Problem with the connector's Kafka Logging Events properties")
    }
  }
}
