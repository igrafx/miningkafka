package com.igrafx.kafka.sink.aggregationmain.domain.columnmapping.domain.entities

import com.igrafx.kafka.sink.aggregationmain.domain.columnmapping.domain.entities.enums.{
  ColumnAggregation,
  DimensionAggregation,
  MetricAggregation
}
import com.igrafx.kafka.sink.aggregationmain.domain.exceptions.WrongAggregationException

sealed trait AggregationInformation {
  val aggregation: Option[ColumnAggregation]
  val isCaseScope: Boolean

  require(!(isCaseScope && aggregation.isEmpty))
}

final case class DimensionAggregationInformation(
    override val aggregation: Option[DimensionAggregation],
    isCaseScope: Boolean
) extends AggregationInformation

object DimensionAggregationInformation {
  @throws[WrongAggregationException]
  def apply(
      aggregation: Option[DimensionAggregation],
      isCaseScope: Boolean
  ): DimensionAggregationInformation = {
    aggregation match {
      case _: Some[DimensionAggregation] => new DimensionAggregationInformation(aggregation, isCaseScope)
      case None =>
        if (isCaseScope) {
          throw WrongAggregationException()
        } else {
          new DimensionAggregationInformation(aggregation, isCaseScope)
        }
    }
  }
}

final case class MetricAggregationInformation(
    override val aggregation: Option[MetricAggregation],
    isCaseScope: Boolean
) extends AggregationInformation

object MetricAggregationInformation {
  @throws[WrongAggregationException]
  def apply(
      aggregation: Option[MetricAggregation],
      isCaseScope: Boolean
  ): MetricAggregationInformation = {
    aggregation match {
      case _: Some[MetricAggregation] => new MetricAggregationInformation(aggregation, isCaseScope)
      case None =>
        if (isCaseScope) {
          throw WrongAggregationException()
        } else {
          new MetricAggregationInformation(aggregation, isCaseScope)
        }
    }
  }
}
