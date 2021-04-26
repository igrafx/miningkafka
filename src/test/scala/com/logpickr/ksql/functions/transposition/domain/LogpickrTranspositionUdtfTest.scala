package com.logpickr.ksql.functions.transposition.domain

import com.logpickr.ksql.functions.transposition.domain.structs.Structs
import core.UnitTestSpec
import org.apache.kafka.connect.data.Struct

import java.util

class LogpickrTranspositionUdtfTest extends UnitTestSpec {
  val logpickrTranspositionUdtf = new LogpickrTranspositionUdtf()
  val struct1: Struct = new Struct(Structs.STRUCT_SCHEMA).put("TASK", "Etape 1").put("TIME", "12/01/2020")
  val struct2: Struct = new Struct(Structs.STRUCT_SCHEMA).put("TASK", "Etape 2").put("TIME", "14/01/2020")
  val struct3: Struct = new Struct(Structs.STRUCT_SCHEMA).put("TASK", "Etape 3").put("TIME", "15/01/2020")
  val struct4: Struct = new Struct(Structs.STRUCT_SCHEMA).put("TASK", "Etape 4").put("TIME", "16/01/2020")
  val struct5: Struct = new Struct(Structs.STRUCT_SCHEMA).put("TASK", "Etape 5").put("TIME", "17/01/2020")
  val structVide: Struct = new Struct(Structs.STRUCT_SCHEMA).put("TASK", "Etape Vide").put("TIME", "")
  val structNull: Struct = new Struct(Structs.STRUCT_SCHEMA).put("TASK", "Etape Null").put("TIME", null)

  describe("filteredSeq") {
    it(
      "should return an empty Seq if the input Seq is either empty or only have empty or null values for the TIME field of the Struct"
    ) {
      assert(Seq.empty == logpickrTranspositionUdtf.filteredSeq(Seq.empty))

      val seqTest1 = Seq(structVide, structNull)
      assert(Seq.empty == logpickrTranspositionUdtf.filteredSeq(seqTest1))
    }
    it("should return a Seq without the elements having a TIME field empty or null") {
      val seqTest1: Seq[Struct] = Seq(struct1, struct2, struct3, struct4, struct5)
      assert(seqTest1 == logpickrTranspositionUdtf.filteredSeq(seqTest1))

      val seqTest2 = Seq(struct1, structVide, struct3, structNull, struct5)
      val seqTest2Expected = Seq(struct1, struct3, struct5)
      assert(seqTest2Expected == logpickrTranspositionUdtf.filteredSeq(seqTest2))
    }
  }

