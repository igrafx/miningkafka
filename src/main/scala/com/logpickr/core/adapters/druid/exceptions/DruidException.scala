package com.logpickr.core.adapters.druid.exceptions

final case class DruidException(message: String) extends Exception(message)
