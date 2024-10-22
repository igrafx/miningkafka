package com.igrafx.utils

import org.apache.kafka.common.config.{ConfigException, ConfigValue}

trait ConfigUtils {

  /** Method used to get the configValue of a given property
    *
    * @param configValues An Iterable containing the values of the properties of the connector
    * @param configName The name of the property for which we want to retrieve the value
    *
    * @throws ConfigException If the property is not found
    */
  @throws[ConfigException]
  def getConfigValue(configValues: Iterable[ConfigValue], configName: String): ConfigValue = {
    configValues
      .find(value => value.name() == configName)
      .getOrElse(throw new ConfigException(s"Impossible to find the '$configName' property in the configuration"))
  }
}
