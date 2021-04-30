# UDF Transposition

Cette UDF de type *Tabular* permet de transposer des données depuis ksqlDB. L'UDF propose par ailleurs deux déclinaisons, et il est possible d'avoir des informations sur l'UDF directement dans ksqlDB avec la commande :

```
DESCRIBE FUNCTION LOGPICKR_TRANSPOSITION;
```

Commencer par rentrer la commande suivante pour appliquer les modifications d'un STREAM sur des données insérées avant la création/l'affichage du STREAM :
``` 
SET 'auto.offset.reset'='earliest';
```

## Déclinaison 1

signature de l'UDF : 
```
logpickrTransposition(input: util.List[Struct]): util.List[Struct]
```

Les structures en entrée et en sortie sont toutes les deux au format :

``` 
"STRUCT<TASK VARCHAR(STRING), TIME VARCHAR(STRING)>"
```

L'objectif de cette déclinaison est simplement de permettre d'*exploser* les colonnes d'une ligne en plusieurs lignes avec comme colonnes leur activité et l'horodatage

Par exemple si l'on a en entrée ces données 

| Cas   | Etape 1    | Etape 2    | Etape 3    | Etape 4    | Etape 5                                                                                     |
| ------|:----------:| :---------:|:----------:|:----------:|-----------:|
| cas 1 | 12/01/2020 | 14/01/2020 | 15/01/2020 | 16/01/2020 | 17/01/2020 |
| cas 2 | 14/02/2020 | 15/02/2020 | 18/02/2020 |            | 18/02/2020 |
| cas 3 | 17/03/2020 |            | 24/03/2020 |            |            |

On va chercher à obtenir en sortie :

| Cas   | Activité    | Horodatage                                                                                          |
| ------|:-----------:| :---------:|
| cas 1 | Etape 1     | 12/01/2020 |
| cas 1 | Etape 2     | 14/01/2020 |
| cas 1 | Etape 3     | 15/01/2020 |
| cas 1 | Etape 4     | 16/01/2020 |
| cas 1 | Etape 5     | 17/01/2020 |
| cas 2 | Etape 1     | 14/02/2020 |
| cas 2 | Etape 2     | 15/02/2020 |
| cas 2 | Etape 3     | 18/02/2020 |
| cas 2 | Etape 5     | 18/02/2020 |
| cas 3 | Etape 1     | 17/03/2020 |
| cas 3 | Etape 3     | 24/03/2020 |

Néanmoins, l'UDF ne permettant pas directement de passer de l'un à l'autre, il faut utiliser en supplément les STREAM de ksqlDB.

L'UDF prend en entrée une liste java de **STRUCT<TASK VARCHAR(STRING), TIME VARCHAR(STRING)>**, où la liste correspond à une ligne, et où chaque élement correspond à une colonne dans la ligne (*par exemple le premier élément de la liste correspondante à la première ligne de l'exemple ci-dessus est : **STRUCT<Etape 1, 12/01/2020>***)

Ici, la sortie est identique à l'entrée car c'est le type de l'UDF, à savoir **Tabular**, qui va permettre à ksqlDB de savoir qu'il faut créer une ligne pour chaque élément présent dans la liste retour. La fonction se contente ici seulement de supprimer de la liste en entrée les élements qui ont un champ **TIME** vide ou null

Pour tester cette déclinaison directement dans ksqlDB, il suffit de lancer ksqlDB comme décrit dans le CONTRIBUTING.md à la racine du projet, puis de lancer les commandes suivantes :

* Création du STREAM initial représentant les lignes avec leurs colonnes au format : Cas | Etape 1 | Etape 2 | Etape 3 | Etape 4 | Etape 5
```
CREATE STREAM s1 (
	cas VARCHAR,
	etape1 VARCHAR,
	etape2 VARCHAR,
	etape3 VARCHAR,
	etape4 VARCHAR,
	etape5 VARCHAR
) WITH (
    	kafka_topic = 's1',
	partitions = 1,
	value_format = 'avro'
);
```

