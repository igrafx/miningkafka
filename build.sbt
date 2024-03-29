scalaVersion := "2.13.5"
version := "2.30.0"
organization := "com.logpickr"
organizationName := "Logpickr"
organizationHomepage := Some(url("https://www.logpickr.com"))

lazy val dependencies = new {
  private val kafkaVersion = "3.4.1"
  private val ksqldbUdfVersion = "7.4.4"
  private val scalajVersion = "2.4.2"
  private val json4sVersion = "4.0.5"
  private val jooqVersion = "3.14.15"
  private val jodaVersion = "2.10.14"
  private val scalatestVersion = "3.2.11"
  private val scalaTestMockitoVersion = "3.2.10.0"
  private val nettyVersion = "4.1.77.Final"
  private val jacksonVersion = "2.15.3"

  val kafka = "org.apache.kafka"            %% "kafka"                        % kafkaVersion
  val kafkaApi = "org.apache.kafka"          % "connect-api"                  % kafkaVersion
  val ksqldbUdf = "io.confluent.ksql"        % "ksqldb-udf"                   % ksqldbUdfVersion
  val scalaj = "org.scalaj"                 %% "scalaj-http"                  % scalajVersion
  val json4sNative = "org.json4s"           %% "json4s-native"                % json4sVersion
  val json4sJackson = "org.json4s"          %% "json4s-jackson"               % json4sVersion
  val json4sExt = "org.json4s"              %% "json4s-ext"                   % json4sVersion
  val jooq = "org.jooq"                      % "jooq"                         % jooqVersion
  val joda = "joda-time"                     % "joda-time"                    % jodaVersion
  val nettyHandler = "io.netty"              % "netty-handler"                % nettyVersion
  val nettyTransport = "io.netty"            % "netty-transport-native-epoll" % nettyVersion
  val jackson = "com.fasterxml.jackson.core" % "jackson-databind"             % jacksonVersion
  val scalatest = "org.scalatest"           %% "scalatest-funspec"            % scalatestVersion        % Test
  val mockito = "org.scalatestplus"         %% "mockito-3-4"                  % scalaTestMockitoVersion % Test
}

libraryDependencies ++= Seq(
  dependencies.kafka,
  dependencies.kafkaApi,
  dependencies.ksqldbUdf,
  dependencies.scalaj,
  dependencies.json4sNative,
  dependencies.json4sJackson,
  dependencies.json4sExt,
  dependencies.jooq,
  dependencies.joda,
  dependencies.scalatest,
  dependencies.mockito
)

dependencyOverrides ++= Seq(
  dependencies.nettyHandler,
  dependencies.nettyTransport,
  dependencies.jackson
)

lazy val compilerOptionsWithWarnings = Seq(
  "-unchecked",
  "-feature",
  "-language:existentials",
  "-language:higherKinds",
  "-language:implicitConversions",
  "-language:postfixOps",
  "-deprecation",
  "-encoding",
  "utf8"
)

fork := true
parallelExecution in test := true
testForkedParallel in test := true
resolvers ++= Seq(
  "bintray-spark-packages" at "https://dl.bintray.com/spark-packages/maven",
  "Typesafe Simple Repository" at "https://repo.typesafe.com/typesafe/simple/maven-releases",
  "MavenRepository" at "https://mvnrepository.com",
  "confluent" at "https://packages.confluent.io/maven/"
)
scalacOptions ++= compilerOptionsWithWarnings

javaOptions in run ++= Seq(
  "-Xdebug",
  "-Xrunjdwp:transport=dt_socket,server=y,suspend=n,address=5005"
)

//test in assembly := {}

assemblyMergeStrategy in assembly := {
  case x if x.contains("io.netty.versions.properties") => MergeStrategy.concat
  case x if x.contains("module-info.class") => MergeStrategy.concat
  case x =>
    val oldStrategy = (assemblyMergeStrategy in assembly).value
    oldStrategy(x)
}
