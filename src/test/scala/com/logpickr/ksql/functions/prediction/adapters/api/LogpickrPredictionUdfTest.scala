package com.logpickr.ksql.functions.prediction.adapters.api

import com.logpickr.ksql.functions.prediction.PredictionConstants
import com.logpickr.ksql.functions.prediction.adapters.api.dtos.{
  CasePredictionDto,
  FinalProcessKeyPredictionDto,
  PredictionConfidenceIntervalDto,
  PredictionDto,
  PredictionResponseDto,
  PredictionStepDto,
  PredictionStructs
}
import core.UnitTestSpec
import org.apache.kafka.connect.data.Struct
import org.joda.time.DateTime

import java.util.UUID
import scala.jdk.CollectionConverters._

class LogpickrPredictionUdfTest extends UnitTestSpec {
  val logpickrPredictionUdf = new LogpickrPredictionUdf
  val time: DateTime = DateTime.now()

  // PredictionConfidenceIntervalDto
  val predictionConfidenceInterval: PredictionConfidenceIntervalDto = PredictionConfidenceIntervalDto(
    startInterval = time,
    endInterval = time,
    intervalProbability = 0.8
  )
  val predictionConfidenceIntervalStruct: Struct =
    new Struct(PredictionStructs.PREDICTION_CONFIDENCE_INTERVAL)
      .put(PredictionConstants.startInterval, time.toString)
      .put(PredictionConstants.endInterval, time.toString)
      .put(PredictionConstants.intervalProbability, 0.8)

  describe("generatePredictionConfidenceIntervalStruct") {
    it("should create a Struct from a PredictionConfidenceIntervalDto correctly") {
      assert(
        logpickrPredictionUdf.generatePredictionConfidenceIntervalStruct(
          predictionConfidenceInterval
        ) == predictionConfidenceIntervalStruct
      )
    }
  }

  // PredictionStepDto
  val predictionStep1: PredictionStepDto = PredictionStepDto(
    name = "predictionStep1",
    start = time,
    startConfidenceInterval = Some(predictionConfidenceInterval),
    end = time,
    endConfidenceInterval = Some(predictionConfidenceInterval)
  )
  val predictionStep1Struct: Struct =
    new Struct(PredictionStructs.PREDICTION_STEP)
      .put(PredictionConstants.name, "predictionStep1")
      .put(PredictionConstants.start, time.toString)
      .put(PredictionConstants.startConfidenceInterval, predictionConfidenceIntervalStruct)
      .put(PredictionConstants.end, time.toString)
      .put(PredictionConstants.endConfidenceInterval, predictionConfidenceIntervalStruct)

  val predictionStep2: PredictionStepDto = PredictionStepDto(
    name = "predictionStep2",
    start = time,
    startConfidenceInterval = None,
    end = time,
    endConfidenceInterval = Some(predictionConfidenceInterval)
  )
  val predictionStep2Struct: Struct =
    new Struct(PredictionStructs.PREDICTION_STEP)
      .put(PredictionConstants.name, "predictionStep2")
      .put(PredictionConstants.start, time.toString)
      .put(PredictionConstants.end, time.toString)
      .put(PredictionConstants.endConfidenceInterval, predictionConfidenceIntervalStruct)

  val predictionStep3: PredictionStepDto = PredictionStepDto(
    name = "predictionStep3",
    start = time,
    startConfidenceInterval = Some(predictionConfidenceInterval),
    end = time,
    endConfidenceInterval = None
  )
  val predictionStep3Struct: Struct =
    new Struct(PredictionStructs.PREDICTION_STEP)
      .put(PredictionConstants.name, "predictionStep3")
      .put(PredictionConstants.start, time.toString)
      .put(PredictionConstants.startConfidenceInterval, predictionConfidenceIntervalStruct)
      .put(PredictionConstants.end, time.toString)

  val predictionStep4: PredictionStepDto = PredictionStepDto(
    name = "predictionStep4",
    start = time,
    startConfidenceInterval = None,
    end = time,
    endConfidenceInterval = None
  )
  val predictionStep4Struct: Struct =
    new Struct(PredictionStructs.PREDICTION_STEP)
      .put(PredictionConstants.name, "predictionStep4")
      .put(PredictionConstants.start, time.toString)
      .put(PredictionConstants.end, time.toString)

  describe("generatePredictionStepStruct") {
    it("should create a Struct from a PredictionStepDto correctly") {
      assert(logpickrPredictionUdf.generatePredictionStepStruct(predictionStep1) == predictionStep1Struct)
      assert(logpickrPredictionUdf.generatePredictionStepStruct(predictionStep2) == predictionStep2Struct)
      assert(logpickrPredictionUdf.generatePredictionStepStruct(predictionStep3) == predictionStep3Struct)
      assert(logpickrPredictionUdf.generatePredictionStepStruct(predictionStep4) == predictionStep4Struct)
    }
  }

  // PredictionDto
  val prediction1: PredictionDto = PredictionDto(
    finalProcessKeyConfidence = 0.8,
    nextStep = Some(predictionStep1),
    estimatedEndOfCase = Some(time),
    endOfCaseConfidenceInterval = Some(predictionConfidenceInterval)
  )
  val prediction1Struct: Struct =
    new Struct(PredictionStructs.PREDICTION)
      .put(PredictionConstants.finalProcessKeyConfidence, 0.8)
      .put(PredictionConstants.nextStep, predictionStep1Struct)
      .put(PredictionConstants.estimatedEndOfCase, time.toString)
      .put(PredictionConstants.endOfCaseConfidenceInterval, predictionConfidenceIntervalStruct)

