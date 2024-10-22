package com.igrafx.kafka.sink.aggregationmain.domain.columnmapping.domain.entities

import com.igrafx.kafka.sink.aggregationmain.domain.columnmapping.domain.entities.enums.ColumnAggregation
import com.igrafx.kafka.sink.aggregationmain.domain.entities.mocks.ColumnsNumberMock
import com.igrafx.kafka.sink.aggregationmain.domain.exceptions.InvalidColumnMappingException
import core.UnitTestSpec

final class ValidColumnMappingTest extends UnitTestSpec {
  describe("ValidColumnMapping.apply") {
    it("should remove case id index from grouped tasks") {
      val caseIdIndex = 0
      val activityIndex = 1
      val timestampIndex = 2
      val dimensionIndex = 3
      val metricIndex = 4
      val groupedTasksColumns =
        Set(
          new IndexMock().setIndex(caseIdIndex).build(),
          new IndexMock().setIndex(activityIndex).build(),
          new IndexMock().setIndex(timestampIndex).build()
        )

      val groupedTasksColumnsOpt =
        Some(new ValidGroupedTasksColumnsMock().setGroupedTasksColumns(groupedTasksColumns).build())
      val caseId = new ValidCaseIdColumnMock().setColumnIndex(new IndexMock().setIndex(caseIdIndex).build()).build()
      val activity =
        new ValidActivityColumnMock().setColumnIndex(new IndexMock().setIndex(activityIndex).build()).build()
      val time = Set(new ValidTimeColumnMock().setColumnIndex(new IndexMock().setIndex(timestampIndex).build()).build())
      val dimension = Set(
        new ValidDimensionColumnMock()
          .setColumnIndex(new IndexMock().setIndex(dimensionIndex).build())
          .setGroupedTasksAggregation(Some(ColumnAggregation.FIRST))
          .build()
      )
      val metric = Set(
        new ValidMetricColumnMock()
          .setColumnIndex(new IndexMock().setIndex(metricIndex).build())
          .setGroupedTasksAggregation(Some(ColumnAggregation.MAX))
          .build()
      )
      val columnsNumber = new ColumnsNumberMock().setNumber(5).build()

      val res = ValidColumnMapping(
        caseId = caseId,
        activity = activity,
        time = time,
        dimension = dimension,
        metric = metric,
        groupedTasksColumnsOpt = groupedTasksColumnsOpt,
        columnsNumber = columnsNumber
      )

      assert(res.caseId == caseId)
      assert(res.activity == activity)
      assert(res.time == time)
      assert(res.dimension == dimension)
      assert(res.metric == metric)
      assert(
        res.groupedTasksColumnsOpt.map(_.groupedTasksColumns) == groupedTasksColumnsOpt.map(
          _.groupedTasksColumns.filterNot(index => index == caseId.columnIndex)
        )
      )
    }
    it("should add activity index to grouped tasks if not already defined") {
      val caseIdIndex = 0
      val activityIndex = 1
      val timestampIndex = 2
      val dimensionIndex = 3
      val metricIndex = 4
      val groupedTasksColumns =
        Set(
          new IndexMock().setIndex(timestampIndex).build()
        )

      val groupedTasksColumnsOpt =
        Some(new ValidGroupedTasksColumnsMock().setGroupedTasksColumns(groupedTasksColumns).build())
      val caseId = new ValidCaseIdColumnMock().setColumnIndex(new IndexMock().setIndex(caseIdIndex).build()).build()
      val activity =
        new ValidActivityColumnMock().setColumnIndex(new IndexMock().setIndex(activityIndex).build()).build()
      val time = Set(new ValidTimeColumnMock().setColumnIndex(new IndexMock().setIndex(timestampIndex).build()).build())
      val dimension = Set(
        new ValidDimensionColumnMock()
          .setColumnIndex(new IndexMock().setIndex(dimensionIndex).build())
          .setGroupedTasksAggregation(Some(ColumnAggregation.FIRST))
          .build()
      )
      val metric = Set(
        new ValidMetricColumnMock()
          .setColumnIndex(new IndexMock().setIndex(metricIndex).build())
          .setGroupedTasksAggregation(Some(ColumnAggregation.MAX))
          .build()
      )
      val columnsNumber = new ColumnsNumberMock().setNumber(5).build()

      val res = ValidColumnMapping(
        caseId = caseId,
        activity = activity,
        time = time,
        dimension = dimension,
        metric = metric,
        groupedTasksColumnsOpt = groupedTasksColumnsOpt,
        columnsNumber = columnsNumber
      )

      assert(res.caseId == caseId)
      assert(res.activity == activity)
      assert(res.time == time)
      assert(res.dimension == dimension)
      assert(res.metric == metric)
      assert(
        res.groupedTasksColumnsOpt.map(_.groupedTasksColumns) == groupedTasksColumnsOpt.map(
          _.groupedTasksColumns + activity.columnIndex
        )
      )
    }
  }

