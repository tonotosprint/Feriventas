package com.example.myapplication

import com.example.myapplication.model.PaymentMethod
import com.example.myapplication.model.SaleItem
import com.example.myapplication.model.SaleRecord
import com.example.myapplication.repository.ISalesRepository
import com.example.myapplication.viewmodel.SalesViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class FakeSalesRepository : ISalesRepository {
    private var sales = mutableListOf<SaleRecord>()

    override fun getSales(): List<SaleRecord> = sales

    override fun addSale(sale: SaleRecord) {
        sales.add(0, sale)
    }

    override fun removeSale(saleId: String) {
        sales.removeAll { it.id == saleId }
    }

    override fun clearSales() {
        sales.clear()
    }

    override fun setOnSalesUpdatedListener(listener: (List<SaleRecord>) -> Unit) {}
}

@OptIn(ExperimentalCoroutinesApi::class)
class SalesViewModelTest {

    private val testDispatcher = UnconfinedTestDispatcher()
    private lateinit var fakeRepository: FakeSalesRepository
    private lateinit var viewModel: SalesViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        fakeRepository = FakeSalesRepository()
        viewModel = SalesViewModel(fakeRepository)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun addSale_calculatesTodayTotalsAndPaymentMethodsCorrectly() = runBlocking {
        val sale1 = SaleRecord(
            items = listOf(
                SaleItem(productId = "1", productName = "Sticker", price = 400.0, quantity = 3, category = "Stickers")
            ),
            totalAmount = 1200.0,
            paymentMethod = PaymentMethod.CASH,
            amountPaid = 2000.0,
            changeGiven = 800.0
        )

        val sale2 = SaleRecord(
            items = listOf(
                SaleItem(productId = "2", productName = "Pin55", price = 1300.0, quantity = 1, category = "Pins")
            ),
            totalAmount = 1300.0,
            paymentMethod = PaymentMethod.TRANSFER
        )

        viewModel.addSale(sale1)
        viewModel.addSale(sale2)

        val todayTotal = viewModel.todayTotal.first()
        val todayCash = viewModel.todayCashTotal.first()
        val todayTransfer = viewModel.todayTransferTotal.first()
        val transactionCount = viewModel.todayTransactionCount.first()

        assertEquals(2500.0, todayTotal, 0.001)
        assertEquals(1200.0, todayCash, 0.001)
        assertEquals(1300.0, todayTransfer, 0.001)
        assertEquals(2, transactionCount)
    }

    @Test
    fun deleteSale_removesSaleRecord() = runBlocking {
        val sale = SaleRecord(
            items = listOf(SaleItem("1", "Remera", 5000.0, 1)),
            totalAmount = 5000.0,
            paymentMethod = PaymentMethod.CASH
        )

        viewModel.addSale(sale)
        assertEquals(1, viewModel.sales.value.size)

        viewModel.deleteSale(sale.id)
        assertTrue(viewModel.sales.value.isEmpty())
    }
}
