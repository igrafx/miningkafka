package com.igrafx.kafka.sink.aggregationmain.domain

import com.igrafx.kafka.sink.aggregationmain.Constants
import com.igrafx.kafka.sink.aggregationmain.domain.columnmapping.domain.entities._
import com.igrafx.kafka.sink.aggregationmain.domain.columnmapping.domain.entities.enums.{
  ColumnAggregation,
  DimensionAggregation,
  MetricAggregation
}
import com.igrafx.kafka.sink.aggregationmain.domain.entities.mocks._
import com.igrafx.kafka.sink.aggregationmain.domain.enums._
import com.igrafx.kafka.sink.aggregationmain.domain.exceptions.ColumnMappingAlreadyExistsException
import com.igrafx.kafka.sink.aggregationmain.domain.interfaces.MainApi
import com.igrafx.kafka.sink.aggregationmain.domain.usecases.ColumnMappingUseCases
import core.UnitTestSpec
import org.apache.kafka.common.config.{Config, ConfigException, ConfigValue}
import org.apache.kafka.connect.connector.Task
import org.apache.kafka.connect.sink.SinkTask
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.{times, verify, when}

import java.util
import scala.concurrent.Future
import scala.jdk.CollectionConverters._

class IGrafxAggregationSinkConnectorTest extends UnitTestSpec {
  class TestAggregationSinkConnector extends IGrafxAggregationSinkConnector {
    override val mainApi: MainApi = mock[MainApi]
  }

  class TestAggregationSinkConnectorColumnMapping extends IGrafxAggregationSinkConnector {
    override val mainApi: MainApi = mock[MainApi]
    override val columnMappingUseCases: ColumnMappingUseCases = mock[ColumnMappingUseCases]
  }

  private val aggregationSinkConnector = new TestAggregationSinkConnector

  val dateIndex1 = 2
  val dateFormat1 = "dd/MM/yy HH:mm"
  val date1Mock: ValidTimeColumn =
    new ValidTimeColumnMock()
      .setColumnIndex(new IndexMock().setIndex(dateIndex1).build())
      .setFormat(dateFormat1)
      .build()
  val dateIndex2 = 3
  val dateFormat2 = "dd/MM/yy HH:mm"
  val date2Mock: ValidTimeColumn =
    new ValidTimeColumnMock()
      .setColumnIndex(new IndexMock().setIndex(dateIndex2).build())
      .setFormat(dateFormat2)
      .build()
  val dateColumns: Seq[ValidTimeColumn] = Seq(date1Mock, date2Mock)
  val dateDefinition =
    s"${Constants.characterForElementStartInPropertyValue}$dateIndex1${Constants.delimiterForElementInPropertyValue}$dateFormat1${Constants.characterForElementEndInPropertyValue}${Constants.delimiterForPropertiesWithListValue}${Constants.characterForElementStartInPropertyValue}$dateIndex2${Constants.delimiterForElementInPropertyValue}$dateFormat2${Constants.characterForElementEndInPropertyValue}"

  val dimensionColumnIndex1 = 4
  val dimensionName1 = "Country"
  val dimensionIsCaseScope1 = false
  val dimension1Mock: ValidDimensionColumn = new ValidDimensionColumnMock()
    .setColumnIndex(new IndexMock().setIndex(dimensionColumnIndex1).build())
    .setName(new NonEmptyStringMock().setStringValue(dimensionName1).build())
    .setAggregationInformation(
      new DimensionAggregationInformationMock()
        .setAggregation(None)
        .setIsCaseScope(false)
        .build()
    )
    .build()
  val dimensionColumnIndex2 = 5
  val dimensionName2 = "Region"
  val dimensionIsCaseScope2 = true
  val dimensionAggregation2: DimensionAggregation = ColumnAggregation.FIRST
  val dimension2Mock: ValidDimensionColumn = new ValidDimensionColumnMock()
    .setColumnIndex(new IndexMock().setIndex(dimensionColumnIndex2).build())
    .setName(new NonEmptyStringMock().setStringValue(dimensionName2).build())
    .setAggregationInformation(
      new DimensionAggregationInformationMock()
        .setAggregation(Some(dimensionAggregation2))
        .setIsCaseScope(true)
        .build()
    )
    .build()
  val dimensionColumnIndex3 = 6
  val dimensionName3 = "City"
  val dimensionIsCaseScope3 = false
  val dimension3Mock: ValidDimensionColumn = new ValidDimensionColumnMock()
    .setColumnIndex(new IndexMock().setIndex(dimensionColumnIndex3).build())
    .setName(new NonEmptyStringMock().setStringValue(dimensionName3).build())
    .setAggregationInformation(
      new DimensionAggregationInformationMock()
        .setAggregation(None)
        .setIsCaseScope(false)
        .build()
    )
    .build()
  val dimensionColumns: Seq[ValidDimensionColumn] = Seq(dimension1Mock, dimension2Mock, dimension3Mock)
  val dimensionsJson: String =
    s"""[{"columnIndex": $dimensionColumnIndex1, "name": "$dimensionName1", "isCaseScope": $dimensionIsCaseScope1}, {"columnIndex": $dimensionColumnIndex2, "name": "$dimensionName2", "isCaseScope": $dimensionIsCaseScope2, "aggregation": "$dimensionAggregation2"}, {"columnIndex": $dimensionColumnIndex3, "name": "$dimensionName3", "isCaseScope": $dimensionIsCaseScope3}]"""

  val metricColumnIndex1 = 7
  val metricName1 = "DepartmentNumber"
  val metricIsCaseScope1 = false
  val metric1Mock: ValidMetricColumn = new ValidMetricColumnMock()
    .setColumnIndex(new IndexMock().setIndex(metricColumnIndex1).build())
    .setName(new NonEmptyStringMock().setStringValue(metricName1).build())
    .setAggregationInformation(
      new MetricAggregationInformationMock()
        .setAggregation(None)
        .setIsCaseScope(false)
        .build()
    )
    .build()
  val metricColumnIndex2 = 8
  val metricName2 = "Price"
  val metricUnit2 = "Euros"
  val metricIsCaseScope2 = true
  val metricAggregation2: MetricAggregation = ColumnAggregation.MIN
  val metric2Mock: ValidMetricColumn = new ValidMetricColumnMock()
    .setColumnIndex(new IndexMock().setIndex(metricColumnIndex2).build())
    .setName(new NonEmptyStringMock().setStringValue(metricName2).build())
    .setUnit(Some(metricUnit2))
    .setAggregationInformation(
      new MetricAggregationInformationMock()
        .setAggregation(Some(metricAggregation2))
        .setIsCaseScope(true)
        .build()
    )
    .build()
  val metricColumns: Seq[ValidMetricColumn] = Seq(metric1Mock, metric2Mock)
  val metricsJson: String =
    s"""[{"columnIndex": $metricColumnIndex1, "name": "$metricName1", "isCaseScope": $metricIsCaseScope1}, {"columnIndex": $metricColumnIndex2, "name": "$metricName2", "unit": "$metricUnit2", "isCaseScope": $metricIsCaseScope2, "aggregation": "$metricAggregation2"}]"""

  val map: util.Map[String, String] = createPropertiesMap()
  val mapColumnMapping: util.Map[String, String] = createColumnMappingPropertiesMap()
  val mapNoHeader: util.Map[String, String] = createPropertiesMapNoHeader()
  val mapColumnMappingNoHeader: util.Map[String, String] = createColumnMappingPropertiesMapNoHeader()
  val mapWithLogging: util.Map[String, String] = createPropertiesMapWithLogging()
  val mapColumnMappingWithLogging: util.Map[String, String] = createColumnMappingPropertiesMapWithLogging()

