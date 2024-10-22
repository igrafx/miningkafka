# UDF Sessions

This UDF of type *Tabular* allows to divide a collection of ordered lines into sessions (a session has its own ID and corresponds to a regroupment of lines sharing a common point). To create a session, Regex are used in order to describe the lines belonging to a same group, the session's starting lines, the session's ending lines, and the lines that need to be ignored.

To retrieve information about this UDF directly in ksqlDB, use the following command :

```
DESCRIBE FUNCTION IGRAFX_SESSIONS;
```

**UDF Signature :**

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

Here, we take in input a collection of rows (where each row has some columns). Then we regroup the rows per groups (in function of the values of the columns specified by **groupSessionPattern**, where two rows having the same value for the specified columns will end up in the same group). And finally within each group we calculate sessions for some consecutive rows. To calculate the sessions, some rows correspond to the start of the sessions, others to the end, and rows in between just belong to the session (a row corresponds to a start/end of a session according to whether or not the row matches the startSessionPattern/endSessionPattern defined by the user).
Hence, groups are used to separate and reorganize the rows of the input collection. Each session is only calculated from the rows of a same group and correspond to a gathering of related events, with limits defined by the user.

Additionaly, there are options to choose if we want to hash the sessionId created for each new session, and to choose if we ignore the sessions not having one row matching the descriptions given either for the start of a session or for the end of a session 

The Structure used in the function's return :

``` 
STRUCT<SESSION_ID VARCHAR(STRING), LINE VARCHAR(STRING)>
```

The **LINE** field corresponds to a line from the initial input collection, and the **SESSION_ID** field corresponds to the ID of the session associated to the line

## Parameters :

* **inputLines** : Corresponds to the initial collection of rows
* **ignorePattern** : Regex describing the rows to ignore. Rows verifying this pattern won't be used for the sessions creation and won't be returned by the function
* **groupSessionPattern** : Regex allowing to regroup lines having the same values for the specified columns. The session will be determined within these groups. For instance for lines with the following format :
  **timeStamp;userID;targetApp;eventType**  
  and for the following pattern :   
  **".\*;(.\*);.\*;(.\*)"**  
  The group of a row will be determined by concatenating its userId and eventType columns values (because those columns are into brackets in the Regex)
* **startSessionPattern** : Regex describing the lines that can be considered as a Start of a session
* **endSessionPattern** : Regex describing the lines that can be considered as End of a session
* **sessionIdPattern** : Regex informing about the parts of the lines that will be used to create the sessionId. For instance for lines with the following format :
  **timeStamp;userID;targetApp;eventType**  
  and for the following pattern :   
  **".\*;(.\*);(.\*);.\*"**  
  The sessionID will be created by concatenating the userId and targetApp columns (which are into brackets in the Regex)
* **isSessionIdHash** : A sessionId is created according to the columns specified in the **sessionIdPattern** parameter. If **isSessionIdHash** is **false**, then the sessionId will only correspond to the concatenation of the values of the columns specified in **sessionIdPattern**. But if **isSessionIdHash** is **true**, the result of this concatenation is hashed to create the sessionId. The Hash function used is **MD5**
* **isIgnoreIfNoStart** : Boolean indicating if sessions that don't have a line matching the **startSessionPattern** are kept or not. If **true**, the corresponding sessions are not returned. If **false**, they are returned 
* **isIgnoreIfNoEnd** : Boolean indicating if sessions that don't have a line matching the **endSessionPattern** are kept or not. If **true**, the corresponding sessions are not returned. If **false**, they are returned

For more information about Regex follow this link : https://medium.com/factory-mind/regex-tutorial-a-simple-cheatsheet-by-examples-649dc1c3f285

## Examples in ksqlDB

To follow those examples, start by using the following command to apply modifications of a STREAM on data inserted before the creation/display of the STREAM :

``` 
SET 'auto.offset.reset'='earliest';
```

We consider the following collection of rows : 

| timeStamp     | userID | targetApp |  eventType  |
|---------------|:------:|:---------:|:-----------:|
| 2020-06-16T04 |   1    |  appli1   |    Start    | 
| 2020-06-16T04 |   1    |  appli1   |   event1    | 
| 2020-06-16T04 |   1    |  appli1   |   event2    |
| 2020-06-16T04 |   2    |  appli1   |    Start    | 
| 2020-06-16T04 |   2    |  appli1   |   event4    | 
| 2020-06-16T04 |   2    |  appli2   |    Start    | 
| 2020-06-16T04 |   2    |  appli3   |   event5    | 
| 2020-06-16T04 |   1    |  appli1   |   event3    | 
| 2020-06-16T04 |   1    |  appli1   | ignoreEvent | 
| 2020-06-16T04 |   1    |  appli1   |     End     | 
| 2020-06-16T04 |   1    |  appli2   | aloneEvent1 | 
| 2020-06-16T04 |   1    |  appli2   | aloneEvent2 | 
| 2020-06-16T04 |   2    |  appli2   |   event6    | 
| 2020-06-16T04 |   2    |  appli2   |     End     |
| 2020-06-16T04 |   2    |  appli2   |   event7    | 
| 2020-06-16T04 |   2    |  appli3   |     End     |


