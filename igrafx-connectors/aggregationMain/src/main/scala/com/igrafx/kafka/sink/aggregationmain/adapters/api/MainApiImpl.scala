package com.igrafx.kafka.sink.aggregationmain.adapters.api

import com.igrafx.kafka.sink.aggregationmain.Constants
import com.igrafx.kafka.sink.aggregationmain.adapters.system.MainSystemImpl
import com.igrafx.kafka.sink.aggregationmain.config.MainConfig
import com.igrafx.kafka.sink.aggregationmain.domain.columnmapping.domain.entities.ColumnMappingProperties
import com.igrafx.kafka.sink.aggregationmain.domain.entities.Properties
import com.igrafx.kafka.sink.aggregationmain.domain.exceptions.{
  ColumnMappingAlreadyExistsException,
  ColumnMappingCreationException,
  InvalidTokenException,
  SendFileException
}
import com.igrafx.kafka.sink.aggregationmain.domain.interfaces.MainApi
import org.json4s._
import org.json4s.jackson.JsonMethods.{pretty, render}
import org.json4s.native.JsonMethods._
import org.slf4j.{Logger, LoggerFactory}
import scalaj.http.{Http, HttpOptions, MultiPart}

import java.io.File
import java.nio.file.Files
import java.util.UUID
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.util.{Failure, Success, Try}

class MainApiImpl extends MainApi {
  private val log: Logger = LoggerFactory.getLogger(getClass)

  implicit private val formats: DefaultFormats.type = org.json4s.DefaultFormats

  /**  Method called to send a csv file to the iGrafx Mining API for a given project
    *
    * @param properties properties of the connector used to call the API (apiUrl, authUrl, workGroupId, workGroupKey, projectId)
    * @param archivedFile path of the file we want to send
    *
    * @throws InvalidTokenException In case of an issue with the Authentication a InvalidTokenException is thrown
    * @throws SendFileException In case of an issue during the sending of the File, a SendFileException is thrown
    */
  override def sendCsvToIGrafx(properties: Properties, archivedFile: File): Future[Unit] = {

    val finalApiUrl: String = getPublicApiUrl(properties.apiUrl)
    (for {
      token <- sendAuthentication(properties.authUrl, properties.workGroupId, properties.workGroupKey)
      _ <- sendFile(finalApiUrl, properties.workGroupId, properties.projectId, archivedFile, token)
    } yield ()) recover {
      case exception: InvalidTokenException =>
        val mainSystem = new MainSystemImpl
        mainSystem.storeFailedFile(
          archivedFile,
          properties.projectId,
          properties.connectorName
        )
        if (exception.canRetry) {
          log.error(
            s"Couldn't Authenticate to iGrafx because of a Server issue. The file $archivedFile is stored under the '${MainConfig.csvPath}/${properties.projectId}/${properties.connectorName}/archiveFailed/' repository"
              .replaceAll("[\r\n]", "")
          )
        } else {
          log.error(
            s"Couldn't Authenticate to iGrafx platform. The file $archivedFile is stored under the '${MainConfig.csvPath}/${properties.projectId}/${properties.connectorName}/archiveFailed/' repository"
              .replaceAll("[\r\n]", "")
          )
        }
        throw exception // to stop the Task
      case exception: SendFileException =>
        val mainSystem = new MainSystemImpl
        mainSystem.storeFailedFile(
          archivedFile,
          properties.projectId,
          properties.connectorName
        )
        if (exception.canRetry) {
          log.error(
            s"Couldn't send the file $archivedFile to the iGrafx API because of a Server issue, therefore the file is stored under the '${MainConfig.csvPath}/${properties.projectId}/${properties.connectorName}/archiveFailed/' repository"
              .replaceAll("[\r\n]", "")
          )
        } else {
          log.error(
            s"Couldn't send the file $archivedFile to the iGrafx API, therefore the file is stored under the '${MainConfig.csvPath}/${properties.projectId}/${properties.connectorName}/archiveFailed/' repository"
              .replaceAll("[\r\n]", "")
          )
          throw exception // to stop the Task
        }
      case exception =>
        val mainSystem = new MainSystemImpl
        mainSystem.storeFailedFile(
          archivedFile,
          properties.projectId,
          properties.connectorName
        )
        log.error(
          s"An unexpected exception happened while trying to send the file $archivedFile to the iGrafx Mining API. The file is stored under the '${MainConfig.csvPath}/${properties.projectId}/${properties.connectorName}/archiveFailed/' repository"
            .replaceAll("[\r\n]", "")
        )
        throw exception // to stop the Task
    }
  }

