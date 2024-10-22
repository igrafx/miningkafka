package com.igrafx.kafka.sink.aggregationmain.domain.columnmapping.controllers.dtos

import com.igrafx.kafka.sink.aggregationmain.domain.columnmapping.domain.entities.{FileStructureMock, NonEmptyString}
import com.igrafx.kafka.sink.aggregationmain.domain.entities.Character
import com.igrafx.kafka.sink.aggregationmain.domain.entities.mocks.{CharacterMock, NonEmptyStringMock}
import com.igrafx.kafka.sink.aggregationmain.domain.enums.EncodingEnum
import core.UnitTestSpec

final class FileStructureDtoTest extends UnitTestSpec {
  describe("toEntity") {
    it("should return a FileStructure") {
      val csvEncoding: EncodingEnum = EncodingEnum.UTF_8
      val csvSeparator: String = ","
      val csvQuote: String = "\""
      val csvEscape: Character = new CharacterMock().setCharacter("\\").build()
      val csvEndOfLine: NonEmptyString = new NonEmptyStringMock().setStringValue("\\n").build()
      val csvHeader: Boolean = true
      val csvComment: Character = new CharacterMock().setCharacter("#").build()

      val fileStructureDto =
        FileStructureDto(
          csvEncoding = csvEncoding,
          csvSeparator = csvSeparator,
          csvQuote = csvQuote,
          csvEscape = csvEscape,
          csvEndOfLine = csvEndOfLine,
          csvHeader = csvHeader,
          csvComment = csvComment
        )

      val expectedResult = new FileStructureMock()
        .setCsvEncoding(csvEncoding)
        .setCsvSeparator(csvSeparator)
        .setCsvQuote(csvQuote)
        .setCsvEscape(csvEscape)
        .setCsvEndOfLine(csvEndOfLine)
        .setCsvHeader(csvHeader)
        .setCsvComment(csvComment)
        .build()

      val res = fileStructureDto.toEntity

      assert(res == expectedResult)
    }
  }
}
