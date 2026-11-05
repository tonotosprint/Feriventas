package com.example.myapplication.model

import java.util.UUID

/**
 * Representa un registro individual en el historial de la calculadora.
 */
data class CalculationHistory(
    val id: String = UUID.randomUUID().toString(),
    val expression: String,
    val result: String,
    val timestamp: Long = System.currentTimeMillis()
)
