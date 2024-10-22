package com.igrafx.kafka.sink.aggregation.adapters

import com.igrafx.kafka.sink.aggregation.Constants
import com.igrafx.kafka.sink.aggregation.domain.enums.ConnectorPropertiesEnum
import com.igrafx.kafka.sink.aggregation.domain.usecases.ConnectorUseCases
import core.UnitTestSpec
import org.apache.kafka.common.config.ConfigException
import org.apache.kafka.connect.connector.Task
import org.apache.kafka.connect.sink.SinkTask
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when

import java.util
import scala.jdk.CollectionConverters._

class AggregationSinkConnectorTest extends UnitTestSpec {
  private val aggregationConnector = new TestAggregationSinkConnector

  private val map: util.Map[String, String] = createPropertiesMap()

  class TestAggregationSinkConnector extends AggregationSinkConnector {
    override val connectorUseCases: ConnectorUseCases =
      mock[ConnectorUseCases]
  }

  override def beforeAll(): Unit = {
    when(
      aggregationConnector.connectorUseCases.getTopicMaxMessageBytes(
        any(),
        any()
      )
    ).thenReturn(1000000)

    aggregationConnector.start(map)
    super.beforeAll()
  }

  override def afterAll(): Unit = {
    aggregationConnector.stop()
    super.afterAll()
  }

  private def createPropertiesMap(): util.Map[String, String] = {
    val map: util.Map[String, String] = new util.HashMap[String, String]()

    map.put(Constants.connectorNameProperty, "connectorName")
    map.put(Constants.topicsProperty, "topicInExample")
    map.put(ConnectorPropertiesEnum.topicOutProperty.toStringDescription, "topicOutExample")
    map.put(ConnectorPropertiesEnum.aggregationColumnNameProperty.toStringDescription, "LINEAG")
    map.put(ConnectorPropertiesEnum.elementNumberThresholdProperty.toStringDescription, "10")
    map.put(ConnectorPropertiesEnum.valuePatternThresholdProperty.toStringDescription, "")
    map.put(ConnectorPropertiesEnum.timeoutInSecondsThresholdProperty.toStringDescription, "10")
    map.put(ConnectorPropertiesEnum.bootstrapServersProperty.toStringDescription, "bootstrapServersExample")
    map.put(Constants.schemaRegistryUrlProperty, "schemaPropertyUrlExample")

    map
  }

  describe("taskClass") {
    it("should return a SinkTask") {
      val task: Task = aggregationConnector.taskClass().getDeclaredConstructor().newInstance()
      assert(task.isInstanceOf[SinkTask])
    }
  }

  describe("taskConfigs") {
    it("should be able to start Tasks with the correct properties") {
      val properties: util.List[util.Map[String, String]] = aggregationConnector.taskConfigs(3)
      assert(properties.asScala.forall { map: util.Map[String, String] =>
        map.containsKey(Constants.connectorNameProperty)
        map.containsKey(ConnectorPropertiesEnum.topicOutProperty.toStringDescription)
        map.containsKey(ConnectorPropertiesEnum.aggregationColumnNameProperty.toStringDescription)
        map.containsKey(ConnectorPropertiesEnum.elementNumberThresholdProperty.toStringDescription)
        map.containsKey(ConnectorPropertiesEnum.valuePatternThresholdProperty.toStringDescription)
        map.containsKey(ConnectorPropertiesEnum.timeoutInSecondsThresholdProperty.toStringDescription)
        map.containsKey(ConnectorPropertiesEnum.bootstrapServersProperty.toStringDescription)
        map.containsKey(Constants.schemaRegistryUrlProperty)
        map.containsKey(Constants.maxMessageBytesConfigurationName)
      })
    }

    it("should return a number of configurations equal to maxTasks") {
      for (i <- 1 until 4) {
        assert(aggregationConnector.taskConfigs(i).size() == i)
      }
      assert(true)
    }
  }

