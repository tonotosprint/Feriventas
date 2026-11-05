package com.example.myapplication.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.myapplication.model.CalculationHistory
import com.example.myapplication.viewmodel.CalculatorViewModel
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale

private val calcNumberFormatSymbols = DecimalFormatSymbols(Locale.US).apply {
    groupingSeparator = '.'
    decimalSeparator = ','
}

private val calcNumberFormatter = DecimalFormat("#,##0.####", calcNumberFormatSymbols)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalculatorScreen(
    viewModel: CalculatorViewModel = viewModel()
) {
    val expression by viewModel.expression.collectAsState()
    val resultText by viewModel.resultText.collectAsState()
    val history by viewModel.history.collectAsState()
    val showHistoryDialog by viewModel.showHistoryDialog.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Calculadora",
                        fontWeight = FontWeight.Bold
                    )
                },
                actions = {
                    IconButton(onClick = { viewModel.openHistoryDialog() }) {
                        Icon(
                            imageVector = Icons.Default.DateRange,
                            contentDescription = "Historial de cálculos"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Pantalla de la calculadora
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    verticalArrangement = Arrangement.Bottom,
                    horizontalAlignment = Alignment.End
                ) {
                    Text(
                        text = expression,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.End,
                        maxLines = 2
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = resultText,
                        style = MaterialTheme.typography.displayMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        textAlign = TextAlign.End,
                        fontSize = 44.sp,
                        maxLines = 1
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Teclado de botones
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Fila 1: C, (, ), ÷
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    CalcButton("C", Modifier.weight(1f), isSpecial = true) { viewModel.onClearClick() }
                    CalcButton("(", Modifier.weight(1f), isOperator = true) { viewModel.onNumberClick("(") }
                    CalcButton(")", Modifier.weight(1f), isOperator = true) { viewModel.onNumberClick(")") }
                    CalcButton("÷", Modifier.weight(1f), isOperator = true) { viewModel.onOperatorClick("÷") }
                }

                // Fila 2: 7, 8, 9, ×
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    CalcButton("7", Modifier.weight(1f)) { viewModel.onNumberClick("7") }
                    CalcButton("8", Modifier.weight(1f)) { viewModel.onNumberClick("8") }
                    CalcButton("9", Modifier.weight(1f)) { viewModel.onNumberClick("9") }
                    CalcButton("×", Modifier.weight(1f), isOperator = true) { viewModel.onOperatorClick("×") }
                }

                // Fila 3: 4, 5, 6, −
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    CalcButton("4", Modifier.weight(1f)) { viewModel.onNumberClick("4") }
                    CalcButton("5", Modifier.weight(1f)) { viewModel.onNumberClick("5") }
                    CalcButton("6", Modifier.weight(1f)) { viewModel.onNumberClick("6") }
                    CalcButton("−", Modifier.weight(1f), isOperator = true) { viewModel.onOperatorClick("−") }
                }

                // Fila 4: 1, 2, 3, +
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    CalcButton("1", Modifier.weight(1f)) { viewModel.onNumberClick("1") }
                    CalcButton("2", Modifier.weight(1f)) { viewModel.onNumberClick("2") }
                    CalcButton("3", Modifier.weight(1f)) { viewModel.onNumberClick("3") }
                    CalcButton("+", Modifier.weight(1f), isOperator = true) { viewModel.onOperatorClick("+") }
                }

                // Fila 5: 0, ., ⌫, =
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    CalcButton("0", Modifier.weight(1f)) { viewModel.onNumberClick("0") }
                    CalcButton(".", Modifier.weight(1f)) { viewModel.onNumberClick(".") }
                    CalcButton("⌫", Modifier.weight(1f), isOperator = true) { viewModel.onBackspaceClick() }
                    CalcButton("=", Modifier.weight(1f), isEquals = true) { viewModel.onEqualsClick() }
                }
            }
        }
    }

    if (showHistoryDialog) {
        HistoryDialog(
            history = history,
            onSelect = { viewModel.selectHistoryItem(it) },
            onClear = { viewModel.clearHistory() },
            onDismiss = { viewModel.closeHistoryDialog() }
        )
    }
}

