package com.igrafx.kafka.sink.aggregationmain.domain.columnmapping.controllers.dtos.enums

import com.igrafx.kafka.sink.aggregationmain.domain.columnmapping.domain.entities.enums.{
  ColumnAggregation,
  DimensionAggregation,
  GroupedTasksDimensionAggregation,
  MetricAggregation
}
import enumeratum._

sealed trait ColumnAggregationDto extends EnumEntry

sealed abstract class DimensionAggregationDto(override val entryName: String) extends ColumnAggregationDto

object DimensionAggregationDto extends Enum[DimensionAggregationDto] {
  override def values: IndexedSeq[DimensionAggregationDto] = findValues

  final case object FIRST extends DimensionAggregationDto("FIRST")
  final case object LAST extends DimensionAggregationDto("LAST")
  final case object DISTINCT extends DimensionAggregationDto("DISTINCT")

  implicit class DimensionAggregationDtoToEntity(dto: DimensionAggregationDto) {
    def toEntity: DimensionAggregation = {
      dto match {
        case FIRST => ColumnAggregation.FIRST
        case LAST => ColumnAggregation.LAST
        case DISTINCT => ColumnAggregation.DISTINCT
      }
    }
  }
}

sealed abstract class MetricAggregationDto(override val entryName: String) extends ColumnAggregationDto

object MetricAggregationDto extends Enum[MetricAggregationDto] {
  override def values: IndexedSeq[MetricAggregationDto] = findValues

  final case object FIRST extends MetricAggregationDto("FIRST")
  final case object LAST extends MetricAggregationDto("LAST")
  final case object MIN extends MetricAggregationDto("MIN")
  final case object MAX extends MetricAggregationDto("MAX")
  final case object SUM extends MetricAggregationDto("SUM")
  final case object AVG extends MetricAggregationDto("AVG")
  final case object MEDIAN extends MetricAggregationDto("MEDIAN")

  implicit class MetricAggregationDtoToEntity(dto: MetricAggregationDto) {
    def toEntity: MetricAggregation = {
      dto match {
        case FIRST => ColumnAggregation.FIRST
        case LAST => ColumnAggregation.LAST
        case MIN => ColumnAggregation.MIN
        case MAX => ColumnAggregation.MAX
        case SUM => ColumnAggregation.SUM
        case AVG => ColumnAggregation.AVG
        case MEDIAN => ColumnAggregation.MEDIAN
      }
    }
  }
}

sealed abstract class GroupedTasksDimensionAggregationDto(override val entryName: String) extends ColumnAggregationDto

object GroupedTasksDimensionAggregationDto extends Enum[GroupedTasksDimensionAggregationDto] {
  override def values: IndexedSeq[GroupedTasksDimensionAggregationDto] = findValues

  final case object FIRST extends GroupedTasksDimensionAggregationDto("FIRST")
  final case object LAST extends GroupedTasksDimensionAggregationDto("LAST")

  implicit class GroupedTasksDimensionAggregationDtoToEntity(dto: GroupedTasksDimensionAggregationDto) {
    def toEntity: GroupedTasksDimensionAggregation = {
      dto match {
        case FIRST => ColumnAggregation.FIRST
        case LAST => ColumnAggregation.LAST
      }
    }
  }
}
