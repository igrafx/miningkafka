package com.igrafx.kafka.sink.aggregationmain.domain.usecases

import com.igrafx.kafka.sink.aggregationmain.Constants
import com.igrafx.kafka.sink.aggregationmain.domain.columnmapping.domain.entities._
import com.igrafx.kafka.sink.aggregationmain.domain.entities.ColumnsNumber
import com.igrafx.kafka.sink.aggregationmain.domain.entities.mocks.ColumnsNumberMock
import com.igrafx.kafka.sink.aggregationmain.domain.enums.ConnectorPropertiesEnum
import com.igrafx.kafka.sink.aggregationmain.domain.exceptions.{
  InvalidColumnMappingException,
  InvalidPropertyValueException
}
import core.UnitTestSpec
import org.apache.kafka.common.config.ConfigValue
import org.json4s.ParserUtil.ParseException
import org.mockito.ArgumentMatchers
import org.mockito.Mockito._

import java.util
import scala.util.{Failure, Success, Try}

class ColumnMappingUseCasesImplTest extends UnitTestSpec {
  class TestColumnMappingUseCasesImpl extends ColumnMappingUseCasesImpl

  private val columnMappingUseCases: TestColumnMappingUseCasesImpl = new TestColumnMappingUseCasesImpl

  describe("checkColumnMappingProperties") {
    it("should check the column mapping properties") {
      val columnMappingUseCasesSpy = spy(columnMappingUseCases)

      val columnMappingCreateCfgMock = mock[ConfigValue]
      val configValuesMock = mock[Iterable[ConfigValue]]

      val columnsNumber = ColumnsNumber(4)
      doAnswer(_ => columnsNumber).when(columnMappingUseCasesSpy).getColumnsNumber(configValuesMock)

      doAnswer(_ => ())
        .when(columnMappingUseCasesSpy)
        .checkColumnsProperties(columnMappingCreateCfgMock, configValuesMock, columnsNumber)

      val columnMappingEndOfLineCfgMock = mock[ConfigValue]
      doAnswer(_ => columnMappingEndOfLineCfgMock)
        .when(columnMappingUseCasesSpy)
        .getConfigValue(configValuesMock, ConnectorPropertiesEnum.csvEndOfLineProperty.toStringDescription)
      doAnswer(_ => ())
        .when(columnMappingUseCasesSpy)
        .checkNonEmptyStringProperty(columnMappingEndOfLineCfgMock, ConnectorPropertiesEnum.csvEndOfLineProperty)

      val columnMappingEscapeCfgMock = mock[ConfigValue]
      doAnswer(_ => columnMappingEscapeCfgMock)
        .when(columnMappingUseCasesSpy)
        .getConfigValue(configValuesMock, ConnectorPropertiesEnum.csvEscapeProperty.toStringDescription)
      doAnswer(_ => ())
        .when(columnMappingUseCasesSpy)
        .checkColumnMappingCharacterProperty(columnMappingEscapeCfgMock, ConnectorPropertiesEnum.csvEscapeProperty)

      val columnMappingCommentCfgMock = mock[ConfigValue]
      doAnswer(_ => columnMappingCommentCfgMock)
        .when(columnMappingUseCasesSpy)
        .getConfigValue(configValuesMock, ConnectorPropertiesEnum.csvCommentProperty.toStringDescription)
      doAnswer(_ => ())
        .when(columnMappingUseCasesSpy)
        .checkColumnMappingCharacterProperty(columnMappingCommentCfgMock, ConnectorPropertiesEnum.csvCommentProperty)

      columnMappingUseCasesSpy.checkColumnMappingProperties(columnMappingCreateCfgMock, configValuesMock)

      verify(columnMappingUseCasesSpy, times(1))
        .getColumnsNumber(configValuesMock)
      verify(columnMappingUseCasesSpy, times(1))
        .checkColumnsProperties(columnMappingCreateCfgMock, configValuesMock, columnsNumber)
      verify(columnMappingUseCasesSpy, times(1))
        .getConfigValue(configValuesMock, ConnectorPropertiesEnum.csvEndOfLineProperty.toStringDescription)
      verify(columnMappingUseCasesSpy, times(1))
        .checkNonEmptyStringProperty(columnMappingEndOfLineCfgMock, ConnectorPropertiesEnum.csvEndOfLineProperty)
      verify(columnMappingUseCasesSpy, times(1))
        .getConfigValue(configValuesMock, ConnectorPropertiesEnum.csvEscapeProperty.toStringDescription)
      verify(columnMappingUseCasesSpy, times(1))
        .checkColumnMappingCharacterProperty(columnMappingEscapeCfgMock, ConnectorPropertiesEnum.csvEscapeProperty)
      verify(columnMappingUseCasesSpy, times(1))
        .getConfigValue(configValuesMock, ConnectorPropertiesEnum.csvCommentProperty.toStringDescription)
      verify(columnMappingUseCasesSpy, times(1))
        .checkColumnMappingCharacterProperty(columnMappingCommentCfgMock, ConnectorPropertiesEnum.csvCommentProperty)

      succeed
    }
  }

  describe("getColumnsNumber") {
    it("should return the ColumnsNumber") {
      val columnMappingUseCasesSpy = spy(columnMappingUseCases)

      val configValuesMock = mock[Iterable[ConfigValue]]

      val columnsNumberConfigMock = mock[ConfigValue]
      doAnswer(_ => columnsNumberConfigMock)
        .when(columnMappingUseCasesSpy)
        .getConfigValue(configValuesMock, ConnectorPropertiesEnum.csvFieldsNumberProperty.toStringDescription)

      val errorMessages = new util.ArrayList[String]()
      doAnswer(_ => errorMessages).when(columnsNumberConfigMock).errorMessages()

      val columnsNumberAsInt = 4
      doAnswer(_ => columnsNumberAsInt.toString).when(columnsNumberConfigMock).value()

      val expectedResult = ColumnsNumber(columnsNumberAsInt)

      val res = columnMappingUseCasesSpy.getColumnsNumber(configValuesMock)

      verify(columnMappingUseCasesSpy, times(1))
        .getConfigValue(configValuesMock, ConnectorPropertiesEnum.csvFieldsNumberProperty.toStringDescription)
      verify(columnsNumberConfigMock, times(1)).errorMessages()
      verify(columnsNumberConfigMock, times(1)).value()

      assert(res == expectedResult)
    }
    it("should throw an InvalidPropertyValueException when property value is not readable") {
      val columnMappingUseCasesSpy = spy(columnMappingUseCases)

      val configValuesMock = mock[Iterable[ConfigValue]]

      val columnsNumberConfigMock = mock[ConfigValue]
      doAnswer(_ => columnsNumberConfigMock)
        .when(columnMappingUseCasesSpy)
        .getConfigValue(configValuesMock, ConnectorPropertiesEnum.csvFieldsNumberProperty.toStringDescription)

      val errorMessages = new util.ArrayList[String]()
      doAnswer(_ => errorMessages).when(columnsNumberConfigMock).errorMessages()

      val wrongPropertyValue = "test"
      doAnswer(_ => wrongPropertyValue).when(columnsNumberConfigMock).value()

      assertThrows[InvalidPropertyValueException](columnMappingUseCasesSpy.getColumnsNumber(configValuesMock)).map {
        assert =>
          verify(columnMappingUseCasesSpy, times(1))
            .getConfigValue(configValuesMock, ConnectorPropertiesEnum.csvFieldsNumberProperty.toStringDescription)
          verify(columnsNumberConfigMock, times(1)).errorMessages()
          verify(columnsNumberConfigMock, times(1)).value()

          assert
      }
    }
    it("should throw an InvalidPropertyValueException if the connector already has an issue with this property") {
      val columnMappingUseCasesSpy = spy(columnMappingUseCases)

      val configValuesMock = mock[Iterable[ConfigValue]]

      val columnsNumberConfigMock = mock[ConfigValue]
      doAnswer(_ => columnsNumberConfigMock)
        .when(columnMappingUseCasesSpy)
        .getConfigValue(configValuesMock, ConnectorPropertiesEnum.csvFieldsNumberProperty.toStringDescription)

      val errorMessages = new util.ArrayList[String]()
      errorMessages.add("testError")
      doAnswer(_ => errorMessages).when(columnsNumberConfigMock).errorMessages()

      assertThrows[InvalidPropertyValueException](columnMappingUseCasesSpy.getColumnsNumber(configValuesMock)).map {
        assert =>
          verify(columnMappingUseCasesSpy, times(1))
            .getConfigValue(configValuesMock, ConnectorPropertiesEnum.csvFieldsNumberProperty.toStringDescription)
          verify(columnsNumberConfigMock, times(1)).errorMessages()
          verify(columnsNumberConfigMock, never).value()

          assert
      }
    }
  }