@Composable
fun HistoryDialog(
    history: List<CalculationHistory>,
    onSelect: (CalculationHistory) -> Unit,
    onClear: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = "Historial", fontWeight = FontWeight.Bold)
                if (history.isNotEmpty()) {
                    TextButton(onClick = onClear) {
                        Text("Borrar", color = MaterialTheme.colorScheme.error)
                    }
                }
            }
        },
        text = {
            if (history.isEmpty()) {
                Text("No hay cálculos guardados aún.", color = MaterialTheme.colorScheme.onSurfaceVariant)
            } else {
                LazyColumn(
                    modifier = Modifier.heightIn(max = 300.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(history, key = { it.id }) { item ->
                        Card(
                            onClick = { onSelect(item) },
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                            )
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text(
                                    text = item.expression,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = "= ${item.result}",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Cerrar")
            }
        }
    )
}

@Composable
fun CalcButton(
    text: String,
    modifier: Modifier = Modifier,
    isOperator: Boolean = false,
    isSpecial: Boolean = false,
    isEquals: Boolean = false,
    onClick: () -> Unit
) {
    val buttonModifier = modifier.aspectRatio(1.2f)

    if (isEquals) {
        Button(
            onClick = onClick,
            modifier = buttonModifier,
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary
            )
        ) {
            Text(text = text, fontSize = 24.sp, fontWeight = FontWeight.Bold)
        }
    } else if (isSpecial) {
        Button(
            onClick = onClick,
            modifier = buttonModifier,
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.errorContainer,
                contentColor = MaterialTheme.colorScheme.onErrorContainer
            )
        ) {
            Text(text = text, fontSize = 22.sp, fontWeight = FontWeight.Bold)
        }
    } else if (isOperator) {
        FilledTonalButton(
            onClick = onClick,
            modifier = buttonModifier,
            shape = RoundedCornerShape(16.dp)
        ) {
            Text(text = text, fontSize = 22.sp, fontWeight = FontWeight.Bold)
        }
    } else {
        OutlinedButton(
            onClick = onClick,
            modifier = buttonModifier,
            shape = RoundedCornerShape(16.dp)
        ) {
            Text(
                text = text,
                fontSize = 22.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

fun evaluateExpression(expr: String): String {
    if (expr.isEmpty()) return "0"
    return try {
        val sanitized = expr
            .replace('×', '*')
            .replace('÷', '/')
            .replace('−', '-')
            .let { s ->
                if (s.contains(',')) {
                    s.replace(".", "").replace(',', '.')
                } else {
                    s
                }
            }

        val result = evalSimpleMath(sanitized)
        if (result.isNaN() || result.isInfinite()) {
            "Error"
        } else {
            calcNumberFormatter.format(result)
        }
    } catch (e: Exception) {
        "Error"
    }
}

private fun evalSimpleMath(expression: String): Double {
    val tokens = mutableListOf<String>()
    var numberBuffer = StringBuilder()

    for (ch in expression) {
        if (ch.isDigit() || ch == '.') {
            numberBuffer.append(ch)
        } else if (ch in listOf('+', '-', '*', '/', '(', ')')) {
            if (numberBuffer.isNotEmpty()) {
                tokens.add(numberBuffer.toString())
                numberBuffer = StringBuilder()
            }
            if (ch == '-' && (tokens.isEmpty() || tokens.last() in listOf("+", "-", "*", "/", "("))) {
                numberBuffer.append(ch)
            } else {
                tokens.add(ch.toString())
            }
        }
    }
    if (numberBuffer.isNotEmpty()) {
        tokens.add(numberBuffer.toString())
    }

    if (tokens.isEmpty()) return 0.0

    return evalTokens(tokens)
}

private fun evalTokens(tokens: List<String>): Double {
    val output = mutableListOf<String>()
    val operatorStack = mutableListOf<String>()

    fun precedence(op: String): Int = when (op) {
        "+", "-" -> 1
        "*", "/" -> 2
        else -> 0
    }

    for (token in tokens) {
        when {
            token.toDoubleOrNull() != null -> output.add(token)
            token == "(" -> operatorStack.add(token)
            token == ")" -> {
                while (operatorStack.isNotEmpty() && operatorStack.last() != "(") {
                    output.add(operatorStack.removeAt(operatorStack.size - 1))
                }
                if (operatorStack.isNotEmpty() && operatorStack.last() == "(") {
                    operatorStack.removeAt(operatorStack.size - 1)
                }
            }
            token in listOf("+", "-", "*", "/") -> {
                while (operatorStack.isNotEmpty() && precedence(operatorStack.last()) >= precedence(token)) {
                    output.add(operatorStack.removeAt(operatorStack.size - 1))
                }
                operatorStack.add(token)
            }
        }
    }

    while (operatorStack.isNotEmpty()) {
        output.add(operatorStack.removeAt(operatorStack.size - 1))
    }

    val valueStack = mutableListOf<Double>()
    for (token in output) {
        val num = token.toDoubleOrNull()
        if (num != null) {
            valueStack.add(num)
        } else if (token in listOf("+", "-", "*", "/")) {
            if (valueStack.size < 2) return 0.0
            val b = valueStack.removeAt(valueStack.size - 1)
            val a = valueStack.removeAt(valueStack.size - 1)
            val res = when (token) {
                "+" -> a + b
                "-" -> a - b
                "*" -> a * b
                "/" -> if (b == 0.0) return Double.NaN else a / b
                else -> 0.0
            }
            valueStack.add(res)
        }
    }

    return valueStack.firstOrNull() ?: 0.0
}

@Preview(showBackground = true)
@Composable
fun CalculatorScreenPreview() {
    MaterialTheme {
        CalculatorScreen(viewModel = CalculatorViewModel())
    }
}
