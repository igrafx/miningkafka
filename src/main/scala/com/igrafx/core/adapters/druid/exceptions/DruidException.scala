package com.igrafx.core.adapters.druid.exceptions

import com.igrafx.core.exceptions.SafeException

final case class DruidException(message: String) extends SafeException(message)
