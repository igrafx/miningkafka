package com.igrafx.kafka.sink.aggregationmain.domain.columnmapping.domain.entities

import com.igrafx.core.Mock

final class ColumnMappingPropertiesMock extends Mock[ColumnMappingProperties] {
  private var configurationProperties: ColumnMappingConfigurationProperties =
    new ColumnMappingConfigurationPropertiesMock().build()
  private var columnMapping: ValidColumnMapping = new ValidColumnMappingMock().build()
  private var fileStructure: FileStructure = new FileStructureMock().build()

  def setConfigurationProperties(
      configurationProperties: ColumnMappingConfigurationProperties
  ): ColumnMappingPropertiesMock = {
    this.configurationProperties = configurationProperties
    this
  }

  def setColumnMapping(columnMapping: ValidColumnMapping): ColumnMappingPropertiesMock = {
    this.columnMapping = columnMapping
    this
  }

  def setFileStructure(fileStructure: FileStructure): ColumnMappingPropertiesMock = {
    this.fileStructure = fileStructure
    this
  }

  override def build(): ColumnMappingProperties = {
    ColumnMappingProperties(
      configurationProperties = configurationProperties,
      columnMapping = columnMapping,
      fileStructure = fileStructure
    )
  }
}
