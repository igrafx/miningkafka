package com.igrafx.kafka.sink.aggregationmain.domain.columnmapping.domain.entities

import com.igrafx.core.Mock
import com.igrafx.kafka.sink.aggregationmain.domain.columnmapping.domain.entities.enums.{
  ColumnAggregation,
  MetricAggregation
}
import com.igrafx.kafka.sink.aggregationmain.domain.entities.mocks.NonEmptyStringMock

final class ValidMetricColumnMock extends Mock[ValidMetricColumn] {
  private var columnIndex: Index = new IndexMock().setIndex(4).build()
  private var name: NonEmptyString = new NonEmptyStringMock().setStringValue("Price").build()
  private var unit: Option[String] = Some("Euros")
  private var aggregationInformation: MetricAggregationInformation =
    new MetricAggregationInformationMock().setAggregation(Some(ColumnAggregation.AVG)).setIsCaseScope(true).build()
  private var groupedTasksAggregation: Option[MetricAggregation] = None

  def setColumnIndex(columnIndex: Index): ValidMetricColumnMock = {
    this.columnIndex = columnIndex
    this
  }

  def setName(name: NonEmptyString): ValidMetricColumnMock = {
    this.name = name
    this
  }

  def setUnit(unit: Option[String]): ValidMetricColumnMock = {
    this.unit = unit
    this
  }

  def setAggregationInformation(aggregationInformation: MetricAggregationInformation): ValidMetricColumnMock = {
    this.aggregationInformation = aggregationInformation
    this
  }

  def setGroupedTasksAggregation(
      groupedTasksAggregation: Option[MetricAggregation]
  ): ValidMetricColumnMock = {
    this.groupedTasksAggregation = groupedTasksAggregation
    this
  }

  override def build(): ValidMetricColumn = {
    ValidMetricColumn(
      columnIndex = columnIndex,
      name = name,
      unit = unit,
      aggregationInformation = aggregationInformation,
      groupedTasksAggregation = groupedTasksAggregation
    )
  }
}
