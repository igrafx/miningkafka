package com.igrafx.kafka.sink.aggregationmain.adapters.system

import com.igrafx.kafka.sink.aggregationmain.config.MainConfig
import com.igrafx.kafka.sink.aggregationmain.domain.entities.{CsvProperties, Event, Param, SrcDestFiles}
import com.igrafx.kafka.sink.aggregationmain.domain.exceptions.{
  FileCreationException,
  OldArchivedFilesDeletionException,
  ProjectPathInitializationException
}
import com.igrafx.kafka.sink.aggregationmain.domain.interfaces.MainSystem
import org.json4s._
import org.slf4j.{Logger, LoggerFactory}

import java.io._
import java.nio.file.{Files, Path, Paths, StandardOpenOption}
import java.util.UUID
import scala.annotation.tailrec
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.util.{Failure, Success, Try, Using}

class MainSystemImpl extends MainSystem {
  private val log: Logger = LoggerFactory.getLogger(getClass)

  implicit private val formats: DefaultFormats.type = org.json4s.DefaultFormats

  /** Method used to store data in a csv file
    *
    * @param csvProperties the iGrafx properties useful to write data in the file
    * @param filePath The path to the file to create and fill
    * @param lines Iterable with each line corresponding to a "block" of multiple events buffered in ksqlDB (an event being one line with caseId, activity, time...). All the lines are appended to the file that wil be sent to the iGrafx Mining API
    *
    * @throws FileCreationException issue with the creation or the writing of the file storing data. Nothing is sent to the iGrafx Mining API. Can embed an exception among : IOException, UnsupportedEncodingException, IllegalArgumentException, UnsupportedOperationException, SecurityException, DataException, AvroRuntimeException, Avro4sDecodingException, InvalidPathException or an other unexpected Throwable exception
    */
  override def appendToCsv(
      csvProperties: CsvProperties,
      filePath: Path,
      lines: Iterable[Event]
  ): Future[Int] = {
    Future {
      log.debug(
        s"[MainSystemImpl.appendToCsv] CSV PROPERTIES : Encoding => ${csvProperties.csvEncoding}, Separator => ${csvProperties.csvSeparator}, Quote => ${csvProperties.csvQuote}, Number of fields => ${csvProperties.csvFieldsNumber}, Header => ${csvProperties.csvHeader}, DefaultTextValue => ${csvProperties.csvDefaultTextValue}"
          .replaceAll("[\r\n]", "")
      )
      log.debug(s"[MainSystemImpl.appendToCsv] Writing lines to $filePath".replaceAll("[\r\n]", ""))

      val tryResult: Try[Int] = Using(
        new PrintStream(
          Files.newOutputStream(filePath, StandardOpenOption.CREATE, StandardOpenOption.APPEND),
          false,
          csvProperties.csvEncoding.toCharset.name()
        )
      ) { outputStream =>
        val file = filePath.toFile
        csvProperties.csvHeaderValue match {
          case Some(headerValue) =>
            if (file.exists() && file.length() == 0) {
              outputStream.println(headerValue)
            } else {
              throw new IOException(
                "Issue with the creation of the file storing data, file does not exists or is not empty. Hence we can't add headers to the file. Nothing is sent to the iGrafx Mining API"
              )
            }
          case None => ()
        }

        val lineNumberWritten: Int = lines.foldLeft(0) {
          case (lineNumberForRecordAcc: Int, event: Event) =>
            val parsedRecord = parsedForIGrafx(event, csvProperties)
            //log.debug(s"[MainSystemImpl.appendToCsv] Writing line to $filename : $parsedRecord")
            outputStream.println(
              parsedRecord
            )
            lineNumberForRecordAcc + 1
        }

        if (file.exists() && file.length() > 0) {
          if (csvProperties.csvHeader) {
            lineNumberWritten + 1
          } else {
            lineNumberWritten
          }
        } else {
          throw new IOException(
            "Issue with the creation of the file storing data, file does not exists or is empty. Nothing is sent to the iGrafx Mining API"
          )
        }
      }

      tryResult match {
        case Success(lineNumber) => lineNumber
        case Failure(exception: IOException) =>
          log.error(
            s"Couldn't find or create file '$filePath' for IGrafxSinkTask".replaceAll("[\r\n]", ""),
            exception
          )
          throw FileCreationException(
            new IOException(
              s"Couldn't find or create file '$filePath' for IGrafxSinkTask".replaceAll("[\r\n]", ""),
              exception
            )
          )
        case Failure(exception) =>
          log.error(
            s"Problem while trying to create or write data into '$filePath'".replaceAll("[\r\n]", ""),
            exception
          )
          throw FileCreationException(exception)
      }
    }
  }

