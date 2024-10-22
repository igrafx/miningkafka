# UDF

Project regrouping the different iGrafx UDF you can use in ksqlDB

To deploy UDFs, please see the **CONTRIBUTING.md** file

Links towards the ksqlDB UDFs documentation :

* https://docs.ksqldb.io/en/latest/reference/user-defined-functions/
* https://docs.ksqldb.io/en/latest/how-to-guides/create-a-user-defined-function/

## UDF Transposition

* name in ksqlDB : **igrafx_transposition**
* package : **com.igrafx.ksql.functions.transposition.domain**

UDTF transposing a line which contains multiple dates associated to activities, into multiple lines with one date (or period) linked to its activity. Read more [here](src/main/scala/com/igrafx/ksql/functions/transposition/domain/README.md).

## UDF Case Events

* name in ksqlDB : **igrafx_case_events**
* package : **com.igrafx.ksql.functions.caseevents.domain**

UDF allowing to obtain caseId related information in Druid. Read more [here](src/main/scala/com/igrafx/ksql/functions/caseevents/domain/README.md).

## UDF Sessions

* name in ksqlDB : **igrafx_sessions**
* package : **com.igrafx.ksql.functions.sessions.domain**

UDTF distributing a collection of lines into different sessions. Read more [here](src/main/scala/com/igrafx/ksql/functions/sessions/domain/README.md).

## UDF Prediction

* name in ksqlDB : **igrafx_prediction**
* package : **com.igrafx.ksql.functions.prediction.adapters.api**
  
UDF allowing retrieving prediction information on given caseIds belonging to a iGrafx project. Read more [here](src/main/scala/com/igrafx/ksql/functions/prediction/README.md).
