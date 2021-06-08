# UDF

Projet regroupant les différentes UDF Logpickr qu'il est ensuite possible d'utiliser dans ksqlDB

Pour déployer les UDFs voir le **CONTRIBUTING.md** 

Liens vers la documentation des UDFs ksqlDB :

* https://docs.ksqldb.io/en/latest/reference/user-defined-functions/
* https://docs.ksqldb.io/en/latest/how-to-guides/create-a-user-defined-function/

## UDF Transposition

* nom dans ksqlDB : **logpickr_transposition**
* package : **com.logpickr.ksql.functions.transposition.domain**

UDTF transposant une ligne contenant plusieurs dates associées à des activités, en plusieurs lignes avec une date (ou période) liée à une activité

## UDF Case Events

* nom dans ksqlDB : **logpickr_case_events**
* package : **com.logpickr.ksql.functions.caseevents.domain**

UDF permettant d'obtenir des informations liées à un caseId dans Druid

## UDF Sessions

* nom dans ksqlDB : **logpickr_sessions**
* package : **com.logpickr.ksql.functions.sessions.domain**

UDTF répartissant un ensemble de lignes en différentes sessions

## UDF Prediction

* name in ksqlDB : **logpickr_prediction**
* package : **com.logpickr.ksql.functions.prediction.adapters.api**
  
UDF allowing retrieving prediction information on given caseIds belonging to a Logpickr project