  /** Method used to create the directories for the archived files and the failed files
    *
    * @param projectId the iGrafx projectId
    * @param connectorName The name of the connector
    *
    * @throws ProjectPathInitializationException If there is an issue during the initialization of the project paths, can embed an exception among UnsupportedOperationException, FileAlreadyExistsException, IOException, SecurityException, InvalidPathException or an other unexpected Throwable exception
    */
  override def initProjectPaths(projectId: UUID, connectorName: String): Future[Unit] = {
    Future {
      // Only create if the directory doesn't already exist, also create parent directories if needed
      val archivePath = s"${MainConfig.csvPath}/$projectId/$connectorName/archive/"
      Files.createDirectories(
        Paths.get(archivePath)
      )
      val failedPath = s"${MainConfig.csvPath}/$projectId/$connectorName/archiveFailed/"
      Files.createDirectories(
        Paths.get(failedPath)
      )

      ()
    } recover {
      case exception =>
        log.error("Problem with the project path initialization", exception)
        throw ProjectPathInitializationException(exception)
    }
  }

  /** Method used to delete old archive or failed files
    *
    * @param projectId the iGrafx projectId
    * @param connectorName The name of the connector
    * @param retentionTime number of days between the creation and the destruction of the file
    *
    * @throws NumberFormatException suppressFilesWithTooLowTimestamp method
    * @throws SecurityException suppressFilesWithTooLowTimestamp method
    * @throws IndexOutOfBoundsException suppressFilesWithTooLowTimestamp method
    *
    * @throws OldArchivedFilesDeletionException If there is an issue during the deletion of old archived files. Can embed exceptions among : NumberFormatException, SecurityException, IndexOutOfBoundsException or other unexpected Throwable exceptions
    */
  override def deleteOldArchivedFiles(projectId: UUID, connectorName: String, retentionTime: Int): Future[Unit] = {
    Future {
      val archivePath = Paths.get(s"${MainConfig.csvPath}/$projectId/$connectorName/archive/")
      val failedPath = Paths.get(s"${MainConfig.csvPath}/$projectId/$connectorName/archiveFailed/")
      val timestamp: Int = (System.currentTimeMillis() / 1000).toString.toInt

      suppressFilesWithTooLowTimestamp(archivePath, timestamp, retentionTime)
      suppressFilesWithTooLowTimestamp(failedPath, timestamp, retentionTime)

      ()
    }.recover {
      case exception =>
        log.error("Issue during the deletion of old archived files", exception)
        throw OldArchivedFilesDeletionException(exception)
    }
  }

  /** Method used to delete files out of date for a given directory
    *
    * @param directoryPath The path of the directory
    * @param timestamp The timestamp
    * @param retentionTime The time in days between the creation of a file and its deletion
    *
    * @throws NumberFormatException toInt method : If the string does not contain a parsable Int
    * @throws SecurityException In the exists, isDirectory, listFiles, isFile and delete methods : If a security manager exists and its SecurityManager.checkRead(String) method denies read access to the file or directory
    * @throws IndexOutOfBoundsException substring method : when start < 0, start > end or end > length()
    */
  private def suppressFilesWithTooLowTimestamp(directoryPath: Path, timestamp: Int, retentionTime: Int): Unit = {
    val directory = directoryPath.toFile
    if (directory.exists && directory.isDirectory) {
      directory.listFiles.foreach { file: File =>
        if (file.exists() && file.isFile) {
          val tabTmp = file.getName.split("_")
          val ts = tabTmp(tabTmp.size - 1) // Only the timestamp and its extension (without the path)
          val ficTimestamp: Int = ts.substring(0, ts.length - 4).toInt // Only the timestamp
          // 24hours = 86400seconds
          val timestampCompare = timestamp - retentionTime * 86400
          if (ficTimestamp < timestampCompare) {
            file.delete()
          }
        }
      }
    }
  }

