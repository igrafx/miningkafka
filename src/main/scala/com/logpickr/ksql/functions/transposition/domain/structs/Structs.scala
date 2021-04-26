package com.logpickr.ksql.functions.transposition.domain.structs

import org.apache.kafka.connect.data.{Schema, SchemaBuilder}

object Structs {
  final val STRUCT_SCHEMA = SchemaBuilder
    .struct()
    .optional()
    .field("TASK", Schema.OPTIONAL_STRING_SCHEMA)
    .field("TIME", Schema.OPTIONAL_STRING_SCHEMA)
    .build()

  final val STRUCT_SCHEMA_DESCRIPTOR = "STRUCT<TASK VARCHAR(STRING), TIME VARCHAR(STRING)>"

  final val STRUCT_SCHEMA_SPECIAL = SchemaBuilder
    .struct()
    .optional()
    .field("TASK", Schema.OPTIONAL_STRING_SCHEMA)
    .field("START", Schema.OPTIONAL_STRING_SCHEMA)
    .field("STOP", Schema.OPTIONAL_STRING_SCHEMA)
    .build()

  final val STRUCT_SCHEMA_SPECIAL_DESCRIPTOR =
    "STRUCT<TASK VARCHAR(STRING), START VARCHAR(STRING), STOP VARCHAR(STRING)>"

}
