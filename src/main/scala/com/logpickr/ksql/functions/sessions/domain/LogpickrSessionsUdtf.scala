package com.logpickr.ksql.functions.sessions.domain

import com.logpickr.ksql.functions.sessions.domain.entities.SessionAccumulator
import com.logpickr.ksql.functions.sessions.domain.structs.Structs
import io.confluent.ksql.function.udf.UdfParameter
import io.confluent.ksql.function.udtf.{Udtf, UdtfDescription}
import org.apache.kafka.connect.data.Struct
import org.slf4j.{Logger, LoggerFactory}

import scala.jdk.CollectionConverters._
import java.util
import scala.util.matching.Regex

import java.nio.charset.StandardCharsets
import java.util.UUID
import java.util.regex.PatternSyntaxException
import scala.util.{Failure, Success, Try}

@UdtfDescription(
  name = "logpickr_sessions",
  author = "Logpickr",
  description = "Udtf dividing a Collection of ordered lines into sessions with their start and end defined by patterns"
)
class LogpickrSessionsUdtf {
  private val log: Logger = LoggerFactory.getLogger(getClass)

  @Udtf(
    description = "Transforms a single collection of rows into multiple rows associated with a sessionId",
    schema = Structs.STRUCT_OUTPUT_SCHEMA_DESCRIPTOR
  )
  def logpickrSessions(
      @UdfParameter(
        value = "inputLines",
        description = "The initial collection of rows"
      ) inputLines: util.List[String],
      @UdfParameter(
        value = "ignorePattern",
        description =
          "Regex precising the lines to ignore (those lines won't be taken into account and won't be returned by the function)"
      ) ignorePattern: String,
      @UdfParameter(
        value = "groupSessionPattern",
        description =
          "Regex allowing to regroup lines having the same values for the specified columns. The session will be determined within these groups. For instance for lines with the following format :\n\ttimeStamp;userID;targetApp;eventType\nIf the regex is :\n\t\".*;(.*);.*;(.*)\"\nThen it's the userID and eventType columns that are going to be used to determine the group of a line (because those two columns are between brackets in the Regex)"
      ) groupSessionPattern: String,
      @UdfParameter(
        value = "startSessionPattern",
        description = "Regex describing the lines that can be considered as a Start of a session"
      ) startSessionPattern: String,
      @UdfParameter(
        value = "endSessionPattern",
        description = "Regex describing the lines that can be considered as a End of a session"
      ) endSessionPattern: String,
      @UdfParameter(
        value = "sessionIdPattern",
        description =
          "Regex informing about the parts of the lines that will be used to create the sessionId. For instance for lines with the following format :\n\ttimeStamp;userID;targetApp;eventType\nIf the regex is :\n\t\".*;(.*);(.*);.*\"\nThen it's the userID and TargetApp columns that are going to be used to create the sessionId for a line starting a session (because those two columns are between brackets in the Regex)"
      ) sessionIdPattern: String,
      @UdfParameter(
        value = "isSessionIdHash",
        description =
          "Boolean indicating whether or not the sessionId is hashed. If false, then the sessionId will only be the concatenation of the values of the columns described in the sessionIdPattern parameter. If true, the sessionId will be this concatenation hashed by the MD5 hash function"
      ) isSessionIdHash: Boolean,
      @UdfParameter(
        value = "isIgnoreIfNoStart",
        description =
          "Boolean indicating if sessions that don't have a line matching the startSessionPattern are kept or not. If true, the corresponding sessions are not returned. If false, they are returned"
      ) isIgnoreIfNoStart: Boolean,
      @UdfParameter(
        value = "isIgnoreIfNoEnd",
        description =
          "Boolean indicating if sessions that don't have a line matching the endSessionPattern are kept or not. If true, the corresponding sessions are not returned. If false, they are returned"
      ) isIgnoreIfNoEnd: Boolean
  ): util.List[Struct] = {
    val filteredLines = getFilteredLines(inputLines.asScala.toSeq, ignorePattern)

    val groupSessionPatternRegex = groupSessionPattern.r
    val sessionIdPatternRegex = sessionIdPattern.r

    Try {
      val sessionAccumulators: Seq[(String, SessionAccumulator)] = getSessionAccumulatorForEachGroup(
        filteredLines,
        groupSessionPatternRegex,
        sessionIdPatternRegex,
        startSessionPattern,
        endSessionPattern,
        isSessionIdHash,
        isIgnoreIfNoStart,
        isIgnoreIfNoEnd
      )

      val sessions: Seq[Struct] = if (!isIgnoreIfNoEnd) {
        sessionAccumulators.flatMap {
          case (_: String, sessionAccumulator: SessionAccumulator) =>
            sessionAccumulator.groupList ++ sessionAccumulator.pendingSessionList
        }
      } else {
        sessionAccumulators.flatMap {
          case (_: String, sessionAccumulator: SessionAccumulator) =>
            if (
              sessionAccumulator.startingSessionLine
                .matches(startSessionPattern) && sessionAccumulator.startingSessionLine.matches(endSessionPattern)
            ) {
              sessionAccumulator.groupList ++ sessionAccumulator.pendingSessionList
            } else {
              sessionAccumulator.groupList
            }
        }
      }

      sessions.asJava
    } match {
      case Success(sessions) => sessions
      case Failure(exception) =>
        log.error("Issue while trying to create sessions from lines in the Session UDF", exception)
        throw exception
    }
  }

