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
        currentSales.add(0, sale) // Agregar al principio
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
            jsonArray.put(sale.toJson())
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
                list.add(obj.toSaleRecord())
            }
            list
        } catch (e: Exception) {
            emptyList()
        }
    }

    private fun SaleRecord.toJson(): JSONObject {
        val itemsArray = JSONArray()
        items.forEach { item ->
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

        return JSONObject().apply {
            put("id", id)
            put("items", itemsArray)
            put("subtotalAmount", subtotalAmount)
            put("discountAmount", discountAmount)
            put("totalAmount", totalAmount)
            put("paymentMethod", paymentMethod.name)
            put("amountPaid", amountPaid)
            put("changeGiven", changeGiven)
            put("timestamp", timestamp)
            put("vendorName", vendorName)
        }
    }

    private fun JSONObject.toSaleRecord(): SaleRecord {
        val itemsArray = getJSONArray("items")
        val itemsList = mutableListOf<SaleItem>()
        for (i in 0 until itemsArray.length()) {
            val itemObj = itemsArray.getJSONObject(i)
            itemsList.add(
                SaleItem(
                    productId = itemObj.optString("productId"),
                    productName = itemObj.getString("productName"),
                    price = itemObj.getDouble("price"),
                    costPrice = itemObj.optDouble("costPrice", 0.0),
                    quantity = itemObj.getInt("quantity"),
                    category = itemObj.optString("category", "General")
                )
            )
        }

        val pmString = optString("paymentMethod", PaymentMethod.CASH.name)
        val pm = try {
            PaymentMethod.valueOf(pmString)
        } catch (e: Exception) {
            PaymentMethod.CASH
        }

        val total = getDouble("totalAmount")

        return SaleRecord(
            id = getString("id"),
            items = itemsList,
            subtotalAmount = optDouble("subtotalAmount", total),
            discountAmount = optDouble("discountAmount", 0.0),
            totalAmount = total,
            paymentMethod = pm,
            amountPaid = optDouble("amountPaid", total),
            changeGiven = optDouble("changeGiven", 0.0),
            timestamp = getLong("timestamp"),
            vendorName = optString("vendorName", "Admin")
        )
    }
}
