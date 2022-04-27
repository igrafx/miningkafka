package com.logpickr.core.adapters.druid.exceptions

import com.logpickr.core.exceptions.SafeException

final case class UnknownDruidSqlTypeException(message: String) extends SafeException(message)