  override def beforeAll(): Unit = {
    aggregationSinkConnector.validate(map)
    aggregationSinkConnector.start(map)
    super.beforeAll()
  }

  override def afterAll(): Unit = {
    aggregationSinkConnector.stop()
    super.afterAll()
  }

  private def createPropertiesMap(): util.Map[String, String] = {
    val map: util.Map[String, String] = new util.HashMap[String, String]()

    map.put("name", "connectorName")
    map.put(ConnectorPropertiesEnum.apiUrlProperty.toStringDescription, "api_url_example")
    map.put(ConnectorPropertiesEnum.authUrlProperty.toStringDescription, "auth_url_example")
    map.put(ConnectorPropertiesEnum.workGroupIdProperty.toStringDescription, "w_id_example")
    map.put(ConnectorPropertiesEnum.workGroupKeyProperty.toStringDescription, "w_key_example")
    map.put(ConnectorPropertiesEnum.projectIdProperty.toStringDescription, "0f61e0f3-68c8-4c7f-bc1d-a4aabf7b74e5")
    map.put("topics", "topic_example")
    map.put(ConnectorPropertiesEnum.csvEncodingProperty.toStringDescription, "UTF8")
    map.put(ConnectorPropertiesEnum.csvSeparatorProperty.toStringDescription, ",")
    map.put(ConnectorPropertiesEnum.csvQuoteProperty.toStringDescription, "\"")
    map.put(ConnectorPropertiesEnum.csvFieldsNumberProperty.toStringDescription, "4")
    map.put(ConnectorPropertiesEnum.csvHeaderProperty.toStringDescription, "true")
    map.put(ConnectorPropertiesEnum.csvDefaultTextValueProperty.toStringDescription, " ")
    map.put(ConnectorPropertiesEnum.retentionTimeInDayProperty.toStringDescription, "100")
    map.put(ConnectorPropertiesEnum.elementNumberThresholdProperty.toStringDescription, "10")
    map.put(ConnectorPropertiesEnum.valuePatternThresholdProperty.toStringDescription, "")
    map.put(ConnectorPropertiesEnum.timeoutInSecondsThresholdProperty.toStringDescription, "10")
    map.put(ConnectorPropertiesEnum.bootstrapServersProperty.toStringDescription, "bootstrapServersExample")
    map.put(Constants.schemaRegistryUrlProperty, "schemaPropertyUrlExample")

    map
  }

  private def createColumnMappingPropertiesMap(): util.Map[String, String] = {
    val map: util.Map[String, String] = new util.HashMap[String, String]()

    map.put("name", "connectorName")
    map.put(ConnectorPropertiesEnum.apiUrlProperty.toStringDescription, "api_url_example")
    map.put(ConnectorPropertiesEnum.authUrlProperty.toStringDescription, "auth_url_example")
    map.put(ConnectorPropertiesEnum.workGroupIdProperty.toStringDescription, "w_id_example")
    map.put(ConnectorPropertiesEnum.workGroupKeyProperty.toStringDescription, "w_key_example")
    map.put(ConnectorPropertiesEnum.projectIdProperty.toStringDescription, "0f61e0f3-68c8-4c7f-bc1d-a4aabf7b74e5")
    map.put("topics", "topic_example")
    map.put(ConnectorPropertiesEnum.csvEncodingProperty.toStringDescription, "UTF8")
    map.put(ConnectorPropertiesEnum.csvSeparatorProperty.toStringDescription, ",")
    map.put(ConnectorPropertiesEnum.csvQuoteProperty.toStringDescription, "\"")
    map.put(ConnectorPropertiesEnum.csvFieldsNumberProperty.toStringDescription, "9")
    map.put(ConnectorPropertiesEnum.csvHeaderProperty.toStringDescription, "true")
    map.put(ConnectorPropertiesEnum.csvDefaultTextValueProperty.toStringDescription, " ")
    map.put(ConnectorPropertiesEnum.retentionTimeInDayProperty.toStringDescription, "100")
    map.put(ConnectorPropertiesEnum.columnMappingCreateProperty.toStringDescription, "true")
    map.put(ConnectorPropertiesEnum.columnMappingCaseIdColumnProperty.toStringDescription, "0")
    map.put(ConnectorPropertiesEnum.columnMappingActivityColumnProperty.toStringDescription, "1")
    map.put(
      ConnectorPropertiesEnum.columnMappingTimeColumnsProperty.toStringDescription,
      dateDefinition
    )
    map.put(
      ConnectorPropertiesEnum.columnMappingDimensionColumnsProperty.toStringDescription,
      dimensionsJson
    )
    map.put(
      ConnectorPropertiesEnum.columnMappingMetricColumnsProperty.toStringDescription,
      metricsJson
    )
    map.put(ConnectorPropertiesEnum.csvEndOfLineProperty.toStringDescription, "\\n")
    map.put(ConnectorPropertiesEnum.csvEscapeProperty.toStringDescription, "\\")
    map.put(ConnectorPropertiesEnum.csvCommentProperty.toStringDescription, "#")
    map.put(ConnectorPropertiesEnum.elementNumberThresholdProperty.toStringDescription, "10")
    map.put(ConnectorPropertiesEnum.valuePatternThresholdProperty.toStringDescription, "")
    map.put(ConnectorPropertiesEnum.timeoutInSecondsThresholdProperty.toStringDescription, "10")
    map.put(ConnectorPropertiesEnum.bootstrapServersProperty.toStringDescription, "bootstrapServersExample")
    map.put(Constants.schemaRegistryUrlProperty, "schemaPropertyUrlExample")

    map
  }

  private def createPropertiesMapNoHeader(): util.Map[String, String] = {
    val map: util.Map[String, String] = new util.HashMap[String, String]()

    map.put("name", "connectorName")
    map.put(ConnectorPropertiesEnum.apiUrlProperty.toStringDescription, "api_url_example")
    map.put(ConnectorPropertiesEnum.authUrlProperty.toStringDescription, "auth_url_example")
    map.put(ConnectorPropertiesEnum.workGroupIdProperty.toStringDescription, "w_id_example")
    map.put(ConnectorPropertiesEnum.workGroupKeyProperty.toStringDescription, "w_key_example")
    map.put(ConnectorPropertiesEnum.projectIdProperty.toStringDescription, "0f61e0f3-68c8-4c7f-bc1d-a4aabf7b74e5")
    map.put("topics", "topic_example")
    map.put(ConnectorPropertiesEnum.csvEncodingProperty.toStringDescription, "UTF8")
    map.put(ConnectorPropertiesEnum.csvSeparatorProperty.toStringDescription, ",")
    map.put(ConnectorPropertiesEnum.csvQuoteProperty.toStringDescription, "\"")
    map.put(ConnectorPropertiesEnum.csvFieldsNumberProperty.toStringDescription, "4")
    map.put(ConnectorPropertiesEnum.csvHeaderProperty.toStringDescription, "false")
    map.put(ConnectorPropertiesEnum.csvDefaultTextValueProperty.toStringDescription, " ")
    map.put(ConnectorPropertiesEnum.retentionTimeInDayProperty.toStringDescription, "100")
    map.put(ConnectorPropertiesEnum.elementNumberThresholdProperty.toStringDescription, "10")
    map.put(ConnectorPropertiesEnum.valuePatternThresholdProperty.toStringDescription, "")
    map.put(ConnectorPropertiesEnum.timeoutInSecondsThresholdProperty.toStringDescription, "10")
    map.put(ConnectorPropertiesEnum.bootstrapServersProperty.toStringDescription, "bootstrapServersExample")
    map.put(Constants.schemaRegistryUrlProperty, "schemaPropertyUrlExample")

    map
  }

