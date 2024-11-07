# How to Use the iGrafx Kafka Modules

This document provides guidance on installing and using the **iGrafx Kafka Modules**, which include the **iGrafx LiveConnect**, **iGrafx Connectors**, and **iGrafx UDFs**. It also offers examples and best practices for integrating with your Kafka environment.

The **iGrafx Kafka Modules** are open-source applications designed to enhance your data streaming and integration workflows. These modules enable real-time data processing and transformation, allowing you to connect, enrich, and analyze data across multiple platforms.

Using these modules, you can configure kafka connectors, define custom UDFs (User-Defined Functions), and enable live connections for seamless data streaming and analysis.

Please note that an iGrafx account is required to fully utilize these modules. For account setup, please contact iGrafx support.

Find the GitHub repository for the iGrafx Kafka Modules [here](https://github.com/igrafx/miningkafka).

***
## Table of Contents




## iGrafx Kafka Connectors:

The **iGrafx Kafka Connectors** module provides connectors specifically designed to streamline data flow in and out of Kafka. These connectors enable seamless integration with various data sources and destinations, allowing users to effortlessly move data into Kafka for analysis or export it for further processing.

There are 2 existing connectors in the **iGrafx Kafka Connectors** module:

- **iGrafx Aggregation** : aggregates Kafka records from a same partition.
- **iGrafx Aggregation Main** : aggregates Kafka records from a same partition and send the aggregation result to the iGrafx Mining API.

### iGrafx Aggregation:

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


#### Connector Properties

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

### AVRO

To maintain data type consistency during aggregation, the ksqlDB STREAMs (applied to both input and output topics) must use the **AVRO** format for their values.

The connector retrieves the AVRO schema associated with the output topic in ksqlDB via HTTP. It then adjusts the schema of incoming Kafka records to align with this output schema, ensuring compatibility and data integrity throughout the aggregation process.

### Maximum Message Size

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

This connector leverages the aggregation capabilities of the standard aggregation connector (explained in the last section) to combine multiple events, but it also sends the aggregation results directly to the iGrafx Mining API. Typically, before sending data to the iGrafx Mining API, multiple records representing process events are aggregated together, formatted into a CSV file, and then transmitted to the API. The Aggregation iGrafx Sink Connector automates this process.

Using this connector, events are pulled from Kafka, aggregated, and sent as a file to the iGrafx Mining API when a specified threshold is reached per partition. Unlike the standard aggregation connector, this module is dedicated to iGrafx data handling and bypasses Kafka’s message size limitations since the data destination is not a Kafka topic.

Additionally, the connector allows for creating column mappings for an iGrafx project directly from the connector, and it can also send log events to a Kafka topic.

The connector performs aggregations based on four thresholds:

* **Element Number**: When the aggregation reaches a specified number of elements, the result is sent to the iGrafx Mining API.
* **Value Pattern**: A regex pattern can be defined to flush the current aggregation to the iGrafx Mining API if the value of an incoming sink record matches the pattern.
* **Timeout**: If a specified amount of time has elapsed since the last aggregation result was sent, the current aggregated data is sent (regardless of the element number threshold).
* **Retention**: This threshold is not user-defined but is based on the **retention.ms** configuration of the Kafka topic from which data originates. For more details, see the retention section below.

### Connector properties

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

#### Mandatory Properties

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

#### Optional Properties

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

### AVRO Format

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

### iGrafx API

The iGrafx API is used to send the CSV file to the API.
The file transfer to the API is handled in **adapters/api/MainApiImpl.scala** by the **sendCsvToIGrafx** method, which takes as parameters the connector's properties and the path of the file to send.

To send the file, follow these two steps:

1. **Retrieve the connection token**:  
   Use the URL path **{authUrl}/protocol/openid-connect/token**, where *{authUrl}* corresponds to the **api.authUrl** property of the connector. The request also includes details about the workgroup ID and workgroup Key. Upon success, the HTTP response contains a JSON object with an **access_token** key.

2. **Send the file**:  
   Use the URL **{apiUrl}/project/{projectId}/file?teamId={workGroupId}**, where *{apiUrl}* corresponds to the **api.url** property of the connector. This request requires information about the workgroup ID, project ID, file path, and the previously obtained token.

The Workgroup ID, Workgroup Key, API URL and API Auth URL can be found in the iGrafx workgroup settings, under the **Open API** tab.


## Connector Commonalities

### Offset Management

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

### Retention

Values in a Kafka topic are retained according to the **retention.ms** configuration. To prevent data loss during a connector crash, the connector must send aggregated data before any individual record in the aggregation reaches its retention limit in the input topic.

Although the connector temporarily stores received data during aggregation, if a record surpasses its retention time in Kafka and the connector crashes before sending the aggregation, the data will be lost and will not be recoverable upon restart. This is because it will no longer be available in the original Kafka topic.

To mitigate this, the connector is configured to send the aggregation for a partition if any record in the aggregation reaches **80%** of its retention time. However, if a crash occurs before this threshold and the connector is not restarted before the end of the retention period, that data will still be lost.

### Error Handling

To view the logs of the connector, use the following command from the directory where the `docker-compose.yml` file is located:
``` 
docker-compose logs -f connect
```
Here, *connect* refers to the Kafka Connect service name specified in the `docker-compose.yml` file. For more detailed DEBUG-level logs, add the following line to the **CONNECT_LOG4J_LOGGERS** configuration parameter in the **connect** service:

``` 
com.igrafx.kafka.sink.aggregation.adapters.AggregationSinkTask=DEBUG,com.igrafx.kafka.sink.aggregation.adapters.AggregationSinkConnector=DEBUG
```

### Compilation and Deployment on LiveConnect

To compile the connector and generate the **.jar** file needed for Kafka Connect, navigate to the root of the project (Aggregation or AggregationMain) and run:
```
sbt assembly
```

After compilation, locate the **aggregation-connector_{version}.jar** file (or **aggregation-main-connector_{version}.jar** for AggregationMain) in the **artifacts** directory. 

Copy this file and paste it into the **docker-compose/connect-plugins/** directory in LiveConnect (create this directory if it doesn’t already exist).

Once LiveConnect is launched, the connector will be available for use.

### Connector Monitoring

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


## Adding a New Connector

To add a new connector, begin by including a module for it in the project’s **build.sbt** file. Then, create a class for your connector that extends either **SourceConnector** or **SinkConnector**, along with a class that extends **SourceTask** or **SinkTask** to define the tasks for the connector.

Additionally, thoroughly document the connector, detailing its functionality, usage instructions, and configurable properties.

## iGrafx UDFs

The **iGrafx UDFs** module offers a set of User-Defined Functions (UDFs) specifically designed to enhance data transformation and analysis within the Kafka ecosystem. These UDFs empower users to perform customized data manipulations and calculations directly in ksqlDB, enabling more efficient and targeted processing for insights and decision-making in real time.

For more information on ksqlDB UDFs, please refer to the following links:

* https://docs.ksqldb.io/en/latest/reference/user-defined-functions/
* https://docs.ksqldb.io/en/latest/how-to-guides/create-a-user-defined-function/

There are several iGrafx UDFs available in the **iGrafx UDFs** module.
They will be discussed in more detail in the following sections.

### iGrafx Case Events UDF

* name in ksqlDB : **igrafx_case_events**
* package : **com.igrafx.ksql.functions.caseevents.domain**

This User-Defined Function (UDF) retrieves detailed information related to specific case IDs within Druid, allowing users to access and analyze case-based data directly.

This function can be particularly useful in process mining and operational analytics, where case-centric data (such as customer journey steps or order fulfillment stages) is essential for generating insights.

#### Overview
This UDF retrieves information from the **_vertex** Druid DataSource related to a specific `caseId`. The information provided includes:

* `__time` (start date)
* `enddate` (end date)
* `vertex_name` (name of the vertex associated with the case)


To get information about this UDF direclty in ksqlDB, use the command :

``` 
DESCRIBE FUNCTION IGRAFX_CASE_EVENTS;
```

The UDF requires the following parameters:

- **caseId**: The case ID for which information is requested.
- **projectId**: The ID of the iGrafx project containing the data.
- **workgroupId**: The ID of the iGrafx workgroup.
- **workgroupKey**: The key for the iGrafx workgroup.
- **host**: The Druid host.
- **port**: The Druid connection port.

#### UDF Signature and Output Format
The UDF signature is as follows:
``` 
def igrafxCaseEvents(caseId: String, projectId: String, workgroupId: String, workgroupKey: String, host: String, port: String): util.List[Struct]
```

The output is an array of structs with the structure:
``` 
STRUCT<START_DATE VARCHAR(STRING), END_DATE VARCHAR(STRING), VERTEX_NAME VARCHAR(STRING)>
```
- **START_DATE** corresponds to the `__time` column.
- **END_DATE** corresponds to the `enddate` column.
- **VERTEX_NAME** corresponds to the `vertex_name` column.

An array of these structs is returned to provide information for each row associated with the specified `caseId`.

Furthermore, the SQL query executed by this UDF is as follows:
``` 
SELECT __time AS startdate, enddate, vertex_name AS vertexName
FROM "projectId_vertex"
WHERE vertex_name is not NULL AND caseid = 'caseIdParam'
```

In this query:
- **projectId** corresponds to the iGrafx project ID.
- **caseIdParam** is the `caseId` parameter provided to the UDF.


### iGrafx Sessions UDF

* name in ksqlDB : **igrafx_sessions**
* package : **com.igrafx.ksql.functions.sessions.domain**

This UDTF (User Defined Table Function) takes a collection of lines and organizes them into separate sessions. 
Each session groups related events, making it easier to analyze behavior patterns or activity sequences within a particular context. 

This function is particularly useful for breaking down continuous data into meaningful segments, helping with tasks like user session tracking, activity clustering, or time-based event grouping.

#### Overview
The **iGrafx Sessions UDF** is a tabular user-defined function that divides a collection of ordered lines into sessions. Each session is assigned a unique ID and represents a grouping of lines sharing a common attribute. Regular expressions (regex) are used to determine which lines belong to the same session, which lines start and end sessions, and which lines should be ignored.

To retrieve information about this UDF directly in ksqlDB, use the following command:
```
DESCRIBE FUNCTION IGRAFX_SESSIONS;
```

The UDF requires the following parameters:

* **inputLines** : Corresponds to the initial collection of rows
* **ignorePattern** : Regex describing the rows to ignore. Rows verifying this pattern won't be used for the sessions creation and won't be returned by the function
* **groupSessionPattern** : Regex to regroup lines having the same values for the specified columns. The session will be determined within these groups. For instance for lines with the following format :
  **timeStamp;userID;targetApp;eventType**  
  and for the following pattern :   
  ``.\*;(.\*);.\*;(.\*)``

  The group of a row will be determined by concatenating its userId and eventType columns values (because those columns are into brackets in the Regex)
* **startSessionPattern** : Regex describing the lines that can be considered as a Start of a session
* **endSessionPattern** : Regex describing the lines that can be considered as End of a session
* **sessionIdPattern** : Regex informing about the parts of the lines that will be used to create the sessionId. For instance for lines with the following format :
  **timeStamp;userID;targetApp;eventType**  
  and for the following pattern :   
  ``.\*;(.\*);(.\*);.\*``

  The sessionID will be created by concatenating the userId and targetApp columns (which are into brackets in the Regex)
* **isSessionIdHash** : A sessionId is created according to the columns specified in the **sessionIdPattern** parameter. If **isSessionIdHash** is **false**, then the sessionId will only correspond to the concatenation of the values of the columns specified in **sessionIdPattern**. But if **isSessionIdHash** is **true**, the result of this concatenation is hashed to create the sessionId. The Hash function used is **MD5**
* **isIgnoreIfNoStart** : Boolean indicating if sessions that don't have a line matching the **startSessionPattern** are kept or not. If **true**, the corresponding sessions are not returned. If **false**, they are returned
* **isIgnoreIfNoEnd** : Boolean indicating if sessions that don't have a line matching the **endSessionPattern** are kept or not. If **true**, the corresponding sessions are not returned. If **false**, they are returned

For more information about Regex follow this [link](https://medium.com/factory-mind/regex-tutorial-a-simple-cheatsheet-by-examples-649dc1c3f285).


#### UDF Signature and Output Format

This UDF takes a collection of rows, each containing several columns, as input. 
It first groups the rows based on shared values in specified columns, as defined by the `groupSessionPattern`. 
Rows with matching values in the designated columns are placed in the same group. 
Within each group, the function identifies sessions, where each session consists of a sequence of consecutive rows. 
Rows are classified as session starters, session enders, or session members based on their match with `startSessionPattern` and `endSessionPattern`, provided by the user. 
Thus, groups help organize and segment the input rows into cohesive sessions, each representing a collection of related events with boundaries set by user-defined criteria.


In instances where a single row satisfies both the ``startSessionPattern`` and ``endSessionPattern``, that row is recognized as the beginning of a new session while simultaneously concluding the previous session. 
Consequently, this new session is retained regardless of the value of isIgnoreIfNoEnd.

Additional options allow you to specify whether the generated session ID should be hashed and whether to exclude sessions lacking rows that match the start or end criteria. This provides flexibility in handling incomplete sessions and customizing session identifiers.


The function signature is as follows:

``` 
def igrafxSessions(
    inputLines: util.List[String],
    ignorePattern: String,
    groupSessionPattern: String,
    startSessionPattern: String,
    endSessionPattern: String,
    sessionIdPattern: String,
    isSessionIdHash: Boolean,
    isIgnoreIfNoStart: Boolean,
    isIgnoreIfNoEnd: Boolean
    ): util.List[Struct]
```

The output format is structured as:
``` 
STRUCT<SESSION_ID VARCHAR(STRING), LINE VARCHAR(STRING)>
```

- **SESSION_ID**: The unique ID for each session.
- **LINE**: A line from the initial input collection that belongs to the session.

### iGrafx Transposition UDF

* name in ksqlDB : **igrafx_case_events**
* package : **com.igrafx.ksql.functions.caseevents.domain**

This **User-Defined Table Function (UDTF)** transforms a single row containing multiple dates, each associated with specific activities, into multiple rows—each with a single date (or period) linked to its corresponding activity.

This function helps to break down complex, aggregated data into a more manageable, row-based format, making it easier to analyze and process activity timelines. 

#### Overview

The **Transposition User-Defined Function (UDF)** is a tabular function that enables transposing data within ksqlDB. This function is versatile, providing two different variations to suit a range of data transformation needs.

Regarding the behavior of the UDF, for both variations, it’s important to be mindful of any additional columns in the initial row.

To get more details about this UDF directly within ksqlDB, you can use the command:

```sql
DESCRIBE FUNCTION IGRAFX_TRANSPOSITION;
```

To ensure any modifications to a STREAM are applied to all previously inserted data, set the offset configuration to the earliest position with this command:

``` 
SET 'auto.offset.reset'='earliest';
```


#### Variation 1

**UDF Signature :**
```
igrafxTransposition(input: util.List[Struct]): util.List[Struct]
```

Both the input and output structures are formatted as follows:

``` 
"STRUCT<TASK VARCHAR(STRING), TIME VARCHAR(STRING)>"
```

This variation is designed to *explode* a row’s columns, transforming each into multiple rows where each row contains the **Task** and its associated **Timestamp**.

#### Variation 2

**UDF Signature :**
```
igrafxTransposition(input: util.List[Struct], dateFormat: String, isStartInformation: Boolean, isTaskNameAscending: Boolean): util.List[Struct]
```

The input structure is formatted as follows:
``` 
"STRUCT<TASK VARCHAR(STRING), TIME VARCHAR(STRING)>"
```

The output structure is formatted as follows:

``` 
"STRUCT<TASK VARCHAR(STRING), START VARCHAR(STRING), STOP VARCHAR(STRING)>"
``` 

This function is designed to **explode** a row with multiple columns into multiple rows, each containing four columns: the *case*, the **activity**, the **starting date**, and the **ending date**.

The UDF requires the following parameters:
* **input** : corresponds as for the first variation to the input row we want to **explode**
* **dateFormat** : corresponds to the date format (for instance : for an activity having for date 12/01/2020, the date format is "dd/MM/yyyy" )
* **isStartInformation** : **true** indicates that the date associated to the activity corresponds to the beginning of the activity, and that we hence need to calculate the end of the activity. **false** indicates that the date corresponds to the end of the activity meaning we have to calculate its start date (calculations are made when possible in function of the dates of the other activities)
* **isTaskNameAscending** : **true** indicates that in case of identical dates for two (or more) rows, the order of the rows is determined in an ascending manner according to the activity's name, while **false** means that the order is determined in a descending manner according to the activity's name

#### Compilation and Deployment on LiveConnect

To compile the connector and generate the **.jar** file needed for Kafka Connect, navigate to the root of the project and run:
```
sbt assembly
```
Place the newly created `.jar` file (located in the `target/scala-2.13` directory) into the `docker-compose/extensions/` directory of the iGrafx Liveconnect project. If this directory does not exist, create it. Ensure the following lines are included in the `ksqldb-server` configuration in `docker-compose.yml`:

``` 
ksqldb-server:
    ...
    volumes:
        - "./extensions/:/opt/ksqldb-udfs"
    environment:
      ...
      KSQL_KSQL_EXTENSION_DIR: "/opt/ksqldb-udfs"
```

Once LiveConnect is launched, the connector will be available for use.

We can then connect to the ksqlDB CLI, from the ``docker-compose/`` repository of Liveconnect, with the command :

``` 
docker-compose exec ksqldb-cli ksql http://ksqldb-server:8088
```

Once in the ksqlDB CLI, the different UDFs at your disposal can be listed with the function :

``` 
SHOW FUNCTIONS;
```

### iGrafx Liveconnect:

This module provides a Kafka infrastructure setup located in the `docker-compose/` subdirectory. It includes essential components for managing and interacting with Kafka and optional tools for extended functionality:

- **Broker and Zookeeper**: Core components for managing Kafka topics and messages.
- **Schema Registry**: Service for registering schemas on Kafka topics.
- **Kafka Connect (Connect)**: Supports Kafka Connect connectors for data integration.
- **ksqlDB and CLI**: Enables stream processing and querying of Kafka topics.
- **Kafka UI (or Confluent Control Center)**: A graphical interface for monitoring and managing Kafka cluster functionalities.
- **SFTP Server** *(optional)*
- **PostgreSQL Database** *(optional)*: For auxiliary processing.

#### Requirements

To use the LiveConnect module, you must have Docker and Docker Compose installed on your system. Follow these links for installation instructions:

- [Install Docker](https://docs.docker.com/get-docker/)
- [Install Docker Compose](https://docs.docker.com/compose/install/)

#### Launching Liveconnect

The containers within this infrastructure communicate through the internal Docker network, `kafka-network`.

**To launch **LiveConnect** (Dockerized Kafka infrastructure):**
```
cd docker-compose/
make liveconnect
```

**To stop the LiveConnect infrastructure:**
```
cd docker-compose/
make liveconnect-down
```
# Recommended Connectors

Below are the installation commands for the recommended connectors:

- **[File System Source Connector](https://www.confluent.io/hub/jcustenborder/kafka-connect-spooldir)**: For loading files in formats such as CSV, JSON, etc.
    ```bash
    docker-compose exec connect confluent-hub install --component-dir /connect-plugins/ --verbose jcustenborder/kafka-connect-spooldir:2.0.65
    ```

- **[JDBC Connector (Source and Sink)](https://www.confluent.io/hub/confluentinc/kafka-connect-jdbc)**: For connecting to JDBC-compatible databases.
    ```bash
    docker-compose exec connect confluent-hub install --component-dir /connect-plugins/ --verbose confluentinc/kafka-connect-jdbc:10.8.0
    ```

- **[iGrafx Sink Connector](#igrafx-aggregation-main-aggregation-and-igrafx-sink-connector)**: For sending data from Kafka topics to an iGrafx project.

  You have two options to install the iGrafx Sink connector:
    1. **Build the Connector Jar**: Follow the instructions in the [iGrafx Connectors section](#compilation-and-deployment-on-liveconnect) to build the connector JAR.
    2. **Retrieve the Connector Jar from the pipeline**
  
> Note that you may also download the iGrafx UDFs by following [similar commands](#igrafx-udfs)

#### Installing New Connectors

To add a Kafka connector, place it in the `docker-compose/connect-plugins/` directory, as referenced by the `CONNECT_PLUGIN_PATH` variable in the `docker-compose.yml`.

You can easily find and install a new connector using the [Confluent Hub Client](https://docs.confluent.io/home/connect/confluent-hub/client.html).

If needed you may also update the version of the connector.

For instance, if you want to update the`kafka-connect-servicenow` connector to Version 2.5.4, 
first check the connector reference on [Confluent Hub](https://confluent.io/hub), then run: 

````
docker-compose exec connect confluent-hub  install --component-dir /connect-plugins/ --verbose confluentinc/kafka-connect-servicenow:2.5.4

````
>Note: The ``--component-dir /connect-plugins/`` option specifies the install path. This addition differs from the Confluent site's default command.
>
>Furthermore, `confluent-hub` aims to simplify the retrieval and installation of a version of a module referenced under https://confluent.io/hub.


In practice, this process involves *manually* copying a directory with the required JAR files and configurations into the designated connectors directory (`connect-plugins` in our setup). 
After adding a new connector, restart the `liveconnect` Docker container with the ``make liveconnect`` command (which uses the `confluentinc/cp-kafka-connect` image) to load the changes.

There are numerous Kafka connectors, including many from the [Camel Kafka ecosystem](https://camel.apache.org/camel-kafka-connector/latest/), 
which do better with manual installation. 
However, note that you may also try looking for them in the [Maven repository](https://mvnrepository.com/) and directly download a **jar** or a **targz** as per your preference.

Nevertheless, if you wish to download them manually, follow the procedure below:

**Manual Installation Procedure:**

1. Download the connector package. For example:
```
wget https://repo.maven.apache.org/maven2/org/apache/camel/kafkaconnector/camel-smtp-kafka-connector/0.10.1/camel-smtp-kafka-connector-0.10.1-package.tar.gz
````
2. Decompress the files.
````
tar -xvzf camel-smtp-kafka-connector-0.10.1-package.tar.gz
````
3. Move the connector folder into `connect-plugins`.
````
mv camel-smtp-kafka-connector connect-plugins/camel-smtp-kafka-connector-0.10.1-package/

````
4. Restart the `liveconnect` container to activate the connector.
````
make liveconnect-down
make liveconnect
````

#### Kafka-UI Configuration

Kafka-UI is a user-friendly graphical interface for managing and interacting with a Kafka/KSQLDB cluster. It allows you to easily view Kafka topic messages, manage created connectors, run ksqlDB queries, and monitor various aspects of your Kafka cluster's performance.

#### Kafka-UI Credentials

The username and password required to log in to Kafka-UI are specified in the `docker-compose.yml` file, within the `kafka-ui` section. These credentials are set via the `JAVA_OPTS` variable. Specifically, the `-Dspring.security.user.name` option defines the username, while the `-Dspring.security.user.password` option sets the password.

#### ksqlDB CLI Console
The ksqlDB CLI provides command-line access for managing KSQL commands, viewing connectors, topics, streams, tables, and more.

To access the ksqlDB CLI, use the following command:
```bash
docker-compose exec ksqldb-cli ksql http://ksqldb-server:8088
```

Once inside the ksqlDB CLI prompt, you can set environment-specific variables as needed. For example, to configure the offset setting, use:
``` 
SET 'auto.offset.reset' = 'earliest';
```
This command sets the offset to the earliest, ensuring that the CLI reads from the beginning of each topic.

To quit the ksqlDB CLI, type `exit` and press enter.

For further information on ksqlDB CLI configuration, please refer to the documentation at [click here](https://docs.ksqldb.io/en/latest/operate-and-deploy/installation/cli-config/)

For further documentation on ksqlDB, please refer to the documentation at [click here](https://ksqldb.io/)

#### Local Execution
you can access Kafka UI locally at: [http://localhost:9021](http://localhost:9021)

#### Configuration for a Specific Kafka Topic

The `igrafx-liveconnect` template can be deployed on a separate VM from the main application.

However, to ensure proper communication, the Kafka registry and broker associated with the topic must be accessible to the `api` container of the target application. This requires opening the registry and Kafka broker ports on the VM host where they are installed and confirming that the host is reachable from the `api` service.

To set up a workgroup with LiveConnect:

- **Set the Workgroup ID:** Define the workgroup ID in the `.env` file under `WORKGROUP_ID`.
- **Configure Kafka Connection in Database:** In the `WORKGROUPS` table in PostgreSQL, update the `KAFKA_BROKER` and `KAFKA_REGISTRY` columns with the appropriate URLs. For example:
  - `KAFKA_BROKER`: `http://kafka-broker:29092`
  - `KAFKA_REGISTRY`: `http://schema-registry:8081`

#### Example Configuration for Cross-VM Communication

If the VMs are on the same private network and ports have been opened on the LiveConnect VM, you can configure the `WORKGROUPS` table as follows:

- **Kafka Broker URL:** Set `KAFKA_BROKER` to `http://192.168.1.128:19092`
- **Kafka Registry URL:** Set `KAFKA_REGISTRY` to `http://192.168.1.128:8081`

Once configured, the workgroup administrator can activate the Kafka topic, allowing the topic to receive updates on all cases in a project during project updates.


#### Data-Transform Database

A PostgreSQL database is available to perform data transformations that are not yet supported in ksqlDB. This setup allows you to add additional columns or perform advanced processing on data before it's ingested back into Kafka.

The following example demonstrates how to use an intermediate PostgreSQL database to generate an additional column `cnt`, which numbers events within each case (identified here by the `INCIDENT` column).

#### Step 1: Create a Table with Auto-Increment Index `id`

The auto-increment `id` index allows the ksqlDB connector to continuously retrieve the latest data. If available, other fields (such as timestamp or unique event identifier) can also serve this purpose.

> **Note:** This table can be set up to create automatically on initial launch (see `conf/pg-initdb.d/`).

Example command to create the table in your local PostgreSQL instance (accessible on the default port, with connection details in the `.env` file):

```sql
CREATE TABLE public."JDBC_TABLE" (
  id SERIAL PRIMARY KEY NOT NULL
);
```
#### Step 2: Feed the Table from a Kafka Topic Using a JDBC Sink Connector
The following JDBC sink connector populates the PostgreSQL ``JDBC_TABLE`` table from the ``JDBC_TABLE`` Kafka topic:

``` 
CREATE SINK CONNECTOR JDBC_SINK_01 WITH (
  'connector.class'          = 'io.confluent.connect.jdbc.JdbcSinkConnector',
  'key.converter'             = 'org.apache.kafka.connect.storage.StringConverter',
  'topics'                         = 'JDBC_TABLE',
  'table.name.format'     = 'JDBC_TABLE',
  'connection.url'           = 'jdbc:postgresql://data-transform:5432/transform?verifyServerCertificate=false',
  'connection.user'          = 'datamanager',
  'connection.password'      = '1r8P!eXx',
  'auto.evolve'              = 'true'
);
```

#### Step 3: Read Data from the Table Using a JDBC Source Connector
The following JDBC source connector reads data from the PostgreSQL table and includes a generated ``cnt`` column that assigns sequential numbers to events within each ``INCIDENT`` case. 
The ``query`` parameter in the connector specifies this transformation:
``` 
CREATE SOURCE CONNECTOR JDBCSOURCEConnector1 WITH (
    'connector.class' = 'io.confluent.connect.jdbc.JdbcSourceConnector',
    'tasks.max' = '1',
  'connection.url'           = 'jdbc:postgresql://data-transform:5432/transform?verifyServerCertificate=false',
  'connection.user'          = 'datamanager',
  'connection.password'      = '1r8P!eXx',
    'mode' = 'incrementing',
    'incrementing.column.name' = 'id',
    'numeric.mapping' = 'best_fit',
    'topic.prefix' = 'jdbc_cnt_case_lines',
    'query' = 'SELECT * , ROW_NUMBER() OVER(PARTITION BY INCIDENT ORDER BY id ASC) as cnt from JDBC_TABLE'
);
```

#### SFTP Server Configuration

An SFTP server may be necessary to allow users to regularly upload CSV files for data updates. 
A pre-configured SFTP server is included in the `docker-compose` setup, using the [corilus/sftp container](https://hub.docker.com/r/corilus/sftp).

Furthermore, the `docker-compose.yml` file provides options to configure:
- **SFTP User and Password:** Define the username and password for accessing the SFTP server.
- **File Directory and UID:** Specify the local directory where uploaded files will be stored, along with the user ID (UID) for permissions.
- **Local Port:** Set the port on which the SFTP server will be accessible.

Ensure that any directories or files uploaded via the SFTP server match the directory mounts used by the `connect` service in `docker-compose.yml`. This alignment is particularly important if changes or additional users are added, to maintain seamless file access between the `connect` and `sftp` containers.

#### Connecting to the SFTP Server

To connect to the SFTP server using the default user `foo`, you can use an SFTP client such as **FileZilla** or **WinSCP**. Connect to `localhost` on the port specified in the `docker-compose` file (default is `2222`).

- **Username:** `foo`
- **Password:** Use the password defined in `docker-compose.yml`
- **Directory:** Upload files to the directory specified in `docker-compose.yml`

You can connect via command line:

```bash
sftp -P 2222 foo@<host-ip>
```