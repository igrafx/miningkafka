package com.igrafx.kafka.sink.aggregationmain.config

object MainConfig {
  val csvPath: String = sys.env.getOrElse("IGRAFX_AGGREGATION_SINK_MAIN_CSV_PATH", "/aggregation-sink-igrafx")
}
