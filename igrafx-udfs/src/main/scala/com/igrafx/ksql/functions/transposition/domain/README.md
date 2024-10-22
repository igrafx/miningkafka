# UDF Transposition

This UDF of type *Tabular* allows transposing data from ksqlDB. Moreover, the UDF offers 2 variations.
To get information about the UDF directly in kqslDB, use the following command :

```
DESCRIBE FUNCTION IGRAFX_TRANSPOSITION;
```

Start by using the following command to apply modifications of a STREAM on data inserted before the creation/display of the STREAM :

``` 
SET 'auto.offset.reset'='earliest';
```

## Variation 1

**UDF Signature :**

```
igrafxTransposition(input: util.List[Struct]): util.List[Struct]
```

Both the input and output structures follow the format :

``` 
"STRUCT<TASK VARCHAR(STRING), TIME VARCHAR(STRING)>"
```

This variation aims to *explode* the columns of a row into multiple rows having for columns their **Task** and **Timestamp** 

For instance, if we have the following data in input :

| Case   | Step 1     | Step 2     | Step 3     | Step 4     | Step 5                                                                                      |
| -------|:----------:| :---------:|:----------:|:----------:|-----------:|
| case 1 | 12/01/2020 | 14/01/2020 | 15/01/2020 | 16/01/2020 | 17/01/2020 |
| case 2 | 14/02/2020 | 15/02/2020 | 18/02/2020 |            | 18/02/2020 |
| case 3 | 17/03/2020 |            | 24/03/2020 |            |            |

We expect the following output :

| Case   | Task     | Timestamp                                                                                          |
| -------|:--------:| :---------:|
| case 1 | Step 1   | 12/01/2020 |
| case 1 | Step 2   | 14/01/2020 |
| case 1 | Step 3   | 15/01/2020 |
| case 1 | Step 4   | 16/01/2020 |
| case 1 | Step 5   | 17/01/2020 |
| case 2 | Step 1   | 14/02/2020 |
| case 2 | Step 2   | 15/02/2020 |
| case 2 | Step 3   | 18/02/2020 |
| case 2 | Step 5   | 18/02/2020 |
| case 3 | Step 1   | 17/03/2020 |
| case 3 | Step 3   | 24/03/2020 |

Nevertheless, the UDF doesn't allow to directly go from the first table to the second one, and we need to use intermediate ksqlDB STREAMs

The UDF takes in input a **Java List** of type **STRUCT<TASK VARCHAR(STRING), TIME VARCHAR(STRING)>**, where the list corresponds to one row, and where each element in the list corresponds to one Column in the row (*for instance the first element of the List which corresponds to the first row of the former example is : **STRUCT<Step 1, 12/01/2020>***)

Here, the type of the output is identical to the type of the input because it is the type of the UDF (**Tabular**), which tells to ksqlDB to create a row for each element present in the output List.  
The first variation just removes from the input List the elements with a **TIME** field empty or null

To test this variation directly in ksqlDB, we just need to launch ksqlDB as described in the CONTRIBUTING.md located at the root of the project. Then we can use the following commands :

* Creation of the initial STREAM representing the rows with their columns, each row following the format : Case | Step 1 | Step 2 | Step 3 | Step 4 | Step 5

```
CREATE STREAM s1 (
	case VARCHAR,
	step1 VARCHAR,
	step2 VARCHAR,
	step3 VARCHAR,
	step4 VARCHAR,
	step5 VARCHAR
) WITH (
        kafka_topic = 's1',
	partitions = 1,
	value_format = 'avro'
);
```

* Creation of the STREAM preparing the call to the UDF, here an ARRAY<STRUCT(...), STRUCT(...), ...> corresponds to the UDF **input** parameter (ARRAY being the ksqlDB type corresponding to the Java util.List)

```
CREATE STREAM s2 AS SELECT 
    case, 
    ARRAY[STRUCT(task := 'Step 1', time := step1), 
          STRUCT(task := 'Step 2', time := step2), 
          STRUCT(task := 'Step 3', time := step3), 
          STRUCT(task := 'Step 4', time := step4), 
          STRUCT(task := 'Step 5', time := step5)] AS steps
    FROM s1 EMIT CHANGES;
```

* Creation of the STREAM doing the **call to the igrafx_transposition UDF**

```
CREATE STREAM s3 AS SELECT 
    case, 
    igrafx_transposition(steps) AS steps 
    FROM s2 EMIT CHANGES;
```

With the previous example, the s3 STREAM contains the following data :