  describe("checkColumnsProperties") {
    it("should check column mapping properties individually and then as a whole") {
      val columnMappingUseCasesSpy = spy(columnMappingUseCases)

      val columnMappingCreateCfgMock = mock[ConfigValue]
      val configValuesMock = mock[Iterable[ConfigValue]]
      val columnsNumber = new ColumnsNumberMock().setNumber(4).build()

      val columnMappingCaseIdCfgMock = mock[ConfigValue]
      doAnswer(_ => columnMappingCaseIdCfgMock)
        .when(columnMappingUseCasesSpy)
        .getConfigValue(configValuesMock, ConnectorPropertiesEnum.columnMappingCaseIdColumnProperty.toStringDescription)
      val columnMappingActivityCfgMock = mock[ConfigValue]
      doAnswer(_ => columnMappingActivityCfgMock)
        .when(columnMappingUseCasesSpy)
        .getConfigValue(
          configValuesMock,
          ConnectorPropertiesEnum.columnMappingActivityColumnProperty.toStringDescription
        )
      val columnMappingTimeCfgMock = mock[ConfigValue]
      doAnswer(_ => columnMappingTimeCfgMock)
        .when(columnMappingUseCasesSpy)
        .getConfigValue(
          configValuesMock,
          ConnectorPropertiesEnum.columnMappingTimeColumnsProperty.toStringDescription
        )
      val columnMappingDimensionsCfgMock = mock[ConfigValue]
      doAnswer(_ => columnMappingDimensionsCfgMock)
        .when(columnMappingUseCasesSpy)
        .getConfigValue(
          configValuesMock,
          ConnectorPropertiesEnum.columnMappingDimensionColumnsProperty.toStringDescription
        )
      val columnMappingMetricsCfgMock = mock[ConfigValue]
      doAnswer(_ => columnMappingMetricsCfgMock)
        .when(columnMappingUseCasesSpy)
        .getConfigValue(
          configValuesMock,
          ConnectorPropertiesEnum.columnMappingMetricColumnsProperty.toStringDescription
        )
      val columnMappingGroupedTasksCfgMock = mock[ConfigValue]
      doAnswer(_ => columnMappingGroupedTasksCfgMock)
        .when(columnMappingUseCasesSpy)
        .getConfigValue(
          configValuesMock,
          ConnectorPropertiesEnum.columnMappingGroupedTasksColumnsProperty.toStringDescription
        )

      val validCaseIdColumn = new ValidCaseIdColumnMock().build()
      val validActivityColumn = new ValidActivityColumnMock().build()
      val validTimeColumns = Set(new ValidTimeColumnMock().build())
      val dimensionColumns = Set(new ValidDimensionColumnMock().build())
      val metricColumns = Set(new ValidMetricColumnMock().build())
      val groupedTasksColumns = None

      doAnswer(_ => Some(validCaseIdColumn))
        .when(columnMappingUseCasesSpy)
        .checkColumnMappingCaseIdProperty(columnMappingCaseIdCfgMock, columnsNumber)
      doAnswer(_ => Some(validActivityColumn))
        .when(columnMappingUseCasesSpy)
        .checkColumnMappingActivityProperty(columnMappingActivityCfgMock, columnsNumber)
      doAnswer(_ => Some(validTimeColumns))
        .when(columnMappingUseCasesSpy)
        .checkColumnMappingTimeProperty(columnMappingTimeCfgMock, columnsNumber)
      doAnswer(_ => Some(dimensionColumns))
        .when(columnMappingUseCasesSpy)
        .checkColumnMappingDimensionProperty(columnMappingDimensionsCfgMock, columnsNumber)
      doAnswer(_ => Some(metricColumns))
        .when(columnMappingUseCasesSpy)
        .checkColumnMappingMetricProperty(columnMappingMetricsCfgMock, columnsNumber)
      doAnswer(_ => Some(groupedTasksColumns))
        .when(columnMappingUseCasesSpy)
        .checkColumnMappingGroupedTasksColumnsProperty(columnMappingGroupedTasksCfgMock, columnsNumber)

      doAnswer(_ => ())
        .when(columnMappingUseCasesSpy)
        .checkCompleteColumnMapping(
          columnMappingCreateCfg = columnMappingCreateCfgMock,
          caseIdColumn = validCaseIdColumn,
          activityColumn = validActivityColumn,
          timeColumns = validTimeColumns,
          dimensionColumns = dimensionColumns,
          metricColumns = metricColumns,
          groupedTasksColumns = groupedTasksColumns,
          columnsNumber = columnsNumber
        )

      columnMappingUseCasesSpy.checkColumnsProperties(columnMappingCreateCfgMock, configValuesMock, columnsNumber)

      verify(columnMappingUseCasesSpy, times(1))
        .getConfigValue(configValuesMock, ConnectorPropertiesEnum.columnMappingCaseIdColumnProperty.toStringDescription)
      verify(columnMappingUseCasesSpy, times(1))
        .getConfigValue(
          configValuesMock,
          ConnectorPropertiesEnum.columnMappingActivityColumnProperty.toStringDescription
        )
      verify(columnMappingUseCasesSpy, times(1))
        .getConfigValue(configValuesMock, ConnectorPropertiesEnum.columnMappingTimeColumnsProperty.toStringDescription)
      verify(columnMappingUseCasesSpy, times(1))
        .getConfigValue(
          configValuesMock,
          ConnectorPropertiesEnum.columnMappingDimensionColumnsProperty.toStringDescription
        )
      verify(columnMappingUseCasesSpy, times(1))
        .getConfigValue(
          configValuesMock,
          ConnectorPropertiesEnum.columnMappingMetricColumnsProperty.toStringDescription
        )
      verify(columnMappingUseCasesSpy, times(1)).getConfigValue(
        configValuesMock,
        ConnectorPropertiesEnum.columnMappingGroupedTasksColumnsProperty.toStringDescription
      )
      verify(columnMappingUseCasesSpy, times(1))
        .checkColumnMappingCaseIdProperty(columnMappingCaseIdCfgMock, columnsNumber)
      verify(columnMappingUseCasesSpy, times(1))
        .checkColumnMappingActivityProperty(columnMappingActivityCfgMock, columnsNumber)
      verify(columnMappingUseCasesSpy, times(1)).checkColumnMappingTimeProperty(columnMappingTimeCfgMock, columnsNumber)
      verify(columnMappingUseCasesSpy, times(1))
        .checkColumnMappingDimensionProperty(columnMappingDimensionsCfgMock, columnsNumber)
      verify(columnMappingUseCasesSpy, times(1))
        .checkColumnMappingMetricProperty(columnMappingMetricsCfgMock, columnsNumber)
      verify(columnMappingUseCasesSpy, times(1))
        .checkColumnMappingGroupedTasksColumnsProperty(columnMappingGroupedTasksCfgMock, columnsNumber)
      verify(columnMappingUseCasesSpy, times(1)).checkCompleteColumnMapping(
        columnMappingCreateCfgMock,
        validCaseIdColumn,
        validActivityColumn,
        validTimeColumns,
        dimensionColumns,
        metricColumns,
        groupedTasksColumns,
        columnsNumber
      )

      succeed
    }
    it("should only check column mapping properties individually if one of those added an error message") {
      val columnMappingUseCasesSpy = spy(columnMappingUseCases)

      val columnMappingCreateCfgMock = mock[ConfigValue]
      val configValuesMock = mock[Iterable[ConfigValue]]
      val columnsNumber = new ColumnsNumberMock().setNumber(4).build()

      val columnMappingCaseIdCfgMock = mock[ConfigValue]
      doAnswer(_ => columnMappingCaseIdCfgMock)
        .when(columnMappingUseCasesSpy)
        .getConfigValue(configValuesMock, ConnectorPropertiesEnum.columnMappingCaseIdColumnProperty.toStringDescription)
      val columnMappingActivityCfgMock = mock[ConfigValue]
      doAnswer(_ => columnMappingActivityCfgMock)
        .when(columnMappingUseCasesSpy)
        .getConfigValue(
          configValuesMock,
          ConnectorPropertiesEnum.columnMappingActivityColumnProperty.toStringDescription
        )
      val columnMappingTimeCfgMock = mock[ConfigValue]
      doAnswer(_ => columnMappingTimeCfgMock)
        .when(columnMappingUseCasesSpy)
        .getConfigValue(
          configValuesMock,
          ConnectorPropertiesEnum.columnMappingTimeColumnsProperty.toStringDescription
        )
      val columnMappingDimensionsCfgMock = mock[ConfigValue]
      doAnswer(_ => columnMappingDimensionsCfgMock)
        .when(columnMappingUseCasesSpy)
        .getConfigValue(
          configValuesMock,
          ConnectorPropertiesEnum.columnMappingDimensionColumnsProperty.toStringDescription
        )
      val columnMappingMetricsCfgMock = mock[ConfigValue]
      doAnswer(_ => columnMappingMetricsCfgMock)
        .when(columnMappingUseCasesSpy)
        .getConfigValue(
          configValuesMock,
          ConnectorPropertiesEnum.columnMappingMetricColumnsProperty.toStringDescription
        )
      val columnMappingGroupedTasksCfgMock = mock[ConfigValue]
      doAnswer(_ => columnMappingGroupedTasksCfgMock)
        .when(columnMappingUseCasesSpy)
        .getConfigValue(
          configValuesMock,
          ConnectorPropertiesEnum.columnMappingGroupedTasksColumnsProperty.toStringDescription
        )

      val validCaseIdColumn = new ValidCaseIdColumnMock().build()
      val validActivityColumn = new ValidActivityColumnMock().build()
      val validTimeColumns = Set(new ValidTimeColumnMock().build())
      val metricColumns = Set(new ValidMetricColumnMock().build())
      val groupedTasksColumns = None

      doAnswer(_ => Some(validCaseIdColumn))
        .when(columnMappingUseCasesSpy)
        .checkColumnMappingCaseIdProperty(columnMappingCaseIdCfgMock, columnsNumber)
      doAnswer(_ => Some(validActivityColumn))
        .when(columnMappingUseCasesSpy)
        .checkColumnMappingActivityProperty(columnMappingActivityCfgMock, columnsNumber)
      doAnswer(_ => Some(validTimeColumns))
        .when(columnMappingUseCasesSpy)
        .checkColumnMappingTimeProperty(columnMappingTimeCfgMock, columnsNumber)
      doAnswer(_ => None)
        .when(columnMappingUseCasesSpy)
        .checkColumnMappingDimensionProperty(columnMappingDimensionsCfgMock, columnsNumber)
      doAnswer(_ => Some(metricColumns))
        .when(columnMappingUseCasesSpy)
        .checkColumnMappingMetricProperty(columnMappingMetricsCfgMock, columnsNumber)
      doAnswer(_ => Some(groupedTasksColumns))
        .when(columnMappingUseCasesSpy)
        .checkColumnMappingGroupedTasksColumnsProperty(columnMappingGroupedTasksCfgMock, columnsNumber)

      columnMappingUseCasesSpy.checkColumnsProperties(columnMappingCreateCfgMock, configValuesMock, columnsNumber)

      verify(columnMappingUseCasesSpy, times(1))
        .getConfigValue(configValuesMock, ConnectorPropertiesEnum.columnMappingCaseIdColumnProperty.toStringDescription)
      verify(columnMappingUseCasesSpy, times(1))
        .getConfigValue(
          configValuesMock,
          ConnectorPropertiesEnum.columnMappingActivityColumnProperty.toStringDescription
        )
      verify(columnMappingUseCasesSpy, times(1))
        .getConfigValue(configValuesMock, ConnectorPropertiesEnum.columnMappingTimeColumnsProperty.toStringDescription)
      verify(columnMappingUseCasesSpy, times(1))
        .getConfigValue(
          configValuesMock,
          ConnectorPropertiesEnum.columnMappingDimensionColumnsProperty.toStringDescription
        )
      verify(columnMappingUseCasesSpy, times(1))
        .getConfigValue(
          configValuesMock,
          ConnectorPropertiesEnum.columnMappingMetricColumnsProperty.toStringDescription
        )
      verify(columnMappingUseCasesSpy, times(1)).getConfigValue(
        configValuesMock,
        ConnectorPropertiesEnum.columnMappingGroupedTasksColumnsProperty.toStringDescription
      )
      verify(columnMappingUseCasesSpy, times(1))
        .checkColumnMappingCaseIdProperty(columnMappingCaseIdCfgMock, columnsNumber)
      verify(columnMappingUseCasesSpy, times(1))
        .checkColumnMappingActivityProperty(columnMappingActivityCfgMock, columnsNumber)
      verify(columnMappingUseCasesSpy, times(1)).checkColumnMappingTimeProperty(columnMappingTimeCfgMock, columnsNumber)
      verify(columnMappingUseCasesSpy, times(1))
        .checkColumnMappingDimensionProperty(columnMappingDimensionsCfgMock, columnsNumber)
      verify(columnMappingUseCasesSpy, times(1))
        .checkColumnMappingMetricProperty(columnMappingMetricsCfgMock, columnsNumber)
      verify(columnMappingUseCasesSpy, times(1))
        .checkColumnMappingGroupedTasksColumnsProperty(columnMappingGroupedTasksCfgMock, columnsNumber)
      verify(columnMappingUseCasesSpy, never).checkCompleteColumnMapping(
        ArgumentMatchers.eq(columnMappingCreateCfgMock),
        ArgumentMatchers.any[ValidCaseIdColumn],
        ArgumentMatchers.any[ValidActivityColumn],
        ArgumentMatchers.any[Set[ValidTimeColumn]],
        ArgumentMatchers.any[Set[ValidDimensionColumn]],
        ArgumentMatchers.any[Set[ValidMetricColumn]],
        ArgumentMatchers.any[Option[ValidGroupedTasksColumns]],
        ArgumentMatchers.eq(columnsNumber)
      )

      succeed
    }
  }

