package com.logpickr.ksql.functions.sessions.domain

import com.logpickr.ksql.functions.sessions.Constants
import com.logpickr.ksql.functions.sessions.domain.structs.Structs
import core.UnitTestSpec
import org.apache.commons.codec.digest.DigestUtils
import org.apache.kafka.connect.data.Struct

import java.nio.charset.StandardCharsets
import java.util

class LogpickrSessionsUdtfTest extends UnitTestSpec {
  val logpickrSessionsUdtf = new LogpickrSessionsUdtf()

  val listTest: util.List[String] = new util.ArrayList[String]()

  override def beforeAll(): Unit = {
    //timeStamp;userID;targetApp;eventType
    listTest.add("2020-06-16T04;1;appTest1;Start")
    listTest.add("2020-06-16T04;1;appTest1;testLine1")
    listTest.add("2020-06-16T04;1;appTest1;testLine2")
    listTest.add("2020-06-16T04;2;appTest1;Start")
    listTest.add("2020-06-16T04;2;appTest1;testLine4")
    listTest.add("2020-06-16T04;2;appTest2;Start")
    listTest.add("2020-06-16T04;2;appTest3;testLine5")
    listTest.add("2020-06-16T04;1;appTest1;testLine3")
    listTest.add("2020-06-16T04;1;appTest1;ignoreLine")
    listTest.add("2020-06-16T04;1;appTest1;End")
    listTest.add("2020-06-16T04;1;appTest2;aloneTestLine1")
    listTest.add("2020-06-16T04;1;appTest2;aloneTestLine2")
    listTest.add("2020-06-16T04;2;appTest2;testLine6")
    listTest.add("2020-06-16T04;2;appTest2;End")
    listTest.add("2020-06-16T04;2;appTest2;testLine7")
    listTest.add("2020-06-16T04;2;appTest3;End")
    listTest.add("2020-06-16T04;3;appTest1;Start")
    listTest.add("2020-06-16T04;3;appTest2;testLine8")
    listTest.add("2020-06-16T04;3;appTest2;testLine9")
  }

