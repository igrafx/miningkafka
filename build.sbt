scalaVersion := "2.13.5"
version := "0.1.0"
organization := "com.logpickr"
organizationName := "Logpickr"
organizationHomepage := Some(url("https://www.logpickr.com"))

lazy val dependencies = new {
  private val kafkaVersion = "2.7.0"
  private val ksqldbUdfVersion = "6.1.1"
  private val scalatestVersion = "3.2.6"
  private val scalaTestMockitoVersion = "3.2.5.0"

  val kafka = "org.apache.kafka"     %% "kafka"             % kafkaVersion
  val kafkaApi = "org.apache.kafka"   % "connect-api"       % kafkaVersion
  val ksqldbUdf = "io.confluent.ksql" % "ksqldb-udf"        % ksqldbUdfVersion
  val scalatest = "org.scalatest"    %% "scalatest-funspec" % scalatestVersion        % Test
  val mockito = "org.scalatestplus"  %% "mockito-3-4"       % scalaTestMockitoVersion % Test
}

libraryDependencies ++= Seq(
  dependencies.kafka,
  dependencies.kafkaApi,
  dependencies.ksqldbUdf,
  dependencies.scalatest,
  dependencies.mockito
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