package com.igrafx.kafka.sink.aggregationmain.domain.recommenders

import org.apache.kafka.common.config.ConfigDef

import java.util
import scala.jdk.CollectionConverters._

class CsvEncodingRecommender extends ConfigDef.Recommender {
  override def validValues(name: String, parsedConfig: util.Map[String, AnyRef]): util.List[AnyRef] = {
    Seq("UTF-8": AnyRef, "ASCII": AnyRef, "ISO-8859-1": AnyRef).asJava
  }
  override def visible(name: String, parsedConfig: util.Map[String, AnyRef]): Boolean = {
    true
  }
}
