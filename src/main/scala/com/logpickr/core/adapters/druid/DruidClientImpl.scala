package com.logpickr.core.adapters.druid

import com.logpickr.core.adapters.druid.exceptions.{DruidException, UnknownDruidSqlTypeException}
import com.logpickr.core.adapters.druid.interfaces.DruidClient
import com.logpickr.ksql.functions.caseevents.Constants.{sqlJsonParameterTypeName, sqlJsonParameterValueName}
import org.jooq.{Record, ResultQuery}
import org.json4s.JsonDSL._
import org.json4s.jackson.JsonMethods.compact
import org.json4s.native.JsonMethods._
import org.json4s.{DefaultFormats, JValue}
import org.slf4j.{Logger, LoggerFactory}
import scalaj.http.{Http, HttpOptions, HttpResponse}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.jdk.CollectionConverters._

class DruidClientImpl extends DruidClient {
  private val log: Logger = LoggerFactory.getLogger(getClass)

  implicit private val formats: DefaultFormats.type = org.json4s.DefaultFormats

  @throws[UnknownDruidSqlTypeException]
  @throws[DruidException]
  override def sendSqlQuery[R <: Record](
      query: ResultQuery[R],
      basicAuth: String,
      host: String,
      port: String
  ): Future[JValue] = {
    val jsonQuery = sqlToJsonQuery(query)

    sendSqlJsonQuery(jsonQuery, basicAuth, host, port)
  }

  @throws[DruidException]
  override def sendSqlJsonQuery(jsonQuery: String, basicAuth: String, host: String, port: String): Future[JValue] = {
    val uri = s"http://$host:$port/druid/v2/sql"
    sendJsonQuery(uri, jsonQuery, basicAuth)
  }

  @throws[UnknownDruidSqlTypeException]
  private def sqlToJsonQuery[R <: Record](query: ResultQuery[R]): String = {
    val jsonParameters = getSqlJsonParameters(query)
    val formattedParametersOption = if (jsonParameters.isEmpty) None else Some("parameters" -> jsonParameters)

    val jsonQuery: JValue = formattedParametersOption match {
      case Some(formattedParameters) => ("query" -> query.getSQL) ~ formattedParameters
      case _ => "query" -> query.getSQL
    }

    compact(jsonQuery)
  }

  @throws[UnknownDruidSqlTypeException]
  private def getSqlJsonParameters[R <: Record](query: ResultQuery[R]): Seq[JValue] = {
    query.getParams.asScala.toSeq.map {
      case (_, parameter) =>
        val dataType = parameter.getDataType.getTypeName match {
          case "numeric" => "BIGINT"
          case other => other.toUpperCase
        }

        parameter.getValue match {
          case string: String => (sqlJsonParameterTypeName -> dataType) ~ (sqlJsonParameterValueName -> string)
          case bigInt: java.math.BigInteger =>
            (sqlJsonParameterTypeName -> dataType) ~ (sqlJsonParameterValueName -> BigInt(bigInt))
          case bigDecimal: java.math.BigDecimal =>
            (sqlJsonParameterTypeName -> dataType) ~ (sqlJsonParameterValueName -> BigDecimal(bigDecimal))
          case double: Double => (sqlJsonParameterTypeName -> dataType) ~ (sqlJsonParameterValueName -> double)
          case int: Int => (sqlJsonParameterTypeName -> dataType) ~ (sqlJsonParameterValueName -> int)
          case dateTime: org.joda.time.DateTime =>
            (sqlJsonParameterTypeName -> dataType) ~ (sqlJsonParameterValueName -> dateTime.toString)
          case timestamp: java.sql.Timestamp =>
            (sqlJsonParameterTypeName -> dataType) ~ (sqlJsonParameterValueName -> timestamp.getTime)
          case long: java.lang.Long =>
            (sqlJsonParameterTypeName -> dataType) ~ (sqlJsonParameterValueName -> long.toLong)
          case unknown =>
            throw UnknownDruidSqlTypeException(s"Type ${unknown.getClass} is not handle in Druid SQL requests.")
        }
    }
  }

  @throws[DruidException]
  private def sendJsonQuery(
      uri: String,
      jsonQuery: String,
      basicAuth: String,
      errorAttribute: String = "errorMessage"
  ): Future[JValue] = {
    Future {
      Http(uri)
        .header("Authorization", s"Basic $basicAuth")
        .header("content-type", "application/json")
        .postData(jsonQuery)
        .option(HttpOptions.readTimeout(10000))
        .asString
    }.map { druidResponse: HttpResponse[String] =>
      val parsedResponse = parse(druidResponse.body)
      if (druidResponse.isSuccess) {
        parsedResponse
      } else {
        val errorDescription = (parsedResponse \\ errorAttribute).extract[String]
        log.error(
          s"[UDF logpickr_case_events] Error in the Http Response : $errorDescription for the query : $jsonQuery"
            .replaceAll("[\r\n]", "")
        )
        throw DruidException(errorDescription)
      }
    } recover {
      case exception =>
        log.error(
          s"[UDF logpickr_case_events] Problem with the Http Request for the query $jsonQuery".replaceAll("[\r\n]", ""),
          exception
        )
        throw exception
    }
  }
}