  describe("logpickrSessions") {

    val start1SessionId = "1appTest1"
    val start2SessionId = "1appTest2"
    val start3SessionId = "2appTest1"
    val start4SessionId = "2appTest2"
    val start5SessionId = "3appTest1"

    val structStart1: Struct =
      new Struct(Structs.STRUCT_OUTPUT_SCHEMA)
        .put(Constants.structSessionId, start1SessionId)
        .put(Constants.structLine, "2020-06-16T04;1;appTest1;Start")
    val structTestLine1: Struct =
      new Struct(Structs.STRUCT_OUTPUT_SCHEMA)
        .put(Constants.structSessionId, start1SessionId)
        .put(Constants.structLine, "2020-06-16T04;1;appTest1;testLine1")
    val structTestLine2: Struct =
      new Struct(Structs.STRUCT_OUTPUT_SCHEMA)
        .put(Constants.structSessionId, start1SessionId)
        .put(Constants.structLine, "2020-06-16T04;1;appTest1;testLine2")
    val structTestLine3: Struct =
      new Struct(Structs.STRUCT_OUTPUT_SCHEMA)
        .put(Constants.structSessionId, start1SessionId)
        .put(Constants.structLine, "2020-06-16T04;1;appTest1;testLine3")
    val structEnd1: Struct =
      new Struct(Structs.STRUCT_OUTPUT_SCHEMA)
        .put(Constants.structSessionId, start1SessionId)
        .put(Constants.structLine, "2020-06-16T04;1;appTest1;End")
    val structAlone1: Struct =
      new Struct(Structs.STRUCT_OUTPUT_SCHEMA)
        .put(Constants.structSessionId, start2SessionId)
        .put(Constants.structLine, "2020-06-16T04;1;appTest2;aloneTestLine1")
    val structAlone2: Struct =
      new Struct(Structs.STRUCT_OUTPUT_SCHEMA)
        .put(Constants.structSessionId, start2SessionId)
        .put(Constants.structLine, "2020-06-16T04;1;appTest2;aloneTestLine2")

    val structStart2: Struct =
      new Struct(Structs.STRUCT_OUTPUT_SCHEMA)
        .put(Constants.structSessionId, start3SessionId)
        .put(Constants.structLine, "2020-06-16T04;2;appTest1;Start")
    val structTestLine4: Struct =
      new Struct(Structs.STRUCT_OUTPUT_SCHEMA)
        .put(Constants.structSessionId, start3SessionId)
        .put(Constants.structLine, "2020-06-16T04;2;appTest1;testLine4")
    val structStart3: Struct =
      new Struct(Structs.STRUCT_OUTPUT_SCHEMA)
        .put(Constants.structSessionId, start4SessionId)
        .put(Constants.structLine, "2020-06-16T04;2;appTest2;Start")
    val structTestLine5: Struct =
      new Struct(Structs.STRUCT_OUTPUT_SCHEMA)
        .put(Constants.structSessionId, start4SessionId)
        .put(Constants.structLine, "2020-06-16T04;2;appTest3;testLine5")
    val structTestLine6: Struct =
      new Struct(Structs.STRUCT_OUTPUT_SCHEMA)
        .put(Constants.structSessionId, start4SessionId)
        .put(Constants.structLine, "2020-06-16T04;2;appTest2;testLine6")
    val structEnd2: Struct =
      new Struct(Structs.STRUCT_OUTPUT_SCHEMA)
        .put(Constants.structSessionId, start4SessionId)
        .put(Constants.structLine, "2020-06-16T04;2;appTest2;End")
    val structTestLine7: Struct =
      new Struct(Structs.STRUCT_OUTPUT_SCHEMA)
        .put(Constants.structSessionId, start4SessionId)
        .put(Constants.structLine, "2020-06-16T04;2;appTest2;testLine7")
    val structEnd3: Struct =
      new Struct(Structs.STRUCT_OUTPUT_SCHEMA)
        .put(Constants.structSessionId, start4SessionId)
        .put(Constants.structLine, "2020-06-16T04;2;appTest3;End")

    val structStart4: Struct =
      new Struct(Structs.STRUCT_OUTPUT_SCHEMA)
        .put(Constants.structSessionId, start5SessionId)
        .put(Constants.structLine, "2020-06-16T04;3;appTest1;Start")
    val structTestLine8: Struct =
      new Struct(Structs.STRUCT_OUTPUT_SCHEMA)
        .put(Constants.structSessionId, start5SessionId)
        .put(Constants.structLine, "2020-06-16T04;3;appTest2;testLine8")
    val structTestLine9: Struct =
      new Struct(Structs.STRUCT_OUTPUT_SCHEMA)
        .put(Constants.structSessionId, start5SessionId)
        .put(Constants.structLine, "2020-06-16T04;3;appTest2;testLine9")

    it("should return a Java List with the good results if isIgnoreIfNoStart is true and isIgnoreIfNoEnd is true") {
      val expectedResult: util.List[Struct] = new util.ArrayList[Struct]()
      expectedResult.add(structStart1)
      expectedResult.add(structTestLine1)
      expectedResult.add(structTestLine2)
      expectedResult.add(structTestLine3)
      expectedResult.add(structEnd1)
      expectedResult.add(structStart3)
      expectedResult.add(structTestLine5)
      expectedResult.add(structTestLine6)
      expectedResult.add(structEnd2)

      val res = logpickrSessionsUdtf.logpickrSessions(
        listTest,
        ".*;.*;.*;ignoreLine",
        ".*;(.*);.*;.*",
        ".*;.*;.*;Start",
        ".*;.*;.*;End",
        ".*;(.*);(.*);.*",
        isSessionIdHash = false,
        isIgnoreIfNoStart = true,
        isIgnoreIfNoEnd = true
      )

      assert(expectedResult == res)
    }

    it("should return a Java List with the good results if isIgnoreIfNoStart is true and isIgnoreIfNoEnd is false") {
      val expectedResult: util.List[Struct] = new util.ArrayList[Struct]()
      expectedResult.add(structStart1)
      expectedResult.add(structTestLine1)
      expectedResult.add(structTestLine2)
      expectedResult.add(structTestLine3)
      expectedResult.add(structEnd1)
      expectedResult.add(structStart2)
      expectedResult.add(structTestLine4)
      expectedResult.add(structStart3)
      expectedResult.add(structTestLine5)
      expectedResult.add(structTestLine6)
      expectedResult.add(structEnd2)
      expectedResult.add(structStart4)
      expectedResult.add(structTestLine8)
      expectedResult.add(structTestLine9)

      val res = logpickrSessionsUdtf.logpickrSessions(
        listTest,
        ".*;.*;.*;ignoreLine",
        ".*;(.*);.*;.*",
        ".*;.*;.*;Start",
        ".*;.*;.*;End",
        ".*;(.*);(.*);.*",
        isSessionIdHash = false,
        isIgnoreIfNoStart = true,
        isIgnoreIfNoEnd = false
      )

      assert(expectedResult == res)
    }

    it("should return a Java List with the good results if isIgnoreIfNoStart is false and isIgnoreIfNoEnd is true") {
      val expectedResult: util.List[Struct] = new util.ArrayList[Struct]()
      expectedResult.add(structStart1)
      expectedResult.add(structTestLine1)
      expectedResult.add(structTestLine2)
      expectedResult.add(structTestLine3)
      expectedResult.add(structEnd1)
      expectedResult.add(structStart3)
      expectedResult.add(structTestLine5)
      expectedResult.add(structTestLine6)
      expectedResult.add(structEnd2)
      expectedResult.add(structTestLine7)
      expectedResult.add(structEnd3)

      val res = logpickrSessionsUdtf.logpickrSessions(
        listTest,
        ".*;.*;.*;ignoreLine",
        ".*;(.*);.*;.*",
        ".*;.*;.*;Start",
        ".*;.*;.*;End",
        ".*;(.*);(.*);.*",
        isSessionIdHash = false,
        isIgnoreIfNoStart = false,
        isIgnoreIfNoEnd = true
      )

      assert(expectedResult == res)
    }

    it("should return a Java List with the good results if isIgnoreIfNoStart is false and isIgnoreIfNoEnd is false") {
      val expectedResult: util.List[Struct] = new util.ArrayList[Struct]()
      expectedResult.add(structStart1)
      expectedResult.add(structTestLine1)
      expectedResult.add(structTestLine2)
      expectedResult.add(structTestLine3)
      expectedResult.add(structEnd1)
      expectedResult.add(structAlone1)
      expectedResult.add(structAlone2)
      expectedResult.add(structStart2)
      expectedResult.add(structTestLine4)
      expectedResult.add(structStart3)
      expectedResult.add(structTestLine5)
      expectedResult.add(structTestLine6)
      expectedResult.add(structEnd2)
      expectedResult.add(structTestLine7)
      expectedResult.add(structEnd3)
      expectedResult.add(structStart4)
      expectedResult.add(structTestLine8)
      expectedResult.add(structTestLine9)

      val res = logpickrSessionsUdtf.logpickrSessions(
        listTest,
        ".*;.*;.*;ignoreLine",
        ".*;(.*);.*;.*",
        ".*;.*;.*;Start",
        ".*;.*;.*;End",
        ".*;(.*);(.*);.*",
        isSessionIdHash = false,
        isIgnoreIfNoStart = false,
        isIgnoreIfNoEnd = false
      )

      assert(expectedResult == res)
    }

    it("should make the sessions within a group even if there are two groups with the same sessionId") {
      val listTest2: util.List[String] = new util.ArrayList[String]()
      //timeStamp;userID;targetApp;eventType
      listTest2.add("2020-06-16T04;1;appTest;CommonStart")
      listTest2.add("2020-06-16T04;2;appTest;CommonStart")
      listTest2.add("2020-06-16T04;1;appTest;group1Line1")
      listTest2.add("2020-06-16T04;3;appTest;CommonStart")
      listTest2.add("2020-06-16T04;2;appTest;group2Line1")
      listTest2.add("2020-06-16T04;2;appTest;group2Line2")
      listTest2.add("2020-06-16T04;3;appTest;group3Line1")
      listTest2.add("2020-06-16T04;3;appTest;group3Line2")
      listTest2.add("2020-06-16T04;1;appTest;group1Line2")
      listTest2.add("2020-06-16T04;3;appTest;CommonEnd")
      listTest2.add("2020-06-16T04;1;appTest;CommonEnd")
      listTest2.add("2020-06-16T04;2;appTest;CommonEnd")

      val res = logpickrSessionsUdtf.logpickrSessions(
        listTest2,
        ".*;.*;.*;ignoreLine",
        ".*;(.*);.*;.*",
        ".*;.*;.*;CommonStart",
        ".*;.*;.*;CommonEnd",
        ".*;.*;(.*);.*",
        isSessionIdHash = false,
        isIgnoreIfNoStart = false,
        isIgnoreIfNoEnd = false
      )

      val expectedResult: util.List[Struct] = new util.ArrayList[Struct]()
      expectedResult.add(
        new Struct(Structs.STRUCT_OUTPUT_SCHEMA)
          .put(Constants.structSessionId, "appTest")
          .put(Constants.structLine, "2020-06-16T04;1;appTest;CommonStart")
      )
      expectedResult.add(
        new Struct(Structs.STRUCT_OUTPUT_SCHEMA)
          .put(Constants.structSessionId, "appTest")
          .put(Constants.structLine, "2020-06-16T04;1;appTest;group1Line1")
      )
      expectedResult.add(
        new Struct(Structs.STRUCT_OUTPUT_SCHEMA)
          .put(Constants.structSessionId, "appTest")
          .put(Constants.structLine, "2020-06-16T04;1;appTest;group1Line2")
      )
      expectedResult.add(
        new Struct(Structs.STRUCT_OUTPUT_SCHEMA)
          .put(Constants.structSessionId, "appTest")
          .put(Constants.structLine, "2020-06-16T04;1;appTest;CommonEnd")
      )
      expectedResult.add(
        new Struct(Structs.STRUCT_OUTPUT_SCHEMA)
          .put(Constants.structSessionId, "appTest")
          .put(Constants.structLine, "2020-06-16T04;2;appTest;CommonStart")
      )
      expectedResult.add(
        new Struct(Structs.STRUCT_OUTPUT_SCHEMA)
          .put(Constants.structSessionId, "appTest")
          .put(Constants.structLine, "2020-06-16T04;2;appTest;group2Line1")
      )
      expectedResult.add(
        new Struct(Structs.STRUCT_OUTPUT_SCHEMA)
          .put(Constants.structSessionId, "appTest")
          .put(Constants.structLine, "2020-06-16T04;2;appTest;group2Line2")
      )
      expectedResult.add(
        new Struct(Structs.STRUCT_OUTPUT_SCHEMA)
          .put(Constants.structSessionId, "appTest")
          .put(Constants.structLine, "2020-06-16T04;2;appTest;CommonEnd")
      )
      expectedResult.add(
        new Struct(Structs.STRUCT_OUTPUT_SCHEMA)
          .put(Constants.structSessionId, "appTest")
          .put(Constants.structLine, "2020-06-16T04;3;appTest;CommonStart")
      )
      expectedResult.add(
        new Struct(Structs.STRUCT_OUTPUT_SCHEMA)
          .put(Constants.structSessionId, "appTest")
          .put(Constants.structLine, "2020-06-16T04;3;appTest;group3Line1")
      )
      expectedResult.add(
        new Struct(Structs.STRUCT_OUTPUT_SCHEMA)
          .put(Constants.structSessionId, "appTest")
          .put(Constants.structLine, "2020-06-16T04;3;appTest;group3Line2")
      )
      expectedResult.add(
        new Struct(Structs.STRUCT_OUTPUT_SCHEMA)
          .put(Constants.structSessionId, "appTest")
          .put(Constants.structLine, "2020-06-16T04;3;appTest;CommonEnd")
      )

      assert(expectedResult == res)
    }

    it("should create a new session when a line verifies both the startSessionPattern and endSessionPattern") {
      val listTest2: util.List[String] = new util.ArrayList[String]()
      //timeStamp;userID;targetApp;eventType
      listTest2.add("2020-06-16T04;1;appTest1;CommonStartEnd")
      listTest2.add("2020-06-16T04;1;appTest1;testLine1")
      listTest2.add("2020-06-16T04;1;appTest2;CommonStartEnd")
      listTest2.add("2020-06-16T04;1;appTest2;testLine2")
      listTest2.add("2020-06-16T04;1;appTest3;CommonStartEnd")
      listTest2.add("2020-06-16T04;1;appTest3;testLine3")

      val res = logpickrSessionsUdtf.logpickrSessions(
        listTest2,
        ".*;.*;.*;ignoreLine",
        ".*;(.*);.*;.*",
        ".*;.*;.*;CommonStartEnd",
        ".*;.*;.*;CommonStartEnd",
        ".*;.*;(.*);.*",
        isSessionIdHash = false,
        isIgnoreIfNoStart = true,
        isIgnoreIfNoEnd = true
      )

      val expectedResult: util.List[Struct] = new util.ArrayList[Struct]()
      expectedResult.add(
        new Struct(Structs.STRUCT_OUTPUT_SCHEMA)
          .put(Constants.structSessionId, "appTest1")
          .put(Constants.structLine, "2020-06-16T04;1;appTest1;CommonStartEnd")
      )
      expectedResult.add(
        new Struct(Structs.STRUCT_OUTPUT_SCHEMA)
          .put(Constants.structSessionId, "appTest1")
          .put(Constants.structLine, "2020-06-16T04;1;appTest1;testLine1")
      )
      expectedResult.add(
        new Struct(Structs.STRUCT_OUTPUT_SCHEMA)
          .put(Constants.structSessionId, "appTest2")
          .put(Constants.structLine, "2020-06-16T04;1;appTest2;CommonStartEnd")
      )
      expectedResult.add(
        new Struct(Structs.STRUCT_OUTPUT_SCHEMA)
          .put(Constants.structSessionId, "appTest2")
          .put(Constants.structLine, "2020-06-16T04;1;appTest2;testLine2")
      )
      expectedResult.add(
        new Struct(Structs.STRUCT_OUTPUT_SCHEMA)
          .put(Constants.structSessionId, "appTest3")
          .put(Constants.structLine, "2020-06-16T04;1;appTest3;CommonStartEnd")
      )
      expectedResult.add(
        new Struct(Structs.STRUCT_OUTPUT_SCHEMA)
          .put(Constants.structSessionId, "appTest3")
          .put(Constants.structLine, "2020-06-16T04;1;appTest3;testLine3")
      )

      assert(expectedResult == res)
    }
  }

