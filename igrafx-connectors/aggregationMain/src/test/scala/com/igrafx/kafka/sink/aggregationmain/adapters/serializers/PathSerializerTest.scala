package com.igrafx.kafka.sink.aggregationmain.adapters.serializers

import core.UnitTestSpec
import org.json4s.{DefaultFormats, Extraction, Formats, JString}

import java.nio.file.Paths

final class PathSerializerTest extends UnitTestSpec {
  private implicit val formats: Formats = DefaultFormats + new PathSerializer

  describe("serialize") {
    it("should serialize a Path") {
      val filename = "test"
      val pathAsString = s"/toto/$filename"
      val path = Paths.get(pathAsString)
      val pathJson = Extraction.decompose(path)

      pathJson match {
        case JString(filenameString) => assert(filenameString == filename)
        case _ => fail
      }
    }
  }
}
