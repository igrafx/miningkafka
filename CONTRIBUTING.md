# UDF

Pour créer une nouvelle UDF, ajouter un nouveau package pour l'UDF dans **src.main.scala.com.logpickr.ksql.functions**.

Pour pouvoir utiliser les UDFs dans ksqlDB, il faut suivre les étapes suivantes :

Pour créer le **.jar** du projet, utiliser à la racine du projet la commande :

``` 
sbt assembly
```

Il faut ensuite placer le **.jar** créé dans le répertoire **kafka/extensions/** du projet Logpickr Liveconnect puis lancer l'infrastructure depuis le répertoire **kafka/** de Liveconnect en suviant les commandes :

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

Pour plus d'information sur une UDF Logpickr, voir le README associé

