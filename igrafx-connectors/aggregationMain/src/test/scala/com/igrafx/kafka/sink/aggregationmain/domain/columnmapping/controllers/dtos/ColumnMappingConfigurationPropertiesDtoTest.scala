package com.igrafx.kafka.sink.aggregationmain.domain.columnmapping.controllers.dtos

import com.igrafx.kafka.sink.aggregationmain.domain.columnmapping.domain.entities.ColumnMappingConfigurationPropertiesMock
import core.UnitTestSpec

import java.util.UUID

final class ColumnMappingConfigurationPropertiesDtoTest extends UnitTestSpec {
  describe("toEntity") {
    it("should return a ColumnMappingConfigurationProperties") {
      val apiUrl: String = "apiUrl"
      val authUrl: String = "authUrl"
      val workgroupId: String = "workGroupId"
      val workgroupKey: String = "workGroupKey"
      val projectId: UUID = UUID.fromString("0f61e0f3-68c8-4c7f-bc1d-a4aabf7b74e5")

      val columnMappingConfigurationPropertiesDto = ColumnMappingConfigurationPropertiesDto(
        apiUrl = apiUrl,
        authUrl = authUrl,
        workGroupId = workgroupId,
        workGroupKey = workgroupKey,
        projectId = projectId
      )

      val expectedResult = new ColumnMappingConfigurationPropertiesMock()
        .setApiUrl(apiUrl)
        .setAuthUrl(authUrl)
        .setWorkgroupId(workgroupId)
        .setWorkgroupKey(workgroupKey)
        .setProjectId(projectId)
        .build()

      val res = columnMappingConfigurationPropertiesDto.toEntity

      assert(res == expectedResult)
    }
  }
}
