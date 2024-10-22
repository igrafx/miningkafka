package com.igrafx.kafka.sink.aggregationmain.domain.columnmapping.domain.entities

import com.igrafx.kafka.sink.aggregationmain.domain.entities.ColumnsNumber
import com.igrafx.kafka.sink.aggregationmain.domain.enums.ConnectorPropertiesEnum
import com.igrafx.kafka.sink.aggregationmain.domain.exceptions.InvalidColumnMappingException

final case class ValidColumnMapping(
    caseId: ValidCaseIdColumn,
    activity: ValidActivityColumn,
    time: Set[ValidTimeColumn],
    dimension: Set[ValidDimensionColumn],
    metric: Set[ValidMetricColumn],
    groupedTasksColumnsOpt: Option[ValidGroupedTasksColumns],
    private val columnsNumber: ColumnsNumber
) {
  require(
    time.nonEmpty &&
      time.size <= 2 &&
      groupedTasksColumnsOpt.forall(validGroupedTasksColumns =>
        (!validGroupedTasksColumns.groupedTasksColumns.contains(caseId.columnIndex)) &&
          validGroupedTasksColumns.groupedTasksColumns.contains(activity.columnIndex)
      )
  )
  checkColumnIndexes()
  checkGroupedTasks()

  @throws[InvalidColumnMappingException]
  def checkColumnIndexes(): Unit = {
    val indexSeqSize =
      (Seq(caseId.columnIndex, activity.columnIndex) ++
        time.map(_.columnIndex) ++
        dimension.map(_.columnIndex) ++
        metric.map(_.columnIndex)).size
    val indexSetSize =
      (Set(caseId.columnIndex, activity.columnIndex) ++
        time.map(_.columnIndex) ++
        dimension.map(_.columnIndex) ++
        metric.map(_.columnIndex)).size

    if (indexSeqSize != indexSetSize) {
      throw InvalidColumnMappingException(
        "Incoherence among the Column Indexes, they should all be different"
      )
    }
    if (indexSetSize != columnsNumber.number) {
      throw InvalidColumnMappingException(
        s"The expected number of columns is ${columnsNumber.number}, as defined by " +
          s"the ${ConnectorPropertiesEnum.csvFieldsNumberProperty} property, but $indexSetSize columns are defined"
      )
    }
  }

  @throws[InvalidColumnMappingException]
  def checkGroupedTasks(): Unit = {
    groupedTasksColumnsOpt match {
      case Some(ValidGroupedTasksColumns(groupedTasksColumns)) =>
        if (groupedTasksColumns.size < 2) {
          throw InvalidColumnMappingException(
            s"Incoherent grouped tasks information: When groupedTasksColumns is defined in property " +
              s"${ConnectorPropertiesEnum.columnMappingGroupedTasksColumnsProperty}, it must contain at least one " +
              s"column index from a time/dimension/metric column"
          )
        } else {
          if (
            dimension.forall(_.groupedTasksAggregation.isDefined) &&
            metric.forall(_.groupedTasksAggregation.isDefined)
          ) {
            ()
          } else {
            throw InvalidColumnMappingException(
              "Incoherent grouped tasks information: groupedTasksColumns defined in property " +
                s"${ConnectorPropertiesEnum.columnMappingGroupedTasksColumnsProperty} but not all dimensions or " +
                s"metrics have their grouped tasks aggregation defined. Please refer to the documentation to find " +
                s"the valid grouped tasks aggregation types"
            )
          }
        }
      case None =>
        if (
          dimension.exists(_.groupedTasksAggregation.isDefined) ||
          metric.exists(_.groupedTasksAggregation.isDefined)
        ) {
          throw InvalidColumnMappingException(
            "Incoherent grouped tasks information: at least one dimension or metric grouped tasks aggregation " +
              "defined but no groupedTasksColumns defined in property " +
              s"${ConnectorPropertiesEnum.columnMappingGroupedTasksColumnsProperty}"
          )
        } else {
          ()
        }
    }
  }
}

object ValidColumnMapping {
  @throws[InvalidColumnMappingException]
  def apply(
      caseId: ValidCaseIdColumn,
      activity: ValidActivityColumn,
      time: Set[ValidTimeColumn],
      dimension: Set[ValidDimensionColumn],
      metric: Set[ValidMetricColumn],
      groupedTasksColumnsOpt: Option[ValidGroupedTasksColumns],
      columnsNumber: ColumnsNumber
  ): ValidColumnMapping = {
    val groupedTasksColumnsWithoutCaseIdWithActivityOpt = groupedTasksColumnsOpt.map { validGroupedTasksColumns =>
      val groupedTasksColumnsWithoutCaseId =
        validGroupedTasksColumns.groupedTasksColumns.filterNot(index => index == caseId.columnIndex)
      val groupedTasksColumnsWithoutCaseIdWithActivity =
        if (groupedTasksColumnsWithoutCaseId.contains(activity.columnIndex)) {
          groupedTasksColumnsWithoutCaseId
        } else {
          groupedTasksColumnsWithoutCaseId + activity.columnIndex
        }

      ValidGroupedTasksColumns(groupedTasksColumnsWithoutCaseIdWithActivity)
    }

    new ValidColumnMapping(
      caseId = caseId,
      activity = activity,
      time = time,
      dimension = dimension,
      metric = metric,
      groupedTasksColumnsOpt = groupedTasksColumnsWithoutCaseIdWithActivityOpt,
      columnsNumber = columnsNumber
    )
  }
}
