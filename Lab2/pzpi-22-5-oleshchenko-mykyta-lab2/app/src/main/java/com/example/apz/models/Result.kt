package com.example.apz.models

data class Result(
    val ResultID: Int,
    val AnalysisDate: String,
    val StressLevel: Int,
    val EmotionalState: String,
    val AccountID: Int
)

data class ResultRequest(
    val stress_level: Int
)
