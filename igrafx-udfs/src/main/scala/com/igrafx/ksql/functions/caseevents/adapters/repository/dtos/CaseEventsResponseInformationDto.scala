package com.igrafx.ksql.functions.caseevents.adapters.repository.dtos

import com.igrafx.ksql.functions.caseevents.domain.entities.CaseEventsResponseInformation

protected[repository] final case class CaseEventsResponseInformationDto(
    startDate: String,
    endDate: String,
    vertexName: String
)

protected[repository] object CaseEventsResponseInformationDto {
  val startDateAlias: String = "startDate"
  val endDateAlias: String = "endDate"
  val vertexNameAlias: String = "vertexName"

  implicit class CaseEventsResponseInformationDtoToCaseEventsResponseInformation(
      caseEventsResponseInformationDto: CaseEventsResponseInformationDto
  ) {
    def toCaseEventsResponseInformation: CaseEventsResponseInformation = {
      caseEventsResponseInformationDtoTocaseEventsResponseInformation(caseEventsResponseInformationDto)
    }
  }

  private def caseEventsResponseInformationDtoTocaseEventsResponseInformation(
      caseEventsResponseInformationDto: CaseEventsResponseInformationDto
  ): CaseEventsResponseInformation = {
    CaseEventsResponseInformation(
      startDate = caseEventsResponseInformationDto.startDate,
      endDate = caseEventsResponseInformationDto.endDate,
      vertexName = caseEventsResponseInformationDto.vertexName
    )
  }
}
