package com.igrafx.kafka.sink.aggregationmain.domain.dtos

import com.igrafx.kafka.sink.aggregationmain.domain.enums.{ConnectorPropertiesEnum, EncodingEnum}
import org.apache.kafka.common.config.ConfigException
import org.slf4j.Logger

object EncodingEnumDto {
  type EncodingEnumDto = String

  val UTF_8 = "UTF-8"
  val ISO_8859_1 = "ISO-8859-1"
  val ASCII = "ASCII"

  implicit class EncodingEnumDtoToEncodingEnum(encodingEnumDto: EncodingEnumDto) {
    def toEncodingEnum: EncodingEnum = {
      encodingEnumDtoToEncodingEnum(encodingEnumDto)
    }
  }

  private def encodingEnumDtoToEncodingEnum(encodingEnumDto: EncodingEnumDto): EncodingEnum = {
    encodingEnumDto match {
      case EncodingEnumDto.UTF_8 => EncodingEnum.UTF_8
      case EncodingEnumDto.ASCII => EncodingEnum.ASCII
      case EncodingEnumDto.ISO_8859_1 => EncodingEnum.ISO_8859_1
    }
  }

  /**
    * @param csvEncoding The encoding value defined by the user, needs to be UTF-8/ASCII/ISO-8859-1
    *
    * @throws ConfigException The encoding value defined by the user is not valid
    */
  def getEncoding(csvEncoding: String, log: Logger): EncodingEnumDto = {
    csvEncoding.toLowerCase match {
      case "utf8" | "utf-8" | "utf_8" => EncodingEnumDto.UTF_8
      case "ascii" => EncodingEnumDto.ASCII
      case "iso-8859-1" | "iso_8859_1" => EncodingEnumDto.ISO_8859_1
      case _ =>
        log.error(
          s"ConfigException : The ${ConnectorPropertiesEnum.csvEncodingProperty.toStringDescription} property having for value $csvEncoding is not correct, choose one value between UTF-8, ASCII, ISO-8859-1"
            .replaceAll("[\r\n]", "")
        )
        throw new ConfigException(
          s"The ${ConnectorPropertiesEnum.csvEncodingProperty.toStringDescription} property having for value $csvEncoding is not correct, choose one value between UTF-8, ASCII, ISO-8859-1"
            .replaceAll("[\r\n]", "")
        )
    }
  }
}