| case   | steps                                                                                              |
| ------|:---------------------------:|
| case 1 | STRUCT<Step 1, 12/01/2020> | 
| case 1 | STRUCT<Step 2, 14/01/2020> | 
| case 1 | STRUCT<Step 3, 15/01/2020> | 
| case 1 | STRUCT<Step 4, 16/01/2020> | 
| case 1 | STRUCT<Step 5, 17/01/2020> | 
| case 2 | STRUCT<Step 1, 14/02/2020> | 
| case 2 | STRUCT<Step 2, 15/02/2020> | 
| case 2 | STRUCT<Step 3, 18/02/2020> | 
| case 2 | STRUCT<Step 5, 18/02/2020> | 
| case 3 | STRUCT<Step 1, 17/03/2020> | 
| case 3 | STRUCT<Step 3, 24/03/2020> | 

* Creation of the final STREAM which deconstructs the STRUCT from **s3** into two different columns

``` 
CREATE STREAM s4 AS SELECT 
    case, 
    steps->task AS activity, 
    steps->time AS timestamp 
    FROM s3 EMIT CHANGES;
```

Once all the STREAMs have been created, it is possible to add the data, and here each INSERT corresponds to a row

```
INSERT INTO s1 (case, step1, step2, step3, step4, step5) VALUES ('case 1', '12/01/2020', '14/01/2020', '15/01/2020', '16/01/2020', '17/01/2020');
INSERT INTO s1 (case, step1, step2, step3, step4, step5) VALUES ('case 2', '14/02/2020', '15/02/2020', '18/02/2020', '', '18/02/2020');
INSERT INTO s1 (case, step1, step2, step3, step4, step5) VALUES ('case 3', '17/03/2020', '', '24/03/2020', '', '');

INSERT INTO s1 (case, step1, step2, step3, step4, step5) VALUES ('case 4', '17/03/2020', '25/03/2020', '', '16/03/2020', '24/03/2020');
INSERT INTO s1 (case, step1, step2, step3, step4, step5) VALUES ('case 5', '', '', '', '16/03/2020', '');
INSERT INTO s1 (case, step1, step2, step3, step4, step5) VALUES ('case 6', '', '', '', '', '');

INSERT INTO s1 (case, step1, step2, step3, step4, step5) VALUES ('case 7', '17/03/2020', '17/03/2020', '17/03/2020', '17/03/2020', '17/03/2020');
INSERT INTO s1 (case, step1, step2, step3, step4, step5) VALUES ('cas e8', '17/03/2020', '16/03/2020', '17/03/2020', '18/03/2020', '17/03/2020');
```

Eventually, it is possible to display the final result in ksqlDB with :

```
SELECT case, activity, timestamp FROM s4 EMIT CHANGES;
```

## Variation 2

**UDF Signature**

```
igrafxTransposition(input: util.List[Struct], dateFormat: String, isStartInformation: Boolean, isTaskNameAscending: Boolean): util.List[Struct]
```

The input structure is :

``` 
"STRUCT<TASK VARCHAR(STRING), TIME VARCHAR(STRING)>"
```

The output structure is :

``` 
"STRUCT<TASK VARCHAR(STRING), START VARCHAR(STRING), STOP VARCHAR(STRING)>"
``` 

This function aims to **explode** a row with multiple columns into multiple rows with 4 columns each (for the *case*, the **activity**, the **starting date**, and the **ending date**)

For the parameters :

* **input** : corresponds as for the first variation to the input row we want to **explode**
* **dateFormat** : corresponds to the date format (for instance : for an activity having for date 12/01/2020, the date format is "dd/MM/yyyy" )
* **isStartInformation** : **true** indicates that the date associated to the activity corresponds to the beginning of the activity, and that we hence need to calculate the end of the activity. **false** indicates that the date corresponds to the end of the activity meaning we have to calculate its start date (calculations are made when possible in function of the dates of the other activities)
* **isTaskNameAscending** : **true** indicates that in case of identical dates for two (or more) rows, the order of the rows is determined in an ascending manner according to the activity's name, while **false** means that the order is determined in a descending manner according to the activity's name

If we take this case in input :

| Case   | Step 1     | Step 2     | Step 3     | Step 4     | Step 5                                                                                     |
| -------|:----------:| :---------:|:----------:|:----------:|-----------:|
| case 1 | 17/03/2020 | 16/03/2020 | 17/03/2020 | 18/03/2020 | 17/03/2020 |

We get the following output if **isStartInformation = true** and **isTaskNameAscending = true** :

