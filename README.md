# iGrafx Kafka Modules

[![made-with-scala](https://img.shields.io/badge/Made%20with-Scala-1f425f.svg)](https://scala-lang.org/)
[![Scala versions](https://img.shields.io/badge/scala-2.12%2B-blue)](https://scala-lang.org/)
[![Open Source Love svg2](https://badges.frapsoft.com/os/v2/open-source.svg?v=103)](https://github.com/ellerbrock/open-source-badges/)
[![License](https://img.shields.io/badge/license-MIT-green.svg)](https://github.com/igrafx/miningkafka/blob/master/LICENSE)
![GitHub commit activity (branch)](https://img.shields.io/github/commit-activity/m/igrafx/miningkafka?color=orange)
[![GitHub forks](https://badgen.net/github/forks/igrafx/miningkafka)](https://github.com/igrafx/miningkafka/forks)
![GitHub issues](https://img.shields.io/github/issues/igrafx/miningkafka?color=blue)
[![Project Status](http://www.repostatus.org/badges/latest/active.svg)](http://www.repostatus.org/#active)
![GitHub code size in bytes](https://img.shields.io/github/languages/code-size/igrafx/miningkafka?color=purple)
![GitHub repo size](https://img.shields.io/github/repo-size/igrafx/miningkafka?color=yellow)
![GitHub repo file count (file type)](https://img.shields.io/github/directory-file-count/igrafx/miningkafka?color=pink)


***

## Introduction

Welcome to the **iGrafx Kafka Modules**!

The **iGrafx Kafka Modules** are open-source tools designed to integrate seamlessly with Kafka, enabling efficient data streaming, transformation, and analysis. 
These modules empower you to use data, enrich their streams, and send real-time insights to the iGrafx Mining platform.

Built for flexibility and performance, these modules include the **iGrafx LiveConnect**, **iGrafx Connectors**, and **iGrafx UDFs (User-Defined Functions)**, 
offering a complete solution for modern data streaming and analytics workflows.

A detailed tutorial can be found in the [howto.md](https://github.com/igrafx/miningkafka/blob/dev/howto.md) file.

### Key Features

- **Streamlined Integration**: Connect, process, and deliver data effortlessly to the iGrafx Mining platform using Kafka.
- **Powerful Customization**: Define and deploy custom UDFs tailored to your specific data transformation needs.
- **Real-Time Data Processing**: Enable live connections for seamless, real-time analytics and decision-making.
- **Open-Source**: Freely use and adapt these modules to meet your requirements.

> **Note:** An iGrafx account is required to fully utilize these modules. For account setup, please contact [iGrafx Support](https://www.igrafx.com/).

Explore the repository and get started [here](https://github.com/igrafx/miningkafka).

You may also explore other iGrafx opensource projects such as the [iGrafx Mining SDK](https://github.com/igrafx/mining-python-sdk) or the [iGrafx KNIME Mining Extension](https://github.com/igrafx/KNIME-Mining-connector).

## Quickstart

This Quickstart guide covers setting up iGrafx Kafka Modules,
from cloning the repository to managing data streams and transformations.
It includes instructions for configuring **LiveConnect**,
using the **ksqlDB CLI** and **Kafka UI** to manage topics and connectors,
setting up **iGrafx Connectors** for streaming data, and implementing **User Defined Functions (UDFs)** for custom data transformations.
Each component can be run locally, giving you flexibility for testing and development.


To use the iGrafx Kafka Modules, first, clone the repository:

```
git clone https://github.com/igrafx/miningkafka.git
```
Then, make sure you have Docker and Docker Compose installed on your system.
Follow these links for installation instructions:
- [Docker](https://docs.docker.com/get-started/get-docker/)
- [Docker Compose](https://docs.docker.com/compose/install/)

### iGrafx Liveconnect: Quickstart

To launch **LiveConnect** run the following command:
```
cd igrafx-liveconnect/docker-compose/
make liveconnect
````

To stop **LiveConnect** run the following command:
```
make liveconnect-down
```
Furthermore, if you want to remove all the streams, tables or connectors and delete the data you inserted during your tests,
you can delete the ``/data`` folder that is in the ``/docker-compose`` directory.


### ksqlDB CLI and Kafka UI: Quickstart
With liveconnect running, you can now connect to the **ksqlDB CLI** that will allow you to send the desired commands.

Type the following command from /docker-compose in a terminal to connect to the CLI:
````bash
docker-compose exec ksqldb-cli ksql http://ksqldb-server:8088
````
Once in the CLI, you can send ksql commands, and you can quit it by typing ``exit``.

Moreover, Kafka UI is a user interface you can use to get information about your kafka cluster,
the topics, the ksql pipelines you created and more.

You can then access it at http://localhost:9021/.

The credentials for Kafka UI are set in the ``docker-compose.yml`` file, within the ``JAVA_OPTS`` variable.
``-Dspring.security.user.name`` is the username and ``-Dspring.security.user.password`` is the password.

### Getting the JAR builds from the CI/CD pipelines

To utilize the iGrafx Connectors and UDFs, you need the corresponding JAR files. There are two ways to obtain these files:

1. **Build the JAR Files Yourself**: Follow the instructions provided in the subsequent sections to build the JAR files manually.
2. **Retrieve JAR Files from the CI/CD Pipelines**: You can directly download the JAR files from the CI/CD pipelines in the iGrafx GitHub Project.

To retrieve the JAR files from the CI/CD pipelines, follow these steps:

1. Navigate to the **[Actions tab](https://github.com/igrafx/miningkafka/actions)** in the GitHub repository.
2. In the left sidebar, select the workflow of interest: **iGrafx Connectors** or **UDFs**, as shown below:

   ![iGrafx Connectors or UDFs](./imgs/igrafx-connectors-or-udfs.png)

3. Click on the workflow name to access the workflow runs. For this example, we will focus on the **iGrafx Connectors** workflow.
- Click on **iGrafx Connectors CI**. This will take you to the workflow runs page:

  ![iGrafx Connectors CI](./imgs/igrafx-connectors-ci.png)

4. Select the most recent successful run (indicated by a **green checkmark**). You will arrive at a page similar to this:

   ![iGrafx Connectors CI](./imgs/igrafx-connectors-ci-2.png)

5. Locate and click on **igrafx-connectors-artifacts** (or **igrafx-udfs-artifacts** for the UDFs workflow) to download a ZIP file containing the JAR files.

6. Once downloaded, extract the desired JAR files:
- Place the **Connectors JAR files** in the `igrafx-liveconnect/docker-compose/connect-plugins/` directory of the **iGrafx Liveconnect** module. This allows them to be used in the **ksqlDB CLI**.

  ![iGrafx Connectors CI](./imgs/igrafx-connectors-ci-3.png)

- For **UDFs JAR files**, place them in the `igrafx-liveconnect/docker-compose/extensions/` directory of the **iGrafx Liveconnect** module. If this directory does not exist, create it.

By following these steps, you can easily retrieve and configure the required JAR files for iGrafx Connectors and UDFs.

### iGrafx Connectors: Quickstart
If you want to use the iGrafx Connectors to send data from Kafka to the Process360 Live,
you must  go to the ``igrafx-connectors`` directory as follows:

```bash
cd igrafx-connectors/
```
Then, you can build the desired JAR file using the following command:

```
sbt aggregationMain/assembly
````
Once the **JAR** is created, you can find it in the ``/igrafx-connectors/artifacts`` repository.
Copy the latest **JAR** and paste it in the ``/docker-compose/connect-plugins/`` directory of the iGrafx Liveconnect module.

Now, by relaunching Liveconnect with the ``make liveconnect`` command, you will now be able to use the connector in ksql.

Furthermore, if you wish to check the status of the connectors you created, use the following command in the **ksqlDB CLI**:

````sql
SHOW CONNECTORS;
````
Finally, if one connector has a ``FAILED`` state, you can check the logs in ``Kafka-connect`` by using the following command from the ``/docker-compose`` directory in the Liveconnect module :
````bash
docker-compose logs -f connect
````

### iGrafx UDFs: Quickstart
UDFs (User Defined Functions) are useful for applying custom transformations to each value in a specific column of a stream.

You can create custom UDFs and integrate them into LiveConnect, making them available for use in data pipelines to enhance processing and transformation capabilities.

If you want to use the iGrafx UDFs, you must first go to the ``igrafx-udfs`` directory as follows:

```bash
cd igrafx-udfs/
```
Then, you can build the desired JAR file containing all the UDFsusing the following command:

```bash
sbt assembly
```
Once the **JAR** is created, you can find it in the ``/igrafx-udfs/target/scala-2.13`` repository.
Copy the latest **JAR** and paste it in the ``/docker-compose/extensions/`` directory of the iGrafx Liveconnect module.
If this directory doesn't exist, you can create it.

Now, by relaunching Liveconnect with the ``make liveconnect`` command, you will now be able to use the UDFs in ksql.

Moreover, you can display a list of available UDFs using the following command in the **ksqlDB CLI**:


````sql
SHOW FUNCTIONS;
````
You can also check the documentation of a given UDF by using the following command:

````sql
DESCRIBE FUNCTION <UDF_NAME>;
````
Where <UDF_NAME> is the name of the UDF you want to check the documentation of.

## Documentation

The full documentation can be found in the ```howto.md``` file [here](https://github.com/igrafx/miningkafka/blob/dev/howto.md).
Follow the instructions to try out the modules.

You may directly refer to examples in the [howto.md](https://github.com/igrafx/miningkafka/blob/dev/howto.md#examples) file.


## Contributing

Pull requests are welcome. For major changes, please open an issue first to discuss what you would like to change.

Please make sure to update tests as appropriate.

For more information on how to contribute, please see the [CONTRIBUTING.md](https://github.com/igrafx/miningkafka/blob/dev/CONTRIBUTING.md) file.

## Support

Support is available at the following address: [support@igrafx.com](mailto:support@igrafx.com).

## Notice

Your feedback and contributions are important to us. Don't hesitate to contribute to the project.

## License

These iGrafx Kafka Modules is licensed under the MIT License. See the [LICENSE](https://github.com/igrafx/miningkafka/blob/dev/LICENSE) file for more details.