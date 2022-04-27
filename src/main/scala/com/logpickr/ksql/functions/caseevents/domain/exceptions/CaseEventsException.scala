package com.logpickr.ksql.functions.caseevents.domain.exceptions

import com.logpickr.core.exceptions.SafeException

import scala.util.control.NoStackTrace

final case class CaseEventsException(message: String) extends SafeException(message) with NoStackTrace
