package com.logpickr.core.adapters.druid.exceptions

import com.logpickr.core.exceptions.SafeException

final case class DruidException(message: String) extends SafeException(message)
