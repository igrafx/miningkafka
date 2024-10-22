package com.igrafx.kafka.sink.aggregationmain.domain.columnmapping.controllers.dtos

import com.igrafx.kafka.sink.aggregationmain.Constants
import com.igrafx.kafka.sink.aggregationmain.domain.enums.ConnectorPropertiesEnum
import com.igrafx.kafka.sink.aggregationmain.domain.exceptions.InvalidPropertyValueException
import core.UnitTestSpec
import org.apache.kafka.common.config.{AbstractConfig, ConfigValue}
import org.mockito.Mockito.{doAnswer, times, verify}

final class TimeColumnsDtoTest extends UnitTestSpec {
  describe("fromConnectorConfig") {
    it("should return a Set[TimeColumnDto]") {
      val connectorConfigMock = mock[AbstractConfig]

      val timeColumn1Index = "2"
      val timeColumn1Format = "dd/MM/yy HH:mm"
      val timeColumn2Index = "3"
      val timeColumn2Format = "dd/MM/yy HH:mm"
      val timePropertyValue =
        s"${Constants.characterForElementStartInPropertyValue}$timeColumn1Index${Constants.delimiterForElementInPropertyValue}$timeColumn1Format${Constants.characterForElementEndInPropertyValue}${Constants.delimiterForPropertiesWithListValue}${Constants.characterForElementStartInPropertyValue}$timeColumn2Index${Constants.delimiterForElementInPropertyValue}$timeColumn2Format${Constants.characterForElementEndInPropertyValue}"
      doAnswer(_ => timePropertyValue)
        .when(connectorConfigMock)
        .getString(ConnectorPropertiesEnum.columnMappingTimeColumnsProperty.toStringDescription)

      val expectedResult =
        Set(TimeColumnDto(timeColumn1Index, timeColumn1Format), TimeColumnDto(timeColumn2Index, timeColumn2Format))

      val res = TimeColumnsDto.fromConnectorConfig(connectorConfigMock)

      verify(connectorConfigMock, times(1)).getString(
        ConnectorPropertiesEnum.columnMappingTimeColumnsProperty.toStringDescription
      )

      assert(res == expectedResult)
    }
  }

  describe("fromTimeConfig") {
    it("should return a Set[TimeColumnDto]") {
      val columnMappingTimeCfgMock = mock[ConfigValue]

      val timeColumn1Index = "2"
      val timeColumn1Format = "dd/MM/yy HH:mm"
      val timeColumn2Index = "3"
      val timeColumn2Format = "dd/MM/yy HH:mm"
      val timePropertyValue =
        s"${Constants.characterForElementStartInPropertyValue}$timeColumn1Index${Constants.delimiterForElementInPropertyValue}$timeColumn1Format${Constants.characterForElementEndInPropertyValue}${Constants.delimiterForPropertiesWithListValue}${Constants.characterForElementStartInPropertyValue}$timeColumn2Index${Constants.delimiterForElementInPropertyValue}$timeColumn2Format${Constants.characterForElementEndInPropertyValue}"
      doAnswer(_ => timePropertyValue).when(columnMappingTimeCfgMock).value()

      val expectedResult =
        Set(TimeColumnDto(timeColumn1Index, timeColumn1Format), TimeColumnDto(timeColumn2Index, timeColumn2Format))

      val res = TimeColumnsDto.fromTimeConfig(columnMappingTimeCfgMock)

      verify(columnMappingTimeCfgMock, times(1)).value()

      assert(res == expectedResult)
    }
  }

  describe("getTimeColumnsDto") {
    it("should return a Set[TimeColumnDto] with one element if only one column is defined") {
      val timeColumn1Index = "2"
      val timeColumn1Format = "dd/MM/yy HH:mm"
      val timePropertyValue =
        s"${Constants.characterForElementStartInPropertyValue}$timeColumn1Index${Constants.delimiterForElementInPropertyValue}$timeColumn1Format${Constants.characterForElementEndInPropertyValue}"

      val expectedResult = Set(TimeColumnDto(timeColumn1Index, timeColumn1Format))

      val res = TimeColumnsDto.getTimeColumnsDto(timePropertyValue)

      assert(res == expectedResult)
    }
    it("should return a Set[TimeColumnDto] with two elements if only two columns are defined") {
      val timeColumn1Index = "2"
      val timeColumn1Format = "dd/MM/yy HH:mm"
      val timeColumn2Index = "3"
      val timeColumn2Format = "dd/MM/yy HH:mm"
      val timePropertyValue =
        s"${Constants.characterForElementStartInPropertyValue}$timeColumn1Index${Constants.delimiterForElementInPropertyValue}$timeColumn1Format${Constants.characterForElementEndInPropertyValue}${Constants.delimiterForPropertiesWithListValue}${Constants.characterForElementStartInPropertyValue}$timeColumn2Index${Constants.delimiterForElementInPropertyValue}$timeColumn2Format${Constants.characterForElementEndInPropertyValue}"

      val expectedResult =
        Set(TimeColumnDto(timeColumn1Index, timeColumn1Format), TimeColumnDto(timeColumn2Index, timeColumn2Format))

      val res = TimeColumnsDto.getTimeColumnsDto(timePropertyValue)

      assert(res == expectedResult)
    }
    it("should throw an InvalidPropertyValueException if the property is not defined") {
      val timePropertyValue = Constants.columnMappingStringDefaultValue

      assertThrows[InvalidPropertyValueException](TimeColumnsDto.getTimeColumnsDto(timePropertyValue))
    }
    it("should throw an InvalidPropertyValueException if the number of columns is different than one or two") {
      val timeColumn1Index = "2"
      val timeColumn1Format = "dd/MM/yy HH:mm"
      val timeColumn2Index = "3"
      val timeColumn2Format = "dd/MM/yy HH:mm"
      val timeColumn3Index = "4"
      val timeColumn3Format = "dd/MM/yy HH:mm"
      val timePropertyValue =
        s"${Constants.characterForElementStartInPropertyValue}$timeColumn1Index${Constants.delimiterForElementInPropertyValue}$timeColumn1Format${Constants.characterForElementEndInPropertyValue}${Constants.delimiterForPropertiesWithListValue}${Constants.characterForElementStartInPropertyValue}$timeColumn2Index${Constants.delimiterForElementInPropertyValue}$timeColumn2Format${Constants.characterForElementEndInPropertyValue}${Constants.delimiterForPropertiesWithListValue}${Constants.characterForElementStartInPropertyValue}$timeColumn3Index${Constants.delimiterForElementInPropertyValue}$timeColumn3Format${Constants.characterForElementEndInPropertyValue}"

      assertThrows[InvalidPropertyValueException](TimeColumnsDto.getTimeColumnsDto(timePropertyValue))
    }
  }
}
