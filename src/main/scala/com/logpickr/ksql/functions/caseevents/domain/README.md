# UDF Case Events requesting Druid

To get information about this UDF direclty in ksqlDB, use the command :

``` 
DESCRIBE FUNCTION LOGPICKR_CASE_EVENTS;
```

This UDF allows retrieving from the **_vertex** Druid DataSource information related to a given caseId. The information corresponds to the following columns :

* __time
* enddate
* vertex_name

**UDF Signature :**

``` 
def logpickrCaseEvents(caseId: String, projectId: String, workgroupId: String, workgroupKey: String, host: String, port: String): util.List[Struct]
```

The output Struct format :

``` 
STRUCT<START_DATE VARCHAR(STRING), END_DATE VARCHAR(STRING), VERTEX_NAME VARCHAR(STRING)>
```

Here, **START_DATE** corresponds to the **__time** column, **END_DATE** to the **enddate** one, and finally **VERTEX_NAME** which corresponds to the **vertex_name** column

Nevertheless, it is an Array with this Structure that is returned, in order to retrieve those information for each line associated to the caseId

The SQL request used by the UDF is the following :

``` 
SELECT __time AS startdate, enddate, vertex_name AS vertexName
FROM "projectId_vertex"
WHERE vertex_name is not NULL AND caseid = 'caseIdParam'
```
where **projectId** corresponds to the Logpickr project ID and **caseIdParam** corresponds to UDF parameter related to the caseId

## Parameters :

* **caseId** : The caseId for which we want to get information
* **projectId** : The id of the Logpickr project containing the information
* **workgroupId** : The id of the Logpickr workgroup related to the project containing the information
* **workgroupKey** : The key of the Logpickr workgroup related to the project containing the information
* **host** : Corresponds to the Druid host
* **port** : Corresponds to the Druid connexion port

## Examples in ksqlDB

To follow the examples, start by using the following command to apply modifications of a STREAM on data inserted before the creation/display of the STREAM :

``` 
SET 'auto.offset.reset'='earliest';
```

* Creation of the initial STREAM storing the caseId for which we want to get information :

```
CREATE STREAM s1 (
  caseId VARCHAR
  ) WITH (
  kafka_topic = 's1',
  partitions = 1,
  value_format = 'avro'
  );
```

* Creation of the STREAM doing the call to the UDF (here the UDF parameters are given as an example and can be modified, except for the caseId, as it must correspond to the caseId column of the previous STREAM)

``` 
CREATE STREAM s2 AS SELECT 
    caseId, 
    logpickr_case_events(caseId, '8968a61c-1fb5-4721-9a8b-d09198eef06b', 'druid_system', 'lpkAuth', 'dev-api.logpickr.com', '8082') AS informations 
    FROM s1 EMIT CHANGES;
```

* Insertion of a caseID for which we want to get information (in order to retrieve information for a given caseId, the caseId needs to exist in the Logpickr project)

``` 
INSERT INTO s1 (caseId) VALUES ('3');
```

* Display of the final result

```` 
SELECT caseId, informations FROM s2 EMIT CHANGES;
````