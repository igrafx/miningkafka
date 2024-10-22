package com.igrafx.kafka.sink.aggregationmain.domain.columnmapping.domain.entities

import com.igrafx.kafka.sink.aggregationmain.domain.entities.mocks.{CharacterMock, NonEmptyStringMock}
import core.UnitTestSpec

final class FileStructureTest extends UnitTestSpec {
  describe("FileStructure") {
    it("should throw an exception if csvEndOfLine is empty") {
      assertThrows[IllegalArgumentException] {
        new FileStructureMock().setCsvEndOfLine(new NonEmptyStringMock().setStringValue("").build()).build()
      }
    }
    it("should throw an exception if csvEscape.length != 1") {
      assertThrows[IllegalArgumentException] {
        new FileStructureMock().setCsvEscape(new CharacterMock().setCharacter("").build()).build()
      }
      assertThrows[IllegalArgumentException] {
        new FileStructureMock().setCsvEscape(new CharacterMock().setCharacter("\\\\").build()).build()
      }
    }
    it("should throw an exception if csvComment.length != 1") {
      assertThrows[IllegalArgumentException] {
        new FileStructureMock().setCsvComment(new CharacterMock().setCharacter("").build()).build()
      }
      assertThrows[IllegalArgumentException] {
        new FileStructureMock().setCsvComment(new CharacterMock().setCharacter("##").build()).build()
      }
    }
  }
}
