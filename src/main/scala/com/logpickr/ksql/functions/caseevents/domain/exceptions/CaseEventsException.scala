package com.logpickr.ksql.functions.caseevents.domain.exceptions

import scala.util.control.NoStackTrace

final case class CaseEventsException(message: String) extends Exception(message) with NoStackTrace
