package com.igrafx.kafka.sink.aggregationmain.domain.enums

import java.nio.charset.{Charset, StandardCharsets}

sealed trait EncodingEnum {
  val toStringDescription: String
  val toCharset: Charset
}

object EncodingEnum {
  case object UTF_8 extends EncodingEnum {
    val toStringDescription: String = "UTF-8"
    val toCharset: Charset = StandardCharsets.UTF_8
  }
  case object ASCII extends EncodingEnum {
    val toStringDescription: String = "ASCII"
    val toCharset: Charset = StandardCharsets.US_ASCII
  }
  case object ISO_8859_1 extends EncodingEnum {
    val toStringDescription: String = "ISO-8859-1"
    val toCharset: Charset = StandardCharsets.ISO_8859_1
  }
}
