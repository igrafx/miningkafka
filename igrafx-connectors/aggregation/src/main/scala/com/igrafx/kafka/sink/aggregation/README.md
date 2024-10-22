# Aggregation Connector

## Overview 

This connector aims to aggregate together multiple records coming from a same partition.
For instance, if the incoming data is composed of two columns having for types :

 ``` 
 LINE1 VARCHAR,
 LINE2 VARCHAR
 ```

The result of the aggregation will be sent to a kafka topic with the type :

``` 
LINEAG ARRAY<STRUCT<LINE1 VARCHAR, LINE2 VARCHAR>>
```

where each Record coming from Kafka will be aggregated with other records in an ARRAY (please note that **LINEAG** will in this case be used as the value for the **aggregationColumnName** connector's property)

Here the :

``` 
LINEAG ARRAY<STRUCT<...>>
```

that is added over the columns of the incoming data, corresponds to the aggregation. The ARRAY contains the different aggregated results, and the STRUCT allows keeping together the different columns of the input data

To aggregate, multiple thresholds are used :

* Element number : when the number of aggregated elements reach a certain value the aggregation result is sent to Kafka
* Value pattern : A Regex pattern triggering the flush of the current aggregation to Kafka if the value of the incoming sink record matches it 
* Timeout : After a certain time since the last sending of an aggregation result, the current aggregated data are sent (even if the number of elements threshold is not reached)
* Retention : This threshold is not defined by the user in the connector's properties. It is linked to the value of the **retention.ms** configuration in the Kafka topic from which a data comes. See the section below related to the retention for more information

<hr/>

### Connector properties

To instantiate the connector, we have to fill the following properties (values are given as an example) :

``` 
'connector.class' = 'com.igrafx.kafka.sink.aggregation.adapters.AggregationSinkConnector', 
'tasks.max' = '1',
'topics' = 'aggregation_input_topic',
'topicOut' = 'aggregation_output_topic',
'aggregationColumnName' = 'aggregationColumnNameTest',
'threshold.elementNumber' = '6',
'threshold.valuePattern' = '.*regex_example.*',
'threshold.timeoutInSeconds' = 3000,
'bootstrap.servers' = 'broker:29092',
'key.converter' = 'org.apache.kafka.connect.storage.StringConverter',
'value.converter' = 'io.confluent.connect.avro.AvroConverter',
'value.converter.schema.registry.url' = 'http://schema-registry:8081'
```

The values for the following properties should not change :

* **connector.class** (String)
* **key.converter** (String)
* **value.converter** (String)

The values for the other properties can be modified :

* **tasks.max** (Int) : Number of Tasks we want to create for the connector
* **topics** (String) : The list of Kafka topics containing the data we want to aggregate
* **topicOut** (String) : The Kafka topic containing the results of the aggregations
* **aggregationColumnName** : The name of the column storing the aggregation in ksqlDB
* **threshold.elementNumber** (Int) : The maximum number of elements in one aggregation (when the number is reached, the current aggregation is sent to the Kafka topic described by **topicOut**)
* **threshold.valuePattern** (String) : **Facultative property** sending to **topicOut** Kafka topic the current aggregation if the value of an incoming sink record matches the **regex** defined in **threshold.valuePattern** (the aggregation is sent even if the number of elements corresponding to **threshold.elementNumber** is not reached). This threshold is not relevant if the property is not defined while creating the connector. The pattern is on the entire string value of a SinkRecord (if the value has the JSON format for example, the pattern will have to take into account the '{' '}' of the JSON, it needs to be adapted to the structure of the data)
* **threshold.timeoutInSeconds** (Int) : If the time since the last sending of an aggregation result exceeds the value in seconds of **threshold.timeoutInSeconds**, the current aggregation (which was in construction) is sent to the Kafka topic described by **topicOut** (even if the number of elements corresponding to **threshold.elementNumber** is not reached)
* **bootstrap.servers** (String) : The List of Kafka brokers
* **value.converter.schema.registry.url** : The url of the Kafka Schema Registry

For more information about regex (used with the **threshold.valuePattern** property) : https://medium.com/factory-mind/regex-tutorial-a-simple-cheatsheet-by-examples-649dc1c3f285

<hr/>

### Offsets management

Offsets are managed in the code by the **PartitionTracker** case class. Each partition in a topic present in the **topics** property has a corresponding **PartitionTracker**, and **the aggregation is only performed on data coming from the same partition**.

3 types of offsets are used and maintained by the **PartitionTracker** : 

* **Processed Offset** : Corresponds in the PartitionTracker to the offset of the last Record received for the related partition (handled by the Put function in the AggregationSinkTask)
* **Flushed Offset** : Corresponds in the PartitionTracker to the offset of the last Record that has been sent with its aggregation to the Kafka topic defined by **topicOut**
* **Commit Offset** : Corresponds in the PartitionTracker to the offset of the last Record that has been flushed and whose offset has been committed to Kafka (once the offset of a Record is committed, the Record will not be red again, even in case of a Task error/rebalance...)

This means that when a new SinkRecord arrives to the Put function in AggregationSinkTask, its offset is processed. When a threshold (element number/value pattern/timeout/retention) is reached, the aggregation with the record is sent to Kafka and therefore the record's offset is considered flushed. Finally, once the preCommit method is called in AggregationSinkTask, all the flushed offsets in each partition are committed (if they weren't already).

