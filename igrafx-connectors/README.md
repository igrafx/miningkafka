# iGrafx Connectors

This project gathers the iGrafx Kafka Connectors that can be used to send data in or out of Kafka

If you want to use a Connector or create a new one, please follow the instructions in the [CONTRIBUTING.md](./CONTRIBUTING.md) file

## Existing Connectors

Each Connector corresponds to a Module and has its own README explaining what it does and how to use it


### Connector Aggregation

* module : aggregation
* package : com.igrafx.kafka.sink.aggregation
  
Connector aggregating Kafka records from a same partition. Read more [here](aggregation/src/main/scala/com/igrafx/kafka/sink/aggregation/README.md).

### Connector Main iGrafx And Aggregation

* module : aggregationMain
* package : com.igrafx.kafka.sink.aggregationmain

Connector specific to iGrafx, aggregates Kafka records from a same partition and send the aggregation result to the iGrafx Mining API. Read more [here](aggregationMain/src/main/scala/com/igrafx/kafka/sink/aggregationmain/README.md).