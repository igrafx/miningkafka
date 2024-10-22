package com.igrafx.kafka.sink.aggregationmain.domain.columnmapping.domain.entities

import com.igrafx.core.Mock
import com.igrafx.kafka.sink.aggregationmain.domain.columnmapping.domain.entities.enums.{
  ColumnAggregation,
  DimensionAggregation
}

final class DimensionAggregationInformationMock extends Mock[DimensionAggregationInformation] {
  private var aggregation: Option[DimensionAggregation] = Some(ColumnAggregation.FIRST)
  private var isCaseScope: Boolean = true

  def setAggregation(aggregation: Option[DimensionAggregation]): DimensionAggregationInformationMock = {
    this.aggregation = aggregation
    this
  }

  def setIsCaseScope(isCaseScope: Boolean): DimensionAggregationInformationMock = {
    this.isCaseScope = isCaseScope
    this
  }

  override def build(): DimensionAggregationInformation = {
    DimensionAggregationInformation(aggregation = aggregation, isCaseScope = isCaseScope)
  }
}
