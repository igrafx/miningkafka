package com.igrafx.kafka.sink.aggregationmain.domain.columnmapping.domain.entities

import com.igrafx.core.Mock

import java.util.UUID

final class ColumnMappingConfigurationPropertiesMock extends Mock[ColumnMappingConfigurationProperties] {
  private var apiUrl: String = "apiUrl"
  private var authUrl: String = "authUrl"
  private var workgroupId: String = "workGroupId"
  private var workgroupKey: String = "workGroupKey"
  private var projectId: UUID = UUID.fromString("0f61e0f3-68c8-4c7f-bc1d-a4aabf7b74e5")

  def setApiUrl(apiUrl: String): ColumnMappingConfigurationPropertiesMock = {
    this.apiUrl = apiUrl
    this
  }

  def setAuthUrl(authUrl: String): ColumnMappingConfigurationPropertiesMock = {
    this.authUrl = authUrl
    this
  }

  def setWorkgroupId(workgroupId: String): ColumnMappingConfigurationPropertiesMock = {
    this.workgroupId = workgroupId
    this
  }

  def setWorkgroupKey(workgroupKey: String): ColumnMappingConfigurationPropertiesMock = {
    this.workgroupKey = workgroupKey
    this
  }

  def setProjectId(projectId: UUID): ColumnMappingConfigurationPropertiesMock = {
    this.projectId = projectId
    this
  }

  override def build(): ColumnMappingConfigurationProperties = {
    ColumnMappingConfigurationProperties(
      apiUrl = apiUrl,
      authUrl = authUrl,
      workgroupId = workgroupId,
      workgroupKey = workgroupKey,
      projectId = projectId
    )
  }
}
