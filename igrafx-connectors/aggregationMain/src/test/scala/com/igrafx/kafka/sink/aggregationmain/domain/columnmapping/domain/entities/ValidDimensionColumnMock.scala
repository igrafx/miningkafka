package com.igrafx.kafka.sink.aggregationmain.domain.columnmapping.domain.entities

import com.igrafx.core.Mock
import com.igrafx.kafka.sink.aggregationmain.domain.columnmapping.domain.entities.enums.{
  ColumnAggregation,
  GroupedTasksDimensionAggregation
}
import com.igrafx.kafka.sink.aggregationmain.domain.entities.mocks.NonEmptyStringMock

final class ValidDimensionColumnMock extends Mock[ValidDimensionColumn] {
  private var columnIndex: Index = new IndexMock().setIndex(3).build()
  private var name: NonEmptyString = new NonEmptyStringMock().setStringValue("Country").build()
  private var aggregationInformation: DimensionAggregationInformation =
    new DimensionAggregationInformationMock().setAggregation(Some(ColumnAggregation.FIRST)).setIsCaseScope(true).build()
  private var groupedTasksAggregation: Option[GroupedTasksDimensionAggregation] = None

  def setColumnIndex(columnIndex: Index): ValidDimensionColumnMock = {
    this.columnIndex = columnIndex
    this
  }

  def setName(name: NonEmptyString): ValidDimensionColumnMock = {
    this.name = name
    this
  }

  def setAggregationInformation(aggregationInformation: DimensionAggregationInformation): ValidDimensionColumnMock = {
    this.aggregationInformation = aggregationInformation
    this
  }

  def setGroupedTasksAggregation(
      groupedTasksAggregation: Option[GroupedTasksDimensionAggregation]
  ): ValidDimensionColumnMock = {
    this.groupedTasksAggregation = groupedTasksAggregation
    this
  }

  override def build(): ValidDimensionColumn = {
    ValidDimensionColumn(
      columnIndex = columnIndex,
      name = name,
      aggregationInformation = aggregationInformation,
      groupedTasksAggregation = groupedTasksAggregation
    )
  }
}
