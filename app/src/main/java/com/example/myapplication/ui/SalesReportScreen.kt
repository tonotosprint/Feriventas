package com.example.myapplication.ui

import android.app.DatePickerDialog
import android.content.Intent
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.myapplication.model.ExpenseRecord
import com.example.myapplication.model.PaymentMethod
import com.example.myapplication.model.SaleRecord
import com.example.myapplication.model.UserRole
import com.example.myapplication.util.PdfReportGenerator
import com.example.myapplication.viewmodel.ExpenseViewModel
import com.example.myapplication.viewmodel.ProductViewModel
import com.example.myapplication.viewmodel.SalesViewModel
import com.example.myapplication.viewmodel.TopProductStat
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SalesReportScreen(
    salesViewModel: SalesViewModel = viewModel(),
    productViewModel: ProductViewModel = viewModel(),
    expenseViewModel: ExpenseViewModel = viewModel(),
    userRole: UserRole = UserRole.ADMIN
) {
    val context = LocalContext.current
    val businessName by productViewModel.businessName.collectAsState()
    val selectedDateMillis by salesViewModel.selectedDateMillis.collectAsState()

    val sales by salesViewModel.selectedDateSales.collectAsState(initial = emptyList())
    val total by salesViewModel.selectedDateTotal.collectAsState(initial = 0.0)
    val cashTotal by salesViewModel.selectedDateCashTotal.collectAsState(initial = 0.0)
    val transferTotal by salesViewModel.selectedDateTransferTotal.collectAsState(initial = 0.0)
    val transactionCount by salesViewModel.selectedDateTransactionCount.collectAsState(initial = 0)
    val topProducts by salesViewModel.selectedDateTopProducts.collectAsState(initial = emptyList())

    val allExpenses by expenseViewModel.expenses.collectAsState()

    var showClearSalesDialog by remember { mutableStateOf(false) }
    var showExportPdfDialog by remember { mutableStateOf(false) }
    var showAddExpenseDialog by remember { mutableStateOf(false) }

    val dateFormat = remember { SimpleDateFormat("dd 'de' MMMM, yyyy", Locale("es", "ES")) }
    val formattedDateString = remember(selectedDateMillis) { dateFormat.format(Date(selectedDateMillis)) }

    val fileDateFormat = remember { SimpleDateFormat("dd-MM-yyyy", Locale.getDefault()) }
    val pdfFileName = remember(selectedDateMillis) { "reporte_ventas_${fileDateFormat.format(Date(selectedDateMillis))}.pdf" }

    // Gastos del día seleccionado
    val dayExpenses = remember(allExpenses, selectedDateMillis) {
        val targetCal = Calendar.getInstance().apply { timeInMillis = selectedDateMillis }
        allExpenses.filter { expense ->
            val expCal = Calendar.getInstance().apply { timeInMillis = expense.timestamp }
            expCal.get(Calendar.YEAR) == targetCal.get(Calendar.YEAR) &&
                    expCal.get(Calendar.DAY_OF_YEAR) == targetCal.get(Calendar.DAY_OF_YEAR)
        }
    }

    val totalExpenses = remember(dayExpenses) { dayExpenses.sumOf { it.amount } }
    val netProfit = total - totalExpenses

    val starProduct = remember(topProducts) { topProducts.firstOrNull() }

    // DatePicker Dialog
    val calendar = remember(selectedDateMillis) { Calendar.getInstance().apply { timeInMillis = selectedDateMillis } }
    val datePickerDialog = remember(context, selectedDateMillis) {
        DatePickerDialog(
            context,
            { _, year, month, dayOfMonth ->
                val newCal = Calendar.getInstance().apply {
                    set(Calendar.YEAR, year)
                    set(Calendar.MONTH, month)
                    set(Calendar.DAY_OF_MONTH, dayOfMonth)
                }
                salesViewModel.setSelectedDate(newCal.timeInMillis)
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        )
    }

    // Launcher nativo para guardar reporte PDF en almacenamiento del dispositivo
    val createPdfLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/pdf")
    ) { uri ->
        if (uri != null) {
            try {
                val pdfFile = PdfReportGenerator.generateSalesReportPdf(
                    context = context,
                    businessName = businessName,
                    dateMillis = selectedDateMillis,
                    sales = sales,
                    cashTotal = cashTotal,
                    transferTotal = transferTotal,
                    grandTotal = total
                )
                if (pdfFile != null && pdfFile.exists()) {
                    context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                        outputStream.write(pdfFile.readBytes())
                    }
                    Toast.makeText(context, "¡Reporte guardado con éxito!", Toast.LENGTH_SHORT).show()
                    showExportPdfDialog = false
                } else {
                    Toast.makeText(context, "Error al generar el PDF", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(context, "Error al guardar el archivo", Toast.LENGTH_SHORT).show()
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Reporte de Ventas e Informes",
                        fontWeight = FontWeight.Bold
                    )
                },
                actions = {
                    if (sales.isNotEmpty() && userRole == UserRole.ADMIN) {
                        IconButton(onClick = { showClearSalesDialog = true }) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "Reiniciar historial de ventas",
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Selector de Fecha / Calendario
            item {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.secondaryContainer,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Fecha seleccionada:",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                            Text(
                                text = formattedDateString,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                        }

                        OutlinedButton(
                            onClick = { datePickerDialog.show() },
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(imageVector = Icons.Default.DateRange, contentDescription = "Seleccionar fecha")
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Calendario")
                        }
                    }
                }
            }

            // Tarjeta de Resumen Bruto
            item {
                SummaryCard(
                    dateString = formattedDateString,
                    total = total,
                    cashTotal = cashTotal,
                    transferTotal = transferTotal,
                    transactionCount = transactionCount
                )
            }

            // Ganancia Neta Real de la Jornada
            item {
                NetProfitCard(
                    grossSales = total,
                    totalExpenses = totalExpenses,
                    netProfit = netProfit
                )
            }

            // Control de Ventas por Vendedor
            item {
                val salesByVendor = remember(sales) {
                    sales.groupBy { it.vendorName }.mapValues { (_, records) ->
                        Pair(records.size, records.sumOf { it.totalAmount })
                    }
                }
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "👥 Control de Ventas por Vendedor",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        if (salesByVendor.isEmpty()) {
                            Text(
                                text = "No hay ventas registradas en esta fecha.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        } else {
                            salesByVendor.forEach { (vendor, stats) ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(text = "• $vendor (${stats.first} ventas)", style = MaterialTheme.typography.bodyMedium)
                                    Text(text = formatMoney(stats.second), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }

            // Producto Estrella
            if (starProduct != null) {
                item {
                    StarProductCard(starProduct = starProduct)
                }
            }

            // Gastos Operativos de la Feria/Jornada
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "💸 Gastos Operativos (${dayExpenses.size})",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )

                    if (userRole == UserRole.ADMIN) {
                        Button(
                            onClick = { showAddExpenseDialog = true },
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(imageVector = Icons.Default.Add, contentDescription = null)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Registrar Gasto")
                        }
                    }
                }
            }

            if (dayExpenses.isEmpty()) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                        )
                    ) {
                        Box(
                            modifier = Modifier.padding(16.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "No se han registrado costos fijos o gastos operativos para este día (Stand, Transporte, Viáticos).",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            } else {
                items(dayExpenses, key = { it.id }) { expense ->
                    ExpenseCard(
                        expense = expense,
                        isAdmin = userRole == UserRole.ADMIN,
                        onDelete = { expenseViewModel.deleteExpense(expense.id) }
                    )
                }
            }

            // Botón Exportar a PDF
            if (sales.isNotEmpty()) {
                item {
                    Button(
                        onClick = { showExportPdfDialog = true },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(imageVector = Icons.Default.Share, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("📄 Exportar Reporte a PDF", fontWeight = FontWeight.Bold)
                    }
                }
            }

            // Top Productos
            if (topProducts.isNotEmpty()) {
                item {
                    Text(
                        text = "🏆 Más Vendidos en la Fecha",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            topProducts.take(5).forEachIndexed { index, stat ->
                                TopProductItem(rank = index + 1, stat = stat)
                            }
                        }
                    }
                }
            }

            // Historial de Ventas
            item {
                Text(
                    text = "📋 Transacciones (${sales.size})",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            if (sales.isEmpty()) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                        )
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(32.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "No hay ventas registradas en la fecha seleccionada.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            } else {
                items(sales, key = { it.id }) { sale ->
                    SaleRecordCard(
                        sale = sale,
                        isAdmin = userRole == UserRole.ADMIN,
                        onDelete = { salesViewModel.deleteSale(sale.id) }
                    )
                }
            }
        }
    }

    if (showAddExpenseDialog) {
        AddExpenseDialog(
            onDismiss = { showAddExpenseDialog = false },
            onConfirmAdd = { category, amount, description ->
                expenseViewModel.addExpense(
                    eventName = formattedDateString,
                    category = category,
                    amount = amount,
                    description = description
                )
                showAddExpenseDialog = false
            }
        )
    }

    if (showExportPdfDialog) {
        ExportReportDialog(
            onSaveFile = {
                createPdfLauncher.launch(pdfFileName)
            },
            onShareFile = {
                val pdfFile = PdfReportGenerator.generateSalesReportPdf(
                    context = context,
                    businessName = businessName,
                    dateMillis = selectedDateMillis,
                    sales = sales,
                    cashTotal = cashTotal,
                    transferTotal = transferTotal,
                    grandTotal = total
                )
                if (pdfFile != null && pdfFile.exists()) {
                    try {
                        val uri = FileProvider.getUriForFile(
                            context,
                            "${context.packageName}.fileprovider",
                            pdfFile
                        )
                        val shareIntent = Intent(Intent.ACTION_SEND).apply {
                            type = "application/pdf"
                            putExtra(Intent.EXTRA_STREAM, uri)
                            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                        }
                        context.startActivity(Intent.createChooser(shareIntent, "Compartir Reporte en PDF"))
                        showExportPdfDialog = false
                    } catch (e: Exception) {
                        Toast.makeText(context, "Error al compartir el archivo PDF.", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    Toast.makeText(context, "Error al generar el PDF.", Toast.LENGTH_SHORT).show()
                }
            },
            onDismiss = { showExportPdfDialog = false }
        )
    }

    if (showClearSalesDialog && userRole == UserRole.ADMIN) {
        AlertDialog(
            onDismissRequest = { showClearSalesDialog = false },
            title = { Text("¿Cierre diario / Borrar ventas?") },
            text = { Text("Se eliminará todo el historial de ventas registradas.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        salesViewModel.clearAllSales()
                        showClearSalesDialog = false
                    }
                ) {
                    Text("Borrar Historial", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearSalesDialog = false }) {
                    Text("Cancelar")
                }
            }
        )
    }
}

@Composable
fun NetProfitCard(
    grossSales: Double,
    totalExpenses: Double,
    netProfit: Double
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (netProfit >= 0)
                MaterialTheme.colorScheme.secondaryContainer
            else
                MaterialTheme.colorScheme.errorContainer
        )
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "📊 GANANCIA NETA REAL DE LA JORNADA",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.8f)
            )

            Text(
                text = formatMoney(netProfit),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = if (netProfit >= 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Recaudación Bruta: ${formatMoney(grossSales)}",
                    style = MaterialTheme.typography.bodySmall
                )
                Text(
                    text = "Gastos: -${formatMoney(totalExpenses)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}

@Composable
fun StarProductCard(starProduct: TopProductStat) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.tertiaryContainer,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "🌟 PRODUCTO ESTRELLA DEL DÍA",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onTertiaryContainer
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = starProduct.productName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onTertiaryContainer
                )
                Text(
                    text = "${starProduct.totalQuantity} unidades vendidas (${formatMoney(starProduct.totalRevenue)})",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.8f)
                )
            }
        }
    }
}

