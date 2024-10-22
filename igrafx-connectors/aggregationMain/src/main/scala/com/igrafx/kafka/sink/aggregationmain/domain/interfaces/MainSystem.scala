package com.igrafx.kafka.sink.aggregationmain.domain.interfaces

import com.igrafx.kafka.sink.aggregationmain.domain.entities.{CsvProperties, Event}
import com.igrafx.kafka.sink.aggregationmain.domain.exceptions.{
  FileCreationException,
  OldArchivedFilesDeletionException,
  ProjectPathInitializationException
}

import java.nio.file.Path
import java.util.UUID
import scala.concurrent.Future

trait MainSystem {

  @throws[FileCreationException]
  def appendToCsv(csvProperties: CsvProperties, filePath: Path, lines: Iterable[Event]): Future[Int]

  @throws[ProjectPathInitializationException]
  def initProjectPaths(projectId: UUID, connectorName: String): Future[Unit]

  @throws[OldArchivedFilesDeletionException]
  def deleteOldArchivedFiles(projectId: UUID, connectorName: String, retentionTime: Int): Future[Unit]
}
