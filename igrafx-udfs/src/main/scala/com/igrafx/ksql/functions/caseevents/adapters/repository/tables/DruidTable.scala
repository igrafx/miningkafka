package com.igrafx.ksql.functions.caseevents.adapters.repository.tables

import org.jooq.{Field, Record, Table}

import java.sql.Timestamp
import java.util.UUID

trait DruidTable {
  val __time: Field[Timestamp]
  val __timeAsInsert: String = "startdate"
  val postfix: String

  def generateTableName(projectId: UUID): String = s"$projectId$postfix"

  def getTable(projectId: UUID): Table[Record] = tableEscape(generateTableName(projectId))

  def tableEscape(sql: String): Table[Record] = org.jooq.impl.DSL.table("\"" + sql + "\"")
}