  describe("getFilteredLines") {
    it("should filter the lines and remove those matching ignorePattern") {
      val inputSeq =
        Seq("2020-06-16T04;1;appTest1;Start", "2020-06-16T04;1;appTest1;ignoreLine", "2020-06-16T04;1;appTest1;End")
      val expectedResultSeq = Seq("2020-06-16T04;1;appTest1;Start", "2020-06-16T04;1;appTest1;End")
      assert(expectedResultSeq == logpickrSessionsUdtf.getFilteredLines(inputSeq, ".*;.*;.*;ignoreLine"))
    }
  }

  describe("getSessionLineStruct") {
    it(s"should create a Struct ${Structs.STRUCT_OUTPUT_SCHEMA_DESCRIPTOR} from a sessionId and a line") {
      val expectedResult: Struct =
        new Struct(Structs.STRUCT_OUTPUT_SCHEMA)
          .put(Constants.structSessionId, "sessionIdTest")
          .put(Constants.structLine, "sessionLineTest")

      assert(expectedResult == logpickrSessionsUdtf.getSessionLineStruct("sessionIdTest", "sessionLineTest"))
    }
  }

  describe("getGroup") {
    it(
      "should return a correct group corresponding to the concatenation of the groupSessionPattern important columns"
    ) {
      val res = logpickrSessionsUdtf.getGroup(
        "2020-06-16T04;1;appTest1;Start",
        ".*;(.*);(.*);.*".r
      )
      res match {
        case Some(res) => assert("1appTest1" == res)
        case None => assert(false)
      }
    }

    it(
      "should return a correct group even if there is only one important column in the groupSessionPattern (no concatenation)"
    ) {
      val res = logpickrSessionsUdtf.getGroup(
        "2020-06-16T04;1;appTest1;Start",
        ".*;(.*);.*;.*".r
      )
      res match {
        case Some(res) => assert("1" == res)
        case None => assert(false)
      }
    }

    it("should return None in case of incoherence between the groupSessionPattern and the session line") {
      val res = logpickrSessionsUdtf.getGroup(
        "2020-06-16T04;1;appTest1;Start",
        ".*;.*;.*;.*;(.*)".r
      )
      res match {
        case Some(_) => assert(false)
        case None => assert(true)
      }

      val res2 = logpickrSessionsUdtf.getGroup(
        "2020-06-16T04;1;appTest1;Start",
        ".*;.*;.*;.*".r
      )
      res2 match {
        case Some(_) => assert(false)
        case None => assert(true)
      }

      val res3 = logpickrSessionsUdtf.getGroup(
        "2020-06-16T04;1;appTest1;Start",
        "".r
      )
      res3 match {
        case Some(_) => assert(false)
        case None => assert(true)
      }

      val res4 = logpickrSessionsUdtf.getGroup(
        "2020-06-16T04;;appTest1;Start",
        ".*;(.+);.*;.*".r
      )
      res4 match {
        case Some(_) => assert(false)
        case None => assert(true)
      }
    }
  }

