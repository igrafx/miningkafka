# UDF requête Druid

Pour avoir des informations sur cette UDF dans ksqlDB, tapper la commande :

``` 
DESCRIBE FUNCTION LOGPICKR_CASE_EVENTS;
```

Cette UDF permet de récupérer depuis la Datasource _vertex de Druid les informations des colonnes suivantes liées à un caseId :

* __time
* enddate
* vertex_name

Signature de l'UDF : 

``` 
def logpickrCaseEvents(caseId: String, projectId: String, workgroupId: String, workgroupKey: String, host: String, port: String): util.List[Struct]
```

où la Struct en retour a le format : 

``` 
STRUCT<START_DATE VARCHAR(STRING), END_DATE VARCHAR(STRING), VERTEX_NAME VARCHAR(STRING)>
```

Avec **START_DATE** qui correspond à la colonne **__time**, **END_DATE** qui correspond à la colonne **enddate** et enfin **VERTEX_NAME** qui correspond à la colonne **vertex_name**

Néanmoins c'est bien un ARRAY de cette structure qui est retourné, pour récupérer les informations de chaque ligne associée au caseId

La requête SQL réalisée dans l'UDF est :

``` 
SELECT __time AS startdate, enddate, vertex_name AS vertexName
FROM "projectId_vertex"
WHERE vertex_name is not NULL AND caseid = 'caseIdParam'
```
où **projectId** correspond à l'id du projet Logpickr et **caseIdParam** correspond au caseId passé en paramètre de l'UDF

## Paramètres :

* **caseId** : correspond au caseId dont on veut connaître les informations
* **projectId** : correspond à l'id du projet Logpickr
* **workgroupId** : correspond à l'id du workgroup Logpickr
* **workgroupKey** : correspond à la clé du workgroup Logpickr
* **host** : correspond à l'hôte Druid
* **port** : correspond au port de connexion Druid

## Exemple d'utilisation dans ksqlDB :

* Commande permettant de prendre en compte les données présentes dans le STREAM source avant la création/affichage d'un nouveau STREAM :

``` 
SET 'auto.offset.reset'='earliest';
```

* Création du STREAM initial comportant les caseId dont on veut obtenir des informations :

```
CREATE STREAM s1 (
  caseId VARCHAR
  ) WITH (
  kafka_topic = 's1',
  partitions = 1,
  value_format = 'avro'
  );
```

* Création du STREAM réalisant l'appel à l'UDF (les paramètres de l'UDF sont données ici à titre d'exemple et peuvent être modifiés, sauf pour le caseId, qui doit correspondre à la colonne caseId du STREAM créé précédemment) :

``` 
CREATE STREAM s2 AS SELECT 
    caseId, 
    logpickr_case_events(caseId, '8968a61c-1fb5-4721-9a8b-d09198eef06b', 'druid_system', 'lpkAuth', 'dev.logpickr.com', '8082') AS informations 
    FROM s1 EMIT CHANGES;
```

* Insertion d'un caseId dont on veut connaître les informations (pour récupérer des informations il faut que ce caseId existe dans le projet Logpickr) :

``` 
INSERT INTO s1 (caseId) VALUES ('3');
```

* Affichage du résultat :

```` 
SELECT caseId, informations FROM s2 EMIT CHANGES;
````