  describe("checkCompleteColumnMapping") {
    it("should do nothing if complete column mapping is valid") {
      val columnMappingUseCasesSpy = spy(columnMappingUseCases)

      val columnMappingCreateCfgMock = mock[ConfigValue]
      val caseIdColumn: ValidCaseIdColumn = new ValidCaseIdColumnMock().build()
      val activityColumn: ValidActivityColumn = new ValidActivityColumnMock().build()
      val timeColumns: Set[ValidTimeColumn] = Set(new ValidTimeColumnMock().build())
      val dimensionColumns: Set[ValidDimensionColumn] = Set(new ValidDimensionColumnMock().build())
      val metricColumns: Set[ValidMetricColumn] = Set(new ValidMetricColumnMock().build())
      val groupedTasksColumns: Option[ValidGroupedTasksColumns] = None
      val columnsNumber: ColumnsNumber = new ColumnsNumberMock().setNumber(5).build()

      doAnswer(_ => new util.ArrayList[String]()).when(columnMappingCreateCfgMock).errorMessages()

      val columnMapping =
        new ValidColumnMappingMock()
          .setCaseId(caseIdColumn)
          .setActivity(activityColumn)
          .setTime(timeColumns)
          .setDimension(dimensionColumns)
          .setMetric(metricColumns)
          .setGroupedTasksColumnsOpt(groupedTasksColumns)
          .setColumnsNumber(columnsNumber)
          .build()
      doAnswer(_ => columnMapping)
        .when(columnMappingUseCasesSpy)
        .getValidColumnMapping(
          caseIdColumn = caseIdColumn,
          activityColumn = activityColumn,
          timeColumns = timeColumns,
          dimensionColumns = dimensionColumns,
          metricColumns = metricColumns,
          groupedTasksColumns = groupedTasksColumns,
          columnsNumber = columnsNumber
        )

      columnMappingUseCasesSpy.checkCompleteColumnMapping(
        columnMappingCreateCfg = columnMappingCreateCfgMock,
        caseIdColumn = caseIdColumn,
        activityColumn = activityColumn,
        timeColumns = timeColumns,
        dimensionColumns = dimensionColumns,
        metricColumns = metricColumns,
        groupedTasksColumns = groupedTasksColumns,
        columnsNumber = columnsNumber
      )

      verify(columnMappingCreateCfgMock, times(1)).errorMessages()
      verify(columnMappingUseCasesSpy, times(1)).getValidColumnMapping(
        caseIdColumn,
        activityColumn,
        timeColumns,
        dimensionColumns,
        metricColumns,
        groupedTasksColumns,
        columnsNumber
      )
      verify(columnMappingCreateCfgMock, never).addErrorMessage(ArgumentMatchers.any[String])

      succeed
    }
    it("should add an error message if complete column mapping returns InvalidColumnMappingException") {
      val columnMappingUseCasesSpy = spy(columnMappingUseCases)

      val columnMappingCreateCfgMock = mock[ConfigValue]
      val caseIdColumn: ValidCaseIdColumn = new ValidCaseIdColumnMock().build()
      val activityColumn: ValidActivityColumn = new ValidActivityColumnMock().build()
      val timeColumns: Set[ValidTimeColumn] = Set(new ValidTimeColumnMock().build())
      val dimensionColumns: Set[ValidDimensionColumn] = Set(new ValidDimensionColumnMock().build())
      val metricColumns: Set[ValidMetricColumn] = Set(new ValidMetricColumnMock().build())
      val groupedTasksColumns: Option[ValidGroupedTasksColumns] = None
      val columnsNumber: ColumnsNumber = new ColumnsNumberMock().setNumber(5).build()

      doAnswer(_ => new util.ArrayList[String]()).when(columnMappingCreateCfgMock).errorMessages()

      doThrow(InvalidColumnMappingException("test"))
        .when(columnMappingUseCasesSpy)
        .getValidColumnMapping(
          caseIdColumn = caseIdColumn,
          activityColumn = activityColumn,
          timeColumns = timeColumns,
          dimensionColumns = dimensionColumns,
          metricColumns = metricColumns,
          groupedTasksColumns = groupedTasksColumns,
          columnsNumber = columnsNumber
        )

      columnMappingUseCasesSpy.checkCompleteColumnMapping(
        columnMappingCreateCfg = columnMappingCreateCfgMock,
        caseIdColumn = caseIdColumn,
        activityColumn = activityColumn,
        timeColumns = timeColumns,
        dimensionColumns = dimensionColumns,
        metricColumns = metricColumns,
        groupedTasksColumns = groupedTasksColumns,
        columnsNumber = columnsNumber
      )

      verify(columnMappingCreateCfgMock, times(1)).errorMessages()
      verify(columnMappingUseCasesSpy, times(1)).getValidColumnMapping(
        caseIdColumn,
        activityColumn,
        timeColumns,
        dimensionColumns,
        metricColumns,
        groupedTasksColumns,
        columnsNumber
      )
      verify(columnMappingCreateCfgMock, times(1)).addErrorMessage(ArgumentMatchers.any[String])

      succeed
    }
    it("should add an error message if complete column mapping returns an unexpected exception") {
      val columnMappingUseCasesSpy = spy(columnMappingUseCases)

      val columnMappingCreateCfgMock = mock[ConfigValue]
      val caseIdColumn: ValidCaseIdColumn = new ValidCaseIdColumnMock().build()
      val activityColumn: ValidActivityColumn = new ValidActivityColumnMock().build()
      val timeColumns: Set[ValidTimeColumn] = Set(new ValidTimeColumnMock().build())
      val dimensionColumns: Set[ValidDimensionColumn] = Set(new ValidDimensionColumnMock().build())
      val metricColumns: Set[ValidMetricColumn] = Set(new ValidMetricColumnMock().build())
      val groupedTasksColumns: Option[ValidGroupedTasksColumns] = None
      val columnsNumber: ColumnsNumber = new ColumnsNumberMock().setNumber(5).build()

      doAnswer(_ => new util.ArrayList[String]()).when(columnMappingCreateCfgMock).errorMessages()

      doThrow(new IllegalArgumentException())
        .when(columnMappingUseCasesSpy)
        .getValidColumnMapping(
          caseIdColumn = caseIdColumn,
          activityColumn = activityColumn,
          timeColumns = timeColumns,
          dimensionColumns = dimensionColumns,
          metricColumns = metricColumns,
          groupedTasksColumns = groupedTasksColumns,
          columnsNumber = columnsNumber
        )

      columnMappingUseCasesSpy.checkCompleteColumnMapping(
        columnMappingCreateCfg = columnMappingCreateCfgMock,
        caseIdColumn = caseIdColumn,
        activityColumn = activityColumn,
        timeColumns = timeColumns,
        dimensionColumns = dimensionColumns,
        metricColumns = metricColumns,
        groupedTasksColumns = groupedTasksColumns,
        columnsNumber = columnsNumber
      )

      verify(columnMappingCreateCfgMock, times(1)).errorMessages()
      verify(columnMappingUseCasesSpy, times(1)).getValidColumnMapping(
        caseIdColumn,
        activityColumn,
        timeColumns,
        dimensionColumns,
        metricColumns,
        groupedTasksColumns,
        columnsNumber
      )
      verify(columnMappingCreateCfgMock, times(1)).addErrorMessage(ArgumentMatchers.any[String])

      succeed
    }
    it("should do nothing if property already has an error message") {
      val columnMappingUseCasesSpy = spy(columnMappingUseCases)

      val columnMappingCreateCfgMock = mock[ConfigValue]
      val caseIdColumn: ValidCaseIdColumn = new ValidCaseIdColumnMock().build()
      val activityColumn: ValidActivityColumn = new ValidActivityColumnMock().build()
      val timeColumns: Set[ValidTimeColumn] = Set(new ValidTimeColumnMock().build())
      val dimensionColumns: Set[ValidDimensionColumn] = Set(new ValidDimensionColumnMock().build())
      val metricColumns: Set[ValidMetricColumn] = Set(new ValidMetricColumnMock().build())
      val groupedTasksColumns: Option[ValidGroupedTasksColumns] = None
      val columnsNumber: ColumnsNumber = new ColumnsNumberMock().setNumber(5).build()

      val errorMessages = new util.ArrayList[String]()
      errorMessages.add("testError")
      doAnswer(_ => errorMessages).when(columnMappingCreateCfgMock).errorMessages()

      columnMappingUseCasesSpy.checkCompleteColumnMapping(
        columnMappingCreateCfg = columnMappingCreateCfgMock,
        caseIdColumn = caseIdColumn,
        activityColumn = activityColumn,
        timeColumns = timeColumns,
        dimensionColumns = dimensionColumns,
        metricColumns = metricColumns,
        groupedTasksColumns = groupedTasksColumns,
        columnsNumber = columnsNumber
      )

      verify(columnMappingCreateCfgMock, times(1)).errorMessages()
      verify(columnMappingUseCasesSpy, never).getValidColumnMapping(
        caseIdColumn,
        activityColumn,
        timeColumns,
        dimensionColumns,
        metricColumns,
        groupedTasksColumns,
        columnsNumber
      )
      verify(columnMappingCreateCfgMock, never).addErrorMessage(ArgumentMatchers.any[String])

      succeed
    }
  }

