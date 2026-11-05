package com.example.myapplication.model

import java.util.UUID

data class RequestedProduct(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val customerName: String = "",
    val note: String = "",
    val quantity: Int = 1,
    val isCompleted: Boolean = false,
    val timestamp: Long = System.currentTimeMillis()
)