  /** Method used to manage all the steps followed to create the Column Mapping
    *
    * @param columnMappingProperties Connector's properties
    */
  @throws[InvalidTokenException](cause = "Problem with Http Request, not able to deliver a valid token")
  @throws[ColumnMappingCreationException](cause = "Problem with the creation of the Column Mapping for the project")
  @throws[ColumnMappingAlreadyExistsException](cause = "Column mapping already exists in targeted Mining project")
  override def createColumnMapping(columnMappingProperties: ColumnMappingProperties): Future[Unit] = {
    val finalApiUrl: String = getPublicApiUrl(columnMappingProperties.configurationProperties.apiUrl)
    (for {
      token <- sendAuthentication(
        columnMappingProperties.configurationProperties.authUrl,
        columnMappingProperties.configurationProperties.workgroupId,
        columnMappingProperties.configurationProperties.workgroupKey
      )
      exists <- checkIfColumnMappingExists(
        finalApiUrl,
        columnMappingProperties.configurationProperties.projectId,
        token
      )
      _ <-
        if (!exists) {
          val jsonToSend: String = pretty(render(columnMappingProperties.toJson))
          log.debug(
            s"JSON with file and column mapping information that we send to the iGrafx Mining API: \n${jsonToSend.replaceAll("[\r\n]", "")}"
          )
          sendColumnMappingJson(
            finalApiUrl,
            columnMappingProperties.configurationProperties.projectId,
            token,
            jsonToSend
          )
        } else {
          throw ColumnMappingAlreadyExistsException()
        }
    } yield ()).recover {
      case exception =>
        log.error("[MainApiImpl.createColumnMapping] Issue with the Column Mapping Creation".replaceAll("[\r\n]", ""))
        throw exception
    }
  }

  // -------------------------- With scalaj  --------------------------

  /**  log to the iGrafx Mining API using workGroupId and workGroupKey and retrieve a token for next requests
    *
    * @param authUrl url used to connect with the authentication API
    * @param workGroupId iGrafx workgroup Id
    * @param workGroupKey iGrafx workgroup Key
    */
  @throws[InvalidTokenException](cause = "Problem with Http Request, not able to deliver a valid token")
  private[aggregationmain] def sendAuthentication(
      authUrl: String,
      workGroupId: String,
      workGroupKey: String
  ): Future[String] = {
    val form: Seq[(String, String)] = Seq(
      ("grant_type", "client_credentials"),
      ("client_id", workGroupId),
      ("client_secret", workGroupKey)
    )

    Future {
      Http(s"$authUrl/protocol/openid-connect/token")
        .postForm(form)
        .header("Content-Type", "application/json")
        .header("Charset", "UTF-8")
        .option(HttpOptions.readTimeout(Constants.timeoutApiCallValueInMilliSeconds))
        .asString
    }.map { resultLogin =>
      resultLogin.code match {
        case 200 =>
          val token = (parse(resultLogin.body) \\ "access_token").extract[String]
          log.debug(s"Token has been successfully retrieved : $token".replaceAll("[\r\n]", ""))
          token
        case errorCode =>
          throw InvalidTokenException(
            s"Couldn't retrieve a token for this Http Request Authentication : ${resultLogin.code}, ${resultLogin.body}",
            canRetry = errorCode >= 500
          )
      }
    }.recover {
      case tokenException: InvalidTokenException =>
        log.error(
          s"InvalidTokenException : Issue with the Http Request for authentication",
          tokenException
        )
        throw tokenException
      case exception =>
        log.error(
          "Unexpected exception while trying to retrieve a token via the iGrafx authentication API",
          exception
        )
        throw InvalidTokenException(exception.getMessage, canRetry = false)
    }
  }

