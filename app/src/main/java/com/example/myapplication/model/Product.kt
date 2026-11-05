package com.example.myapplication.model

import java.util.UUID

/**
 * Representa un producto en el catálogo.
 * @param id Identificador único del producto.
 * @param name Nombre del producto.
 * @param price Precio unitario de venta del producto.
 * @param costPrice Precio unitario de fabricación o de compra del producto.
 * @param quantity Cantidad del producto seleccionada para la venta.
 * @param category Categoría o cajón al que pertenece el producto.
 * @param stock Cantidad de stock disponible del producto.
 * @param isTemporary Indica si es un producto rápido creado solo para la venta actual.
 * @param position Orden/Posición del producto dentro de su categoría.
 * @param barcode Código de barras del producto.
 */
data class Product(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val price: Double,
    val costPrice: Double = 0.0,
    val quantity: Int = 0,
    val category: String = "General",
    val stock: Int = 0,
    val isTemporary: Boolean = false,
    val position: Int = 0,
    val barcode: String = ""
) {
    val totalRevenue: Double
        get() = price * quantity

    val totalCostPrice: Double
        get() = costPrice * quantity

    val netProfitPerUnit: Double
        get() = (price - costPrice)

    val totalNetProfit: Double
        get() = (price - costPrice) * quantity

    @Deprecated("Use totalRevenue instead", ReplaceWith("totalRevenue"))
    val totalCost: Double
        get() = totalRevenue
}
