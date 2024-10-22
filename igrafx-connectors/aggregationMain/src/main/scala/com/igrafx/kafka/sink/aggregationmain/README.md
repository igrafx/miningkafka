# Aggregation and iGrafx Sink Connector AVRO

## Overview

This connector uses the method of the aggregation connector to aggregate together multiple events, but it also sends the result of the aggregation to the iGrafx Mining API. With this connector, the usual processing before sending data to The iGrafx Mining API is to aggregate together multiple records (corresponding to process events), as we would do with the aggregation connector, and then put the aggregation in a CSV file which is sent to the iGrafx Mining API.

Here the Aggregation iGrafx Sink Connector will retrieve events coming from Kafka, will aggregate them, and will send the aggregation result in a file to the iGrafx Mining API when a threshold is reached for a partition. The connector does it alone, is specific to sending events to iGrafx, and isn't subject to the limitation of the messages size encountered by the aggregation connector (as the destination doesn't correspond to a Kafka topic)

Moreover, the connector also benefits from the ability to create the Column Mapping of a iGrafx project from the connector, and send logging events to a Kafka Topic. 

The connector aggregates data according to four thresholds :

* Element number : when the number of aggregated elements reach a certain value the aggregation result is sent to the iGrafx Mining API
* Value pattern : A Regex pattern triggering the flush of the current aggregation to the iGrafx Mining API if the value of the incoming sink record matches it
* Timeout : After a certain time since the last sending of an aggregation result, the current aggregated data are sent (even if the number of elements threshold is not reached)
* Retention : This threshold is not defined by the user in the connector's properties. It is linked to the value of the **retention.ms** configuration in the Kafka topic from which a data comes. See the section below related to the retention for more information

### Connector properties

To instantiate the connector, we just have to fill the following properties :

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

Warning : There is a need to escape the backslash character

#### Mandatory properties :

Here, the values are given as an example, even if those for the following properties should not change :

* **connector.class** (String)
* **key.converter** (String)
* **value.converter** (String)

Other properties can be modified :

* **tasks.max** (Int) : Number of Tasks we want to create for the connector
* **api.url** (String) : The **url** corresponding to the iGrafx Mining API and allowing to send a file to iGrafx
* **api.authUrl** (String) : The **url** to use for authentication in order to retrieve a connexion token, the value of this property needs to finish with **/realms/realm_name**, where realm_name corresponds to the name of the keycloak realm to use.
* **workGroupId** (String) : The **ID** of the workgroup related to the iGrafx project
* **workGroupKey** (String) : The **Key** of the workgroup related to the iGrafx project
* **projectId** (String) : The **ID** of the iGrafx project (*needs to follow the UUID format*)
* **csv.encoding** (String) : The encoding to use for the file (*UTF-8/ASCII/ISO-8859-1*)
* **csv.separator** (String) : The separator character to use between two fields in the csv file (*only 1 character*)
* **csv.quote** (String) : The quote character (*only 1 character*)
* **csv.fieldsNumber** (Int) : The number of fields in a line (*needs to be >= 3*)
* **csv.header** (Boolean) : Tells whether the CSV file has a header or not (*true/false*)
* **csv.defaultTextValue** (String) : Value to write in the file for any column with a missing value
* **retentionTimeInDay** (Int) : Lifetime of an archived file in **days** (*need to be > 0*)
* **threshold.elementNumber** (Int) : The maximum number of elements in one aggregation (when the number is reached, the current aggregation is sent to the iGrafx Mining API)
* **threshold.valuePattern** (String) : **Facultative property** sending to the iGrafx Mining API the current aggregation if the value of an incoming sink record matches the **regex** defined in **threshold.valuePattern** (the aggregation is sent even if the number of elements corresponding to **threshold.elementNumber** is not reached). This threshold is not relevant if the property is not defined while creating the connector. The pattern is on the entire string value of a SinkRecord (if the value has the JSON format for example, the pattern will have to take into account the '{' '}' of the JSON, it needs to be adapted to the structure of the data)
* **threshold.timeoutInSeconds** (Int) : If the time since the last sending of an aggregation result exceeds the value in seconds of **threshold.timeoutInSeconds**, the current aggregation (which was in construction) is sent to the iGrafx Mining API (even if the number of elements corresponding to **threshold.elementNumber** is not reached)
* **bootstrap.servers** (String) : The List of Kafka brokers
* **value.converter.schema.registry.url** (String) : The URL to the Confluent Schema Registry

For more information about regex (used with the **threshold.valuePattern** property) : https://medium.com/factory-mind/regex-tutorial-a-simple-cheatsheet-by-examples-649dc1c3f285

#### Facultative properties :

The following properties only need to be filled if you want the connector to create a Column Mapping for the iGrafx Project :