@Composable
fun ExpenseCard(
    expense: ExpenseRecord,
    isAdmin: Boolean,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = expense.category,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                if (expense.description.isNotBlank()) {
                    Text(
                        text = expense.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "-${formatMoney(expense.amount)}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.error
                )

                if (isAdmin) {
                    IconButton(onClick = onDelete) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Eliminar gasto",
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun AddExpenseDialog(
    onDismiss: () -> Unit,
    onConfirmAdd: (category: String, amount: Double, description: String) -> Unit
) {
    var category by remember { mutableStateOf("Pago de Stand") }
    var amountText by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val presetCategories = listOf("Pago de Stand", "Viáticos", "Transporte", "Insumos", "Varios")

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Registrar Gasto Operativo",
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Categoría de costo fijo:",
                    style = MaterialTheme.typography.labelMedium
                )

                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(presetCategories) { cat ->
                        FilterChip(
                            selected = category == cat,
                            onClick = { category = cat },
                            label = { Text(cat) }
                        )
                    }
                }

                OutlinedTextField(
                    value = amountText,
                    onValueChange = {
                        amountText = it
                        errorMessage = null
                    },
                    label = { Text("Monto del gasto ($)") },
                    placeholder = { Text("Ej: 5000") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )

                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Descripción (Opcional)") },
                    placeholder = { Text("Ej: Nafta, taxi, alquiler de mesa") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )

                if (errorMessage != null) {
                    Text(
                        text = errorMessage!!,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val p = parseUserMoneyInput(amountText)
                    if (p == null || p <= 0) {
                        errorMessage = "Por favor ingresa un monto válido."
                    } else {
                        onConfirmAdd(category, p, description)
                    }
                }
            ) {
                Text("Guardar Gasto", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar")
            }
        }
    )
}

@Composable
fun ExportReportDialog(
    onSaveFile: () -> Unit,
    onShareFile: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(text = "Exportar Reporte de Ventas", fontWeight = FontWeight.Bold)
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = "Selecciona una opción para guardar o compartir el reporte en formato PDF:",
                    style = MaterialTheme.typography.bodyMedium
                )

                Button(
                    onClick = onSaveFile,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(imageVector = Icons.Default.Share, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("💾 Guardar en el dispositivo")
                }

                OutlinedButton(
                    onClick = onShareFile,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(imageVector = Icons.Default.Share, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("📤 Compartir PDF (WhatsApp / Mail)")
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar")
            }
        }
    )
}

