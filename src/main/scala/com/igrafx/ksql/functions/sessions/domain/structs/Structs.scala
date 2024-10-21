package com.igrafx.ksql.functions.sessions.domain.structs

import com.igrafx.ksql.functions.sessions.Constants.{structLine, structSessionId}
import org.apache.kafka.connect.data.{Schema, SchemaBuilder}

object Structs {
  final val STRUCT_OUTPUT_SCHEMA = SchemaBuilder
    .struct()
    .optional()
    .field(structSessionId, Schema.OPTIONAL_STRING_SCHEMA)
    .field(structLine, Schema.OPTIONAL_STRING_SCHEMA)
    .build()

  final val STRUCT_OUTPUT_SCHEMA_DESCRIPTOR = "STRUCT<SESSION_ID VARCHAR(STRING), LINE VARCHAR(STRING)>"
}