  private def createColumnMappingPropertiesMapNoHeader(): util.Map[String, String] = {
    val map: util.Map[String, String] = new util.HashMap[String, String]()

    map.put("name", "connectorName")
    map.put(ConnectorPropertiesEnum.apiUrlProperty.toStringDescription, "api_url_example")
    map.put(ConnectorPropertiesEnum.authUrlProperty.toStringDescription, "auth_url_example")
    map.put(ConnectorPropertiesEnum.workGroupIdProperty.toStringDescription, "w_id_example")
    map.put(ConnectorPropertiesEnum.workGroupKeyProperty.toStringDescription, "w_key_example")
    map.put(ConnectorPropertiesEnum.projectIdProperty.toStringDescription, "0f61e0f3-68c8-4c7f-bc1d-a4aabf7b74e5")
    map.put("topics", "topic_example")
    map.put(ConnectorPropertiesEnum.csvEncodingProperty.toStringDescription, "UTF8")
    map.put(ConnectorPropertiesEnum.csvSeparatorProperty.toStringDescription, ",")
    map.put(ConnectorPropertiesEnum.csvQuoteProperty.toStringDescription, "\"")
    map.put(ConnectorPropertiesEnum.csvFieldsNumberProperty.toStringDescription, "9")
    map.put(ConnectorPropertiesEnum.csvHeaderProperty.toStringDescription, "false")
    map.put(ConnectorPropertiesEnum.csvDefaultTextValueProperty.toStringDescription, " ")
    map.put(ConnectorPropertiesEnum.retentionTimeInDayProperty.toStringDescription, "100")
    map.put(ConnectorPropertiesEnum.columnMappingCreateProperty.toStringDescription, "true")
    map.put(ConnectorPropertiesEnum.columnMappingCaseIdColumnProperty.toStringDescription, "0")
    map.put(ConnectorPropertiesEnum.columnMappingActivityColumnProperty.toStringDescription, "1")
    map.put(
      ConnectorPropertiesEnum.columnMappingTimeColumnsProperty.toStringDescription,
      dateDefinition
    )
    map.put(
      ConnectorPropertiesEnum.columnMappingDimensionColumnsProperty.toStringDescription,
      dimensionsJson
    )
    map.put(
      ConnectorPropertiesEnum.columnMappingMetricColumnsProperty.toStringDescription,
      metricsJson
    )
    map.put(ConnectorPropertiesEnum.csvEndOfLineProperty.toStringDescription, "\\n")
    map.put(ConnectorPropertiesEnum.csvEscapeProperty.toStringDescription, "\\")
    map.put(ConnectorPropertiesEnum.csvCommentProperty.toStringDescription, "#")
    map.put(ConnectorPropertiesEnum.elementNumberThresholdProperty.toStringDescription, "10")
    map.put(ConnectorPropertiesEnum.valuePatternThresholdProperty.toStringDescription, "")
    map.put(ConnectorPropertiesEnum.timeoutInSecondsThresholdProperty.toStringDescription, "10")
    map.put(ConnectorPropertiesEnum.bootstrapServersProperty.toStringDescription, "bootstrapServersExample")
    map.put(Constants.schemaRegistryUrlProperty, "schemaPropertyUrlExample")

    map
  }

  private def createPropertiesMapWithLogging(): util.Map[String, String] = {
    val map: util.Map[String, String] = new util.HashMap[String, String]()

    map.put("name", "connectorName")
    map.put(ConnectorPropertiesEnum.apiUrlProperty.toStringDescription, "api_url_example")
    map.put(ConnectorPropertiesEnum.authUrlProperty.toStringDescription, "auth_url_example")
    map.put(ConnectorPropertiesEnum.workGroupIdProperty.toStringDescription, "w_id_example")
    map.put(ConnectorPropertiesEnum.workGroupKeyProperty.toStringDescription, "w_key_example")
    map.put(ConnectorPropertiesEnum.projectIdProperty.toStringDescription, "0f61e0f3-68c8-4c7f-bc1d-a4aabf7b74e5")
    map.put("topics", "topic_example")
    map.put(ConnectorPropertiesEnum.csvEncodingProperty.toStringDescription, "UTF8")
    map.put(ConnectorPropertiesEnum.csvSeparatorProperty.toStringDescription, ",")
    map.put(ConnectorPropertiesEnum.csvQuoteProperty.toStringDescription, "\"")
    map.put(ConnectorPropertiesEnum.csvFieldsNumberProperty.toStringDescription, "4")
    map.put(ConnectorPropertiesEnum.csvHeaderProperty.toStringDescription, "false")
    map.put(ConnectorPropertiesEnum.csvDefaultTextValueProperty.toStringDescription, " ")
    map.put(ConnectorPropertiesEnum.retentionTimeInDayProperty.toStringDescription, "100")

    map.put(ConnectorPropertiesEnum.kafkaLoggingEventsIsLoggingProperty.toStringDescription, "true")
    map.put(ConnectorPropertiesEnum.kafkaLoggingEventsTopicProperty.toStringDescription, "loggingTopicPropertyValue")
    map.put(ConnectorPropertiesEnum.elementNumberThresholdProperty.toStringDescription, "10")
    map.put(ConnectorPropertiesEnum.valuePatternThresholdProperty.toStringDescription, "")
    map.put(ConnectorPropertiesEnum.timeoutInSecondsThresholdProperty.toStringDescription, "10")
    map.put(ConnectorPropertiesEnum.bootstrapServersProperty.toStringDescription, "bootstrapServersExample")
    map.put(Constants.schemaRegistryUrlProperty, "schemaPropertyUrlExample")

    map
  }

