# Installation 

This module consists of a Kafka infrastructure (in the `docker-compose/` subdirectory) including:
-  a broker (and associated Zookeeper)
-  schema-registry for registering topic schemas
-  connect (for Kafka Connect connectors)
-  ksqldb (and its CLI)
-  kafka-ui (or Confluent Control Center) for a graphical interface giving access to the various functionalities of the Kafka cluster
-  kafkacat tool for testing and commands on the cluster (optional)
-  an SFTP server (optional)
-  a PostgreSQL database for auxiliary processing (optional)
-  a docker-compose to launch dbt, a scripting tool for databases (getdbt.com) (optional)

## Prerequisites
The use of the liveconnect module is based on Docker and Docker-compose.

[Installation of Docker](https://docs.docker.com/get-docker/)

[Installation of Docker-compose](https://docs.docker.com/compose/install/)

## Installation

The basic installation mainly consists of copying and adapting this `docker-compose/` directory including:
-  docker-compose.yml
-  configuration files for different modules (/conf)
-  pre-installed Kafka connectors (connect-plugins)

If necessary, a DNS for resolution will need to be put in place (the configuration examples are based on the .moon domain, to be adapted according to instantiation)

The containers of this infrastructure communicate with each other through an internal Docker network `kafka-network`

Launching the Dockerized Kafka infrastructure:

```
cd docker-compose/
make liveconnect
```

Stopping the Dockerized Kafka infrastructure:

```
cd docker-compose/
make liveconnect-down
```

### Installing new connectors

Kafka connectors are to be added to the `docker-compose/connect-plugins/` directory (referenced in the CONNECT_PLUGIN_PATH variable)

To install a new connector, use the [Confluent Hub Client](https://docs.confluent.io/home/connect/confluent-hub/client.html). Note: the Confluent Hub client is integrated into the `connect` image (confluentinc/cp-kafka-connect)

Example for updating the kafka-connect-servicenow:2.3.4 connector, after checking its reference under https://confluent.io/hub:

`docker-compose exec connect confluent-hub  install --component-dir /connect-plugins/ --verbose confluentinc/kafka-connect-servicenow:2.3.4`

Note the addition of the `--component-dir /connect-plugins/` option compared to the command proposed on the Confluent site.

Note: `confluent-hub` aims to simplify the retrieval and installation of a version of a module referenced under https://confluent.io/hub.

In practice, this involves *manually* copying a directory containing jars and configurations under the directory declared as containing the connectors (`connect-plugins` in our case). Installing a new connector requires restarting the `connect` Docker container (confluentinc/cp-kafka-connect image).

There are many other Kafka connectors, particularly in the [Camel](https://camel.apache.org/camel-kafka-connector/latest/) ecosystem. For these connectors, we will use the manual method described below:

Example of manual procedure (download, decompress, copy the folder, and restart the `connect` container):

```
wget https://repo.maven.apache.org/maven2/org/apache/camel/kafkaconnector/camel-smtp-kafka-connector/0.10.1/camel-smtp-kafka-connector-0.10.1-package.tar.gz

tar xvzf camel-smtp-kafka-connector-0.10.1-package.tar.gz

mv camel-smtp-kafka-connector connect-plugins/camel-smtp-kafka-connector-0.10.1-package/

docker-compose rm -sf connect

docker-compose up -d connect
```


# Recommended Connectors

Here are the installation commands for the recommended connectors:
- [File System Source Connector](https://www.confluent.io/hub/jcustenborder/kafka-connect-spooldir): for loading files (CSV, JSON, ...)
    ```
    docker-compose exec connect confluent-hub install --component-dir /connect-plugins/ --verbose jcustenborder/kafka-connect-spooldir:2.0.65
    ```

- [JDBC Connector (Source and Sink)](https://www.confluent.io/hub/confluentinc/kafka-connect-jdbc): for JDBC database connection
    ```
    docker-compose exec connect confluent-hub install --component-dir /connect-plugins/ --verbose confluentinc/kafka-connect-jdbc:10.7.1
    ```
- [iGrafx Sink](https://gitlab.com/igrafx/logpickr/logpickr-kafka-connectors/-/blob/master/aggregationMain/src/main/scala/com/logpickr/kafka/sink/aggregationmain/README.md): for feeding a iGrafx project from a Kafka topic

Two possible ways : 

  -   Build the connector jar by following the instructions in the [documentation to use iGrafx connectors](https://gitlab.com/igrafx/logpickr/logpickr-kafka-connectors/-/blob/master/CONTRIBUTING.md).

  -   Retrieve the connector jar from the *build:archive* artifact of the pipeline corresponding to the target version tag on the mining platform (on [this](https://gitlab.com/igrafx/logpickr/logpickr-kafka-connectors/-/pipelines?scope=tags&page=1) page). The jar file for the iGrafx Sink connector is named **aggregation-main-connector_x**, where x should be replaced by the version number.

Then copy the .jar under connect-plugins/igrafx/
### iGrafx UDFs installation

The documentation for the different UDFs developed by iGrafx can be found [here](https://gitlab.com/igrafx/logpickr/logpickr-ksqldb-udf/-/blob/master/README.md).

Two possible installation instructions:
-    Build the UDFs jar by following the instructions in the [documentation to use iGrafx UDFs](https://gitlab.com/igrafx/logpickr/logpickr-ksqldb-udf/-/blob/master/CONTRIBUTING.md).
-    Retrieve the UDFs jar from the *build:archive* artifact of the pipeline corresponding to the target version tag on the mining platform (on [this](https://gitlab.com/igrafx/logpickr/logpickr-ksqldb-udf/-/pipelines?scope=tags&page=1) page).

Then copy the .jar under connect-plugins/extensions/
# Configuration

## Kafka-UI Configuration

Kafka-UI is a graphical interface for interacting with a Kafka/KSQLDB cluster. With Kafka-UI, you can view Kafka topic messages, created connectors, enter ksqlDB commands, and monitor a portion of the Kafka cluster.

### Kafka-UI Passwords

The username and password to use to log in to the graphical interface are defined in the kafka-ui section of the docker-compose file (see JAVA_OPTS variable, *-Dspring.security.user.name* corresponds to the username and *-Dspring.security.user.password* corresponds to the password).



## Data-Transform Database

A Postgres database is available for performing transformations that are not yet possible in kSQLDB.

The following example allows you to go through an intermediate Postgres database to generate an additional column `cnt` that numbers the events within the same case (identified here by the `INCIDENT` column).

1/ Create a table with auto-increment index `id`.
This index allows the ksqlDB connector to continuously retrieve data from the latest processed data. Another field can be used for this purpose if the data set allows it (timestamp, event identifier, ...).

```
-- CREATE TABLE WITH AUTO_INCREMENT INDEX FOR JDBC SOURCE READING
-- This table can be created automatically at initial launch: see conf/pg-initdb.d/
```
Example of command launched in your local postgres interface, accessible on the standard port and the connection information in the `.env` file
```
CREATE TABLE public."JDBC_TABLE" (
  id SERIAL PRIMARY KEY NOT NULL
);
```

2/ This table is fed from the content of a `JDBC_TABLE` topic by a JDBC sink connector:

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

3/ To read the data from the table, a JDBC source connector can be used. Note the `query` for the output:

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

## Configuration of SFTP server

It may be necessary to provide an SFTP server so that the user can regularly upload updates to their data in the form of CSV files.

An SFTP server is provided in the docker-compose, based on the container https://hub.docker.com/r/corilus/sftp


The parameters in the docker-compose.yml file allow you to define:
-  the SFTP user and their password
-  the local directory and uid where the files will be downloaded
-  the local port of the SFTP server

The local directories or files that will be uploaded and consumed by the `connect` image should have their directory mounts in conformity between the `connect` and `sftp` images, especially in case of changes or additions of users.

### Connecting to the SFTP server
with the default user `foo`

To connect to the SFTP server with the default user `foo`, you can use an SFTP client such as FileZilla or WinSCP and connect to `localhost` on the port specified in the docker-compose file (default is `2222`). 

Use `foo` as the username and the password specified in the docker-compose file. Then, you can upload files to the directory specified in the docker-compose file.

```
sftp -P 2222 foo@<host-ip>
```

## DNS associated with liveconnect

The DNS should point to the public IP address of the machine on which the liveconnect module is installed.

Note: remember to adapt the `CONTROL_CENTER_KSQL_LIVECONNECT1_ADVERTISED_URL` variable in the docker-compose.yml file.

Example:
`CONTROL_CENTER_KSQL_LIVECONNECT1_ADVERTISED_URL: "https://liveconnect.demo.logpickr.cloud/ksqldb-server"`

The `ksqldb-server` path has been deliberately added to discriminate the processing in a reverse proxy, with specific processing to be specified for websockets. An example is provided under nginx-proxy/template/liveconnect.conf.template. The adaptation to the DNS used is done through the two variables used:
`NGINX_HOST`: main domain of the platform (e.g. pf1.logpickr.com)
`WORKGROUP_ID`: workgroup identifier, used as a subdomain of the main domain (e.g. `WORKGROUP_ID = 325151bf-2007-4308-b94d-b436a61a35c5` will give a final DNS of `325151bf-2007-4308-b94d-b436a61a35c5.pf1.logpickr.com`)
Different subdomains can then be used for the same workgroup, examples:

`liveconnect` (access to the control-center console of the ksqldb infra): `liveconnect-325151bf-2007-4308-b94d-b436a61a35c5.pf1.logpickr.com`

`liveconnect-batch` (access to the jupyter notebook for batch processing in spark/scala): `liveconnect-batch-325151bf-2007-4308-b94d-b436a61a35c5.pf1.logpickr.com`

It is important to configure an access address to the control center in the `CONTROL_CENTER_KSQL_LIVECONNECT1_ADVERTISED_URL` (or equivalent) variable, otherwise some errors may occur in the interface.
This address can correspond, for example, to the public access URL that you have configured via your DNS for the control-center access page (by default, locally).

More details:
https://github.com/confluentinc/ksql/issues/3374
https://docs.confluent.io/5.5.0/ksqldb/integrate-ksql-with-confluent-control-center.html#check-network-connectivity-between-ksqldb-and-c3-short

Note: `LIVECONNECT1` in the `CONTROL_CENTER_KSQL_LIVECONNECT1_ADVERTISED_URL` variable can be modified and will correspond to the name of the ksqlDB instance in the control-center interface.

# Usage

## Access to the Kafka interface

Via the DNS associated with the control center (via the nginx reverse proxy)

Example: https://liveconnect-325151bf-2007-4308-b94d-b436a61a35c5.pf1.logpickr.com

See above for DNS configuration, e.g.

for environment variables
`NGINX_HOST`: main domain of the platform (e.g. pf1.logpickr.com)
`WORKGROUP_ID`: workgroup identifier, used as a subdomain of the main domain (e.g. `WORKGROUP_ID = 325151bf-2007-4308-b94d-b436a61a35c5` will give a final DNS of `325151bf-2007-4308-b94d-b436a61a35c5.pf1.logpickr.com`)

In the nginx templates, we use the prefix `liveconnect-` for accessing the control-center: `liveconnect-325151bf-2007-4308-b94d-b436a61a35c5.pf1.logpickr.com`

The composition of DNS can be adjusted in the nginx templates (see nginx-config/templates/)

## Access to the ksqldb-cli console

ksqldb-cli allows command-line access to entering ksql commands, viewing KSQL connectors, topics, streams, and tables, etc.

```
docker-compose exec ksqldb-cli ksql http://ksqldb-server:8088
```
Once you have accessed the prompt, it is possible to set environment-specific variables:

``` 
SET 'auto.offset.reset' = 'earliest';
```

[Reference documentation](https://docs.ksqldb.io/en/latest/operate-and-deploy/installation/cli-config/)

## KSQLDB Reference

To go further in the use of KSQLDB: [https://ksqldb.io/](https://ksqldb.io/)


### How to trigger a dbt process from the ksqldb pipeline?

The solution depends on the deployment mode of liveconnect. Here we assume that liveconnect is installed on a dedicated node, accessible via SSH.

The selected approach is to execute an ssh command that triggers the `dbt` process (which is done by default in the docker-compose launch of the previous chapter).

To do this, you can use a [Kafka-connect ssh connector](https://camel.apache.org/camel-kafka-connector/latest/reference/connectors/camel-ssh-kafka-sink-connector.html) to link:

-  a Kafka topic on which commands are sent, and
-  the remote ssh server that will execute these same commands through the connector.

An example of a [basic usage of this connector](https://dzone.com/articles/a-simple-example-of-camel-kafka-connector) (or [that of the Apache project](https://github.com/apache/camel-kafka-connector-examples/tree/main/ssh/ssh-sink)) will need to be adapted to your configuration.

# Access to the Jupyter Notebook

Definition of the password (in `conf/jupyter/jupyter_notebook_config.py`)

https://jupyter-notebook.readthedocs.io/en/stable/public_server.html#adding-hashed-password-to-your-notebook-configuration-file

See above for DNS configuration, e.g.

for environment variables
`NGINX_HOST`: main domain of the platform (e.g. pf1.logpickr.com)
`WORKGROUP_ID`: workgroup identifier, used as a subdomain of the main domain (e.g. `WORKGROUP_ID = 325151bf-2007-4308-b94d-b436a61a35c5` will give a final DNS of `325151bf-2007-4308-b94d-b436a61a35c5.pf1.logpickr.com`)

In the nginx templates, we use the prefix `liveconnect-batch-` (access to the jupyter notebook for batch processing in spark/scala): `liveconnect-batch-325151bf-2007-4308-b94d-b436a61a35c5.pf1.logpickr.com`

The composition of DNS can be adjusted in the nginx templates (see nginx-config/templates/)

# Local execution
Note : the interfaces can still be accessed locally without DNS

Ex : http://localhost:9021  for kafka-ui

The following points may be necessary to be able to locally execute the docker-compose using the nginx reverse proxy and associated DNS:

-  define in `.env` the variables `HOST` and `WORKGROUP_ID` adapted to the platform and the workgroup to which you want to associate liveconnect. These variables are used in the `nginx-proxy/templates/` templates.

-  copy the letsencrypt certificates corresponding to the generic subdomains managed by the certificate (e.g. for `demo2.logpickr.cloud`, the certificates can manage all subdomains `*.demo2.logpickr.cloud`) into `nginx-proxy/certificates/`.

```
scp demo2:/opt/docker-compose/nginx-proxy/certificates/docker-star.* nginx-proxy/certificates/
```

Note: the `docker-star.key` private key is not accessible to all users.

-  use a local DNS or modify `/etc/hosts` to define the local IP addresses of the domains used. For example:

```
127.0.0.1       liveconnect-325151bf-2007-4308-b94d-b436a61a35c5.demo2.logpickr.cloud
127.0.0.1       liveconnect-wk1.demo2.logpickr.cloud
127.0.0.1       liveconnect-batch-wk1.demo2.logpickr.cloud
```

# Configuration for a given topic

The `igrafx-liveconnect` template can be installed on a separate VM from the rest of the application.

However, the Kafka registry and broker associated with the topic must be reachable by the `api` container of the targeted application.
In this case, it is necessary to open the registry and Kafka broker ports on the host of the VM where they are installed, and that this host is reachable from `api`.

For the workgroup to which you want to associate liveconnect:
-  configure its ID in `.env/WORKGROUP_ID`
-  in the `WORKGROUPS` PostgreSQL table, fill in the `KAFKA_BROKER` and `KAFKA_REGISTRY` columns with the corresponding URL (e.g. `http://kafka-broker:29092` and `http://schema-registry:8081`)

For example, if the two VMs can communicate on the same private network, and the corresponding ports have been opened on the liveconnect VM:

In the `WORKGROUPS` table, for the relevant workgroup:
-  `KAFKA_BROKER`: `http://192.168.1.128:19092`
-  `KAFKA_REGISTRY`: `http://192.168.1.128:8081`

The workgroup administrator will then be able to activate the Kafka topic that receives all cases of a project during project updates.