  /**  Send the file with last data from topics to the iGrafx Mining API
    *
    * @param apiUrl url used to connect with the iGrafx Mining API
    * @param workGroupId iGrafx workgroup Id
    * @param projectId iGrafx project ID
    * @param file the file we want to send
    * @param token Connection token
    *
    * @throws SendFileException Problem transforming the file into bytes or with the Http Request, couldn't send file
    */
  private def sendFile(
      apiUrl: String,
      workGroupId: String,
      projectId: UUID,
      file: File,
      token: String
  ): Future[Unit] = {
    val ficName = file.getName

    val bytes: Array[Byte] = Try { Files.readAllBytes(file.toPath) } match {
      case Success(bytes) => bytes
      case Failure(exception) =>
        log.error("Issue while trying to transform a file into bytes before sending it to the iGrafx Mining API", exception)
        throw SendFileException(
          s"Issue while trying to transform a file into bytes before sending it to the iGrafx Mining API : ${exception.getMessage}",
          canRetry = false
        )
    }

    Future {
      Http(s"$apiUrl/project/$projectId/file?teamId=$workGroupId")
        .postMulti(MultiPart("file", ficName, "text/csv", bytes))
        .header("Authorization", s"Bearer $token")
        .header("accept", "application/json, text/plain, */*")
        .option(HttpOptions.readTimeout(Constants.timeoutApiCallValueInMilliSeconds))
        .asString
    }.map { resultAddFile =>
      resultAddFile.code match {
        case 200 =>
          log.info(
            s"[MainApiImpl.sendFile] The file $file was successfully sent to the iGrafx Mining API"
              .replaceAll("[\r\n]", "")
          )
        case 201 =>
          log.info(
            s"[MainApiImpl.sendFile] The file $file was successfully sent to the iGrafx Mining API"
              .replaceAll("[\r\n]", "")
          )
        case 400 =>
          throw SendFileException(
            s"Invalid parameters for Http Request : ${resultAddFile.code}, ${resultAddFile.body}"
              .replaceAll("[\r\n]", ""),
            canRetry = false
          )
        case 401 =>
          throw SendFileException(
            s"API key is missing or invalid for Http Request : ${resultAddFile.code}, ${resultAddFile.body}"
              .replaceAll("[\r\n]", ""),
            canRetry = false
          )
        case errorCode =>
          throw SendFileException(
            s"Problem with Http Request : ${resultAddFile.code}, ${resultAddFile.body}",
            canRetry = errorCode >= 500
          )
      }
    }.recover {
      case sendFileException: SendFileException =>
        log.error(
          s"Issue with the information in the Http Request sending the file to the iGrafx Mining API",
          sendFileException
        )
        throw sendFileException
      case exception =>
        log.error(
          "Unexpected exception while trying to send the csv archived file to the iGrafx Mining API",
          exception
        )
        throw SendFileException(exception.getMessage, canRetry = false)
    }

  }

