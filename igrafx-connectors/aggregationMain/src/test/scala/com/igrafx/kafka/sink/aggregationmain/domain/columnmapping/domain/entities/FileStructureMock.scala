package com.igrafx.kafka.sink.aggregationmain.domain.columnmapping.domain.entities

import com.igrafx.core.Mock
import com.igrafx.kafka.sink.aggregationmain.domain.entities.Character
import com.igrafx.kafka.sink.aggregationmain.domain.entities.mocks.{CharacterMock, NonEmptyStringMock}
import com.igrafx.kafka.sink.aggregationmain.domain.enums.EncodingEnum

final class FileStructureMock extends Mock[FileStructure] {
  private var csvEncoding: EncodingEnum = EncodingEnum.UTF_8
  private var csvSeparator: String = ","
  private var csvQuote: String = "\""
  private var csvEscape: Character = new CharacterMock().setCharacter("\\").build()
  private var csvEndOfLine: NonEmptyString = new NonEmptyStringMock().setStringValue("\\n").build()
  private var csvHeader: Boolean = true
  private var csvComment: Character = new CharacterMock().setCharacter("#").build()
  private var fileType: String = "csv"

  def setCsvEncoding(csvEncoding: EncodingEnum): FileStructureMock = {
    this.csvEncoding = csvEncoding
    this
  }

  def setCsvSeparator(csvSeparator: String): FileStructureMock = {
    this.csvSeparator = csvSeparator
    this
  }

  def setCsvQuote(csvQuote: String): FileStructureMock = {
    this.csvQuote = csvQuote
    this
  }

  def setCsvEscape(csvEscape: Character): FileStructureMock = {
    this.csvEscape = csvEscape
    this
  }

  def setCsvEndOfLine(csvEndOfLine: NonEmptyString): FileStructureMock = {
    this.csvEndOfLine = csvEndOfLine
    this
  }

  def setCsvHeader(csvHeader: Boolean): FileStructureMock = {
    this.csvHeader = csvHeader
    this
  }

  def setCsvComment(csvComment: Character): FileStructureMock = {
    this.csvComment = csvComment
    this
  }

  def setFileType(fileType: String): FileStructureMock = {
    this.fileType = fileType
    this
  }

  override def build(): FileStructure = {
    FileStructure(
      csvEncoding = csvEncoding,
      csvSeparator = csvSeparator,
      csvQuote = csvQuote,
      csvEscape = csvEscape,
      csvEndOfLine = csvEndOfLine,
      csvHeader = csvHeader,
      csvComment = csvComment,
      fileType = fileType
    )
  }
}
