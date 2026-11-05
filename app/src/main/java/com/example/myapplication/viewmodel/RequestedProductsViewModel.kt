package com.example.myapplication.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import com.example.myapplication.model.RequestedProduct
import com.example.myapplication.repository.RequestedProductsRepository
import com.example.myapplication.repository.RequestedProductsRepositoryInterface
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class RequestedProductsViewModel private constructor(
    application: Application?,
    private val repository: RequestedProductsRepositoryInterface
) : AndroidViewModel(application ?: Application()) {

    constructor(application: Application) : this(
        application = application,
        repository = RequestedProductsRepository(application)
    )

    constructor() : this(
        application = null,
        repository = object : RequestedProductsRepositoryInterface {
            override val requestedProducts: StateFlow<List<RequestedProduct>> = MutableStateFlow(emptyList())
            override fun addRequestedProduct(product: RequestedProduct) {}
            override fun updateRequestedProduct(product: RequestedProduct) {}
            override fun toggleRequestedProductCompleted(id: String) {}
            override fun deleteRequestedProduct(id: String) {}
        }
    )

    val requestedProducts: StateFlow<List<RequestedProduct>> = repository.requestedProducts

    fun addRequestedProduct(name: String, customerName: String, note: String, quantity: Int) {
        if (name.isBlank()) return
        val product = RequestedProduct(
            name = name.trim(),
            customerName = customerName.trim(),
            note = note.trim(),
            quantity = quantity
        )
        repository.addRequestedProduct(product)
    }

    fun updateRequestedProduct(product: RequestedProduct) {
        repository.updateRequestedProduct(product)
    }

    fun toggleRequestedProductCompleted(id: String) {
        repository.toggleRequestedProductCompleted(id)
    }

    fun deleteRequestedProduct(id: String) {
        repository.deleteRequestedProduct(id)
    }
}
