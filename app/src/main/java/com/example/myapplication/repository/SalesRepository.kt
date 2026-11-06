package com.example.myapplication.repository

import android.content.Context
import android.content.SharedPreferences
import com.example.myapplication.model.PaymentMethod
import com.example.myapplication.model.SaleItem
import com.example.myapplication.model.SaleRecord
import com.example.myapplication.util.FirestoreSyncHelper
import org.json.JSONArray
import org.json.JSONObject

interface ISalesRepository {
    fun getSales(): List<SaleRecord>
    fun addSale(sale: SaleRecord)
    fun removeSale(saleId: String)
    fun clearSales()
    fun setOnSalesUpdatedListener(listener: (List<SaleRecord>) -> Unit)
}

/**
 * Repositorio encargado de la persistencia local y sincronización en la nube con Firestore de las ventas.
 */
class SalesRepository(context: Context) : ISalesRepository {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("sales_storage_prefs", Context.MODE_PRIVATE)

    private var onSalesUpdated: ((List<SaleRecord>) -> Unit)? = null

    companion object {
        private const val KEY_SALES = "key_sales_records"
    }

    init {
        FirestoreSyncHelper.listenToData("sales_records") { json ->
            if (!json.isNullOrEmpty()) {
                prefs.edit().putString(KEY_SALES, json).apply()
                onSalesUpdated?.invoke(parseSalesJson(json))
            } else {
                prefs.edit().remove(KEY_SALES).apply()
                onSalesUpdated?.invoke(emptyList())
            }
        }
    }

    override fun setOnSalesUpdatedListener(listener: (List<SaleRecord>) -> Unit) {
        onSalesUpdated = listener
    }

    override fun getSales(): List<SaleRecord> {
        val jsonString = prefs.getString(KEY_SALES, null) ?: return emptyList()
        return parseSalesJson(jsonString)
    }

    override fun addSale(sale: SaleRecord) {
        val currentSales = getSales().toMutableList()
        currentSales.add(0, sale)
        saveSalesList(currentSales)
    }

    override fun removeSale(saleId: String) {
        val currentSales = getSales().filter { it.id != saleId }
        saveSalesList(currentSales)
    }

    override fun clearSales() {
        prefs.edit().remove(KEY_SALES).apply()
        FirestoreSyncHelper.syncDataUp("sales_records", "[]")
        onSalesUpdated?.invoke(emptyList())
    }

    private fun saveSalesList(sales: List<SaleRecord>) {
        val jsonArray = JSONArray()
        sales.forEach { sale ->
            val saleObj = JSONObject().apply {
                put("id", sale.id)
                put("subtotalAmount", sale.subtotalAmount)
                put("discountAmount", sale.discountAmount)
                put("totalAmount", sale.totalAmount)
                put("paymentMethod", sale.paymentMethod.name)
                put("amountPaid", sale.amountPaid)
                put("changeGiven", sale.changeGiven)
                put("timestamp", sale.timestamp)
                put("vendorName", sale.vendorName)

                val itemsArray = JSONArray()
                sale.items.forEach { item ->
                    val itemObj = JSONObject().apply {
                        put("productId", item.productId)
                        put("productName", item.productName)
                        put("price", item.price)
                        put("costPrice", item.costPrice)
                        put("quantity", item.quantity)
                        put("category", item.category)
                    }
                    itemsArray.put(itemObj)
                }
                put("items", itemsArray)
            }
            jsonArray.put(saleObj)
        }
        val jsonString = jsonArray.toString()
        prefs.edit().putString(KEY_SALES, jsonString).apply()
        FirestoreSyncHelper.syncDataUp("sales_records", jsonString)
        onSalesUpdated?.invoke(sales)
    }

    private fun parseSalesJson(jsonString: String): List<SaleRecord> {
        return try {
            val jsonArray = JSONArray(jsonString)
            val list = mutableListOf<SaleRecord>()
            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)
                val id = obj.optString("id")
                val subtotal = obj.optDouble("subtotalAmount", 0.0)
                val discount = obj.optDouble("discountAmount", 0.0)
                val total = obj.optDouble("totalAmount", 0.0)
                val methodStr = obj.optString("paymentMethod", PaymentMethod.CASH.name)
                val method = try { PaymentMethod.valueOf(methodStr) } catch (e: Exception) { PaymentMethod.CASH }
                val paid = obj.optDouble("amountPaid", 0.0)
                val change = obj.optDouble("changeGiven", 0.0)
                val timestamp = obj.optLong("timestamp", System.currentTimeMillis())
                val vendorName = obj.optString("vendorName", "Admin")

                val itemsList = mutableListOf<SaleItem>()
                val itemsArray = obj.optJSONArray("items")
                if (itemsArray != null) {
                    for (j in 0 until itemsArray.length()) {
                        val itemObj = itemsArray.getJSONObject(j)
                        itemsList.add(
                            SaleItem(
                                productId = itemObj.optString("productId"),
                                productName = itemObj.optString("productName"),
                                price = itemObj.optDouble("price", 0.0),
                                costPrice = itemObj.optDouble("costPrice", 0.0),
                                quantity = itemObj.optInt("quantity", 1),
                                category = itemObj.optString("category", "General")
                            )
                        )
                    }
                }

                list.add(
                    SaleRecord(
                        id = id,
                        items = itemsList,
                        subtotalAmount = subtotal,
                        discountAmount = discount,
                        totalAmount = total,
                        paymentMethod = method,
                        amountPaid = paid,
                        changeGiven = change,
                        timestamp = timestamp,
                        vendorName = vendorName
                    )
                )
            }
            list
        } catch (e: Exception) {
            emptyList()
        }
    }
}
