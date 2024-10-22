package com.igrafx.kafka.sink.aggregationmain.domain.columnmapping.domain.entities

import com.igrafx.core.Mock
import com.igrafx.kafka.sink.aggregationmain.domain.columnmapping.domain.entities.enums.{
  ColumnAggregation,
  MetricAggregation
}

final class MetricAggregationInformationMock extends Mock[MetricAggregationInformation] {
  private var aggregation: Option[MetricAggregation] = Some(ColumnAggregation.AVG)
  private var isCaseScope: Boolean = true

  def setAggregation(aggregation: Option[MetricAggregation]): MetricAggregationInformationMock = {
    this.aggregation = aggregation
    this
  }

  def setIsCaseScope(isCaseScope: Boolean): MetricAggregationInformationMock = {
    this.isCaseScope = isCaseScope
    this
  }

  override def build(): MetricAggregationInformation = {
    MetricAggregationInformation(aggregation = aggregation, isCaseScope = isCaseScope)
  }
}