| Case   | Activity   | Start      | End                                                                                        |
| ------|:-----------:| :---------:| :---------: |
| case 1 | Step 2     | 16/03/2020 | 17/03/2020  |
| case 1 | Step 1     | 17/03/2020 | 17/03/2020  |
| case 1 | Step 3     | 17/03/2020 | 17/03/2020  |
| case 1 | Step 5     | 17/03/2020 | 18/03/2020  |
| case 1 | Step 4     | 18/03/2020 | 18/03/2020  |

We get the following output if **isStartInformation = true** and **isTaskNameAscending = false** :

| Case   | Activity   | Start      | End                                                                                              |
| ------|:-----------:| :---------:| :---------: |
| case 1 | Step 2     | 16/03/2020 | 17/03/2020  |
| case 1 | Step 5     | 17/03/2020 | 17/03/2020  |
| case 1 | Step 3     | 17/03/2020 | 17/03/2020  |
| case 1 | Step 1     | 17/03/2020 | 18/03/2020  |
| case 1 | Step 4     | 18/03/2020 | 18/03/2020  |

We get the following output if **isStartInformation = false** and **isTaskNameAscending = true** :

| Case   | Activity   | Start      | End                                                                                              |
| ------|:-----------:| :---------:| :---------: |
| case 1 | Step 2     | 16/03/2020 | 16/03/2020  |
| case 1 | Step 1     | 16/03/2020 | 17/03/2020  |
| case 1 | Step 3     | 17/03/2020 | 17/03/2020  |
| case 1 | Step 5     | 17/03/2020 | 17/03/2020  |
| case 1 | Step 4     | 17/03/2020 | 18/03/2020  |

We get the following output if si **isStartInformation = false** and **isTaskNameAscending = false** :

| Case   | Activity   | Start      | End                                                                                               |
| ------|:-----------:| :---------:| :---------: |
| case 1 | Step 2     | 16/03/2020 | 16/03/2020  |
| case 1 | Step 5     | 16/03/2020 | 17/03/2020  |
| case 1 | Step 3     | 17/03/2020 | 17/03/2020  |
| case 1 | Step 1     | 17/03/2020 | 17/03/2020  |
| case 1 | Step 4     | 17/03/2020 | 18/03/2020  |

To test this variation directly in ksqlDB, we first need to launch ksqlDB as described in the CONTRIBUTING.md file located at the root of the project. Then, write the following commands :

(The first two STREAMs are the same as the ones in the first variation)
* Creation of the initial STREAM representing the rows with their columns, each row following the format : Case | Step 1 | Step 2 | Step 3 | Step 4 | Step 5

```
CREATE STREAM s1 (
	case VARCHAR,
	step1 VARCHAR,
	step2 VARCHAR,
	step3 VARCHAR,
	step4 VARCHAR,
	step5 VARCHAR
) WITH (
        kafka_topic = 's1',
	partitions = 1,
	value_format = 'avro'
);
```

* Creation of the STREAM preparing the call to the UDF, here an ARRAY<STRUCT(...), STRUCT(...), ...> corresponds to the UDF **input** parameter (ARRAY being the ksqlDB type corresponding to the Java util.List)

```
CREATE STREAM s2 AS SELECT 
    case, 
    ARRAY[STRUCT(task := 'Step 1', time := step1), 
          STRUCT(task := 'Step 2', time := step2), 
          STRUCT(task := 'Step 3', time := step3), 
          STRUCT(task := 'Step 4', time := step4), 
          STRUCT(task := 'Step 5', time := step5)] AS steps
    FROM s1 EMIT CHANGES;
```

* Creation of the STREAM doing the **call to the igrafx_transposition UDF function**

```
CREATE STREAM s3 AS SELECT 
    case, 
    igrafx_transposition(steps, "dd/MM/yyyy", true, true) AS steps 
    FROM s2 EMIT CHANGES;
```
or 
```
CREATE STREAM s3 AS SELECT 
    case, 
    igrafx_transposition(steps, "dd/MM/yyyy", true, false) AS steps 
    FROM s2 EMIT CHANGES;
```
or
```
CREATE STREAM s3 AS SELECT 
    case, 
    igrafx_transposition(steps, "dd/MM/yyyy", false, true) AS steps 
    FROM s2 EMIT CHANGES;
```
or
```
CREATE STREAM s3 AS SELECT 
    case, 
    igrafx_transposition(steps, "dd/MM/yyyy", false, false) AS steps 
    FROM s2 EMIT CHANGES;
```

