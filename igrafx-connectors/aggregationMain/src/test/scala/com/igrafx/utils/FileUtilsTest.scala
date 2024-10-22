package com.igrafx.utils

import core.UnitTestSpec

final class FileUtilsTest extends UnitTestSpec {

  describe("sanitizeFileName") {
    it("should return IllegalArgumentException if null filename") {
      val fileName = null
      FileUtils.sanitizeFileName(fileName) match {
        case Left(_) => succeed
        case Right(_) => fail()
      }
    }
    it("should return IllegalArgumentException if not null filename with null byte") {
      val fileName = new String(Array[Byte](95, 0, 105))
      FileUtils.sanitizeFileName(new String(fileName)) match {
        case Left(_) => succeed
        case Right(_) => fail()
      }
    }
    it("should return string corrected if string with separators") {
      val fileName = "toto/bidule\\../machin.csv"
      FileUtils.sanitizeFileName(new String(fileName)) match {
        case Left(_) => fail()
        case Right(value) => assert(value == "machin.csv")
      }
    }
  }
}
