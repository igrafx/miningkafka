package com.igrafx.kafka.sink.aggregationmain.domain

import com.igrafx.kafka.sink.aggregationmain.Constants
import com.igrafx.kafka.sink.aggregationmain.domain.enums.ConnectorPropertiesEnum
import com.igrafx.kafka.sink.aggregationmain.domain.usecases.TaskAggregationUseCases
import core.UnitTestSpec
import org.apache.kafka.common.config.ConfigException
import org.mockito.Mockito.{spy, times, verify}

import java.util

class IGrafxAggregationSinkTaskTest extends UnitTestSpec {
  private val taskAggregationUseCasesMock = mock[TaskAggregationUseCases]

  class TestAggregationSinkTask extends IGrafxAggregationSinkTask {
    override val taskAggregationUseCases: TaskAggregationUseCases = taskAggregationUseCasesMock
  }

  private val aggregationSinkTask = new TestAggregationSinkTask

  override def beforeAll(): Unit = {
    aggregationSinkTask.start(createPropertiesMapWithoutHeaderAndLogging())
    super.beforeAll()
  }

  private def createPropertiesMapWithoutHeaderAndLogging(): util.Map[String, String] = {
    val map: util.Map[String, String] = new util.HashMap[String, String]()
    map.put("name", "connectorName")
    map.put(ConnectorPropertiesEnum.apiUrlProperty.toStringDescription, "api_url_example")
    map.put(ConnectorPropertiesEnum.authUrlProperty.toStringDescription, "auth_url_example")
    map.put(ConnectorPropertiesEnum.workGroupIdProperty.toStringDescription, "w_id_example")
    map.put(ConnectorPropertiesEnum.workGroupKeyProperty.toStringDescription, "w_key_example")
    map.put(ConnectorPropertiesEnum.projectIdProperty.toStringDescription, "0f61e0f3-68c8-4c7f-bc1d-a4aabf7b74e5")
    map.put(ConnectorPropertiesEnum.csvEncodingProperty.toStringDescription, "UTF8")
    map.put(ConnectorPropertiesEnum.csvSeparatorProperty.toStringDescription, ",")
    map.put(ConnectorPropertiesEnum.csvQuoteProperty.toStringDescription, "\"")
    map.put(ConnectorPropertiesEnum.csvFieldsNumberProperty.toStringDescription, 4.toString)
    map.put(ConnectorPropertiesEnum.csvHeaderProperty.toStringDescription, "false")
    map.put(ConnectorPropertiesEnum.csvDefaultTextValueProperty.toStringDescription, " ")
    map.put(ConnectorPropertiesEnum.retentionTimeInDayProperty.toStringDescription, "100")
    map.put(ConnectorPropertiesEnum.kafkaLoggingEventsIsLoggingProperty.toStringDescription, "false")
    map.put(ConnectorPropertiesEnum.elementNumberThresholdProperty.toStringDescription, "10")
    map.put(ConnectorPropertiesEnum.valuePatternThresholdProperty.toStringDescription, "")
    map.put(ConnectorPropertiesEnum.timeoutInSecondsThresholdProperty.toStringDescription, "10")
    map.put(ConnectorPropertiesEnum.bootstrapServersProperty.toStringDescription, "bootstrapServersExample")
    map.put(Constants.schemaRegistryUrlProperty, "schemaPropertyUrlExample")

    map
  }

