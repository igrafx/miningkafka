package com.igrafx.kafka.sink.aggregationmain.domain.columnmapping.domain.entities

import com.igrafx.core.Mock
import com.igrafx.kafka.sink.aggregationmain.domain.columnmapping.domain.entities.enums.ColumnAggregation
import com.igrafx.kafka.sink.aggregationmain.domain.entities.ColumnsNumber
import com.igrafx.kafka.sink.aggregationmain.domain.entities.mocks.{ColumnsNumberMock, NonEmptyStringMock}

final class ValidColumnMappingMock extends Mock[ValidColumnMapping] {
  private var caseId: ValidCaseIdColumn =
    new ValidCaseIdColumnMock().setColumnIndex(new IndexMock().setIndex(0).build()).build()
  private var activity: ValidActivityColumn =
    new ValidActivityColumnMock()
      .setColumnIndex(new IndexMock().setIndex(1).build())
      .build()
  private var time: Set[ValidTimeColumn] = Set(
    new ValidTimeColumnMock().setColumnIndex(new IndexMock().setIndex(2).build()).setFormat("dd/MM/yy HH:mm").build(),
    new ValidTimeColumnMock().setColumnIndex(new IndexMock().setIndex(3).build()).setFormat("dd/MM/yy HH:mm").build()
  )
  private var dimension: Set[ValidDimensionColumn] = Set(
    new ValidDimensionColumnMock()
      .setColumnIndex(new IndexMock().setIndex(4).build())
      .setName(new NonEmptyStringMock().setStringValue("Country").build())
      .setAggregationInformation(
        new DimensionAggregationInformationMock()
          .setAggregation(Some(ColumnAggregation.LAST))
          .setIsCaseScope(true)
          .build()
      )
      .setGroupedTasksAggregation(None)
      .build(),
    new ValidDimensionColumnMock()
      .setColumnIndex(new IndexMock().setIndex(5).build())
      .setName(new NonEmptyStringMock().setStringValue("Region").build())
      .setAggregationInformation(
        new DimensionAggregationInformationMock()
          .setAggregation(None)
          .setIsCaseScope(false)
          .build()
      )
      .setGroupedTasksAggregation(None)
      .build(),
    new ValidDimensionColumnMock()
      .setColumnIndex(new IndexMock().setIndex(6).build())
      .setName(new NonEmptyStringMock().setStringValue("City").build())
      .setAggregationInformation(
        new DimensionAggregationInformationMock()
          .setAggregation(Some(ColumnAggregation.FIRST))
          .setIsCaseScope(false)
          .build()
      )
      .setGroupedTasksAggregation(None)
      .build()
  )
  private var metric: Set[ValidMetricColumn] =
    Set(
      new ValidMetricColumnMock()
        .setColumnIndex(new IndexMock().setIndex(7).build())
        .setName(new NonEmptyStringMock().setStringValue("DepartmentNumber").build())
        .setUnit(None)
        .setAggregationInformation(
          new MetricAggregationInformationMock()
            .setAggregation(Some(ColumnAggregation.MIN))
            .setIsCaseScope(true)
            .build()
        )
        .setGroupedTasksAggregation(None)
        .build(),
      new ValidMetricColumnMock()
        .setColumnIndex(new IndexMock().setIndex(8).build())
        .setName(new NonEmptyStringMock().setStringValue("Price").build())
        .setUnit(Some("Euros"))
        .setAggregationInformation(
          new MetricAggregationInformationMock().setAggregation(None).setIsCaseScope(false).build()
        )
        .setGroupedTasksAggregation(None)
        .build()
    )
  private var groupedTasksColumnsOpt: Option[ValidGroupedTasksColumns] = None
  private var columnsNumber: ColumnsNumber = new ColumnsNumberMock().setNumber(9).build()

  def setCaseId(caseId: ValidCaseIdColumn): ValidColumnMappingMock = {
    this.caseId = caseId
    this
  }

  def setActivity(activity: ValidActivityColumn): ValidColumnMappingMock = {
    this.activity = activity
    this
  }

  def setTime(time: Set[ValidTimeColumn]): ValidColumnMappingMock = {
    this.time = time
    this
  }

  def setDimension(dimension: Set[ValidDimensionColumn]): ValidColumnMappingMock = {
    this.dimension = dimension
    this
  }

  def setMetric(metric: Set[ValidMetricColumn]): ValidColumnMappingMock = {
    this.metric = metric
    this
  }

  def setGroupedTasksColumnsOpt(groupedTasksColumnsOpt: Option[ValidGroupedTasksColumns]): ValidColumnMappingMock = {
    this.groupedTasksColumnsOpt = groupedTasksColumnsOpt
    this
  }

  def setColumnsNumber(columnsNumber: ColumnsNumber): ValidColumnMappingMock = {
    this.columnsNumber = columnsNumber
    this
  }

  override def build(): ValidColumnMapping = {
    ValidColumnMapping(
      caseId = caseId,
      activity = activity,
      time = time,
      dimension = dimension,
      metric = metric,
      groupedTasksColumnsOpt = groupedTasksColumnsOpt,
      columnsNumber = columnsNumber
    )
  }
}
