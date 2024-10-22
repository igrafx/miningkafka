package com.igrafx.kafka.sink.aggregationmain.domain.columnmapping.domain.entities

import com.igrafx.kafka.sink.aggregationmain.Constants
import com.igrafx.kafka.sink.aggregationmain.domain.entities.mocks.{ColumnsNumberMock, NonEmptyStringMock}
import core.UnitTestSpec

final class ColumnMappingPropertiesTest extends UnitTestSpec {
  describe("toColumnNamesSeq") {
    it("should return the columns name in correct order") {
      val caseIdIndex = 0
      val activityIndex = 1
      val timestamp1Index = 2
      val timestamp2Index = 3
      val dimensionIndex1 = 4
      val dimensionName1 = "dimensionNameTest1"
      val metricIndex1 = 5
      val metricName1 = "dimensionNameTest1"
      val dimensionIndex2 = 6
      val dimensionName2 = "dimensionNameTest2"
      val metricIndex2 = 7
      val metricName2 = "dimensionNameTest2"
      val columnMapping = new ValidColumnMappingMock()
        .setCaseId(new ValidCaseIdColumnMock().setColumnIndex(new IndexMock().setIndex(caseIdIndex).build()).build())
        .setActivity(
          new ValidActivityColumnMock().setColumnIndex(new IndexMock().setIndex(activityIndex).build()).build()
        )
        .setTime(
          Set(
            new ValidTimeColumnMock().setColumnIndex(new IndexMock().setIndex(timestamp1Index).build()).build(),
            new ValidTimeColumnMock().setColumnIndex(new IndexMock().setIndex(timestamp2Index).build()).build()
          )
        )
        .setDimension(
          Set(
            new ValidDimensionColumnMock()
              .setColumnIndex(new IndexMock().setIndex(dimensionIndex1).build())
              .setName(new NonEmptyStringMock().setStringValue(dimensionName1).build())
              .build(),
            new ValidDimensionColumnMock()
              .setColumnIndex(new IndexMock().setIndex(dimensionIndex2).build())
              .setName(new NonEmptyStringMock().setStringValue(dimensionName2).build())
              .build()
          )
        )
        .setMetric(
          Set(
            new ValidMetricColumnMock()
              .setColumnIndex(new IndexMock().setIndex(metricIndex1).build())
              .setName(new NonEmptyStringMock().setStringValue(metricName1).build())
              .build(),
            new ValidMetricColumnMock()
              .setColumnIndex(new IndexMock().setIndex(metricIndex2).build())
              .setName(new NonEmptyStringMock().setStringValue(metricName2).build())
              .build()
          )
        )
        .setColumnsNumber(new ColumnsNumberMock().setNumber(8).build())
        .build()

      val columnMappingProperties =
        new ColumnMappingPropertiesMock().setColumnMapping(columnMapping).build()

      val expectedResult =
        Seq(
          Constants.caseIdColumnName,
          Constants.activityColumnName,
          s"${Constants.timestampColumnName}1",
          s"${Constants.timestampColumnName}2",
          dimensionName1,
          metricName1,
          dimensionName2,
          metricName2
        )

      val res = columnMappingProperties.toColumnNamesSeq

      assert(res == expectedResult)
    }
  }
}