As a Record is considered done only when its offset is committed, it means that any record having its offset only considered flushed or processed (not yet committed) will be received once again by the connector in case of an issue with the Task which was handling the record. As such, a record already flushed (in Kafka with its aggregation) but not yet committed could, in case of a Task failure or rebalance, be received again by the connector and sent again to Kafka.

This is made to ensure the **at least once** delivery guarantee.

<hr/>

### AVRO

To keep track during the aggregation of the types used for the data, the KsqlDB STREAMs (used over the input and output topic) must follow the AVRO format for their values.

The connector will retrieve the schema linked to the output topic in ksqlDB via Http and will update the schemas of the values coming from Kafka, in order to match the output schema

### Max Message Bytes

Kafka has a limitation towards the size of single messages we send to a topic. By default, a message sent to Kafka can't have a size greater than 1048588 bytes.

However, it is possible to change this default maximum size from the **docker-compose** file. In order to do this, change the value of the following properties :

(For this example the new maximum size is equal to 20000000 bytes, but you can change this value according to your needs)

In the **broker** service add : 

```
KAFKA_MESSAGE_MAX_BYTES: 20000000
KAFKA_MAX_REQUEST_SIZE: 20000000
KAFKA_PRODUCER_MAX_REQUEST_SIZE: 20000000
KAFKA_REPLICA_FETCH_MAX_BYTES: 20000000
```

In the **connect** service add :

``` 
CONNECT_PRODUCER_MAX_REQUEST_SIZE: 20000000
CONNECT_CONSUMER_MAX_PARTITION_FETCH_BYTES: 20000000
```

In the **ksqldb-server** service add :

``` 
KSQL_OPTS: "-Dmax.request.size=20000000"
```

It is important to note that such changes may impact **negatively** the performance of Kafka and most of the time big messages that require to change those configurations are handled in a different cluster than the one used for regular data

**Important :** The connector uses the **max.message.bytes** configuration of the Kafka topic to which the aggregation is sent to determine the maximum size of the messages it will send. 

If a threshold is reached and if the size of the message containing the entire aggregation is greater than the size defined by the **max.message.bytes** configuration of output Kafka topic, then the connector will cut the aggregation and send as many messages as needed to respect the limitation (for instance if the limitation is 1000000 bytes, and the connector needs to send 1500000 bytes of aggregation, then two messages will be sent, one of roughly 900000 bytes, to keep a little margin towards the limit, and one of 600000 bytes).

### Retention

Values in a Kafka topic are stored for as much time as defined by the **retention.ms** configuration of the topic. Hence, to avoid loosing data in case of a connector crash, the connector needs to send the aggregation before one of the value of the aggregation reaches its retention time in the input topic. 

Indeed, even if the connector stores itself the data it receives from Kafka during the aggregation process, in the case where the data exceeds its retention time in the topic and if the connector crashes before sending the aggregation to Kafka, then the data is lost and won't be recovered at the connector restart, as it will not be stored anymore in the source Kafka topic. 

Consequently, in order to avoid this issue, the connector sends the aggregation of a partition if any of its data reaches 80% of its retention time. Nevertheless, if the connector crashes before one of its data reaches 80% of its retention time, and if the connector isn't restarted before the end of the retention time, the data is still lost. 

### Future works

