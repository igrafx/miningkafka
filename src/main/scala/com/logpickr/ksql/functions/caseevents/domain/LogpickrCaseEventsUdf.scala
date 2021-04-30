package com.logpickr.ksql.functions.caseevents.domain

import com.logpickr.ksql.functions.caseevents.Constants.{structEndDate, structStartDate, structVertexName}
import com.logpickr.ksql.functions.caseevents.adapters.repository.LogpickrCaseEventsRepositoryImpl
import com.logpickr.ksql.functions.caseevents.domain.entities.CaseEventsResponseInformation
import com.logpickr.ksql.functions.caseevents.domain.exceptions.CaseEventsException
import com.logpickr.ksql.functions.caseevents.domain.interfaces.LogpickrCaseEventsRepository
import com.logpickr.ksql.functions.caseevents.domain.structs.CaseEventsStructs
import io.confluent.ksql.function.udf.{Udf, UdfDescription, UdfParameter}
import org.apache.kafka.connect.data.Struct
import org.json4s.DefaultFormats

import java.util
import org.slf4j.{Logger, LoggerFactory}

import java.util.UUID
import scala.util.{Failure, Success, Try}
import scala.jdk.CollectionConverters._

@UdfDescription(
  name = "logpickr_case_events",
  author = "Logpickr",
  description = "Udf retrieving information about a caseId from Druid"
)
class LogpickrCaseEventsUdf {
  private val log: Logger = LoggerFactory.getLogger(getClass)
  implicit private val formats: DefaultFormats.type = org.json4s.DefaultFormats

  private val druidRequest: LogpickrCaseEventsRepository = new LogpickrCaseEventsRepositoryImpl

  @Udf(
    description = "Returns information from the _vertex Datasource related to a given caseId",
    schema = "ARRAY<" + CaseEventsStructs.STRUCT_SCHEMA_DESCRIPTOR + ">"
  )
  def logpickrCaseEvents(
      @UdfParameter(value = "caseId", description = "The caseId for which we want to get information") caseId: String,
      @UdfParameter(
        value = "projectId",
        description = "The id of the Logpickr project containing the information"
      ) projectId: String,
      @UdfParameter(
        value = "workgroupId",
        description = "The id of the Logpickr workgroup related to the project containing the information"
      ) workgroupId: String,
      @UdfParameter(
        value = "workgroupKey",
        description = "The key of the Logpickr workgroup related to the project containing the information"
      ) workgroupKey: String,
      @UdfParameter(value = "host", description = "Corresponds to the Druid host") host: String,
      @UdfParameter(value = "port", description = "Corresponds to the Druid connexion port") port: String
  ): util.List[Struct] = {
    val projectUuid = Try {
      UUID.fromString(projectId)
    } match {
      case Success(value) => value
      case Failure(exception: IllegalArgumentException) =>
        log.error(
          s"Couldn't retrieve information about the caseId $caseId because the projectId argument does not correspond to UUID format",
          exception
        )
        throw CaseEventsException(exception.getMessage)
    }

    Try {
      val response = druidRequest.getCaseIdInformation(caseId, projectUuid, workgroupId, workgroupKey, host, port)
      response.map { druidInformation: CaseEventsResponseInformation =>
        val result = new Struct(CaseEventsStructs.STRUCT_SCHEMA)
        result.put(structStartDate, druidInformation.startDate)
        result.put(structEndDate, druidInformation.endDate)
        result.put(structVertexName, druidInformation.vertexName)
        result
      }.asJava
    } match {
      case Success(result) => result
      case Failure(exception) =>
        log.error(s"Couldn't retrieve information about the caseId $caseId", exception)
        throw CaseEventsException(exception.getMessage)
    }
  }
}