  /** Method used to copy a file to the failed directory
    *
    * @param sourceFile The file to copy
    * @param projectId The iGrafx projectId
    * @param connectorName The name of the connector
    *
    * @throws SecurityException In the exists, File.length, isFile, delete and createNewFile methods : If a security manager exists and its SecurityManager.checkRead(String) method denies read access to the file or directory
    * @throws IndexOutOfBoundsException substring method : when start < 0, start > end or end > length()
    */
  private[adapters] def storeFailedFile(
      sourceFile: File,
      projectId: UUID,
      connectorName: String
  ): Future[Unit] = {
    Future {
      val failedFile =
        Paths.get(s"${MainConfig.csvPath}/$projectId/$connectorName/archiveFailed/${sourceFile.getName}").toFile

      if (sourceFile.exists() && sourceFile.isFile) {
        fileCopy(sourceFile, failedFile)
      }

      ()
    } recover {
      case exception =>
        log.error(
          s"Issue during the copy of the file $sourceFile to the failed repository".replaceAll("[\r\n]", ""),
          exception
        )
        throw exception
    }
  }

  // ----------- Utility methods -----------

  /** Method used to transform a List[Param] corresponding to an event into a string corresponding to the line with the good csv format
    *
    * Example :
    *
    * If in input we have :
    * List(Param(QUOTE = true, TEXT = "activity", COLUMNID = 1), Param(QUOTE = false, TEXT = "caseId", COLUMNID = 0), Param(QUOTE = false, TEXT = "endDate", COLUMNID = 3))
    *
    * The method will create the following the result string : caseId,"activity",null,endDate
    * if in the connector's properties :
    *      the csvSeparator = ','
    *      the csvQuote = '"'
    *      the csvDefaultTextValue = null
    *
    * @param eventWithParams Event corresponding to an event (List[Param])
    * @param csvProperties csv properties of the file we want to send to the iGrafx Mining API
    *
    * @throws IllegalArgumentException defaultTextValueRec : if the number parameter is < 0
    */
  private[aggregationmain] def parsedForIGrafx(eventWithParams: Event, csvProperties: CsvProperties): String = {
    val paramsSorted: Seq[Param] = eventWithParams.event.toSeq.sortBy(param => param.COLUMNID)

    val result: (String, Int) = paramsSorted
      .foldLeft(("", 0)) { (acc: (String, Int), param: Param) =>
        val suffix = if (param.QUOTE) {
          s"${csvProperties.csvQuote}${param.TEXT}${csvProperties.csvQuote}"
        } else {
          s"${param.TEXT}"
        }
        acc._2 match {
          case 0 =>
            if (param.COLUMNID == 0) {
              (s"${acc._1}$suffix", acc._2 + 1)
            } else {
              (
                s"${acc._1}${csvProperties.csvDefaultTextValue}${defaultTextValuesRec("", param.COLUMNID - 1, csvProperties)}${csvProperties.csvSeparator}$suffix",
                param.COLUMNID + 1
              )
            }
          case currIndex =>
            (
              s"${acc._1}${defaultTextValuesRec("", param.COLUMNID - currIndex, csvProperties)}${csvProperties.csvSeparator}$suffix",
              param.COLUMNID + 1
            )
        }
      }

    s"${result._1}${defaultTextValuesRec("", csvProperties.csvFieldsNumber.number - result._2, csvProperties)}"
  }

