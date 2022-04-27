package com.logpickr.ksql.functions.caseevents.domain.structs

import com.logpickr.ksql.functions.caseevents.Constants.{structEndDate, structStartDate, structVertexName}
import org.apache.kafka.connect.data.{Schema, SchemaBuilder}

object CaseEventsStructs {
  final val STRUCT_SCHEMA = SchemaBuilder
    .struct()
    .optional()
    .field(structStartDate, Schema.OPTIONAL_STRING_SCHEMA)
    .field(structEndDate, Schema.OPTIONAL_STRING_SCHEMA)
    .field(structVertexName, Schema.OPTIONAL_STRING_SCHEMA)
    .build()

  final val STRUCT_SCHEMA_DESCRIPTOR =
    "STRUCT<START_DATE VARCHAR(STRING), END_DATE VARCHAR(STRING), VERTEX_NAME VARCHAR(STRING)>"
}
