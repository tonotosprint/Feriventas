package com.example.myapplication

import com.example.myapplication.ui.evaluateExpression
import org.junit.Assert.assertEquals
import org.junit.Test

class CalculatorTest {

    @Test
    fun testBasicAddition() {
        val result = evaluateExpression("15+25")
        assertEquals("40", result)
    }

    @Test
    fun testDecimals() {
        val result = evaluateExpression("12.5+3.5")
        assertEquals("16", result)
    }

    @Test
    fun testOperatorPrecedence() {
        val result = evaluateExpression("2+3×4")
        assertEquals("14", result)
    }

    @Test
    fun testParentheses() {
        val result = evaluateExpression("(2+3)×4")
        assertEquals("20", result)
    }

    @Test
    fun testDivisionByZero() {
        val result = evaluateExpression("10÷0")
        assertEquals("Error", result)
    }

    @Test
    fun testComplexExpression() {
        val result = evaluateExpression("100−25÷5")
        assertEquals("95", result)
    }
}