* Création du STREAM permettant l'appel à l'UDF, ici un ARRAY<STRUCT(...), STRUCT(...), ...> correspond au paramètre **input** de l'UDF (ARRAY étant le type de ksqlDB qui fait la correspondance avec util.List de Java)

```
CREATE STREAM s2 AS SELECT 
    cas, 
    ARRAY[STRUCT(task := 'Etape 1', time := etape1), 
          STRUCT(task := 'Etape 2', time := etape2), 
          STRUCT(task := 'Etape 3', time := etape3), 
          STRUCT(task := 'Etape 4', time := etape4), 
          STRUCT(task := 'Etape 5', time := etape5)] AS etapes
    FROM s1 EMIT CHANGES;
```

* Création du STREAM avec l'**appel à l'UDF logpickr_transposition**

```
CREATE STREAM s3 AS SELECT 
    cas, 
    logpickr_transposition(etapes) AS etapes 
    FROM s2 EMIT CHANGES;
```

Avec l'exemple précédent, le STREAM s3 contient les données suivantes :

| cas   | etapes                                                                                              |
| ------|:---------------------------:|
| cas 1 | STRUCT<Etape 1, 12/01/2020> | 
| cas 1 | STRUCT<Etape 2, 14/01/2020> | 
| cas 1 | STRUCT<Etape 3, 15/01/2020> | 
| cas 1 | STRUCT<Etape 4, 16/01/2020> | 
| cas 1 | STRUCT<Etape 5, 17/01/2020> | 
| cas 2 | STRUCT<Etape 1, 14/02/2020> | 
| cas 2 | STRUCT<Etape 2, 15/02/2020> | 
| cas 2 | STRUCT<Etape 3, 18/02/2020> | 
| cas 2 | STRUCT<Etape 5, 18/02/2020> | 
| cas 3 | STRUCT<Etape 1, 17/03/2020> | 
| cas 3 | STRUCT<Etape 3, 24/03/2020> | 

* Création du STREAM final en déconstruisant la STRUCT de **s3** en deux colonnes différentes

``` 
CREATE STREAM s4 AS SELECT 
    cas, 
    etapes->task AS activite, 
    etapes->time AS horodatage 
    FROM s3 EMIT CHANGES;
```

Une fois les STREAMS créés il est possible d'ajouter des données, et dans ce qui suit, chaque INSERT correspond à une ligne

```
INSERT INTO s1 (cas, etape1, etape2, etape3, etape4, etape5) VALUES ('cas1', '12/01/2020', '14/01/2020', '15/01/2020', '16/01/2020', '17/01/2020');
INSERT INTO s1 (cas, etape1, etape2, etape3, etape4, etape5) VALUES ('cas2', '14/02/2020', '15/02/2020', '18/02/2020', '', '18/02/2020');
INSERT INTO s1 (cas, etape1, etape2, etape3, etape4, etape5) VALUES ('cas3', '17/03/2020', '', '24/03/2020', '', '');

INSERT INTO s1 (cas, etape1, etape2, etape3, etape4, etape5) VALUES ('cas4', '17/03/2020', '25/03/2020', '', '16/03/2020', '24/03/2020');
INSERT INTO s1 (cas, etape1, etape2, etape3, etape4, etape5) VALUES ('cas5', '', '', '', '16/03/2020', '');
INSERT INTO s1 (cas, etape1, etape2, etape3, etape4, etape5) VALUES ('cas6', '', '', '', '', '');

INSERT INTO s1 (cas, etape1, etape2, etape3, etape4, etape5) VALUES ('cas7', '17/03/2020', '17/03/2020', '17/03/2020', '17/03/2020', '17/03/2020');
INSERT INTO s1 (cas, etape1, etape2, etape3, etape4, etape5) VALUES ('cas8', '17/03/2020', '16/03/2020', '17/03/2020', '18/03/2020', '17/03/2020');
```

Il est possible d'afficher le résultat final dans ksqlDB avec la commande :

```
SELECT cas, activite, horodatage FROM s4 EMIT CHANGES;
```

