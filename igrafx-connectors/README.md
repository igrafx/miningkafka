# iGrafx Kafka Connectors:

The **iGrafx Kafka Connectors** module provides connectors specifically designed to streamline data flow in and out of Kafka. These connectors enable seamless integration with various data sources and destinations, allowing users to effortlessly move data into Kafka for analysis or export it for further processing.

There are 2 existing connectors in the **iGrafx Kafka Connectors** module:

- **iGrafx Aggregation** : aggregates Kafka records from a same partition.
- **iGrafx Aggregation Main** : aggregates Kafka records from a same partition and send the aggregation result to the iGrafx Mining API.

## iGrafx Aggregation:

* module : aggregation
* package : com.igrafx.kafka.sink.aggregation

This connector is designed to aggregate multiple records originating from the same partition into a single, structured array. For example, if the incoming data contains two columns with the following types:

 ``` 
 LINE1 VARCHAR,
 LINE2 VARCHAR
 ```

The result of the aggregation will be sent to a Kafka topic in the following format:

``` 
LINEAG ARRAY<STRUCT<LINE1 VARCHAR, LINE2 VARCHAR>>
```


Each record from Kafka will be aggregated with others in an array structure.
In this case, **LINEAG** is used as the value for the `aggregationColumnName` connector property, defining the name of the aggregated column.

Here, the aggregation column:
``` 
LINEAG ARRAY<STRUCT<...>>
```

that is appended over the columns of the incoming data to represent the aggregation results.
The **ARRAY** contains the different aggregated results, while the **STRUCT** preserves the various columns of the input data.

The aggregation is triggered based on several thresholds:

* **Element Number:** When the number of aggregated elements reaches a specified count, the aggregation result is sent to Kafka.
* **Value Pattern:** A regular expression (ReGex) pattern can be defined to flush the current aggregation to Kafka if an incoming sink record's value matches this pattern.
* **Timeout:** After a certain period since the last aggregation was sent, the current aggregated data is pushed to Kafka, even if the element count threshold hasn't been met.
* **Retention:** This threshold is governed by the **retention.ms** configuration in the source Kafka topic. It is not set by the user in the connector’s properties but can impact data retention within the aggregation window. See the Retention section below for more details.

>**Note**: The aggregation schema is obtained from the Kafka output topic specified by the **topicOut** property. Therefore, this topic’s schema must be created before any data is sent to the connector.


## Connector Properties

To set up the connector, specify the following properties (example values provided):


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

Certain properties should remain fixed:

* **connector.class** (String): Specifies the connector class to be used.
* **key.converter** (String): Defines the converter for Kafka record keys.
* **value.converter** (String): Defines the converter for Kafka record values.

The other properties can be customized based on your requirements:

* **tasks.max** (Int): Number of tasks to instantiate for this connector.
* **topics** (String): List of Kafka topics that contain the data to aggregate.
* **topicOut** (String): Kafka topic where aggregated results are contained.
* **aggregationColumnName**: Name of the column that stores the aggregation result in ksqlDB.
* **threshold.elementNumber** (Int): Maximum number of elements in a single aggregation batch; once reached, the aggregation is sent to **topicOut**.
* **threshold.valuePattern** (String): *Optional.* A regex pattern that, when matched by the incoming record’s value, triggers the aggregation to be published immediately to **topicOut**, regardless of the element count. If this property is not defined, this threshold will not apply. Note that the pattern will need to align with the format of the SinkRecord (e.g., JSON structure).
* **threshold.timeoutInSeconds** (Int): Maximum time (in seconds) since the last aggregation was sent; once exceeded, the current aggregation is pushed to **topicOut**, even if **threshold.elementNumber** is not met.
* **bootstrap.servers** (String): List of Kafka brokers.
* **value.converter.schema.registry.url**: URL for the Kafka Schema Registry.