The first STREAM to create in ksqlDB is :

``` 
CREATE STREAM s1 (
	lines ARRAY<VARCHAR>
) WITH (
	kafka_topic = 's1',
	partitions = 1,
	value_format = 'avro'
);
```

where each row is present in the **lines** array 

It is then possible to insert data :

``` 
INSERT INTO s1 (lines) VALUES (ARRAY[
    '2020-06-16T04;1;appli1;Start', 
    '2020-06-16T04;1;appli1;event1', 
    '2020-06-16T04;1;appli1;event2', 
    '2020-06-16T04;2;appli1;Start', 
    '2020-06-16T04;2;appli1;event4', 
    '2020-06-16T04;2;appli2;Start', 
    '2020-06-16T04;2;appli3;event5',
    '2020-06-16T04;1;appli1;event3', 
    '2020-06-16T04;1;appli1;ignoreEvent', 
    '2020-06-16T04;1;appli1;End', 
    '2020-06-16T04;1;appli2;aloneEvent1', 
    '2020-06-16T04;1;appli2;aloneEvent2',  
    '2020-06-16T04;2;appli2;event6', 
    '2020-06-16T04;2;appli2;End' , 
    '2020-06-16T04;2;appli2;event7', 
    '2020-06-16T04;2;appli3;End']);
```

And everything is ready to call the UDF. 

In these examples, the rows verifying :

* **ignorePattern** = '.\*;.\*;.*;ignoreEvent' are ignored
* **startSessionPattern** = '.\*;.\*;.*;Start' correspond to the start of a new session
* **endSessionPattern** = '.\*;.\*;.*;End' correspond to the end of the current session

Furthermore, the rows follow the format : **timeStamp;userID;targetApp;eventType**

The group pattern used in the examples is :

* **groupSessionPattern** = '.\*;(.\*);.\*;.*' meaning that rows are divided into group according to the value of the userID column

And the pattern used to create the sessionId of a session starting row is :

* **sessionIdPattern** = '.\*;(.\*);(.\*);.*' meaning that for a session starting row the sessionId will be calculated according to the values of the userID and targetApp columns 

Moreover, in function of the **isSessionIdHash** value, the following sessionId1/sessionId2/sessionId3/sessionId4 will correspond either to the concatenation of the userId and targetApp columns, or to the hashed value of the concatenation of the userId and targetApp columns (those columns because they are described by **sessionIdPattern** = '.*;(.*);(.*);.*')

Numerous combinations for the **igrafx_sessions** functions are possible :

* **isIgnoreIfNoStart = true** and **isIgnoreIfNoEnd = true**

``` 
CREATE STREAM s2 AS SELECT 
    igrafx_sessions(lines, '.*;.*;.*;ignoreEvent', '.*;(.*);.*;.*', '.*;.*;.*;Start', '.*;.*;.*;End', '.*;(.*);(.*);.*', true, true, true) AS sessions 
    FROM s1 EMIT CHANGES;
    
CREATE STREAM s3 AS SELECT 
    sessions->session_id AS session_id, 
    sessions->line AS session_line 
    FROM s2 EMIT CHANGES;
    
SELECT session_id, session_line FROM s3 EMIT CHANGES;
```

The awaited result is then :

| session_id |         session_line          |
|------------|:-----------------------------:|
| sessionId1 | 2020-06-16T04;1;appli1;Start  | 
| sessionId1 | 2020-06-16T04;1;appli1;event1 | 
| sessionId1 | 2020-06-16T04;1;appli1;event2 | 
| sessionId1 | 2020-06-16T04;1;appli1;event3 |
| sessionId1 |  2020-06-16T04;1;appli1;End   |
| sessionId2 | 2020-06-16T04;2;appli2;Start  | 
| sessionId2 | 2020-06-16T04;2;appli3;event5 | 
| sessionId2 | 2020-06-16T04;2;appli2;event6 | 
| sessionId2 |  2020-06-16T04;2;appli2;End   |

* **isIgnoreIfNoStart = false** and **isIgnoreIfNoEnd = true**

``` 
CREATE STREAM s4 AS SELECT 
    igrafx_sessions(lines, '.*;.*;.*;ignoreEvent', '.*;(.*);.*;.*', '.*;.*;.*;Start', '.*;.*;.*;End', '.*;(.*);(.*);.*', true, false, true) AS sessions 
    FROM s1 EMIT CHANGES;
    
CREATE STREAM s5 AS SELECT 
    sessions->session_id AS session_id, 
    sessions->line AS session_line 
    FROM s4 EMIT CHANGES;
    
SELECT session_id, session_line FROM s5 EMIT CHANGES;
```