  val headerValuePropertyValue = "headerTest"
  private def createPropertiesMapWithHeader(): util.Map[String, String] = {
    val map: util.Map[String, String] = new util.HashMap[String, String]()
    map.put("name", "connectorName")
    map.put(ConnectorPropertiesEnum.apiUrlProperty.toStringDescription, "api_url_example")
    map.put(ConnectorPropertiesEnum.authUrlProperty.toStringDescription, "auth_url_example")
    map.put(ConnectorPropertiesEnum.workGroupIdProperty.toStringDescription, "w_id_example")
    map.put(ConnectorPropertiesEnum.workGroupKeyProperty.toStringDescription, "w_key_example")
    map.put(ConnectorPropertiesEnum.projectIdProperty.toStringDescription, "0f61e0f3-68c8-4c7f-bc1d-a4aabf7b74e5")
    map.put(ConnectorPropertiesEnum.csvEncodingProperty.toStringDescription, "UTF8")
    map.put(ConnectorPropertiesEnum.csvSeparatorProperty.toStringDescription, ",")
    map.put(ConnectorPropertiesEnum.csvQuoteProperty.toStringDescription, "\"")
    map.put(ConnectorPropertiesEnum.csvFieldsNumberProperty.toStringDescription, 4.toString)
    map.put(ConnectorPropertiesEnum.csvHeaderProperty.toStringDescription, "true")
    map.put(ConnectorPropertiesEnum.csvDefaultTextValueProperty.toStringDescription, " ")
    map.put(ConnectorPropertiesEnum.retentionTimeInDayProperty.toStringDescription, "100")
    map.put(Constants.headerValueProperty, headerValuePropertyValue)
    map.put(ConnectorPropertiesEnum.kafkaLoggingEventsIsLoggingProperty.toStringDescription, "false")
    map.put(ConnectorPropertiesEnum.elementNumberThresholdProperty.toStringDescription, "10")
    map.put(ConnectorPropertiesEnum.valuePatternThresholdProperty.toStringDescription, "")
    map.put(ConnectorPropertiesEnum.timeoutInSecondsThresholdProperty.toStringDescription, "10")
    map.put(ConnectorPropertiesEnum.bootstrapServersProperty.toStringDescription, "bootstrapServersExample")
    map.put(Constants.schemaRegistryUrlProperty, "schemaPropertyUrlExample")

    map
  }

  val loggingTopicPropertyValue = "loggingTopic"
  val loggingBootstrapServersPropertyValue = "brokers"
  val loggingSchemaRegistryLoggingValue = "schemaRegistry"
  private def createPropertiesWithLogging(): util.Map[String, String] = {
    val map: util.Map[String, String] = new util.HashMap[String, String]()
    map.put("name", "connectorName")
    map.put(ConnectorPropertiesEnum.apiUrlProperty.toStringDescription, "api_url_example")
    map.put(ConnectorPropertiesEnum.authUrlProperty.toStringDescription, "auth_url_example")
    map.put(ConnectorPropertiesEnum.workGroupIdProperty.toStringDescription, "w_id_example")
    map.put(ConnectorPropertiesEnum.workGroupKeyProperty.toStringDescription, "w_key_example")
    map.put(ConnectorPropertiesEnum.projectIdProperty.toStringDescription, "0f61e0f3-68c8-4c7f-bc1d-a4aabf7b74e5")
    map.put(ConnectorPropertiesEnum.csvEncodingProperty.toStringDescription, "UTF8")
    map.put(ConnectorPropertiesEnum.csvSeparatorProperty.toStringDescription, ",")
    map.put(ConnectorPropertiesEnum.csvQuoteProperty.toStringDescription, "\"")
    map.put(ConnectorPropertiesEnum.csvFieldsNumberProperty.toStringDescription, 4.toString)
    map.put(ConnectorPropertiesEnum.csvHeaderProperty.toStringDescription, "false")
    map.put(ConnectorPropertiesEnum.csvDefaultTextValueProperty.toStringDescription, " ")
    map.put(ConnectorPropertiesEnum.retentionTimeInDayProperty.toStringDescription, "100")
    map.put(ConnectorPropertiesEnum.kafkaLoggingEventsIsLoggingProperty.toStringDescription, "true")
    map.put(ConnectorPropertiesEnum.kafkaLoggingEventsTopicProperty.toStringDescription, loggingTopicPropertyValue)
    map.put(ConnectorPropertiesEnum.elementNumberThresholdProperty.toStringDescription, "10")
    map.put(ConnectorPropertiesEnum.valuePatternThresholdProperty.toStringDescription, "")
    map.put(ConnectorPropertiesEnum.timeoutInSecondsThresholdProperty.toStringDescription, "10")
    map.put(ConnectorPropertiesEnum.bootstrapServersProperty.toStringDescription, "bootstrapServersExample")
    map.put(Constants.schemaRegistryUrlProperty, "schemaPropertyUrlExample")

    map
  }