  private def createColumnMappingPropertiesMapWithLogging(): util.Map[String, String] = {
    val map: util.Map[String, String] = new util.HashMap[String, String]()

    map.put("name", "connectorName")
    map.put(ConnectorPropertiesEnum.apiUrlProperty.toStringDescription, "api_url_example")
    map.put(ConnectorPropertiesEnum.authUrlProperty.toStringDescription, "auth_url_example")
    map.put(ConnectorPropertiesEnum.workGroupIdProperty.toStringDescription, "w_id_example")
    map.put(ConnectorPropertiesEnum.workGroupKeyProperty.toStringDescription, "w_key_example")
    map.put(ConnectorPropertiesEnum.projectIdProperty.toStringDescription, "0f61e0f3-68c8-4c7f-bc1d-a4aabf7b74e5")
    map.put("topics", "topic_example")
    map.put(ConnectorPropertiesEnum.csvEncodingProperty.toStringDescription, "UTF8")
    map.put(ConnectorPropertiesEnum.csvSeparatorProperty.toStringDescription, ",")
    map.put(ConnectorPropertiesEnum.csvQuoteProperty.toStringDescription, "\"")
    map.put(ConnectorPropertiesEnum.csvFieldsNumberProperty.toStringDescription, "9")
    map.put(ConnectorPropertiesEnum.csvHeaderProperty.toStringDescription, "false")
    map.put(ConnectorPropertiesEnum.csvDefaultTextValueProperty.toStringDescription, " ")
    map.put(ConnectorPropertiesEnum.retentionTimeInDayProperty.toStringDescription, "100")
    map.put(ConnectorPropertiesEnum.columnMappingCreateProperty.toStringDescription, "true")
    map.put(ConnectorPropertiesEnum.columnMappingCaseIdColumnProperty.toStringDescription, "0")
    map.put(ConnectorPropertiesEnum.columnMappingActivityColumnProperty.toStringDescription, "1")
    map.put(
      ConnectorPropertiesEnum.columnMappingTimeColumnsProperty.toStringDescription,
      dateDefinition
    )
    map.put(
      ConnectorPropertiesEnum.columnMappingDimensionColumnsProperty.toStringDescription,
      dimensionsJson
    )
    map.put(
      ConnectorPropertiesEnum.columnMappingMetricColumnsProperty.toStringDescription,
      metricsJson
    )
    map.put(ConnectorPropertiesEnum.csvEndOfLineProperty.toStringDescription, "\\n")
    map.put(ConnectorPropertiesEnum.csvEscapeProperty.toStringDescription, "\\")
    map.put(ConnectorPropertiesEnum.csvCommentProperty.toStringDescription, "#")

    map.put(ConnectorPropertiesEnum.kafkaLoggingEventsIsLoggingProperty.toStringDescription, "true")
    map.put(ConnectorPropertiesEnum.kafkaLoggingEventsTopicProperty.toStringDescription, "loggingTopicPropertyValue")
    map.put(ConnectorPropertiesEnum.elementNumberThresholdProperty.toStringDescription, "10")
    map.put(ConnectorPropertiesEnum.valuePatternThresholdProperty.toStringDescription, "")
    map.put(ConnectorPropertiesEnum.timeoutInSecondsThresholdProperty.toStringDescription, "10")
    map.put(ConnectorPropertiesEnum.bootstrapServersProperty.toStringDescription, "bootstrapServersExample")
    map.put(Constants.schemaRegistryUrlProperty, "schemaPropertyUrlExample")

    map
  }

  describe("taskClass") {
    it("should return a SinkTask") {
      val task: Task = aggregationSinkConnector.taskClass().getDeclaredConstructor().newInstance()
      assert(task.isInstanceOf[SinkTask])
    }
  }

