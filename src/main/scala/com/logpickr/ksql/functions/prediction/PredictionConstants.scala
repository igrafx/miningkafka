package com.logpickr.ksql.functions.prediction

object PredictionConstants {
  val timeoutFutureValueInSeconds = 1800
  val timeoutApiCallValueInMilliSeconds = 300000
  val tryIntervalInMilliseconds = 1000 // must be > 0

  val tokenHeader = "Authorization"

  // names of structures' fields, if you want to change one name, please also change it in the PredictionStructs.STRUCT_OUTPUT_SCHEMA_DESCRIPTOR and in the README where the following variables can't be used
  val predictionId = "PREDICTION_ID"
  val predictionsPredictionResponse = "PREDICTIONS"
  val caseId = "CASE_ID"
  val finalProcessKeyPredictions = "FINAL_PROCESS_KEY_PREDICTIONS"
  val finalProcessKey = "FINAL_PROCESS_KEY"
  val predictionsFinalProcessKeyPrediction = "PREDICTIONS"
  val finalProcessKeyConfidence = "FINAL_PROCESS_KEY_CONFIDENCE"
  val nextStep = "NEXT_STEP"
  val estimatedEndOfCase = "ESTIMATED_END_OF_CASE"
  val endOfCaseConfidenceInterval = "END_OF_CASE_CONFIDENCE_INTERVAL"
  val name = "NAME"
  val start = "START_PREDICTION_STEP" // corresponds to the "start" parameter in PredictionStep
  val startConfidenceInterval = "START_CONFIDENCE_INTERVAL"
  val end =
    "END_PREDICTION_STEP" // corresponds to the "end" parameter in PredictionStep, but "END" couldn't be used as it is a keyword in ksqlDB
  val endConfidenceInterval = "END_CONFIDENCE_INTERVAL"
  val startInterval = "START_INTERVAL"
  val endInterval = "END_INTERVAL"
  val intervalProbability = "INTERVAL_PROBABILITY"
  // End of names of structures' fields
}
