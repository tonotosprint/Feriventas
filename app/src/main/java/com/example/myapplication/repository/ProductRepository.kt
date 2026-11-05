package com.example.myapplication.repository

import android.content.Context
import android.content.SharedPreferences
import com.example.myapplication.model.Product
import com.example.myapplication.util.FirestoreSyncHelper
import org.json.JSONArray
import org.json.JSONObject

interface IProductRepository {
    fun getProducts(): List<Product>
    fun saveProducts(products: List<Product>)
    fun getBusinessName(): String?
    fun saveBusinessName(name: String)
    fun getCategories(): List<String>
    fun saveCategory(category: String)
    fun deleteCategory(category: String)
    fun getCategoryOrder(): List<String>
    fun saveCategoryOrder(categories: List<String>)
    fun setOnProductsUpdatedListener(listener: (List<Product>) -> Unit)
    fun setOnBusinessNameUpdatedListener(listener: (String) -> Unit)
    fun setOnCategoriesUpdatedListener(listener: (List<String>) -> Unit)
    fun setOnCategoryOrderUpdatedListener(listener: (List<String>) -> Unit)
}

/**
 * Repositorio encargado de la persistencia local y sincronización en tiempo real con Firestore.
 */
class ProductRepository(context: Context) : IProductRepository {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("products_storage_prefs", Context.MODE_PRIVATE)

    private var onProductsUpdated: ((List<Product>) -> Unit)? = null
    private var onBusinessNameUpdated: ((String) -> Unit)? = null
    private var onCategoriesUpdated: ((List<String>) -> Unit)? = null
    private var onCategoryOrderUpdated: ((List<String>) -> Unit)? = null

    companion object {
        private const val KEY_PRODUCTS = "key_products_list"
        private const val KEY_BUSINESS_NAME = "key_business_name"
        private const val KEY_CATEGORIES = "key_categories_list"
        private const val KEY_CATEGORY_ORDER = "key_category_order_list"
    }

    init {
        // Escuchar cambios en tiempo real desde Firestore
        FirestoreSyncHelper.listenToData("business_name") { json ->
            if (!json.isNullOrEmpty()) {
                prefs.edit().putString(KEY_BUSINESS_NAME, json).apply()
                onBusinessNameUpdated?.invoke(json)
            }
        }
        FirestoreSyncHelper.listenToData("products") { json ->
            if (!json.isNullOrEmpty()) {
                prefs.edit().putString(KEY_PRODUCTS, json).apply()
                onProductsUpdated?.invoke(parseProductsJson(json))
            }
        }
        FirestoreSyncHelper.listenToData("categories") { json ->
            if (!json.isNullOrEmpty()) {
                prefs.edit().putString(KEY_CATEGORIES, json).apply()
                onCategoriesUpdated?.invoke(parseStringListJson(json))
            }
        }
        FirestoreSyncHelper.listenToData("category_order") { json ->
            if (!json.isNullOrEmpty()) {
                prefs.edit().putString(KEY_CATEGORY_ORDER, json).apply()
                onCategoryOrderUpdated?.invoke(parseStringListJson(json))
            }
        }
    }

    override fun setOnProductsUpdatedListener(listener: (List<Product>) -> Unit) {
        onProductsUpdated = listener
    }

    override fun setOnBusinessNameUpdatedListener(listener: (String) -> Unit) {
        onBusinessNameUpdated = listener
    }

    override fun setOnCategoriesUpdatedListener(listener: (List<String>) -> Unit) {
        onCategoriesUpdated = listener
    }

    override fun setOnCategoryOrderUpdatedListener(listener: (List<String>) -> Unit) {
        onCategoryOrderUpdated = listener
    }

    override fun getBusinessName(): String? {
        return prefs.getString(KEY_BUSINESS_NAME, null)
    }

    override fun saveBusinessName(name: String) {
        prefs.edit().putString(KEY_BUSINESS_NAME, name).apply()
        FirestoreSyncHelper.syncDataUp("business_name", name)
        onBusinessNameUpdated?.invoke(name)
    }

    override fun getProducts(): List<Product> {
        val jsonString = prefs.getString(KEY_PRODUCTS, null) ?: return emptyList()
        return parseProductsJson(jsonString)
    }

