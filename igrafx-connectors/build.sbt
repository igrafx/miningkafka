//sbt clean
//File -> Invalidate and restart
// supprimer .idea puis sbt clean et relancer Intellij

val description = Seq(
  organization := "com.igrafx",
  organizationName := "iGrafx",
  organizationHomepage := Some(url("https://www.igrafx.com"))
)

lazy val root = project
  .in(file("."))
  .settings(
    commonSettings,
    dependencyCheckSkipTestScope := true,
    dependencyCheckSkipProvidedScope := true
  )
  .aggregate(
    aggregationMain,
    aggregation,
    core
  )

val aggregationMainVersion = "2.35.0"
lazy val aggregationMain = project
  .settings(
    name := "aggregationmain",
    version := aggregationMainVersion,
    commonSettings,
    libraryDependencies ++= mainDependencies ++ aggregationDependencies ++ commonDependencies,
    dependencyOverrides ++= overrideDependencies,
    aggregationMainAssembly
  )
  .dependsOn(core % "compile->compile;test->test")

val aggregationVersion = "2.35.0"
lazy val aggregation = project
  .settings(
    name := "aggregation",
    version := aggregationVersion,
    commonSettings,
    libraryDependencies ++= aggregationDependencies ++ commonDependencies,
    dependencyOverrides ++= overrideDependencies,
    aggregationAssembly
  )
  .dependsOn(core % "compile->compile;test->test")

lazy val core = project
  .settings(
    name := "core",
    version := "2.35.0",
    commonSettings,
    libraryDependencies ++= commonDependencies,
    dependencyOverrides ++= overrideDependencies
  )
  .disablePlugins(AssemblyPlugin)

lazy val dependencies = new {
  private val kafkaVersion = "3.4.0"
  private val confluentConnectVersion = "7.4.4"
  private val scalajVersion = "2.4.2"
  private val slf4jVersion = "2.0.7"
  private val json4sVersion = "4.0.6"
  private val avroVersion = "1.11.3"
  private val avro4sVersion = "4.1.1"
  private val enumeratumVersion = "1.7.2"
  private val scalatestVersion = "3.2.16"
  private val scalaTestMockitoVersion = "3.2.10.0"
  private val jacksonVersion = "2.15.3"
  private val snakeyamlVersion = "2.0"
  private val commonsCompressVersion = "1.26.1"

  val kafka = "org.apache.kafka"             % "connect-api"                  % kafkaVersion
  val kafkaClients = "org.apache.kafka"      % "kafka-clients"                % kafkaVersion
  val confluentConnect = "io.confluent"      % "kafka-connect-avro-converter" % confluentConnectVersion
  val scalaj = "org.scalaj"                 %% "scalaj-http"                  % scalajVersion
  val slf4jApi = "org.slf4j"                 % "slf4j-api"                    % slf4jVersion
  val slf4jSimple = "org.slf4j"              % "slf4j-simple"                 % slf4jVersion
  val json4sNative = "org.json4s"           %% "json4s-native"                % json4sVersion
  val json4sJackson = "org.json4s"          %% "json4s-jackson"               % json4sVersion
  val avro = "org.apache.avro"               % "avro"                         % avroVersion
  val avro4s = "com.sksamuel.avro4s"        %% "avro4s-core"                  % avro4sVersion
  val enumeratum = "com.beachape"           %% "enumeratum"                   % enumeratumVersion
  val enumeratumJson4s = "com.beachape"     %% "enumeratum-json4s"            % enumeratumVersion
  val jackson = "com.fasterxml.jackson.core" % "jackson-databind"             % jacksonVersion
  val snakeyaml = "org.yaml"                 % "snakeyaml"                    % snakeyamlVersion
  val commonsCompress = "org.apache.commons" % "commons-compress"             % commonsCompressVersion
  val scalatest = "org.scalatest"           %% "scalatest-funspec"            % scalatestVersion        % Test
  val mockito = "org.scalatestplus"         %% "mockito-3-4"                  % scalaTestMockitoVersion % Test
}

lazy val commonDependencies = Seq(
  dependencies.kafka % "provided",
  dependencies.slf4jApi,
  dependencies.slf4jSimple,
  dependencies.kafkaClients,
  dependencies.confluentConnect,
  dependencies.avro,
  dependencies.enumeratum,
  dependencies.enumeratumJson4s,
  dependencies.scalatest,
  dependencies.mockito
)

lazy val mainDependencies = Seq(
  dependencies.scalaj,
  dependencies.json4sNative,
  dependencies.json4sJackson,
  dependencies.avro4s
)

lazy val overrideDependencies = Seq(
  dependencies.jackson,
  dependencies.snakeyaml,
  dependencies.avro,
  dependencies.commonsCompress
)

lazy val aggregationDependencies = Seq(
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

lazy val commonSettings = description ++ Seq(
  scalaVersion := "2.13.11",
  fork := true,
  parallelExecution in test := true,
  testForkedParallel in test := true,
  resolvers ++= Seq(
    "bintray-spark-packages" at "https://dl.bintray.com/spark-packages/maven",
    "Typesafe Simple Repository" at "https://repo.typesafe.com/typesafe/simple/maven-releases",
    "MavenRepository" at "https://mvnrepository.com",
    "confluent" at "https://packages.confluent.io/maven/"
  ),
  scalacOptions ++= compilerOptionsWithWarnings
)

lazy val aggregationMainAssembly = Seq(
  assemblyOutputPath in assembly := file(s"./artifacts/aggregation-main-connector_$aggregationMainVersion.jar"),
  test in assembly := {},
  assemblyMergeStrategy in assembly := {
    case x if x.contains("module-info.class") => MergeStrategy.concat
    case x =>
      val oldStrategy = (assemblyMergeStrategy in assembly).value
      oldStrategy(x)
  }
)

lazy val aggregationAssembly = Seq(
  assemblyOutputPath in assembly := file(s"./artifacts/aggregation-connector_$aggregationVersion.jar"),
  test in assembly := {},
  assemblyMergeStrategy in assembly := {
    case x if x.contains("module-info.class") => MergeStrategy.concat
    case x =>
      val oldStrategy = (assemblyMergeStrategy in assembly).value
      oldStrategy(x)
  }
)
