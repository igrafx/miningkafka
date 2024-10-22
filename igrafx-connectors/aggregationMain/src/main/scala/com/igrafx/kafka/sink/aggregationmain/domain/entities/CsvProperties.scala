package com.igrafx.kafka.sink.aggregationmain.domain.entities

import com.igrafx.kafka.sink.aggregationmain.domain.enums.EncodingEnum

import java.util.UUID

final case class CsvProperties(
    connectorName: String,
    projectId: UUID,
    csvEncoding: EncodingEnum,
    csvSeparator: String,
    csvQuote: String,
    csvFieldsNumber: ColumnsNumber,
    csvHeader: Boolean,
    csvDefaultTextValue: String,
    csvHeaderValue: Option[String]
) {
  require(
    csvFieldsNumber.number >= 3
      && csvSeparator.length == 1
      && csvQuote.length == 1
  )
}