  describe("ValidColumnMapping") {
    it("should throw an exception if time is empty") {
      assertThrows[IllegalArgumentException] {
        new ValidColumnMappingMock()
          .setCaseId(new ValidCaseIdColumnMock().setColumnIndex(new IndexMock().setIndex(0).build()).build())
          .setActivity(new ValidActivityColumnMock().setColumnIndex(new IndexMock().setIndex(1).build()).build())
          .setTime(Set.empty)
          .setDimension(Set.empty)
          .setMetric(Set.empty)
          .setColumnsNumber(new ColumnsNumberMock().setNumber(2).build())
          .build()
      }
    }
    it("should throw an exception if time.size is > 2") {
      assertThrows[IllegalArgumentException] {
        new ValidColumnMappingMock()
          .setCaseId(new ValidCaseIdColumnMock().setColumnIndex(new IndexMock().setIndex(0).build()).build())
          .setActivity(new ValidActivityColumnMock().setColumnIndex(new IndexMock().setIndex(1).build()).build())
          .setTime(
            Set(
              new ValidTimeColumnMock()
                .setColumnIndex(new IndexMock().setIndex(2).build())
                .setFormat("dd/MM/yy HH:mm")
                .build(),
              new ValidTimeColumnMock()
                .setColumnIndex(new IndexMock().setIndex(3).build())
                .setFormat("dd/MM/yy HH:mm")
                .build(),
              new ValidTimeColumnMock()
                .setColumnIndex(new IndexMock().setIndex(4).build())
                .setFormat("dd/MM/yy HH:mm")
                .build()
            )
          )
          .setDimension(Set.empty)
          .setMetric(Set.empty)
          .setColumnsNumber(new ColumnsNumberMock().setNumber(2).build())
          .build()
      }
    }
  }

  describe("checkColumnIndexes") {
    it("should throw an InvalidColumnMappingException exception if at least two columnIndex are equal") {
      assertThrows[InvalidColumnMappingException] {
        new ValidColumnMappingMock()
          .setCaseId(new ValidCaseIdColumnMock().setColumnIndex(new IndexMock().setIndex(0).build()).build())
          .setActivity(new ValidActivityColumnMock().setColumnIndex(new IndexMock().setIndex(0).build()).build())
          .build()
      }
    }
    it("should throw an InvalidColumnMappingException exception if the number of expected columns is wrong") {
      assertThrows[InvalidColumnMappingException] {
        new ValidColumnMappingMock()
          .setCaseId(new ValidCaseIdColumnMock().setColumnIndex(new IndexMock().setIndex(0).build()).build())
          .setActivity(new ValidActivityColumnMock().setColumnIndex(new IndexMock().setIndex(1).build()).build())
          .setTime(Set(new ValidTimeColumnMock().setColumnIndex(new IndexMock().setIndex(2).build()).build()))
          .setDimension(Set(new ValidDimensionColumnMock().setColumnIndex(new IndexMock().setIndex(3).build()).build()))
          .setMetric(Set(new ValidMetricColumnMock().setColumnIndex(new IndexMock().setIndex(4).build()).build()))
          .setColumnsNumber(new ColumnsNumberMock().setNumber(4).build())
          .build()
      }

      assertThrows[InvalidColumnMappingException] {
        new ValidColumnMappingMock()
          .setCaseId(new ValidCaseIdColumnMock().setColumnIndex(new IndexMock().setIndex(0).build()).build())
          .setActivity(new ValidActivityColumnMock().setColumnIndex(new IndexMock().setIndex(1).build()).build())
          .setTime(Set(new ValidTimeColumnMock().setColumnIndex(new IndexMock().setIndex(2).build()).build()))
          .setDimension(Set(new ValidDimensionColumnMock().setColumnIndex(new IndexMock().setIndex(3).build()).build()))
          .setMetric(Set(new ValidMetricColumnMock().setColumnIndex(new IndexMock().setIndex(4).build()).build()))
          .setColumnsNumber(new ColumnsNumberMock().setNumber(6).build())
          .build()
      }
    }
  }

