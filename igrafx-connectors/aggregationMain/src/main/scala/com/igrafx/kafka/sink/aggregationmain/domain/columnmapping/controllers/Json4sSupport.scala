package com.igrafx.kafka.sink.aggregationmain.domain.columnmapping.controllers

import com.igrafx.kafka.sink.aggregationmain.domain.columnmapping.controllers.dtos.enums.{
  DimensionAggregationDto,
  GroupedTasksDimensionAggregationDto,
  MetricAggregationDto
}
import enumeratum.Json4s
import org.json4s.{DefaultFormats, Formats}

trait Json4sSupport {
  protected final implicit val jsonFormats: Formats =
    DefaultFormats +
      Json4s.serializer(DimensionAggregationDto) +
      Json4s.serializer(MetricAggregationDto) +
      Json4s.serializer(GroupedTasksDimensionAggregationDto)
}
