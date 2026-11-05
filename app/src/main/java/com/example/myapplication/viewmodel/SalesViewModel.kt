package com.example.myapplication.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import com.example.myapplication.model.PaymentMethod
import com.example.myapplication.model.SaleRecord
import com.example.myapplication.repository.ISalesRepository
import com.example.myapplication.repository.SalesRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import java.util.Calendar

data class TopProductStat(
    val productName: String,
    val totalQuantity: Int,
    val totalRevenue: Double
)

class SalesViewModel private constructor(
    application: Application?,
    private val repository: ISalesRepository
) : AndroidViewModel(application ?: Application()) {

    constructor(application: Application) : this(
        application = application,
        repository = SalesRepository(application)
    )

    // Constructor sin argumentos para Jetpack Compose Previews
    constructor() : this(
        application = null,
        repository = object : ISalesRepository {
            private var salesList = mutableListOf<SaleRecord>()
            override fun getSales() = salesList
            override fun addSale(sale: SaleRecord) { salesList.add(0, sale) }
            override fun removeSale(saleId: String) { salesList.removeAll { it.id == saleId } }
            override fun clearSales() { salesList.clear() }
            override fun setOnSalesUpdatedListener(listener: (List<SaleRecord>) -> Unit) {}
        }
    )

    constructor(repository: ISalesRepository) : this(
        application = null,
        repository = repository
    )

    private val _sales = MutableStateFlow<List<SaleRecord>>(repository.getSales())
    val sales: StateFlow<List<SaleRecord>> = _sales.asStateFlow()

    init {
        repository.setOnSalesUpdatedListener { newSales ->
            _sales.value = newSales
        }
    }

    private val _selectedDateMillis = MutableStateFlow(System.currentTimeMillis())
    val selectedDateMillis: StateFlow<Long> = _selectedDateMillis.asStateFlow()

    fun setSelectedDate(millis: Long) {
        _selectedDateMillis.value = millis
    }

    // Ventas de la fecha seleccionada
    val selectedDateSales = combine(_sales, _selectedDateMillis) { list, dateMillis ->
        val targetCal = Calendar.getInstance().apply { timeInMillis = dateMillis }
        list.filter { sale ->
            val saleCal = Calendar.getInstance().apply { timeInMillis = sale.timestamp }
            saleCal.get(Calendar.YEAR) == targetCal.get(Calendar.YEAR) &&
                    saleCal.get(Calendar.DAY_OF_YEAR) == targetCal.get(Calendar.DAY_OF_YEAR)
        }
    }

    val selectedDateTotal = selectedDateSales.map { list -> list.sumOf { it.totalAmount } }

    val selectedDateCashTotal = selectedDateSales.map { list ->
        list.filter { it.paymentMethod == PaymentMethod.CASH }.sumOf { it.totalAmount }
    }

    val selectedDateTransferTotal = selectedDateSales.map { list ->
        list.filter { it.paymentMethod == PaymentMethod.TRANSFER }.sumOf { it.totalAmount }
    }

    val selectedDateTransactionCount = selectedDateSales.map { list -> list.size }

    // Ranking de productos más vendidos en la fecha seleccionada
    val selectedDateTopProducts = selectedDateSales.map { list ->
        val map = mutableMapOf<String, Pair<Int, Double>>()
        list.flatMap { it.items }.forEach { item ->
            val current = map.getOrDefault(item.productName, Pair(0, 0.0))
            map[item.productName] = Pair(
                current.first + item.quantity,
                current.second + item.totalCost
            )
        }
        map.map { (name, stats) ->
            TopProductStat(
                productName = name,
                totalQuantity = stats.first,
                totalRevenue = stats.second
            )
        }.sortedByDescending { it.totalQuantity }
    }

    // Compatibilidad con hoy
    val todaySales = _sales.map { list ->
        val todayCalendar = Calendar.getInstance()
        list.filter { sale ->
            val saleCalendar = Calendar.getInstance().apply { timeInMillis = sale.timestamp }
            saleCalendar.get(Calendar.YEAR) == todayCalendar.get(Calendar.YEAR) &&
                    saleCalendar.get(Calendar.DAY_OF_YEAR) == todayCalendar.get(Calendar.DAY_OF_YEAR)
        }
    }

    val todayTotal = todaySales.map { list -> list.sumOf { it.totalAmount } }

    val todayCashTotal = todaySales.map { list ->
        list.filter { it.paymentMethod == PaymentMethod.CASH }.sumOf { it.totalAmount }
    }

    val todayTransferTotal = todaySales.map { list ->
        list.filter { it.paymentMethod == PaymentMethod.TRANSFER }.sumOf { it.totalAmount }
    }

    val todayTransactionCount = todaySales.map { list -> list.size }

    val todayTopProducts = todaySales.map { list ->
        val map = mutableMapOf<String, Pair<Int, Double>>()
        list.flatMap { it.items }.forEach { item ->
            val current = map.getOrDefault(item.productName, Pair(0, 0.0))
            map[item.productName] = Pair(
                current.first + item.quantity,
                current.second + item.totalCost
            )
        }
        map.map { (name, stats) ->
            TopProductStat(
                productName = name,
                totalQuantity = stats.first,
                totalRevenue = stats.second
            )
        }.sortedByDescending { it.totalQuantity }
    }

    fun addSale(sale: SaleRecord) {
        repository.addSale(sale)
        _sales.value = repository.getSales()
    }

    fun deleteSale(saleId: String) {
        repository.removeSale(saleId)
        _sales.value = repository.getSales()
    }

    fun clearAllSales() {
        repository.clearSales()
        _sales.value = emptyList()
    }
}
