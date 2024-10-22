package com.igrafx.kafka.sink.aggregationmain.adapters.serializers

import org.json4s.CustomSerializer
import org.json4s.JsonAST.JString

import java.nio.file.Path

protected[adapters] class PathSerializer
    extends CustomSerializer[Path](_ =>
      (
        {
          case _ => throw new UnsupportedOperationException("Unexpected deserialization of Path")
        },
        {
          case path: Path => JString(path.getFileName.toString)
        }
      )
    )
