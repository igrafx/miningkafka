package com.igrafx.kafka.sink.aggregationmain.domain.columnmapping.domain.entities

import com.igrafx.kafka.sink.aggregationmain.domain.entities.Character
import com.igrafx.kafka.sink.aggregationmain.domain.enums.EncodingEnum

final case class FileStructure(
    csvEncoding: EncodingEnum,
    csvSeparator: String,
    csvQuote: String,
    csvEscape: Character,
    csvEndOfLine: NonEmptyString,
    csvHeader: Boolean,
    csvComment: Character,
    fileType: String = "csv"
) {
  require(
    csvEndOfLine.stringValue.nonEmpty
      && csvEscape.character.length == 1
      && csvComment.character.length == 1
  )
}
