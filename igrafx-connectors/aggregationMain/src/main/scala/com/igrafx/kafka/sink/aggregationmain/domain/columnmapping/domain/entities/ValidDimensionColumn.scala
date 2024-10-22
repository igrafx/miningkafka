package com.igrafx.kafka.sink.aggregationmain.domain.columnmapping.domain.entities

import com.igrafx.kafka.sink.aggregationmain.domain.columnmapping.domain.entities.enums.GroupedTasksDimensionAggregation

final case class ValidDimensionColumn(
    columnIndex: Index,
    name: NonEmptyString,
    aggregationInformation: DimensionAggregationInformation,
    groupedTasksAggregation: Option[GroupedTasksDimensionAggregation]
)
