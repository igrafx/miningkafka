# UDF

Pour créer une nouvelle UDF, ajouter un nouveau package pour l'UDF dans **src.main.scala.com.logpickr.ksql.functions**.

Pour pouvoir utiliser les UDFs dans ksqlDB, il faut suivre les étapes suivantes :

Pour créer le **.jar** du projet, utiliser à la racine du projet la commande :

``` 
sbt assembly
```

Il faut ensuite placer le **.jar** créé (qui se trouve dans le répertoire **target/scala-2.13**) dans le répertoire **kafka/extensions/** du projet Logpickr Liveconnect. Si ce répertoire n'existe pas, le créer, et vérifier que les lignes suivantes se trouvent dans le service **ksqldb-server** du docker-compose.yml :

``` 
ksqldb-server:
    ...
    volumes:
        - "./extensions/:/opt/ksqldb-udfs"
    environment:
      ...
      KSQL_KSQL_EXTENSION_DIR: "/opt/ksqldb-udfs"
```

On lance ensuite l'infrastructure depuis le répertoire **kafka/** de Liveconnect en suivant les commandes :

``` 
docker-compose up -d
cd ../traefik
docker-compose up -d
```

On peut alors se connecter au CLI de ksqlDB depuis le répertoire kafka de Liveconnect avec la commande :

``` 
docker-compose exec ksqldb-cli ksql http://ksqldb-server:8088
```

Une fois dans le CLI ksqlDB, les différentes UDFs qu'il est possible d'utiliser sont listées via la fonction :

``` 
SHOW FUNCTIONS;
```

Pour plus d'informations sur une UDF Logpickr, voir le README associé

