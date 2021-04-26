package com.logpickr.ksql.functions.transposition.domain

import com.logpickr.ksql.functions.transposition.domain.entities.{TaskStartStop, TaskTime}
import com.logpickr.ksql.functions.transposition.domain.structs.Structs
import io.confluent.ksql.function.udf.UdfParameter
import io.confluent.ksql.function.udtf.{Udtf, UdtfDescription}
import org.apache.kafka.connect.data.Struct
import org.slf4j.{Logger, LoggerFactory}

import java.text.SimpleDateFormat
import java.util
import scala.jdk.CollectionConverters._
import scala.util.{Failure, Success, Try}

@UdtfDescription(
  name = "logpickr_transposition",
  author = "Logpickr",
  description = "Disassembles a row of date events related to tasks for Logpickr"
)
class LogpickrTranspositionUdtf {
  private val log: Logger = LoggerFactory.getLogger(getClass)

  @Udtf(
    description =
      "Takes an array of " + Structs.STRUCT_SCHEMA_DESCRIPTOR + " and returns rows with a single " + Structs.STRUCT_SCHEMA_DESCRIPTOR + " per row",
    schema = Structs.STRUCT_SCHEMA_DESCRIPTOR
  )
  def logpickrTransposition(
      @UdfParameter(
        value = "input",
        schema = "ARRAY<" + Structs.STRUCT_SCHEMA_DESCRIPTOR + ">"
      ) input: util.List[Struct]
  ): util.List[Struct] = {
    filteredSeq(input.asScala.toSeq).asJava
  }

  @Udtf(
    description =
      "Takes an array of " + Structs.STRUCT_SCHEMA_DESCRIPTOR + " and returns rows with a single " + Structs.STRUCT_SCHEMA_SPECIAL_DESCRIPTOR + " per row",
    schema = Structs.STRUCT_SCHEMA_SPECIAL_DESCRIPTOR
  )
  def logpickrTransposition(
      @UdfParameter(
        value = "input",
        schema = "ARRAY<" + Structs.STRUCT_SCHEMA_DESCRIPTOR + ">"
      ) input: util.List[Struct],
      @UdfParameter(value = "dateFormat") dateFormat: String,
      @UdfParameter(value = "isStartInformation") isStartInformation: Boolean,
      @UdfParameter(value = "isTaskNameAscending") isTaskNameAscending: Boolean
  ): util.List[Struct] = {
    Try {
      val simpleDateFormat = new SimpleDateFormat(dateFormat)
      val timedTasks = filteredSeq(input.asScala.toSeq)
        .map { struct: Struct =>
          TaskTime(Option(struct.getString("TASK")).getOrElse(""), struct.getString("TIME"))
        }
        .sortBy { taskTime: TaskTime =>
          (simpleDateFormat.parse(taskTime.time).getTime, taskTime.task)
        }(
          Ordering.Tuple2(
            Ordering.Long,
            if (isTaskNameAscending) { Ordering.String }
            else { Ordering.String.reverse }
          )
        )
        .map(Some(_))

      val timedTasksResult = if (timedTasks.nonEmpty) {
        val timedTasksForSlide =
          if (isStartInformation) { // None added to have a Seq of timedActivities' size after the use of "sliding"
            timedTasks ++ Seq(None)
          } else {
            Seq(None) ++ timedTasks
          }

        timedTasksForSlide
          .sliding(2)
          .map {
            case List(Some(startTimedTask), Some(stopTimedTask)) =>
              val currentTask = if (isStartInformation) { startTimedTask.task }
              else { stopTimedTask.task }
              TaskStartStop(
                task = currentTask,
                start = startTimedTask.time,
                stop = stopTimedTask.time
              )
            case List(None, Some(timedTask)) =>
              TaskStartStop(task = timedTask.task, start = timedTask.time, stop = timedTask.time)
            case List(Some(timedTask), None) =>
              TaskStartStop(task = timedTask.task, start = timedTask.time, stop = timedTask.time)
          }
          .toSeq
      } else {
        Seq.empty
      }

      timedTasksResult.map { taskStartStop: TaskStartStop =>
        val result = new Struct(Structs.STRUCT_SCHEMA_SPECIAL)
        result.put("TASK", taskStartStop.task)
        result.put("START", taskStartStop.start)
        result.put("STOP", taskStartStop.stop)
        result
      }.asJava
    } match {
      case Success(result) => result
      case Failure(exception) =>
        log.error("Issue in the logpickrTransposition UDTF function", exception)
        Seq.empty.asJava
    }
  }

  private[ksql] def filteredSeq(seqToFilter: Iterable[Struct]): Seq[Struct] = {
    seqToFilter.filter { struct =>
      Option(struct.getString("TIME")) match {
        case Some(timeAsString) => timeAsString.nonEmpty
        case None => false
      }
    }.toSeq
  }
}