  describe("checkColumnMappingCaseIdProperty") {
    it("should return a Some[ValidCaseIdColumn] if property is valid") {
      val columnMappingUseCasesSpy = spy(columnMappingUseCases)

      val columnMappingCaseIdCfgMock = mock[ConfigValue]
      val columnsNumber = new ColumnsNumberMock().setNumber(5).build()

      val errorMessages = new util.ArrayList[String]()
      doAnswer(_ => errorMessages).when(columnMappingCaseIdCfgMock).errorMessages()

      val validCaseIdColumn = new ValidCaseIdColumnMock().build()
      doAnswer(_ => validCaseIdColumn)
        .when(columnMappingUseCasesSpy)
        .getValidCaseIdColumnFromProperty(columnMappingCaseIdCfgMock, columnsNumber)

      val expectedResult = Some(validCaseIdColumn)
      doAnswer(_ => expectedResult)
        .when(columnMappingUseCasesSpy)
        .checkErrors(Success(expectedResult), columnMappingCaseIdCfgMock)

      val res = columnMappingUseCasesSpy.checkColumnMappingCaseIdProperty(columnMappingCaseIdCfgMock, columnsNumber)

      verify(columnMappingCaseIdCfgMock, times(1)).errorMessages()
      verify(columnMappingUseCasesSpy, times(1))
        .getValidCaseIdColumnFromProperty(columnMappingCaseIdCfgMock, columnsNumber)
      verify(columnMappingUseCasesSpy, times(1)).checkErrors(Success(expectedResult), columnMappingCaseIdCfgMock)

      assert(res == expectedResult)
    }
    it("should return None if property leads to InvalidPropertyValueException") {
      val columnMappingUseCasesSpy = spy(columnMappingUseCases)

      val columnMappingCaseIdCfgMock = mock[ConfigValue]
      val columnsNumber = new ColumnsNumberMock().setNumber(5).build()

      val errorMessages = new util.ArrayList[String]()
      doAnswer(_ => errorMessages).when(columnMappingCaseIdCfgMock).errorMessages()

      val exception = InvalidPropertyValueException("test")
      doThrow(exception)
        .when(columnMappingUseCasesSpy)
        .getValidCaseIdColumnFromProperty(columnMappingCaseIdCfgMock, columnsNumber)

      val expectedResult = None
      doAnswer(_ => expectedResult)
        .when(columnMappingUseCasesSpy)
        .checkErrors(Failure(exception), columnMappingCaseIdCfgMock)

      val res = columnMappingUseCasesSpy.checkColumnMappingCaseIdProperty(columnMappingCaseIdCfgMock, columnsNumber)

      verify(columnMappingCaseIdCfgMock, times(1)).errorMessages()
      verify(columnMappingUseCasesSpy, times(1))
        .getValidCaseIdColumnFromProperty(columnMappingCaseIdCfgMock, columnsNumber)
      verify(columnMappingUseCasesSpy, times(1)).checkErrors(Failure(exception), columnMappingCaseIdCfgMock)

      assert(res == expectedResult)
    }
    it("should return None if property already has an error message") {
      val columnMappingUseCasesSpy = spy(columnMappingUseCases)

      val columnMappingCaseIdCfgMock = mock[ConfigValue]
      val columnsNumber = new ColumnsNumberMock().setNumber(5).build()

      val errorMessages = new util.ArrayList[String]()
      errorMessages.add("testError")
      doAnswer(_ => errorMessages).when(columnMappingCaseIdCfgMock).errorMessages()

      val res = columnMappingUseCasesSpy.checkColumnMappingCaseIdProperty(columnMappingCaseIdCfgMock, columnsNumber)

      verify(columnMappingCaseIdCfgMock, times(1)).errorMessages()
      verify(columnMappingUseCasesSpy, never)
        .getValidCaseIdColumnFromProperty(columnMappingCaseIdCfgMock, columnsNumber)
      verify(columnMappingUseCasesSpy, never).checkErrors(
        ArgumentMatchers.any[Try[Option[ValidCaseIdColumn]]],
        ArgumentMatchers.eq(columnMappingCaseIdCfgMock)
      )

      assert(res.isEmpty)
    }
  }

