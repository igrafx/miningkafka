package com.igrafx.kafka.sink.aggregationmain.domain.columnmapping.domain.entities

import java.util.UUID

final case class ColumnMappingConfigurationProperties(
    apiUrl: String,
    authUrl: String,
    workgroupId: String,
    workgroupKey: String,
    projectId: UUID
)
