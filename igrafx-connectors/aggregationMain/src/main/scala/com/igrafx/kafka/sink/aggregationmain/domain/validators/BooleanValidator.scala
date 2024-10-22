package com.igrafx.kafka.sink.aggregationmain.domain.validators

import org.apache.kafka.common.config.{ConfigDef, ConfigException}

import scala.util.{Failure, Try}

class BooleanValidator extends ConfigDef.Validator {
  override def ensureValid(name: String, value: Any): Unit = {
    if (value == null) {
      throw new ConfigException(
        s"The $name property is not defined !"
      )
    }
    val booleanValueAsString: String = value.toString

    Try {
      booleanValueAsString.toBoolean
    } match {
      case Failure(_) =>
        throw new ConfigException(
          s"The $name property is not parsable to Boolean"
        )
      case _ => ()
    }
  }
}