## Déclinaison 2

signature de l'UDF :

```
logpickrTransposition(input: util.List[Struct], dateFormat: String, isStartInformation: Boolean, isTaskNameAscending: Boolean): util.List[Struct]
```

La structure en entrée est :

``` 
"STRUCT<TASK VARCHAR(STRING), TIME VARCHAR(STRING)>"
```

La structure en sortie est : 

``` 
"STRUCT<TASK VARCHAR(STRING), START VARCHAR(STRING), STOP VARCHAR(STRING)>"
``` 

Le principe de cette fonction est d'*exploser* une ligne avec plusieurs colonnes en plusieurs lignes avec quatre colonnes (pour le cas, l'activité, la date de début et la date de fin)

Pour les paramètres :

* **input** : correspond comme pour la déclinaison 1 de l'UDF à la ligne que l'on passe en entrée et que l'on veut *exploser*
* **dateFormat** : correspond au format de la date (ex: pour une activité qui a pour date 12/01/2020, le format est "dd/MM/yyyy" )
* **isStartInformation** : true indique que la date associée à l'activité en entrée correspond au début de l'activité, et qu'il faut donc calculer la fin, et false indique que la date correspond à la fin de l'activité et qu'il faut donc calculer le début (les calculs se font quand c'est possible en fonctions des dates des autres activités)
* **isTaskNameAscending** : true indique qu'en cas de date identiques, l'ordre est déterminé de manière ascendante par le nom de l'activité, alors que false signifie que l'ordre est determiné de manière descendante par le nom de l'activité

Par exemple en cas d'entrée :

| Cas   | Etape 1    | Etape 2    | Etape 3    | Etape 4    | Etape 5                                                                                     |
| ------|:----------:| :---------:|:----------:|:----------:|-----------:|
| cas 1 | 17/03/2020 | 16/03/2020 | 17/03/2020 | 18/03/2020 | 17/03/2020 |

La sortie est la suivante si **isStartInformation = true** et **isTaskNameAscending = true** :

| Cas   | Activité    | Debut      | Fin                                                                                        |
| ------|:-----------:| :---------:| :---------: |
| cas 1 | Etape 2     | 16/03/2020 | 17/03/2020  |
| cas 1 | Etape 1     | 17/03/2020 | 17/03/2020  |
| cas 1 | Etape 3     | 17/03/2020 | 17/03/2020  |
| cas 1 | Etape 5     | 17/03/2020 | 18/03/2020  |
| cas 1 | Etape 4     | 18/03/2020 | 18/03/2020  |

La sortie est la suivante si **isStartInformation = true** et **isTaskNameAscending = false** :

| Cas   | Activité    | Debut      | Fin                                                                                        |
| ------|:-----------:| :---------:| :---------: |
| cas 1 | Etape 2     | 16/03/2020 | 17/03/2020  |
| cas 1 | Etape 5     | 17/03/2020 | 17/03/2020  |
| cas 1 | Etape 3     | 17/03/2020 | 17/03/2020  |
| cas 1 | Etape 1     | 17/03/2020 | 18/03/2020  |
| cas 1 | Etape 4     | 18/03/2020 | 18/03/2020  |

La sortie est la suivante si **isStartInformation = false** et **isTaskNameAscending = true** :

| Cas   | Activité    | Debut      | Fin                                                                                        |
| ------|:-----------:| :---------:| :---------: |
| cas 1 | Etape 2     | 16/03/2020 | 16/03/2020  |
| cas 1 | Etape 1     | 16/03/2020 | 17/03/2020  |
| cas 1 | Etape 3     | 17/03/2020 | 17/03/2020  |
| cas 1 | Etape 5     | 17/03/2020 | 17/03/2020  |
| cas 1 | Etape 4     | 17/03/2020 | 18/03/2020  |

La sortie est la suivante si **isStartInformation = false** et **isTaskNameAscending = false** :