  /**
    * @param textValuesAcc accumulator containing all default values already added
    * @param number number of default values we have yet to add to textValuesAcc, must be >= 0
    * @param csvProperties the csv properties of the connector
    *
    * @throws IllegalArgumentException if number < 0
    */
  @tailrec
  private[aggregationmain] final def defaultTextValuesRec(
      textValuesAcc: String,
      number: Int,
      csvProperties: CsvProperties
  ): String = {
    number match {
      case 0 => textValuesAcc
      case value if value < 0 =>
        log.error(
          s"[IGrafxSinkConnector.defaultTextValuesRec] IllegalArgumentException : Issue with the number of default text values to add : $number , and can't be inferior than 0"
            .replaceAll("[\r\n]", "")
        )
        throw new IllegalArgumentException(
          s"Issue with the number of default text values to add : $number , and can't be inferior than 0"
            .replaceAll("[\r\n]", "")
        )
      case _ =>
        defaultTextValuesRec(
          s"$textValuesAcc${csvProperties.csvSeparator}${csvProperties.csvDefaultTextValue}",
          number - 1,
          csvProperties
        )
    }
  }

  /** Utility method called to copy a source file to a destination file
    *
    * @param srcPath path to the source file
    * @param destPath path to the destination file
    *
    * @throws SecurityException getSrcAndDestFiles method : If a security manager exists and its SecurityManager.checkRead(String) method denies read access to the file or directory
    * @throws IOException getSrcAndDestFiles and copyRec methods : If an I/O error occurred
    */
  private def fileCopy(srcPath: File, destPath: File): Unit = {
    val srcAndDestFiles: SrcDestFiles = Try[SrcDestFiles] {
      getSrcAndDestFiles(srcPath, destPath)
    } match {
      case Success(srcAndDestFilesResult) => srcAndDestFilesResult
      case Failure(exception) =>
        log.error(
          s"Problem while creating File for source ($srcPath) and destination ($destPath) files"
            .replaceAll("[\r\n]", ""),
          exception
        )
        throw exception
    }

    val tryResult: Try[Unit] = Using.Manager { use =>
      val fileReader: FileReader = use(new FileReader(srcAndDestFiles.src))
      val bufferedReader: BufferedReader = use(new BufferedReader(fileReader))
      val fileWriter: FileWriter = use(new FileWriter(srcAndDestFiles.dest))
      val bufferedWriter: BufferedWriter = use(new BufferedWriter(fileWriter))
      fileCopyRec(bufferedReader, bufferedWriter)
    }

    tryResult match {
      case Success(_) => ()
      case Failure(exception) =>
        log.error(
          s"Problem while copying $srcPath to $destPath".replaceAll("[\r\n]", ""),
          exception
        )
        throw exception
    }
  }

  /** Utility method returning source and destination "File" and creating the destination file if it doesn't already exist
    *
    * @param src the path to the source file
    * @param dest the path to the destination file
    *
    * @throws SecurityException In the exists and createNewFile methods : If a security manager exists and its SecurityManager.checkRead(String) method denies read access to the file or directory
    * @throws IOException In the createNewFile method : If an I/O error occurred
    */
  private def getSrcAndDestFiles(src: File, dest: File): SrcDestFiles = {
    if (!dest.exists()) {
      dest.createNewFile()
    }
    SrcDestFiles(src, dest)
  }

  /** Recursive method, copy the content of the source file into the destination file
    *
    * @param bufferedReader the BufferedReader of the source file
    * @param bufferedWriter the BufferedWriter of the destination file
    *
    * @throws IOException In the readLine, write and flush methods : If an I/O error occurs
    */
  @tailrec
  private def fileCopyRec(bufferedReader: BufferedReader, bufferedWriter: BufferedWriter): Unit = {
    val lineOption: Option[String] = Option(bufferedReader.readLine())

    lineOption match {
      case Some(line) =>
        bufferedWriter.write(line)
        bufferedWriter.write("\n")
        fileCopyRec(bufferedReader, bufferedWriter)
      case None =>
        bufferedWriter.flush()
        () //cas d'arrêt de la récursion
    }
  }
}