For more information on regular expressions used in **threshold.valuePattern**, refer to [this regex guide](https://medium.com/factory-mind/regex-tutorial-a-simple-cheatsheet-by-examples-649dc1c3f285).

## AVRO

To maintain data type consistency during aggregation, the ksqlDB STREAMs (applied to both input and output topics) must use the **AVRO** format for their values.

The connector retrieves the AVRO schema associated with the output topic in ksqlDB via HTTP. It then adjusts the schema of incoming Kafka records to align with this output schema, ensuring compatibility and data integrity throughout the aggregation process.

## Maximum Message Size

Kafka imposes a default size limit for individual messages sent to a topic. By default, messages cannot exceed **1048588 bytes** in size.

However, this limit can be adjusted in the **docker-compose** file by modifying the following properties to accommodate larger message sizes. In this example, the maximum size is set to **20000000 bytes**; adjust this value based on your needs.

In the **broker** service, add:

```
KAFKA_MESSAGE_MAX_BYTES: 20000000
KAFKA_MAX_REQUEST_SIZE: 20000000
KAFKA_PRODUCER_MAX_REQUEST_SIZE: 20000000
KAFKA_REPLICA_FETCH_MAX_BYTES: 20000000
```

In the **connect** service, add :

``` 
CONNECT_PRODUCER_MAX_REQUEST_SIZE: 20000000
CONNECT_CONSUMER_MAX_PARTITION_FETCH_BYTES: 20000000
```

In the **ksqldb-server** service, add :

``` 
KSQL_OPTS: "-Dmax.request.size=20000000"
```


> **Note**: Adjusting message sizes beyond the default may negatively impact Kafka performance. Large messages often require a dedicated Kafka cluster, separate from clusters handling regular data, to manage these configurations efficiently.

**Important:** The connector relies on the **max.message.bytes** configuration of the output Kafka topic to determine the maximum allowable message size for aggregation results.

If an aggregation exceeds the size set by **max.message.bytes**, the connector will divide the aggregation into multiple messages. For instance, if **max.message.bytes** is set to 1000000 bytes, and the aggregation size is 1500000 bytes, the connector will split the aggregation, sending two messages: one of approximately 900000 bytes (leaving a buffer) and another of 600000 bytes.


## iGrafx Aggregation Main (Aggregation and iGrafx Sink Connector)

* module : aggregationMain
* package : com.igrafx.kafka.sink.aggregationmain

This connector leverages the aggregation capabilities of the standard aggregation connector (explained in the last section) to combine multiple events, but it also sends the aggregation results directly to the iGrafx Mining API. Typically, before sending data to the iGrafx Mining API, multiple records representing process events are aggregated together, formatted into a CSV file, and then transmitted to the API. The Aggregation iGrafx Sink Connector automates this process.

Using this connector, events are pulled from Kafka, aggregated, and sent as a file to the iGrafx Mining API when a specified threshold is reached per partition. Unlike the standard aggregation connector, this module is dedicated to iGrafx data handling and bypasses Kafka’s message size limitations since the data destination is not a Kafka topic.

Additionally, the connector allows for creating column mappings for an iGrafx project directly from the connector, and it can also send log events to a Kafka topic.

The connector performs aggregations based on four thresholds:

* **Element Number**: When the aggregation reaches a specified number of elements, the result is sent to the iGrafx Mining API.
* **Value Pattern**: A regex pattern can be defined to flush the current aggregation to the iGrafx Mining API if the value of an incoming sink record matches the pattern.
* **Timeout**: If a specified amount of time has elapsed since the last aggregation result was sent, the current aggregated data is sent (regardless of the element number threshold).
* **Retention**: This threshold is not user-defined but is based on the **retention.ms** configuration of the Kafka topic from which data originates. For more details, see the retention section below.

## Connector properties

To instantiate the connector, we just have to fill in the following properties :

```
connector.class = "com.igrafx.kafka.sink.main.domain.IGrafxAggregationSinkConnector",
tasks.max = "1",
topics = "igrafx_topic_example",
api.url = "api_url_example",
api.authUrl = "auth_url_example",
workGroupId = "w_id_example",
workGroupKey = "w_key_example",
projectId = "project_id_example",
csv.encoding = "UTF-8",
csv.separator = ",",
csv.quote = """,
csv.fieldsNumber = "9",
csv.header = "true",
csv.defaultTextValue = "null",
retentionTimeInDay = "100",
columnMapping.create = "true",
columnMapping.caseIdColumnIndex = "0",
columnMapping.activityColumnIndex = "1",
columnMapping.timeInformationList = "{2;dd/MM/yy HH:mm},{3;dd/MM/yy HH:mm}",
columnMapping.dimensionsInformationList = "[{"columnIndex": 4, "name": "Country", "isCaseScope": true, "aggregation": "FIRST", "groupedTasksAggregation": "FIRST"},{"columnIndex": 5, "name": "Region", "isCaseScope": false, "groupedTasksAggregation": "FIRST"},{"columnIndex": 6, "name": "City", "isCaseScope": false, "groupedTasksAggregation": "LAST"}]",
columnMapping.metricsInformationList = "[{"columnIndex": 7, "name": "Price", "unit": "Euros", "isCaseScope": true, "aggregation": "MIN", "groupedTasksAggregation": "AVG"},{"columnIndex": 8, "name": "DepartmentNumber", "isCaseScope": false, "groupedTasksAggregation": "FIRST"}]",
columnMapping.groupedTasksColumns = "[1, 2, 3]",
csv.endOfLine = "\\n",
csv.escape = "\",
csv.comment = "#",
kafkaLoggingEvents.isLogging = "true",
kafkaLoggingEvents.topic = "event_logging_topic_example",
threshold.elementNumber = "6",
threshold.valuePattern = ".*regex_example.*",
threshold.timeoutInSeconds = "3000",
bootstrap.servers = "broker:29092",
key.converter = "org.apache.kafka.connect.storage.StringConverter",
value.converter = "io.confluent.connect.avro.AvroConverter",
value.converter.schema.registry.url = "http://schema-registry:8081"
```

**Warning**: It is necessary to escape the backslash character.

## Mandatory Properties

Below are examples of values for required properties. The following properties, however, should remain unchanged:

* **connector.class** (String)
* **key.converter** (String)
* **value.converter** (String)

You may modify the following properties as needed:

* **tasks.max** (Int): Specifies the number of tasks to create for the connector.
* **api.url** (String): API URL of the iGrafx Mining API for file transfer.
* **api.authUrl** (String): URL for authentication to obtain a connection token.
* **workGroupId** (String): ID of the workgroup associated with the iGrafx project.
* **workGroupKey** (String): Key of the workgroup associated with the iGrafx project.
* **projectId** (String): ID of the iGrafx project.
* **csv.encoding** (String): Encoding for the file (generally *UTF-8*).
* **csv.separator** (String): Field separator character in the CSV file (*only one character*).
* **csv.quote** (String): Quote character (*only one character*).
* **csv.fieldsNumber** (Int): Number of fields per line (*must be >= 3*).
* **csv.header** (Boolean): Indicates whether the CSV file includes a header (*true/false*).
* **csv.defaultTextValue** (String): Default value for missing fields in the file.
* **retentionTimeInDay** (Int): Archive file retention time in **days** (*must be > 0*).
* **threshold.elementNumber** (Int): Maximum number of elements in one aggregation; aggregation is sent to the iGrafx Mining API when this number is reached.
* **threshold.valuePattern** (String): **Optional**—triggers an aggregation flush to the iGrafx Mining API if an incoming sink record matches the **regex** pattern in **threshold.valuePattern** (applies even if **threshold.elementNumber** is not met). The pattern applies to the entire string value of a SinkRecord, and may need adjustments based on the data structure (e.g., accounting for `{}` in JSON format).
* **threshold.timeoutInSeconds** (Int): Maximum time in seconds since the last aggregation result was sent. If exceeded, the aggregation is sent to the iGrafx Mining API (even if **threshold.elementNumber** is not met).
* **bootstrap.servers** (String): List of Kafka brokers.
* **value.converter.schema.registry.url** (String): URL of the Confluent Schema Registry.

For more information on regex (used with the **threshold.valuePattern** property): [Regex Cheat Sheet](https://medium.com/factory-mind/regex-tutorial-a-simple-cheatsheet-by-examples-649dc1c3f285)

## Optional Properties

The following properties are only necessary if the connector should create a Column Mapping for the iGrafx Project:

* **columnMapping.create** (Boolean): Specifies whether the connector creates the project’s Column Mapping (*true/false*). If set to **true**, all subsequent properties must be defined; if **false**, the following properties can be omitted.
* **columnMapping.caseIdColumnIndex** (Int): Index of the CaseId column (*must be >= 0*).
* **columnMapping.activityColumnIndex** (Int): Index of the Activity Column (*must be >= 0*).
* **columnMapping.timeInformationList** (String): Specifies Time columns in the format `{columnIndex;dateFormat}`, separated by a comma if there are two columns. At least one and at most two columns must be provided. *columnIndex* should be an *Int >= 0*, and *dateFormat* should be a *non-empty String*.
* **columnMapping.dimensionsInformationList** (String): Specifies Dimension columns in JSON format, with the following structure: *columnIndex* as an *Int >= 0*, *columnName* as a *non-empty String*, and **isCaseScope** as a boolean that indicates whether the column’s value is calculated for an entire case. Valid aggregations for Dimensions are "FIRST", "LAST", and "DISTINCT", specified with the **aggregation** parameter. If **isCaseScope** is set to true, an aggregation type is required; if **false**, **aggregation** is optional. When **columnMapping.groupedTasksColumns** is defined, each dimension must include the **groupedTasksAggregation** parameter, choosing from "FIRST" and "LAST".
* **columnMapping.metricsInformationList** (String): Defines Metric columns in JSON format. Each *columnIndex* should be an *Int >= 0*, *columnName* a *non-empty String*, and **isCaseScope** a boolean indicating whether the column’s value is computed for an entire case. For Metrics, valid aggregation types are "FIRST", "LAST", "MIN", "MAX", "SUM", "AVG", and "MEDIAN", as defined by the **aggregation** parameter. If **isCaseScope** is true, an aggregation type is required; if **false**, **aggregation** is optional. When **columnMapping.groupedTasksColumns** is specified, each metric must include the **groupedTasksAggregation** parameter, with options for "FIRST", "LAST", "MIN", "MAX", "SUM", "AVG", and "MEDIAN". Additionally, **unit** is an optional *String* parameter.
* **columnMapping.groupedTasksColumns** (String): Defines columns used for grouping events, formatted as a JSON List. If not specified, events are not grouped. If defined, at least one time/dimension/metric column index should be included. When this property is set, all dimensions (*columnMapping.dimensionsInformationList*) and metrics (*columnMapping.metricsInformationList*) must include a groupedTasksAggregation parameter.
* **csv.endOfLine** (String): Specifies the end-of-line character (*minimum length 1*).
* **csv.escape** (String): Defines the escape character (*only one character*).
* **csv.comment** (String): Sets the comment character (*only one character*).

Characters like `{}`, `;`, and `,` used in formatting Time columns can be customized in the **com/igrafx/kafka/sink/main/Constants** file.

If **csv.header** is true and the connector creates a Column Mapping in the iGrafx project, then the headers in generated files will align with the Column Mapping column names. If **csv.header** is true but the connector doesn’t create a Column Mapping, headers will simply include **csv.fieldsNumber - 1** separator characters, as defined by **csv.separator**.

The following properties should be defined only if you want the connector to log file-related events to a Kafka topic (see the Logging Events section below):

* **kafkaLoggingEvents.isLogging** (Boolean): Determines if the connector logs file-related events to a Kafka topic (*true/false*). If **true**, events will be logged to a Kafka topic; if **false** (the default), they won’t.
* **kafkaLoggingEvents.topic** (String): Specifies the Kafka topic name for logging events (*minimum length 1*).

## AVRO

This connector requires data in AVRO format; other formats may lead to errors.

Each record from Kafka should match the following structure, verified by comparing the schema to the AVRO record:

```
ARRAY<STRUCT<columnID INT, text VARCHAR, quote BOOLEAN>>
```


The **Array** represents one event (which corresponds to one line in the CSV file), with each **STRUCT** in the Array representing a column of the event (a field in the CSV file, like the *caseId* or *activity*).

Thus, one record from Kafka equates to one event, and the connector aggregates multiple events. When a threshold is met, these aggregated events are written to the same file, which is then sent to the iGrafx API.

To correctly write a field to the CSV file, the following are needed:

* The column number (**columnId**),
* The value (**text**),
* Whether or not the field is quoted (**quote**).

For example, the following data from a Kafka topic (illustrated here in JSON format but actually in AVRO):

```json
{
    "DATAARRAY": [
        {"QUOTE": true, "TEXT": "activity1", "COLUMNID": 1},
        {"QUOTE": false, "TEXT": "caseId1", "COLUMNID": 0},
        {"QUOTE": false, "TEXT": "endDate1", "COLUMNID": 3}
    ]
}
````

will be written as the following line in the CSV file:

```
caseId1,"activity1",null,endDate1
```
If the following connector properties are set:

- csv.separator = ``,``
- csv.quote = ``"``
- csv.defaultTextValue = ``null``
- csv.fieldsNumber = ``4``

Note: The field names **DATAARRAY**, **QUOTE**, **TEXT**, and **COLUMNID** must be respected in ksqlDB to correctly read AVRO data from a Kafka topic.

Any null value for an event, a column in an event, or a parameter in a column is considered an error and will halt the Task.

## iGrafx API

The iGrafx API is used to send the CSV file to the API.
The file transfer to the API is handled in **adapters/api/MainApiImpl.scala** by the **sendCsvToIGrafx** method, which takes as parameters the connector's properties and the path of the file to send.

To send the file, follow these two steps:

1. **Retrieve the connection token**:  
   Use the URL path **{authUrl}/protocol/openid-connect/token**, where *{authUrl}* corresponds to the **api.authUrl** property of the connector. The request also includes details about the workgroup ID and workgroup Key. Upon success, the HTTP response contains a JSON object with an **access_token** key.

2. **Send the file**:  
   Use the URL **{apiUrl}/project/{projectId}/file?teamId={workGroupId}**, where *{apiUrl}* corresponds to the **api.url** property of the connector. This request requires information about the workgroup ID, project ID, file path, and the previously obtained token.

The Workgroup ID, Workgroup Key, API URL and API Auth URL can be found in the iGrafx workgroup settings, under the **Open API** tab.

## Kafka Logging Events
The connector has the possibility via its **`kafkaLoggingEvents.sendInformation`**, **`kafkaLoggingEvents.topic`**
properties to log file related events in a Kafka topic.

The AVRO schema expected for the Logging is the following :

```json
{
  "fields": [
    {
      "default": null,
      "name": "EVENT_TYPE",
      "type": [
        "null",
        "string"
      ]
    },
    {
      "default": null,
      "name": "IGRAFX_PROJECT",
      "type": [
        "null",
        "string"
      ]
    },
    {
      "default": null,
      "name": "EVENT_DATE",
      "type": [
        "null",
        "long"
      ]
    },
    {
      "default": null,
      "name": "EVENT_SEQUENCE_ID",
      "type": [
        "null",
        "string"
      ]
    },
    {
      "default": null,
      "name": "PAYLOAD",
      "type": [
        "null",
        "string"
      ]
    }
  ],
  "name": "IGrafxKafkaLoggingEventsSchema",
  "namespace": "io.confluent.ksql.avro_schemas",
  "type": "record"
}
```

An event is composed of :

* an **`eventType`** (String) : currently there are **pushFile** and **issuePushFile**
* a **`igrafxProject`** (String : UUID) : corresponds to the iGrafx Project ID to which we want to send  (the **projectId** connector's property)
* an **`eventDate`** (Long) : corresponds to the date of the event
* an **`eventSequenceId`** (String) : corresponds to the ID of the sequence of events related to a file
* a **`payload`** (String : JSON) : can contain any information related to a certain event type

To create a STREAM to manipulate those events in ksqlDB there are two possibilities :

* Create the following STREAM before sending any event to the Kafka Logging Events topic (creation of the topic with a correct schema) :

``` 
CREATE STREAM LOGGING_1 (
	EVENT_TYPE VARCHAR,
	IGRAFX_PROJECT VARCHAR,
	EVENT_DATE BIGINT,
	EVENT_SEQUENCE_ID VARCHAR,
	PAYLOAD VARCHAR
) WITH (
	KAFKA_TOPIC='event_logging_topic_example', 
	PARTITIONS=1, 
	REPLICAS=1, 
	VALUE_FORMAT='AVRO'
);
```

* Create the following STREAM after sending the first events to the Kafka Logging Events topic (the topic needs to exist with a correct schema) :

``` 
CREATE STREAM LOGGING_2 WITH (
	KAFKA_TOPIC='journalisation_connecteur_test',
	VALUE_FORMAT='AVRO'
);
```

<hr/>

For now, those 2 events are generated by the connector :

* **pushFile** : event generated when the sending of a file by the connector ended successfully

For this event, the information embedded in the **payload** is : the **file name (filename: String)**, the **event date (date: Long)**, and the **number of lines in the file (lineNumber: Int)**. Here is an example of payload for this event :

``` 
{
    "filename": "filename_example",
    "date": 3446454564,
    "lineNumber": 100
}
```

* **issuePushFile** : event generated when there was an issue during the creation/sending of a file

The information embedded in its **payload** is : the **file name (filename: String)**, the **event date (date: Long)**, and the **exception type (exceptionType: String)** corresponding to the name of the thrown exception. Here is an example of payload for this event :

``` 
{
    "filename": "filename_example",
    "date": 3446454564,
    "exceptionType": "com.igrafx.kafka.sink.main.domain.exceptions.SendFileException"
}
```

<hr/>

Table summarizing the meaning of the different events fields :

|                   | **eventType** |      **igrafxProject**       |                     **eventDate**                      |                                                 **eventSequenceId**                                                  |         **payload**         |
|-------------------|:-------------:|:----------------------------:|:------------------------------------------------------:|:--------------------------------------------------------------------------------------------------------------------:|:---------------------------:|
| **pushFile**      |   pushFile    | The ID of the iGrafx project | The date for which the file has been successfully sent |              MD5 hash of a String containing the source topic/partition/offset of the data in the file               |  filename/date/lineNumber   |
| **issuePushFile** | issuePushFile | The ID of the iGrafx project |                 The date of the issue                  | MD5 hash of a String containing the source topic/partition/offset of the data that should have been sent in the file | filename/date/exceptionType |

When the sending of the event to Kafka fails, there are two possibilities :

* if the event was an `issuePushFile` event, the exception stopping the Task is the one that occurred during the creation/sending of the file, prior to the event sending issue (but the event's exception is still logged)
* if the event was a `pushFile` event, the exception stopping the Task is the event's exception


## Connector Commonalities

## Offset Management

Offset management is handled in the code by the **PartitionTracker** class. Each partition of a topic specified in the **topics** property has an associated **PartitionTracker**,
ensuring that **aggregation is only performed on data coming from the same partition**.

The **PartitionTracker** maintains and uses three types of offsets:

* **Processed Offset**: Tracks the offset of the most recent record received for the relevant partition. This is managed by the `Put` function within the `AggregationSinkTask`.
* **Flushed Offset**: Represents the offset of the latest record that has been sent, along with its aggregation, to the Kafka topic defined by **topicOut**.
* **Commit Offset**: Refers to the offset of the last record that was flushed and has had its offset committed to Kafka. Once a record's offset is committed, it will not be reprocessed, even in cases of task error or rebalance.

Here’s how these offsets are managed in practice:

When a new `SinkRecord` arrives in the `Put` function of `AggregationSinkTask`, its offset is processed. Once a threshold (such as element count, value pattern, timeout, or retention) is met, the aggregation, including the record, is sent to Kafka, and the record’s offset is marked as flushed. When the `preCommit` method is triggered in `AggregationSinkTask`, all flushed offsets across each partition are committed, provided they weren’t already.

**At least once** delivery is guaranteed, meaning a record is considered fully processed only when its offset is committed.
Any record with a processed or flushed (but uncommitted) offset may be received again by the connector if a task failure or rebalance occurs. This ensures that a record, even if already flushed, could be reprocessed and sent again to Kafka under failure scenarios.

This design ensures a reliable **at least once** delivery model.

## Retention

Values in a Kafka topic are retained according to the **retention.ms** configuration. To prevent data loss during a connector crash, the connector must send aggregated data before any individual record in the aggregation reaches its retention limit in the input topic.

Although the connector temporarily stores received data during aggregation, if a record surpasses its retention time in Kafka and the connector crashes before sending the aggregation, the data will be lost and will not be recoverable upon restart. This is because it will no longer be available in the original Kafka topic.

To mitigate this, the connector is configured to send the aggregation for a partition if any record in the aggregation reaches **80%** of its retention time. However, if a crash occurs before this threshold and the connector is not restarted before the end of the retention period, that data will still be lost.

## Error Handling

To view the logs of the connector, use the following command from the directory where the `docker-compose.yml` file is located:
``` 
docker-compose logs -f connect
```
Here, *`connect`* refers to the Kafka Connect service name specified in the `docker-compose.yml` file. For more detailed DEBUG-level logs, add the following line to the **CONNECT_LOG4J_LOGGERS** configuration parameter in the **connect** service:

``` 
com.igrafx.kafka.sink.aggregation.adapters.AggregationSinkTask=DEBUG,com.igrafx.kafka.sink.aggregation.adapters.AggregationSinkConnector=DEBUG
```

## Compilation and Deployment with docker compose

To compile the connector and generate the **.jar** file needed for Kafka Connect, navigate to the root of the module (Aggregation or AggregationMain) and run:
```
sbt assembly
```

After compilation, locate the **aggregation-connector_{version}.jar** file (or **aggregation-main-connector_{version}.jar** for AggregationMain) in the **artifacts** directory.

Copy this file and paste it into the **docker-compose/connect-plugins/** directory in Docker Compose (create this directory if it doesn’t already exist).

Once the infrastructure is launched, the connector will be available for use.


## Creating and Adding a New Connector

### Creating a New Connector
You can create the source connector either in the ksqlDB CLI or with kafka-ui via :

```
CREATE SOURCE CONNECTOR ConnectorName WITH (
...
);
```

Or via the following command for a Sink Connector:

```
CREATE SINK CONNECTOR ConnectorName WITH (
...
);
```

### Adding a New Connector
To add a new connector, begin by including a module for it in the module’s **build.sbt** file. Then, create a class for your connector that extends either **SourceConnector** or **SinkConnector**, along with a class that extends **SourceTask** or **SinkTask** to define the tasks for the connector.

Additionally, thoroughly document the connector, detailing its functionality, usage instructions, and configurable properties.


## Connector Monitoring

It is possible to monitor a connector:

You can retrieve the state of a connector and its tasks in the CLI ksqlDB with the command:

```sql
SHOW CONNECTORS;
````
For errors that lead to the termination of a task, once the administrator has resolved the problem (for example, a permissions issue with writing a file), the task can be restarted with the following commands (these commands use the REST interface of Kafka Connect):

``` 
curl localhost:8083/connectors
```

This command retrieves the list of the connector's tasks and provides information about them, such as their ID and status.
```
curl localhost:8083/connectors/<connectorName>/status | jq 
```

Replace **connectorName** with the name of the connector retrieved with the previous command (case sensitive).

You can then restart the **FAILED** tasks with the command:
``` 
curl -X POST localhost:8083/connectors/connectorName/tasks/taskId/restart
```

Here, replace **taskId** with the ID of the task retrieved with the previous command, and **connectorName** with the name of the connector (case sensitive) from two commands ago.

For more information and commands about the monitoring of connectors/tasks, follow this link: [Confluent Documentation](https://docs.confluent.io/home/connect/monitoring.html).

It is important to note that when a task goes to the **FAILED** state, the partitions that it was responsible for are redistributed among the remaining **RUNNING** tasks. Consequently, if there is an error in the **put** method of the initial task, the same data may cause the same error in the newly assigned task, potentially leading to all tasks of the connector being stopped. In this case, you need to restart all **FAILED** tasks of the connector once the issue is resolved.

Moreover, if a worker leaves the cluster, the connectors/tasks associated with this worker enter the **UNASSIGNED** state for 5 minutes (default value of the **scheduled.rebalance.max.delay.ms** worker property). If the worker does not return after 5 minutes, the connectors/tasks are reassigned to new workers in the cluster.

If tasks are added or removed, the partitions can also be rebalanced and redistributed among the new number of tasks (partition rebalance).

