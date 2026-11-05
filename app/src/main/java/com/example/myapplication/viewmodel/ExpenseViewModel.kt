package com.example.myapplication.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import com.example.myapplication.model.ExpenseRecord
import com.example.myapplication.repository.ExpenseRepository
import com.example.myapplication.repository.IExpenseRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class ExpenseViewModel private constructor(
    application: Application?,
    private val repository: IExpenseRepository
) : AndroidViewModel(application ?: Application()) {

    constructor(application: Application) : this(
        application = application,
        repository = ExpenseRepository(application)
    )

    // Preview constructor
    constructor() : this(
        application = null,
        repository = object : IExpenseRepository {
            private val list = mutableListOf<ExpenseRecord>()
            override fun getExpenses(): List<ExpenseRecord> = list
            override fun addExpense(expense: ExpenseRecord) { list.add(0, expense) }
            override fun removeExpense(expenseId: String) { list.removeAll { it.id == expenseId } }
            override fun clearExpenses() { list.clear() }
            override fun setOnExpensesUpdatedListener(listener: (List<ExpenseRecord>) -> Unit) {}
        }
    )

    constructor(repository: IExpenseRepository) : this(
        application = null,
        repository = repository
    )

    private val _expenses = MutableStateFlow<List<ExpenseRecord>>(repository.getExpenses())
    val expenses: StateFlow<List<ExpenseRecord>> = _expenses.asStateFlow()

    init {
        repository.setOnExpensesUpdatedListener { newExpenses ->
            _expenses.value = newExpenses
        }
    }

    fun addExpense(eventName: String, category: String, amount: Double, description: String = "") {
        val trimmedEvent = eventName.trim().ifEmpty { "General" }
        val trimmedCategory = category.trim().ifEmpty { "Varios" }
        if (amount > 0) {
            val record = ExpenseRecord(
                eventName = trimmedEvent,
                category = trimmedCategory,
                amount = amount,
                description = description.trim()
            )
            repository.addExpense(record)
            _expenses.value = repository.getExpenses()
        }
    }

    fun deleteExpense(expenseId: String) {
        repository.removeExpense(expenseId)
        _expenses.value = repository.getExpenses()
    }

    fun clearAllExpenses() {
        repository.clearExpenses()
        _expenses.value = emptyList()
    }
}