  describe("taskConfigs") {
    it("should be able to start Tasks with the correct properties") {
      // with header and without Logging

      val properties: util.List[util.Map[String, String]] = aggregationSinkConnector.taskConfigs(3)
      assert(properties.asScala.forall { map: util.Map[String, String] =>
        map.containsKey("name")
        map.containsKey(ConnectorPropertiesEnum.apiUrlProperty.toStringDescription)
        map.containsKey(ConnectorPropertiesEnum.authUrlProperty.toStringDescription)
        map.containsKey(ConnectorPropertiesEnum.workGroupIdProperty.toStringDescription)
        map.containsKey(ConnectorPropertiesEnum.workGroupKeyProperty.toStringDescription)
        map.containsKey(ConnectorPropertiesEnum.projectIdProperty.toStringDescription)
        map.containsKey(ConnectorPropertiesEnum.csvEncodingProperty.toStringDescription)
        map.containsKey(ConnectorPropertiesEnum.csvSeparatorProperty.toStringDescription)
        map.containsKey(ConnectorPropertiesEnum.csvQuoteProperty.toStringDescription)
        map.containsKey(ConnectorPropertiesEnum.csvFieldsNumberProperty.toStringDescription)
        map.containsKey(ConnectorPropertiesEnum.csvHeaderProperty.toStringDescription)
        map.containsKey(ConnectorPropertiesEnum.csvDefaultTextValueProperty.toStringDescription)
        map.containsKey(ConnectorPropertiesEnum.retentionTimeInDayProperty.toStringDescription)
        map.containsKey(Constants.headerValueProperty)
        !map.containsKey(ConnectorPropertiesEnum.kafkaLoggingEventsTopicProperty.toStringDescription)
        map.containsKey(ConnectorPropertiesEnum.elementNumberThresholdProperty.toStringDescription)
        map.containsKey(ConnectorPropertiesEnum.valuePatternThresholdProperty.toStringDescription)
        map.containsKey(ConnectorPropertiesEnum.timeoutInSecondsThresholdProperty.toStringDescription)
        map.containsKey(ConnectorPropertiesEnum.bootstrapServersProperty.toStringDescription)
        map.containsKey(Constants.schemaRegistryUrlProperty)
      })

      val sinkConnectorColumnMapping = new TestAggregationSinkConnectorColumnMapping
      when(sinkConnectorColumnMapping.columnMappingUseCases.createColumnMapping(any(), any()))
        .thenReturn(Future.successful(()))

      sinkConnectorColumnMapping.validate(mapColumnMapping)
      sinkConnectorColumnMapping.start(mapColumnMapping)
      val properties2: util.List[util.Map[String, String]] = sinkConnectorColumnMapping.taskConfigs(3)
      assert(properties2.asScala.forall { map: util.Map[String, String] =>
        map.containsKey("name")
        map.containsKey(ConnectorPropertiesEnum.apiUrlProperty.toStringDescription)
        map.containsKey(ConnectorPropertiesEnum.authUrlProperty.toStringDescription)
        map.containsKey(ConnectorPropertiesEnum.workGroupIdProperty.toStringDescription)
        map.containsKey(ConnectorPropertiesEnum.workGroupKeyProperty.toStringDescription)
        map.containsKey(ConnectorPropertiesEnum.projectIdProperty.toStringDescription)
        map.containsKey(ConnectorPropertiesEnum.csvEncodingProperty.toStringDescription)
        map.containsKey(ConnectorPropertiesEnum.csvSeparatorProperty.toStringDescription)
        map.containsKey(ConnectorPropertiesEnum.csvQuoteProperty.toStringDescription)
        map.containsKey(ConnectorPropertiesEnum.csvFieldsNumberProperty.toStringDescription)
        map.containsKey(ConnectorPropertiesEnum.csvHeaderProperty.toStringDescription)
        map.containsKey(ConnectorPropertiesEnum.csvDefaultTextValueProperty.toStringDescription)
        map.containsKey(ConnectorPropertiesEnum.retentionTimeInDayProperty.toStringDescription)
        map.containsKey(Constants.headerValueProperty)
        !map.containsKey(ConnectorPropertiesEnum.kafkaLoggingEventsTopicProperty.toStringDescription)
        map.containsKey(ConnectorPropertiesEnum.elementNumberThresholdProperty.toStringDescription)
        map.containsKey(ConnectorPropertiesEnum.valuePatternThresholdProperty.toStringDescription)
        map.containsKey(ConnectorPropertiesEnum.timeoutInSecondsThresholdProperty.toStringDescription)
        map.containsKey(ConnectorPropertiesEnum.bootstrapServersProperty.toStringDescription)
        map.containsKey(Constants.schemaRegistryUrlProperty)
      })
      sinkConnectorColumnMapping.stop()

      // without header

      val sinkConnectorNoHeader = new TestAggregationSinkConnector
      sinkConnectorNoHeader.validate(mapNoHeader)
      sinkConnectorNoHeader.start(mapNoHeader)
      val properties3: util.List[util.Map[String, String]] = sinkConnectorNoHeader.taskConfigs(3)
      assert(properties3.asScala.forall { map: util.Map[String, String] =>
        map.containsKey("name")
        map.containsKey(ConnectorPropertiesEnum.apiUrlProperty.toStringDescription)
        map.containsKey(ConnectorPropertiesEnum.authUrlProperty.toStringDescription)
        map.containsKey(ConnectorPropertiesEnum.workGroupIdProperty.toStringDescription)
        map.containsKey(ConnectorPropertiesEnum.workGroupKeyProperty.toStringDescription)
        map.containsKey(ConnectorPropertiesEnum.projectIdProperty.toStringDescription)
        map.containsKey(ConnectorPropertiesEnum.csvEncodingProperty.toStringDescription)
        map.containsKey(ConnectorPropertiesEnum.csvSeparatorProperty.toStringDescription)
        map.containsKey(ConnectorPropertiesEnum.csvQuoteProperty.toStringDescription)
        map.containsKey(ConnectorPropertiesEnum.csvFieldsNumberProperty.toStringDescription)
        map.containsKey(ConnectorPropertiesEnum.csvHeaderProperty.toStringDescription)
        map.containsKey(ConnectorPropertiesEnum.csvDefaultTextValueProperty.toStringDescription)
        map.containsKey(ConnectorPropertiesEnum.retentionTimeInDayProperty.toStringDescription)
        !map.containsKey(Constants.headerValueProperty)
        map.containsKey(ConnectorPropertiesEnum.elementNumberThresholdProperty.toStringDescription)
        map.containsKey(ConnectorPropertiesEnum.valuePatternThresholdProperty.toStringDescription)
        map.containsKey(ConnectorPropertiesEnum.timeoutInSecondsThresholdProperty.toStringDescription)
        map.containsKey(ConnectorPropertiesEnum.bootstrapServersProperty.toStringDescription)
        map.containsKey(Constants.schemaRegistryUrlProperty)
      })
      sinkConnectorNoHeader.stop()

      val sinkConnectorColumnMappingNoHeader = new TestAggregationSinkConnectorColumnMapping
      when(sinkConnectorColumnMappingNoHeader.columnMappingUseCases.createColumnMapping(any(), any()))
        .thenReturn(
          Future.successful(())
        )

      sinkConnectorColumnMappingNoHeader.validate(mapColumnMappingNoHeader)
      sinkConnectorColumnMappingNoHeader.start(mapColumnMappingNoHeader)
      val properties4: util.List[util.Map[String, String]] = sinkConnectorColumnMappingNoHeader.taskConfigs(3)
      assert(properties4.asScala.forall { map: util.Map[String, String] =>
        map.containsKey("name")
        map.containsKey(ConnectorPropertiesEnum.apiUrlProperty.toStringDescription)
        map.containsKey(ConnectorPropertiesEnum.authUrlProperty.toStringDescription)
        map.containsKey(ConnectorPropertiesEnum.workGroupIdProperty.toStringDescription)
        map.containsKey(ConnectorPropertiesEnum.workGroupKeyProperty.toStringDescription)
        map.containsKey(ConnectorPropertiesEnum.projectIdProperty.toStringDescription)
        map.containsKey(ConnectorPropertiesEnum.csvEncodingProperty.toStringDescription)
        map.containsKey(ConnectorPropertiesEnum.csvSeparatorProperty.toStringDescription)
        map.containsKey(ConnectorPropertiesEnum.csvQuoteProperty.toStringDescription)
        map.containsKey(ConnectorPropertiesEnum.csvFieldsNumberProperty.toStringDescription)
        map.containsKey(ConnectorPropertiesEnum.csvHeaderProperty.toStringDescription)
        map.containsKey(ConnectorPropertiesEnum.csvDefaultTextValueProperty.toStringDescription)
        map.containsKey(ConnectorPropertiesEnum.retentionTimeInDayProperty.toStringDescription)
        !map.containsKey(Constants.headerValueProperty)
        map.containsKey(ConnectorPropertiesEnum.elementNumberThresholdProperty.toStringDescription)
        map.containsKey(ConnectorPropertiesEnum.valuePatternThresholdProperty.toStringDescription)
        map.containsKey(ConnectorPropertiesEnum.timeoutInSecondsThresholdProperty.toStringDescription)
        map.containsKey(ConnectorPropertiesEnum.bootstrapServersProperty.toStringDescription)
        map.containsKey(Constants.schemaRegistryUrlProperty)
      })
      sinkConnectorColumnMappingNoHeader.stop()

      // With Logging
      val sinkConnectorWithLogging = new TestAggregationSinkConnector
      sinkConnectorWithLogging.validate(mapWithLogging)
      sinkConnectorWithLogging.start(mapWithLogging)
      val properties5: util.List[util.Map[String, String]] = sinkConnectorWithLogging.taskConfigs(3)
      assert(properties5.asScala.forall { map: util.Map[String, String] =>
        map.containsKey("name")
        map.containsKey(ConnectorPropertiesEnum.apiUrlProperty.toStringDescription)
        map.containsKey(ConnectorPropertiesEnum.authUrlProperty.toStringDescription)
        map.containsKey(ConnectorPropertiesEnum.workGroupIdProperty.toStringDescription)
        map.containsKey(ConnectorPropertiesEnum.workGroupKeyProperty.toStringDescription)
        map.containsKey(ConnectorPropertiesEnum.projectIdProperty.toStringDescription)
        map.containsKey(ConnectorPropertiesEnum.csvEncodingProperty.toStringDescription)
        map.containsKey(ConnectorPropertiesEnum.csvSeparatorProperty.toStringDescription)
        map.containsKey(ConnectorPropertiesEnum.csvQuoteProperty.toStringDescription)
        map.containsKey(ConnectorPropertiesEnum.csvFieldsNumberProperty.toStringDescription)
        map.containsKey(ConnectorPropertiesEnum.csvHeaderProperty.toStringDescription)
        map.containsKey(ConnectorPropertiesEnum.csvDefaultTextValueProperty.toStringDescription)
        map.containsKey(ConnectorPropertiesEnum.retentionTimeInDayProperty.toStringDescription)
        map.containsKey(ConnectorPropertiesEnum.kafkaLoggingEventsTopicProperty.toStringDescription)
        map.containsKey(ConnectorPropertiesEnum.elementNumberThresholdProperty.toStringDescription)
        map.containsKey(ConnectorPropertiesEnum.valuePatternThresholdProperty.toStringDescription)
        map.containsKey(ConnectorPropertiesEnum.timeoutInSecondsThresholdProperty.toStringDescription)
        map.containsKey(ConnectorPropertiesEnum.bootstrapServersProperty.toStringDescription)
        map.containsKey(Constants.schemaRegistryUrlProperty)
      })
      sinkConnectorWithLogging.stop()

      val sinkConnectorColumnMappingWithLogging = new TestAggregationSinkConnectorColumnMapping
      when(sinkConnectorColumnMappingWithLogging.columnMappingUseCases.createColumnMapping(any(), any()))
        .thenReturn(
          Future.successful(())
        )

      sinkConnectorColumnMappingWithLogging.validate(mapColumnMappingWithLogging)
      sinkConnectorColumnMappingWithLogging.start(mapColumnMappingWithLogging)
      val properties6: util.List[util.Map[String, String]] = sinkConnectorColumnMappingWithLogging.taskConfigs(3)
      assert(properties6.asScala.forall { map: util.Map[String, String] =>
        map.containsKey("name")
        map.containsKey(ConnectorPropertiesEnum.apiUrlProperty.toStringDescription)
        map.containsKey(ConnectorPropertiesEnum.authUrlProperty.toStringDescription)
        map.containsKey(ConnectorPropertiesEnum.workGroupIdProperty.toStringDescription)
        map.containsKey(ConnectorPropertiesEnum.workGroupKeyProperty.toStringDescription)
        map.containsKey(ConnectorPropertiesEnum.projectIdProperty.toStringDescription)
        map.containsKey(ConnectorPropertiesEnum.csvEncodingProperty.toStringDescription)
        map.containsKey(ConnectorPropertiesEnum.csvSeparatorProperty.toStringDescription)
        map.containsKey(ConnectorPropertiesEnum.csvQuoteProperty.toStringDescription)
        map.containsKey(ConnectorPropertiesEnum.csvFieldsNumberProperty.toStringDescription)
        map.containsKey(ConnectorPropertiesEnum.csvHeaderProperty.toStringDescription)
        map.containsKey(ConnectorPropertiesEnum.csvDefaultTextValueProperty.toStringDescription)
        map.containsKey(ConnectorPropertiesEnum.retentionTimeInDayProperty.toStringDescription)
        map.containsKey(ConnectorPropertiesEnum.kafkaLoggingEventsTopicProperty.toStringDescription)
        map.containsKey(ConnectorPropertiesEnum.elementNumberThresholdProperty.toStringDescription)
        map.containsKey(ConnectorPropertiesEnum.valuePatternThresholdProperty.toStringDescription)
        map.containsKey(ConnectorPropertiesEnum.timeoutInSecondsThresholdProperty.toStringDescription)
        map.containsKey(ConnectorPropertiesEnum.bootstrapServersProperty.toStringDescription)
        map.containsKey(Constants.schemaRegistryUrlProperty)
      })
      sinkConnectorColumnMappingWithLogging.stop()

      assert(true)
    }

    it("should return a number of configurations equal to maxTasks") {
      for (i <- 1 until 4) {
        assert(aggregationSinkConnector.taskConfigs(i).size() == i)
      }
      assert(true)
    }
  }

