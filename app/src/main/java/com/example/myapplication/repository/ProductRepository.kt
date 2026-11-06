package com.example.myapplication.repository

import android.content.Context
import android.content.SharedPreferences
import com.example.myapplication.model.Product
import com.example.myapplication.util.FirestoreSyncHelper
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

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
            } else {
                prefs.edit().remove(KEY_BUSINESS_NAME).apply()
                onBusinessNameUpdated?.invoke("")
            }
        }
        FirestoreSyncHelper.listenToData("products") { json ->
            if (!json.isNullOrEmpty()) {
                prefs.edit().putString(KEY_PRODUCTS, json).apply()
                onProductsUpdated?.invoke(parseProductsJson(json))
            } else {
                prefs.edit().remove(KEY_PRODUCTS).apply()
                onProductsUpdated?.invoke(emptyList())
            }
        }
        FirestoreSyncHelper.listenToData("categories") { json ->
            if (!json.isNullOrEmpty()) {
                prefs.edit().putString(KEY_CATEGORIES, json).apply()
                onCategoriesUpdated?.invoke(parseStringListJson(json))
            } else {
                prefs.edit().remove(KEY_CATEGORIES).apply()
                onCategoriesUpdated?.invoke(emptyList())
            }
        }
        FirestoreSyncHelper.listenToData("category_order") { json ->
            if (!json.isNullOrEmpty()) {
                prefs.edit().putString(KEY_CATEGORY_ORDER, json).apply()
                onCategoryOrderUpdated?.invoke(parseStringListJson(json))
            } else {
                prefs.edit().remove(KEY_CATEGORY_ORDER).apply()
                onCategoryOrderUpdated?.invoke(emptyList())
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
            val obj = JSONObject().apply {
                put("id", product.id)
                put("name", product.name)
                put("price", product.price)
                put("costPrice", product.costPrice)
                put("quantity", product.quantity)
                put("category", product.category)
                put("stock", product.stock)
                put("isTemporary", product.isTemporary)
                put("position", product.position)
                put("barcode", product.barcode)
            }
            jsonArray.put(obj)
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
        val currentCategories = getCategories().toMutableList()
        val trimmed = category.trim()
        if (trimmed.isNotEmpty() && !currentCategories.contains(trimmed)) {
            currentCategories.add(trimmed)
            val jsonArray = JSONArray()
            currentCategories.forEach { jsonArray.put(it) }
            val jsonString = jsonArray.toString()
            prefs.edit().putString(KEY_CATEGORIES, jsonString).apply()
            FirestoreSyncHelper.syncDataUp("categories", jsonString)
            onCategoriesUpdated?.invoke(currentCategories)
        }
    }

    override fun deleteCategory(category: String) {
        val currentCategories = getCategories().filter { it != category }
        val jsonArray = JSONArray()
        currentCategories.forEach { jsonArray.put(it) }
        val jsonString = jsonArray.toString()
        prefs.edit().putString(KEY_CATEGORIES, jsonString).apply()
        FirestoreSyncHelper.syncDataUp("categories", jsonString)

        val currentOrder = getCategoryOrder().filter { it != category }
        saveCategoryOrder(currentOrder)

        onCategoriesUpdated?.invoke(currentCategories)
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
                val obj = jsonArray.getJSONObject(i)
                list.add(
                    Product(
                        id = obj.optString("id", UUID.randomUUID().toString()),
                        name = obj.optString("name", ""),
                        price = obj.optDouble("price", 0.0),
                        costPrice = obj.optDouble("costPrice", 0.0),
                        quantity = obj.optInt("quantity", 0),
                        category = obj.optString("category", "General"),
                        stock = obj.optInt("stock", 0),
                        isTemporary = obj.optBoolean("isTemporary", false),
                        position = obj.optInt("position", 0),
                        barcode = obj.optString("barcode", "")
                    )
                )
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
}
