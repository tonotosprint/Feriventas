package com.example.myapplication

import com.example.myapplication.model.CalculationHistory
import com.example.myapplication.repository.ICalculatorRepository
import com.example.myapplication.viewmodel.CalculatorViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class FakeCalculatorRepository : ICalculatorRepository {
    private var expression = ""
    private var result = "0"
    private var history = mutableListOf<CalculationHistory>()

    override fun getSavedExpression(): String = expression
    override fun getSavedResult(): String = result

    override fun saveCalculatorState(expression: String, resultText: String) {
        this.expression = expression
        this.result = resultText
    }

    override fun getHistory(): List<CalculationHistory> = history

    override fun saveHistory(history: List<CalculationHistory>) {
        this.history = history.toMutableList()
    }

    override fun clearHistory() {
        this.history.clear()
    }
}

@OptIn(ExperimentalCoroutinesApi::class)
class CalculatorViewModelTest {

    private val testDispatcher = UnconfinedTestDispatcher()
    private lateinit var fakeRepository: FakeCalculatorRepository
    private lateinit var viewModel: CalculatorViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        fakeRepository = FakeCalculatorRepository()
        viewModel = CalculatorViewModel(fakeRepository)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun numberClick_updatesExpressionAndResult() {
        viewModel.onNumberClick("5")
        viewModel.onOperatorClick("+")
        viewModel.onNumberClick("3")

        assertEquals("5+3", viewModel.expression.value)
        assertEquals("8", viewModel.resultText.value)

        // Verificamos que se haya guardado en el repositorio
        assertEquals("5+3", fakeRepository.getSavedExpression())
        assertEquals("8", fakeRepository.getSavedResult())
    }

    @Test
    fun equalsClick_savesToHistory() {
        viewModel.onNumberClick("1")
        viewModel.onNumberClick("0")
        viewModel.onOperatorClick("+")
        viewModel.onNumberClick("5")
        viewModel.onEqualsClick()

        val history = viewModel.history.value
        assertEquals(1, history.size)
        assertEquals("10+5", history[0].expression)
        assertEquals("15", history[0].result)
    }

    @Test
    fun clearHistory_resetsHistoryList() {
        viewModel.onNumberClick("4")
        viewModel.onOperatorClick("×")
        viewModel.onNumberClick("2")
        viewModel.onEqualsClick()

        viewModel.clearHistory()
        assertTrue(viewModel.history.value.isEmpty())
    }

    @Test
    fun clearClick_resetsCalculator() {
        viewModel.onNumberClick("9")
        viewModel.onOperatorClick("×")
        viewModel.onNumberClick("9")
        viewModel.onClearClick()

        assertEquals("", viewModel.expression.value)
        assertEquals("0", viewModel.resultText.value)
        assertEquals("", fakeRepository.getSavedExpression())
    }

    @Test
    fun persistence_restoresStateFromRepository() {
        fakeRepository.saveCalculatorState("10+20", "30")

        val restoredViewModel = CalculatorViewModel(fakeRepository)
        assertEquals("10+20", restoredViewModel.expression.value)
        assertEquals("30", restoredViewModel.resultText.value)
    }
}
