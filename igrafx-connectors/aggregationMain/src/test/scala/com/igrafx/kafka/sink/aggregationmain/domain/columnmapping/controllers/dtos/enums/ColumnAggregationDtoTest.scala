package com.igrafx.kafka.sink.aggregationmain.domain.columnmapping.controllers.dtos.enums

import com.igrafx.kafka.sink.aggregationmain.domain.columnmapping.domain.entities.enums.ColumnAggregation
import core.UnitTestSpec

final class ColumnAggregationDtoTest extends UnitTestSpec {
  describe("DimensionAggregationDto toEntity") {
    it("should return the appropriate value") {
      assert(DimensionAggregationDto.withName("FIRST").toEntity == ColumnAggregation.FIRST)
      assert(DimensionAggregationDto.withName("LAST").toEntity == ColumnAggregation.LAST)
      assert(DimensionAggregationDto.withName("DISTINCT").toEntity == ColumnAggregation.DISTINCT)
    }
  }

  describe("MetricAggregationDto toEntity") {
    it("should return the appropriate value") {
      assert(MetricAggregationDto.withName("FIRST").toEntity == ColumnAggregation.FIRST)
      assert(MetricAggregationDto.withName("LAST").toEntity == ColumnAggregation.LAST)
      assert(MetricAggregationDto.withName("MIN").toEntity == ColumnAggregation.MIN)
      assert(MetricAggregationDto.withName("MAX").toEntity == ColumnAggregation.MAX)
      assert(MetricAggregationDto.withName("SUM").toEntity == ColumnAggregation.SUM)
      assert(MetricAggregationDto.withName("AVG").toEntity == ColumnAggregation.AVG)
      assert(MetricAggregationDto.withName("MEDIAN").toEntity == ColumnAggregation.MEDIAN)
    }
  }

  describe("GroupedTasksDimensionAggregationDto toEntity") {
    it("should return the appropriate value") {
      assert(GroupedTasksDimensionAggregationDto.withName("FIRST").toEntity == ColumnAggregation.FIRST)
      assert(GroupedTasksDimensionAggregationDto.withName("LAST").toEntity == ColumnAggregation.LAST)
    }
  }
}