  private[sessions] def getSessionLineStruct(sessionId: String, sessionLine: String): Struct = {
    val result = new Struct(Structs.STRUCT_OUTPUT_SCHEMA)
    result.put("SESSION_ID", sessionId)
    result.put("LINE", sessionLine)
    result
  }

  private[sessions] def getGroup(line: String, groupSessionPatternRegex: Regex): Option[String] = {
    patternConcatTry(line, groupSessionPatternRegex) match {
      case Success(group) => Some(group)
      case Failure(exception) =>
        log.error("Incoherence between the groupSessionPatternRegex and the session line", exception)
        None
    }
  }

  private[sessions] def getSessionId(
      line: String,
      sessionIdPatternRegex: Regex,
      isSessionIdHash: Boolean
  ): String = {
    val sessionIdConcat =
      patternConcatTry(line, sessionIdPatternRegex) match {
        case Success(sessionId) => sessionId
        case Failure(exception) =>
          log.error("Incoherence between the sessionIdPatternRegex and the session line", exception)
          ""
      }
    if (isSessionIdHash && (sessionIdConcat != "")) {
      UUID.nameUUIDFromBytes(sessionIdConcat.getBytes(StandardCharsets.UTF_8)).toString
    } else {
      sessionIdConcat
    }
  }

  private def patternConcatTry(line: String, patternRegex: Regex): Try[String] = {
    Try {
      patternRegex.findAllIn(line).matchData.flatMap(_.subgroups).reduce((elem1, elem2) => s"$elem1$elem2")
    }
  }

  private[sessions] def getFilteredLines(inputLines: Seq[String], ignorePattern: String): Seq[String] = {
    Try {
      inputLines.filter { line =>
        !line.matches(ignorePattern)
      }
    } match {
      case Success(filteredLines) => filteredLines
      case Failure(exception: PatternSyntaxException) =>
        log.error("Invalid Syntax for the ignorePattern in the Session UDF", exception)
        throw exception
      case Failure(exception: Throwable) =>
        log.error("Unexpected Exception while filtering lines to ignore in the Session UDF", exception)
        throw exception
    }
  }

  private def getSessionAccumulatorForGivenGroup(
      acc: Map[String, SessionAccumulator],
      lineGroup: String
  ): SessionAccumulator = {
    acc.get(lineGroup) match {
      case Some(groupSessionAccumulator) => groupSessionAccumulator
      case None =>
        SessionAccumulator(
          startingSessionLine = "",
          isSessionStarted = false,
          currentSessionId = "",
          pendingSessionList = Seq.empty,
          groupList = Seq.empty
        )
    }
  }

  private def getSessionAccumulatorForEachGroup(
      filteredLines: Seq[String],
      groupSessionPatternRegex: Regex,
      sessionIdPatternRegex: Regex,
      startSessionPattern: String,
      endSessionPattern: String,
      isSessionIdHash: Boolean,
      isIgnoreIfNoStart: Boolean,
      isIgnoreIfNoEnd: Boolean
  ): Seq[(String, SessionAccumulator)] = {
    filteredLines
      .foldLeft(
        Map.empty[String, SessionAccumulator]
      ) { (acc: Map[String, SessionAccumulator], line: String) =>
        getGroup(line, groupSessionPatternRegex) match {
          case None => acc
          case Some(lineGroup) =>
            val matchesStart = line.matches(startSessionPattern)
            val matchesEnd = line.matches(endSessionPattern)

            val groupSessionAccumulator = getSessionAccumulatorForGivenGroup(acc, lineGroup)

            if (matchesStart) { // Line Type
              val newSessionId = getSessionId(line, sessionIdPatternRegex, isSessionIdHash)
              if (
                (groupSessionAccumulator.isSessionStarted && !isIgnoreIfNoEnd) || (groupSessionAccumulator.isSessionStarted && matchesEnd)
              ) {
                newSessionStartWithGroupListUpdateForGivenGroup(
                  line,
                  lineGroup,
                  newSessionId,
                  groupSessionAccumulator,
                  acc
                )
              } else {
                newSessionStartForGivenGroup(line, lineGroup, newSessionId, groupSessionAccumulator, acc)
              }
            } else if (matchesEnd) { // Line Type
              if (groupSessionAccumulator.isSessionStarted) {
                currentSessionEndForGivenGroup(line, lineGroup, groupSessionAccumulator, acc)
              } else if (!isIgnoreIfNoStart) {
                val newSessionId = getSessionId(line, sessionIdPatternRegex, isSessionIdHash)
                newSessionForEndLineForGivenGroup(line, lineGroup, groupSessionAccumulator, newSessionId, acc)
              } else {
                acc
              }
            } else { // Line Type
              if (groupSessionAccumulator.isSessionStarted) {
                currentSessionUpdatePendingListForGivenGroup(line, lineGroup, groupSessionAccumulator, acc)
              } else if (!isIgnoreIfNoStart) {
                val newSessionId = getSessionId(line, sessionIdPatternRegex, isSessionIdHash)
                newSessionForNormalLineForGivenGroup(line, lineGroup, groupSessionAccumulator, newSessionId, acc)
              } else {
                acc
              }
            }
        }
      }
      .toSeq
  }

