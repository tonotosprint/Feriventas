package com.example.myapplication.model

import java.util.UUID

data class FairItem(
    val productName: String,
    val quantityTaken: Int
)

data class FairRecord(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val dateMillis: Long = System.currentTimeMillis(),
    val location: String = "",
    val itemsTaken: List<FairItem> = emptyList(),
    val associatedSaleIds: List<String> = emptyList(),
    val associatedExpenseIds: List<String> = emptyList()
)
