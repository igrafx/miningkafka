package com.igrafx.kafka.sink.aggregationmain.domain.validators

import org.apache.kafka.common.config.{ConfigDef, ConfigException}

class CsvEncodingValidator extends ConfigDef.Validator {
  override def ensureValid(name: String, value: Any): Unit = {
    if (value == null) {
      throw new ConfigException(
        s"The $name property is not defined !"
      )
    }
    val csvEncoding: String = value.toString
    csvEncoding.toLowerCase match {
      case "utf8" | "utf-8" | "utf_8" | "ascii" | "iso-8859-1" | "iso_8859_1" => ()
      case _ =>
        throw new ConfigException(
          s"The $name property having for value $csvEncoding is not correct, choose one value between UTF-8, ASCII, ISO-8859-1"
        )
    }
  }
}
