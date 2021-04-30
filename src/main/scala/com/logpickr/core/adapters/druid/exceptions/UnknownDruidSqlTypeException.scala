package com.logpickr.core.adapters.druid.exceptions

final case class UnknownDruidSqlTypeException(message: String) extends Exception(message)