  describe("checkColumnMappingActivityProperty") {
    it("should return a Some[ValidActivityColumn] if property is valid") {
      val columnMappingUseCasesSpy = spy(columnMappingUseCases)

      val columnMappingActivityCfgMock = mock[ConfigValue]
      val columnsNumber = new ColumnsNumberMock().setNumber(5).build()

      val errorMessages = new util.ArrayList[String]()
      doAnswer(_ => errorMessages).when(columnMappingActivityCfgMock).errorMessages()

      val validActivityColumn = new ValidActivityColumnMock().build()
      doAnswer(_ => validActivityColumn)
        .when(columnMappingUseCasesSpy)
        .getValidActivityColumnFromProperty(columnMappingActivityCfgMock, columnsNumber)

      val expectedResult = Some(validActivityColumn)
      doAnswer(_ => expectedResult)
        .when(columnMappingUseCasesSpy)
        .checkErrors(Success(expectedResult), columnMappingActivityCfgMock)

      val res = columnMappingUseCasesSpy.checkColumnMappingActivityProperty(columnMappingActivityCfgMock, columnsNumber)

      verify(columnMappingActivityCfgMock, times(1)).errorMessages()
      verify(columnMappingUseCasesSpy, times(1))
        .getValidActivityColumnFromProperty(columnMappingActivityCfgMock, columnsNumber)
      verify(columnMappingUseCasesSpy, times(1)).checkErrors(Success(expectedResult), columnMappingActivityCfgMock)

      assert(res == expectedResult)
    }
    it("should return None if property leads to InvalidPropertyValueException") {
      val columnMappingUseCasesSpy = spy(columnMappingUseCases)

      val columnMappingActivityCfgMock = mock[ConfigValue]
      val columnsNumber = new ColumnsNumberMock().setNumber(5).build()

      val errorMessages = new util.ArrayList[String]()
      doAnswer(_ => errorMessages).when(columnMappingActivityCfgMock).errorMessages()

      val exception = InvalidPropertyValueException("test")
      doThrow(exception)
        .when(columnMappingUseCasesSpy)
        .getValidActivityColumnFromProperty(columnMappingActivityCfgMock, columnsNumber)

      val expectedResult = None
      doAnswer(_ => expectedResult)
        .when(columnMappingUseCasesSpy)
        .checkErrors(Failure(exception), columnMappingActivityCfgMock)

      val res = columnMappingUseCasesSpy.checkColumnMappingActivityProperty(columnMappingActivityCfgMock, columnsNumber)

      verify(columnMappingActivityCfgMock, times(1)).errorMessages()
      verify(columnMappingUseCasesSpy, times(1))
        .getValidActivityColumnFromProperty(columnMappingActivityCfgMock, columnsNumber)
      verify(columnMappingUseCasesSpy, times(1)).checkErrors(Failure(exception), columnMappingActivityCfgMock)

      assert(res == expectedResult)
    }
    it("should return None if property already has an error message") {
      val columnMappingUseCasesSpy = spy(columnMappingUseCases)

      val columnMappingActivityCfgMock = mock[ConfigValue]
      val columnsNumber = new ColumnsNumberMock().setNumber(5).build()

      val errorMessages = new util.ArrayList[String]()
      errorMessages.add("testError")
      doAnswer(_ => errorMessages).when(columnMappingActivityCfgMock).errorMessages()

      val res = columnMappingUseCasesSpy.checkColumnMappingActivityProperty(columnMappingActivityCfgMock, columnsNumber)

      verify(columnMappingActivityCfgMock, times(1)).errorMessages()
      verify(columnMappingUseCasesSpy, never)
        .getValidActivityColumnFromProperty(columnMappingActivityCfgMock, columnsNumber)
      verify(columnMappingUseCasesSpy, never).checkErrors(
        ArgumentMatchers.any[Try[Option[ValidActivityColumn]]],
        ArgumentMatchers.eq(columnMappingActivityCfgMock)
      )

      assert(res.isEmpty)
    }
  }