  private def newSessionStartWithGroupListUpdateForGivenGroup(
      line: String,
      lineGroup: String,
      newSessionId: String,
      groupSessionAccumulator: SessionAccumulator,
      currentAcc: Map[String, SessionAccumulator]
  ): Map[String, SessionAccumulator] = {
    val updatedGroupList: Seq[Struct] =
      groupSessionAccumulator.groupList ++ groupSessionAccumulator.pendingSessionList
    currentAcc + (lineGroup -> SessionAccumulator(
      startingSessionLine = line,
      isSessionStarted = true,
      currentSessionId = newSessionId,
      pendingSessionList = Seq(getSessionLineStruct(newSessionId, line)),
      groupList = updatedGroupList
    ))
  }

  private def newSessionStartForGivenGroup(
      line: String,
      lineGroup: String,
      newSessionId: String,
      groupSessionAccumulator: SessionAccumulator,
      currentAcc: Map[String, SessionAccumulator]
  ): Map[String, SessionAccumulator] = {
    currentAcc + (lineGroup -> SessionAccumulator(
      startingSessionLine = line,
      isSessionStarted = true,
      currentSessionId = newSessionId,
      pendingSessionList = Seq(getSessionLineStruct(newSessionId, line)),
      groupList = groupSessionAccumulator.groupList
    ))
  }

  private def currentSessionEndForGivenGroup(
      line: String,
      lineGroup: String,
      groupSessionAccumulator: SessionAccumulator,
      currentAcc: Map[String, SessionAccumulator]
  ): Map[String, SessionAccumulator] = {
    val updatedGroupList: Seq[Struct] =
      groupSessionAccumulator.groupList ++ (groupSessionAccumulator.pendingSessionList :+ getSessionLineStruct(
        groupSessionAccumulator.currentSessionId,
        line
      ))
    currentAcc + (lineGroup -> SessionAccumulator(
      startingSessionLine = "",
      isSessionStarted = false,
      currentSessionId = "",
      pendingSessionList = Seq.empty,
      groupList = updatedGroupList
    ))
  }

  private def newSessionForEndLineForGivenGroup(
      line: String,
      lineGroup: String,
      groupSessionAccumulator: SessionAccumulator,
      newSessionId: String,
      currentAcc: Map[String, SessionAccumulator]
  ): Map[String, SessionAccumulator] = {
    val updatedGroupList: Seq[Struct] =
      groupSessionAccumulator.groupList ++ Seq(getSessionLineStruct(newSessionId, line))
    currentAcc + (lineGroup -> SessionAccumulator(
      startingSessionLine = "",
      isSessionStarted = false,
      currentSessionId = "",
      pendingSessionList = Seq.empty,
      groupList = updatedGroupList
    ))
  }

  private def currentSessionUpdatePendingListForGivenGroup(
      line: String,
      lineGroup: String,
      groupSessionAccumulator: SessionAccumulator,
      currentAcc: Map[String, SessionAccumulator]
  ): Map[String, SessionAccumulator] = {
    val updatedPendingSessionList: Seq[Struct] =
      groupSessionAccumulator.pendingSessionList :+ getSessionLineStruct(
        groupSessionAccumulator.currentSessionId,
        line
      )
    currentAcc + (lineGroup ->
      SessionAccumulator(
        startingSessionLine = groupSessionAccumulator.startingSessionLine,
        isSessionStarted = groupSessionAccumulator.isSessionStarted,
        currentSessionId = groupSessionAccumulator.currentSessionId,
        pendingSessionList = updatedPendingSessionList,
        groupList = groupSessionAccumulator.groupList
      ))
  }

  private def newSessionForNormalLineForGivenGroup(
      line: String,
      lineGroup: String,
      groupSessionAccumulator: SessionAccumulator,
      newSessionId: String,
      currentAcc: Map[String, SessionAccumulator]
  ): Map[String, SessionAccumulator] = {
    val updatedPendingSessionList: Seq[Struct] =
      groupSessionAccumulator.pendingSessionList :+ getSessionLineStruct(newSessionId, line)
    currentAcc + (lineGroup ->
      SessionAccumulator(
        startingSessionLine = line,
        isSessionStarted = true,
        currentSessionId = newSessionId,
        pendingSessionList = updatedPendingSessionList,
        groupList = groupSessionAccumulator.groupList
      ))
  }
}
