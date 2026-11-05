package com.example.myapplication.model

import java.util.UUID

data class ExpenseRecord(
    val id: String = UUID.randomUUID().toString(),
    val eventName: String = "General",
    val category: String = "Varios", // Stand, Viáticos, Transporte, Varios
    val amount: Double,
    val description: String = "",
    val timestamp: Long = System.currentTimeMillis()
)
