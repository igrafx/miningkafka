package com.logpickr.ksql.functions.caseevents.domain.interfaces

import com.logpickr.core.adapters.druid.exceptions.{DruidException, UnknownDruidSqlTypeException}
import com.logpickr.ksql.functions.caseevents.domain.entities.CaseEventsResponseInformation
import org.json4s.MappingException

import java.util.UUID

trait LogpickrCaseEventsRepository {

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