The strategy currently used to send the aggregation to Kafka in the case where we need more than **max.message.bytes** bytes (configuration from the output Kafka topic) to send an aggregation, when a threshold is reached, is to send the entire aggregated values in smaller messages respecting the limit, even if the last message only reaches 10% of the limit. 

Another politic could be to send as much full single messages (reaching a size close to the limit) as possible, and putting the rest of the data (the 10% of the previous sentence) back into the current aggregation for the partition (data are not sent yet and wait for more data to arrive, in order to avoid the sending of small messages). However, this may not be appropriate for the value pattern threshold.

### Limitations

As the schema used for aggregation is retrieved from the Kafka output topic defined by the **topicOut** connector's property, the topic needs to have its schema created before we end the first data to the connector 

### Error Handling

The logs of the connector are available via the following command (launch the command from the repository where the docker-compose is) :

``` 
docker-compose logs -f connect
```

Where *connect* corresponds to the name of the service related to Kafka Connect in the docker-compose. If you also want the DEBUG level logs, add the following line to the value of the **CONNECT_LOG4J_LOGGERS** configuration parameter (in the **connect** service of the docker-compose) :

``` 
com.igrafx.kafka.sink.aggregation.adapters.AggregationSinkTask=DEBUG,com.igrafx.kafka.sink.aggregation.adapters.AggregationSinkConnector=DEBUG
```

### Compilation and deployment on LiveConnect

To compile and create the **.jar** needed to be able to use the connector in Kafka Connect, you need to go to the root of the project and to use the following command :

```
sbt assembly
```

