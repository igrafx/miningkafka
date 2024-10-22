package com.igrafx.core.adapters.druid.interfaces

import com.igrafx.core.adapters.druid.exceptions.{DruidException, UnknownDruidSqlTypeException}
import org.jooq.{Record, ResultQuery}
import org.json4s.JsonAST.JValue

import scala.concurrent.Future

trait DruidClient {
  @throws[DruidException]
  def sendSqlJsonQuery(jsonQuery: String, basicAuth: String, host: String, port: String): Future[JValue]

  @throws[UnknownDruidSqlTypeException]
  @throws[DruidException]
  def sendSqlQuery[R <: Record](query: ResultQuery[R], basicAuth: String, host: String, port: String): Future[JValue]
}
