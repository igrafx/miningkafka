package com.igrafx.kafka.sink.aggregationmain.domain.columnmapping.domain.entities

import com.igrafx.kafka.sink.aggregationmain.domain.columnmapping.domain.entities.enums.ColumnAggregation
import com.igrafx.kafka.sink.aggregationmain.domain.exceptions.WrongAggregationException
import core.UnitTestSpec

final class MetricAggregationInformationTest extends UnitTestSpec {
  describe("MetricAggregationInformation.apply") {
    it("should return a MetricAggregationInformation if isCaseScope is false and aggregation is defined") {
      val aggregation = Some(ColumnAggregation.FIRST)
      val isCaseScope = false

      val res = MetricAggregationInformation(aggregation, isCaseScope)

      assert(res.isCaseScope == isCaseScope && res.aggregation == aggregation)
    }
    it("should return a MetricAggregationInformation if isCaseScope is false and no aggregation defined") {
      val aggregation = None
      val isCaseScope = false

      val res = MetricAggregationInformation(aggregation, isCaseScope)

      assert(res.isCaseScope == isCaseScope && res.aggregation == aggregation)
    }
    it("should return a MetricAggregationInformation if isCaseScope is true and aggregation is defined") {
      val aggregation = Some(ColumnAggregation.FIRST)
      val isCaseScope = true

      val res = MetricAggregationInformation(aggregation, isCaseScope)

      assert(res.isCaseScope == isCaseScope && res.aggregation == aggregation)
    }
    it("should throw a WrongAggregationException if isCaseScope is true and no aggregation defined") {
      val aggregation = None
      val isCaseScope = true

      assertThrows[WrongAggregationException](MetricAggregationInformation(aggregation, isCaseScope))
    }
  }
}
