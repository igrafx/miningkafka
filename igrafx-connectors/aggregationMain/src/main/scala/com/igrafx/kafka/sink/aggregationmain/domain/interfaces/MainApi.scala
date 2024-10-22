package com.igrafx.kafka.sink.aggregationmain.domain.interfaces

import com.igrafx.kafka.sink.aggregationmain.domain.columnmapping.domain.entities.ColumnMappingProperties
import com.igrafx.kafka.sink.aggregationmain.domain.entities.Properties
import com.igrafx.kafka.sink.aggregationmain.domain.exceptions.{
  ColumnMappingAlreadyExistsException,
  ColumnMappingCreationException,
  InvalidTokenException,
  SendFileException
}

import java.io.File
import scala.concurrent.Future

trait MainApi {

  @throws[InvalidTokenException]
  @throws[SendFileException]
  def sendCsvToIGrafx(properties: Properties, archivedFile: File): Future[Unit]

  @throws[InvalidTokenException]
  @throws[ColumnMappingCreationException]
  @throws[ColumnMappingAlreadyExistsException]
  def createColumnMapping(columnMappingProperties: ColumnMappingProperties): Future[Unit]
}
