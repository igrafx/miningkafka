# Installation 


Ce module est constitué d'une infrastructure kafka (sous répertoire `docker-compose/`) comprenant
- un broker (et zookeeper associé)
- schema-registry pour l'enregistrement des schémas des topics
- connect (pour les connecteurs kafka connect)
- ksqldb (et sa cli)
- kafka-ui (ou confluent control-center) pour une interface graphique donnant accès aux différentes fonctionnalités du cluster kafka
- outil kafkacat pour tests et commandes sur le cluster (optionnel)
- un serveur sftp (optionnel)
- une base de données postgresql pour traitements auxiliaires (optionnel)
- un docker-compose pour lancer dbt, outil de scripting pour bases de données (getdbt.com) (optionnel)

## Prérequis
L'utilisation du module liveconnect se base sur docker et docker-compose.

[Installation de docker](https://docs.docker.com/get-docker/)

[Installation de docker-compose](https://docs.docker.com/compose/install/)

## Installation


L'installation de base consiste principalement à copier et adapter ce répertoire `docker-compose/` incluant 
- docker-compose.yml
- fichiers de configuration de différents modules (/conf)
- des connecteurs kafka préinstallés (connect-plugins)

Si nécessaire,  un dns pour la résolution devra être mis en place (les exemples de configuration se basent sur le domaine .moon, à adapter selon l'instanciation)

Les conteneurs de cette infrastructure dialoguent entre eux par le biais d'un réseau docker interne `kafka-network`

Lancement de l'infrastructure kafka dockerisée :

```
cd docker-compose/
make liveconnect
```

Arrêt de l'infrastructure kafka dockerisée :

```
cd docker-compose/
make liveconnect-down
```

### Installation de nouveaux connecteurs

Les connecteurs Kafka sont à ajouter dans le répertoire `docker-compose/connect-plugins/` (référencé dans la variable CONNECT_PLUGIN_PATH)

Pour installer un nouveau connecteur,
utiliser le [Confluent Hub Client](https://docs.confluent.io/home/connect/confluent-hub/client.html). Note : le Confluent Hub client est intégré à l'image `connect` (confluentinc/cp-kafka-connect)

Exemple pour mise à jour du connecteur kafka-connect-servicenow:2.3.4, après avoir vérifié sa référence sous https://confluent.io/hub :

`docker-compose exec connect confluent-hub  install --component-dir /connect-plugins/ --verbose confluentinc/kafka-connect-servicenow:2.3.4`

noter l'ajout de l'option `--component-dir /connect-plugins/` par rapport à la commande proposée sur le site confluent.

Note : `confluent-hub` a pour but de simplifier la récupération et l'installation d'une version d'un module référencé sous https://confluent.io/hub.

Dans la pratique cela revient à copier *manuellement* un répertoire contenant jars et configurations sous le répertoire déclaré comme contenant les connecteurs (`connect-plugins` dans notre cas). L'installation d'un nouveau connecteur nécessite le redémarrage du conteneur docker `connect` (image confluentinc/cp-kafka-connect)

Il existe un grand nombre d'autres connecteurs pour kafka, notamment dans l'écosystème [Camel](https://camel.apache.org/camel-kafka-connector/latest/). Pour ces connecteurs, on utilisera donc la méthode manuelle décrite ci-dessous:

Exemple de procédure manuelle (téléchargement, décompression, copie du dossier et redémarrage du conteneur `connect`) :

```
wget https://repo.maven.apache.org/maven2/org/apache/camel/kafkaconnector/camel-smtp-kafka-connector/0.10.1/camel-smtp-kafka-connector-0.10.1-package.tar.gz

tar xvzf camel-smtp-kafka-connector-0.10.1-package.tar.gz

mv camel-smtp-kafka-connector connect-plugins/camel-smtp-kafka-connector-0.10.1-package/

docker-compose rm -sf connect

docker-compose up -d connect
```



### Connecteurs recommandés :
Pour des usages courants, les connecteurs suivants vous seront utiles :


- [File System Source Connector](https://www.confluent.io/hub/jcustenborder/kafka-connect-spooldir) : Chargement de fichiers (CSV, JSON, ...).

installation : 

`docker-compose exec connect confluent-hub  install --component-dir /connect-plugins/ --verbose jcustenborder/kafka-connect-spooldir:2.0.62`



- [JDBC Connector (Source and Sink)](https://www.confluent.io/hub/confluentinc/kafka-connect-jdbc) : Connexion bases de données JDBC

installation : 

`docker-compose exec connect confluent-hub  install --component-dir /connect-plugins/ --verbose confluentinc/kafka-connect-jdbc:10.2.4`

- [iGrafx Sink](https://gitlab.com/igrafx/logpickr/logpickr-kafka-connectors/-/blob/master/aggregationMain/src/main/scala/com/logpickr/kafka/sink/aggregationmain/README.md) : Connecteur Sink iGrafx permettant d'alimenter un projet iGrafx à partir d'un topic kafka

installation :

Deux possibilités :
* builder le jar du connecteur soi même en suivant les indications présentes au début de la [documentation pour utiliser les connecteurs iGrafx](https://gitlab.com/igrafx/logpickr/logpickr-kafka-connectors/-/blob/master/CONTRIBUTING.md)
* récupérer le jar du connecteur dans l'artifact *build:archive* du pipeline correspondant au tag de la version cible sur la plateforme de mining (sur [cette](https://gitlab.com/igrafx/logpickr/logpickr-kafka-connectors/-/pipelines?scope=tags&page=1) page). Le jar correspondant au connecteur sink iGrafx a pour nom **aggregation-main-connector_x** où x doit être remplacé par le numéro de version

### Installation d'UDFs iGrafx :

La documentation des différentes UDFs développées par iGrafx se trouve [ici](https://gitlab.com/igrafx/logpickr/logpickr-ksqldb-udf/-/blob/master/README.md)

installation :

Deux possibilités :
* builder le jar des UDFs soi même en suivant les indications présentes au début de la [documentation pour utiliser les UDFs iGrafx](https://gitlab.com/igrafx/logpickr/logpickr-ksqldb-udf/-/blob/master/CONTRIBUTING.md)
* récupérer le jar des UDFs dans l'artifact *build:archive* du pipeline correspondant au tag de la version cible sur la plateforme de mining (sur [cette](https://gitlab.com/igrafx/logpickr/logpickr-ksqldb-udf/-/pipelines?scope=tags&page=1) page).

# Configuration

## configuration de kafka-ui

kafka-ui est une interface graphique permettant d'interagir avec un cluster kafka/ksqlDB. Grâce à kafka-ui, il est possible de visualiser les messages des topics Kafka, les connecteurs créés, d'entrer des commandes ksqlDB et de monitorer une partie du cluster Kafka.

### Mots de passe de kafka-ui

Le nom d'utilisateur et le mot de passe à utiliser pour se connecter à l'interface graphique sont définis dans la partie kafka-ui du docker-compose (voir variable JAVA_OPTS, *-Dspring.security.user.name* correspond au nom d'utilisateur et *-Dspring.security.user.password* correspond au mot de passe



## database data-transform
Une base de données postgres est disponible afin d'effectuer des transformations non encore possibles en kSQLDB.

Un exemple ci-dessous permet de passer par une base de données postgres intermédiaire, afin de générer une colonne supplémentaire `cnt`qui numérote les événements au sein d'un même cas (identifié ici par la colonne `INCIDENT`)

1/ Création d'une table avec index autoincrement `id`.
Cet index permet au connecteur ksqlDB de récupérer en flux continu les données à partir des dernières données traitées. Un autre champ peut être utilisé à cette fin si le jeu de données s'y prête (timestamp, identifiant d'événements, ....)

```
-- CREER LA TABLE AVEC INDEX AUTO_INCREMENT POUR LECTURE JDBC SOURCE
-- Cette table peut être créée automatiquement au lancement initial : voir conf/pg-initdb.d/
```
Exemple de commande lancée dans votre interface postgres, accessible en local sur le port standard et les informations de connexion figurant dans le fichier `.env`
```
CREATE TABLE public."JDBC_TABLE" (
  id SERIAL PRIMARY KEY NOT NULL
);
```

2/ Cette table est alimentée à partir du contenu d'un topic `JDBC_TABLE` par un connecteur sink JDBC: 

``` 
CREATE SINK CONNECTOR JDBC_SINK_01 WITH (
  'connector.class'          = 'io.confluent.connect.jdbc.JdbcSinkConnector',
  'key.converter'             = 'org.apache.kafka.connect.storage.StringConverter',
  'topics'                         = 'JDBC_TABLE',
  'table.name.format'     = 'JDBC_TABLE',
  'connection.url'           = 'jdbc:postgresql://data-transform:5432/transform?verifyServerCertificate=false',
  'connection.user'          = 'datamanager',
  'connection.password'      = '1r8P!eXx',
  'auto.evolve'              = 'true'
);
```

3/ En sortie, on peut lire les données issues de la table avec un connecteur source JDBC: noter la requête `query` en sortie :

``` 
CREATE SOURCE CONNECTOR JDBCSOURCEConnector1 WITH (
    'connector.class' = 'io.confluent.connect.jdbc.JdbcSourceConnector',
    'tasks.max' = '1',
  'connection.url'           = 'jdbc:postgresql://data-transform:5432/transform?verifyServerCertificate=false',
  'connection.user'          = 'datamanager',
  'connection.password'      = '1r8P!eXx',
    'mode' = 'incrementing',
    'incrementing.column.name' = 'id',
    'numeric.mapping' = 'best_fit',
    'topic.prefix' = 'jdbc_cnt_case_lines',
    'query' = 'SELECT * , ROW_NUMBER() OVER(PARTITION BY INCIDENT ORDER BY id ASC) as cnt from JDBC_TABLE'
);
```

## Configuration serveur sftp

Il peut s'avérer nécessaire de fournir un serveur sftp afin que l'utilisateur dépose régulièrement une mise à jour de ses données sous forme de fichier csv.

Un serveur sftp est fourni dans le docker-compose, basé sur le conteneur https://hub.docker.com/r/corilus/sftp


Les paramètres dans le docker-compose.yml permettent de définir :
- l'utilisateur sftp et son mot de passe
- le répertoire et l'uid local dans lequel les fichiers seront téléchargés
- le port local du serveur sftp

Les répertoires locaux ou les fichiers seront uploadés ayant vocation à être consommés par l'image connect, penser à mettre en conformité les montages de répertoires entre les images `connect`et `sftp`, notamment en cas de changement ou d'ajout d'utilisateurs.

### Connexion au serveur sftp
avec l'utilisateur par défaut `foo`

```
sftp -P 2222 foo@<host-ip>
```

## DNS associé à liveconnect


Le DNS doit pointer vers l'IP publique de la machine sur laquelle est installé le module liveconnect.


Note : penser à adapter la variable CONTROL_CENTER_KSQL_LIVECONNECT1_ADVERTISED_URL dans docker-compose.yml

exemple : 
CONTROL_CENTER_KSQL_LIVECONNECT1_ADVERTISED_URL: "https://liveconnect.demo.logpickr.cloud/ksqldb-server"

Le chemin ksqldb-server a été volontairement ajouté afin de discriminer le traitement dans un reverse proxy, un traitement spécifique devant être spécifié pour les websockets. Un exemple est fourni sous nginx-proxy/template/liveconnect.conf.template.
L'adaptation au dns utilisé s'effectue par l'intermédiaire des deux variables utilisées :
NGINX_HOST : domaine principal de la plateforme (ex pf1.logpickr.com)
WORKGROUP_ID : identifiant du workgroup, utilisé comme sous domaine du domaine principal (ex WORKGROUP_ID = 325151bf-2007-4308-b94d-b436a61a35c5 donnera un dns final 325151bf-2007-4308-b94d-b436a61a35c5.pf1.logpickr.com )
Différents sous domaines pourront être ensuite utilisés pour un même workgroup, exemples :

liveconnect (accès à la console control-center de l'infra ksqldb)  :  liveconnect-325151bf-2007-4308-b94d-b436a61a35c5.pf1.logpickr.com

liveconnect-batch  (accès au notebook jupyter pour les traitements batch en spark/scala ) : liveconnect-batch-325151bf-2007-4308-b94d-b436a61a35c5.pf1.logpickr.com

Il est important de configurer une adresse d'accès au control center dans la variable CONTROL_CENTER_KSQL_LIVECONNECT1_ADVERTISED_URL (ou equivalente), sans quoi certaines erreurs pourraient survenir dans l'interface.
Cette addresse peut correspondre par exemple à l'url d'accès publique que vous avez configuré via votre DNS pour la page d'accès au control-center (par défault, en local, )
plus de détails : 

https://github.com/confluentinc/ksql/issues/3374 
https://docs.confluent.io/5.5.0/ksqldb/integrate-ksql-with-confluent-control-center.html#check-network-connectivity-between-ksqldb-and-c3-short

Note : LIVECONNECT1  dans la variable CONTROL_CENTER_KSQL_LIVECONNECT1_ADVERTISED_URL peut être modifié, et correspondra au nom de l'instance ksqlDB dans l'interface de control-center.

# Utilisation

## Accès à Kafka-ui

Via le DNS associé au control center (via le reverse proxy nginx)

exemple : https://liveconnect-325151bf-2007-4308-b94d-b436a61a35c5.pf1.logpickr.com

Voir plus haut la configuration des DNS , exemple

pour les variables d'environnement
NGINX_HOST : domaine principal de la plateforme (ex pf1.logpickr.com)
WORKGROUP_ID : identifiant du workgroup, utilisé comme sous domaine du domaine principal (ex WORKGROUP_ID = 325151bf-2007-4308-b94d-b436a61a35c5 donnera un dns final 325151bf-2007-4308-b94d-b436a61a35c5.pf1.logpickr.com )

on utilise dans les templates nginx le préfixe liveconnect-  pour l'accès  au control-center ) : liveconnect-325151bf-2007-4308-b94d-b436a61a35c5.pf1.logpickr.com

La composition des DNS peut être ajustée dans les templates nginx (voir nginx-config/templates/)

## Accès à la console ksqldb-cli

ksqldb-cli permet d'accéder en ligne de commande à la saisie des commandes ksql, à la visualisation des connecteurs, topics, streams et tables KSQL, ....

```
docker-compose exec ksqldb-cli ksql http://ksqldb-server:8088
```
une fois accédé au prompt, il est possible de positionner des variables propres à l'environnement :

``` 
SET 'auto.offset.reset' = 'earliest';
```

[Documentation de référence](https://docs.ksqldb.io/en/latest/operate-and-deploy/installation/cli-config/)

## Référence KSQLDB

Afin d'aller plus avant dans l'utilisation de KSQLDB : [https://ksqldb.io/](https://ksqldb.io/)



### Comment déclencher un traitement dbt à partir du pipeline ksqldb ?

La solution dépend du mode de déploiement de liveconnect. On supposera ici que liveconnect est installé sur un noeud dédié, accessible en SSH.

Le principe retenu consiste alors à exécuter une commande ssh déclenchant le traitement `dbt` (ce qui est fait par défaut dans le lancement du docker-compose du chapitre précédent).

On peut à cet effet utiliser un connecteur [Kafka-connect ssh](https://camel.apache.org/camel-kafka-connector/latest/reference/connectors/camel-ssh-kafka-sink-connector.html) afin de faire le lien entre 

- un topic kafka sur lequel on envoie des commandes, et
- le serveur ssh distant qui va exécuter ces mêmes commandes au travers du connecteur.

Un exemple d'[utilisation basique de ce connecteur](https://dzone.com/articles/a-simple-example-of-camel-kafka-connector) (ou [celui du projet apache](https://github.com/apache/camel-kafka-connector-examples/tree/main/ssh/ssh-sink)) sera à adapter à votre configuration.

# Accès au Notebook jupyter

Définition du mot de passe (dans conf/jupyter/jupyter_notebook_config.py)

https://jupyter-notebook.readthedocs.io/en/stable/public_server.html#adding-hashed-password-to-your-notebook-configuration-file

Voir plus haut la configuration des DNS , exemple

pour les variables d'environnement
NGINX_HOST : domaine principal de la plateforme (ex pf1.logpickr.com)
WORKGROUP_ID : identifiant du workgroup, utilisé comme sous domaine du domaine principal (ex WORKGROUP_ID = 325151bf-2007-4308-b94d-b436a61a35c5 donnera un dns final 325151bf-2007-4308-b94d-b436a61a35c5.pf1.logpickr.com )

on utilise dans les templates nginx le préfixe  liveconnect-batch-  (accès au notebook jupyter pour les traitements batch en spark/scala ) : liveconnect-batch-325151bf-2007-4308-b94d-b436a61a35c5.pf1.logpickr.com

La composition des DNS peut être ajustée dans les templates nginx (voir nginx-config/templates/)

# Execution locale

Les points suivants peuvent être nécessaires pour pouvoir executer localement le docker-compose en utilisant le revers proxy nginx et les DNS associés :

- définir dans .env les variables
HOST=demo2.logpickr.cloud
WORKGROUP_ID=wk1

adaptées à la plateforme et au workgroup auquel on souhaite associer liveconnect.
Ces variables sont utilisées dans les templates nginx-proxy/templates/

- copier dans nginx-proxy/certificates les certificats letsencrypt correspondant aux sous domaines génériques gérés par le certificat  (exe pour demo2.logpickr.cloud, les certificats pouvant gérer tous les sous domaines *.demo2.logpickr.cloud)

scp demo2:/opt/docker-compose/nginx-proxy/certificates/docker-star.* nginx-proxy/certificates/

attention la clé privée docker-star.key n'est pas accessible à tous les utilisateurs.

- utiliser un dns local ou modifier /etc/hosts pour définir l'ip locale des domaines utilisés :
extrait de /etc/hosts :
127.0.0.1       liveconnect-325151bf-2007-4308-b94d-b436a61a35c5.demo2.logpickr.cloud
127.0.0.1       liveconnect-wk1.demo2.logpickr.cloud
127.0.0.1       liveconnect-batch-wk1.demo2.logpickr.cloud

# Configuration pour un topic donné
le template igrafx-liveconnect peut être installé sur une VM distincte du reste de l'application.

Toutefois, le registry et le broker kafka associé doivent être joignables par le conteneur `api` de l'application ciblée.
Il faut dans ce cas ouvrir les ports registry et du broker kafka sur l'hôte de la VM ou ils sont installés, et que cet hôte soit joignable depuis `api`.
Pour le workgroup auquel on souhaite associer liveconnect :
- configurer son ID dans .env/WORKGROUP_ID
- dans la table postgres WORKGROUPS, renseigner les colonnes KAFKA_BROKER et KAFKA_REGISTRY avec l'url contenant l'IP et le port correspondant ( http://kafka-broker:29092  et http://schema-registry:8081)

exemple, si les deux VMs peuvent communiquer sur le même réseau privé, et que les ports correspondants ont été ouverts sur la vm liveconnect :
dans la table WORGROUPS, pour le workgroup considéré :
KAFKA_BROKER : http://192.168.1.128:19092
KAFKA_REGISTRY : http://192.168.1.128:8081

Il sera alors possible, pour l'administrateur du workgroup, d'activer le topic kafka recevant l'ensemble des cas d'un projet lors des mises à jour des projets du workgroup.