  describe("validate") {
    it("should add an error to the configuration if two different columns have the same index") {
      val connector: IGrafxAggregationSinkConnector = new TestAggregationSinkConnector()

      val map: util.Map[String, String] = createColumnMappingPropertiesMap()
      map.put(ConnectorPropertiesEnum.columnMappingActivityColumnProperty.toStringDescription, "0")

      val config: Config = connector.validate(map)
      val configValues = config.configValues().asScala
      val createConfig =
        connector.getConfigValue(configValues, ConnectorPropertiesEnum.columnMappingCreateProperty.toStringDescription)
      assert(createConfig.errorMessages().size() != 0)
    }

    it(
      s"should add an error to the configuration if there is a missing column according to the ${ConnectorPropertiesEnum.csvFieldsNumberProperty.toStringDescription} property"
    ) {
      val connector: IGrafxAggregationSinkConnector = new TestAggregationSinkConnector()

      val map: util.Map[String, String] = createColumnMappingPropertiesMap()
      map.remove(ConnectorPropertiesEnum.columnMappingDimensionColumnsProperty.toStringDescription)

      val config: Config = connector.validate(map)
      val configValues = config.configValues().asScala
      val createConfig =
        connector.getConfigValue(configValues, ConnectorPropertiesEnum.columnMappingCreateProperty.toStringDescription)
      assert(createConfig.errorMessages().size() != 0)
    }

    it(
      s"should add an error to the configuration if a mandatory property is undefined while the ${ConnectorPropertiesEnum.columnMappingCreateProperty.toStringDescription} property is true"
    ) {
      val connector: IGrafxAggregationSinkConnector = new TestAggregationSinkConnector()

      val map: util.Map[String, String] = createColumnMappingPropertiesMap()

      map.remove(ConnectorPropertiesEnum.columnMappingCaseIdColumnProperty.toStringDescription)
      val caseIdConfig: Config = connector.validate(map)
      val caseIdConfigValues = caseIdConfig.configValues().asScala
      val caseIdConfigValue: ConfigValue =
        connector.getConfigValue(
          caseIdConfigValues,
          ConnectorPropertiesEnum.columnMappingCaseIdColumnProperty.toStringDescription
        )
      assert(caseIdConfigValue.errorMessages().size() != 0)
      map.put(ConnectorPropertiesEnum.columnMappingCaseIdColumnProperty.toStringDescription, "0")

      map.remove(ConnectorPropertiesEnum.columnMappingActivityColumnProperty.toStringDescription)
      val activityConfig: Config = connector.validate(map)
      val activityConfigValues = activityConfig.configValues().asScala
      val activityConfigValue: ConfigValue =
        connector.getConfigValue(
          activityConfigValues,
          ConnectorPropertiesEnum.columnMappingActivityColumnProperty.toStringDescription
        )
      assert(activityConfigValue.errorMessages().size() != 0)
      map.put(ConnectorPropertiesEnum.columnMappingActivityColumnProperty.toStringDescription, "1")

      map.remove(ConnectorPropertiesEnum.columnMappingTimeColumnsProperty.toStringDescription)
      val timeConfig: Config = connector.validate(map)
      val timeConfigValues = timeConfig.configValues().asScala
      val timeConfigValue: ConfigValue =
        connector.getConfigValue(
          timeConfigValues,
          ConnectorPropertiesEnum.columnMappingTimeColumnsProperty.toStringDescription
        )
      assert(timeConfigValue.errorMessages().size() != 0)
      map.put(
        ConnectorPropertiesEnum.columnMappingTimeColumnsProperty.toStringDescription,
        s"${Constants.characterForElementStartInPropertyValue}2${Constants.delimiterForElementInPropertyValue}dd/MM/yy HH:mm${Constants.characterForElementEndInPropertyValue}${Constants.delimiterForPropertiesWithListValue}${Constants.characterForElementStartInPropertyValue}3${Constants.delimiterForElementInPropertyValue}dd/MM/yy HH:mm${Constants.characterForElementEndInPropertyValue}"
      )

      map.remove(ConnectorPropertiesEnum.csvEndOfLineProperty.toStringDescription)
      val endOfLineConfig: Config = connector.validate(map)
      val endOfLineConfigValues = endOfLineConfig.configValues().asScala
      val endOfLineConfigValue: ConfigValue =
        connector.getConfigValue(
          endOfLineConfigValues,
          ConnectorPropertiesEnum.csvEndOfLineProperty.toStringDescription
        )
      assert(endOfLineConfigValue.errorMessages().size() != 0)
      map.put(ConnectorPropertiesEnum.csvEndOfLineProperty.toStringDescription, "\\n")

      map.remove(ConnectorPropertiesEnum.csvEscapeProperty.toStringDescription)
      val escapeConfig: Config = connector.validate(map)
      val escapeConfigValues = escapeConfig.configValues().asScala
      val escapeConfigValue: ConfigValue =
        connector.getConfigValue(escapeConfigValues, ConnectorPropertiesEnum.csvEscapeProperty.toStringDescription)
      assert(escapeConfigValue.errorMessages().size() != 0)
      map.put(ConnectorPropertiesEnum.csvEscapeProperty.toStringDescription, "\\")

      map.remove(ConnectorPropertiesEnum.csvCommentProperty.toStringDescription)
      val commentConfig: Config = connector.validate(map)
      val commentConfigValues = commentConfig.configValues().asScala
      val commentConfigValue: ConfigValue =
        connector.getConfigValue(commentConfigValues, ConnectorPropertiesEnum.csvCommentProperty.toStringDescription)
      assert(commentConfigValue.errorMessages().size() != 0)
    }

    it(
      s"should add an error to the configuration if the ${ConnectorPropertiesEnum.columnMappingTimeColumnsProperty.toStringDescription} property value has too much columns"
    ) {
      val connector: IGrafxAggregationSinkConnector = new TestAggregationSinkConnector()

      val map: util.Map[String, String] = createColumnMappingPropertiesMap()
      map.put(
        ConnectorPropertiesEnum.columnMappingTimeColumnsProperty.toStringDescription,
        s"${Constants.characterForElementStartInPropertyValue}2${Constants.delimiterForElementInPropertyValue}dd/MM/yy HH:mm${Constants.characterForElementEndInPropertyValue}${Constants.delimiterForPropertiesWithListValue}${Constants.characterForElementStartInPropertyValue}3${Constants.delimiterForElementInPropertyValue}dd/MM/yy HH:mm${Constants.characterForElementEndInPropertyValue}${Constants.delimiterForPropertiesWithListValue}${Constants.characterForElementStartInPropertyValue}4${Constants.delimiterForElementInPropertyValue}dd/MM/yy HH:mm${Constants.characterForElementEndInPropertyValue}"
      )

      val config: Config = connector.validate(map)
      val configValues = config.configValues().asScala
      val timeConfig =
        connector.getConfigValue(
          configValues,
          ConnectorPropertiesEnum.columnMappingTimeColumnsProperty.toStringDescription
        )
      assert(timeConfig.errorMessages().size() != 0)
    }

    it(
      s"should add an error to the configuration if a mandatory property is undefined while the ${ConnectorPropertiesEnum.kafkaLoggingEventsIsLoggingProperty.toStringDescription} property is true"
    ) {
      val connector: IGrafxAggregationSinkConnector = new TestAggregationSinkConnector()

      val map: util.Map[String, String] = createPropertiesMapWithLogging()

      map.remove(ConnectorPropertiesEnum.kafkaLoggingEventsTopicProperty.toStringDescription)
      val loggingTopicConfig: Config = connector.validate(map)
      val loggingTopicConfigValues = loggingTopicConfig.configValues().asScala
      val loggingTopicConfigValue: ConfigValue =
        connector.getConfigValue(
          loggingTopicConfigValues,
          ConnectorPropertiesEnum.kafkaLoggingEventsTopicProperty.toStringDescription
        )
      assert(loggingTopicConfigValue.errorMessages().size() != 0)
    }
  }

