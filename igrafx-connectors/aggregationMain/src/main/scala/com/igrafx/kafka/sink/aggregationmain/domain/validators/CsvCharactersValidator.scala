package com.igrafx.kafka.sink.aggregationmain.domain.validators

import org.apache.kafka.common.config.{ConfigDef, ConfigException}

class CsvCharactersValidator extends ConfigDef.Validator {
  override def ensureValid(name: String, value: Any): Unit = {
    if (value == null) {
      throw new ConfigException(
        s"The $name property is not defined !"
      )
    }
    val csvCharacter: String = value.toString
    if (csvCharacter.length != 1) {
      throw new ConfigException(
        s"The $name property having for value '$csvCharacter' is not correct, String length needs to be 1"
      )
    }

    ()
  }
}
