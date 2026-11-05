package com.example.myapplication.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import com.example.myapplication.model.Product
import com.example.myapplication.model.SaleItem
import com.example.myapplication.repository.IProductRepository
import com.example.myapplication.repository.ProductRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

class ProductViewModel private constructor(
    application: Application?,
    private val repository: IProductRepository
) : AndroidViewModel(application ?: Application()) {

    constructor(application: Application) : this(
        application = application,
        repository = ProductRepository(application)
    )

    // Constructor sin argumentos para Jetpack Compose Previews
    constructor() : this(
        application = null,
        repository = object : IProductRepository {
            override fun getProducts(): List<Product> = emptyList()
            override fun saveProducts(products: List<Product>) {}
            override fun getBusinessName(): String? = "Mi Emprendimiento"
            override fun saveBusinessName(name: String) {}
            override fun getCategories(): List<String> = listOf("Stickers", "Pins", "General")
            override fun saveCategory(category: String) {}
            override fun deleteCategory(category: String) {}
            override fun getCategoryOrder(): List<String> = listOf("Stickers", "Pins", "General")
            override fun saveCategoryOrder(categories: List<String>) {}
            override fun setOnProductsUpdatedListener(listener: (List<Product>) -> Unit) {}
            override fun setOnBusinessNameUpdatedListener(listener: (String) -> Unit) {}
            override fun setOnCategoriesUpdatedListener(listener: (List<String>) -> Unit) {}
            override fun setOnCategoryOrderUpdatedListener(listener: (List<String>) -> Unit) {}
        }
    )

    // Constructor útil para pruebas unitarias sin dependencia de Context
    constructor(repository: IProductRepository) : this(
        application = null,
        repository = repository
    )

    private val _products = MutableStateFlow<List<Product>>(repository.getProducts())
    val products: StateFlow<List<Product>> = _products.asStateFlow()

    private val _categories = MutableStateFlow<List<String>>(repository.getCategories())
    val categories: StateFlow<List<String>> = _categories.asStateFlow()

    private val _categoryOrder = MutableStateFlow<List<String>>(repository.getCategoryOrder())
    val categoryOrder: StateFlow<List<String>> = _categoryOrder.asStateFlow()

    private val _collapsedCategories = MutableStateFlow<Set<String>>(emptySet())
    val collapsedCategories: StateFlow<Set<String>> = _collapsedCategories.asStateFlow()

    private val _showReorderCategoriesDialog = MutableStateFlow(false)
    val showReorderCategoriesDialog: StateFlow<Boolean> = _showReorderCategoriesDialog.asStateFlow()

    private val _showBulkStockDialog = MutableStateFlow(false)
    val showBulkStockDialog: StateFlow<Boolean> = _showBulkStockDialog.asStateFlow()

    private val _selectedProductForLabel = MutableStateFlow<Product?>(null)
    val selectedProductForLabel: StateFlow<Product?> = _selectedProductForLabel.asStateFlow()

    private val _businessName = MutableStateFlow(repository.getBusinessName() ?: "")
    val businessName: StateFlow<String> = _businessName.asStateFlow()

    private val _showBusinessNameDialog = MutableStateFlow(_businessName.value.isBlank())
    val showBusinessNameDialog: StateFlow<Boolean> = _showBusinessNameDialog.asStateFlow()

    private val _showAddDialog = MutableStateFlow(false)
    val showAddDialog: StateFlow<Boolean> = _showAddDialog.asStateFlow()

    private val _showQuickAddDialog = MutableStateFlow(false)
    val showQuickAddDialog: StateFlow<Boolean> = _showQuickAddDialog.asStateFlow()

    private val _productToEdit = MutableStateFlow<Product?>(null)
    val productToEdit: StateFlow<Product?> = _productToEdit.asStateFlow()

    private val _showEditDialog = MutableStateFlow(false)
    val showEditDialog: StateFlow<Boolean> = _showEditDialog.asStateFlow()

    private val _showClearConfirmDialog = MutableStateFlow(false)
    val showClearConfirmDialog: StateFlow<Boolean> = _showClearConfirmDialog.asStateFlow()

    private val _showExportDialog = MutableStateFlow(false)
    val showExportDialog: StateFlow<Boolean> = _showExportDialog.asStateFlow()

    private val _showImportDialog = MutableStateFlow(false)
    val showImportDialog: StateFlow<Boolean> = _showImportDialog.asStateFlow()

    private val _showCheckoutDialog = MutableStateFlow(false)
    val showCheckoutDialog: StateFlow<Boolean> = _showCheckoutDialog.asStateFlow()

    init {
        repository.setOnProductsUpdatedListener { newProducts ->
            val currentQuantities = _products.value.associate { it.id to it.quantity }
            _products.value = newProducts.map { product ->
                val localQty = currentQuantities[product.id] ?: 0
                product.copy(quantity = localQty)
            }
        }
        repository.setOnBusinessNameUpdatedListener { newName ->
            _businessName.value = newName
            if (newName.isNotBlank()) {
                _showBusinessNameDialog.value = false
            }
        }
        repository.setOnCategoriesUpdatedListener { newCategories ->
            _categories.value = newCategories
        }
        repository.setOnCategoryOrderUpdatedListener { newOrder ->
            _categoryOrder.value = newOrder
        }
    }

    // Total general acumulado (suma de precio * cantidad de todos los productos)
    val totalPrice = _products.map { list ->
        list.sumOf { it.totalCost }
    }

    // Total de unidades acumuladas
    val totalUnits = _products.map { list ->
        list.sumOf { it.quantity }
    }

    fun toggleCategoryCollapse(categoryName: String) {
        val current = _collapsedCategories.value
        _collapsedCategories.value = if (current.contains(categoryName)) {
            current - categoryName
        } else {
            current + categoryName
        }
    }

    fun moveCategoryUp(category: String) {
        val productCategories = _products.value.map { it.category.trim() }.filter { it.isNotEmpty() }.distinct()
        val currentOrder = (_categoryOrder.value + productCategories).distinct().toMutableList()
        val index = currentOrder.indexOf(category)
        if (index > 0) {
            val temp = currentOrder[index]
            currentOrder[index] = currentOrder[index - 1]
            currentOrder[index - 1] = temp
            _categoryOrder.value = currentOrder
            repository.saveCategoryOrder(currentOrder)
        }
    }

    fun moveCategoryDown(category: String) {
        val productCategories = _products.value.map { it.category.trim() }.filter { it.isNotEmpty() }.distinct()
        val currentOrder = (_categoryOrder.value + productCategories).distinct().toMutableList()
        val index = currentOrder.indexOf(category)
        if (index >= 0 && index < currentOrder.size - 1) {
            val temp = currentOrder[index]
            currentOrder[index] = currentOrder[index + 1]
            currentOrder[index + 1] = temp
            _categoryOrder.value = currentOrder
            repository.saveCategoryOrder(currentOrder)
        }
    }

    fun moveProductUpInCategory(product: Product) {
        val categoryProducts = _products.value
            .filter { it.category.trim().equals(product.category.trim(), ignoreCase = true) }
            .sortedWith(compareBy<Product> { it.position }.thenBy { it.name })

        val index = categoryProducts.indexOfFirst { it.id == product.id }
        if (index > 0) {
            val prevProduct = categoryProducts[index - 1]
            val prevPos = prevProduct.position
            val currPos = product.position
            val newPos1 = if (prevPos == currPos) prevPos else prevPos
            val newPos2 = if (prevPos == currPos) currPos + 1 else currPos

            _products.update { currentList ->
                currentList.map { p ->
                    when (p.id) {
                        product.id -> p.copy(position = newPos1)
                        prevProduct.id -> p.copy(position = newPos2)
                        else -> p
                    }
                }
            }
            repository.saveProducts(_products.value.filter { !it.isTemporary })
        }
    }

    fun moveProductDownInCategory(product: Product) {
        val categoryProducts = _products.value
            .filter { it.category.trim().equals(product.category.trim(), ignoreCase = true) }
            .sortedWith(compareBy<Product> { it.position }.thenBy { it.name })

        val index = categoryProducts.indexOfFirst { it.id == product.id }
        if (index >= 0 && index < categoryProducts.size - 1) {
            val nextProduct = categoryProducts[index + 1]
            val nextPos = nextProduct.position
            val currPos = product.position
            val newPos1 = if (nextPos == currPos) nextPos + 1 else nextPos
            val newPos2 = if (nextPos == currPos) currPos else currPos

            _products.update { currentList ->
                currentList.map { p ->
                    when (p.id) {
                        product.id -> p.copy(position = newPos1)
                        nextProduct.id -> p.copy(position = newPos2)
                        else -> p
                    }
                }
            }
            repository.saveProducts(_products.value.filter { !it.isTemporary })
        }
    }

    fun updateBulkStock(stockUpdates: Map<String, Int>) {
        _products.update { current ->
            current.map { product ->
                if (stockUpdates.containsKey(product.id)) {
                    val newStock = stockUpdates[product.id] ?: product.stock
                    product.copy(stock = newStock)
                } else {
                    product
                }
            }
        }
        repository.saveProducts(_products.value.filter { !it.isTemporary })
    }

    fun openReorderCategoriesDialog() {
        _showReorderCategoriesDialog.value = true
    }

    fun removeProductFromCart(productId: String) {
        _products.update { current ->
            current.map { if (it.id == productId) it.copy(quantity = 0) else it }
        }
    }

    fun closeReorderCategoriesDialog() {
        _showReorderCategoriesDialog.value = false
    }

    fun openBulkStockDialog() {
        _showBulkStockDialog.value = true
    }

    fun closeBulkStockDialog() {
        _showBulkStockDialog.value = false
    }

    fun openPrintLabelDialog(product: Product) {
        _selectedProductForLabel.value = product
    }

    fun closePrintLabelDialog() {
        _selectedProductForLabel.value = null
    }

    fun addProduct(name: String, price: Double, category: String = "General", stock: Int = 0, barcode: String = "", costPrice: Double = 0.0) {
        val trimmedName = name.trim()
        val trimmedCategory = category.trim().ifEmpty { "General" }
        if (trimmedName.isNotEmpty() && price >= 0) {
            val categoryProducts = _products.value.filter { it.category.trim().equals(trimmedCategory, ignoreCase = true) }
            val nextPos = (categoryProducts.maxOfOrNull { it.position } ?: -1) + 1
            val newProduct = Product(
                name = trimmedName,
                price = price,
                costPrice = costPrice.coerceAtLeast(0.0),
                quantity = 0,
                category = trimmedCategory,
                stock = stock.coerceAtLeast(0),
                position = nextPos,
                barcode = barcode.trim()
            )
            saveCategory(trimmedCategory)
            _products.update { currentList -> currentList + newProduct }
            repository.saveProducts(_products.value.filter { !it.isTemporary })
            closeAddDialog()
        }
    }

    fun addQuickSaleProduct(name: String, price: Double, quantity: Int = 1, category: String = "Varios") {
        val trimmedName = name.trim().ifEmpty { "Producto Varios" }
        val trimmedCategory = category.trim().ifEmpty { "Varios" }
        if (price >= 0 && quantity > 0) {
            val quickProduct = Product(
                name = trimmedName,
                price = price,
                quantity = quantity,
                category = trimmedCategory,
                stock = 0,
                isTemporary = true
            )
            _products.update { current -> current + quickProduct }
            closeQuickAddDialog()
        }
    }

    fun updateProduct(id: String, name: String, price: Double, category: String, stock: Int, barcode: String = "", costPrice: Double = 0.0) {
        val trimmedName = name.trim()
        val trimmedCategory = category.trim().ifEmpty { "General" }
        if (trimmedName.isNotEmpty() && price >= 0) {
            saveCategory(trimmedCategory)
            _products.update { list ->
                list.map { product ->
                    if (product.id == id) {
                        product.copy(
                            name = trimmedName,
                            price = price,
                            costPrice = costPrice.coerceAtLeast(0.0),
                            category = trimmedCategory,
                            stock = stock.coerceAtLeast(0),
                            barcode = barcode.trim()
                        )
                    } else {
                        product
                    }
                }
            }
            repository.saveProducts(_products.value.filter { !it.isTemporary })
            closeEditDialog()
        }
    }

    fun saveCategory(category: String) {
        val trimmed = category.trim()
        if (trimmed.isNotEmpty()) {
            repository.saveCategory(trimmed)
            _categories.value = repository.getCategories()
            _categoryOrder.value = repository.getCategoryOrder()
        }
    }

    fun deleteCategory(categoryName: String) {
        val trimmed = categoryName.trim()
        if (trimmed.isNotEmpty()) {
            repository.deleteCategory(trimmed)
            _categories.value = repository.getCategories()
            _categoryOrder.value = repository.getCategoryOrder()
            // Reasignamos los productos pertenecientes a esa categoría a "General"
            _products.update { currentList ->
                currentList.map { product ->
                    if (product.category.trim().equals(trimmed, ignoreCase = true)) {
                        product.copy(category = "General")
                    } else product
                }
            }
            repository.saveProducts(_products.value.filter { !it.isTemporary })
        }
    }

    fun incrementQuantity(productId: String) {
        _products.update { list ->
            list.map { product ->
                if (product.id == productId) {
                    val nextQty = product.quantity + 1
                    // Previene seleccionar más unidades que las disponibles en stock (si stock > 0)
                    if (product.stock == 0 || nextQty <= product.stock) {
                        product.copy(quantity = nextQty)
                    } else {
                        product
                    }
                } else {
                    product
                }
            }
        }
    }

    fun decrementQuantity(productId: String) {
        _products.update { list ->
            list.map { product ->
                if (product.id == productId) {
                    val newQty = product.quantity - 1
                    if (newQty >= 0) {
                        product.copy(quantity = newQty)
                    } else {
                        product
                    }
                } else {
                    product
                }
            }
        }
    }

    fun deductStockForSale(saleItems: List<SaleItem>) {
        _products.update { currentList ->
            currentList.map { product ->
                val soldItem = saleItems.find { it.productId == product.id }
                if (soldItem != null && product.stock > 0) {
                    val newStock = (product.stock - soldItem.quantity).coerceAtLeast(0)
                    product.copy(stock = newStock)
                } else {
                    product
                }
            }
        }
        repository.saveProducts(_products.value.filter { !it.isTemporary })
    }

    fun resetQuantities() {
        _products.update { list ->
            list.filter { !it.isTemporary }.map { it.copy(quantity = 0) }
        }
    }

    fun cancelSale() {
        _products.update { list ->
            list.filter { !it.isTemporary }.map { it.copy(quantity = 0) }
        }
    }

    fun removeProduct(productId: String) {
        _products.update { list ->
            list.filter { it.id != productId }
        }
        repository.saveProducts(_products.value.filter { !it.isTemporary })
    }

    fun clearAllProducts() {
        _products.value = emptyList()
        repository.saveProducts(emptyList())
        closeClearDialog()
    }

    fun setBusinessName(name: String) {
        val trimmed = name.trim()
        if (trimmed.isNotEmpty()) {
            _businessName.value = trimmed
            repository.saveBusinessName(trimmed)
            _showBusinessNameDialog.value = false
        }
    }

    fun openBusinessNameDialog() {
        _showBusinessNameDialog.value = true
    }

    fun closeBusinessNameDialog() {
        if (_businessName.value.isNotBlank()) {
            _showBusinessNameDialog.value = false
        }
    }

    fun openAddDialog() {
        _showAddDialog.value = true
    }

    fun closeAddDialog() {
        _showAddDialog.value = false
    }

    fun openQuickAddDialog() {
        _showQuickAddDialog.value = true
    }

    fun closeQuickAddDialog() {
        _showQuickAddDialog.value = false
    }

    fun openEditDialog(product: Product) {
        _productToEdit.value = product
        _showEditDialog.value = true
    }

    fun closeEditDialog() {
        _productToEdit.value = null
        _showEditDialog.value = false
    }

    fun openClearDialog() {
        _showClearConfirmDialog.value = true
    }

    fun closeClearDialog() {
        _showClearConfirmDialog.value = false
    }

    fun openExportDialog() {
        _showExportDialog.value = true
    }

    fun closeExportDialog() {
        _showExportDialog.value = false
    }

    fun openImportDialog() {
        _showImportDialog.value = true
    }

    fun closeImportDialog() {
        _showImportDialog.value = false
    }

    fun openCheckoutDialog() {
        _showCheckoutDialog.value = true
    }

    fun closeCheckoutDialog() {
        _showCheckoutDialog.value = false
    }

    fun exportProductsJson(): String {
        val jsonArray = JSONArray()
        _products.value.filter { !it.isTemporary }.forEach { product ->
            val jsonObject = JSONObject().apply {
                put("id", product.id)
                put("name", product.name)
                put("price", product.price)
                put("costPrice", product.costPrice)
                put("quantity", product.quantity)
                put("category", product.category)
                put("stock", product.stock)
                put("position", product.position)
                put("barcode", product.barcode)
            }
            jsonArray.put(jsonObject)
        }
        return jsonArray.toString(2)
    }

    fun parseProductsFromJson(jsonString: String): List<Product>? {
        return try {
            val jsonArray = JSONArray(jsonString.trim())
            val importedList = mutableListOf<Product>()
            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)
                val name = obj.getString("name")
                val price = obj.getDouble("price")
                val costPrice = obj.optDouble("costPrice", 0.0)
                val quantity = obj.optInt("quantity", 0)
                val category = obj.optString("category", "General")
                val stock = obj.optInt("stock", 0)
                val position = obj.optInt("position", 0)
                val barcode = obj.optString("barcode", "")
                val id = if (obj.has("id")) obj.getString("id") else UUID.randomUUID().toString()
                importedList.add(
                    Product(
                        id = id,
                        name = name,
                        price = price,
                        costPrice = costPrice,
                        quantity = quantity,
                        category = category,
                        stock = stock,
                        position = position,
                        barcode = barcode
                    )
                )
            }
            if (importedList.isEmpty()) null else importedList
        } catch (e: Exception) {
            null
        }
    }

    fun importProductsFromJson(jsonString: String, replaceExisting: Boolean): Boolean {
        val importedList = parseProductsFromJson(jsonString) ?: return false

        _products.update { current ->
            if (replaceExisting) {
                importedList
            } else {
                current + importedList.map { item ->
                    item.copy(id = UUID.randomUUID().toString())
                }
            }
        }
        repository.saveProducts(_products.value.filter { !it.isTemporary })
        closeImportDialog()
        return true
    }
}
