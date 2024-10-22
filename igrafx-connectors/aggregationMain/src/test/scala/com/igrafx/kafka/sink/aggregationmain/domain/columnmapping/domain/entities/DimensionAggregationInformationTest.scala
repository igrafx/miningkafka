package com.igrafx.kafka.sink.aggregationmain.domain.columnmapping.domain.entities

import com.igrafx.kafka.sink.aggregationmain.domain.columnmapping.domain.entities.enums.ColumnAggregation
import com.igrafx.kafka.sink.aggregationmain.domain.exceptions.WrongAggregationException
import core.UnitTestSpec

final class DimensionAggregationInformationTest extends UnitTestSpec {
  describe("DimensionAggregationInformation.apply") {
    it("should return a DimensionAggregationInformation if isCaseScope is false and aggregation is defined") {
      val aggregation = Some(ColumnAggregation.FIRST)
      val isCaseScope = false

      val res = DimensionAggregationInformation(aggregation, isCaseScope)

      assert(res.isCaseScope == isCaseScope && res.aggregation == aggregation)
    }
    it("should return a DimensionAggregationInformation if isCaseScope is false and no aggregation defined") {
      val aggregation = None
      val isCaseScope = false

      val res = DimensionAggregationInformation(aggregation, isCaseScope)

      assert(res.isCaseScope == isCaseScope && res.aggregation == aggregation)
    }
    it("should return a DimensionAggregationInformation if isCaseScope is true and aggregation is defined") {
      val aggregation = Some(ColumnAggregation.FIRST)
      val isCaseScope = true

      val res = DimensionAggregationInformation(aggregation, isCaseScope)

      assert(res.isCaseScope == isCaseScope && res.aggregation == aggregation)
    }
    it("should throw a WrongAggregationException if isCaseScope is true and no aggregation defined") {
      val aggregation = None
      val isCaseScope = true

      assertThrows[WrongAggregationException](DimensionAggregationInformation(aggregation, isCaseScope))
    }
  }
}
