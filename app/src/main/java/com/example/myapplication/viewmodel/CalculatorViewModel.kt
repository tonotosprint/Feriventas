package com.example.myapplication.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.myapplication.model.CalculationHistory
import com.example.myapplication.repository.CalculatorRepository
import com.example.myapplication.repository.ICalculatorRepository
import com.example.myapplication.ui.evaluateExpression
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class CalculatorViewModel private constructor(
    application: Application?,
    private val repository: ICalculatorRepository
) : AndroidViewModel(application ?: Application()) {

    constructor(application: Application) : this(
        application = application,
        repository = CalculatorRepository(application)
    )

    // Constructor sin argumentos para Previews de Compose
    constructor() : this(
        application = null,
        repository = object : ICalculatorRepository {
            private var expr = ""
            private var res = "0"
            private var hist = mutableListOf<CalculationHistory>()
            override fun getSavedExpression() = expr
            override fun getSavedResult() = res
            override fun saveCalculatorState(expression: String, resultText: String) {
                expr = expression
                res = resultText
            }
            override fun getHistory() = hist
            override fun saveHistory(history: List<CalculationHistory>) {
                hist = history.toMutableList()
            }
            override fun clearHistory() {
                hist.clear()
            }
        }
    )

    constructor(repository: ICalculatorRepository) : this(
        application = null,
        repository = repository
    )

    private val _expression = MutableStateFlow(repository.getSavedExpression())
    val expression: StateFlow<String> = _expression.asStateFlow()

    private val _resultText = MutableStateFlow(repository.getSavedResult())
    val resultText: StateFlow<String> = _resultText.asStateFlow()

    private val _history = MutableStateFlow(repository.getHistory())
    val history: StateFlow<List<CalculationHistory>> = _history.asStateFlow()

    private val _showHistoryDialog = MutableStateFlow(false)
    val showHistoryDialog: StateFlow<Boolean> = _showHistoryDialog.asStateFlow()

    private var isNewCalculation = false

    private fun saveState() {
        viewModelScope.launch {
            repository.saveCalculatorState(_expression.value, _resultText.value)
        }
    }

    fun onNumberClick(number: String) {
        if (isNewCalculation) {
            _expression.value = number
            isNewCalculation = false
        } else {
            _expression.value += number
        }
        _resultText.value = evaluateExpression(_expression.value)
        saveState()
    }

    fun onOperatorClick(op: String) {
        isNewCalculation = false
        val currentExpr = _expression.value
        val currentResult = _resultText.value

        if (currentExpr.isEmpty()) {
            if (currentResult != "0" && currentResult != "Error") {
                _expression.value = currentResult + op
            }
        } else {
            val lastChar = currentExpr.last()
            if (lastChar in listOf('+', '−', '×', '÷', '.')) {
                _expression.value = currentExpr.dropLast(1) + op
            } else {
                _expression.value += op
            }
        }
        saveState()
    }

    fun onClearClick() {
        _expression.value = ""
        _resultText.value = "0"
        isNewCalculation = false
        saveState()
    }

    fun onBackspaceClick() {
        val currentExpr = _expression.value
        if (currentExpr.isNotEmpty()) {
            val updatedExpr = currentExpr.dropLast(1)
            _expression.value = updatedExpr
            _resultText.value = if (updatedExpr.isEmpty()) "0" else evaluateExpression(updatedExpr)
            saveState()
        }
    }

    fun onEqualsClick() {
        val currentExpr = _expression.value
        if (currentExpr.isNotEmpty()) {
            val calculated = evaluateExpression(currentExpr)
            _resultText.value = calculated

            if (calculated != "Error") {
                val newHistoryItem = CalculationHistory(
                    expression = currentExpr,
                    result = calculated
                )
                val updatedHistory = (listOf(newHistoryItem) + _history.value).take(50)
                _history.value = updatedHistory
                repository.saveHistory(updatedHistory)
            }

            _expression.value = ""
            isNewCalculation = true
            saveState()
        }
    }

    fun openHistoryDialog() {
        _showHistoryDialog.value = true
    }

    fun closeHistoryDialog() {
        _showHistoryDialog.value = false
    }

    fun clearHistory() {
        _history.value = emptyList()
        repository.clearHistory()
    }

    fun selectHistoryItem(item: CalculationHistory) {
        _expression.value = item.result
        _resultText.value = item.result
        isNewCalculation = true
        saveState()
        closeHistoryDialog()
    }
}
