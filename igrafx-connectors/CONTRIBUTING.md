# Contribution

Each connector corresponds to a specific module in this project. The path to the Connector class corresponds to **src/main/scala/com/igrafx/kafka/source** if this is a Source Connector, and to **src/main/scala/com/igrafx/kafka/sink** if this is a Sink one

Moreover, each Connector can be used with its related JAR, you can either create a JAR for all the connectors of the project with :

```
sbt assembly
```

Or you can create a JAR only for the connector you need with :

``` 
sbt moduleName/assembly
```

Where *moduleName* corresponds to the name of the module related to the connector

Then, you can find the *.jar* in the **artifacts/** directory

To use the connector in Kafka Connect, you need to add the *.jar* in the **docker-compose/connect-plugins/** directory of the **iGrafx Liveconnect** project

You can then launch the infrastructure from the **docker-compose/** directory of the **iGrafx Liveconnect** project with the commands :

```
make liveconnect
```

Finally, you can create the connector either in the ksqlDB CLI or with kafka-ui via :

```
CREATE SOURCE CONNECTOR ConnectorName WITH (
...
);
```

For a Source Connector, or via :

```
CREATE SINK CONNECTOR ConnectorName WITH (
...
);
```

For a Sink Connector

You can access the ksqlDB CLI from the **docker-compose/** directory of the **iGrafx Liveconnect** project with the command :

``` 
docker-compose exec ksqldb-cli ksql http://ksqldb-server:8088
```

And you can also access the Connector's logs with the following command written from the **docker-compose/** directory of the **iGrafx Liveconnect**

```
docker-compose logs -f connect
```

### Creation of a new Connector

If you want to create a new Connector, start by adding a module for this connector in the build.sbt file at the root of the project. You can then create a class that extends **SourceConnector**/**SinkConnector** for your Connector, and a class that extends **SourceTask**/**SinkTask** for the Connector's Tasks.

Add a README for your Connector describing the way it works, how to use it and its properties

### Connector Monitoring

It is possible to monitor a connector :

We can retrieve the state of a Connector and its Tasks in the CLI ksqlDB with the command :

``` 
SHOW CONNECTORS;
```

For errors that lead to the termination of a Task, once the administrator was able to solve the problem (for example a problem of rights on the writing of a file), it is possible to launch again the Task with the following commands (the commands use the REST interface of Kafka Connect) :

``` 
curl localhost:8083/connectors
```

To list the active connectors, then :

```
curl localhost:8083/connectors/connectorName/status | jq 
```

where **connectorName** is to replace by the name of the connector retrieved with the previous command (case sensitive). The command allows to get the list of the connector's Tasks (and obtain information about them, like their id or their status). It is then possible to restart the **FAILED** Tasks with the command :

``` 
curl -X POST localhost:8083/connectors/connectorName/tasks/taskId/restart
```

where **taskId** is to replace by the id of the Task retrieved with the previous command and where **connectorName** is to replace with the name (case sensitive) of the connector (retrieved two commands ago)

For more information and commands about the monitoring of Connectors/Tasks, follow this link : https://docs.confluent.io/home/connect/monitoring.html

It is important to note that when a Task goes to the **FAILED** state, the partitions that she was in charge of are redistributed among the remaining **RUNNING** Tasks (consequently, if there is an error in the **put** method of the initial Task, there is a possibility that the same data create the same error in their new assigned Task, and so on until all the Task of the connector are stopped. In this case you need to restart all the **FAILED** Tasks of the connector once the problem is solved

Moreover, in the event of a worker leaving the cluster, the Connectors/Tasks associated to this Worker go to the **UNASSIGNED** state for 5 minutes (default value of the **scheduled.rebalance.max.delay.ms** Worker property). If after 5 minutes the Worker didn't came back, then the Connectors/Tasks are reassigned to new Workers of the cluster

In the event of an addition/suppression of Worker, the Tasks can be rebalanced and redistributed on the new number of Workers (Task rebalance)

In the event of an addition/suppression of Tasks, the partitions can be rebalanced et redistributed on the new number of Tasks (Partition rebalance)

### Version Commit

* modify the version number related to the connector in the build.sbt file and in the function **version()** present in the Connector's class
* git fetch --all --tags
* generate the changelogs :
  * if needed install conventional-changelog-cli with the command : *npm install -g conventional-changelog-cli* or *sudo npm install -g conventional-changelog-cli*
  * at the root of the project, launch the command : *conventional-changelog -p angular -i CHANGELOG.md -s* (to keep the existing changelogs) or *conventional-changelog -p angular -i CHANGELOG.md -s -r 0* (to suppress the existing changelogs)
* Check that a CHANGELOG.md file has been generated at the root of the project, and that it contains the changelogs of the version

