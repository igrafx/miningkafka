package com.igrafx.kafka.sink.aggregationmain.domain.entities

import com.igrafx.kafka.sink.aggregationmain.Constants

final case class ColumnsNumber(number: Int) {
  require(number >= Constants.minimumColumnsNumber)
}
