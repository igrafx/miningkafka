package com.igrafx.kafka.sink.aggregationmain.domain.columnmapping.domain.entities.enums

import enumeratum._

import scala.collection.immutable

sealed trait ColumnAggregation extends EnumEntry

sealed trait DimensionAggregation extends ColumnAggregation
sealed trait GroupedTasksDimensionAggregation extends ColumnAggregation
sealed trait MetricAggregation extends ColumnAggregation

object ColumnAggregation extends Enum[ColumnAggregation] {
  override def values: immutable.IndexedSeq[ColumnAggregation] = findValues

  final case object FIRST extends MetricAggregation with DimensionAggregation with GroupedTasksDimensionAggregation
  final case object LAST extends MetricAggregation with DimensionAggregation with GroupedTasksDimensionAggregation
  final case object MIN extends MetricAggregation
  final case object MAX extends MetricAggregation
  final case object SUM extends MetricAggregation
  final case object AVG extends MetricAggregation
  final case object MEDIAN extends MetricAggregation
  final case object DISTINCT extends DimensionAggregation
}
