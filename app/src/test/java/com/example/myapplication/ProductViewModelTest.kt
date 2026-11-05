package com.example.myapplication

import com.example.myapplication.model.Product
import com.example.myapplication.repository.IProductRepository
import com.example.myapplication.viewmodel.ProductViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class FakeProductRepository : IProductRepository {
    private var storedProducts = mutableListOf<Product>()
    private var storedBusinessName: String? = null
    private var storedCategories = mutableListOf<String>()
    private var storedCategoryOrder = mutableListOf<String>()

    override fun getProducts(): List<Product> = storedProducts

    override fun saveProducts(products: List<Product>) {
        storedProducts = products.toMutableList()
    }

    override fun getBusinessName(): String? = storedBusinessName

    override fun saveBusinessName(name: String) {
        storedBusinessName = name
    }

    override fun getCategories(): List<String> = storedCategories

    override fun saveCategory(category: String) {
        if (!storedCategories.contains(category)) {
            storedCategories.add(category)
        }
        if (!storedCategoryOrder.contains(category)) {
            storedCategoryOrder.add(category)
        }
    }

    override fun deleteCategory(category: String) {
        storedCategories.remove(category)
        storedCategoryOrder.remove(category)
    }

    override fun getCategoryOrder(): List<String> = storedCategoryOrder

    override fun saveCategoryOrder(categories: List<String>) {
        storedCategoryOrder = categories.toMutableList()
    }

    override fun setOnProductsUpdatedListener(listener: (List<Product>) -> Unit) {}
    override fun setOnBusinessNameUpdatedListener(listener: (String) -> Unit) {}
    override fun setOnCategoriesUpdatedListener(listener: (List<String>) -> Unit) {}
    override fun setOnCategoryOrderUpdatedListener(listener: (List<String>) -> Unit) {}
}

@OptIn(ExperimentalCoroutinesApi::class)
class ProductViewModelTest {

    private val testDispatcher = UnconfinedTestDispatcher()
    private lateinit var fakeRepository: FakeProductRepository
    private lateinit var viewModel: ProductViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        fakeRepository = FakeProductRepository()
        viewModel = ProductViewModel(fakeRepository)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun addProduct_startsAtQuantityZero_andTotalIsZero() = runBlocking {
        viewModel.addProduct("Leche", 150.0, "Lácteos", 10)

        val products = viewModel.products.value
        assertEquals(1, products.size)
        assertEquals("Leche", products[0].name)
        assertEquals(150.0, products[0].price, 0.001)
        assertEquals(0, products[0].quantity)
        assertEquals(10, products[0].stock)

        val total = viewModel.totalPrice.first()
        assertEquals(0.0, total, 0.001)
    }

    @Test
    fun addProduct_withCostPrice_calculatesNetProfitCorrectly() = runBlocking {
        viewModel.addProduct("Remera", 5000.0, "Ropa", 20, "", 3000.0)

        val product = viewModel.products.value[0]
        assertEquals(5000.0, product.price, 0.001)
        assertEquals(3000.0, product.costPrice, 0.001)
        assertEquals(2000.0, product.netProfitPerUnit, 0.001)

        viewModel.incrementQuantity(product.id)
        viewModel.incrementQuantity(product.id) // qty = 2

        val updated = viewModel.products.value[0]
        assertEquals(10000.0, updated.totalRevenue, 0.001)
        assertEquals(6000.0, updated.totalCostPrice, 0.001)
        assertEquals(4000.0, updated.totalNetProfit, 0.001)
    }

    @Test
    fun addQuickSaleProduct_createsTemporaryItem_andRemovesItOnReset() {
        viewModel.addProduct("Producto Normal", 1000.0)
        viewModel.addQuickSaleProduct("Servicio Extra", 500.0, 2)

        assertEquals(2, viewModel.products.value.size)
        val quickItem = viewModel.products.value.find { it.isTemporary }
        assertTrue(quickItem != null)
        assertEquals("Servicio Extra", quickItem?.name)
        assertEquals(2, quickItem?.quantity)

        viewModel.resetQuantities()

        // El ítem temporal debe eliminarse de la lista tras reiniciar cantidades post venta
        assertEquals(1, viewModel.products.value.size)
        assertEquals("Producto Normal", viewModel.products.value[0].name)
    }