The awaited result is then :

| session_id |         session_line          |
|------------|:-----------------------------:|
| sessionId1 | 2020-06-16T04;1;appli1;Start  | 
| sessionId1 | 2020-06-16T04;1;appli1;event1 | 
| sessionId1 | 2020-06-16T04;1;appli1;event2 | 
| sessionId1 | 2020-06-16T04;1;appli1;event3 |
| sessionId1 |  2020-06-16T04;1;appli1;End   |
| sessionId2 | 2020-06-16T04;2;appli2;Start  | 
| sessionId2 | 2020-06-16T04;2;appli3;event5 | 
| sessionId2 | 2020-06-16T04;2;appli2;event6 | 
| sessionId2 |  2020-06-16T04;2;appli2;End   |
| sessionId2 | 2020-06-16T04;2;appli2;event7 | 
| sessionId2 |  2020-06-16T04;2;appli3;End   |

* **isIgnoreIfNoStart = true** and **isIgnoreIfNoEnd = false**

``` 
CREATE STREAM s6 AS SELECT 
    igrafx_sessions(lines, '.*;.*;.*;ignoreEvent', '.*;(.*);.*;.*', '.*;.*;.*;Start', '.*;.*;.*;End', '.*;(.*);(.*);.*', true, true, false) AS sessions 
    FROM s1 EMIT CHANGES;
    
CREATE STREAM s7 AS SELECT 
    sessions->session_id AS session_id, 
    sessions->line AS session_line 
    FROM s6 EMIT CHANGES;
    
SELECT session_id, session_line FROM s7 EMIT CHANGES;
```

The awaited result is then :

| session_id |         session_line          |
|------------|:-----------------------------:|
| sessionId1 | 2020-06-16T04;1;appli1;Start  | 
| sessionId1 | 2020-06-16T04;1;appli1;event1 | 
| sessionId1 | 2020-06-16T04;1;appli1;event2 | 
| sessionId1 | 2020-06-16T04;1;appli1;event3 |
| sessionId1 |  2020-06-16T04;1;appli1;End   |
| sessionId2 | 2020-06-16T04;2;appli1;Start  | 
| sessionId2 | 2020-06-16T04;2;appli1;event4 | 
| sessionId3 | 2020-06-16T04;2;appli2;Start  | 
| sessionId3 | 2020-06-16T04;2;appli3;event5 | 
| sessionId3 | 2020-06-16T04;2;appli2;event6 | 
| sessionId3 |  2020-06-16T04;2;appli2;End   |

* **isIgnoreIfNoStart = false** and **isIgnoreIfNoEnd = false**

``` 
CREATE STREAM s8 AS SELECT 
    igrafx_sessions(lines, '.*;.*;.*;ignoreEvent', '.*;(.*);.*;.*', '.*;.*;.*;Start', '.*;.*;.*;End', '.*;(.*);(.*);.*', true, false, false) AS sessions 
    FROM s1 EMIT CHANGES;
    
CREATE STREAM s9 AS SELECT 
    sessions->session_id AS session_id, 
    sessions->line AS session_line 
    FROM s8 EMIT CHANGES;
    
SELECT session_id, session_line FROM s9 EMIT CHANGES;
```

| session_id |            session_line            |
|------------|:----------------------------------:|
| sessionId1 |    2020-06-16T04;1;appli1;Start    | 
| sessionId1 |   2020-06-16T04;1;appli1;event1    | 
| sessionId1 |   2020-06-16T04;1;appli1;event2    | 
| sessionId1 |   2020-06-16T04;1;appli1;event3    |
| sessionId1 |     2020-06-16T04;1;appli1;End     |
| sessionId2 | 2020-06-16T04;1;appli2;aloneEvent1 |
| sessionId2 | 2020-06-16T04;1;appli2;aloneEvent2 |
| sessionId3 |    2020-06-16T04;2;appli1;Start    | 
| sessionId3 |   2020-06-16T04;2;appli1;event4    | 
| sessionId4 |    2020-06-16T04;2;appli2;Start    | 
| sessionId4 |   2020-06-16T04;2;appli3;event5    | 
| sessionId4 |   2020-06-16T04;2;appli2;event6    | 
| sessionId4 |     2020-06-16T04;2;appli2;End     |
| sessionId4 |   2020-06-16T04;2;appli2;event7    | 
| sessionId4 |     2020-06-16T04;2;appli3;End     |

## Further Information

In the case where **startSessionPattern** and **endSessionPattern** are both verified by the same row, the row is considered as the start of a new session and end the previous session. The new session is then kept independently of the **isIgnoreIfNoEnd** value