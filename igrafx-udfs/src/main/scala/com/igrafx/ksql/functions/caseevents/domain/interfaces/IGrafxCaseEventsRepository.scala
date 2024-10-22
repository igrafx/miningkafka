package com.igrafx.ksql.functions.caseevents.domain.interfaces

import com.igrafx.core.adapters.druid.exceptions.{DruidException, UnknownDruidSqlTypeException}
import com.igrafx.ksql.functions.caseevents.domain.entities.CaseEventsResponseInformation
import org.json4s.MappingException

import java.util.UUID

trait IGrafxCaseEventsRepository {

  @throws[UnknownDruidSqlTypeException]
  @throws[DruidException]
  @throws[InterruptedException]
  @throws[concurrent.TimeoutException]
  @throws[IllegalArgumentException]
  @throws[MappingException]
  def getCaseIdInformation(
      caseId: String,
      projectUuid: UUID,
      workgroupId: String,
      workgroupKey: String,
      host: String,
      port: String
  ): List[CaseEventsResponseInformation]
}