@Composable
fun SummaryCard(
    dateString: String,
    total: Double,
    cashTotal: Double,
    transferTotal: Double,
    transactionCount: Int
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Resumen del día",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
            )

            Text(
                text = formatMoney(total),
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "$transactionCount ${if (transactionCount == 1) "venta realizada" else "ventas realizadas"}",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }

            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.7f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceAround
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "💵 Efectivo",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = formatMoney(cashTotal),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }

                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "💳 Transferencia / MP",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = formatMoney(transferTotal),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.tertiary
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun TopProductItem(rank: Int, stat: TopProductStat) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                modifier = Modifier.size(28.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = "#$rank",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            Text(
                text = stat.productName,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold
            )
        }

        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(
                text = "${stat.totalQuantity} un.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Text(
                text = formatMoney(stat.totalRevenue),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}

@Composable
fun SaleRecordCard(
    sale: SaleRecord,
    isAdmin: Boolean,
    onDelete: () -> Unit
) {
    val dateFormat = remember { SimpleDateFormat("HH:mm - dd/MM", Locale.getDefault()) }
    val timeString = remember(sale.timestamp) { dateFormat.format(Date(sale.timestamp)) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = if (sale.paymentMethod == PaymentMethod.CASH)
                            MaterialTheme.colorScheme.primaryContainer
                        else
                            MaterialTheme.colorScheme.tertiaryContainer
                    ) {
                        Text(
                            text = sale.paymentMethod.label,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = if (sale.paymentMethod == PaymentMethod.CASH)
                                MaterialTheme.colorScheme.onPrimaryContainer
                            else
                                MaterialTheme.colorScheme.onTertiaryContainer,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }

                    Text(
                        text = timeString,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = formatMoney(sale.totalAmount),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )

                    if (isAdmin) {
                        IconButton(onClick = onDelete) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "Eliminar venta",
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }
            }

            if (sale.discountAmount > 0) {
                Text(
                    text = "Subtotal: ${formatMoney(sale.subtotalAmount)} | Descuento: -${formatMoney(sale.discountAmount)}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.SemiBold
                )
            }

            // Detalle de productos vendidos
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                sale.items.forEach { item ->
                    Text(
                        text = "• ${item.quantity}x ${item.productName} (${formatMoney(item.totalCost)})",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            if (sale.paymentMethod == PaymentMethod.CASH && sale.changeGiven > 0) {
                Text(
                    text = "Abonó con: ${formatMoney(sale.amountPaid)} | Vuelto entregado: ${formatMoney(sale.changeGiven)}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
