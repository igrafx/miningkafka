package com.igrafx.ksql.functions.prediction.adapters.api.dtos

import com.igrafx.ksql.functions.prediction.PredictionConstants
import org.apache.kafka.connect.data.{Schema, SchemaBuilder}

protected[api] object PredictionStructs {

  final lazy val PREDICTION_RESPONSE: Schema = SchemaBuilder
    .struct()
    .optional()
    .field(PredictionConstants.predictionId, Schema.OPTIONAL_STRING_SCHEMA)
    .field(PredictionConstants.predictionsPredictionResponse, PREDICTION_RESPONSE_PREDICTIONS_ARRAY)
    .build()

  private final lazy val PREDICTION_RESPONSE_PREDICTIONS_ARRAY: Schema =
    SchemaBuilder.array(CASE_PREDICTION).optional().build()

  final lazy val CASE_PREDICTION: Schema = SchemaBuilder
    .struct()
    .optional()
    .field(PredictionConstants.caseId, Schema.OPTIONAL_STRING_SCHEMA)
    .field(PredictionConstants.finalProcessKeyPredictions, FINAL_PROCESS_KEY_PREDICTIONS)
    .build()

  private final lazy val FINAL_PROCESS_KEY_PREDICTIONS: Schema =
    SchemaBuilder.array(FINAL_PROCESS_KEY_PREDICTION).optional().build()

  final lazy val FINAL_PROCESS_KEY_PREDICTION: Schema =
    SchemaBuilder
      .struct()
      .optional()
      .field(PredictionConstants.finalProcessKey, Schema.OPTIONAL_STRING_SCHEMA)
      .field(PredictionConstants.predictionsFinalProcessKeyPrediction, FINAL_PROCESS_KEY_PREDICTION_PREDICTIONS_ARRAY)
      .build()

  private final lazy val FINAL_PROCESS_KEY_PREDICTION_PREDICTIONS_ARRAY: Schema =
    SchemaBuilder.array(PREDICTION).optional().build()

  final lazy val PREDICTION: Schema =
    SchemaBuilder
      .struct()
      .optional()
      .field(PredictionConstants.finalProcessKeyConfidence, Schema.OPTIONAL_FLOAT64_SCHEMA)
      .field(PredictionConstants.nextStep, PREDICTION_STEP) //option
      .field(PredictionConstants.estimatedEndOfCase, Schema.OPTIONAL_STRING_SCHEMA) //option
      .field(PredictionConstants.endOfCaseConfidenceInterval, PREDICTION_CONFIDENCE_INTERVAL) //option
      .build()

  final lazy val PREDICTION_STEP: Schema =
    SchemaBuilder
      .struct()
      .optional()
      .field(PredictionConstants.name, Schema.OPTIONAL_STRING_SCHEMA)
      .field(PredictionConstants.start, Schema.OPTIONAL_STRING_SCHEMA) //DateTime
      .field(PredictionConstants.startConfidenceInterval, PREDICTION_CONFIDENCE_INTERVAL) //option
      .field(PredictionConstants.end, Schema.OPTIONAL_STRING_SCHEMA) //DateTime
      .field(PredictionConstants.endConfidenceInterval, PREDICTION_CONFIDENCE_INTERVAL) //option
      .build()

  final lazy val PREDICTION_CONFIDENCE_INTERVAL: Schema =
    SchemaBuilder
      .struct()
      .optional()
      .field(PredictionConstants.startInterval, Schema.OPTIONAL_STRING_SCHEMA) //DateTime
      .field(PredictionConstants.endInterval, Schema.OPTIONAL_STRING_SCHEMA) //DateTime
      .field(PredictionConstants.intervalProbability, Schema.OPTIONAL_FLOAT64_SCHEMA)
      .build()

  final val STRUCT_OUTPUT_SCHEMA_DESCRIPTOR =
    "STRUCT<PREDICTION_ID VARCHAR(STRING), PREDICTIONS ARRAY<STRUCT<CASE_ID VARCHAR(STRING), FINAL_PROCESS_KEY_PREDICTIONS ARRAY<STRUCT<FINAL_PROCESS_KEY VARCHAR(STRING), PREDICTIONS ARRAY<STRUCT<FINAL_PROCESS_KEY_CONFIDENCE DOUBLE, NEXT_STEP STRUCT<NAME VARCHAR(STRING), START_PREDICTION_STEP VARCHAR(STRING), START_CONFIDENCE_INTERVAL STRUCT<START_INTERVAL VARCHAR(STRING), END_INTERVAL VARCHAR(STRING), INTERVAL_PROBABILITY DOUBLE>, END_PREDICTION_STEP VARCHAR(STRING), END_CONFIDENCE_INTERVAL STRUCT<START_INTERVAL VARCHAR(STRING), END_INTERVAL VARCHAR(STRING), INTERVAL_PROBABILITY DOUBLE>>, ESTIMATED_END_OF_CASE VARCHAR(STRING), END_OF_CASE_CONFIDENCE_INTERVAL STRUCT<START_INTERVAL VARCHAR(STRING), END_INTERVAL VARCHAR(STRING), INTERVAL_PROBABILITY DOUBLE>>>>>>>>"
}
