package com.igrafx.ksql.functions.transposition.domain

import com.igrafx.ksql.functions.transposition.domain.entities.{TaskStartStop, TaskTime}
import com.igrafx.ksql.functions.transposition.domain.structs.Structs
import io.confluent.ksql.function.udf.UdfParameter
import io.confluent.ksql.function.udtf.{Udtf, UdtfDescription}
import org.apache.kafka.connect.data.Struct
import org.slf4j.{Logger, LoggerFactory}

import java.text.SimpleDateFormat
import java.util
import scala.jdk.CollectionConverters._
import scala.util.{Failure, Success, Try}

@UdtfDescription(
  name = "igrafx_transposition",
  author = "iGrafx",
  description = "Disassembles a row of date events related to tasks for iGrafx"
)
class IGrafxTranspositionUdtf {
  private val log: Logger = LoggerFactory.getLogger(getClass)

  @Udtf(
    description = "Transpose a row of timed tasks into multiple rows with one timed task per row",
    schema = Structs.STRUCT_SCHEMA_DESCRIPTOR
  )
  def igrafxTransposition(
      @UdfParameter(
        value = "input",
        description = "An array corresponding to a row of timed tasks",
        schema = "ARRAY<" + Structs.STRUCT_SCHEMA_DESCRIPTOR + ">"
      ) input: util.List[Struct]
  ): util.List[Struct] = {
    filteredSeq(input.asScala.toSeq).asJava
  }

  @Udtf(
    description =
      "Transpose a row of timed tasks into multiple rows with one timed task (start and stop times) per row. Moreover, this function allows to calculate either the start date or the end date of the task, according to the other timed tasks of the row, and enables to order tasks with the same time either with an ascending or descending ordering according to the name of the task. Here is an example : \nThis function can be used to transform a line like :\n\n Case | Task 1     | Task 2     | Task 3     | Task 4     | Task 4     |\n 3    | 17/03/2020 | 16/03/2020 | 17/03/2020 | 18/03/2020 | 17/03/2020 |\n\n into : \n\n Case | Task   | Start      | Stop       |\n 3    | Task 2 | 16/03/2020 | 17/03/2020 |\n 3    | Task 1 | 17/03/2020 | 17/03/2020 |\n 3    | Task 3 | 17/03/2020 | 17/03/2020 |\n 3    | Task 5 | 17/03/2020 | 18/03/2020 |\n 3    | Task 4 | 18/03/2020 | 18/03/2020 |\n\n if isStartInformation = true and isTaskNameAscending = true. For more information and examples check the README of the UDF",
    schema = Structs.STRUCT_SCHEMA_SPECIAL_DESCRIPTOR
  )
  def igrafxTransposition(
      @UdfParameter(
        value = "input",
        description = "An array corresponding to a row of timed tasks",
        schema = "ARRAY<" + Structs.STRUCT_SCHEMA_DESCRIPTOR + ">"
      ) input: util.List[Struct],
      @UdfParameter(
        value = "dateFormat",
        description =
          "Corresponds to the date format, for instance : a task having the following date '12/01/2020' will need this parameter to be equal to 'dd/MM/yyyy'"
      ) dateFormat: String,
      @UdfParameter(
        value = "isStartInformation",
        description =
          "true indicates that the date associated to a task in the input parameter corresponds to the start of the task and that it is therefore necessary to calculate the end of the task. On the contrary, false indicates that the date corresponds to the end of the task and that it is therefore necessary to calculate the start of the task (those calculations are performed according to the dates of the other tasks in the input row)"
      ) isStartInformation: Boolean,
      @UdfParameter(
        value = "isTaskNameAscending",
        description =
          "true indicates that for tasks having the same date, the ordering is determined in an ascending manner according to the name of the tasks, while false indicates that the ordering is determined in a descending manner according to the name of the tasks"
      ) isTaskNameAscending: Boolean
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
        log.error("Issue in the igrafxTransposition UDTF function", exception)
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