  describe("start") {
    it(
      s"should throw a ConfigException if the ${ConnectorPropertiesEnum.projectIdProperty} property doesn't correspond to a UUID"
    ) {
      val connector: IGrafxAggregationSinkConnector = new TestAggregationSinkConnector()

      val map: util.Map[String, String] = createPropertiesMap()
      map.put(ConnectorPropertiesEnum.projectIdProperty.toStringDescription, "project_id_example")

      assertThrows[ConfigException] {
        connector.start(map)
      }
    }

    it(
      s"should throw a ConfigException if the ${ConnectorPropertiesEnum.csvSeparatorProperty} property has a value with a length != 1"
    ) {
      val connector: IGrafxAggregationSinkConnector = new TestAggregationSinkConnector()

      val map: util.Map[String, String] = createPropertiesMap()
      map.put(ConnectorPropertiesEnum.csvSeparatorProperty.toStringDescription, ",,")

      assertThrows[ConfigException] {
        connector.start(map)
      }
    }

    it(
      s"should throw a ConfigException if the ${ConnectorPropertiesEnum.csvQuoteProperty} property has a value with a length != 1"
    ) {
      val connector: IGrafxAggregationSinkConnector = new TestAggregationSinkConnector()

      val map: util.Map[String, String] = createPropertiesMap()
      map.put(ConnectorPropertiesEnum.csvQuoteProperty.toStringDescription, "\"\"")

      assertThrows[ConfigException] {
        connector.start(map)
      }
    }

    it(
      s"should throw a ConfigException if the ${ConnectorPropertiesEnum.csvFieldsNumberProperty} property has a value < 3"
    ) {
      val connector: IGrafxAggregationSinkConnector = new TestAggregationSinkConnector()

      val map: util.Map[String, String] = createPropertiesMap()
      map.put(ConnectorPropertiesEnum.csvFieldsNumberProperty.toStringDescription, "2")

      assertThrows[ConfigException] {
        connector.start(map)
      }
    }

    it(
      s"should throw a ConfigException if the ${ConnectorPropertiesEnum.retentionTimeInDayProperty} property has a value < 1"
    ) {
      val connector: IGrafxAggregationSinkConnector = new TestAggregationSinkConnector()

      val map: util.Map[String, String] = createPropertiesMap()
      map.put(ConnectorPropertiesEnum.retentionTimeInDayProperty.toStringDescription, "0")

      assertThrows[ConfigException] {
        connector.start(map)
      }
    }

    it(s"should throw a ConfigException if the name property is missing") {
      val connector: IGrafxAggregationSinkConnector = new TestAggregationSinkConnector()

      val map: util.Map[String, String] = createPropertiesMap()
      map.remove("name")

      assertThrows[ConfigException] {
        connector.start(map)
      }
    }

    it(s"should throw a ConfigException if the topics property is missing") {
      val connector: IGrafxAggregationSinkConnector = new TestAggregationSinkConnector()

      val map: util.Map[String, String] = createPropertiesMap()
      map.remove("topics")

      assertThrows[ConfigException] {
        connector.start(map)
      }
    }

    it(s"should throw a ConfigException if the ${ConnectorPropertiesEnum.apiUrlProperty} property is missing") {
      val connector: IGrafxAggregationSinkConnector = new TestAggregationSinkConnector()

      val map: util.Map[String, String] = createPropertiesMap()
      map.remove(ConnectorPropertiesEnum.apiUrlProperty.toStringDescription)

      assertThrows[ConfigException] {
        connector.start(map)
      }
    }

    it(s"should throw a ConfigException if the ${ConnectorPropertiesEnum.authUrlProperty} property is missing") {
      val connector: IGrafxAggregationSinkConnector = new TestAggregationSinkConnector()

      val map: util.Map[String, String] = createPropertiesMap()
      map.remove(ConnectorPropertiesEnum.authUrlProperty.toStringDescription)

      assertThrows[ConfigException] {
        connector.start(map)
      }
    }

    it(s"should throw a ConfigException if the ${ConnectorPropertiesEnum.workGroupIdProperty} property is missing") {
      val connector: IGrafxAggregationSinkConnector = new TestAggregationSinkConnector()

      val map: util.Map[String, String] = createPropertiesMap()
      map.remove(ConnectorPropertiesEnum.workGroupIdProperty.toStringDescription)

      assertThrows[ConfigException] {
        connector.start(map)
      }
    }

    it(s"should throw a ConfigException if the ${ConnectorPropertiesEnum.workGroupKeyProperty} property is missing") {
      val connector: IGrafxAggregationSinkConnector = new TestAggregationSinkConnector()

      val map: util.Map[String, String] = createPropertiesMap()
      map.remove(ConnectorPropertiesEnum.workGroupKeyProperty.toStringDescription)

      assertThrows[ConfigException] {
        connector.start(map)
      }
    }

    it(s"should throw a ConfigException if the ${ConnectorPropertiesEnum.projectIdProperty} property is missing") {
      val connector: IGrafxAggregationSinkConnector = new TestAggregationSinkConnector()

      val map: util.Map[String, String] = createPropertiesMap()
      map.remove(ConnectorPropertiesEnum.projectIdProperty.toStringDescription)

      assertThrows[ConfigException] {
        connector.start(map)
      }
    }

    it(s"should throw a ConfigException if the ${ConnectorPropertiesEnum.csvEncodingProperty} property is missing") {
      val connector: IGrafxAggregationSinkConnector = new TestAggregationSinkConnector()

      val map: util.Map[String, String] = createPropertiesMap()
      map.remove(ConnectorPropertiesEnum.csvEncodingProperty.toStringDescription)

      assertThrows[ConfigException] {
        connector.start(map)
      }
    }

    it(s"should throw a ConfigException if the ${ConnectorPropertiesEnum.csvSeparatorProperty} property is missing") {
      val connector: IGrafxAggregationSinkConnector = new TestAggregationSinkConnector()

      val map: util.Map[String, String] = createPropertiesMap()
      map.remove(ConnectorPropertiesEnum.csvSeparatorProperty.toStringDescription)

      assertThrows[ConfigException] {
        connector.start(map)
      }
    }

    it(s"should throw a ConfigException if the ${ConnectorPropertiesEnum.csvQuoteProperty} property is missing") {
      val connector: IGrafxAggregationSinkConnector = new TestAggregationSinkConnector()

      val map: util.Map[String, String] = createPropertiesMap()
      map.remove(ConnectorPropertiesEnum.csvQuoteProperty.toStringDescription)

      assertThrows[ConfigException] {
        connector.start(map)
      }
    }

    it(
      s"should throw a ConfigException if the ${ConnectorPropertiesEnum.csvFieldsNumberProperty} property is missing"
    ) {
      val connector: IGrafxAggregationSinkConnector = new TestAggregationSinkConnector()

      val map: util.Map[String, String] = createPropertiesMap()
      map.remove(ConnectorPropertiesEnum.csvFieldsNumberProperty.toStringDescription)

      assertThrows[ConfigException] {
        connector.start(map)
      }
    }

    it(s"should throw a ConfigException if the ${ConnectorPropertiesEnum.csvHeaderProperty} property is missing") {
      val connector: IGrafxAggregationSinkConnector = new TestAggregationSinkConnector()

      val map: util.Map[String, String] = createPropertiesMap()
      map.remove(ConnectorPropertiesEnum.csvHeaderProperty.toStringDescription)

      assertThrows[ConfigException] {
        connector.start(map)
      }
    }

    it(
      s"should throw a ConfigException if the ${ConnectorPropertiesEnum.csvDefaultTextValueProperty} property is missing"
    ) {
      val connector: IGrafxAggregationSinkConnector = new TestAggregationSinkConnector()

      val map: util.Map[String, String] = createPropertiesMap()
      map.remove(ConnectorPropertiesEnum.csvDefaultTextValueProperty.toStringDescription)

      assertThrows[ConfigException] {
        connector.start(map)
      }
    }

    it(
      s"should throw a ConfigException if the ${ConnectorPropertiesEnum.retentionTimeInDayProperty} property is missing"
    ) {
      val connector: IGrafxAggregationSinkConnector = new TestAggregationSinkConnector()

      val map: util.Map[String, String] = createPropertiesMap()
      map.remove(ConnectorPropertiesEnum.retentionTimeInDayProperty.toStringDescription)

      assertThrows[ConfigException] {
        connector.start(map)
      }
    }

    it(
      s"should throw a ConfigException if the ${ConnectorPropertiesEnum.elementNumberThresholdProperty.toStringDescription} property is missing"
    ) {
      val connector: IGrafxAggregationSinkConnector = new TestAggregationSinkConnector()

      val map: util.Map[String, String] = createPropertiesMap()
      map.remove(ConnectorPropertiesEnum.elementNumberThresholdProperty.toStringDescription)
      assertThrows[ConfigException] {
        connector.start(map)
      }
    }

    it(
      s"should throw a ConfigException if the ${ConnectorPropertiesEnum.timeoutInSecondsThresholdProperty.toStringDescription} property is missing"
    ) {
      val connector: IGrafxAggregationSinkConnector = new TestAggregationSinkConnector()

      val map: util.Map[String, String] = createPropertiesMap()
      map.remove(ConnectorPropertiesEnum.timeoutInSecondsThresholdProperty.toStringDescription)

      assertThrows[ConfigException] {
        connector.start(map)
      }
    }

    it(
      s"should throw a ConfigException if the ${ConnectorPropertiesEnum.bootstrapServersProperty.toStringDescription} property is missing"
    ) {
      val connector: IGrafxAggregationSinkConnector = new TestAggregationSinkConnector()

      val map: util.Map[String, String] = createPropertiesMap()
      map.remove(ConnectorPropertiesEnum.bootstrapServersProperty.toStringDescription)

      assertThrows[ConfigException] {
        connector.start(map)
      }
    }

    it(s"should throw a ConfigException if the ${Constants.schemaRegistryUrlProperty} property is missing") {
      val connector: IGrafxAggregationSinkConnector = new TestAggregationSinkConnector()

      val map: util.Map[String, String] = createPropertiesMap()
      map.remove(Constants.schemaRegistryUrlProperty)

      assertThrows[ConfigException] {
        connector.start(map)
      }
    }

    it(
      "should throw an exception (which will stop the connector) if there is a problem with the Column Mapping Creation"
    ) {
      val connector: IGrafxAggregationSinkConnector = new TestAggregationSinkConnector()
      when(connector.mainApi.createColumnMapping(any()))
        .thenReturn(
          Future.failed(new Exception)
        )

      val map: util.Map[String, String] = createColumnMappingPropertiesMap()
      assertThrows[Exception] {
        connector.start(map)
      }
    }

    it(
      "should not throw an exception even if the column mapping already exists"
    ) {
      val connector: IGrafxAggregationSinkConnector = new TestAggregationSinkConnector()
      when(connector.mainApi.createColumnMapping(any()))
        .thenReturn(
          Future.failed(ColumnMappingAlreadyExistsException())
        )

      val map: util.Map[String, String] = createColumnMappingPropertiesMap()
      connector.start(map)
      verify(connector.mainApi, times(1)).createColumnMapping(any())

      assert(true)
    }

    it(
      s"it should not call createColumnMapping if the ${ConnectorPropertiesEnum.columnMappingCreateProperty} is false"
    ) {
      val connector: IGrafxAggregationSinkConnector = new TestAggregationSinkConnector()
      when(connector.mainApi.createColumnMapping(any()))
        .thenReturn(
          Future.successful(())
        )
      val map: util.Map[String, String] = createPropertiesMap()
      connector.start(map)
      verify(connector.mainApi, times(0)).createColumnMapping(any())
      assert(true)
    }

    it(
      s"it should call 1 time createColumnMapping if the ${ConnectorPropertiesEnum.columnMappingCreateProperty} is true"
    ) {
      val connector: IGrafxAggregationSinkConnector = new TestAggregationSinkConnector()
      when(connector.mainApi.createColumnMapping(any()))
        .thenReturn(
          Future.successful(())
        )
      val map: util.Map[String, String] = createColumnMappingPropertiesMap()
      connector.start(map)
      verify(connector.mainApi, times(1)).createColumnMapping(any())
      assert(true)
    }
  }
}
