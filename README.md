# UDF

Project regrouping the different Logpickr UDF you can use in ksqlDB

To deploy UDFs, please see the **CONTRIBUTING.md** file

Links towards the ksqlDB UDFs documentation :

* https://docs.ksqldb.io/en/latest/reference/user-defined-functions/
* https://docs.ksqldb.io/en/latest/how-to-guides/create-a-user-defined-function/

## UDF Transposition

* name in ksqlDB : **logpickr_transposition**
* package : **com.logpickr.ksql.functions.transposition.domain**

UDTF transposing a line which contains multiple dates associated to activities, into multiple lines with one date (or period) linked to its activity

## UDF Case Events

* name in ksqlDB : **logpickr_case_events**
* package : **com.logpickr.ksql.functions.caseevents.domain**

UDF allowing to obtain caseId related information in Druid

## UDF Sessions

* name in ksqlDB : **logpickr_sessions**
* package : **com.logpickr.ksql.functions.sessions.domain**

UDTF distributing a collection of lines into different sessions

## UDF Prediction

* name in ksqlDB : **logpickr_prediction**
* package : **com.logpickr.ksql.functions.prediction.adapters.api**
  
UDF allowing retrieving prediction information on given caseIds belonging to a Logpickr project
