package com.igrafx.kafka.sink.aggregationmain.domain.columnmapping.domain.entities

import com.igrafx.kafka.sink.aggregationmain.Constants
import com.igrafx.kafka.sink.aggregationmain.domain.entities.CsvHeaderColumn
import org.json4s.JObject
import org.json4s.JsonDSL._

final case class ColumnMappingProperties(
    configurationProperties: ColumnMappingConfigurationProperties,
    columnMapping: ValidColumnMapping,
    fileStructure: FileStructure
) {
  def toJson: JObject = {
    //format: off
    ("fileStructure" -> (
      ("charset" -> fileStructure.csvEncoding.toStringDescription) ~
      ("delimiter" -> fileStructure.csvSeparator) ~
      ("quoteChar" -> fileStructure.csvQuote) ~
      ("escapeChar" -> fileStructure.csvEscape.character) ~
      ("eolChar" -> fileStructure.csvEndOfLine.stringValue) ~
      ("header" -> fileStructure.csvHeader) ~
      ("commentChar" -> fileStructure.csvComment.character) ~
      ("fileType" -> fileStructure.fileType)
    )) ~
    ("columnMapping" ->
      ("caseIdMapping" ->
        ("columnIndex" -> columnMapping.caseId.columnIndex.index)) ~
      ("activityMapping" -> (
        ("columnIndex" -> columnMapping.activity.columnIndex.index) ~
        ("groupedTasksColumns" -> columnMapping.groupedTasksColumnsOpt.map(_.groupedTasksColumns.map(_.index)))
      )) ~
      ("timeMappings" ->
        columnMapping.time.map { timeColumnInfo =>
          ("columnIndex" -> timeColumnInfo.columnIndex.index) ~
          ("format" -> timeColumnInfo.format)
        }
      ) ~
      ("dimensionsMappings" ->
        columnMapping.dimension.map { dimensionColumnInfo =>
          ("columnIndex" -> dimensionColumnInfo.columnIndex.index) ~
          ("name" -> dimensionColumnInfo.name.stringValue) ~
          ("isCaseScope" -> dimensionColumnInfo.aggregationInformation.isCaseScope) ~
          ("aggregation" -> dimensionColumnInfo.aggregationInformation.aggregation.map(_.toString)) ~
          ("groupedTasksAggregation" -> dimensionColumnInfo.groupedTasksAggregation.map(_.toString))
        }
      ) ~
      ("metricsMappings" ->
        columnMapping.metric.map { metricColumnInfo =>
          ("columnIndex" -> metricColumnInfo.columnIndex.index) ~
          ("name" -> metricColumnInfo.name.stringValue) ~
          ("unit" -> metricColumnInfo.unit) ~
          ("isCaseScope" -> metricColumnInfo.aggregationInformation.isCaseScope) ~
          ("aggregation" -> metricColumnInfo.aggregationInformation.aggregation.map(_.toString)) ~
          ("groupedTasksAggregation" -> metricColumnInfo.groupedTasksAggregation.map(_.toString))
        }
      )
    )
    //format: on
  }

  def toColumnNamesSeq: Seq[String] = {
    val basicSeq: Seq[CsvHeaderColumn] =
      Seq(
        CsvHeaderColumn(columnMapping.caseId.columnIndex.index, Constants.caseIdColumnName),
        CsvHeaderColumn(columnMapping.activity.columnIndex.index, Constants.activityColumnName)
      )
    val timeSeq: Seq[CsvHeaderColumn] = columnMapping.time.toSeq.sortBy(_.columnIndex.index).zipWithIndex.map {
      case (time: ValidTimeColumn, index: Int) =>
        CsvHeaderColumn(time.columnIndex.index, s"${Constants.timestampColumnName}${index + 1}")
    }
    val dimensionSeq: Seq[CsvHeaderColumn] = columnMapping.dimension.toSeq.map { dimension: ValidDimensionColumn =>
      CsvHeaderColumn(dimension.columnIndex.index, dimension.name.stringValue)
    }
    val metricsSeq: Seq[CsvHeaderColumn] = columnMapping.metric.toSeq.map { metric: ValidMetricColumn =>
      CsvHeaderColumn(metric.columnIndex.index, metric.name.stringValue)
    }
    (basicSeq ++ timeSeq ++ dimensionSeq ++ metricsSeq).sortBy(_.columnIndex).map(_.columnName)
  }
}