| Cas   | Activité    | Debut      | Fin                                                                                        |
| ------|:-----------:| :---------:| :---------: |
| cas 1 | Etape 2     | 16/03/2020 | 16/03/2020  |
| cas 1 | Etape 5     | 16/03/2020 | 17/03/2020  |
| cas 1 | Etape 3     | 17/03/2020 | 17/03/2020  |
| cas 1 | Etape 1     | 17/03/2020 | 17/03/2020  |
| cas 1 | Etape 4     | 17/03/2020 | 18/03/2020  |

Pour tester cette déclinaison directement dans ksqlDB, il suffit de lancer ksqlDB comme décrit dans le CONTRIBUTING.md à la racine du projet, puis de lancer les commandes suivantes :

(Les deux premières créations de STREAMS sont identiques à celles de la première déclinaison)
* Création du STREAM initial représentant les lignes avec leurs colonnes au format : Cas | Etape 1 | Etape 2 | Etape 3 | Etape 4 | Etape 5
```
CREATE STREAM s1 (
	cas VARCHAR,
	etape1 VARCHAR,
	etape2 VARCHAR,
	etape3 VARCHAR,
	etape4 VARCHAR,
	etape5 VARCHAR
) WITH (
    	kafka_topic = 's1',
	partitions = 1,
	value_format = 'avro'
);
```

* Création du STREAM permettant l'appel à l'UDF, ici un ARRAY<STRUCT(...), STRUCT(...), ...> correspond au paramètre **input** de l'UDF (ARRAY étant le type de ksqlDB qui fait la correspondance avec util.List de Java)

```
CREATE STREAM s2 AS SELECT 
    cas, 
    ARRAY[STRUCT(task := 'Etape 1', time := etape1), 
          STRUCT(task := 'Etape 2', time := etape2), 
          STRUCT(task := 'Etape 3', time := etape3), 
          STRUCT(task := 'Etape 4', time := etape4), 
          STRUCT(task := 'Etape 5', time := etape5)] AS etapes
    FROM s1 EMIT CHANGES;
```

* Création du STREAM avec **appel à l'UDF logpickr_transposition**

```
CREATE STREAM s3 AS SELECT 
    cas, 
    logpickr_transposition(etapes, "dd/MM/yyyy", true, true) AS etapes 
    FROM s2 EMIT CHANGES;
```
ou 
```
CREATE STREAM s3 AS SELECT 
    cas, 
    logpickr_transposition(etapes, "dd/MM/yyyy", true, false) AS etapes 
    FROM s2 EMIT CHANGES;
```
ou
```
CREATE STREAM s3 AS SELECT 
    cas, 
    logpickr_transposition(etapes, "dd/MM/yyyy", false, true) AS etapes 
    FROM s2 EMIT CHANGES;
```
ou
```
CREATE STREAM s3 AS SELECT 
    cas, 
    logpickr_transposition(etapes, "dd/MM/yyyy", false, false) AS etapes 
    FROM s2 EMIT CHANGES;
```

* Création du STREAM final en déconstruisant la STRUCT de **s3** en quatre colonnes différentes

``` 
CREATE STREAM s4 AS SELECT 
    cas, 
    etapes->task AS activite, 
    etapes->start AS debut,
    etapes->stop AS fin 
    FROM s3 EMIT CHANGES;
```

Une fois les STREAMS créés il est possible d'ajouter des données, et dans ce qui suit, chaque INSERT correspond à une ligne

