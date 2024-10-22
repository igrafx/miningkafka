package com.igrafx.core.exceptions

abstract class SafeException(message: String) extends Exception(message.replaceAll("[\r\n]", ""))