You then copy the **aggregation-connector_{version}.jar** which is in the **artifacts** repository, and you paste it in the **docker-compose/connect-plugins/** repository of LiveConnect (create the last repository if it doesn't already exist)

By launching LiveConnect, the connector is now available

<hr/>

## Examples

For those two examples the **threshold.valuePattern** connector property is not set, so the aggregation is only made according to the **threshold.elementNumber** and **threshold.timeoutInSeconds** connector properties

#### Example 1

First, start by using the following command to apply modifications of a STREAM on data inserted before the creation/display of the STREAM :

``` 
SET 'auto.offset.reset'='earliest';
```

In this first example, we will simply aggregate messages with a single column of type :

``` 
line VARCHAR
```

The first command to write in ksqlDB creates the STREAM that will aliment the aggregation connector with data :

``` 
CREATE STREAM INPUT_DATA (
	line VARCHAR
) WITH (
	KAFKA_TOPIC='aggregation_input', 
	PARTITIONS=1, 
	REPLICAS=1, 
	VALUE_FORMAT='AVRO'
);
```

We then add a STREAM over the output topic to manipulate the aggregated data (please remember to use : **aggregationColumnName** ARRAY<STRUCT<...[INPUT_DATA columns]...>>) :

``` 
CREATE STREAM OUTPUT_DATA (
	LINEAG ARRAY<STRUCT<LINE VARCHAR>>
) WITH (
	KAFKA_TOPIC='aggregation_output', 
	PARTITIONS=1, 
	REPLICAS=1, 
	VALUE_FORMAT='AVRO'
);
```

We can then create the connector (with the correct **aggregationColumnName**) :

``` 
CREATE SINK CONNECTOR AggregationConnectorTest WITH (
    'connector.class' = 'com.igrafx.kafka.sink.aggregation.adapters.AggregationSinkConnector', 
    'tasks.max' = '1',
    'topics' = 'aggregation_input',
    'topicOut' = 'aggregation_output',
    'aggregationColumnName' = 'LINEAG',
    'threshold.elementNumber' = '6',
    'threshold.timeoutInSeconds' = '30',
    'bootstrap.servers' = 'broker:29092',
    'key.converter' = 'org.apache.kafka.connect.storage.StringConverter',
    'value.converter' = 'io.confluent.connect.avro.AvroConverter',
    'value.converter.schema.registry.url' = 'http://schema-registry:8081'
);
```

And insert new data to aggregate :

``` 
INSERT INTO INPUT_DATA (line) VALUES ('2020-06-16T04;1;appli1;Start');
INSERT INTO INPUT_DATA (line) VALUES ('2020-06-16T04;1;appli1;event1');
INSERT INTO INPUT_DATA (line) VALUES ('2020-06-16T04;1;appli1;End');
INSERT INTO INPUT_DATA (line) VALUES ('2020-06-16T04;2;appli2;Start');
INSERT INTO INPUT_DATA (line) VALUES ('2020-06-16T04;2;appli2;event2');
INSERT INTO INPUT_DATA (line) VALUES ('2020-06-16T04;2;appli2;End');
INSERT INTO INPUT_DATA (line) VALUES ('2020-06-16T04;3;appli3;Start');
```

The *OUTPUT_DATA* STREAM contains the results of the aggregation, results we can display via :

``` 
SELECT * FROM OUTPUT_DATA EMIT CHANGES;
```

To then decompose the generated STRUCT and only manipulate a ARRAY<VARCHAR>, we can use the following command (only possible with the versions 0.17.0 or higher of ksqlDB) :

``` 
CREATE STREAM CORRECT_DATA AS SELECT transform(LINEAG, s => s->LINE) AS LINEAG FROM OUTPUT_DATA EMIT CHANGES;
```

and we can display its result with :

``` 
SELECT * FROM CORRECT_DATA EMIT CHANGES;
```

With this example, the data in the **INPUT_DATA** STREAM corresponds to :

| LINE                          |                                                                                        
|:-----------------------------:|
| 2020-06-16T04;1;appli1;Start  | 
| 2020-06-16T04;1;appli1;event1 |
| 2020-06-16T04;1;appli1;End    |
| 2020-06-16T04;2;appli2;Start  |
| 2020-06-16T04;2;appli2;event2 |
| 2020-06-16T04;2;appli2;End    |
| 2020-06-16T04;3;appli3;Start  | 

After the aggregation, the result in the **OUTPUT_DATA** STREAM should be as follows (the second row only appears after 30 seconds, which corresponds to the **threshold.timeoutInSeconds** property) :

| LINEAG                                                                                                                                                                                                                        |                                                                                        
|:-----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------:|
| [{LINE=2020-06-16T04;1;appli1;Start}, {LINE=2020-06-16T04;1;appli1;event1}, {LINE=2020-06-16T04;1;appli1;End}, {LINE=2020-06-16T04;2;appli2;Start}, {LINE=2020-06-16T04;2;appli2;event2}, {LINE=2020-06-16T04;2;appli2;End}]  | 
| [{LINE=2020-06-16T04;3;appli3;Start}]|

And the result in the **CORRECT_DATA** STREAM should be :

| LINEAG                                                                                                                                                                                                                        |                                                                                        
|:----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------:|
| [2020-06-16T04;1;appli1;Start, 2020-06-16T04;1;appli1;event1, 2020-06-16T04;1;appli1;End, 2020-06-16T04;2;appli2;Start, 2020-06-16T04;2;appli2;event2, 2020-06-16T04;2;appli2;End] | 
| [2020-06-16T04;3;appli3;Start] |

Here, the first row corresponds to the aggregation of the 6 first lines of **INPUT_DATA** as the **threshold.elementNumber** property of the connector was equal to 6, and the second row corresponds to the aggregation of only the last line of **INPUT_DATA** as the **threshold.timeoutInSeconds** property of the connector is equal to 30 and in the 30 seconds that followed the flush of the first result, only "2020-06-16T04;3;appli3;Start" was received by the connector

When the testing is done, you can delete the connector with :

``` 
DROP CONNECTOR AGGREGATIONCONNECTORTEST;
```

#### Example 2

First, start by using the following command to apply modifications of a STREAM on data inserted before the creation/display of the STREAM :

``` 
SET 'auto.offset.reset'='earliest';
```

In this second example, we will aggregate messages with a single column of type :

``` 
dataArray ARRAY<STRUCT<columnID INT, text VARCHAR, quote BOOLEAN>>
```

In order to show how the aggregation works on a more complex type, but the principles are the same as the ones of the **Example 1**

The first command to write in ksqlDB creates the STREAM that will aliment the aggregation connector with data :

``` 
CREATE STREAM INPUT_DATA2 (
	dataArray ARRAY<STRUCT<columnID INT, text VARCHAR, quote BOOLEAN>>
) WITH (
	KAFKA_TOPIC='aggregation_input2', 
	PARTITIONS=1, 
	REPLICAS=1, 
	VALUE_FORMAT='AVRO'
);
```

We then add a STREAM over the output topic to manipulate the aggregated data (please remember to use : **aggregationColumnName** ARRAY<STRUCT<...[INPUT_DATA columns]...>>) :

``` 
CREATE STREAM OUTPUT_DATA2 (
	LINEAG ARRAY<STRUCT<DATAARRAY ARRAY<STRUCT<columnID INT, text VARCHAR, quote BOOLEAN>>>>
) WITH (
	KAFKA_TOPIC='aggregation_output2', 
	PARTITIONS=1, 
	REPLICAS=1, 
	VALUE_FORMAT='AVRO'
);
```

We can then create the connector (with the correct **aggregationColumnName**) :

``` 
CREATE SINK CONNECTOR AggregationConnectorTest2 WITH (
    'connector.class' = 'com.igrafx.kafka.sink.aggregation.adapters.AggregationSinkConnector', 
    'tasks.max' = '1',
    'topics' = 'aggregation_input2',
    'topicOut' = 'aggregation_output2',
    'aggregationColumnName' = 'LINEAG',
    'threshold.elementNumber' = '3',
    'threshold.timeoutInSeconds' = '30',
    'bootstrap.servers' = 'broker:29092',
    'key.converter' = 'org.apache.kafka.connect.storage.StringConverter',
    'value.converter' = 'io.confluent.connect.avro.AvroConverter',
    'value.converter.schema.registry.url' = 'http://schema-registry:8081'
);
```

And insert new data to aggregate :

``` 
INSERT INTO INPUT_DATA2 (dataArray) VALUES (ARRAY[STRUCT(columnId := 0, text := '3', quote := false), STRUCT(columnId := 1, text := 'A', quote := true), STRUCT(columnId := 2, text := '10/10/10 08:05', quote := false), STRUCT(columnId := 3, text := '10/10/10 08:10', quote := false)]);
INSERT INTO INPUT_DATA2 (dataArray) VALUES (ARRAY[STRUCT(columnId := 0, text := '3', quote := false), STRUCT(columnId := 1, text := 'B', quote := true), STRUCT(columnId := 2, text := '10/10/10 08:15', quote := false), STRUCT(columnId := 3, text := '10/10/10 08:16', quote := false)]);
INSERT INTO INPUT_DATA2 (dataArray) VALUES (ARRAY[STRUCT(columnId := 0, text := '3', quote := false), STRUCT(columnId := 1, text := 'C', quote := true), STRUCT(columnId := 2, text := '10/10/10 08:16', quote := false), STRUCT(columnId := 3, text := '10/10/10 08:17', quote := false)]);
INSERT INTO INPUT_DATA2 (dataArray) VALUES (ARRAY[STRUCT(columnId := 0, text := '3', quote := false), STRUCT(columnId := 1, text := 'D', quote := true), STRUCT(columnId := 2, text := '10/10/10 08:26', quote := false), STRUCT(columnId := 3, text := '10/10/10 08:27', quote := false)]);
INSERT INTO INPUT_DATA2 (dataArray) VALUES (ARRAY[STRUCT(columnId := 0, text := '3', quote := false), STRUCT(columnId := 1, text := 'E', quote := true), STRUCT(columnId := 2, text := '10/10/10 08:29', quote := false), STRUCT(columnId := 3, text := '10/10/10 08:31', quote := false)]);
INSERT INTO INPUT_DATA2 (dataArray) VALUES (ARRAY[STRUCT(columnId := 0, text := '3', quote := false), STRUCT(columnId := 1, text := 'F', quote := true), STRUCT(columnId := 2, text := '10/10/10 08:29', quote := false), STRUCT(columnId := 3, text := '10/10/10 08:31', quote := false)]);
INSERT INTO INPUT_DATA2 (dataArray) VALUES (ARRAY[STRUCT(columnId := 0, text := '3', quote := false), STRUCT(columnId := 1, text := 'G', quote := true), STRUCT(columnId := 2, text := '10/10/10 08:31', quote := false), STRUCT(columnId := 3, text := '10/10/10 08:32', quote := false)]);
```

The *OUTPUT_DATA2* STREAM contains the results of the aggregation, results we can display via :

``` 
SELECT * FROM OUTPUT_DATA2 EMIT CHANGES;
```

To then decompose the generated STRUCT and only manipulate a ARRAY<ARRAY<STRUCT<columnID INT, text VARCHAR, quote BOOLEAN>>>, we can use the following command (only possible with the versions 0.17.0 or higher of ksqlDB) :

``` 
CREATE STREAM CORRECT_DATA2 AS SELECT transform(LINEAG, s => s->DATAARRAY) AS LINEAG FROM OUTPUT_DATA2 EMIT CHANGES;
```

and we can display its result with :

``` 
SELECT * FROM CORRECT_DATA2 EMIT CHANGES;
```

With this example, the data in the **INPUT_DATA2** STREAM corresponds to :

| DATAARRAY                                                                                                                                                             |                                                                                        
|:---------------------------------------------------------------------------------------------------------------------------------------------------------------------:|
| [{COLUMNID=0, TEXT=3, QUOTE=false}, {COLUMNID=1, TEXT=A, QUOTE=true}, {COLUMNID=2, TEXT=10/10/10 08:05, QUOTE=false}, {COLUMNID=3, TEXT=10/10/10 08:10, QUOTE=false}] | 
| [{COLUMNID=0, TEXT=3, QUOTE=false}, {COLUMNID=1, TEXT=B, QUOTE=true}, {COLUMNID=2, TEXT=10/10/10 08:15, QUOTE=false}, {COLUMNID=3, TEXT=10/10/10 08:16, QUOTE=false}] |
| [{COLUMNID=0, TEXT=3, QUOTE=false}, {COLUMNID=1, TEXT=C, QUOTE=true}, {COLUMNID=2, TEXT=10/10/10 08:16, QUOTE=false}, {COLUMNID=3, TEXT=10/10/10 08:17, QUOTE=false}] |
| [{COLUMNID=0, TEXT=3, QUOTE=false}, {COLUMNID=1, TEXT=D, QUOTE=true}, {COLUMNID=2, TEXT=10/10/10 08:26, QUOTE=false}, {COLUMNID=3, TEXT=10/10/10 08:27, QUOTE=false}] |
| [{COLUMNID=0, TEXT=3, QUOTE=false}, {COLUMNID=1, TEXT=E, QUOTE=true}, {COLUMNID=2, TEXT=10/10/10 08:29, QUOTE=false}, {COLUMNID=3, TEXT=10/10/10 08:31, QUOTE=false}] |
| [{COLUMNID=0, TEXT=3, QUOTE=false}, {COLUMNID=1, TEXT=F, QUOTE=true}, {COLUMNID=2, TEXT=10/10/10 08:29, QUOTE=false}, {COLUMNID=3, TEXT=10/10/10 08:31, QUOTE=false}] |
| [{COLUMNID=0, TEXT=3, QUOTE=false}, {COLUMNID=1, TEXT=G, QUOTE=true}, {COLUMNID=2, TEXT=10/10/10 08:31, QUOTE=false}, {COLUMNID=3, TEXT=10/10/10 08:32, QUOTE=false}] | 

After the aggregation, the result in the **OUTPUT_DATA2** STREAM should be as follows (the third row only appears after 30 seconds, which corresponds to the **threshold.timeoutInSeconds** property) :

| LINEAG                                                                                                                                                                                                                        |                                                                                        
|:-----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------:|
| [{DATAARRAY=[{COLUMNID=0, TEXT=3, QUOTE=false}, {COLUMNID=1, TEXT=A, QUOTE=true}, {COLUMNID=2, TEXT=10/10/10 08:05, QUOTE=false}, {COLUMNID=3, TEXT=10/10/10 08:10, QUOTE=false}]}, {DATAARRAY=[{COLUMNID=0, TEXT=3, QUOTE=false}, {COLUMNID=1, TEXT=B, QUOTE=true}, {COLUMNID=2, TEXT=10/10/10 08:15, QUOTE=false}, {COLUMNID=3, TEXT=10/10/10 08:16, QUOTE=false}]}, {DATAARRAY=[{COLUMNID=0, TEXT=3, QUOTE=false}, {COLUMNID=1, TEXT=C, QUOTE=true}, {COLUMNID=2, TEXT=10/10/10 08:16, QUOTE=false}, {COLUMNID=3, TEXT=10/10/10 08:17, QUOTE=false}]}] |
| [{DATAARRAY=[{COLUMNID=0, TEXT=3, QUOTE=false}, {COLUMNID=1, TEXT=D, QUOTE=true}, {COLUMNID=2, TEXT=10/10/10 08:26, QUOTE=false}, {COLUMNID=3, TEXT=10/10/10 08:27, QUOTE=false}]}, {DATAARRAY=[{COLUMNID=0, TEXT=3, QUOTE=false}, {COLUMNID=1, TEXT=E, QUOTE=true}, {COLUMNID=2, TEXT=10/10/10 08:29, QUOTE=false}, {COLUMNID=3, TEXT=10/10/10 08:31, QUOTE=false}]}, {DATAARRAY=[{COLUMNID=0, TEXT=3, QUOTE=false}, {COLUMNID=1, TEXT=F, QUOTE=true}, {COLUMNID=2, TEXT=10/10/10 08:29, QUOTE=false}, {COLUMNID=3, TEXT=10/10/10 08:31, QUOTE=false}]}] |
| [{DATAARRAY=[{COLUMNID=0, TEXT=3, QUOTE=false}, {COLUMNID=1, TEXT=G, QUOTE=true}, {COLUMNID=2, TEXT=10/10/10 08:31, QUOTE=false}, {COLUMNID=3, TEXT=10/10/10 08:32, QUOTE=false}]}] |

And the result in the **CORRECT_DATA2** STREAM should be :

| LINEAG                                                                                                                                                                                                                        |                                                                                        
|:----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------:|
| [[{COLUMNID=0, TEXT=3, QUOTE=false}, {COLUMNID=1, TEXT=A, QUOTE=true}, {COLUMNID=2, TEXT=10/10/10 08:05, QUOTE=false}, {COLUMNID=3, TEXT=10/10/10 08:10, QUOTE=false}], [{COLUMNID=0, TEXT=3, QUOTE=false}, {COLUMNID=1, TEXT=B, QUOTE=true}, {COLUMNID=2, TEXT=10/10/10 08:15, QUOTE=false}, {COLUMNID=3, TEXT=10/10/10 08:16, QUOTE=false}], [{COLUMNID=0, TEXT=3, QUOTE=false}, {COLUMNID=1, TEXT=C, QUOTE=true}, {COLUMNID=2, TEXT=10/10/10 08:16, QUOTE=false}, {COLUMNID=3, TEXT=10/10/10 08:17, QUOTE=false}]] | 
| [[{COLUMNID=0, TEXT=3, QUOTE=false}, {COLUMNID=1, TEXT=D, QUOTE=true}, {COLUMNID=2, TEXT=10/10/10 08:26, QUOTE=false}, {COLUMNID=3, TEXT=10/10/10 08:27, QUOTE=false}], [{COLUMNID=0, TEXT=3, QUOTE=false}, {COLUMNID=1, TEXT=E, QUOTE=true}, {COLUMNID=2, TEXT=10/10/10 08:29, QUOTE=false}, {COLUMNID=3, TEXT=10/10/10 08:31, QUOTE=false}], [{COLUMNID=0, TEXT=3, QUOTE=false}, {COLUMNID=1, TEXT=F, QUOTE=true}, {COLUMNID=2, TEXT=10/10/10 08:29, QUOTE=false}, {COLUMNID=3, TEXT=10/10/10 08:31, QUOTE=false}]] |
| [[{COLUMNID=0, TEXT=3, QUOTE=false}, {COLUMNID=1, TEXT=G, QUOTE=true}, {COLUMNID=2, TEXT=10/10/10 08:31, QUOTE=false}, {COLUMNID=3, TEXT=10/10/10 08:32, QUOTE=false}]] |

Here, the first two rows correspond to the aggregation of the 6 first lines of **INPUT_DATA2** as the **threshold.elementNumber** property of the connector was equal to 3 (so it makes 2 rows with 3 lines in each aggregation), and the third row corresponds to the aggregation of only the last line of **INPUT_DATA2** as the **threshold.timeoutInSeconds** property of the connector is equal to 30 and in the 30 seconds that followed the flush of the first result, only "[{COLUMNID=0, TEXT=3, QUOTE=false}, {COLUMNID=1, TEXT=G, QUOTE=true}, {COLUMNID=2, TEXT=10/10/10 08:31, QUOTE=false}, {COLUMNID=3, TEXT=10/10/10 08:32, QUOTE=false}]" was received by the connector

When the testing is done, you can delete the connector with :

``` 
DROP CONNECTOR AGGREGATIONCONNECTORTEST2;
```
