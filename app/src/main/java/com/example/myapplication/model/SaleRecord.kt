package com.example.myapplication.model

import java.util.UUID

enum class PaymentMethod(val label: String) {
    CASH("Efectivo"),
    TRANSFER("Transferencia / MP")
}

data class SaleItem(
    val productId: String,
    val productName: String,
    val price: Double,
    val quantity: Int,
    val category: String = "General",
    val costPrice: Double = 0.0
) {
    val totalRevenue: Double
        get() = price * quantity

    val totalManufacturingCost: Double
        get() = costPrice * quantity

    val netProfit: Double
        get() = (price - costPrice) * quantity

    @Deprecated("Use totalRevenue instead", ReplaceWith("totalRevenue"))
    val totalCost: Double
        get() = totalRevenue
}

data class SaleRecord(
    val id: String = UUID.randomUUID().toString(),
    val items: List<SaleItem>,
    val subtotalAmount: Double = items.sumOf { it.totalRevenue },
    val discountAmount: Double = 0.0,
    val totalAmount: Double,
    val paymentMethod: PaymentMethod,
    val amountPaid: Double = totalAmount,
    val changeGiven: Double = 0.0,
    val timestamp: Long = System.currentTimeMillis(),
    val vendorName: String = "Admin"
) {
    val totalManufacturingCost: Double
        get() = items.sumOf { it.totalManufacturingCost }

    val netProfit: Double
        get() = totalAmount - totalManufacturingCost
}