```
INSERT INTO s1 (cas, etape1, etape2, etape3, etape4, etape5) VALUES ('cas1', '12/01/2020', '14/01/2020', '15/01/2020', '16/01/2020', '17/01/2020');
INSERT INTO s1 (cas, etape1, etape2, etape3, etape4, etape5) VALUES ('cas2', '14/02/2020', '15/02/2020', '18/02/2020', '', '18/02/2020');
INSERT INTO s1 (cas, etape1, etape2, etape3, etape4, etape5) VALUES ('cas3', '17/03/2020', '', '24/03/2020', '', '');

INSERT INTO s1 (cas, etape1, etape2, etape3, etape4, etape5) VALUES ('cas4', '17/03/2020', '25/03/2020', '', '16/03/2020', '24/03/2020');
INSERT INTO s1 (cas, etape1, etape2, etape3, etape4, etape5) VALUES ('cas5', '', '', '', '16/03/2020', '');
INSERT INTO s1 (cas, etape1, etape2, etape3, etape4, etape5) VALUES ('cas6', '', '', '', '', '');

INSERT INTO s1 (cas, etape1, etape2, etape3, etape4, etape5) VALUES ('cas7', '17/03/2020', '17/03/2020', '17/03/2020', '17/03/2020', '17/03/2020');
INSERT INTO s1 (cas, etape1, etape2, etape3, etape4, etape5) VALUES ('cas8', '17/03/2020', '16/03/2020', '17/03/2020', '18/03/2020', '17/03/2020');
```

Il est possible d'afficher le résultat final dans ksqlDB avec la commande :

```
SELECT cas, activite, debut, fin FROM s4 EMIT CHANGES;
```


## Informations complémentaires :

En ce qui concerne le fonctionnement de l'UDF, et ce quelque soit la déclinaison, il faut faire attention en cas de colonnes supplémentaires dans la ligne initiale. 

Par exemple si la ligne initiale correspond à :

| Cas   | Etape 1    | Etape 2    | Etape 3    | Etape 4    | Etape 5    | Prix total                                                                                  |
| ------|:----------:| :---------:|:----------:|:----------:|:----------:| -----------:
| cas 1 | 12/01/2020 | 14/01/2020 | 15/01/2020 | 16/01/2020 | 17/01/2020 | 240        |

Une information est donnée sur le prix total lié au processus. En cas d'utilisation de l'UDF (par exemple la première déclinaison), si les STREAM créés gardent l'information de la colonne liée au prix total, comme avec l'exemple suivant :

```
CREATE STREAM s1 (
	cas VARCHAR,
	etape1 VARCHAR,
	etape2 VARCHAR,
	etape3 VARCHAR,
	etape4 VARCHAR,
	etape5 VARCHAR,
	prix INTEGER
) WITH (
        kafka_topic = 's1',
	partitions = 1,
	value_format = 'avro'
);
```

```
CREATE STREAM s2 AS SELECT 
    cas, 
    ARRAY[STRUCT(task := 'Etape 1', time := etape1), 
          STRUCT(task := 'Etape 2', time := etape2), 
          STRUCT(task := 'Etape 3', time := etape3), 
          STRUCT(task := 'Etape 4', time := etape4), 
          STRUCT(task := 'Etape 5', time := etape5)] AS etapes,
    prix
    FROM s1 EMIT CHANGES;
```

```
CREATE STREAM s3 AS SELECT 
    cas, 
    logpickr_transposition(etapes) AS etapes,
    prix
    FROM s2 EMIT CHANGES;
```

``` 
CREATE STREAM s4 AS SELECT 
    cas, 
    etapes->task AS activite, 
    etapes->time AS horodatage,
    prix
    FROM s3 EMIT CHANGES;
```

Et bien le prix est ensuite présent pour chacune des lignes créées. Le résultat pour l'exemple actuel serait :

| Cas   | Activité    | Horodatage | Prix                                                                                        |
| ------|:-----------:| :---------:| :---------: |
| cas 1 | Etape 1     | 12/01/2020 | 240         |
| cas 1 | Etape 2     | 14/01/2020 | 240         |
| cas 1 | Etape 3     | 15/01/2020 | 240         |
| cas 1 | Etape 4     | 16/01/2020 | 240         |
| cas 1 | Etape 5     | 17/01/2020 | 240         |

Et cela peut ensuite poser problème par exemple en cas de somme de toutes les valeurs dans la colonne Prix pour faire un calcul du prix pour tous les processus, car le résultat ne reflète alors pas la valeur du prix réel pour un processus (pour le processus présenté ici la valeur calculée serait de 5x240, alors que le prix réel est de 240)