Creation of the final STREAM which deconstructs the STRUCT from **s3** into 4 different columns

``` 
CREATE STREAM s4 AS SELECT 
    case, 
    steps->task AS activity, 
    steps->start AS start_date,
    steps->stop AS end_date 
    FROM s3 EMIT CHANGES;
```

Once all the STREAMs have been created, it is possible to add the data, and here each INSERT corresponds to a row

```
INSERT INTO s1 (case, step1, step2, step3, step4, step5) VALUES ('case 1', '12/01/2020', '14/01/2020', '15/01/2020', '16/01/2020', '17/01/2020');
INSERT INTO s1 (case, step1, step2, step3, step4, step5) VALUES ('case 2', '14/02/2020', '15/02/2020', '18/02/2020', '', '18/02/2020');
INSERT INTO s1 (case, step1, step2, step3, step4, step5) VALUES ('case 3', '17/03/2020', '', '24/03/2020', '', '');

INSERT INTO s1 (case, step1, step2, step3, step4, step5) VALUES ('case 4', '17/03/2020', '25/03/2020', '', '16/03/2020', '24/03/2020');
INSERT INTO s1 (case, step1, step2, step3, step4, step5) VALUES ('case 5', '', '', '', '16/03/2020', '');
INSERT INTO s1 (case, step1, step2, step3, step4, step5) VALUES ('case 6', '', '', '', '', '');

INSERT INTO s1 (case, step1, step2, step3, step4, step5) VALUES ('case 7', '17/03/2020', '17/03/2020', '17/03/2020', '17/03/2020', '17/03/2020');
INSERT INTO s1 (case, step1, step2, step3, step4, step5) VALUES ('case 8', '17/03/2020', '16/03/2020', '17/03/2020', '18/03/2020', '17/03/2020');
```

It is now possible to display the final result with :

```
SELECT cas, activite, debut, fin FROM s4 EMIT CHANGES;
```


## Further Information :

Concerning the behavior of the UDF, and this for both variation, there is a need to be careful about the additional columns in the initial row. 

For instance if the initial row corresponds to :

| Case   | Step 1     | Step 2     | Step 3     | Step 4     | Step 5     | Total Price    |
| -------|:----------:| :---------:|:----------:|:----------:|:----------:|----------------|
| case 1 | 12/01/2020 | 14/01/2020 | 15/01/2020 | 16/01/2020 | 17/01/2020 | 240            |

An information is given about the Total Price related to the process. In case of utilisation of the UDF (for instance with the first variation), if the created STREAMs keep the information of the Total Price column, as with the following example :

```
CREATE STREAM s1 (
	case VARCHAR,
	step1 VARCHAR,
	step2 VARCHAR,
	step3 VARCHAR,
	step4 VARCHAR,
	step5 VARCHAR,
	price INTEGER
) WITH (
        kafka_topic = 's1',
	partitions = 1,
	value_format = 'avro'
);
```

```
CREATE STREAM s2 AS SELECT 
    case, 
    ARRAY[STRUCT(task := 'Step 1', time := step1), 
          STRUCT(task := 'Step 2', time := step2), 
          STRUCT(task := 'Step 3', time := step3), 
          STRUCT(task := 'Step 4', time := step4), 
          STRUCT(task := 'Step 5', time := step5)] AS steps,
    price
    FROM s1 EMIT CHANGES;
```

```
CREATE STREAM s3 AS SELECT 
    case, 
    igrafx_transposition(steps) AS steps,
    price
    FROM s2 EMIT CHANGES;
```

``` 
CREATE STREAM s4 AS SELECT 
    case, 
    steps->task AS activity, 
    steps->time AS timestamp,
    price
    FROM s3 EMIT CHANGES;
```

Then the Total Price is present for each of the created rows. The result for the current example would be :

| Case   | Activity   | Timestamp  | Price                                                                                        |
| ------|:-----------:| :---------:| :---------: |
| case 1 | Step 1     | 12/01/2020 | 240         |
| case 1 | Step 2     | 14/01/2020 | 240         |
| case 1 | Step 3     | 15/01/2020 | 240         |
| case 1 | Step 4     | 16/01/2020 | 240         |
| case 1 | Step 5     | 17/01/2020 | 240         |

This can be problematic if for instance we then want to sum all the values in the Price column to determine the total price of all the process, because the result wouldn't take the real value for the total price of each process (for instance, for the process presented here, the calculated value would be 5x240, whereas the real total price of the process is 240)