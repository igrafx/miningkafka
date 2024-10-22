package com.igrafx.kafka.sink.aggregationmain.domain.validators

import org.apache.kafka.common.config.{ConfigDef, ConfigException}

import java.util.UUID
import scala.util.{Failure, Success, Try}

class ProjectIdValidator extends ConfigDef.Validator {
  override def ensureValid(name: String, value: Any): Unit = {
    if (value == null) {
      throw new ConfigException(
        s"The $name property is not defined !"
      )
    }
    val projectId: String = value.toString

    Try {
      UUID.fromString(projectId)
    } match {
      case Success(_) => ()
      case Failure(exception: IllegalArgumentException) =>
        throw new ConfigException(
          s"The $name property defined by the user does not suit UUID",
          exception
        )
      case Failure(exception) =>
        throw new ConfigException(
          s"Problem with the $name property : ${exception.getMessage}",
          exception
        )
    }
  }
}