  describe("start") {
    it("should throw an exception if the " + ConnectorPropertiesEnum.projectIdProperty + " property is not defined") {
      val task: IGrafxAggregationSinkTask = new IGrafxAggregationSinkTask
      val map: util.Map[String, String] = new util.HashMap[String, String]()
      assertThrows[ConfigException] { task.start(map) }
    }

    it(
      s"should retrieve a header value if the ${ConnectorPropertiesEnum.csvHeaderProperty.toStringDescription} property equals true"
    ) {
      val igrafxSinkTaskSpy = spy(new IGrafxAggregationSinkTask)

      igrafxSinkTaskSpy.start(createPropertiesMapWithHeader())
      verify(igrafxSinkTaskSpy, times(1)).checkProperty(headerValuePropertyValue, Constants.headerValueProperty)

      assert(true)
    }

    it(
      s"should not retrieve a header value if the ${ConnectorPropertiesEnum.csvHeaderProperty.toStringDescription} property equals false"
    ) {
      val igrafxSinkTaskSpy = spy(new IGrafxAggregationSinkTask)

      igrafxSinkTaskSpy.start(createPropertiesMapWithoutHeaderAndLogging())
      verify(igrafxSinkTaskSpy, times(0)).checkProperty(null, Constants.headerValueProperty)

      assert(true)
    }

    it(
      s"should throw a ConfigException if the ${ConnectorPropertiesEnum.csvHeaderProperty.toStringDescription} property equals true but there is no header value"
    ) {
      val IGrafxAggregationSinkTask = new IGrafxAggregationSinkTask
      val map = createPropertiesMapWithoutHeaderAndLogging()
      map.put(ConnectorPropertiesEnum.csvHeaderProperty.toStringDescription, "true")

      assertThrows[ConfigException] {
        IGrafxAggregationSinkTask.start(map)
      }
    }

    it(
      s"should retrieve Kafka Event Logging Properties values if the ${ConnectorPropertiesEnum.kafkaLoggingEventsIsLoggingProperty.toStringDescription} property equals true"
    ) {
      val igrafxSinkTaskSpy = spy(new IGrafxAggregationSinkTask)

      igrafxSinkTaskSpy.start(createPropertiesWithLogging())
      verify(igrafxSinkTaskSpy, times(1)).checkProperty(
        loggingTopicPropertyValue,
        ConnectorPropertiesEnum.kafkaLoggingEventsTopicProperty.toStringDescription
      )

      assert(true)
    }

    it(
      s"should not retrieve any Kafka Event Logging Properties value if the ${ConnectorPropertiesEnum.kafkaLoggingEventsIsLoggingProperty.toStringDescription} property is equal to false"
    ) {
      val igrafxSinkTaskSpy = spy(new IGrafxAggregationSinkTask)

      igrafxSinkTaskSpy.start(createPropertiesMapWithoutHeaderAndLogging())
      verify(igrafxSinkTaskSpy, times(0)).checkProperty(
        null,
        ConnectorPropertiesEnum.kafkaLoggingEventsTopicProperty.toStringDescription
      )
      assert(true)
    }

    it(
      s"should throw a ConfigException if the ${ConnectorPropertiesEnum.kafkaLoggingEventsIsLoggingProperty.toStringDescription} property equals true but there is no Kafka Event Logging Properties value"
    ) {
      val IGrafxAggregationSinkTask = new IGrafxAggregationSinkTask
      val map = createPropertiesMapWithoutHeaderAndLogging()
      map.put(ConnectorPropertiesEnum.kafkaLoggingEventsIsLoggingProperty.toStringDescription, "true")

      assertThrows[ConfigException] {
        IGrafxAggregationSinkTask.start(map)
      }
    }
  }
}
