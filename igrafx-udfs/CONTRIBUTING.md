# UDF

## Create a new UDF

To create a new UDF, add a new package for the UDF in the **src.main.scala.com.igrafx.ksql.functions** package.

## How to use iGrafx UDFs

To use the iGrafx UDFs in ksqlDB, follow those steps :

Create the **.jar** of the project with the following command launched from the root of the project :

``` 
sbt assembly
```

The **jar** contains all the UDFs of the project. It means that you can use every UDF of the project in ksqlDB with this jar.

Then, place the newly created **.jar** (which is in the **target/scala-2.13** repository) in the **docker-compose/extensions/** repository of iGrafx Liveconnect project. If this repository doesn't exist, create it and check that the following lines are in the **ksqldb-server** of the docker-compose.yml :

``` 
ksqldb-server:
    ...
    volumes:
        - "./extensions/:/opt/ksqldb-udfs"
    environment:
      ...
      KSQL_KSQL_EXTENSION_DIR: "/opt/ksqldb-udfs"
```

We then launch the infrastructure from the **docker-compose/** repository of Liveconnect with the commands :

``` 
make liveconnect
```

We can then connect to the ksqlDB CLI, from the **docker-compose/** repository of Liveconnect, with the command :

``` 
docker-compose exec ksqldb-cli ksql http://ksqldb-server:8088
```

Once in the ksqlDB CLI, the different UDFs at your disposal are listed with the function : 

``` 
SHOW FUNCTIONS;
```

For more information on a particular UDF, see its associated README

### Version Commit

* modify the version number related to the udfs in the build.sbt file
* git fetch --all --tags
* generate the changelogs :
    * if needed install conventional-changelog-cli with the command : *npm install -g conventional-changelog-cli* or *sudo npm install -g conventional-changelog-cli*
    * at the root of the project, launch the command : *conventional-changelog -p angular -i CHANGELOG.md -s* (to keep the existing changelogs) or *conventional-changelog -p angular -i CHANGELOG.md -s -r 0* (to suppress the existing changelogs)
* Check that a CHANGELOG.md file has been generated at the root of the project, and that it contains the changelogs of the version