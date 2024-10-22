package com.igrafx.kafka.sink.aggregationmain.domain.columnmapping.controllers.dtos

import com.igrafx.kafka.sink.aggregationmain.Constants
import com.igrafx.kafka.sink.aggregationmain.domain.columnmapping.domain.entities.{IndexMock, ValidTimeColumnMock}
import com.igrafx.kafka.sink.aggregationmain.domain.entities.ColumnsNumber
import com.igrafx.kafka.sink.aggregationmain.domain.exceptions.{IndexException, InvalidPropertyValueException}
import core.UnitTestSpec
import org.mockito.ArgumentMatchers
import org.mockito.Mockito.{times, verify}
import org.slf4j.Logger

final class TimeColumnDtoTest extends UnitTestSpec {
  describe("toEntity") {
    it("should return a ValidTimeColumn") {
      val columnIndex = "1"
      val format = "dd/MM/yy HH:mm"
      val timeColumnDto = TimeColumnDto(columnIndex = columnIndex, format = format)
      val columnsNumber = ColumnsNumber(3)
      implicit val log: Logger = mock[Logger]

      val expectedResult =
        new ValidTimeColumnMock()
          .setColumnIndex(new IndexMock().setIndex(columnIndex.toInt).build())
          .setFormat(format)
          .build()

      val res = timeColumnDto.toEntity(columnsNumber)

      assert(res == expectedResult)
    }
    it("should throw an InvalidPropertyValueException if IndexException is received") {
      val format = "dd/MM/yy HH:mm"
      val timeColumnDto = TimeColumnDto(columnIndex = "5", format = format)
      val columnsNumber = ColumnsNumber(3)
      implicit val log: Logger = mock[Logger]

      assertThrows[InvalidPropertyValueException] {
        timeColumnDto.toEntity(columnsNumber)
      }.map { assert =>
        verify(log, times(1))
          .error(ArgumentMatchers.any[String], ArgumentMatchers.any[IndexException])

        assert
      }
    }
  }

  describe("apply") {
    it("should create a TimeColumnDto from the string representation of the column") {
      val columnIndex = "2"
      val columnDateFormat = "dd/MM/yy HH:mm"
      val stringRepresentation =
        s"${Constants.characterForElementStartInPropertyValue}$columnIndex${Constants.delimiterForElementInPropertyValue}$columnDateFormat${Constants.characterForElementEndInPropertyValue}"

      val expectedResult = new TimeColumnDto(columnIndex = columnIndex, format = columnDateFormat)

      val res = TimeColumnDto.apply(stringRepresentation)

      assert(res == expectedResult)
    }
    it(
      s"should throw an InvalidPropertyValueException if the bounds of the string representation do not correspond to '${Constants.characterForElementStartInPropertyValue}' or '${Constants.characterForElementEndInPropertyValue}'"
    ) {
      val columnIndex = "2"
      val columnDateFormat = "dd/MM/yy HH:mm"
      val stringRepresentation1 =
        s"${Constants.characterForElementStartInPropertyValue}$columnIndex${Constants.delimiterForElementInPropertyValue}$columnDateFormat"
      val stringRepresentation2 =
        s"$columnIndex${Constants.delimiterForElementInPropertyValue}$columnDateFormat${Constants.characterForElementEndInPropertyValue}"

      assertThrows[InvalidPropertyValueException](TimeColumnDto.apply(stringRepresentation1))
      assertThrows[InvalidPropertyValueException](TimeColumnDto.apply(stringRepresentation2))
    }
    it(
      "should throw an InvalidPropertyValueException if the string representation has a wrong delimiter or a wrong number of information"
    ) {
      val columnIndex = "2"
      val columnDateFormat = "dd/MM/yy HH:mm"
      val stringRepresentation1 =
        s"${Constants.characterForElementStartInPropertyValue}$columnIndex|$columnDateFormat${Constants.characterForElementEndInPropertyValue}"
      val stringRepresentation2 =
        s"${Constants.characterForElementStartInPropertyValue}$columnIndex${Constants.delimiterForElementInPropertyValue}$columnDateFormat${Constants.delimiterForElementInPropertyValue}test${Constants.characterForElementEndInPropertyValue}"
      val stringRepresentation3 =
        s"${Constants.characterForElementStartInPropertyValue}$columnIndex${Constants.characterForElementEndInPropertyValue}"
      val stringRepresentation4 =
        s"${Constants.characterForElementStartInPropertyValue}$columnIndex${Constants.delimiterForElementInPropertyValue}${Constants.characterForElementEndInPropertyValue}"

      assertThrows[InvalidPropertyValueException](TimeColumnDto.apply(stringRepresentation1))
      assertThrows[InvalidPropertyValueException](TimeColumnDto.apply(stringRepresentation2))
      assertThrows[InvalidPropertyValueException](TimeColumnDto.apply(stringRepresentation3))
      assertThrows[InvalidPropertyValueException](TimeColumnDto.apply(stringRepresentation4))
    }
  }
}