  describe("getSessionId") {
    it(
      "should return a correct sessionId corresponding to the concatenation of the sessionIdPattern important columns"
    ) {
      val res = logpickrSessionsUdtf.getSessionId(
        "2020-06-16T04;1;appTest1;Start",
        ".*;(.*);(.*);.*".r,
        isSessionIdHash = false
      )
      assert("1appTest1" == res)
    }

    it(
      "should return a correct sessionId corresponding to the hash of the concatenation of the sessionIdPattern important columns"
    ) {
      val res = logpickrSessionsUdtf.getSessionId(
        "2020-06-16T04;1;appTest1;Start",
        ".*;(.*);(.*);.*".r,
        isSessionIdHash = true
      )
      assert(DigestUtils.md5Hex("1appTest1".getBytes(StandardCharsets.UTF_8)) == res)
    }

    it(
      "should return a correct sessionId even if there is only one important column in the sessionIdPattern (no concatenation)"
    ) {
      val res = logpickrSessionsUdtf.getSessionId(
        "2020-06-16T04;1;appTest1;Start",
        ".*;.*;(.*);.*".r,
        isSessionIdHash = false
      )
      assert("appTest1" == res)

      val res2 =
        logpickrSessionsUdtf.getSessionId("2020-06-16T04;1;appTest1;Start", ".*;.*;(.*);.*".r, isSessionIdHash = true)
      assert(DigestUtils.md5Hex("appTest1".getBytes(StandardCharsets.UTF_8)) == res2)
    }

    it(
      "should return an empty String in case of incoherence between the sessionIdPattern and the session line"
    ) {
      assert(
        "" == logpickrSessionsUdtf.getSessionId(
          "2020-06-16T04;1;appTest1;Start",
          ".*;.*;.*;.*;(.*)".r,
          isSessionIdHash = true
        )
      )

      assert(
        "" == logpickrSessionsUdtf.getSessionId(
          "2020-06-16T04;1;appTest1;Start",
          ".*;.*;.*;.*".r,
          isSessionIdHash = true
        )
      )

      assert(
        "" == logpickrSessionsUdtf.getSessionId(
          "2020-06-16T04;1;appTest1;Start",
          "".r,
          isSessionIdHash = true
        )
      )

      assert(
        "" == logpickrSessionsUdtf.getSessionId(
          "2020-06-16T04;1;;Start",
          ".*;.*;(.+);.*".r,
          isSessionIdHash = true
        )
      )
    }
  }
}
