package com.example.myapplication.repository

import android.content.Context
import android.content.SharedPreferences
import com.example.myapplication.model.CalculationHistory
import org.json.JSONArray
import org.json.JSONObject

interface ICalculatorRepository {
    fun getSavedExpression(): String
    fun getSavedResult(): String
    fun saveCalculatorState(expression: String, resultText: String)
    fun getHistory(): List<CalculationHistory>
    fun saveHistory(history: List<CalculationHistory>)
    fun clearHistory()
}

/**
 * Repositorio para la persistencia local del estado y del historial de la calculadora.
 */
class CalculatorRepository(context: Context) : ICalculatorRepository {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("calculator_storage_prefs", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_EXPRESSION = "key_calculator_expression"
        private const val KEY_RESULT = "key_calculator_result"
        private const val KEY_HISTORY = "key_calculator_history"
    }

    override fun getSavedExpression(): String {
        return prefs.getString(KEY_EXPRESSION, "") ?: ""
    }

    override fun getSavedResult(): String {
        return prefs.getString(KEY_RESULT, "0") ?: "0"
    }

    override fun saveCalculatorState(expression: String, resultText: String) {
        prefs.edit()
            .putString(KEY_EXPRESSION, expression)
            .putString(KEY_RESULT, resultText)
            .apply()
    }

    override fun getHistory(): List<CalculationHistory> {
        val jsonString = prefs.getString(KEY_HISTORY, null) ?: return emptyList()
        return try {
            val jsonArray = JSONArray(jsonString)
            val list = mutableListOf<CalculationHistory>()
            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)
                list.add(
                    CalculationHistory(
                        id = obj.optString("id"),
                        expression = obj.getString("expression"),
                        result = obj.getString("result"),
                        timestamp = obj.optLong("timestamp", System.currentTimeMillis())
                    )
                )
            }
            list
        } catch (e: Exception) {
            emptyList()
        }
    }

    override fun saveHistory(history: List<CalculationHistory>) {
        val jsonArray = JSONArray()
        history.forEach { item ->
            val obj = JSONObject().apply {
                put("id", item.id)
                put("expression", item.expression)
                put("result", item.result)
                put("timestamp", item.timestamp)
            }
            jsonArray.put(obj)
        }
        prefs.edit().putString(KEY_HISTORY, jsonArray.toString()).apply()
    }

    override fun clearHistory() {
        prefs.edit().remove(KEY_HISTORY).apply()
    }
}