  val prediction2: PredictionDto = PredictionDto(
    finalProcessKeyConfidence = 0.8,
    nextStep = None,
    estimatedEndOfCase = None,
    endOfCaseConfidenceInterval = None
  )
  val prediction2Struct: Struct =
    new Struct(PredictionStructs.PREDICTION)
      .put(PredictionConstants.finalProcessKeyConfidence, 0.8)

  val predictionIterable: Iterable[PredictionDto] = Iterable(prediction1, prediction2)
  val predictionResultList: Iterable[Struct] = Iterable[Struct](prediction1Struct, prediction2Struct)

  describe("generatePredictionsStruct") {
    it("should create a correct Iterable[Struct] from a Iterable[PredictionDto]") {
      assert(logpickrPredictionUdf.generatePredictionsStruct(predictionIterable) == predictionResultList)
    }
  }

  // FinalProcessKeyPredictionDto
  val finalProcessKeyPrediction1: FinalProcessKeyPredictionDto = FinalProcessKeyPredictionDto(
    finalProcessKey = "finalProcessKey1",
    predictions = predictionIterable
  )
  val finalProcessKeyPrediction1Struct: Struct =
    new Struct(PredictionStructs.FINAL_PROCESS_KEY_PREDICTION)
      .put(PredictionConstants.finalProcessKey, "finalProcessKey1")
      .put(PredictionConstants.predictionsFinalProcessKeyPrediction, predictionResultList.toList.asJava)

  val finalProcessKeyPrediction2: FinalProcessKeyPredictionDto = FinalProcessKeyPredictionDto(
    finalProcessKey = "finalProcessKey2",
    predictions = predictionIterable
  )
  val finalProcessKeyPrediction2Struct: Struct =
    new Struct(PredictionStructs.FINAL_PROCESS_KEY_PREDICTION)
      .put(PredictionConstants.finalProcessKey, "finalProcessKey2")
      .put(PredictionConstants.predictionsFinalProcessKeyPrediction, predictionResultList.toList.asJava)

  val finalProcessKeyPredictionIterable: Iterable[FinalProcessKeyPredictionDto] =
    Iterable(finalProcessKeyPrediction1, finalProcessKeyPrediction2)
  val finalProcessKeyPredictionResultList: Iterable[Struct] =
    Iterable[Struct](finalProcessKeyPrediction1Struct, finalProcessKeyPrediction2Struct)

  describe("generateFinalProcessKeyPredictionsStruct") {
    it("should create a correct Iterable[Struct] from a Iterable[FinalProcessKeyPredictionDto]") {
      assert(
        logpickrPredictionUdf.generateFinalProcessKeyPredictionsStruct(
          finalProcessKeyPredictionIterable
        ) == finalProcessKeyPredictionResultList
      )
    }
  }

  // CasePredictionDto
  val casePrediction1: CasePredictionDto = CasePredictionDto(
    caseId = "casePredictionId1",
    finalProcessKeyPredictions = finalProcessKeyPredictionIterable
  )
  val casePrediction2: CasePredictionDto = CasePredictionDto(
    caseId = "casePredictionId2",
    finalProcessKeyPredictions = finalProcessKeyPredictionIterable
  )
  val casePredictionStruct1: Struct =
    new Struct(PredictionStructs.CASE_PREDICTION)
      .put(PredictionConstants.caseId, "casePredictionId1")
      .put(PredictionConstants.finalProcessKeyPredictions, finalProcessKeyPredictionResultList.toList.asJava)
  val casePredictionStruct2: Struct =
    new Struct(PredictionStructs.CASE_PREDICTION)
      .put(PredictionConstants.caseId, "casePredictionId2")
      .put(PredictionConstants.finalProcessKeyPredictions, finalProcessKeyPredictionResultList.toList.asJava)

  val casePredictionIterable: Iterable[CasePredictionDto] = Iterable(casePrediction1, casePrediction2)
  val casePredictionResultList: Iterable[Struct] = Iterable[Struct](casePredictionStruct1, casePredictionStruct2)

  describe("generateCasePredictionStruct") {
    it("should create a correct Iterable[Struct] from a Iterable[CasePredictionDto]") {
      assert(logpickrPredictionUdf.generateCasePredictionStruct(casePredictionIterable) == casePredictionResultList)
    }
  }

  // PredictionResponseDto
  val predictionId = "8961a61c-1fb5-4721-9a8b-d09198eef06b"
  val predictionResponse: PredictionResponseDto = PredictionResponseDto(
    predictionId = UUID.fromString(predictionId),
    predictions = casePredictionIterable
  )
  val predictionResponseStruct: Struct =
    new Struct(PredictionStructs.PREDICTION_RESPONSE)
      .put(PredictionConstants.predictionId, predictionId)
      .put(PredictionConstants.predictionsPredictionResponse, casePredictionResultList.toList.asJava)

  describe("generatePredictionResponseStruct") {
    it("should create a Struct from a PredictionResponseDto correctly") {
      assert(logpickrPredictionUdf.generatePredictionResponseStruct(predictionResponse) == predictionResponseStruct)
    }
  }
}