  describe("checkGroupedTasks") {
    it("should not throw an exception if grouped tasks columns are defined and valid") {
      val caseIdIndex = 0
      val activityIndex = 1
      val timestampIndex = 2
      val dimensionIndex = 3
      val metricIndex = 4
      val groupedTasksColumns =
        Set(new IndexMock().setIndex(activityIndex).build(), new IndexMock().setIndex(timestampIndex).build())

      new ValidColumnMappingMock()
        .setCaseId(new ValidCaseIdColumnMock().setColumnIndex(new IndexMock().setIndex(caseIdIndex).build()).build())
        .setActivity(
          new ValidActivityColumnMock().setColumnIndex(new IndexMock().setIndex(activityIndex).build()).build()
        )
        .setTime(
          Set(new ValidTimeColumnMock().setColumnIndex(new IndexMock().setIndex(timestampIndex).build()).build())
        )
        .setDimension(
          Set(
            new ValidDimensionColumnMock()
              .setColumnIndex(new IndexMock().setIndex(dimensionIndex).build())
              .setGroupedTasksAggregation(Some(ColumnAggregation.FIRST))
              .build()
          )
        )
        .setMetric(
          Set(
            new ValidMetricColumnMock()
              .setColumnIndex(new IndexMock().setIndex(metricIndex).build())
              .setGroupedTasksAggregation(Some(ColumnAggregation.MAX))
              .build()
          )
        )
        .setColumnsNumber(new ColumnsNumberMock().setNumber(5).build())
        .setGroupedTasksColumnsOpt(
          Some(new ValidGroupedTasksColumnsMock().setGroupedTasksColumns(groupedTasksColumns).build())
        )
        .build()

      succeed
    }
    it(
      "should throw an InvalidColumnMappingException if grouped tasks columns are defined but there is less than 2 columns in the group (excluding the caseId column)"
    ) {
      val caseIdIndex = 0
      val activityIndex = 1
      val timestampIndex = 2
      val dimensionIndex = 3
      val metricIndex = 4
      val groupedTasksColumns =
        Set(new IndexMock().setIndex(caseIdIndex).build(), new IndexMock().setIndex(activityIndex).build())

      assertThrows[InvalidColumnMappingException] {
        new ValidColumnMappingMock()
          .setCaseId(new ValidCaseIdColumnMock().setColumnIndex(new IndexMock().setIndex(caseIdIndex).build()).build())
          .setActivity(
            new ValidActivityColumnMock().setColumnIndex(new IndexMock().setIndex(activityIndex).build()).build()
          )
          .setTime(
            Set(new ValidTimeColumnMock().setColumnIndex(new IndexMock().setIndex(timestampIndex).build()).build())
          )
          .setDimension(
            Set(
              new ValidDimensionColumnMock()
                .setColumnIndex(new IndexMock().setIndex(dimensionIndex).build())
                .setGroupedTasksAggregation(Some(ColumnAggregation.FIRST))
                .build()
            )
          )
          .setMetric(
            Set(
              new ValidMetricColumnMock()
                .setColumnIndex(new IndexMock().setIndex(metricIndex).build())
                .setGroupedTasksAggregation(Some(ColumnAggregation.MAX))
                .build()
            )
          )
          .setColumnsNumber(new ColumnsNumberMock().setNumber(5).build())
          .setGroupedTasksColumnsOpt(
            Some(new ValidGroupedTasksColumnsMock().setGroupedTasksColumns(groupedTasksColumns).build())
          )
          .build()
      }
    }
    it(
      "should throw an InvalidColumnMappingException if grouped tasks columns are defined but not all dimensions and/or metrics have a groupedTasksAggregation defined"
    ) {
      val caseIdIndex = 0
      val activityIndex = 1
      val timestampIndex = 2
      val dimensionIndex = 3
      val metricIndex = 4
      val groupedTasksColumns =
        Set(new IndexMock().setIndex(activityIndex).build(), new IndexMock().setIndex(timestampIndex).build())

      assertThrows[InvalidColumnMappingException] {
        new ValidColumnMappingMock()
          .setCaseId(new ValidCaseIdColumnMock().setColumnIndex(new IndexMock().setIndex(caseIdIndex).build()).build())
          .setActivity(
            new ValidActivityColumnMock().setColumnIndex(new IndexMock().setIndex(activityIndex).build()).build()
          )
          .setTime(
            Set(new ValidTimeColumnMock().setColumnIndex(new IndexMock().setIndex(timestampIndex).build()).build())
          )
          .setDimension(
            Set(
              new ValidDimensionColumnMock()
                .setColumnIndex(new IndexMock().setIndex(dimensionIndex).build())
                .setGroupedTasksAggregation(None)
                .build()
            )
          )
          .setMetric(
            Set(
              new ValidMetricColumnMock()
                .setColumnIndex(new IndexMock().setIndex(metricIndex).build())
                .setGroupedTasksAggregation(Some(ColumnAggregation.MAX))
                .build()
            )
          )
          .setColumnsNumber(new ColumnsNumberMock().setNumber(5).build())
          .setGroupedTasksColumnsOpt(
            Some(new ValidGroupedTasksColumnsMock().setGroupedTasksColumns(groupedTasksColumns).build())
          )
          .build()
      }
    }
    it(
      "should throw an InvalidColumnMappingException if at least one dimension and/or metric has a groupedTasksAggregation defined but the grouped tasks columns are not defined"
    ) {
      val caseIdIndex = 0
      val activityIndex = 1
      val timestampIndex = 2
      val dimensionIndex = 3
      val metricIndex = 4

      assertThrows[InvalidColumnMappingException] {
        new ValidColumnMappingMock()
          .setCaseId(new ValidCaseIdColumnMock().setColumnIndex(new IndexMock().setIndex(caseIdIndex).build()).build())
          .setActivity(
            new ValidActivityColumnMock().setColumnIndex(new IndexMock().setIndex(activityIndex).build()).build()
          )
          .setTime(
            Set(new ValidTimeColumnMock().setColumnIndex(new IndexMock().setIndex(timestampIndex).build()).build())
          )
          .setDimension(
            Set(
              new ValidDimensionColumnMock()
                .setColumnIndex(new IndexMock().setIndex(dimensionIndex).build())
                .setGroupedTasksAggregation(Some(ColumnAggregation.FIRST))
                .build()
            )
          )
          .setMetric(
            Set(
              new ValidMetricColumnMock()
                .setColumnIndex(new IndexMock().setIndex(metricIndex).build())
                .setGroupedTasksAggregation(Some(ColumnAggregation.MAX))
                .build()
            )
          )
          .setColumnsNumber(new ColumnsNumberMock().setNumber(5).build())
          .setGroupedTasksColumnsOpt(None)
          .build()
      }
    }
  }
}