  /** Method used to check if the Column Mapping for the project already exists
    *
    * @param apiUrl url used to connect with the iGrafx Mining API
    * @param projectId iGrafx project ID
    * @param token Connection token
    */
  @throws[ColumnMappingCreationException](
    cause = "Problem with Http Request, couldn't check Column Mapping existence for project"
  )
  private def checkIfColumnMappingExists(apiUrl: String, projectId: UUID, token: String): Future[Boolean] = {
    Future {
      Http(s"$apiUrl/project/$projectId/column-mapping-exists")
        .header("Authorization", s"Bearer $token")
        .option(HttpOptions.readTimeout(Constants.timeoutApiCallValueInMilliSeconds))
        .asString
    }.map { resultExists =>
      resultExists.code match {
        case 200 =>
          (parse(resultExists.body) \\ "exists").extract[Boolean]
        case 400 =>
          throw ColumnMappingCreationException(
            s"Bad Request (Problem with the projectId) for Http Request : ${resultExists.code}, ${resultExists.body}",
            canRetry = false
          )
        case 401 =>
          throw ColumnMappingCreationException(
            s"API key is missing or invalid for Http Request : ${resultExists.code}, ${resultExists.body}",
            canRetry = false
          )
        case errorCode =>
          throw ColumnMappingCreationException(
            s"Problem with Http Request : ${resultExists.code}, ${resultExists.body}",
            canRetry = errorCode >= 500
          )
      }
    }.recover {
      case columnMappingCreationException: ColumnMappingCreationException =>
        log.error(
          s"Issue with the information in the Http Request sent to know if a Column Mapping already exists for the project",
          columnMappingCreationException
        )
        throw columnMappingCreationException
      case exception =>
        log.error(
          "Unexpected exception with the Http Request sent to know if a Column Mapping already exists for the project",
          exception
        )
        throw ColumnMappingCreationException(exception.getMessage, canRetry = false)
    }
  }

  /** Method used to send the Column Mapping we want for the project to the iGrafx Mining API
    *
    * @param apiUrl url used to connect with the iGrafx Mining API
    * @param projectId iGrafx project ID
    * @param token Connection token
    * @param json The json containing the information about the Column Mapping
    *
    * @throws ColumnMappingCreationException Problem with Http Request, couldn't create Column Mapping for project
    */
  private def sendColumnMappingJson(apiUrl: String, projectId: UUID, token: String, json: String): Future[Unit] = {
    Future {
      Http(s"$apiUrl/project/$projectId/column-mapping")
        .header("Authorization", s"Bearer $token")
        .header("content-type", "application/json")
        .postData(json)
        .option(HttpOptions.readTimeout(Constants.timeoutApiCallValueInMilliSeconds))
        .asString
    }.map { resultColumnMappingCreation =>
      resultColumnMappingCreation.code match {
        case 204 =>
          log.debug("The Column Mapping has been successfully created !")
        case 400 =>
          throw ColumnMappingCreationException(
            s"Bad Request (Problem with the json or with the projectId) for Http Request : ${resultColumnMappingCreation.code}, ${resultColumnMappingCreation.body}",
            canRetry = false
          )
        case 401 =>
          throw ColumnMappingCreationException(
            s"API key is missing or invalid for Http Request : ${resultColumnMappingCreation.code}, ${resultColumnMappingCreation.body}",
            canRetry = false
          )
        case errorCode =>
          throw ColumnMappingCreationException(
            s"Problem with Http Request : ${resultColumnMappingCreation.code}, ${resultColumnMappingCreation.body}",
            canRetry = errorCode >= 500
          )
      }
    }.recover {
      case columnMappingCreationException: ColumnMappingCreationException =>
        log.error(
          s"Issue with the information in the Http Request sending the Column Mapping to the iGrafx Mining API",
          columnMappingCreationException
        )
        throw columnMappingCreationException
      case exception =>
        log.error(
          "Unexpected exception with the Http Request sending the Column Mapping to the iGrafx Mining API",
          exception
        )
        throw ColumnMappingCreationException(exception.getMessage, canRetry = false)
    }
  }

  private def getPublicApiUrl(apiUrl: String): String = {
    if (!apiUrl.contains("/pub")) {
      apiUrl + "/pub"
    } else {
      apiUrl
    }
  }
}
