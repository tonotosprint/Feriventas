package com.example.myapplication.repository

import android.content.Context
import android.content.SharedPreferences
import com.example.myapplication.model.ExpenseRecord
import com.example.myapplication.util.FirestoreSyncHelper
import org.json.JSONArray
import org.json.JSONObject

interface IExpenseRepository {
    fun getExpenses(): List<ExpenseRecord>
    fun addExpense(expense: ExpenseRecord)
    fun removeExpense(expenseId: String)
    fun clearExpenses()
    fun setOnExpensesUpdatedListener(listener: (List<ExpenseRecord>) -> Unit)
}

class ExpenseRepository(context: Context) : IExpenseRepository {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("expenses_storage_prefs", Context.MODE_PRIVATE)

    private var onExpensesUpdated: ((List<ExpenseRecord>) -> Unit)? = null

    companion object {
        private const val KEY_EXPENSES = "key_expenses_records"
    }

    init {
        FirestoreSyncHelper.listenToData("expenses_records") { json ->
            if (!json.isNullOrEmpty()) {
                prefs.edit().putString(KEY_EXPENSES, json).apply()
                onExpensesUpdated?.invoke(parseExpensesJson(json))
            }
        }
    }

    override fun setOnExpensesUpdatedListener(listener: (List<ExpenseRecord>) -> Unit) {
        onExpensesUpdated = listener
    }

    override fun getExpenses(): List<ExpenseRecord> {
        val jsonString = prefs.getString(KEY_EXPENSES, null) ?: return emptyList()
        return parseExpensesJson(jsonString)
    }

    override fun addExpense(expense: ExpenseRecord) {
        val current = getExpenses().toMutableList()
        current.add(0, expense)
        saveExpensesList(current)
    }

    override fun removeExpense(expenseId: String) {
        val current = getExpenses().filter { it.id != expenseId }
        saveExpensesList(current)
    }

    override fun clearExpenses() {
        prefs.edit().remove(KEY_EXPENSES).apply()
        FirestoreSyncHelper.syncDataUp("expenses_records", "[]")
        onExpensesUpdated?.invoke(emptyList())
    }

    private fun saveExpensesList(expenses: List<ExpenseRecord>) {
        val jsonArray = JSONArray()
        expenses.forEach { expense ->
            jsonArray.put(expense.toJson())
        }
        val jsonString = jsonArray.toString()
        prefs.edit().putString(KEY_EXPENSES, jsonString).apply()
        FirestoreSyncHelper.syncDataUp("expenses_records", jsonString)
        onExpensesUpdated?.invoke(expenses)
    }

    private fun parseExpensesJson(jsonString: String): List<ExpenseRecord> {
        return try {
            val jsonArray = JSONArray(jsonString)
            val list = mutableListOf<ExpenseRecord>()
            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)
                list.add(obj.toExpenseRecord())
            }
            list
        } catch (e: Exception) {
            emptyList()
        }
    }

    private fun ExpenseRecord.toJson(): JSONObject {
        return JSONObject().apply {
            put("id", id)
            put("eventName", eventName)
            put("category", category)
            put("amount", amount)
            put("description", description)
            put("timestamp", timestamp)
        }
    }

    private fun JSONObject.toExpenseRecord(): ExpenseRecord {
        return ExpenseRecord(
            id = getString("id"),
            eventName = optString("eventName", "General"),
            category = optString("category", "Varios"),
            amount = getDouble("amount"),
            description = optString("description", ""),
            timestamp = getLong("timestamp")
        )
    }
}
