package com.logpickr.utils

import org.json4s.ext.{JavaTypesSerializers, JodaTimeSerializers}
import org.json4s.{DefaultFormats, Formats}

object Json4sUtils {
  val defaultFormats: Formats = DefaultFormats.lossless ++ JodaTimeSerializers.all ++ JavaTypesSerializers.all
}
