package com.example.myapplication.repository

import android.content.Context
import android.content.SharedPreferences
import com.example.myapplication.model.RequestedProduct
import com.example.myapplication.util.FirestoreSyncHelper
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray
import org.json.JSONObject

interface RequestedProductsRepositoryInterface {
    val requestedProducts: StateFlow<List<RequestedProduct>>
    fun addRequestedProduct(product: RequestedProduct)
    fun updateRequestedProduct(product: RequestedProduct)
    fun toggleRequestedProductCompleted(id: String)
    fun deleteRequestedProduct(id: String)
}

class RequestedProductsRepository(
    context: Context
) : RequestedProductsRepositoryInterface {

    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private val _requestedProducts = MutableStateFlow<List<RequestedProduct>>(emptyList())
    override val requestedProducts: StateFlow<List<RequestedProduct>> = _requestedProducts.asStateFlow()

    private var listenerRegistration: ListenerRegistration? = null

    init {
        loadFromPrefs()
        startFirestoreListener()
    }

    private fun loadFromPrefs() {
        val json = prefs.getString(KEY_REQUESTED_PRODUCTS, null)
        if (json != null) {
            _requestedProducts.value = parseJsonList(json)
        }
    }

    private fun saveToPrefs(list: List<RequestedProduct>) {
        val json = toJsonString(list)
        prefs.edit().putString(KEY_REQUESTED_PRODUCTS, json).apply()
        FirestoreSyncHelper.syncDataUp("requested_products", json)
    }

    private fun startFirestoreListener() {
        listenerRegistration?.remove()
        listenerRegistration = FirestoreSyncHelper.listenToData("requested_products") { json ->
            if (json != null) {
                val cloudList = parseJsonList(json)
                _requestedProducts.value = cloudList
                prefs.edit().putString(KEY_REQUESTED_PRODUCTS, json).apply()
            }
        }
    }

    override fun addRequestedProduct(product: RequestedProduct) {
        val current = _requestedProducts.value.toMutableList()
        current.add(0, product)
        _requestedProducts.value = current
        saveToPrefs(current)
    }

    override fun updateRequestedProduct(product: RequestedProduct) {
        val current = _requestedProducts.value.map {
            if (it.id == product.id) product else it
        }
        _requestedProducts.value = current
        saveToPrefs(current)
    }

    override fun toggleRequestedProductCompleted(id: String) {
        val current = _requestedProducts.value.map {
            if (it.id == id) it.copy(isCompleted = !it.isCompleted) else it
        }
        _requestedProducts.value = current
        saveToPrefs(current)
    }

    override fun deleteRequestedProduct(id: String) {
        val current = _requestedProducts.value.filter { it.id != id }
        _requestedProducts.value = current
        saveToPrefs(current)
    }

    private fun parseJsonList(json: String): List<RequestedProduct> {
        val result = mutableListOf<RequestedProduct>()
        try {
            val array = JSONArray(json)
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                result.add(
                    RequestedProduct(
                        id = obj.optString("id"),
                        name = obj.optString("name"),
                        customerName = obj.optString("customerName", ""),
                        note = obj.optString("note", ""),
                        quantity = obj.optInt("quantity", 1),
                        isCompleted = obj.optBoolean("isCompleted", false),
                        timestamp = obj.optLong("timestamp", System.currentTimeMillis())
                    )
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return result
    }

    private fun toJsonString(list: List<RequestedProduct>): String {
        val array = JSONArray()
        for (item in list) {
            val obj = JSONObject().apply {
                put("id", item.id)
                put("name", item.name)
                put("customerName", item.customerName)
                put("note", item.note)
                put("quantity", item.quantity)
                put("isCompleted", item.isCompleted)
                put("timestamp", item.timestamp)
            }
            array.put(obj)
        }
        return array.toString()
    }

    companion object {
        private const val PREFS_NAME = "requested_products_prefs"
        private const val KEY_REQUESTED_PRODUCTS = "requested_products_list"
    }
}