    @Test
    fun moveCategoryUpAndDown_reordersCategoriesCorrectly() {
        viewModel.addProduct("Item 1", 100.0, "Categoría A")
        viewModel.addProduct("Item 2", 200.0, "Categoría B")

        val initialOrder = viewModel.categoryOrder.value
        assertEquals(listOf("Categoría A", "Categoría B"), initialOrder)

        viewModel.moveCategoryDown("Categoría A")
        assertEquals(listOf("Categoría B", "Categoría A"), viewModel.categoryOrder.value)

        viewModel.moveCategoryUp("Categoría A")
        assertEquals(listOf("Categoría A", "Categoría B"), viewModel.categoryOrder.value)
    }

    @Test
    fun addProductWithCategory_savesCategory() {
        viewModel.addProduct("Sticker Holo", 400.0, "Stickers")

        val product = viewModel.products.value[0]
        assertEquals("Stickers", product.category)
        assertTrue(fakeRepository.getCategories().contains("Stickers"))
    }

    @Test
    fun resetQuantities_resetsAllProductQuantitiesToZero() {
        viewModel.addProduct("Pin38", 800.0, "Pins")
        val productId = viewModel.products.value[0].id
        viewModel.incrementQuantity(productId)
        viewModel.incrementQuantity(productId)

        assertEquals(2, viewModel.products.value[0].quantity)

        viewModel.resetQuantities()

        assertEquals(0, viewModel.products.value[0].quantity)
    }

    @Test
    fun incrementQuantity_increasesProductQuantity_andUpdatesTotal() = runBlocking {
        viewModel.addProduct("Pan", 50.0)
        val productId = viewModel.products.value[0].id

        viewModel.incrementQuantity(productId) // qty = 1

        val updatedProduct = viewModel.products.value[0]
        assertEquals(1, updatedProduct.quantity)
        assertEquals(50.0, updatedProduct.totalCost, 0.001)

        val total = viewModel.totalPrice.first()
        assertEquals(50.0, total, 0.001)
    }

    @Test
    fun decrementQuantity_neverBelowZero() = runBlocking {
        viewModel.addProduct("Arroz", 80.0)
        val productId = viewModel.products.value[0].id

        viewModel.incrementQuantity(productId) // qty = 1
        viewModel.decrementQuantity(productId) // qty = 0
        viewModel.decrementQuantity(productId) // qty debe seguir en 0

        val updatedProduct = viewModel.products.value[0]
        assertEquals(0, updatedProduct.quantity)
        assertEquals(0.0, updatedProduct.totalCost, 0.001)
    }

    @Test
    fun removeProduct_removesItemFromList() {
        viewModel.addProduct("Manzana", 30.0)
        val productId = viewModel.products.value[0].id

        viewModel.removeProduct(productId)

        assertTrue(viewModel.products.value.isEmpty())
    }

    @Test
    fun clearAllProducts_resetsList() {
        viewModel.addProduct("Producto 1", 10.0)
        viewModel.addProduct("Producto 2", 20.0)

        viewModel.clearAllProducts()

        assertTrue(viewModel.products.value.isEmpty())
    }

    @Test
    fun exportAndImportProducts_worksCorrectly() {
        viewModel.addProduct("Stickers", 400.0, "Stickers")
        viewModel.addProduct("Pin55", 1300.0, "Pins")

        val exportedJson = viewModel.exportProductsJson()
        assertTrue(exportedJson.contains("Stickers"))
        assertTrue(exportedJson.contains("1300"))

        val success = viewModel.importProductsFromJson(exportedJson, replaceExisting = true)
        assertTrue(success)
        assertEquals(2, viewModel.products.value.size)

        val invalidSuccess = viewModel.importProductsFromJson("invalid json", replaceExisting = false)
        assertFalse(invalidSuccess)
    }

    @Test
    fun setBusinessName_updatesStateAndRepository() {
        assertTrue(viewModel.showBusinessNameDialog.value)

        viewModel.setBusinessName("Feriventas Don Juan")

        assertEquals("Feriventas Don Juan", viewModel.businessName.value)
        assertEquals("Feriventas Don Juan", fakeRepository.getBusinessName())
        assertFalse(viewModel.showBusinessNameDialog.value)
    }

    @Test
    fun persistence_savesProductsInRepository() {
        viewModel.addProduct("Harina", 60.0)

        val savedInRepo = fakeRepository.getProducts()
        assertEquals(1, savedInRepo.size)
        assertEquals("Harina", savedInRepo[0].name)
        assertEquals(0, savedInRepo[0].quantity)
    }
}