* **columnMapping.create** (Boolean) : Indicates whether the Column Mapping of the project is created by the connector ot not (*true/false*), if **true** then all the following properties need to be filled, if **false** it is not necessary to write the following properties
* **columnMapping.caseIdColumnIndex** (Int) : The index of the CaseId column (*needs to be >= 0*)
* **columnMapping.activityColumnIndex** (Int) : The index of the Activity Column (*needs to be >= 0*)
* **columnMapping.timeInformationList** (String) : Information about the Time columns under the format {columnIndex;dateFormat} separated by a ',' if there are two columns. The information for at least one column and for a maximum of two columns must be provided. The columnIndex needs to be an *Int >= 0* and dateFormat needs to be a String of *minimum length 1*
* **columnMapping.dimensionsInformationList** (String) : Information about the Dimension columns. The information needs to be given by the user with a JSON format, as presented in the examples. The **columnIndex** needs to be an *Int >= 0* and **columnName** needs to be a String of *minimum length 1*. **IsCaseScope** is a mandatory boolean used to determine if, for an event, the value of the column is calculated according to an aggregation on the entire case. For the Dimension, the following aggregation types are valid : "FIRST", "LAST", "DISTINCT", the type of the chosen aggregation is defined with the **aggregation** parameter. Please note that if **isCaseScope** is true you need to give a valid aggregation type, and if it is false you have the choice between writing or not the **aggregation** parameter. When the property **columnMapping.groupedTasksColumns** is defined, each dimension needs to set the **groupedTasksAggregation** parameter (this parameter should not be set if the **columnMapping.groupedTasksColumns** property is not defined), the following grouped tasks aggregation types are valid : "FIRST", "LAST". For more information about the grouped tasks, click [here](#grouped-tasks-columns).
* **columnMapping.metricsInformationList** (String) : Information about the Metric columns. The information needs to be given by the user with a JSON format, as presented in the examples. The **columnIndex** needs to be an *Int >= 0*, **columnName** needs to be a String of *minimum length 1*. **IsCaseScope** is a mandatory boolean used to determine if, for an event, the value of the column is calculated according to an aggregation on the entire case. For the Metric, the following aggregation types are valid : "FIRST", "LAST", "MIN", "MAX", "SUM", "AVG", "MEDIAN", the type of the chosen aggregation is defined with the **aggregation** parameter. Please note that if **isCaseScope** is true you need to give a valid aggregation type, and if it is false you have the choice between writing or not the **aggregation** parameter. When the property **columnMapping.groupedTasksColumns** is defined, each metric needs to set the **groupedTasksAggregation** parameter (this parameter should not be set if the **columnMapping.groupedTasksColumns** property is not defined), the following grouped tasks aggregation types are valid : "FIRST", "LAST", "MIN", "MAX", "SUM", "AVG", "MEDIAN". For more information about the grouped tasks, click [here](#grouped-tasks-columns). Finally, the **unit** parameter needs to be a String and is *facultative* (It is possible to only fill the **columnIndex**, the **columnName**, and the **isCaseScope**).
* **columnMapping.groupedTasksColumns** (String) : Information about the columns to use for grouping events. The information needs to be given by the user as a List in a JSON format, as presented in the examples. If this property is not defined, similar events are not grouped, if it is defined, it should at least contain the index of one time/dimension/metric column. When this property is defined, all the dimensions (*columnMapping.dimensionsInformationList* property) and metrics (*columnMapping.metricsInformationList*) should define a groupedTasksAggregation. For more information regarding the grouped tasks, click [here](#grouped-tasks-columns). 
* **csv.endOfLine** (String) : The ending character of a line (*length >= 1*)
* **csv.escape** (String) : The escape character (*only 1 character*)
* **csv.comment** (String) : The comment character (*only 1 character*)

Moreover, characters '{' '}' ';' and ',' used for the format of the Time columns can be changed in the **com/igrafx/kafka/sink/main/Constants** file

Note that in the case where **csv.header** is true, and if the connector is supposed to create a Column Mapping in the iGrafx project, then the headers created for the generated files will use the information of the Column Mapping for their columns names.
Otherwise, if **csv.header** is true, but the connector isn't supposed to create a Column Mapping, the headers will simply correspond to a succession of **csv.fieldsNumber - 1** separators characters, defined by **csv.separator**

Finally, the following properties have to be defined only if you need the connector to register its file related events in a Kafka topic (see the below section about Logging Events in Kafka) :

* **kafkaLoggingEvents.isLogging** (Boolean) : Indicates whether the connector registers its file related events in a Kafka topic or not (*true/false*), if **true** events will be logged in a Kafka topic, if **false** (*default value*) they won't
* **kafkaLoggingEvents.topic** (String) : The name of the Kafka topic registering the events (*length >= 1*)

<hr/>

### AVRO Format

This connector awaits data coming with an AVRO format and other formats may cause errors.

Each record coming from Kafka needs to have the following type (verified with a schema that is compared to the AVRO record)

```
ARRAY<STRUCT<columnID INT, text VARCHAR, quote BOOLEAN>>
```

The Array corresponds to one event (and one event corresponds to one line once in the CSV file), with each STRUCT in the Array corresponding to a column of the event (a field once in the CSV file, such as the caseId or the activity for instance)

So one record coming from Kafka corresponds to one event and the connector aggregates together multiple events. When a threshold is reached, the aggregated events are written in the same file, which is then sent to the iGrafx API

To write correctly a field in the CSV file we need :

* Its column number (**columnId**),
* Its value (**text**)
* To know if the field is a quote or not (**quote**)

For instance, the following data coming from a Kafka topic (represented here with a JSON format, but is in reality under the AVRO format) :

```
"{"DATAARRAY":
    [{"QUOTE":true, "TEXT":"activity1", "COLUMNID":1},
     {"QUOTE":false, "TEXT":"caseId1", "COLUMNID":0},
     {"QUOTE":false, "TEXT":"endDate1", "COLUMNID":3}]
}"
```

will be written as the following line in the CSV file :

```
caseId1,"activity1",null,endDate1
```

If in the connector's properties :

* csv.separator = ,
* csv.quote = "
* csv.defaultTextValue = null
* csv.fieldsNumber = 4

Warning : It is important to note that the names **DATAARRAY**, **QUOTE**, **TEXT** and **COLUMNID** need to be respected in ksqlDB in order to correctly read the AVRO data coming from a Kafka topic

Any null value for either one event, one column of an event, or for one parameter in one column of an event, is considered as an error and will stop the Task

<hr/>

### API iGrafx

Link to the iGrafx public API swagger : http://public-api.logpickr.com/

The url path used to send a file to the iGrafx Mining API is : */project/{id}/file*

The sending of the file to the API is managed in the **adapters/api/MainApiImpl.scala** file, by the **sendCsvToIGrafx** method taking as parameters the connector's properties, and the path of the file to send

To send the file there are two steps to follow :
The first one consists in retrieving the connexion token. The url path used is **{authUrl}/protocol/openid-connect/token** where *{authUrl}* corresponds to the **api.authUrl** connector's property. The request also conveys information about the workgroup ID and the workgroup Key. The Http Response then contains a JSON with a "**access_token**" key in case of success
The second step is basically the sending of the file via the **{apiUrl}/project/{projectId}/file?teamId={workGroupId}** url, where *{apiUrl}* corresponds to the **api.url** connector's property. For this call, information about the workgroup ID, the project ID, the path of the file to send and finally the token previously retrieved are necessary

<hr/>

### Architecture

The project tries to follow the "clean architecture"

#### Classes and Methods

##### Class IGrafxAggregationSinkConnector

Located in the **com.igrafx.kafka.sink.aggregationmain.domain** package

This is the class representing the connector. The **start** method of this class corresponds in a way to the entrypoint of the program. This class is used to validate the connector properties defined by the user, add more properties and launch/configure each Task.

##### Class IGrafxAggregationSinkTask

Located in the **com.igrafx.kafka.sink.aggregationmain.domain** package

Corresponds to a Task. This class is used to retrieve the records coming from the kafka topics defined via the **topics** connector property

The number **tasks.max** defined by the user indicates the number of IGrafxAggregationSinkTask that are created across the cluster

Each Task is associated with its own collection of partitions (partitions from the topics defined in the **topics** property are distributed among Tasks). A Task will retrieve the data, related to its partitions, coming from Kafka, and will aggregate this data for each partition and then write the result of an aggregation (when a threshold is reached) in a file before sending this file to the iGrafx Mining API.

How does it work ? The **put** method is regularly called with a collection of SinkRecord. Each SinkRecord corresponds to a message in a Kafka topic. Hence, a SinkRecord is related to a topic, a partition and an offset.
Here, the data represented by a SinkRecord of the collection corresponds to an event. This event is aggregated with other events coming from the same partition and, when a threshold is reached, the aggregated events are written in a CSV file with a good format according to the column mapping of the project. Once the file is completed, it is sent to the iGrafx Mining API to feed the iGrafx project defined by the **projectId** connector property.
For a single Task there is only one call to the **put** method at a time. When a **put** is started, it does all its work even if the connector is passed to the *PAUSED* state in between (in this case the Task stays with a *RUNNING* state until the end of the **put** method, and then switches to the *PAUSED* state)

##### Class MainConfig

Located in the **com.igrafx.kafka.sink.aggregationmain.config** package and containing the environment variables used

##### Object ConnectorPropertiesEnum

Located in the **com.igrafx.kafka.sink.aggregationmain.domain.enums** package

Object where the different properties of the connector are defined. If you want to add a new property to the connector, this is the place to define its name, default value, group...

##### Class MainApiImpl

Located in the **com.igrafx.kafka.sink.aggregationmain.adapters.api** package

Class dealing with the different Http request used to communicate with the iGrafx Mining API

The **sendCsvToIGrafx** is called after the creation of a new CSV file. This is this method that retrieves a connexion token and then send the file to the API
Indeed, **sendCsvToIGrafx** calls the following two methods : **sendAuthentication**, used to retrieve a connexion token and **sendFile** used to send a file to the iGrafx Mining API

About the Column Mapping, it's the **createColumnMapping** method which sends the Column Mapping defined by the user to the iGrafx Mining API (when **columnMapping.create** is true). The steps are to retrieve the connexion token (first http request), check that the project doesn't already have a Column Mapping (second http request) and then send the Column Mapping (third http request). The information on the Column Mapping are determined from the related connector properties and are sent with the following JSON format (Here the example is presented according to the connector properties defined earlier) :

```
{
  "columnMappingWithFileStructure": {
    "fileStructure": {
      "charset": "UTF-8",
      "delimiter": ",",
      "quoteChar": """,
      "escapeChar": "\",
      "eolChar": "\n",
      "header": true,
      "commentChar": "#",
      "fileType": "csv"
    },
    "columnMapping": {
      "caseIdMapping": { "columnIndex": 0 },
      "activityMapping": { "columnIndex": 1, "groupedTasksColumns": [1, 2, 3] },
      "timeMappings": [
        { "columnIndex": 2, "format": "dd/MM/yy HH:mm" }, 
        { "columnIndex": 3, "format": "dd/MM/yy HH:mm" }
      ],
      "dimensionsMappings": [
        { "columnIndex": 4, "name": "Country", "isCaseScope": true, aggregation: "FIRST", "groupedTasksAggregation": "FIRST" },
        { "columnIndex": 5, "name": "Region", "isCaseScope": false, "groupedTasksAggregation": "FIRST" },
        { "columnIndex": 6, "name": "City", "isCaseScope": false, "groupedTasksAggregation": "LAST" }
      ],
      "metricsMappings": [
        { "columnIndex": 7, "name": "Price", "unit": "Euros", "isCaseScope": true, aggregation: "MIN", "groupedTasksAggregation": "AVG" },
        { "columnIndex": 8, "name": "DepartmentNumber", "isCaseScope": false, "groupedTasksAggregation": "FIRST" }
      ]
    }
  }
}
```

#### Class MainSystemImpl

Located in the **com.igrafx.kafka.sink.aggregationmain.adapters.system** package

Contains three important methods :

* **initProjectPaths** : Allows to create the repositories (if they don't already exist) that are in the path of the file created to store the new received data from kafka topics. It also creates the repository used to store the file that couldn't be sent to the iGrafx Mining API
* **deleteOldArchivedFiles** : Allows to compare the creation date of all the archived files with the current date. If for one of those files, the time interval between the two dates is superior (in days) than the value of the **retentionTimeInDay** property, then the archived file is deleted
* **appendToCsv** : called when a threshold is reached, this function creates a new file and write the aggregated events in the created CSV file with a good format.

The name of the CSV file is generated like this :

```
"{MainConfig.csvPath}/{projectId}/{connectorName}/archive/data_{uuid}_{timestamp}.csv"
```

Here :

* **MainConfig.csvPath** : corresponds to the path to use in order to store all the files that will be sent to the iGrafx Mining API. The value is defined in the **csvPath** variable of the com.igrafx.kafka.sink.main.config.MainConfig file
* **projectId** : corresponds to the ID of the iGrafx project the connector will send the files to. The value is defined in the connector's properties
* **connectorName** : corresponds to the name of the connector. The value is defined when creating the connector
* **uuid** : corresponds to a random UUID. This is used to avoid two files with the same name in the case where their names are generated from the same connector and at the same timestamp
* **timestamp** : corresponds to the current timestamp in seconds of the moment where the file name is generated

Please note that all the archived files are in the **{MainConfig.csvPath}/{projectId}/{connectorName}/archive/** repository, and all the files that couldn't be sent to the API are in the  **{MainConfig.csvPath}/{projectId}/{connectorName}/archiveFailed/** repository, as long as they are not deleted (because of the **retentionTimeInDay** for instance)

<hr/>

### Class TaskAggregationUseCases

Located in the **com.igrafx.kafka.sink.aggregationmain.domain.usecases** package

This class contains all the logic behind the **aggregation** part of the connector, from the transformation of the SinkRecord coming from Kafka into business objects, to the addition of a new data to the current aggregation of the concerned partition, to the verification of thresholds.

### Class TaskFileUseCases

Located in the **com.igrafx.kafka.sink.aggregationmain.domain.usecases** package

This class contains all the logic behind the initialization of the project path, the deletion of the old archived files and the creation and sending of a CSV file containing events.

### Kafka Logging Events

The connector has the possibility via its **kafkaLoggingEvents.sendInformation**, **kafkaLoggingEvents.topic** properties to log file related events in a Kafka topic.

The AVRO schema expected for the Logging is the following :

```
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

* an **eventType** (String) : currently there are **pushFile** and **issuePushFile**
* a **igrafxProject** (String : UUID) : corresponds to the iGrafx Project ID to which we want to send  (the **projectId** connector's property)
* an **eventDate** (Long) : corresponds to the date of the event
* an **eventSequenceId** (String) : corresponds to the ID of the sequence of events related to a file
* a **payload** (String : JSON) : can contain any information related to a certain event type

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

For this event, the information embedded in the **payload** are : the **file name (filename: String)**, the **event date (date: Long)**, and the **number of lines in the file (lineNumber: Int)**. Example of payload for this event :

``` 
{
    "filename": "filename_example",
    "date": 3446454564,
    "lineNumber": 100
}
```

* **issuePushFile** : event generated when there was an issue during the creation/sending of a file

The information embedded in its **payload** are : the **file name (filename: String)**, the **event date (date: Long)**, and the **exception type (exceptionType: String)** corresponding to the name of the thrown exception. Example of payload for this event :

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
|-------------------|:-------------:|:------------------------------:|:------------------------------------------------------:|:--------------------------------------------------------------------------------------------------------------------:|:---------------------------:|
| **pushFile**      |   pushFile    | The ID of the iGrafx project | The date for which the file has been successfully sent |              MD5 hash of a String containing the source topic/partition/offset of the data in the file               |  filename/date/lineNumber   |
| **issuePushFile** | issuePushFile | The ID of the iGrafx project |                 The date of the issue                  | MD5 hash of a String containing the source topic/partition/offset of the data that should have been sent in the file | filename/date/exceptionType |

When the sending of the event to Kafka fails, there are two possibilities :

* if the event was an issuePushFile event, the exception stopping the Task is the one that occurred during the creation/sending of the file, prior to the event sending issue (but the event's exception is still logged)
* if the event was a pushFile event, the exception stopping the Task is the event's exception

<hr/>

### Offsets management

As for the aggregation connector, the offset management of the connector is a bit special.

Offsets are managed in the code by the **PartitionTracker** case class. Each partition in a topic present in the **topics** property has a corresponding **PartitionTracker**, and **the aggregation is only performed on data coming from the same partition**.

3 types of offsets are used and maintained by the **PartitionTracker** :

* **Processed Offset** : Corresponds in the PartitionTracker to the offset of the last Record received for the related partition (handled by the Put function in the IGrafxAggregationSinkTask)
* **Flushed Offset** : Corresponds in the PartitionTracker to the offset of the last Record that has been sent with its aggregation to the iGrafx Mining API
* **Commit Offset** : Corresponds in the PartitionTracker to the offset of the last Record that has been flushed and whose offset has been committed to Kafka (once the offset of a Record is committed, the Record will not be red again, even in case of a Task error/rebalance...)

This means that when a new SinkRecord arrives to the Put function in IGrafxAggregationSinkTask, its offset is processed. When a threshold (element number/value pattern/timeout/retention) is reached, the aggregation with the record is sent in a CSV file to the iGrafx Mining API and therefore the record's offset is considered flushed. Finally, once the preCommit method is called in IGrafxAggregationSinkTask, all the flushed offsets in each partition are committed (if they weren't already).

As a Record is considered done only when its offset is committed, it means that any record having its offset only considered flushed or processed (not yet committed) will be received once again by the connector in case of an issue with the Task which was handling the record. As such, a record already flushed (sent to iGrafx with its aggregation) but not yet committed could, in case of a Task failure or rebalance, be received again by the connector and sent again to the iGrafx Mining API. Nevertheless, any data that has its offset committed won't be received and sent again.

This is made to ensure the **at least once** delivery guarantee.

<hr/>

### Retention

Values in a Kafka topic are stored for as much time as defined by the **retention.ms** configuration of the topic. Hence, to avoid loosing data in case of a connector crash, the connector needs to send the aggregation before one of the value of the aggregation reaches its retention time in the input topic.

Indeed, even if the connector stores itself the data it receives from Kafka during the aggregation process, in the case where the data exceeds its retention time in the topic and if the connector crashes before sending the aggregation to the iGrafx Mining API, then the data is lost and won't be recovered at the connector restart, as it will not be stored anymore in the source Kafka topic.

Consequently, in order to avoid this issue, the connector sends the aggregation of a partition if any of its data reaches 80% of its retention time. Nevertheless, if the connector crashes before one of its data reaches 80% of its retention time, and if the connector isn't restarted before the end of the retention time, the data is still lost.

### Error Handling

The logs of the connector are available via the following command (launch the command from the repository where the docker-compose is) :

``` 
docker-compose logs -f connect
```

Where *connect* corresponds to the name of the service related to Kafka Connect in the docker-compose. If you also want the DEBUG level logs, add the following line to the value of the **CONNECT_LOG4J_LOGGERS** configuration parameter (in the **connect** service of the docker-compose) :

``` 
com.igrafx.kafka.sink.aggregationmain.domain.IGrafxAggregationSinkConnector=DEBUG,com.igrafx.kafka.sink.aggregationmain.domain.IGrafxAggregationSinkTask=DEBUG,com.igrafx.kafka.sink.aggregationmain.adapters.api.MainApiImpl=DEBUG,com.igrafx.kafka.sink.aggregationmain.adapters.system.MainSystemImpl=DEBUG
```

If there is an error during the creation of the Column Mapping, the connector is stopped (goes to the **FAILED** state)

If there is an error during the creation of the repositories, the suppression of old files, the writing of new data in the **{MainConfig.csvPath}/{projectId}/{connectorName}/archive/data_{uuid}_{timestamp}.csv** file, or during the sending of the file to the iGrafx Mining API, then the Task is stopped (goes to the **FAILED** state).

If a file couldn't be sent to the iGrafx Mining API, it is stored under the **{MainConfig.csvPath}/{projectId}/{connectorName}/archiveFailed/** repository (as long as it is not deleted because of the **retentionTimeInDay** for instance)

To monitor a connector in case of a failure, see the **CONTRIBUTING.md** file

<hr/>

### Compilation and deployment on LiveConnect

To compile and create the **.jar** needed to be able to use the connector in Kafka Connect, you need to go to the root of the project and to use the following command :

```
sbt assembly
```

You then copy the **aggregation-main-connector_{version}.jar** which is in the **artifacts** repository, and you paste it in the **docker-compose/connect-plugins/** repository of LiveConnect

By launching LiveConnect, the connector is now available

### Grouped Tasks Columns

While defining the Column Mapping (when **columnMapping.create** equals **true**), we can also optionally define, with the **columnMapping.groupedTasksColumns** property, a set of columns to use for grouping events that have the same value (in the defined columns) together. The events are grouped within their case, and grouped tasks aggregations need to be defined for the dimensions and metrics.

The idea of this functionality is to regroup multiple similar events into only one event.

#### Grouped Tasks Example

Let's take the following events as an example : 

| **CaseId** | **Activity** | **StartDate**  |  **EndDate**   | **Country** | **City** | **Price** |
|:-----------|:------------:|:--------------:|:--------------:|:-----------:|:--------:|:---------:|
| 1          |      A       | 10/10/10 08:38 | 11/10/10 08:38 |   France    |  Paris   |    10     |    
| 1          |      B       | 10/10/10 09:40 | 11/10/10 09:40 |   Germany   |  Berlin  |    20     |
| 1          |      A       | 10/10/10 10:42 | 11/10/10 10:42 |   France    | Toulouse |    30     |
| 1          |      C       | 10/10/10 11:50 | 11/10/10 11:50 |   Germany   |  Munich  |    10     |
| 1          |      C       | 10/10/10 12:50 | 11/10/10 12:50 |   Germany   | Hamburg  |    20     |
| 2          |      A       | 10/10/10 08:20 | 11/10/10 08:20 |   France    |  Rennes  |     5     |
| 2          |      B       | 10/10/10 09:30 | 11/10/10 09:30 |   Germany   |  Berlin  |    10     |
| 2          |      A       | 10/10/10 10:40 | 11/10/10 10:40 |   France    | Bordeaux |    25     |
| 2          |      A       | 10/10/10 11:50 | 11/10/10 11:50 |     USA     | New York |    10     |

And let's say that the column mapping properties in the connector look like this : 

```
columnMapping.create = "true",
columnMapping.caseIdColumnIndex = "0",
columnMapping.activityColumnIndex = "1",
columnMapping.timeInformationList = "{2;dd/MM/yy HH:mm},{3;dd/MM/yy HH:mm}",
columnMapping.dimensionsInformationList = "[{"columnIndex": 4, "name": "Country", "isCaseScope": false, "groupedTasksAggregation": "FIRST"},{"columnIndex": 5, "name": "City", "isCaseScope": false, "groupedTasksAggregation": "FIRST"}]",
columnMapping.metricsInformationList = "[{"columnIndex": 6, "name": "Price", "unit": "Euros", "isCaseScope": true, "aggregation": "MIN", "groupedTasksAggregation": "AVG"}]",
columnMapping.groupedTasksColumns = "[1, 4]"
```

Here the columns to use for grouping are the ones corresponding to the indexes 1 and 4 which are respectively the columns Activity and Country. They are defined through the **columnMapping.groupedTasksColumns** property

When **columnMapping.groupedTasksColumns** is defined, we also need to define the **groupedTasksAggregation** argument for each dimension/metric. With this example, here are the grouped tasks aggregations defined for the dimension and metric columns:
* **FIRST** for the **Country** dimension column
* **FIRST** for the **City** dimension column
* **AVG** for the **Price** metric column

For the dimension columns the valid grouped tasks aggregation values are **FIRST**/**LAST**  
For the metric columns the valid grouped tasks aggregation values are **FIRST**/**LAST**/**MIN**/**MAX**/**SUM**/**AVG**/**MEDIAN**

Consequently, within a case, all the events that have the same values for the Activity and Country columns will be grouped together, and the new values for the dimension and metric columns are computed according to their related *groupedTasksAggregation*

If the timestamp columns are not defined in the columns to use for grouping (here columns 2 and 3 are not defined in the **columnMapping.groupedTasksColumns** property), we don't have to define an aggregation as for the dimension or metrics:
* The lowest timestamp of all the events of a group will be used as the new start timestamp of the new single event.
* The highest timestamp of all the events of a group will be used as the new end timestamp of the new single event.

After the creation of the connector, a Mining project that has the column mapping defined above will receive those events and will regroup some of them in the following way:   

<ins>For CaseId 1:</ins>  
* The first and third events of this case have the same values for their **Activity (A)** and **Country (France)** columns. Consequently, they are grouped together to only make one event of activity A and of country France.  
* The second event is not grouped, as no other event in this case has an Activity named B and a Country named Germany.  
* The fourth and fifth events of this case  have the same values for their **Activity (C)** and **Country (Germany)** columns. Consequently, they are grouped together to only make one event of activity C and of country Germany.  

<ins>For CaseId 2:</ins>  
* The first and third events of this case have the same values for their **Activity (A)** and **Country (France)** columns. Consequently, they are grouped together to only make one event of activity A and of country France.  
* The second event is not grouped, as no other event in this case has an Activity named B and a Country named Germany.  
* The fourth event is not grouped, it has the same **Activity (A)** as the first and third events, but its **Country (USA)** is different.  

After grouping the similar events together, it gives us this list of events:

| **CaseId** | **Activity** | **StartDate**  |  **EndDate**   | **Country** | **City** | **Price** |
|:-----------|:------------:|:--------------:|:--------------:|:-----------:|:--------:|:---------:|
| 1          |      A       | 10/10/10 08:38 | 11/10/10 10:42 |   France    |  Paris   |    20     |    
| 1          |      B       | 10/10/10 09:40 | 11/10/10 09:40 |   Germany   |  Berlin  |    20     |
| 1          |      C       | 10/10/10 11:50 | 11/10/10 12:50 |   Germany   |  Munich  |    15     |
| 2          |      A       | 10/10/10 08:20 | 11/10/10 10:40 |   France    |  Rennes  |    15     |
| 2          |      B       | 10/10/10 09:30 | 11/10/10 09:30 |   Germany   |  Berlin  |    10     |
| 2          |      A       | 10/10/10 11:50 | 11/10/10 11:50 |     USA     | New York |    10     |

<ins>For CaseId 1:</ins>  
* The **first** event of this case in the new list of events was created by grouping the first and third events of this case in the initial list of events (before grouping).  
    * CaseId was **1** for the two events that were grouped, so it stays at **1** for the new single event.  
    * Activity was **A** for the two events that were grouped, so it stays at **A** for the new single event.  
    * StartDate was **10/10/10 08:38** for the first event that was grouped, and **10/10/10 10:42** for the second one. The lowest timestamp (**10/10/10 08:38**) is used as the start timestamp of the new single event.  
    * EndDate was **11/10/10 08:38** for the first event that was grouped, and **11/10/10 10:42** for the second one. The highest timestamp (**11/10/10 10:42**) is used as the end timestamp of the new single event.  
    * Country was **France** for the two events that were grouped, so it stays at **France** for the new single event.  
    * City was **Paris** for the first event that was grouped, and **Toulouse** for the second one. In the column mapping, **FIRST** was defined as the *groupedTasksAggregation* for this dimension, consequently, as **Paris** is the first value to come, it is the one used for the new single event.  
    * Price was **10** for the first event that was grouped, and **30** for the second one. In the column mapping, **AVG** was defined as the *groupedTasksAggregation* for this metric, consequently, **20** is the value of this metric for the new single event (20 being the result of the average of 10 and 30).  
* The **second** event of this case in the new list of events is identical to the second event of this case in the initial list of events (before grouping), as we couldn't group it with other events.  
* The **third** event of this case in the new list of events was created by grouping the fourth and fifth events of this case in the initial list of events (before grouping).  
    * CaseId was **1** for the two events that were grouped, so it stays at **1** for the new single event.  
    * Activity was **C** for the two events that were grouped, so it stays at **C** for the new single event.  
    * StartDate was **10/10/10 11:50** for the first event that was grouped, and **10/10/10 12:50** for the second one. The lowest timestamp (**10/10/10 11:50**) is used as the start timestamp of the new single event.  
    * EndDate was **11/10/10 11:50** for the first event that was grouped, and **11/10/10 12:50** for the second one. The highest timestamp (**11/10/10 12:50**) is used as the end timestamp of the new single event.  
    * Country was **Germany** for the two events that were grouped, so it stays at **Germany** for the new single event.  
    * City was **Munich** for the first event that was grouped, and **Hamburg** for the second one. In the column mapping, **FIRST** was defined as the *groupedTasksAggregation* for this dimension, consequently, as **Munich** is the first value to come, it is the one used for the new single event.  
    * Price was **10** for the first event that was grouped, and **20** for the second one. In the column mapping, **AVG** was defined as the *groupedTasksAggregation* for this metric, consequently, **15** is the value of this metric for the new single event (15 being the result of the average of 10 and 20).  

<ins>For CaseId 2:</ins>  
* The **first** event of this case in the new list of events was created by grouping the first and third events of this case in the initial list of events (before grouping).  
    * CaseId was **2** for the two events that were grouped, so it stays at **2** for the new single event.  
    * Activity was **A** for the two events that were grouped, so it stays at **A** for the new single event.  
    * StartDate was **10/10/10 08:20** for the first event that was grouped, and **10/10/10 10:40** for the second one. The lowest timestamp (**10/10/10 08:20**) is used as the start timestamp of the new single event.  
    * EndDate was **11/10/10 08:20** for the first event that was grouped, and **11/10/10 10:40** for the second one. The highest timestamp (**11/10/10 10:40**) is used as the end timestamp of the new single event.  
    * Country was **France** for the two events that were grouped, so it stays at **France** for the new single event.  
    * City was **Rennes** for the first event that was grouped, and **Bordeaux** for the second one. In the column mapping, **FIRST** was defined as the *groupedTasksAggregation* for this dimension, consequently, as **Rennes** is the first value to come, it is the one used for the new single event.  
    * Price was **5** for the first event that was grouped, and **25** for the second one. In the column mapping, **AVG** was defined as the *groupedTasksAggregation* for this metric, consequently, **15** is the value of this metric for the new single event (15 being the result of the average of 5 and 25).  
* The **second** event of this case in the new list of events is identical to the second event of this case in the initial list of events (before grouping), as we couldn't group it with other events.  
* The **third** event of this case in the new list of events is identical to the fourth event of this case in the initial list of events (before grouping), as we couldn't group it with other events.  

This new list of events is then used as the data in the Mining project.

As a side note, if for the same initial list of events we don't want to group any events together, the column mapping should be:

```
columnMapping.create = "true",
columnMapping.caseIdColumnIndex = "0",
columnMapping.activityColumnIndex = "1",
columnMapping.timeInformationList = "{2;dd/MM/yy HH:mm},{3;dd/MM/yy HH:mm}",
columnMapping.dimensionsInformationList = "[{"columnIndex": 4, "name": "Country", "isCaseScope": false},{"columnIndex": 5, "name": "City", "isCaseScope": false}]",
columnMapping.metricsInformationList = "[{"columnIndex": 6, "name": "Price", "unit": "Euros", "isCaseScope": true, "aggregation": "MIN"}]"
```

#### Remarks regarding the Column Mapping for grouped tasks

* An error will appear at the creation of the connector if the **columnMapping.groupedTasksColumns** property is defined but doesn't contain at least one column index of a time or dimension or metric column.  
* An error will appear at the creation of the connector if the **columnMapping.groupedTasksColumns** property is defined but not the **groupedTasksAggregation** argument of **all** the dimensions and/or metrics. 
* An error will appear at the creation of the connector if the **columnMapping.groupedTasksColumns** property is not defined but at least one dimension/metric defined its **groupedTasksAggregation** argument.

* If the **columnMapping.groupedTasksColumns** property is defined without the column index of the activity column, the connector will automatically add it to the set of grouped tasks columns indexes that is sent to the Mining. 
* If the **columnMapping.groupedTasksColumns** property is defined with the column index of the caseId column, the connector will automatically remove it from the set of grouped tasks columns indexes that is sent to the Mining.

## Examples 

For this example the **threshold.valuePattern** connector property is not set, so the aggregation is only made according to the **threshold.elementNumber** and **threshold.timeoutInSeconds** connector properties. The **columnMapping.groupedTasksColumns** is also not set, which means that the events will not be grouped in the Mining.

In this example, the created connector will receive information about events from a Kafka topic, will aggregate this information according to the thresholds defined during the connector's instantiation and will write the result of the aggregation in a CSV file (when one threshold is reached), that will be sent via a Http request to the iGrafx Mining API, to feed a iGrafx project.

Please follow the steps described in the **CONTRIBUTING.md** file at the root of this project to launch the infrastructure and access the ksqlDB CLI. All the following commands need to be written in this ksqlDB CLI.

First, start by creating a **new iGrafx Project** and by using the following command to apply modifications of a STREAM on data inserted before the creation/display of the STREAM :

``` 
SET 'auto.offset.reset'='earliest';
```

Then, create the ksqlDB STREAM associated with the source topic of the connector (the connector will receive data from the topic associated with this STREAM) :

```  
CREATE STREAM IGRAFX_AVRO (
    dataArray ARRAY<STRUCT<columnID INT, text VARCHAR, quote BOOLEAN>>
) WITH (
    kafka_topic = 'igrafx_avro', 
    partitions = 1, 
    value_format = 'AVRO'
);
```

And the STREAM associated with the Kafka topic that will receive the Logging events of the connector : 

``` 
CREATE STREAM LOGGING (
	EVENT_TYPE VARCHAR,
	IGRAFX_PROJECT VARCHAR,
	EVENT_DATE BIGINT,
	EVENT_SEQUENCE_ID VARCHAR,
	PAYLOAD VARCHAR
) WITH (
	KAFKA_TOPIC='logging_connector_test', 
	PARTITIONS=1, 
	REPLICAS=1, 
	VALUE_FORMAT='AVRO',
	VALUE_AVRO_SCHEMA_FULL_NAME='com.igrafx.IGrafxKafkaLoggingEventsSchema'
);
```

You can then instantiate the connector with the following command :

```
CREATE SINK CONNECTOR IGrafxConnectorCMLogging WITH (
    'connector.class' = 'com.igrafx.kafka.sink.aggregationmain.domain.IGrafxAggregationSinkConnector',
    'tasks.max' = '1',
    'topics' = 'igrafx_avro',
    'api.url' = 'https://dev-360-api.igrafxcloud.com',
    'api.authUrl' = 'https://auth-staging.igrafxcloud.com/realms/igrafx',
    'workGroupId' = 'd8a9265a-d47b-4077-9c76-6a39e848bdf2',
    'workGroupKey' = 'efce9417-b6b6-46c8-a0a2-59eee082789f',
    'projectId' = 'afb5707f-b358-4e6a-a6d9-834bca4a781c',
    'csv.encoding' = 'UTF-8',
    'csv.separator' = ',',
    'csv.quote' = '"',
    'csv.fieldsNumber' = '9',
    'csv.header' = 'true',
    'csv.defaultTextValue' = 'null',
    'retentionTimeInDay' = '100',
    'columnMapping.create' = 'true',
    'columnMapping.caseIdColumnIndex' = '0',
    'columnMapping.activityColumnIndex' = '1',
    'columnMapping.timeInformationList' = '{2;dd/MM/yy HH:mm},{3;dd/MM/yy HH:mm}',
    'columnMapping.dimensionsInformationList' = '[{"columnIndex": 4, "name": "Country", "isCaseScope": true, "aggregation": "FIRST"},{"columnIndex": 5, "name": "Region", "isCaseScope": false},{"columnIndex": 6, "name": "City", "isCaseScope": false}]',
    'columnMapping.metricsInformationList' = '[{"columnIndex": 7, "name": "Price", "unit": "Euros", "isCaseScope": true, "aggregation": "MIN"},{"columnIndex": 8, "name": "DepartmentNumber", "isCaseScope": false}]',
    'csv.endOfLine' = '\\n',
    'csv.escape' = '\\',
    'csv.comment' = '#',
    'kafkaLoggingEvents.isLogging' = 'true',
    'kafkaLoggingEvents.topic' = 'logging_connector_test',
    'threshold.elementNumber' = '20',
    'threshold.valuePattern' = '',
    'threshold.timeoutInSeconds' = 30,
    'bootstrap.servers' = 'broker:29092',
    'key.converter' = 'org.apache.kafka.connect.storage.StringConverter',
    'value.converter' = 'io.confluent.connect.avro.AvroConverter',
    'value.converter.schema.registry.url' = 'http://schema-registry:8081'
);
```

Please modify the values of the **api.url**, **api.authUrl**, **workGroupId**, **workGroupKey**, **projectId** properties, according to the iGrafx project you created

Here, the connector will try to aggregate together 20 basic events because of the **threshold.elementNumber** property, but will send to iGrafx an aggregation with any number of events (at least one) 30 seconds after the last sending (according to the value of the **threshold.timeoutInSeconds** property and if the **threshold.elementNumber** threshold isn't reached before those 30 seconds). 

The connector also create a Column Mapping for the iGrafx Project and will log file related events in the **logging_connector_test** Kafka topic (according to the value of the **kafkaLoggingEvents.topic** property)

If you don't want the connector to create a Column Mapping for the iGrafx Project, put the value of the **columnMapping.create** property to **false**, and if you don't want the connector to log its events in a Kafka topic, put the **kafkaLoggingEvents.isLogging** property to **false**

You can then add process events to the **igrafx_avro** Kafka topic with the following (In this example there are 51 events) :

``` 
INSERT INTO IGRAFX_AVRO (dataArray) VALUES (ARRAY[STRUCT(columnId := 0, text := '3', quote := false), STRUCT(columnId := 1, text := 'A', quote := true), STRUCT(columnId := 2, text := '10/10/10 08:05', quote := false), STRUCT(columnId := 3, text := '10/10/10 08:10', quote := false), STRUCT(columnId := 4, text := 'Germany', quote := false), STRUCT(columnId := 5, text := 'Region1', quote := false), STRUCT(columnId := 6, text := 'Berlin', quote := false), STRUCT(columnId := 7, text := '10', quote := false), STRUCT(columnId := 8, text := '10', quote := false)]);
INSERT INTO IGRAFX_AVRO (dataArray) VALUES (ARRAY[STRUCT(columnId := 0, text := '3', quote := false), STRUCT(columnId := 1, text := 'B', quote := true), STRUCT(columnId := 2, text := '10/10/10 08:15', quote := false), STRUCT(columnId := 3, text := '10/10/10 08:16', quote := false), STRUCT(columnId := 4, text := 'USA', quote := false), STRUCT(columnId := 5, text := 'Region2', quote := false), STRUCT(columnId := 6, text := 'Washington', quote := false), STRUCT(columnId := 7, text := '20', quote := false), STRUCT(columnId := 8, text := '16', quote := false)]);
INSERT INTO IGRAFX_AVRO (dataArray) VALUES (ARRAY[STRUCT(columnId := 0, text := '3', quote := false), STRUCT(columnId := 1, text := 'C', quote := true), STRUCT(columnId := 2, text := '10/10/10 08:16', quote := false), STRUCT(columnId := 3, text := '10/10/10 08:17', quote := false), STRUCT(columnId := 4, text := 'France', quote := false), STRUCT(columnId := 5, text := 'Region3', quote := false), STRUCT(columnId := 6, text := 'Paris', quote := false), STRUCT(columnId := 7, text := '20', quote := false), STRUCT(columnId := 8, text := '56', quote := false)]);
INSERT INTO IGRAFX_AVRO (dataArray) VALUES (ARRAY[STRUCT(columnId := 0, text := '3', quote := false), STRUCT(columnId := 1, text := 'D', quote := true), STRUCT(columnId := 2, text := '10/10/10 08:26', quote := false), STRUCT(columnId := 3, text := '10/10/10 08:27', quote := false), STRUCT(columnId := 4, text := 'Italy', quote := false), STRUCT(columnId := 5, text := 'Region4', quote := false), STRUCT(columnId := 6, text := 'Rome', quote := false), STRUCT(columnId := 7, text := '30', quote := false), STRUCT(columnId := 8, text := '47', quote := false)]);
INSERT INTO IGRAFX_AVRO (dataArray) VALUES (ARRAY[STRUCT(columnId := 0, text := '3', quote := false), STRUCT(columnId := 1, text := 'E', quote := true), STRUCT(columnId := 2, text := '10/10/10 08:29', quote := false), STRUCT(columnId := 3, text := '10/10/10 08:31', quote := false), STRUCT(columnId := 4, text := 'Germany', quote := false), STRUCT(columnId := 5, text := 'Region5', quote := false), STRUCT(columnId := 6, text := 'Berlin', quote := false), STRUCT(columnId := 7, text := '50', quote := false), STRUCT(columnId := 8, text := '20', quote := false)]);
INSERT INTO IGRAFX_AVRO (dataArray) VALUES (ARRAY[STRUCT(columnId := 0, text := '3', quote := false), STRUCT(columnId := 1, text := 'F', quote := true), STRUCT(columnId := 2, text := '10/10/10 08:29', quote := false), STRUCT(columnId := 3, text := '10/10/10 08:31', quote := false), STRUCT(columnId := 4, text := 'Italy', quote := false), STRUCT(columnId := 5, text := 'Region6', quote := false), STRUCT(columnId := 6, text := 'Rome', quote := false), STRUCT(columnId := 7, text := '50', quote := false), STRUCT(columnId := 8, text := '10', quote := false)]);
INSERT INTO IGRAFX_AVRO (dataArray) VALUES (ARRAY[STRUCT(columnId := 0, text := '3', quote := false), STRUCT(columnId := 1, text := 'G', quote := true), STRUCT(columnId := 2, text := '10/10/10 08:31', quote := false), STRUCT(columnId := 3, text := '10/10/10 08:32', quote := false), STRUCT(columnId := 4, text := 'France', quote := false), STRUCT(columnId := 5, text := 'Region7', quote := false), STRUCT(columnId := 6, text := 'Paris', quote := false), STRUCT(columnId := 7, text := '60', quote := false), STRUCT(columnId := 8, text := '40', quote := false)]);
INSERT INTO IGRAFX_AVRO (dataArray) VALUES (ARRAY[STRUCT(columnId := 0, text := '3', quote := false), STRUCT(columnId := 1, text := 'H', quote := true), STRUCT(columnId := 2, text := '10/10/10 08:32', quote := false), STRUCT(columnId := 3, text := '10/10/10 08:33', quote := false), STRUCT(columnId := 4, text := 'Germany', quote := false), STRUCT(columnId := 5, text := 'Region8', quote := false), STRUCT(columnId := 6, text := 'Berlin', quote := false), STRUCT(columnId := 7, text := '45', quote := false), STRUCT(columnId := 8, text := '10', quote := false)]);
INSERT INTO IGRAFX_AVRO (dataArray) VALUES (ARRAY[STRUCT(columnId := 0, text := '3', quote := false), STRUCT(columnId := 1, text := 'I', quote := true), STRUCT(columnId := 2, text := '10/10/10 08:33', quote := false), STRUCT(columnId := 3, text := '10/10/10 08:34', quote := false), STRUCT(columnId := 4, text := 'France', quote := false), STRUCT(columnId := 5, text := 'Region9', quote := false), STRUCT(columnId := 6, text := 'Paris', quote := false), STRUCT(columnId := 7, text := '10', quote := false), STRUCT(columnId := 8, text := '10', quote := false)]);
INSERT INTO IGRAFX_AVRO (dataArray) VALUES (ARRAY[STRUCT(columnId := 0, text := '3', quote := false), STRUCT(columnId := 1, text := 'J', quote := true), STRUCT(columnId := 2, text := '10/10/10 08:34', quote := false), STRUCT(columnId := 3, text := '10/10/10 08:35', quote := false), STRUCT(columnId := 4, text := 'Spain', quote := false), STRUCT(columnId := 5, text := 'Region10', quote := false), STRUCT(columnId := 6, text := 'Madrid', quote := false), STRUCT(columnId := 7, text := '20', quote := false), STRUCT(columnId := 8, text := '19', quote := false)]);
INSERT INTO IGRAFX_AVRO (dataArray) VALUES (ARRAY[STRUCT(columnId := 0, text := '3', quote := false), STRUCT(columnId := 1, text := 'K', quote := true), STRUCT(columnId := 2, text := '10/10/10 08:35', quote := false), STRUCT(columnId := 3, text := '10/10/10 08:36', quote := false), STRUCT(columnId := 4, text := 'Canada', quote := false), STRUCT(columnId := 5, text := 'Region11', quote := false), STRUCT(columnId := 6, text := 'Ottawa', quote := false), STRUCT(columnId := 7, text := '30', quote := false), STRUCT(columnId := 8, text := '10', quote := false)]);
INSERT INTO IGRAFX_AVRO (dataArray) VALUES (ARRAY[STRUCT(columnId := 0, text := '3', quote := false), STRUCT(columnId := 1, text := 'L', quote := true), STRUCT(columnId := 2, text := '10/10/10 08:36', quote := false), STRUCT(columnId := 3, text := '10/10/10 08:37', quote := false), STRUCT(columnId := 4, text := 'USA', quote := false), STRUCT(columnId := 5, text := 'Region12', quote := false), STRUCT(columnId := 6, text := 'Washington', quote := false), STRUCT(columnId := 7, text := '40', quote := false), STRUCT(columnId := 8, text := '16', quote := false)]);
INSERT INTO IGRAFX_AVRO (dataArray) VALUES (ARRAY[STRUCT(columnId := 0, text := '3', quote := false), STRUCT(columnId := 1, text := 'M', quote := true), STRUCT(columnId := 2, text := '10/10/10 08:37', quote := false), STRUCT(columnId := 3, text := '10/10/10 08:38', quote := false), STRUCT(columnId := 4, text := 'USA', quote := false), STRUCT(columnId := 5, text := 'Region13', quote := false), STRUCT(columnId := 6, text := 'Washington', quote := false), STRUCT(columnId := 7, text := '50', quote := false), STRUCT(columnId := 8, text := '10', quote := false)]);
INSERT INTO IGRAFX_AVRO (dataArray) VALUES (ARRAY[STRUCT(columnId := 0, text := '3', quote := false), STRUCT(columnId := 1, text := 'N', quote := true), STRUCT(columnId := 2, text := '10/10/10 08:38', quote := false), STRUCT(columnId := 3, text := '10/10/10 08:39', quote := false), STRUCT(columnId := 4, text := 'Canada', quote := false), STRUCT(columnId := 5, text := 'Region14', quote := false), STRUCT(columnId := 6, text := 'Ottawa', quote := false), STRUCT(columnId := 7, text := '20', quote := false), STRUCT(columnId := 8, text := '10', quote := false)]);
INSERT INTO IGRAFX_AVRO (dataArray) VALUES (ARRAY[STRUCT(columnId := 0, text := '3', quote := false), STRUCT(columnId := 1, text := 'O', quote := true), STRUCT(columnId := 2, text := '10/10/10 08:39', quote := false), STRUCT(columnId := 3, text := '10/10/10 08:40', quote := false), STRUCT(columnId := 4, text := 'Italy', quote := false), STRUCT(columnId := 5, text := 'Region15', quote := false), STRUCT(columnId := 6, text := 'Rome', quote := false), STRUCT(columnId := 7, text := '30', quote := false), STRUCT(columnId := 8, text := '84', quote := false)]);
INSERT INTO IGRAFX_AVRO (dataArray) VALUES (ARRAY[STRUCT(columnId := 0, text := '3', quote := false), STRUCT(columnId := 1, text := 'P', quote := true), STRUCT(columnId := 2, text := '10/10/10 08:40', quote := false), STRUCT(columnId := 3, text := '10/10/10 08:41', quote := false), STRUCT(columnId := 4, text := 'Canada', quote := false), STRUCT(columnId := 5, text := 'Region16', quote := false), STRUCT(columnId := 6, text := 'Ottawa', quote := false), STRUCT(columnId := 7, text := '40', quote := false), STRUCT(columnId := 8, text := '74', quote := false)]);
INSERT INTO IGRAFX_AVRO (dataArray) VALUES (ARRAY[STRUCT(columnId := 0, text := '3', quote := false), STRUCT(columnId := 1, text := 'Q', quote := true), STRUCT(columnId := 2, text := '10/10/10 08:41', quote := false), STRUCT(columnId := 3, text := '10/10/10 08:42', quote := false), STRUCT(columnId := 4, text := 'Italy', quote := false), STRUCT(columnId := 5, text := 'Region17', quote := false), STRUCT(columnId := 6, text := 'Rome', quote := false), STRUCT(columnId := 7, text := '50', quote := false), STRUCT(columnId := 8, text := '75', quote := false)]);
INSERT INTO IGRAFX_AVRO (dataArray) VALUES (ARRAY[STRUCT(columnId := 0, text := '3', quote := false), STRUCT(columnId := 1, text := 'R', quote := true), STRUCT(columnId := 2, text := '10/10/10 08:42', quote := false), STRUCT(columnId := 3, text := '10/10/10 08:43', quote := false), STRUCT(columnId := 4, text := 'Canada', quote := false), STRUCT(columnId := 5, text := 'Region18', quote := false), STRUCT(columnId := 6, text := 'Ottawa', quote := false), STRUCT(columnId := 7, text := '20', quote := false), STRUCT(columnId := 8, text := '60', quote := false)]);
INSERT INTO IGRAFX_AVRO (dataArray) VALUES (ARRAY[STRUCT(columnId := 0, text := '3', quote := false), STRUCT(columnId := 1, text := 'S', quote := true), STRUCT(columnId := 2, text := '10/10/10 08:43', quote := false), STRUCT(columnId := 3, text := '10/10/10 08:44', quote := false), STRUCT(columnId := 4, text := 'Germany', quote := false), STRUCT(columnId := 5, text := 'Region19', quote := false), STRUCT(columnId := 6, text := 'Berlin', quote := false), STRUCT(columnId := 7, text := '20', quote := false), STRUCT(columnId := 8, text := '10', quote := false)]);
INSERT INTO IGRAFX_AVRO (dataArray) VALUES (ARRAY[STRUCT(columnId := 0, text := '3', quote := false), STRUCT(columnId := 1, text := 'T', quote := true), STRUCT(columnId := 2, text := '10/10/10 08:44', quote := false), STRUCT(columnId := 3, text := '10/10/10 08:45', quote := false), STRUCT(columnId := 4, text := 'Canada', quote := false), STRUCT(columnId := 5, text := 'Region20', quote := false), STRUCT(columnId := 6, text := 'Ottawa', quote := false), STRUCT(columnId := 7, text := '10', quote := false), STRUCT(columnId := 8, text := '10', quote := false)]);
INSERT INTO IGRAFX_AVRO (dataArray) VALUES (ARRAY[STRUCT(columnId := 0, text := '3', quote := false), STRUCT(columnId := 1, text := 'U', quote := true), STRUCT(columnId := 2, text := '10/10/10 08:45', quote := false), STRUCT(columnId := 3, text := '10/10/10 08:46', quote := false), STRUCT(columnId := 4, text := 'USA', quote := false), STRUCT(columnId := 5, text := 'Region21', quote := false), STRUCT(columnId := 6, text := 'Washington', quote := false), STRUCT(columnId := 7, text := '30', quote := false), STRUCT(columnId := 8, text := '10', quote := false)]);
INSERT INTO IGRAFX_AVRO (dataArray) VALUES (ARRAY[STRUCT(columnId := 0, text := '3', quote := false), STRUCT(columnId := 1, text := 'V', quote := true), STRUCT(columnId := 2, text := '10/10/10 08:46', quote := false), STRUCT(columnId := 3, text := '10/10/10 08:47', quote := false), STRUCT(columnId := 4, text := 'Canada', quote := false), STRUCT(columnId := 5, text := 'Region22', quote := false), STRUCT(columnId := 6, text := 'Ottawa', quote := false), STRUCT(columnId := 7, text := '40', quote := false), STRUCT(columnId := 8, text := '10', quote := false)]);
INSERT INTO IGRAFX_AVRO (dataArray) VALUES (ARRAY[STRUCT(columnId := 0, text := '3', quote := false), STRUCT(columnId := 1, text := 'W', quote := true), STRUCT(columnId := 2, text := '10/10/10 08:47', quote := false), STRUCT(columnId := 3, text := '10/10/10 08:48', quote := false), STRUCT(columnId := 4, text := 'Canada', quote := false), STRUCT(columnId := 5, text := 'Region23', quote := false), STRUCT(columnId := 6, text := 'Ottawa', quote := false), STRUCT(columnId := 7, text := '50', quote := false), STRUCT(columnId := 8, text := '10', quote := false)]);
INSERT INTO IGRAFX_AVRO (dataArray) VALUES (ARRAY[STRUCT(columnId := 0, text := '3', quote := false), STRUCT(columnId := 1, text := 'X', quote := true), STRUCT(columnId := 2, text := '10/10/10 08:48', quote := false), STRUCT(columnId := 3, text := '10/10/10 08:49', quote := false), STRUCT(columnId := 4, text := 'Germany', quote := false), STRUCT(columnId := 5, text := 'Region24', quote := false), STRUCT(columnId := 6, text := 'Berlin', quote := false), STRUCT(columnId := 7, text := '40', quote := false), STRUCT(columnId := 8, text := '10', quote := false)]);
INSERT INTO IGRAFX_AVRO (dataArray) VALUES (ARRAY[STRUCT(columnId := 0, text := '3', quote := false), STRUCT(columnId := 1, text := 'Y', quote := true), STRUCT(columnId := 2, text := '10/10/10 08:49', quote := false), STRUCT(columnId := 3, text := '10/10/10 08:50', quote := false), STRUCT(columnId := 4, text := 'Spain', quote := false), STRUCT(columnId := 5, text := 'Region25', quote := false), STRUCT(columnId := 6, text := 'Madrid', quote := false), STRUCT(columnId := 7, text := '20', quote := false), STRUCT(columnId := 8, text := '10', quote := false)]);
INSERT INTO IGRAFX_AVRO (dataArray) VALUES (ARRAY[STRUCT(columnId := 0, text := '3', quote := false), STRUCT(columnId := 1, text := 'Z', quote := true), STRUCT(columnId := 2, text := '10/10/10 08:50', quote := false), STRUCT(columnId := 3, text := '10/10/10 08:51', quote := false), STRUCT(columnId := 4, text := 'France', quote := false), STRUCT(columnId := 5, text := 'Region26', quote := false), STRUCT(columnId := 6, text := 'Paris', quote := false), STRUCT(columnId := 7, text := '10', quote := false), STRUCT(columnId := 8, text := '10', quote := false)]);
INSERT INTO IGRAFX_AVRO (dataArray) VALUES (ARRAY[STRUCT(columnId := 0, text := '5', quote := false), STRUCT(columnId := 1, text := 'A', quote := true), STRUCT(columnId := 2, text := '10/10/10 08:05', quote := false), STRUCT(columnId := 3, text := '10/10/10 08:10', quote := false), STRUCT(columnId := 4, text := 'France', quote := false), STRUCT(columnId := 5, text := 'Region27', quote := false), STRUCT(columnId := 6, text := 'Paris', quote := false), STRUCT(columnId := 7, text := '10', quote := false), STRUCT(columnId := 8, text := '10', quote := false)]);
INSERT INTO IGRAFX_AVRO (dataArray) VALUES (ARRAY[STRUCT(columnId := 0, text := '5', quote := false), STRUCT(columnId := 1, text := 'B', quote := true), STRUCT(columnId := 2, text := '10/10/10 08:15', quote := false), STRUCT(columnId := 3, text := '10/10/10 08:16', quote := false), STRUCT(columnId := 4, text := 'Spain', quote := false), STRUCT(columnId := 5, text := 'Region28', quote := false), STRUCT(columnId := 6, text := 'Madrid', quote := false), STRUCT(columnId := 7, text := '30', quote := false), STRUCT(columnId := 8, text := '10', quote := false)]);
INSERT INTO IGRAFX_AVRO (dataArray) VALUES (ARRAY[STRUCT(columnId := 0, text := '5', quote := false), STRUCT(columnId := 1, text := 'C', quote := true), STRUCT(columnId := 2, text := '10/10/10 08:16', quote := false), STRUCT(columnId := 3, text := '10/10/10 08:17', quote := false), STRUCT(columnId := 4, text := 'Germany', quote := false), STRUCT(columnId := 5, text := 'Region29', quote := false), STRUCT(columnId := 6, text := 'Berlin', quote := false), STRUCT(columnId := 7, text := '20', quote := false), STRUCT(columnId := 8, text := '10', quote := false)]);
INSERT INTO IGRAFX_AVRO (dataArray) VALUES (ARRAY[STRUCT(columnId := 0, text := '5', quote := false), STRUCT(columnId := 1, text := 'D', quote := true), STRUCT(columnId := 2, text := '10/10/10 08:26', quote := false), STRUCT(columnId := 3, text := '10/10/10 08:27', quote := false), STRUCT(columnId := 4, text := 'USA', quote := false), STRUCT(columnId := 5, text := 'Region30', quote := false), STRUCT(columnId := 6, text := 'Washington', quote := false), STRUCT(columnId := 7, text := '40', quote := false), STRUCT(columnId := 8, text := '10', quote := false)]);
INSERT INTO IGRAFX_AVRO (dataArray) VALUES (ARRAY[STRUCT(columnId := 0, text := '5', quote := false), STRUCT(columnId := 1, text := 'E', quote := true), STRUCT(columnId := 2, text := '10/10/10 08:29', quote := false), STRUCT(columnId := 3, text := '10/10/10 08:31', quote := false), STRUCT(columnId := 4, text := 'Italy', quote := false), STRUCT(columnId := 5, text := 'Region31', quote := false), STRUCT(columnId := 6, text := 'Rome', quote := false), STRUCT(columnId := 7, text := '10', quote := false), STRUCT(columnId := 8, text := '10', quote := false)]);
INSERT INTO IGRAFX_AVRO (dataArray) VALUES (ARRAY[STRUCT(columnId := 0, text := '5', quote := false), STRUCT(columnId := 1, text := 'F', quote := true), STRUCT(columnId := 2, text := '10/10/10 08:29', quote := false), STRUCT(columnId := 3, text := '10/10/10 08:31', quote := false), STRUCT(columnId := 4, text := 'France', quote := false), STRUCT(columnId := 5, text := 'Region32', quote := false), STRUCT(columnId := 6, text := 'Paris', quote := false), STRUCT(columnId := 7, text := '80', quote := false), STRUCT(columnId := 8, text := '10', quote := false)]);
INSERT INTO IGRAFX_AVRO (dataArray) VALUES (ARRAY[STRUCT(columnId := 0, text := '5', quote := false), STRUCT(columnId := 1, text := 'G', quote := true), STRUCT(columnId := 2, text := '10/10/10 08:31', quote := false), STRUCT(columnId := 3, text := '10/10/10 08:32', quote := false), STRUCT(columnId := 4, text := 'Spain', quote := false), STRUCT(columnId := 5, text := 'Region33', quote := false), STRUCT(columnId := 6, text := 'Madrid', quote := false), STRUCT(columnId := 7, text := '40', quote := false), STRUCT(columnId := 8, text := '10', quote := false)]);
INSERT INTO IGRAFX_AVRO (dataArray) VALUES (ARRAY[STRUCT(columnId := 0, text := '5', quote := false), STRUCT(columnId := 1, text := 'H', quote := true), STRUCT(columnId := 2, text := '10/10/10 08:32', quote := false), STRUCT(columnId := 3, text := '10/10/10 08:33', quote := false), STRUCT(columnId := 4, text := 'USA', quote := false), STRUCT(columnId := 5, text := 'Region34', quote := false), STRUCT(columnId := 6, text := 'Washington', quote := false), STRUCT(columnId := 7, text := '30', quote := false), STRUCT(columnId := 8, text := '10', quote := false)]);
INSERT INTO IGRAFX_AVRO (dataArray) VALUES (ARRAY[STRUCT(columnId := 0, text := '5', quote := false), STRUCT(columnId := 1, text := 'I', quote := true), STRUCT(columnId := 2, text := '10/10/10 08:33', quote := false), STRUCT(columnId := 3, text := '10/10/10 08:34', quote := false), STRUCT(columnId := 4, text := 'Italy', quote := false), STRUCT(columnId := 5, text := 'Region35', quote := false), STRUCT(columnId := 6, text := 'Rome', quote := false), STRUCT(columnId := 7, text := '40', quote := false), STRUCT(columnId := 8, text := '10', quote := false)]);
INSERT INTO IGRAFX_AVRO (dataArray) VALUES (ARRAY[STRUCT(columnId := 0, text := '5', quote := false), STRUCT(columnId := 1, text := 'J', quote := true), STRUCT(columnId := 2, text := '10/10/10 08:34', quote := false), STRUCT(columnId := 3, text := '10/10/10 08:35', quote := false), STRUCT(columnId := 4, text := 'Germany', quote := false), STRUCT(columnId := 5, text := 'Region36', quote := false), STRUCT(columnId := 6, text := 'Berlin', quote := false), STRUCT(columnId := 7, text := '20', quote := false), STRUCT(columnId := 8, text := '10', quote := false)]);
INSERT INTO IGRAFX_AVRO (dataArray) VALUES (ARRAY[STRUCT(columnId := 0, text := '5', quote := false), STRUCT(columnId := 1, text := 'K', quote := true), STRUCT(columnId := 2, text := '10/10/10 08:35', quote := false), STRUCT(columnId := 3, text := '10/10/10 08:36', quote := false), STRUCT(columnId := 4, text := 'France', quote := false), STRUCT(columnId := 5, text := 'Region37', quote := false), STRUCT(columnId := 6, text := 'Paris', quote := false), STRUCT(columnId := 7, text := '20', quote := false), STRUCT(columnId := 8, text := '10', quote := false)]);
INSERT INTO IGRAFX_AVRO (dataArray) VALUES (ARRAY[STRUCT(columnId := 0, text := '5', quote := false), STRUCT(columnId := 1, text := 'L', quote := true), STRUCT(columnId := 2, text := '10/10/10 08:36', quote := false), STRUCT(columnId := 3, text := '10/10/10 08:37', quote := false), STRUCT(columnId := 4, text := 'Canada', quote := false), STRUCT(columnId := 5, text := 'Region38', quote := false), STRUCT(columnId := 6, text := 'Ottawa', quote := false), STRUCT(columnId := 7, text := '40', quote := false), STRUCT(columnId := 8, text := '10', quote := false)]);
INSERT INTO IGRAFX_AVRO (dataArray) VALUES (ARRAY[STRUCT(columnId := 0, text := '5', quote := false), STRUCT(columnId := 1, text := 'M', quote := true), STRUCT(columnId := 2, text := '10/10/10 08:37', quote := false), STRUCT(columnId := 3, text := '10/10/10 08:38', quote := false), STRUCT(columnId := 4, text := 'Spain', quote := false), STRUCT(columnId := 5, text := 'Region39', quote := false), STRUCT(columnId := 6, text := 'Madrid', quote := false), STRUCT(columnId := 7, text := '40', quote := false), STRUCT(columnId := 8, text := '10', quote := false)]);
INSERT INTO IGRAFX_AVRO (dataArray) VALUES (ARRAY[STRUCT(columnId := 0, text := '5', quote := false), STRUCT(columnId := 1, text := 'N', quote := true), STRUCT(columnId := 2, text := '10/10/10 08:38', quote := false), STRUCT(columnId := 3, text := '10/10/10 08:39', quote := false), STRUCT(columnId := 4, text := 'Spain', quote := false), STRUCT(columnId := 5, text := 'Region39', quote := false), STRUCT(columnId := 6, text := 'Madrid', quote := false), STRUCT(columnId := 7, text := '60', quote := false), STRUCT(columnId := 8, text := '10', quote := false)]);
INSERT INTO IGRAFX_AVRO (dataArray) VALUES (ARRAY[STRUCT(columnId := 0, text := '5', quote := false), STRUCT(columnId := 1, text := 'O', quote := true), STRUCT(columnId := 2, text := '10/10/10 08:39', quote := false), STRUCT(columnId := 3, text := '10/10/10 08:40', quote := false), STRUCT(columnId := 4, text := 'USA', quote := false), STRUCT(columnId := 5, text := 'Region39', quote := false), STRUCT(columnId := 6, text := 'Washington', quote := false), STRUCT(columnId := 7, text := '70', quote := false), STRUCT(columnId := 8, text := '10', quote := false)]);
INSERT INTO IGRAFX_AVRO (dataArray) VALUES (ARRAY[STRUCT(columnId := 0, text := '5', quote := false), STRUCT(columnId := 1, text := 'P', quote := true), STRUCT(columnId := 2, text := '10/10/10 08:40', quote := false), STRUCT(columnId := 3, text := '10/10/10 08:41', quote := false), STRUCT(columnId := 4, text := 'Spain', quote := false), STRUCT(columnId := 5, text := 'Region39', quote := false), STRUCT(columnId := 6, text := 'Madrid', quote := false), STRUCT(columnId := 7, text := '10', quote := false), STRUCT(columnId := 8, text := '10', quote := false)]);
INSERT INTO IGRAFX_AVRO (dataArray) VALUES (ARRAY[STRUCT(columnId := 0, text := '5', quote := false), STRUCT(columnId := 1, text := 'Q', quote := true), STRUCT(columnId := 2, text := '10/10/10 08:41', quote := false), STRUCT(columnId := 3, text := '10/10/10 08:42', quote := false), STRUCT(columnId := 4, text := 'Spain', quote := false), STRUCT(columnId := 5, text := 'Region39', quote := false), STRUCT(columnId := 6, text := 'Madrid', quote := false), STRUCT(columnId := 7, text := '30', quote := false), STRUCT(columnId := 8, text := '10', quote := false)]);
INSERT INTO IGRAFX_AVRO (dataArray) VALUES (ARRAY[STRUCT(columnId := 0, text := '5', quote := false), STRUCT(columnId := 1, text := 'R', quote := true), STRUCT(columnId := 2, text := '10/10/10 08:42', quote := false), STRUCT(columnId := 3, text := '10/10/10 08:43', quote := false), STRUCT(columnId := 4, text := 'France', quote := false), STRUCT(columnId := 5, text := 'Region39', quote := false), STRUCT(columnId := 6, text := 'Paris', quote := false), STRUCT(columnId := 7, text := '20', quote := false), STRUCT(columnId := 8, text := '10', quote := false)]);
INSERT INTO IGRAFX_AVRO (dataArray) VALUES (ARRAY[STRUCT(columnId := 0, text := '5', quote := false), STRUCT(columnId := 1, text := 'S', quote := true), STRUCT(columnId := 2, text := '10/10/10 08:43', quote := false), STRUCT(columnId := 3, text := '10/10/10 08:44', quote := false), STRUCT(columnId := 4, text := 'Italy', quote := false), STRUCT(columnId := 5, text := 'Region39', quote := false), STRUCT(columnId := 6, text := 'Rome', quote := false), STRUCT(columnId := 7, text := '30', quote := false), STRUCT(columnId := 8, text := '10', quote := false)]);
INSERT INTO IGRAFX_AVRO (dataArray) VALUES (ARRAY[STRUCT(columnId := 0, text := '5', quote := false), STRUCT(columnId := 1, text := 'T', quote := true), STRUCT(columnId := 2, text := '10/10/10 08:44', quote := false), STRUCT(columnId := 3, text := '10/10/10 08:45', quote := false), STRUCT(columnId := 4, text := 'France', quote := false), STRUCT(columnId := 5, text := 'Region39', quote := false), STRUCT(columnId := 6, text := 'Paris', quote := false), STRUCT(columnId := 7, text := '40', quote := false), STRUCT(columnId := 8, text := '10', quote := false)]);
INSERT INTO IGRAFX_AVRO (dataArray) VALUES (ARRAY[STRUCT(columnId := 0, text := '5', quote := false), STRUCT(columnId := 1, text := 'U', quote := true), STRUCT(columnId := 2, text := '10/10/10 08:45', quote := false), STRUCT(columnId := 3, text := '10/10/10 08:46', quote := false), STRUCT(columnId := 4, text := 'Spain', quote := false), STRUCT(columnId := 5, text := 'Region39', quote := false), STRUCT(columnId := 6, text := 'Madrid', quote := false), STRUCT(columnId := 7, text := '50', quote := false), STRUCT(columnId := 8, text := '10', quote := false)]);
INSERT INTO IGRAFX_AVRO (dataArray) VALUES (ARRAY[STRUCT(columnId := 0, text := '5', quote := false), STRUCT(columnId := 1, text := 'V', quote := true), STRUCT(columnId := 2, text := '10/10/10 08:46', quote := false), STRUCT(columnId := 3, text := '10/10/10 08:47', quote := false), STRUCT(columnId := 4, text := 'Germany', quote := false), STRUCT(columnId := 5, text := 'Region39', quote := false), STRUCT(columnId := 6, text := 'Berlin', quote := false), STRUCT(columnId := 7, text := '10', quote := false), STRUCT(columnId := 8, text := '10', quote := false)]);
INSERT INTO IGRAFX_AVRO (dataArray) VALUES (ARRAY[STRUCT(columnId := 0, text := '5', quote := false), STRUCT(columnId := 1, text := 'X', quote := true), STRUCT(columnId := 2, text := '10/10/10 08:47', quote := false), STRUCT(columnId := 3, text := '10/10/10 08:49', quote := false), STRUCT(columnId := 4, text := 'Germany', quote := false), STRUCT(columnId := 5, text := 'Region39', quote := false), STRUCT(columnId := 6, text := 'Berlin', quote := false), STRUCT(columnId := 7, text := '30', quote := false), STRUCT(columnId := 8, text := '10', quote := false)]);
INSERT INTO IGRAFX_AVRO (dataArray) VALUES (ARRAY[STRUCT(columnId := 0, text := '5', quote := false), STRUCT(columnId := 1, text := 'Y', quote := true), STRUCT(columnId := 2, text := '10/10/10 08:49', quote := false), STRUCT(columnId := 3, text := '10/10/10 08:50', quote := false), STRUCT(columnId := 4, text := 'Spain', quote := false), STRUCT(columnId := 5, text := 'Region39', quote := false), STRUCT(columnId := 6, text := 'Madrid', quote := false), STRUCT(columnId := 7, text := '10', quote := false), STRUCT(columnId := 8, text := '10', quote := false)]);
INSERT INTO IGRAFX_AVRO (dataArray) VALUES (ARRAY[STRUCT(columnId := 0, text := '5', quote := false), STRUCT(columnId := 1, text := 'Z', quote := true), STRUCT(columnId := 2, text := '10/10/10 08:50', quote := false), STRUCT(columnId := 3, text := '10/10/10 08:51', quote := false), STRUCT(columnId := 4, text := 'France', quote := false), STRUCT(columnId := 5, text := 'Region39', quote := false), STRUCT(columnId := 6, text := 'Paris', quote := false), STRUCT(columnId := 7, text := '50', quote := false), STRUCT(columnId := 8, text := '10', quote := false)]);
```

The connector will receive this data, and will create and send 3 CSV files to the iGrafx Mining API (2 files with 20 events and, 30 seconds later, one last file with 11 events)

You can check the content of the created files in the **{MainConfig.csvPath}/{projectId}/{connectorName}/archive/** repository (from the repository with the docker-compose.yml file that launched the infrastructure)

You can also check the Logging events sent by the connector with the command : 

``` 
SELECT * FROM LOGGING EMIT CHANGES;
```

Finally, you can drop the connector with the command :

``` 
DROP CONNECTOR IGrafxConnectorCMLogging;
```