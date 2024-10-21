package com.igrafx.core.adapters.druid.exceptions

import com.igrafx.core.exceptions.SafeException

final case class UnknownDruidSqlTypeException(message: String) extends SafeException(message)
