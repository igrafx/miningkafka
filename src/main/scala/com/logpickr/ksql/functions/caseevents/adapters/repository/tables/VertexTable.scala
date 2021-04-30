package com.logpickr.ksql.functions.caseevents.adapters.repository.tables

import org.jooq.impl.DSL._
import org.jooq.impl.SQLDataType._
import org.jooq.{Condition, Field}

import java.lang
import java.sql.Timestamp
import scala.collection.JavaConverters._

object VertexTable extends DruidTable {
  val __time: Field[Timestamp] = field("__time", TIMESTAMP)
  val caseid: Field[String] = field("caseid", VARCHAR)
  val concurrent_vertices: Field[String] = field("concurrent_vertices", VARCHAR)
  val duration: Field[lang.Long] = field("duration", BIGINT)
  val enddate: Field[String] = field("enddate", VARCHAR)
  val graphkey: Field[String] = field("graphkey", VARCHAR)
  val processkey: Field[String] = field("processkey", VARCHAR)
  val total_occurences_by_caseid: Field[lang.Long] = field("total_occurences_by_caseid", BIGINT)
  val total_occurences_by_caseid_null_value: Field[String] = field("total_occurences_by_caseid_null_value", VARCHAR)
  val vertex_id: Field[String] = field("vertex_id", VARCHAR)
  val vertex_name: Field[String] = field("vertex_name", VARCHAR)
  val postfix: String = "_vertex"
}
