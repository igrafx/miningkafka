package com.igrafx.kafka.sink.aggregationmain.domain.columnmapping.domain.entities

import com.igrafx.kafka.sink.aggregationmain.Constants
import com.igrafx.kafka.sink.aggregationmain.domain.entities.ColumnsNumber
import com.igrafx.kafka.sink.aggregationmain.domain.exceptions.{
  DefaultIndexException,
  IncoherentIndexException,
  NegativeIndexException,
  ParsableIndexException
}
import core.UnitTestSpec

final class IndexTest extends UnitTestSpec {
  describe("Index.apply(index: Int, columnsNumber, isOnlyIndexProperty)") {
    it("should return an Index if index is valid and isOnlyIndexProperty is false") {
      val index = 1
      val columnsNumber = ColumnsNumber(3)
      val isOnlyIndexProperty = false

      val res = Index(index, columnsNumber, isOnlyIndexProperty)

      assert(res.index == index)
    }
    it("should return an Index if index is valid and isOnlyIndexProperty is true") {
      val index = 1
      val columnsNumber = ColumnsNumber(3)
      val isOnlyIndexProperty = true

      val res = Index(index, columnsNumber, isOnlyIndexProperty)

      assert(res.index == index)
    }
    it("should throw a DefaultIndexException if isOnlyIndexProperty is true and index has default value") {
      val index = Constants.columnIndexDefaultValue
      val columnsNumber = ColumnsNumber(3)
      val isOnlyIndexProperty = true

      assertThrows[DefaultIndexException](Index(index, columnsNumber, isOnlyIndexProperty))
    }
    it("should throw an NegativeIndexException if index value is inferior than 0") {
      val index = -2
      val columnsNumber = ColumnsNumber(3)
      val isOnlyIndexProperty = false

      assertThrows[NegativeIndexException](Index(index, columnsNumber, isOnlyIndexProperty))
    }
    it("should throw an IncoherentIndexException if index value is greater than or equal to columnsNumber") {
      val index = 3
      val columnsNumber = ColumnsNumber(3)
      val isOnlyIndexProperty = false

      assertThrows[IncoherentIndexException](Index(index, columnsNumber, isOnlyIndexProperty))
    }
  }

  describe("Index.apply(indexAsString: String, columnsNumber, isOnlyIndexProperty)") {
    it("should return an Index if index is valid and isOnlyIndexProperty is false") {
      val index = 1
      val indexAsString = index.toString
      val columnsNumber = ColumnsNumber(3)
      val isOnlyIndexProperty = false

      val res = Index(indexAsString, columnsNumber, isOnlyIndexProperty)

      assert(res.index == index)
    }
    it("should return an Index if index is valid and isOnlyIndexProperty is true") {
      val index = 1
      val indexAsString = index.toString
      val columnsNumber = ColumnsNumber(3)
      val isOnlyIndexProperty = true

      val res = Index(indexAsString, columnsNumber, isOnlyIndexProperty)

      assert(res.index == index)
    }
    it("should throw a ParsableIndexException if indexAsString is not parsable to Int") {
      val indexAsString = "test"
      val columnsNumber = ColumnsNumber(3)
      val isOnlyIndexProperty = true

      assertThrows[ParsableIndexException](Index(indexAsString, columnsNumber, isOnlyIndexProperty))
    }
  }
}
