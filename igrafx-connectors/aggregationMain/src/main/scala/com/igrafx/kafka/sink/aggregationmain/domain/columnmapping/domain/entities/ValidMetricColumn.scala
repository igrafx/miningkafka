package com.igrafx.kafka.sink.aggregationmain.domain.columnmapping.domain.entities

import com.igrafx.kafka.sink.aggregationmain.domain.columnmapping.domain.entities.enums.MetricAggregation

final case class ValidMetricColumn(
    columnIndex: Index,
    name: NonEmptyString,
    unit: Option[String],
    aggregationInformation: MetricAggregationInformation,
    groupedTasksAggregation: Option[MetricAggregation]
)
