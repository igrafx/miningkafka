package com.igrafx.kafka.sink.aggregationmain.domain.columnmapping.controllers.dtos

import com.igrafx.kafka.sink.aggregationmain.domain.columnmapping.domain.entities.ColumnMappingConfigurationProperties

import java.util.UUID

final case class ColumnMappingConfigurationPropertiesDto(
    apiUrl: String,
    authUrl: String,
    workGroupId: String,
    workGroupKey: String,
    projectId: UUID
) {
  def toEntity: ColumnMappingConfigurationProperties = {
    ColumnMappingConfigurationProperties(
      apiUrl = apiUrl,
      authUrl = authUrl,
      workgroupId = workGroupId,
      workgroupKey = workGroupKey,
      projectId = projectId
    )
  }
}
