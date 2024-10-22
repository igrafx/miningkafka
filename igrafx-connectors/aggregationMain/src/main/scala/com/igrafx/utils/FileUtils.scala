package com.igrafx.utils

object FileUtils {
  def sanitizeFileName(fileName: String): Either[IllegalArgumentException, String] = {
    Option(fileName) match {
      case Some(fileName) =>
        if (fileName.indexOf(0) >= 0) {
          Left(
            new IllegalArgumentException(
              "Null byte present in file/path name. There are no known legitimate use cases for such data, but several injection attacks may use it"
            )
          )
        } else {
          val lastUnixSeparator = fileName.lastIndexOf("/")
          val lastWinSeparator = fileName.lastIndexOf("\\")
          Right(fileName.substring(Math.max(lastUnixSeparator, lastWinSeparator) + 1))
        }
      case None => Left(new IllegalArgumentException("empty file name"))
    }
  }
}