    override fun saveProducts(products: List<Product>) {
        val jsonArray = JSONArray()
        products.forEach { product ->
            jsonArray.put(product.toJson())
        }
        val jsonString = jsonArray.toString()
        prefs.edit().putString(KEY_PRODUCTS, jsonString).apply()
        FirestoreSyncHelper.syncDataUp("products", jsonString)
    }

    override fun getCategories(): List<String> {
        val jsonString = prefs.getString(KEY_CATEGORIES, null) ?: return emptyList()
        return parseStringListJson(jsonString)
    }

    override fun saveCategory(category: String) {
        val trimmed = category.trim()
        if (trimmed.isEmpty()) return
        val current = getCategories().toMutableList()
        if (!current.contains(trimmed)) {
            current.add(trimmed)
            val jsonArray = JSONArray()
            current.forEach { jsonArray.put(it) }
            val jsonString = jsonArray.toString()
            prefs.edit().putString(KEY_CATEGORIES, jsonString).apply()
            FirestoreSyncHelper.syncDataUp("categories", jsonString)
            onCategoriesUpdated?.invoke(current)
        }
        val order = getCategoryOrder().toMutableList()
        if (!order.contains(trimmed)) {
            order.add(trimmed)
            saveCategoryOrder(order)
        }
    }

    override fun deleteCategory(category: String) {
        val trimmed = category.trim()
        val currentCategories = getCategories().filter { it != trimmed }
        val jsonArray1 = JSONArray()
        currentCategories.forEach { jsonArray1.put(it) }
        val jsonStr1 = jsonArray1.toString()
        prefs.edit().putString(KEY_CATEGORIES, jsonStr1).apply()
        FirestoreSyncHelper.syncDataUp("categories", jsonStr1)
        onCategoriesUpdated?.invoke(currentCategories)

        val currentOrder = getCategoryOrder().filter { it != trimmed }
        val jsonArray2 = JSONArray()
        currentOrder.forEach { jsonArray2.put(it) }
        val jsonStr2 = jsonArray2.toString()
        prefs.edit().putString(KEY_CATEGORY_ORDER, jsonStr2).apply()
        FirestoreSyncHelper.syncDataUp("category_order", jsonStr2)
        onCategoryOrderUpdated?.invoke(currentOrder)
    }

    override fun getCategoryOrder(): List<String> {
        val jsonString = prefs.getString(KEY_CATEGORY_ORDER, null) ?: return emptyList()
        return parseStringListJson(jsonString)
    }

    override fun saveCategoryOrder(categories: List<String>) {
        val jsonArray = JSONArray()
        categories.forEach { jsonArray.put(it) }
        val jsonString = jsonArray.toString()
        prefs.edit().putString(KEY_CATEGORY_ORDER, jsonString).apply()
        FirestoreSyncHelper.syncDataUp("category_order", jsonString)
        onCategoryOrderUpdated?.invoke(categories)
    }

    private fun parseProductsJson(jsonString: String): List<Product> {
        return try {
            val jsonArray = JSONArray(jsonString)
            val list = mutableListOf<Product>()
            for (i in 0 until jsonArray.length()) {
                val jsonObject = jsonArray.getJSONObject(i)
                list.add(jsonObject.toProduct())
            }
            list
        } catch (e: Exception) {
            emptyList()
        }
    }

    private fun parseStringListJson(jsonString: String): List<String> {
        return try {
            val jsonArray = JSONArray(jsonString)
            val list = mutableListOf<String>()
            for (i in 0 until jsonArray.length()) {
                list.add(jsonArray.getString(i))
            }
            list
        } catch (e: Exception) {
            emptyList()
        }
    }

    private fun Product.toJson(): JSONObject {
        return JSONObject().apply {
            put("id", id)
            put("name", name)
            put("price", price)
            put("costPrice", costPrice)
            put("quantity", 0) // La cantidad del carrito de compras es exclusivamente local de cada dispositivo
            put("category", category)
            put("stock", stock)
            put("position", position)
            put("barcode", barcode)
        }
    }

    private fun JSONObject.toProduct(): Product {
        return Product(
            id = getString("id"),
            name = getString("name"),
            price = getDouble("price"),
            costPrice = optDouble("costPrice", 0.0),
            quantity = 0,
            category = optString("category", "General"),
            stock = optInt("stock", 0),
            position = optInt("position", 0),
            barcode = optString("barcode", "")
        )
    }
}