  describe("start") {
    it(s"should throw a ConfigException if the ${Constants.connectorNameProperty} property is missing") {
      val connector: AggregationSinkConnector = new TestAggregationSinkConnector()

      val map: util.Map[String, String] = createPropertiesMap()
      map.remove(Constants.connectorNameProperty)

      assertThrows[ConfigException] {
        connector.start(map)
      }
    }

    it(s"should throw a ConfigException if the ${Constants.topicsProperty} property is missing") {
      val connector: AggregationSinkConnector = new TestAggregationSinkConnector()

      when(
        connector.connectorUseCases.getTopicMaxMessageBytes(any(), any())
      ).thenReturn(1000000)

      val map: util.Map[String, String] = createPropertiesMap()
      map.remove(Constants.topicsProperty)

      assertThrows[ConfigException] {
        connector.start(map)
      }
    }

    it(
      s"should throw a ConfigException if the ${ConnectorPropertiesEnum.topicOutProperty.toStringDescription} property is missing"
    ) {
      val connector: AggregationSinkConnector = new TestAggregationSinkConnector()

      when(
        connector.connectorUseCases.getTopicMaxMessageBytes(any(), any())
      ).thenReturn(1000000)

      val map: util.Map[String, String] = createPropertiesMap()
      map.remove(ConnectorPropertiesEnum.topicOutProperty.toStringDescription)

      assertThrows[ConfigException] {
        connector.start(map)
      }
    }

    it(
      s"should throw a ConfigException if the ${ConnectorPropertiesEnum.aggregationColumnNameProperty.toStringDescription} property is missing"
    ) {
      val connector: AggregationSinkConnector = new TestAggregationSinkConnector()

      when(
        connector.connectorUseCases.getTopicMaxMessageBytes(any(), any())
      ).thenReturn(1000000)

      val map: util.Map[String, String] = createPropertiesMap()
      map.remove(ConnectorPropertiesEnum.aggregationColumnNameProperty.toStringDescription)

      assertThrows[ConfigException] {
        connector.start(map)
      }
    }

    it(
      s"should throw a ConfigException if the ${ConnectorPropertiesEnum.elementNumberThresholdProperty.toStringDescription} property is missing"
    ) {
      val connector: AggregationSinkConnector = new TestAggregationSinkConnector()

      when(
        connector.connectorUseCases.getTopicMaxMessageBytes(any(), any())
      ).thenReturn(1000000)

      val map: util.Map[String, String] = createPropertiesMap()
      map.remove(ConnectorPropertiesEnum.elementNumberThresholdProperty.toStringDescription)
      assertThrows[ConfigException] {
        connector.start(map)
      }
    }

    it(
      s"should throw a ConfigException if the ${ConnectorPropertiesEnum.timeoutInSecondsThresholdProperty.toStringDescription} property is missing"
    ) {
      val connector: AggregationSinkConnector = new TestAggregationSinkConnector()

      when(
        connector.connectorUseCases.getTopicMaxMessageBytes(any(), any())
      ).thenReturn(1000000)

      val map: util.Map[String, String] = createPropertiesMap()
      map.remove(ConnectorPropertiesEnum.timeoutInSecondsThresholdProperty.toStringDescription)

      assertThrows[ConfigException] {
        connector.start(map)
      }
    }

    it(
      s"should throw a ConfigException if the ${ConnectorPropertiesEnum.bootstrapServersProperty.toStringDescription} property is missing"
    ) {
      val connector: AggregationSinkConnector = new TestAggregationSinkConnector()

      when(
        connector.connectorUseCases.getTopicMaxMessageBytes(any(), any())
      ).thenReturn(1000000)

      val map: util.Map[String, String] = createPropertiesMap()
      map.remove(ConnectorPropertiesEnum.bootstrapServersProperty.toStringDescription)

      assertThrows[ConfigException] {
        connector.start(map)
      }
    }

    it(s"should throw a ConfigException if the ${Constants.schemaRegistryUrlProperty} property is missing") {
      val connector: AggregationSinkConnector = new TestAggregationSinkConnector()

      when(
        connector.connectorUseCases.getTopicMaxMessageBytes(any(), any())
      ).thenReturn(1000000)

      val map: util.Map[String, String] = createPropertiesMap()
      map.remove(Constants.schemaRegistryUrlProperty)

      assertThrows[ConfigException] {
        connector.start(map)
      }
    }

    it(
      s"should throw an IllegalArgumentException if the ${ConnectorPropertiesEnum.topicOutProperty.toStringDescription} property is empty"
    ) {
      val connector: AggregationSinkConnector = new TestAggregationSinkConnector()

      when(
        connector.connectorUseCases.getTopicMaxMessageBytes(any(), any())
      ).thenReturn(1000000)

      val map: util.Map[String, String] = createPropertiesMap()
      map.put(ConnectorPropertiesEnum.topicOutProperty.toStringDescription, "")

      assertThrows[IllegalArgumentException] {
        connector.start(map)
      }
    }

    it(
      s"should throw an IllegalArgumentException if the ${ConnectorPropertiesEnum.elementNumberThresholdProperty.toStringDescription} property has a value <= 0"
    ) {
      val connector: AggregationSinkConnector = new TestAggregationSinkConnector()

      when(
        connector.connectorUseCases.getTopicMaxMessageBytes(any(), any())
      ).thenReturn(1000000)

      val map: util.Map[String, String] = createPropertiesMap()

      map.put(ConnectorPropertiesEnum.elementNumberThresholdProperty.toStringDescription, "0")
      assertThrows[ConfigException] {
        connector.start(map)
      }

      map.put(ConnectorPropertiesEnum.elementNumberThresholdProperty.toStringDescription, "-10")
      assertThrows[ConfigException] {
        connector.start(map)
      }
    }

    it(
      s"should throw an IllegalArgumentException if the ${ConnectorPropertiesEnum.timeoutInSecondsThresholdProperty.toStringDescription} property has a value <= 0"
    ) {
      val connector: AggregationSinkConnector = new TestAggregationSinkConnector()

      when(
        connector.connectorUseCases.getTopicMaxMessageBytes(any(), any())
      ).thenReturn(1000000)

      val map: util.Map[String, String] = createPropertiesMap()

      map.put(ConnectorPropertiesEnum.timeoutInSecondsThresholdProperty.toStringDescription, "0")
      assertThrows[ConfigException] {
        connector.start(map)
      }

      map.put(ConnectorPropertiesEnum.timeoutInSecondsThresholdProperty.toStringDescription, "-10")
      assertThrows[ConfigException] {
        connector.start(map)
      }
    }
  }
}