  describe("checkColumnMappingTimeProperty") {
    it("should return a Some[Set[ValidTimeColumn]] if property is valid") {
      val columnMappingUseCasesSpy = spy(columnMappingUseCases)

      val columnMappingTimeCfgMock = mock[ConfigValue]
      val columnsNumber = new ColumnsNumberMock().setNumber(5).build()

      val errorMessages = new util.ArrayList[String]()
      doAnswer(_ => errorMessages).when(columnMappingTimeCfgMock).errorMessages()

      val validTimeColumns = Set(new ValidTimeColumnMock().build())
      doAnswer(_ => validTimeColumns)
        .when(columnMappingUseCasesSpy)
        .getValidTimeColumnsFromProperty(columnMappingTimeCfgMock, columnsNumber)

      val expectedResult = Some(validTimeColumns)
      doAnswer(_ => expectedResult)
        .when(columnMappingUseCasesSpy)
        .checkErrors(Success(expectedResult), columnMappingTimeCfgMock)

      val res = columnMappingUseCasesSpy.checkColumnMappingTimeProperty(columnMappingTimeCfgMock, columnsNumber)

      verify(columnMappingTimeCfgMock, times(1)).errorMessages()
      verify(columnMappingUseCasesSpy, times(1))
        .getValidTimeColumnsFromProperty(columnMappingTimeCfgMock, columnsNumber)
      verify(columnMappingUseCasesSpy, times(1)).checkErrors(Success(expectedResult), columnMappingTimeCfgMock)

      assert(res == expectedResult)
    }
    it("should return None if property leads to InvalidPropertyValueException") {
      val columnMappingUseCasesSpy = spy(columnMappingUseCases)

      val columnMappingTimeCfgMock = mock[ConfigValue]
      val columnsNumber = new ColumnsNumberMock().setNumber(5).build()

      val errorMessages = new util.ArrayList[String]()
      doAnswer(_ => errorMessages).when(columnMappingTimeCfgMock).errorMessages()

      val exception = InvalidPropertyValueException("test")
      doThrow(exception)
        .when(columnMappingUseCasesSpy)
        .getValidTimeColumnsFromProperty(columnMappingTimeCfgMock, columnsNumber)

      val expectedResult = None
      doAnswer(_ => expectedResult)
        .when(columnMappingUseCasesSpy)
        .checkErrors(Failure(exception), columnMappingTimeCfgMock)

      val res = columnMappingUseCasesSpy.checkColumnMappingTimeProperty(columnMappingTimeCfgMock, columnsNumber)

      verify(columnMappingTimeCfgMock, times(1)).errorMessages()
      verify(columnMappingUseCasesSpy, times(1))
        .getValidTimeColumnsFromProperty(columnMappingTimeCfgMock, columnsNumber)
      verify(columnMappingUseCasesSpy, times(1)).checkErrors(Failure(exception), columnMappingTimeCfgMock)

      assert(res == expectedResult)
    }
    it("should return None if property already has an error message") {
      val columnMappingUseCasesSpy = spy(columnMappingUseCases)

      val columnMappingTimeCfgMock = mock[ConfigValue]
      val columnsNumber = new ColumnsNumberMock().setNumber(5).build()

      val errorMessages = new util.ArrayList[String]()
      errorMessages.add("testError")
      doAnswer(_ => errorMessages).when(columnMappingTimeCfgMock).errorMessages()

      val res = columnMappingUseCasesSpy.checkColumnMappingTimeProperty(columnMappingTimeCfgMock, columnsNumber)

      verify(columnMappingTimeCfgMock, times(1)).errorMessages()
      verify(columnMappingUseCasesSpy, never)
        .getValidTimeColumnsFromProperty(columnMappingTimeCfgMock, columnsNumber)
      verify(columnMappingUseCasesSpy, never).checkErrors(
        ArgumentMatchers.any[Try[Option[Set[ValidTimeColumn]]]],
        ArgumentMatchers.eq(columnMappingTimeCfgMock)
      )

      assert(res.isEmpty)
    }
  }

  describe("checkColumnMappingDimensionProperty") {
    it("should return a Some[Set[ValidDimensionColumn]] if property is valid") {
      val columnMappingUseCasesSpy = spy(columnMappingUseCases)

      val columnMappingDimensionCfgMock = mock[ConfigValue]
      val columnsNumber = new ColumnsNumberMock().setNumber(5).build()

      val errorMessages = new util.ArrayList[String]()
      doAnswer(_ => errorMessages).when(columnMappingDimensionCfgMock).errorMessages()

      val validDimensionColumns = Set(new ValidDimensionColumnMock().build())
      doAnswer(_ => validDimensionColumns)
        .when(columnMappingUseCasesSpy)
        .getValidDimensionColumnsFromProperty(columnMappingDimensionCfgMock, columnsNumber)

      val expectedResult = Some(validDimensionColumns)
      doAnswer(_ => expectedResult)
        .when(columnMappingUseCasesSpy)
        .checkErrors(Success(expectedResult), columnMappingDimensionCfgMock)

      val res =
        columnMappingUseCasesSpy.checkColumnMappingDimensionProperty(columnMappingDimensionCfgMock, columnsNumber)

      verify(columnMappingDimensionCfgMock, times(1)).errorMessages()
      verify(columnMappingUseCasesSpy, times(1))
        .getValidDimensionColumnsFromProperty(columnMappingDimensionCfgMock, columnsNumber)
      verify(columnMappingUseCasesSpy, times(1)).checkErrors(Success(expectedResult), columnMappingDimensionCfgMock)

      assert(res == expectedResult)
    }
    it("should return None if property leads to InvalidPropertyValueException") {
      val columnMappingUseCasesSpy = spy(columnMappingUseCases)

      val columnMappingDimensionCfgMock = mock[ConfigValue]
      val columnsNumber = new ColumnsNumberMock().setNumber(5).build()

      val errorMessages = new util.ArrayList[String]()
      doAnswer(_ => errorMessages).when(columnMappingDimensionCfgMock).errorMessages()

      val exception = InvalidPropertyValueException("test")
      doThrow(exception)
        .when(columnMappingUseCasesSpy)
        .getValidDimensionColumnsFromProperty(columnMappingDimensionCfgMock, columnsNumber)

      val expectedResult = None
      doAnswer(_ => expectedResult)
        .when(columnMappingUseCasesSpy)
        .checkErrors(Failure(exception), columnMappingDimensionCfgMock)

      val res =
        columnMappingUseCasesSpy.checkColumnMappingDimensionProperty(columnMappingDimensionCfgMock, columnsNumber)

      verify(columnMappingDimensionCfgMock, times(1)).errorMessages()
      verify(columnMappingUseCasesSpy, times(1))
        .getValidDimensionColumnsFromProperty(columnMappingDimensionCfgMock, columnsNumber)
      verify(columnMappingUseCasesSpy, times(1)).checkErrors(Failure(exception), columnMappingDimensionCfgMock)

      assert(res == expectedResult)
    }
    it("should return None if property leads to ParseException") {
      val columnMappingUseCasesSpy = spy(columnMappingUseCases)

      val columnMappingDimensionCfgMock = mock[ConfigValue]
      val columnsNumber = new ColumnsNumberMock().setNumber(5).build()

      val errorMessages = new util.ArrayList[String]()
      doAnswer(_ => errorMessages).when(columnMappingDimensionCfgMock).errorMessages()

      val exception = new ParseException("test", new Exception())
      doThrow(exception)
        .when(columnMappingUseCasesSpy)
        .getValidDimensionColumnsFromProperty(columnMappingDimensionCfgMock, columnsNumber)

      val expectedResult = None
      doAnswer(_ => expectedResult)
        .when(columnMappingUseCasesSpy)
        .checkErrors(Failure(exception), columnMappingDimensionCfgMock)

      val res =
        columnMappingUseCasesSpy.checkColumnMappingDimensionProperty(columnMappingDimensionCfgMock, columnsNumber)

      verify(columnMappingDimensionCfgMock, times(1)).errorMessages()
      verify(columnMappingUseCasesSpy, times(1))
        .getValidDimensionColumnsFromProperty(columnMappingDimensionCfgMock, columnsNumber)
      verify(columnMappingUseCasesSpy, times(1)).checkErrors(Failure(exception), columnMappingDimensionCfgMock)

      assert(res == expectedResult)
    }
    it("should return None if property already has an error message") {
      val columnMappingUseCasesSpy = spy(columnMappingUseCases)

      val columnMappingDimensionCfgMock = mock[ConfigValue]
      val columnsNumber = new ColumnsNumberMock().setNumber(5).build()

      val errorMessages = new util.ArrayList[String]()
      errorMessages.add("testError")
      doAnswer(_ => errorMessages).when(columnMappingDimensionCfgMock).errorMessages()

      val res =
        columnMappingUseCasesSpy.checkColumnMappingDimensionProperty(columnMappingDimensionCfgMock, columnsNumber)

      verify(columnMappingDimensionCfgMock, times(1)).errorMessages()
      verify(columnMappingUseCasesSpy, never)
        .getValidDimensionColumnsFromProperty(columnMappingDimensionCfgMock, columnsNumber)
      verify(columnMappingUseCasesSpy, never).checkErrors(
        ArgumentMatchers.any[Try[Option[Set[ValidDimensionColumn]]]],
        ArgumentMatchers.eq(columnMappingDimensionCfgMock)
      )

      assert(res.isEmpty)
    }
  }

