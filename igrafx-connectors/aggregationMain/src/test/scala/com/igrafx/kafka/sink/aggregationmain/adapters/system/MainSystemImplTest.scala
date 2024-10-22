package com.igrafx.kafka.sink.aggregationmain.adapters.system

import com.igrafx.kafka.sink.aggregationmain.domain.entities.mocks.{ColumnsNumberMock, CsvPropertiesMock, ParamMock}
import com.igrafx.kafka.sink.aggregationmain.domain.entities.{CsvProperties, Event}
import core.UnitTestSpec

class MainSystemImplTest extends UnitTestSpec {
  val csvProperties: CsvProperties = new CsvPropertiesMock().build()
  val mainSystem = new MainSystemImpl()

  describe("parsedForIGrafx") {
    it("should work with non quoted params") {
      val paramsList = List(
        Event(event =
          List(
            new ParamMock().setColumnId(0).setText("premier").setQuote(false).build(),
            new ParamMock().setColumnId(1).setText("second").setQuote(false).build(),
            new ParamMock().setColumnId(2).setText("troisieme").setQuote(false).build(),
            new ParamMock().setColumnId(3).setText("quatrieme").setQuote(false).build()
          )
        )
      )
      val result = mainSystem.parsedForIGrafx(paramsList.head, csvProperties)
      assert(
        result == s"premier${csvProperties.csvSeparator}second${csvProperties.csvSeparator}troisieme${csvProperties.csvSeparator}quatrieme"
      )
    }

    it("should work with quoted params") {
      val paramsList = List(
        Event(event =
          List(
            new ParamMock().setColumnId(0).setText("premier").setQuote(true).build(),
            new ParamMock().setColumnId(1).setText("second").setQuote(true).build(),
            new ParamMock().setColumnId(2).setText("troisieme").setQuote(true).build(),
            new ParamMock().setColumnId(3).setText("quatrieme").setQuote(true).build()
          )
        )
      )
      val result = mainSystem.parsedForIGrafx(paramsList.head, csvProperties)
      assert(
        result == s"${csvProperties.csvQuote}premier${csvProperties.csvQuote}${csvProperties.csvSeparator}${csvProperties.csvQuote}second${csvProperties.csvQuote}${csvProperties.csvSeparator}${csvProperties.csvQuote}troisieme${csvProperties.csvQuote}${csvProperties.csvSeparator}${csvProperties.csvQuote}quatrieme${csvProperties.csvQuote}"
      )
    }

    it("should sort params according to their columnId") {
      val paramsList = List(
        Event(
          event = List(
            new ParamMock().setColumnId(1).setText("second").setQuote(false).build(),
            new ParamMock().setColumnId(3).setText("quatrieme").setQuote(false).build(),
            new ParamMock().setColumnId(2).setText("troisieme").setQuote(false).build(),
            new ParamMock().setColumnId(0).setText("premier").setQuote(false).build()
          )
        )
      )
      val result = mainSystem.parsedForIGrafx(paramsList.head, csvProperties)
      assert(
        result == s"premier${csvProperties.csvSeparator}second${csvProperties.csvSeparator}troisieme${csvProperties.csvSeparator}quatrieme"
      )
    }

    it("should parse the json with missing columns in the middle correctly") {
      val paramsList = List(
        Event(event =
          List(
            new ParamMock().setColumnId(0).setText("premier").setQuote(false).build(),
            new ParamMock().setColumnId(1).setText("second").setQuote(false).build(),
            new ParamMock().setColumnId(4).setText("cinquieme").setQuote(false).build()
          )
        )
      )
      val csvProperties =
        new CsvPropertiesMock().setCsvFieldsNumber(new ColumnsNumberMock().setNumber(5).build()).build()
      val result = mainSystem.parsedForIGrafx(paramsList.head, csvProperties)
      println(result)
      assert(
        result == s"premier${csvProperties.csvSeparator}second${csvProperties.csvSeparator}${csvProperties.csvDefaultTextValue}${csvProperties.csvSeparator}${csvProperties.csvDefaultTextValue}${csvProperties.csvSeparator}cinquieme"
      )
    }

    it("should parse the json with missing columns at the beginning correctly") {
      val paramsList = List(
        Event(
          event = List(
            new ParamMock().setColumnId(3).setText("testing").setQuote(true).build()
          )
        )
      )
      val result = mainSystem.parsedForIGrafx(paramsList.head, csvProperties)
      println(result)
      assert(
        result == s"${csvProperties.csvDefaultTextValue}${csvProperties.csvSeparator}${csvProperties.csvDefaultTextValue}${csvProperties.csvSeparator}${csvProperties.csvDefaultTextValue}${csvProperties.csvSeparator}${csvProperties.csvQuote}testing${csvProperties.csvQuote}"
      )
    }

    it("should parse the json with missing columns at the end correctly") {
      val paramsList = List(
        Event(event =
          List(
            new ParamMock().setColumnId(0).setText("premier").setQuote(false).build(),
            new ParamMock().setColumnId(1).setText("second").setQuote(false).build()
          )
        )
      )
      val result = mainSystem.parsedForIGrafx(paramsList.head, csvProperties)
      println(result)
      assert(
        result == s"premier${csvProperties.csvSeparator}second${csvProperties.csvSeparator}${csvProperties.csvDefaultTextValue}${csvProperties.csvSeparator}${csvProperties.csvDefaultTextValue}"
      )
    }

    it("should throw an IllegalArgumentException if the number of columns is the json is too high") {
      val paramsList = List(
        Event(event =
          List(
            new ParamMock().setColumnId(0).setText("premier").setQuote(false).build(),
            new ParamMock().setColumnId(4).setText("cinquieme").setQuote(false).build()
          )
        )
      )
      assertThrows[IllegalArgumentException] {
        mainSystem.parsedForIGrafx(paramsList.head, csvProperties)
      }
    }
  }

  describe("defaultTextValuesRec") {
    it(
      "should return a number of default values equal to the number argument, each default value having the csv separator before her"
    ) {
      val defaultValues = mainSystem.defaultTextValuesRec("", 3, csvProperties)
      assert(
        defaultValues == s"${csvProperties.csvSeparator}${csvProperties.csvDefaultTextValue}${csvProperties.csvSeparator}${csvProperties.csvDefaultTextValue}${csvProperties.csvSeparator}${csvProperties.csvDefaultTextValue}"
      )
    }
    it("should throw an exception if the number argument is < 0") {
      assertThrows[IllegalArgumentException] {
        mainSystem.defaultTextValuesRec("", -1, csvProperties)
      }
    }
  }
}
