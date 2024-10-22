package com.igrafx.kafka.sink.aggregationmain.domain.columnmapping.controllers.dtos

import com.igrafx.kafka.sink.aggregationmain.domain.columnmapping.domain.entities.{FileStructure, NonEmptyString}
import com.igrafx.kafka.sink.aggregationmain.domain.entities.Character
import com.igrafx.kafka.sink.aggregationmain.domain.enums.EncodingEnum

final case class FileStructureDto(
    csvEncoding: EncodingEnum,
    csvSeparator: String,
    csvQuote: String,
    csvEscape: Character,
    csvEndOfLine: NonEmptyString,
    csvHeader: Boolean,
    csvComment: Character
) {
  def toEntity: FileStructure = {
    FileStructure(
      csvEncoding = csvEncoding,
      csvSeparator = csvSeparator,
      csvQuote = csvQuote,
      csvEscape = csvEscape,
      csvEndOfLine = csvEndOfLine,
      csvHeader = csvHeader,
      csvComment = csvComment
    )
  }
}