  describe("checkColumnMappingMetricProperty") {
    it("should return a Some[Set[ValidMetricColumn]] if property is valid") {
      val columnMappingUseCasesSpy = spy(columnMappingUseCases)

      val columnMappingMetricCfgMock = mock[ConfigValue]
      val columnsNumber = new ColumnsNumberMock().setNumber(5).build()

      val errorMessages = new util.ArrayList[String]()
      doAnswer(_ => errorMessages).when(columnMappingMetricCfgMock).errorMessages()

      val validMetricColumns = Set(new ValidMetricColumnMock().build())
      doAnswer(_ => validMetricColumns)
        .when(columnMappingUseCasesSpy)
        .getValidMetricColumnsFromProperty(columnMappingMetricCfgMock, columnsNumber)

      val expectedResult = Some(validMetricColumns)
      doAnswer(_ => expectedResult)
        .when(columnMappingUseCasesSpy)
        .checkErrors(Success(expectedResult), columnMappingMetricCfgMock)

      val res =
        columnMappingUseCasesSpy.checkColumnMappingMetricProperty(columnMappingMetricCfgMock, columnsNumber)

      verify(columnMappingMetricCfgMock, times(1)).errorMessages()
      verify(columnMappingUseCasesSpy, times(1))
        .getValidMetricColumnsFromProperty(columnMappingMetricCfgMock, columnsNumber)
      verify(columnMappingUseCasesSpy, times(1)).checkErrors(Success(expectedResult), columnMappingMetricCfgMock)

      assert(res == expectedResult)
    }
    it("should return None if property leads to InvalidPropertyValueException") {
      val columnMappingUseCasesSpy = spy(columnMappingUseCases)

      val columnMappingMetricCfgMock = mock[ConfigValue]
      val columnsNumber = new ColumnsNumberMock().setNumber(5).build()

      val errorMessages = new util.ArrayList[String]()
      doAnswer(_ => errorMessages).when(columnMappingMetricCfgMock).errorMessages()

      val exception = InvalidPropertyValueException("test")
      doThrow(exception)
        .when(columnMappingUseCasesSpy)
        .getValidMetricColumnsFromProperty(columnMappingMetricCfgMock, columnsNumber)

      val expectedResult = None
      doAnswer(_ => expectedResult)
        .when(columnMappingUseCasesSpy)
        .checkErrors(Failure(exception), columnMappingMetricCfgMock)

      val res =
        columnMappingUseCasesSpy.checkColumnMappingMetricProperty(columnMappingMetricCfgMock, columnsNumber)

      verify(columnMappingMetricCfgMock, times(1)).errorMessages()
      verify(columnMappingUseCasesSpy, times(1))
        .getValidMetricColumnsFromProperty(columnMappingMetricCfgMock, columnsNumber)
      verify(columnMappingUseCasesSpy, times(1)).checkErrors(Failure(exception), columnMappingMetricCfgMock)

      assert(res == expectedResult)
    }
    it("should return None if property leads to ParseException") {
      val columnMappingUseCasesSpy = spy(columnMappingUseCases)

      val columnMappingMetricCfgMock = mock[ConfigValue]
      val columnsNumber = new ColumnsNumberMock().setNumber(5).build()

      val errorMessages = new util.ArrayList[String]()
      doAnswer(_ => errorMessages).when(columnMappingMetricCfgMock).errorMessages()

      val exception = new ParseException("test", new Exception())
      doThrow(exception)
        .when(columnMappingUseCasesSpy)
        .getValidMetricColumnsFromProperty(columnMappingMetricCfgMock, columnsNumber)

      val expectedResult = None
      doAnswer(_ => expectedResult)
        .when(columnMappingUseCasesSpy)
        .checkErrors(Failure(exception), columnMappingMetricCfgMock)

      val res =
        columnMappingUseCasesSpy.checkColumnMappingMetricProperty(columnMappingMetricCfgMock, columnsNumber)

      verify(columnMappingMetricCfgMock, times(1)).errorMessages()
      verify(columnMappingUseCasesSpy, times(1))
        .getValidMetricColumnsFromProperty(columnMappingMetricCfgMock, columnsNumber)
      verify(columnMappingUseCasesSpy, times(1)).checkErrors(Failure(exception), columnMappingMetricCfgMock)

      assert(res == expectedResult)
    }
    it("should return None if property already has an error message") {
      val columnMappingUseCasesSpy = spy(columnMappingUseCases)

      val columnMappingMetricCfgMock = mock[ConfigValue]
      val columnsNumber = new ColumnsNumberMock().setNumber(5).build()

      val errorMessages = new util.ArrayList[String]()
      errorMessages.add("testError")
      doAnswer(_ => errorMessages).when(columnMappingMetricCfgMock).errorMessages()

      val res =
        columnMappingUseCasesSpy.checkColumnMappingMetricProperty(columnMappingMetricCfgMock, columnsNumber)

      verify(columnMappingMetricCfgMock, times(1)).errorMessages()
      verify(columnMappingUseCasesSpy, never)
        .getValidMetricColumnsFromProperty(columnMappingMetricCfgMock, columnsNumber)
      verify(columnMappingUseCasesSpy, never).checkErrors(
        ArgumentMatchers.any[Try[Option[Set[ValidMetricColumn]]]],
        ArgumentMatchers.eq(columnMappingMetricCfgMock)
      )

      assert(res.isEmpty)
    }
  }

