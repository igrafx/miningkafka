package com.logpickr.ksql.functions.caseevents.adapters.repository

import com.logpickr.core.adapters.druid.DruidClientInstance
import com.logpickr.core.adapters.druid.exceptions.{DruidException, UnknownDruidSqlTypeException}
import com.logpickr.core.adapters.druid.interfaces.DruidClient
import com.logpickr.ksql.functions.caseevents.adapters.repository.tables.VertexTable.{
  __time,
  caseid,
  enddate,
  getTable,
  vertex_name
}
import com.logpickr.ksql.functions.caseevents.adapters.repository.dtos.CaseEventsResponseInformationDto
import com.logpickr.ksql.functions.caseevents.domain.entities.CaseEventsResponseInformation
import com.logpickr.ksql.functions.caseevents.domain.interfaces.LogpickrCaseEventsRepository
import org.jooq.impl.DSL.select
import org.json4s.{DefaultFormats, MappingException}
import org.slf4j.{Logger, LoggerFactory}
import scalaj.http.Base64

import java.util.UUID
import scala.concurrent.Await
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.DurationInt

class LogpickrCaseEventsRepositoryImpl(private val druidClient: DruidClient = DruidClientInstance.druidClient)
    extends LogpickrCaseEventsRepository {
  private val log: Logger = LoggerFactory.getLogger(getClass)
  implicit private val formats: DefaultFormats.type = org.json4s.DefaultFormats

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
  ): List[CaseEventsResponseInformation] = {
    val query = (
      // format: off
      select(__time as CaseEventsResponseInformationDto.startDateAlias, enddate as CaseEventsResponseInformationDto.endDateAlias, vertex_name as CaseEventsResponseInformationDto.vertexNameAlias)
        from getTable(projectUuid)
        where vertex_name.isNotNull and caseid.eq(s"$caseId")
      // format: on
    )
    val basicAuth = Base64.encodeString(s"$workgroupId:$workgroupKey")

    def sqlQueryFuture =
      druidClient.sendSqlQuery(query, basicAuth, host, port).recover {
        case exception =>
          log.error(
            s"[UDF logpickr_case_events] Problem with the sql request used to get information for the caseId : $caseId"
              .replaceAll("[\r\n]", "")
          )
          throw exception
      }

    val response: List[CaseEventsResponseInformationDto] = {
      Await.result(sqlQueryFuture, 10 seconds).children.map(_.extract[CaseEventsResponseInformationDto])
    }
    response.map(_.toCaseEventsResponseInformation)
  }
}
