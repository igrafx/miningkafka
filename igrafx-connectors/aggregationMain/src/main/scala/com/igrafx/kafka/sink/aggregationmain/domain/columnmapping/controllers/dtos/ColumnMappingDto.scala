package com.igrafx.kafka.sink.aggregationmain.domain.columnmapping.controllers.dtos

import com.igrafx.kafka.sink.aggregationmain.domain.columnmapping.domain.entities._
import com.igrafx.kafka.sink.aggregationmain.domain.entities.ColumnsNumber
import com.igrafx.kafka.sink.aggregationmain.domain.exceptions.{
  InvalidColumnMappingException,
  InvalidPropertyValueException
}
import org.apache.kafka.common.config.AbstractConfig
import org.json4s.ParserUtil.ParseException
import org.slf4j.Logger

final case class ColumnMappingDto(
    caseIdColumnIndex: CaseIdColumnDto,
    activityColumnIndex: ActivityColumnDto,
    time: Set[TimeColumnDto],
    dimension: Set[DimensionColumnDto],
    metric: Set[MetricColumnDto],
    groupedTasksColumnsOpt: Option[GroupedTasksColumnsDto]
)(implicit log: Logger) {
  @throws[InvalidPropertyValueException]
  @throws[InvalidColumnMappingException]
  def toEntity(columnsNumber: ColumnsNumber)(implicit
      log: Logger
  ): ValidColumnMapping = {
    val columnMappingCaseId: ValidCaseIdColumn =
      caseIdColumnIndex.toEntity(columnsNumber)
    val columnMappingActivity: ValidActivityColumn =
      activityColumnIndex.toEntity(columnsNumber)
    val columnMappingTimes: Set[ValidTimeColumn] =
      time.map(_.toEntity(columnsNumber))
    val columnMappingDimensions: Set[ValidDimensionColumn] =
      dimension.map(_.toEntity(columnsNumber))
    val columnMappingMetrics: Set[ValidMetricColumn] =
      metric.map(_.toEntity(columnsNumber))
    val columnMappingGroupedTasksColumns: Option[ValidGroupedTasksColumns] =
      groupedTasksColumnsOpt.map(_.toEntity(columnsNumber))

    ValidColumnMapping(
      caseId = columnMappingCaseId,
      activity = columnMappingActivity,
      time = columnMappingTimes,
      dimension = columnMappingDimensions,
      metric = columnMappingMetrics,
      groupedTasksColumnsOpt = columnMappingGroupedTasksColumns,
      columnsNumber = columnsNumber
    )
  }
}

object ColumnMappingDto {
  @throws[InvalidPropertyValueException]
  @throws[ParseException]
  def fromConnectorConfig(
      connectorConfig: AbstractConfig
  )(implicit log: Logger): ColumnMappingDto = {
    val caseIdColumnIndex = CaseIdColumnDto.fromConnectorConfig(connectorConfig)
    val activityColumnIndex = ActivityColumnDto.fromConnectorConfig(connectorConfig)
    val timeColumnsDto = TimeColumnsDto.fromConnectorConfig(connectorConfig)
    val dimensionColumnsDto = DimensionColumnsDto.fromConnectorConfig(connectorConfig)
    val metricColumnsDto = MetricColumnsDto.fromConnectorConfig(connectorConfig)
    val groupedTasksColumnsDto = GroupedTasksColumnsDto.fromConnectorConfig(connectorConfig)

    ColumnMappingDto(
      caseIdColumnIndex = caseIdColumnIndex,
      activityColumnIndex = activityColumnIndex,
      time = timeColumnsDto,
      dimension = dimensionColumnsDto,
      metric = metricColumnsDto,
      groupedTasksColumnsOpt = groupedTasksColumnsDto
    )
  }
}