  describe("checkColumnMappingGroupedTasksColumnsProperty") {
    it("should return a Some[Option[ValidGroupedTasksColumns]] if property is valid") {
      val columnMappingUseCasesSpy = spy(columnMappingUseCases)

      val columnMappingGroupedTasksCfgMock = mock[ConfigValue]
      val columnsNumber = new ColumnsNumberMock().setNumber(5).build()

      val errorMessages = new util.ArrayList[String]()
      doAnswer(_ => errorMessages).when(columnMappingGroupedTasksCfgMock).errorMessages()

      val validGroupedTasksColumns = Some(new ValidGroupedTasksColumnsMock().build())
      doAnswer(_ => validGroupedTasksColumns)
        .when(columnMappingUseCasesSpy)
        .getValidGroupedTasksColumnsFromProperty(columnMappingGroupedTasksCfgMock, columnsNumber)

      val expectedResult = Some(validGroupedTasksColumns)
      doAnswer(_ => expectedResult)
        .when(columnMappingUseCasesSpy)
        .checkErrors(Success(expectedResult), columnMappingGroupedTasksCfgMock)

      val res =
        columnMappingUseCasesSpy.checkColumnMappingGroupedTasksColumnsProperty(
          columnMappingGroupedTasksCfgMock,
          columnsNumber
        )

      verify(columnMappingGroupedTasksCfgMock, times(1)).errorMessages()
      verify(columnMappingUseCasesSpy, times(1))
        .getValidGroupedTasksColumnsFromProperty(columnMappingGroupedTasksCfgMock, columnsNumber)
      verify(columnMappingUseCasesSpy, times(1)).checkErrors(Success(expectedResult), columnMappingGroupedTasksCfgMock)

      assert(res == expectedResult)
    }
    it("should return None if property leads to InvalidPropertyValueException") {
      val columnMappingUseCasesSpy = spy(columnMappingUseCases)

      val columnMappingGroupedTasksCfgMock = mock[ConfigValue]
      val columnsNumber = new ColumnsNumberMock().setNumber(5).build()

      val errorMessages = new util.ArrayList[String]()
      doAnswer(_ => errorMessages).when(columnMappingGroupedTasksCfgMock).errorMessages()

      val exception = InvalidPropertyValueException("test")
      doThrow(exception)
        .when(columnMappingUseCasesSpy)
        .getValidGroupedTasksColumnsFromProperty(columnMappingGroupedTasksCfgMock, columnsNumber)

      val expectedResult = None
      doAnswer(_ => expectedResult)
        .when(columnMappingUseCasesSpy)
        .checkErrors(Failure(exception), columnMappingGroupedTasksCfgMock)

      val res =
        columnMappingUseCasesSpy.checkColumnMappingGroupedTasksColumnsProperty(
          columnMappingGroupedTasksCfgMock,
          columnsNumber
        )

      verify(columnMappingGroupedTasksCfgMock, times(1)).errorMessages()
      verify(columnMappingUseCasesSpy, times(1))
        .getValidGroupedTasksColumnsFromProperty(columnMappingGroupedTasksCfgMock, columnsNumber)
      verify(columnMappingUseCasesSpy, times(1)).checkErrors(Failure(exception), columnMappingGroupedTasksCfgMock)

      assert(res == expectedResult)
    }
    it("should return None if property leads to ParseException") {
      val columnMappingUseCasesSpy = spy(columnMappingUseCases)

      val columnMappingGroupedTasksCfgMock = mock[ConfigValue]
      val columnsNumber = new ColumnsNumberMock().setNumber(5).build()

      val errorMessages = new util.ArrayList[String]()
      doAnswer(_ => errorMessages).when(columnMappingGroupedTasksCfgMock).errorMessages()

      val exception = new ParseException("test", new Exception())
      doThrow(exception)
        .when(columnMappingUseCasesSpy)
        .getValidGroupedTasksColumnsFromProperty(columnMappingGroupedTasksCfgMock, columnsNumber)

      val expectedResult = None
      doAnswer(_ => expectedResult)
        .when(columnMappingUseCasesSpy)
        .checkErrors(Failure(exception), columnMappingGroupedTasksCfgMock)

      val res =
        columnMappingUseCasesSpy.checkColumnMappingGroupedTasksColumnsProperty(
          columnMappingGroupedTasksCfgMock,
          columnsNumber
        )

      verify(columnMappingGroupedTasksCfgMock, times(1)).errorMessages()
      verify(columnMappingUseCasesSpy, times(1))
        .getValidGroupedTasksColumnsFromProperty(columnMappingGroupedTasksCfgMock, columnsNumber)
      verify(columnMappingUseCasesSpy, times(1)).checkErrors(Failure(exception), columnMappingGroupedTasksCfgMock)

      assert(res == expectedResult)
    }
    it("should return None if property already has an error message") {
      val columnMappingUseCasesSpy = spy(columnMappingUseCases)

      val columnMappingGroupedTasksCfgMock = mock[ConfigValue]
      val columnsNumber = new ColumnsNumberMock().setNumber(5).build()

      val errorMessages = new util.ArrayList[String]()
      errorMessages.add("testError")
      doAnswer(_ => errorMessages).when(columnMappingGroupedTasksCfgMock).errorMessages()

      val res =
        columnMappingUseCasesSpy.checkColumnMappingGroupedTasksColumnsProperty(
          columnMappingGroupedTasksCfgMock,
          columnsNumber
        )

      verify(columnMappingGroupedTasksCfgMock, times(1)).errorMessages()
      verify(columnMappingUseCasesSpy, never)
        .getValidGroupedTasksColumnsFromProperty(columnMappingGroupedTasksCfgMock, columnsNumber)
      verify(columnMappingUseCasesSpy, never).checkErrors(
        ArgumentMatchers.any[Try[Option[Set[ValidGroupedTasksColumns]]]],
        ArgumentMatchers.eq(columnMappingGroupedTasksCfgMock)
      )

      assert(res.isEmpty)
    }
  }

  describe("checkErrors") {
    it("should return the value in the Try if it was a Success") {
      val resultValue = Some(new ValidCaseIdColumnMock().build())
      val tryResult = Success(resultValue)
      val propertyConfigMock = mock[ConfigValue]

      doAnswer(_ => "name").when(propertyConfigMock).name()

      val res = columnMappingUseCases.checkErrors(tryResult, propertyConfigMock)

      verify(propertyConfigMock, never).addErrorMessage(ArgumentMatchers.any[String])

      assert(res == resultValue)
    }
    it("should add an error message and return None if InvalidPropertyValueException") {
      val tryResult = Failure(InvalidPropertyValueException("test"))
      val propertyConfigMock = mock[ConfigValue]

      doAnswer(_ => "name").when(propertyConfigMock).name()

      val res = columnMappingUseCases.checkErrors(tryResult, propertyConfigMock)

      verify(propertyConfigMock, times(1)).addErrorMessage(ArgumentMatchers.any[String])

      assert(res.isEmpty)
    }
    it("should add an error message and return None if ParseException") {
      val tryResult = Failure(new ParseException("test", new Exception()))
      val propertyConfigMock = mock[ConfigValue]

      doAnswer(_ => "name").when(propertyConfigMock).name()

      val res = columnMappingUseCases.checkErrors(tryResult, propertyConfigMock)

      verify(propertyConfigMock, times(1)).addErrorMessage(ArgumentMatchers.any[String])

      assert(res.isEmpty)
    }
    it("should add an error message and return None if unexpected exception") {
      val tryResult = Failure(new Exception())
      val propertyConfigMock = mock[ConfigValue]

      doAnswer(_ => "name").when(propertyConfigMock).name()

      val res = columnMappingUseCases.checkErrors(tryResult, propertyConfigMock)

      verify(propertyConfigMock, times(1)).addErrorMessage(ArgumentMatchers.any[String])

      assert(res.isEmpty)
    }
  }

  describe("checkColumnMappingCharacter") {
    it("should throw an InvalidPropertyValueException if the property has a default value") {
      val propertyName = "testProperty"
      val propertyValue = Constants.columnMappingStringDefaultValue
      assertThrows[InvalidPropertyValueException] {
        columnMappingUseCases.checkColumnMappingCharacter(propertyValue, propertyName)
      }
    }

    it("should throw an InvalidPropertyValueException if the value of the property has a length different than 1") {
      val propertyName = "testProperty"
      val propertyValue = "test"
      assertThrows[InvalidPropertyValueException] {
        columnMappingUseCases.checkColumnMappingCharacter(propertyValue, propertyName)
      }
    }

    it("should throw an InvalidPropertyValueException if the value of the property is empty") {
      val propertyName = "testProperty"
      val propertyValue = ""
      assertThrows[InvalidPropertyValueException] {
        columnMappingUseCases.checkColumnMappingCharacter(propertyValue, propertyName)
      }
    }
  }
}