  describe("logpickrTransposition") {
    it(
      "should return an empty Java List if the input List is either empty or only have empty or null values for the TIME field of the Struct"
    ) {
      val emptyList: util.List[Struct] = new util.ArrayList[Struct]()
      assert(emptyList == logpickrTranspositionUdtf.logpickrTransposition(emptyList))
      assert(
        emptyList == logpickrTranspositionUdtf.logpickrTransposition(
          emptyList,
          "dd/MM/yyyy",
          isStartInformation = true,
          isTaskNameAscending = true
        )
      )
      assert(
        emptyList == logpickrTranspositionUdtf.logpickrTransposition(
          emptyList,
          "dd/MM/yyyy",
          isStartInformation = true,
          isTaskNameAscending = false
        )
      )
      assert(
        emptyList == logpickrTranspositionUdtf.logpickrTransposition(
          emptyList,
          "dd/MM/yyyy",
          isStartInformation = false,
          isTaskNameAscending = true
        )
      )
      assert(
        emptyList == logpickrTranspositionUdtf.logpickrTransposition(
          emptyList,
          "dd/MM/yyyy",
          isStartInformation = false,
          isTaskNameAscending = false
        )
      )

      val listTest: util.List[Struct] = new util.ArrayList[Struct]()
      listTest.add(structVide)
      listTest.add(structNull)
      assert(emptyList == logpickrTranspositionUdtf.logpickrTransposition(listTest))
      assert(
        emptyList == logpickrTranspositionUdtf.logpickrTransposition(
          emptyList,
          "dd/MM/yyyy",
          isStartInformation = true,
          isTaskNameAscending = true
        )
      )
      assert(
        emptyList == logpickrTranspositionUdtf.logpickrTransposition(
          emptyList,
          "dd/MM/yyyy",
          isStartInformation = true,
          isTaskNameAscending = false
        )
      )
      assert(
        emptyList == logpickrTranspositionUdtf.logpickrTransposition(
          emptyList,
          "dd/MM/yyyy",
          isStartInformation = false,
          isTaskNameAscending = true
        )
      )
      assert(
        emptyList == logpickrTranspositionUdtf.logpickrTransposition(
          emptyList,
          "dd/MM/yyyy",
          isStartInformation = false,
          isTaskNameAscending = false
        )
      )
    }

    it("should return an empty List if the method with multiple parameters take an input generating an exception") {
      val structIncorrect1: Struct =
        new Struct(Structs.STRUCT_SCHEMA).put("TASK", "Etape incorrecte 1").put("TIME", "test1")
      val structIncorrect2: Struct =
        new Struct(Structs.STRUCT_SCHEMA).put("TASK", "Etape incorrecte 2").put("TIME", "test2")

      val listTest: util.List[Struct] = new util.ArrayList[Struct]()
      listTest.add(struct1)
      listTest.add(structIncorrect1)
      listTest.add(struct3)
      listTest.add(structIncorrect2)
      listTest.add(struct5)
      val emptyList = new util.ArrayList[Struct]()
      assert(
        emptyList == logpickrTranspositionUdtf.logpickrTransposition(
          listTest,
          "dd/MM/yyyy",
          isStartInformation = true,
          isTaskNameAscending = true
        )
      )
      assert(
        emptyList == logpickrTranspositionUdtf.logpickrTransposition(
          listTest,
          "dd/MM/yyyy",
          isStartInformation = true,
          isTaskNameAscending = false
        )
      )
      assert(
        emptyList == logpickrTranspositionUdtf.logpickrTransposition(
          listTest,
          "dd/MM/yyyy",
          isStartInformation = false,
          isTaskNameAscending = true
        )
      )
      assert(
        emptyList == logpickrTranspositionUdtf.logpickrTransposition(
          listTest,
          "dd/MM/yyyy",
          isStartInformation = false,
          isTaskNameAscending = false
        )
      )
    }

    it(
      "should return a Java List without the elements having a TIME field empty or null for the method with only one parameter"
    ) {
      val listTest1: util.List[Struct] = new util.ArrayList[Struct]()
      listTest1.add(struct1)
      listTest1.add(struct2)
      listTest1.add(struct3)
      listTest1.add(struct4)
      listTest1.add(struct5)
      assert(listTest1 == logpickrTranspositionUdtf.logpickrTransposition(listTest1))

      val listTest2: util.List[Struct] = new util.ArrayList[Struct]()
      listTest2.add(struct1)
      listTest2.add(structVide)
      listTest2.add(struct2)
      listTest2.add(structNull)
      listTest2.add(struct5)
      val listTest2Expected: util.List[Struct] = new util.ArrayList[Struct]()
      listTest2Expected.add(struct1)
      listTest2Expected.add(struct2)
      listTest2Expected.add(struct5)
      assert(listTest2Expected == logpickrTranspositionUdtf.logpickrTransposition(listTest2))
    }

    val structInput1: Struct = new Struct(Structs.STRUCT_SCHEMA).put("TASK", "Etape 1").put("TIME", "13/01/2020")
    val structInput2: Struct = new Struct(Structs.STRUCT_SCHEMA).put("TASK", "Etape 2").put("TIME", "14/01/2020")
    val structInput3: Struct = new Struct(Structs.STRUCT_SCHEMA).put("TASK", "Etape 3").put("TIME", "13/01/2020")
    val structInput4: Struct = new Struct(Structs.STRUCT_SCHEMA).put("TASK", "Etape 4").put("TIME", "12/01/2020")
    val structInput5: Struct = new Struct(Structs.STRUCT_SCHEMA).put("TASK", "Etape 5").put("TIME", "13/01/2020")

    def getListTest: util.List[Struct] = {
      val listTest: util.List[Struct] = new util.ArrayList[Struct]()
      listTest.add(structInput1)
      listTest.add(structInput2)
      listTest.add(structInput3)
      listTest.add(structInput4)
      listTest.add(structInput5)
      listTest
    }

    val structResult1Start1: Struct =
      new Struct(Structs.STRUCT_SCHEMA_SPECIAL)
        .put("TASK", "Etape 1")
        .put("START", "13/01/2020")
        .put("STOP", "13/01/2020")
    val structResult1Start2: Struct =
      new Struct(Structs.STRUCT_SCHEMA_SPECIAL)
        .put("TASK", "Etape 1")
        .put("START", "13/01/2020")
        .put("STOP", "14/01/2020")
    val structResult1Stop1: Struct =
      new Struct(Structs.STRUCT_SCHEMA_SPECIAL)
        .put("TASK", "Etape 1")
        .put("START", "12/01/2020")
        .put("STOP", "13/01/2020")
    val structResult1Stop2: Struct =
      new Struct(Structs.STRUCT_SCHEMA_SPECIAL)
        .put("TASK", "Etape 1")
        .put("START", "13/01/2020")
        .put("STOP", "13/01/2020")
    val structResult2Start: Struct =
      new Struct(Structs.STRUCT_SCHEMA_SPECIAL)
        .put("TASK", "Etape 2")
        .put("START", "14/01/2020")
        .put("STOP", "14/01/2020")
    val structResult2Stop: Struct =
      new Struct(Structs.STRUCT_SCHEMA_SPECIAL)
        .put("TASK", "Etape 2")
        .put("START", "13/01/2020")
        .put("STOP", "14/01/2020")
    val structResult3: Struct =
      new Struct(Structs.STRUCT_SCHEMA_SPECIAL)
        .put("TASK", "Etape 3")
        .put("START", "13/01/2020")
        .put("STOP", "13/01/2020")
    val structResult4Start: Struct =
      new Struct(Structs.STRUCT_SCHEMA_SPECIAL)
        .put("TASK", "Etape 4")
        .put("START", "12/01/2020")
        .put("STOP", "13/01/2020")
    val structResult4Stop: Struct =
      new Struct(Structs.STRUCT_SCHEMA_SPECIAL)
        .put("TASK", "Etape 4")
        .put("START", "12/01/2020")
        .put("STOP", "12/01/2020")
    val structResult5Start1: Struct =
      new Struct(Structs.STRUCT_SCHEMA_SPECIAL)
        .put("TASK", "Etape 5")
        .put("START", "13/01/2020")
        .put("STOP", "14/01/2020")
    val structResult5Start2: Struct =
      new Struct(Structs.STRUCT_SCHEMA_SPECIAL)
        .put("TASK", "Etape 5")
        .put("START", "13/01/2020")
        .put("STOP", "13/01/2020")
    val structResult5Stop1: Struct =
      new Struct(Structs.STRUCT_SCHEMA_SPECIAL)
        .put("TASK", "Etape 5")
        .put("START", "13/01/2020")
        .put("STOP", "13/01/2020")
    val structResult5Stop2: Struct =
      new Struct(Structs.STRUCT_SCHEMA_SPECIAL)
        .put("TASK", "Etape 5")
        .put("START", "12/01/2020")
        .put("STOP", "13/01/2020")

    it("should return a Java List with good values when isStartInformation and isTaskNameAscending are both true") {
      val listTest: util.List[Struct] = getListTest
      val listTestExpected: util.List[Struct] = new util.ArrayList[Struct]()
      listTestExpected.add(structResult4Start)
      listTestExpected.add(structResult1Start1)
      listTestExpected.add(structResult3)
      listTestExpected.add(structResult5Start1)
      listTestExpected.add(structResult2Start)
      assert(
        listTestExpected == logpickrTranspositionUdtf.logpickrTransposition(
          listTest,
          "dd/MM/yyyy",
          isStartInformation = true,
          isTaskNameAscending = true
        )
      )
    }

    it(
      "should return a Java List with good values when isStartInformation is true and isTaskNameAscending is false"
    ) {
      val listTest: util.List[Struct] = getListTest
      val listTestExpected: util.List[Struct] = new util.ArrayList[Struct]()
      listTestExpected.add(structResult4Start)
      listTestExpected.add(structResult5Start2)
      listTestExpected.add(structResult3)
      listTestExpected.add(structResult1Start2)
      listTestExpected.add(structResult2Start)
      assert(
        listTestExpected == logpickrTranspositionUdtf.logpickrTransposition(
          listTest,
          "dd/MM/yyyy",
          isStartInformation = true,
          isTaskNameAscending = false
        )
      )
    }

    it(
      "should return a Java List with good values when isStartInformation is false and isTaskNameAscending is true"
    ) {
      val listTest: util.List[Struct] = getListTest
      val listTestExpected: util.List[Struct] = new util.ArrayList[Struct]()
      listTestExpected.add(structResult4Stop)
      listTestExpected.add(structResult1Stop1)
      listTestExpected.add(structResult3)
      listTestExpected.add(structResult5Stop1)
      listTestExpected.add(structResult2Stop)
      assert(
        listTestExpected == logpickrTranspositionUdtf.logpickrTransposition(
          listTest,
          "dd/MM/yyyy",
          isStartInformation = false,
          isTaskNameAscending = true
        )
      )
    }

    it(
      "should return a Java List with good values when isStartInformation and isTaskNameAscending are both false"
    ) {
      val listTest: util.List[Struct] = getListTest
      val listTestExpected: util.List[Struct] = new util.ArrayList[Struct]()
      listTestExpected.add(structResult4Stop)
      listTestExpected.add(structResult5Stop2)
      listTestExpected.add(structResult3)
      listTestExpected.add(structResult1Stop2)
      listTestExpected.add(structResult2Stop)
      assert(
        listTestExpected == logpickrTranspositionUdtf.logpickrTransposition(
          listTest,
          "dd/MM/yyyy",
          isStartInformation = false,
          isTaskNameAscending = false
        )
      )
    }
  }
}
