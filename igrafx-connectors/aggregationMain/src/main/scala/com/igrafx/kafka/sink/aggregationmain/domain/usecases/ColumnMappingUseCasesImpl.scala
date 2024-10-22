package com.igrafx.kafka.sink.aggregationmain.domain.usecases

import com.igrafx.kafka.sink.aggregationmain.Constants
import com.igrafx.kafka.sink.aggregationmain.domain.columnmapping.controllers.dtos._
import com.igrafx.kafka.sink.aggregationmain.domain.columnmapping.domain.entities._
import com.igrafx.kafka.sink.aggregationmain.domain.entities._
import com.igrafx.kafka.sink.aggregationmain.domain.enums.ConnectorPropertiesEnum
import com.igrafx.kafka.sink.aggregationmain.domain.exceptions._
import com.igrafx.kafka.sink.aggregationmain.domain.interfaces.MainApi
import com.igrafx.utils.ConfigUtils
import org.apache.kafka.common.config.ConfigValue
import org.json4s.DefaultFormats
import org.json4s.ParserUtil.ParseException
import org.slf4j.{Logger, LoggerFactory}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.util.{Failure, Success, Try}

protected[usecases] class ColumnMappingUseCasesImpl extends ColumnMappingUseCases with ConfigUtils {
  private implicit val log: Logger = LoggerFactory.getLogger(classOf[ColumnMappingUseCasesImpl])
  private implicit val formats: DefaultFormats.type = org.json4s.DefaultFormats

  @throws[InvalidPropertyValueException]
  override def checkColumnMappingProperties(
      columnMappingCreateCfg: ConfigValue,
      configValues: Iterable[ConfigValue]
  ): Unit = {
    val columnsNumber = getColumnsNumber(configValues)

    checkColumnsProperties(columnMappingCreateCfg, configValues, columnsNumber)

    val columnMappingEndOfLineCfg: ConfigValue =
      getConfigValue(configValues, ConnectorPropertiesEnum.csvEndOfLineProperty.toStringDescription)
    checkNonEmptyStringProperty(columnMappingEndOfLineCfg, ConnectorPropertiesEnum.csvEndOfLineProperty)

    val columnMappingEscapeCfg: ConfigValue =
      getConfigValue(configValues, ConnectorPropertiesEnum.csvEscapeProperty.toStringDescription)
    checkColumnMappingCharacterProperty(columnMappingEscapeCfg, ConnectorPropertiesEnum.csvEscapeProperty)

    val columnMappingCommentCfg: ConfigValue =
      getConfigValue(configValues, ConnectorPropertiesEnum.csvCommentProperty.toStringDescription)
    checkColumnMappingCharacterProperty(columnMappingCommentCfg, ConnectorPropertiesEnum.csvCommentProperty)
  }

  // ----------------- COLUMN MAPPING CREATION METHODS -----------------

  /** Method used to create the Column Mapping and handle potential errors
    *
    * @param columnMappingProperties The Column Mapping properties
    * @param mainApi The instance to the mainApi
    */
  @throws[InvalidTokenException](cause = "Couldn't create the Column Mapping because of an Authentication Issue")
  @throws[ColumnMappingCreationException](cause = "Issue during the Column Mapping Creation via the iGrafx Mining API")
  override def createColumnMapping(
      columnMappingProperties: ColumnMappingProperties,
      mainApi: MainApi
  ): Future[Unit] = {
    mainApi.createColumnMapping(columnMappingProperties).recover {
      case exception: InvalidTokenException =>
        if (exception.canRetry) {
          log.error(
            "Couldn't create Column Mapping because of a Server issue during Authentication, connector is going to stop"
          )
        } else {
          log.error(
            "Couldn't create Column Mapping because of an issue with the Authentication, connector is going to stop"
          )
        }
        throw exception
      case exception: ColumnMappingCreationException =>
        if (exception.canRetry) {
          log.error("Couldn't create Column Mapping because of a Server issue, connector is going to stop")
        } else {
          log.error("Problem with the Column Mapping creation, connector is going to stop")
        }
        throw exception
      case _: ColumnMappingAlreadyExistsException =>
        log.warn(
          "Column mapping already exists in targeted project " +
            s"'${columnMappingProperties.configurationProperties.projectId}'".replaceAll("[\r\n]", "") +
            ", please make sure that the column mapping defined in the connector is coherent with the existing " +
            "column mapping in the Mining project"
        )
      case exception =>
        log.error("Problem with the Column Mapping creation, connector is going to stop", exception)
        throw exception
    }
  }

  @throws[IllegalArgumentException](
    cause =
      "if there is no column defined in the Column Mapping or if the number of Columns Defined doesn't correspond " +
        "to csvFieldsNumber"
  )
  override def generateCsvHeaderFromColumnMapping(
      columnMappingProperties: ColumnMappingProperties,
      csvFieldsNumber: ColumnsNumber,
      csvSeparator: String
  ): String = {
    val csvHeaderColumnSeq: Seq[String] = columnMappingProperties.toColumnNamesSeq
    if (csvHeaderColumnSeq.isEmpty || (csvHeaderColumnSeq.size != csvFieldsNumber.number)) {
      log.error(
        s"[IGrafxAggregationSinkConnector.generateCsvHeaderFromColumnMapping] IllegalArgumentException : " +
          s"${csvHeaderColumnSeq.size} columns defined in the Column Mapping and csvFieldsNumber equals " +
          s"$csvFieldsNumber. Both should have the same value (and be superior than zero)"
      )
      throw new IllegalArgumentException(
        s"${csvHeaderColumnSeq.size} columns defined in the Column Mapping and csvFieldsNumber equals " +
          s"$csvFieldsNumber. Both should have the same value (and be superior than zero)"
      )
    }
    csvHeaderColumnSeq.mkString(csvSeparator)
  }

  @throws[IllegalArgumentException](cause = "if csvFieldsNumber <= 0")
  override def generateCsvHeaderWithoutColumnMapping(csvFieldsNumber: ColumnsNumber, csvSeparator: String): String = {
    if (csvFieldsNumber.number <= 0) {
      log.error(
        s"[IGrafxAggregationSinkConnector.generateCsvHeaderWithoutColumnMapping] IllegalArgumentException : " +
          s"Issue with the number of separators to add for the header : ${csvFieldsNumber.number - 1} , and can't " +
          s"be inferior than 0"
      )
      throw new IllegalArgumentException(
        s"Issue with the number of separators to add for the header : ${csvFieldsNumber.number - 1} , and can't be " +
          s"inferior than 0"
      )
    }
    csvSeparator * (csvFieldsNumber.number - 1)
  }

  // ----------------- UTILITY METHODS -----------------

  private[usecases] def checkErrors[T](
      tryResult: Try[Option[T]],
      propertyConfig: ConfigValue
  ): Option[T] = {
    val propertyName = propertyConfig.name()
    tryResult match {
      case Success(value: Option[T]) => value
      case Failure(exception: InvalidPropertyValueException) =>
        log.error(s"Error during Validation of properties : ", exception)
        propertyConfig.addErrorMessage(s"Error during Validation of properties : ${exception.getMessage}")
        None
      case Failure(exception: ParseException) =>
        log.error(
          s"Error during Validation of property $propertyName : Impossible to parse the JSON : ",
          exception
        )
        propertyConfig.addErrorMessage(
          s"Error during Validation of property $propertyName : Impossible to parse the JSON : ${exception.getMessage}"
        )
        None
      case Failure(exception) =>
        log.error(s"Unexpected error during Validation of property $propertyName : ", exception)
        propertyConfig.addErrorMessage(
          s"Unexpected error during Validation of property $propertyName : ${exception.getMessage}"
        )
        None
    }
  }

  // ----------------- COLUMN MAPPING VERIFICATION METHODS -----------------

  // --------- CASE ID PROPERTY ---------

  private[usecases] def checkColumnMappingCaseIdProperty(
      columnMappingCaseIdCfg: ConfigValue,
      columnsNumber: ColumnsNumber
  ): Option[ValidCaseIdColumn] = {
    if (columnMappingCaseIdCfg.errorMessages().size() == 0) {
      val tryResult = Try {
        Some(getValidCaseIdColumnFromProperty(columnMappingCaseIdCfg, columnsNumber))
      }

      checkErrors(
        tryResult,
        columnMappingCaseIdCfg
      )
    } else {
      None
    }
  }

  @throws[InvalidPropertyValueException]
  private[usecases] def getValidCaseIdColumnFromProperty(
      columnMappingCaseIdCfg: ConfigValue,
      columnsNumber: ColumnsNumber
  ): ValidCaseIdColumn = {
    val caseIdColumnDto = CaseIdColumnDto.fromCaseIdConfig(columnMappingCaseIdCfg)
    caseIdColumnDto.toEntity(columnsNumber)
  }

  // --------- ACTIVITY PROPERTY ---------

  private[usecases] def checkColumnMappingActivityProperty(
      columnMappingActivityCfg: ConfigValue,
      columnsNumber: ColumnsNumber
  ): Option[ValidActivityColumn] = {
    if (columnMappingActivityCfg.errorMessages().size() == 0) {
      val tryResult = Try {
        Some(getValidActivityColumnFromProperty(columnMappingActivityCfg, columnsNumber))
      }

      checkErrors(
        tryResult,
        columnMappingActivityCfg
      )
    } else {
      None
    }
  }

  @throws[InvalidPropertyValueException]
  private[usecases] def getValidActivityColumnFromProperty(
      columnMappingActivityCfg: ConfigValue,
      columnsNumber: ColumnsNumber
  ): ValidActivityColumn = {
    val activityColumnDto = ActivityColumnDto.fromActivityConfig(columnMappingActivityCfg)
    activityColumnDto.toEntity(columnsNumber)
  }

  // --------- TIME PROPERTY ---------

  private[usecases] def checkColumnMappingTimeProperty(
      columnMappingTimeCfg: ConfigValue,
      columnsNumber: ColumnsNumber
  ): Option[Set[ValidTimeColumn]] = {
    if (columnMappingTimeCfg.errorMessages().size() == 0) {
      val tryResultForTime = Try {
        Some(getValidTimeColumnsFromProperty(columnMappingTimeCfg, columnsNumber))
      }

      checkErrors(
        tryResultForTime,
        columnMappingTimeCfg
      )
    } else {
      None
    }
  }

  @throws[InvalidPropertyValueException]
  private[usecases] def getValidTimeColumnsFromProperty(
      columnMappingTimeCfg: ConfigValue,
      columnsNumber: ColumnsNumber
  ): Set[ValidTimeColumn] = {
    val timeColumnsDto = TimeColumnsDto.fromTimeConfig(columnMappingTimeCfg)
    timeColumnsDto.map(timeColumnDto => timeColumnDto.toEntity(columnsNumber))
  }

  // --------- DIMENSIONS PROPERTY ---------

  private[usecases] def checkColumnMappingDimensionProperty(
      columnMappingDimensionCfg: ConfigValue,
      columnsNumber: ColumnsNumber
  ): Option[Set[ValidDimensionColumn]] = {
    if (columnMappingDimensionCfg.errorMessages().size() == 0) {
      val tryResult = Try {
        Some(getValidDimensionColumnsFromProperty(columnMappingDimensionCfg, columnsNumber))
      }

      checkErrors(
        tryResult,
        columnMappingDimensionCfg
      )
    } else {
      None
    }
  }

  @throws[ParseException]
  @throws[InvalidPropertyValueException]
  private[usecases] def getValidDimensionColumnsFromProperty(
      columnMappingDimensionCfg: ConfigValue,
      columnsNumber: ColumnsNumber
  ): Set[ValidDimensionColumn] = {
    val dimensionColumnsDto = DimensionColumnsDto.fromDimensionConfig(columnMappingDimensionCfg)
    dimensionColumnsDto.map(dimensionColumnDto => dimensionColumnDto.toEntity(columnsNumber))
  }

  // --------- METRICS PROPERTY ---------

  private[usecases] def checkColumnMappingMetricProperty(
      columnMappingMetricCfg: ConfigValue,
      columnsNumber: ColumnsNumber
  ): Option[Set[ValidMetricColumn]] = {
    if (columnMappingMetricCfg.errorMessages().size() == 0) {
      val tryResult = Try {
        Some(getValidMetricColumnsFromProperty(columnMappingMetricCfg, columnsNumber))
      }

      checkErrors(
        tryResult,
        columnMappingMetricCfg
      )
    } else {
      None
    }
  }

  @throws[ParseException]
  @throws[InvalidPropertyValueException]
  private[usecases] def getValidMetricColumnsFromProperty(
      columnMappingMetricCfg: ConfigValue,
      columnsNumber: ColumnsNumber
  ): Set[ValidMetricColumn] = {
    val metricColumnsDto = MetricColumnsDto.fromMetricConfig(columnMappingMetricCfg)
    metricColumnsDto.map(metricColumnDto => metricColumnDto.toEntity(columnsNumber))
  }

  // --------- GROUPED TASKS PROPERTY ---------

  private[usecases] def checkColumnMappingGroupedTasksColumnsProperty(
      columnMappingGroupedTasksCfg: ConfigValue,
      columnsNumber: ColumnsNumber
  ): Option[Option[ValidGroupedTasksColumns]] = {
    if (columnMappingGroupedTasksCfg.errorMessages().size() == 0) {
      val tryResult = Try {
        Some(getValidGroupedTasksColumnsFromProperty(columnMappingGroupedTasksCfg, columnsNumber))
      }

      checkErrors(
        tryResult,
        columnMappingGroupedTasksCfg
      )
    } else {
      None
    }
  }

  @throws[ParseException]
  @throws[InvalidPropertyValueException]
  private[usecases] def getValidGroupedTasksColumnsFromProperty(
      columnMappingGroupedTasksCfg: ConfigValue,
      columnsNumber: ColumnsNumber
  ): Option[ValidGroupedTasksColumns] = {
    val groupedTasksColumnsDtoOpt = GroupedTasksColumnsDto.fromGroupedTasksColumnsConfig(columnMappingGroupedTasksCfg)
    groupedTasksColumnsDtoOpt.map(groupedTasksColumnsDto => groupedTasksColumnsDto.toEntity(columnsNumber))
  }

  // --------- CHARACTERS PROPERTY ---------

  /** Method used to check properties needing a value with a length equal to 1
    *
    * @param propertyConfig The ConfigValue related to the property
    * @param propertyName The name of the property
    */
  private[usecases] def checkColumnMappingCharacterProperty(
      propertyConfig: ConfigValue,
      propertyName: ConnectorPropertiesEnum
  ): Unit = {
    if (propertyConfig.errorMessages().size() == 0) {
      val tryResult = Try {
        val propertyValue: String = propertyConfig.value().toString
        checkColumnMappingCharacter(propertyValue, propertyName.toStringDescription)
        None
      }
      checkErrors(
        tryResult,
        propertyConfig
      )
    }

    ()
  }

  @throws[InvalidPropertyValueException](cause = "Either the property's value is not defined or has a length != 1")
  private[usecases] def checkColumnMappingCharacter(
      propertyValue: String,
      propertyName: String
  ): Unit = {
    CharacterOrError(propertyValue) match {
      case _: Character => ()
      case _: DefaultCharacterException =>
        throw InvalidPropertyValueException(
          s"The $propertyName property is not defined !"
        )
      case error: CharacterError =>
        throw InvalidPropertyValueException(
          s"Issue with the $propertyName property. ${error.getMessage}"
        )
    }
  }

  // --------- NON EMPTY STRING PROPERTY ---------

  /** Method used to check properties needing a value which is not empty
    *
    * @param propertyConfig The ConfigValue related to the property
    * @param propertyName The name of the property
    */
  override def checkNonEmptyStringProperty(
      propertyConfig: ConfigValue,
      propertyName: ConnectorPropertiesEnum
  ): Unit = {
    if (propertyConfig.errorMessages().size() == 0) {
      val tryResult = Try {
        val propertyValue: String = propertyConfig.value().toString
        checkNonEmptyString(propertyValue, propertyName.toStringDescription)
        None
      }
      checkErrors(
        tryResult,
        propertyConfig
      )
    }
  }

  @throws[InvalidPropertyValueException](cause = "Problem with the property's value")
  private[usecases] def checkNonEmptyString(
      propertyValue: String,
      propertyName: String
  ): Unit = {
    Try(NonEmptyString(propertyValue, isOnlyStringProperty = true)) match {
      case Success(_) => ()
      case Failure(exception: NonEmptyStringException) =>
        throw InvalidPropertyValueException(s"Issue with the $propertyName property. ${exception.getMessage}")
      case Failure(exception) =>
        throw InvalidPropertyValueException(
          s"Unexpected issue with the $propertyName property. ${exception.getMessage}"
        )
    }
  }

  private[usecases] def getColumnsNumber(configValues: Iterable[ConfigValue]): ColumnsNumber = {
    val columnsNumberConfig: ConfigValue =
      getConfigValue(configValues, ConnectorPropertiesEnum.csvFieldsNumberProperty.toStringDescription)

    if (columnsNumberConfig.errorMessages().size() == 0) {
      Try {
        ColumnsNumber(columnsNumberConfig.value().toString.toInt)
      } match {
        case Success(columnsNumber: ColumnsNumber) => columnsNumber
        case Failure(_) =>
          throw InvalidPropertyValueException(
            s"Issue with the ${ConnectorPropertiesEnum.csvFieldsNumberProperty.toStringDescription} property. " +
              s"The value is not greater or equals than ${Constants.minimumColumnsNumber}"
          )
      }
    } else {
      throw InvalidPropertyValueException(
        s"Connector already has an issue with the ${ConnectorPropertiesEnum.csvFieldsNumberProperty} property used " +
          s"to check Column Mapping properties"
      )
    }
  }

  private[usecases] def checkColumnsProperties(
      columnMappingCreateCfg: ConfigValue,
      configValues: Iterable[ConfigValue],
      columnsNumber: ColumnsNumber
  ): Unit = {
    val columnMappingCaseIdCfg: ConfigValue =
      getConfigValue(configValues, ConnectorPropertiesEnum.columnMappingCaseIdColumnProperty.toStringDescription)
    val columnMappingActivityCfg: ConfigValue =
      getConfigValue(configValues, ConnectorPropertiesEnum.columnMappingActivityColumnProperty.toStringDescription)
    val columnMappingTimeCfg: ConfigValue =
      getConfigValue(configValues, ConnectorPropertiesEnum.columnMappingTimeColumnsProperty.toStringDescription)
    val columnMappingDimensionsCfg: ConfigValue =
      getConfigValue(configValues, ConnectorPropertiesEnum.columnMappingDimensionColumnsProperty.toStringDescription)
    val columnMappingMetricsCfg: ConfigValue =
      getConfigValue(configValues, ConnectorPropertiesEnum.columnMappingMetricColumnsProperty.toStringDescription)
    val columnMappingGroupedTasksCfg: ConfigValue =
      getConfigValue(configValues, ConnectorPropertiesEnum.columnMappingGroupedTasksColumnsProperty.toStringDescription)

    (
      checkColumnMappingCaseIdProperty(columnMappingCaseIdCfg, columnsNumber),
      checkColumnMappingActivityProperty(columnMappingActivityCfg, columnsNumber),
      checkColumnMappingTimeProperty(columnMappingTimeCfg, columnsNumber),
      checkColumnMappingDimensionProperty(columnMappingDimensionsCfg, columnsNumber),
      checkColumnMappingMetricProperty(columnMappingMetricsCfg, columnsNumber),
      checkColumnMappingGroupedTasksColumnsProperty(columnMappingGroupedTasksCfg, columnsNumber)
    ) match {
      case (
            Some(caseIdColumn: ValidCaseIdColumn),
            Some(activityColumn: ValidActivityColumn),
            Some(timeColumns: Set[ValidTimeColumn]),
            Some(dimensionColumns: Set[ValidDimensionColumn]),
            Some(metricColumns: Set[ValidMetricColumn]),
            Some(groupedTasksColumns: Option[ValidGroupedTasksColumns])
          ) =>
        checkCompleteColumnMapping(
          columnMappingCreateCfg = columnMappingCreateCfg,
          caseIdColumn = caseIdColumn,
          activityColumn = activityColumn,
          timeColumns = timeColumns,
          dimensionColumns = dimensionColumns,
          metricColumns = metricColumns,
          groupedTasksColumns = groupedTasksColumns,
          columnsNumber = columnsNumber
        )
      case _ => ()
    }
  }

  private[usecases] def checkCompleteColumnMapping(
      columnMappingCreateCfg: ConfigValue,
      caseIdColumn: ValidCaseIdColumn,
      activityColumn: ValidActivityColumn,
      timeColumns: Set[ValidTimeColumn],
      dimensionColumns: Set[ValidDimensionColumn],
      metricColumns: Set[ValidMetricColumn],
      groupedTasksColumns: Option[ValidGroupedTasksColumns],
      columnsNumber: ColumnsNumber
  ): Unit = {
    if (columnMappingCreateCfg.errorMessages().size() == 0) {
      Try {
        getValidColumnMapping(
          caseIdColumn = caseIdColumn,
          activityColumn = activityColumn,
          timeColumns = timeColumns,
          dimensionColumns = dimensionColumns,
          metricColumns = metricColumns,
          groupedTasksColumns = groupedTasksColumns,
          columnsNumber = columnsNumber
        )
      } match {
        case Success(_) => ()
        case Failure(exception: InvalidColumnMappingException) =>
          log.error(
            s"Error during Validation of complete Column Mapping : ",
            exception
          )
          columnMappingCreateCfg.addErrorMessage(
            s"Error during Validation of complete Column Mapping : ${exception.getMessage}"
          )
        case Failure(exception) =>
          log.error(s"Unexpected error during Validation of complete column mapping : ", exception)
          columnMappingCreateCfg.addErrorMessage(
            s"Unexpected error during Validation of complete Column Mapping : ${exception.getMessage}"
          )
      }
    }
  }

  @throws[InvalidColumnMappingException]
  private[usecases] def getValidColumnMapping(
      caseIdColumn: ValidCaseIdColumn,
      activityColumn: ValidActivityColumn,
      timeColumns: Set[ValidTimeColumn],
      dimensionColumns: Set[ValidDimensionColumn],
      metricColumns: Set[ValidMetricColumn],
      groupedTasksColumns: Option[ValidGroupedTasksColumns],
      columnsNumber: ColumnsNumber
  ): ValidColumnMapping = {
    ValidColumnMapping(
      caseId = caseIdColumn,
      activity = activityColumn,
      time = timeColumns,
      dimension = dimensionColumns,
      metric = metricColumns,
      groupedTasksColumnsOpt = groupedTasksColumns,
      columnsNumber = columnsNumber
    )
  }
}
