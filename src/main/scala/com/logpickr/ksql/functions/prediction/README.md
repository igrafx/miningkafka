# UDF Prediction

This UDF allows retrieving prediction information for one or multiple caseId(s) that belong to a given Logpickr project. The prediction is launched and retrieved via the Logpickr API and the results are returned by the UDF 

To get information about this UDF directly in ksqlDB, use the following command :

```
DESCRIBE FUNCTION LOGPICKR_PREDICTION;
```

**UDF Signature :**

``` 
def logpickrSessions(
      caseIds: util.List[String],
      projectId: String,
      workgroupId: String,
      workgroupKey: String,
      apiUrl: String,
      authUrl: String,
      tryNumber: Int
  ): Struct
```

Here the **caseIds** parameter lists all the caseId for which we want to get a prediction. The **projectId**, **workgroupId** and **workgroupKey** parameters are necessary to call the Logpickr API

The Structure used in the function's return :

```
STRUCT<
    PREDICTION_ID VARCHAR(STRING), 
    PREDICTIONS ARRAY<STRUCT<
        CASE_ID VARCHAR(STRING), 
        FINAL_PROCESS_KEY_PREDICTIONS ARRAY<STRUCT<
            FINAL_PROCESS_KEY VARCHAR(STRING), 
            PREDICTIONS ARRAY<STRUCT<
                FINAL_PROCESS_KEY_CONFIDENCE DOUBLE, 
                NEXT_STEP (optional) STRUCT<
                    NAME VARCHAR(STRING), 
                    START_PREDICTION_STEP VARCHAR(STRING), 
                    START_CONFIDENCE_INTERVAL (optional) STRUCT<
                        START_INTERVAL VARCHAR(STRING), 
                        END_INTERVAL VARCHAR(STRING), 
                        INTERVAL_PROBABILITY DOUBLE
                    >, 
                    END_PREDICTION_STEP VARCHAR(STRING), 
                    END_CONFIDENCE_INTERVAL (optional) STRUCT<
                        START_INTERVAL VARCHAR(STRING), 
                        END_INTERVAL VARCHAR(STRING), 
                        INTERVAL_PROBABILITY DOUBLE
                    >
                >, 
                ESTIMATED_END_OF_CASE (optional) VARCHAR(STRING), 
                END_OF_CASE_CONFIDENCE_INTERVAL (optional) STRUCT<
                    START_INTERVAL VARCHAR(STRING), 
                    END_INTERVAL VARCHAR(STRING), 
                    INTERVAL_PROBABILITY DOUBLE
                >
            >>
        >>
    >>
>
```

## Parameters :

* **caseIds** : The list of caseIds for which we want to get a prediction
* **projectId** : The Logpickr project ID containing the caseIds
* **workgroupId** : The ID of the workgroup related to the project, used for the authentication
* **workgroupKey** : The key of the workgroup related to the project, used for the authentication
* **apiUrl** : The URL corresponding to the Logpickr API (for instance : https://dev-api.logpickr.com)
* **authUrl** : The URL corresponding to the Logpickr Authentication API (for instance : https://dev-auth.logpickr.com)
* **tryNumber** : As retrieving a prediction may take some time, this parameter allows to define a maximum number of tries to retrieve the prediction before throwing an error (the time between two tries is defined via the tryIntervalInMilliseconds variable in the PredictionConstants object)

## Examples in ksqlDB

To follow those examples, start by using the following command to apply modifications of a STREAM on data inserted before the creation/display of the STREAM :

``` 
SET 'auto.offset.reset'='earliest';
```

First of all, be sure to have a Logpickr project with a valid Prediction Model. The values given in this example for the projectId/workgroupId/workgroupKey/apiUrl/authUrl/caseIds/tryNumber can be modified.

The first STREAM to create in ksqlDB contains all the arrays with caseIds for which we want to retrieve a prediction :

``` 
CREATE STREAM s1 (
	caseIds ARRAY<VARCHAR>
) WITH (
	kafka_topic = 's1',
	partitions = 1,
	value_format = 'avro'
);
```

It is then possible to insert data :

``` 
INSERT INTO s1 (caseIds) VALUES (ARRAY['10', '100']);
```

It is now possible to call the UDF with the following STREAM :

``` 
CREATE STREAM s2 AS SELECT 
    logpickr_prediction(
        caseIds,         
        'ff37ac92-9811-45ed-8c2a-d312d8be175c',
        'd8a9265a-d47b-4077-9c76-6a39e848bdf2',
        'efce9417-b6b6-46c8-a0a2-59eee082789f',
        'https://dev-api.logpickr.com',
        'https://dev-auth.logpickr.com',
        10) AS predictions
    FROM s1 EMIT CHANGES;
```

And we can display the final result with :

``` 
SELECT * FROM s2 EMIT CHANGES;
```

## Further Information

If a prediction is not yet ready to be retrieved, the **tryNumber** parameter allows to define a maximum number of tries to retrieve it before throwing an error (the time between two tries is defined via the tryIntervalInMilliseconds variable in the PredictionConstants object)

Moreover, it is important to note that the following parameters in the returned Struct are optional and consequently may have a value equal to null in ksqlDB (if they have no value) : 

* NEXT_STEP
* ESTIMATED_END_OF_CASE
* END_OF_CASE_CONFIDENCE_INTERVAL
* START_CONFIDENCE_INTERVAL
* END_CONFIDENCE_INTERVAL

    
    

    
    
