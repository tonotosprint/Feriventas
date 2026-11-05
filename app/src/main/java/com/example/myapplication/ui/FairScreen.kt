package com.example.myapplication.ui

import android.app.DatePickerDialog
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.myapplication.model.ExpenseRecord
import com.example.myapplication.model.FairItem
import com.example.myapplication.model.FairRecord
import com.example.myapplication.model.SaleRecord
import com.example.myapplication.model.UserRole
import com.example.myapplication.viewmodel.ExpenseViewModel
import com.example.myapplication.viewmodel.FairViewModel
import com.example.myapplication.viewmodel.SalesViewModel
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FairScreen(
    fairViewModel: FairViewModel = viewModel(),
    salesViewModel: SalesViewModel = viewModel(),
    expenseViewModel: ExpenseViewModel = viewModel(),
    userRole: UserRole = UserRole.ADMIN
) {
    val context = LocalContext.current
    val fairs by fairViewModel.fairs.collectAsState()
    val showCreateDialog by fairViewModel.showCreateDialog.collectAsState()
    val selectedFair by fairViewModel.selectedFairForDetails.collectAsState()

    val allSales by salesViewModel.sales.collectAsState()
    val allExpenses by expenseViewModel.expenses.collectAsState()

    val dateFormat = remember { SimpleDateFormat("dd/MM/yyyy", Locale("es", "ES")) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Ferias y Eventos",
                        fontWeight = FontWeight.Bold
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        },
        floatingActionButton = {
            if (userRole == UserRole.ADMIN) {
                FloatingActionButton(
                    onClick = { fairViewModel.openCreateDialog() },
                    containerColor = MaterialTheme.colorScheme.primary
                ) {
                    Icon(imageVector = Icons.Default.Add, contentDescription = "Crear Feria")
                }
            }
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "🎪 Planifica y Compara tus Ferias",
                            style = MessageStyle(),
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                        Text(
                            text = "Prepara el stock que llevarás, asocia ventas y gastos por fecha y compara qué feria te resulta más rentable.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    }
                }
            }

            if (fairs.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No hay ferias registradas.\n¡Crea tu próxima feria con el botón '+'!",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                // Sección de Comparativa de Ferias
                item {
                    Text(
                        text = "🏆 Comparativa de Rentabilidad",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                items(fairs) { fair ->
                    // Calcular métricas para esta feria según fecha o eventName
                    val targetCal = Calendar.getInstance().apply { timeInMillis = fair.dateMillis }
                    val fairSales = allSales.filter { sale ->
                        val saleCal = Calendar.getInstance().apply { timeInMillis = sale.timestamp }
                        saleCal.get(Calendar.YEAR) == targetCal.get(Calendar.YEAR) &&
                                saleCal.get(Calendar.DAY_OF_YEAR) == targetCal.get(Calendar.DAY_OF_YEAR)
                    }
                    val fairExpenses = allExpenses.filter { exp ->
                        val expCal = Calendar.getInstance().apply { timeInMillis = exp.timestamp }
                        exp.eventName.equals(fair.name, ignoreCase = true) ||
                                (expCal.get(Calendar.YEAR) == targetCal.get(Calendar.YEAR) &&
                                        expCal.get(Calendar.DAY_OF_YEAR) == targetCal.get(Calendar.DAY_OF_YEAR))
                    }

                    val totalSalesAmount = fairSales.sumOf { it.totalAmount }
                    val totalSalesCost = fairSales.sumOf { it.totalManufacturingCost }
                    val grossProfit = totalSalesAmount - totalSalesCost
                    val totalExpensesAmount = fairExpenses.sumOf { it.amount }
                    val netProfit = grossProfit - totalExpensesAmount
                    val productsSold = fairSales.sumOf { sale -> sale.items.sumOf { it.quantity } }

                    FairCard(
                        fair = fair,
                        dateString = dateFormat.format(Date(fair.dateMillis)),
                        totalSales = totalSalesAmount,
                        netProfit = netProfit,
                        productsSold = productsSold,
                        expenses = totalExpensesAmount,
                        onClick = { fairViewModel.openFairDetails(fair) },
                        onDelete = if (userRole == UserRole.ADMIN) { { fairViewModel.deleteFair(fair.id) } } else null
                    )
                }
            }
        }
    }

    if (showCreateDialog) {
        CreateFairDialog(
            onDismiss = { fairViewModel.closeCreateDialog() },
            onCreate = { name, dateMillis, location ->
                fairViewModel.addFair(name, dateMillis, location)
            }
        )
    }

    if (selectedFair != null) {
        FairDetailsDialog(
            fair = selectedFair!!,
            allSales = allSales,
            allExpenses = allExpenses,
            onDismiss = { fairViewModel.closeFairDetails() },
            onUpdateItems = { items ->
                fairViewModel.updateFairItems(selectedFair!!.id, items)
            }
        )
    }
}

@Composable
fun MessageStyle() = MaterialTheme.typography.titleMedium

@Composable
fun FairCard(
    fair: FairRecord,
    dateString: String,
    totalSales: Double,
    netProfit: Double,
    productsSold: Int,
    expenses: Double,
    onClick: () -> Unit,
    onDelete: (() -> Unit)?
) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
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
                Column {
                    Text(
                        text = fair.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    if (fair.location.isNotBlank()) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(imageVector = Icons.Default.Place, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(end = 4.dp))
                            Text(
                                text = fair.location,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                if (onDelete != null) {
                    IconButton(onClick = onDelete) {
                        Icon(imageVector = Icons.Default.Delete, contentDescription = "Eliminar", tint = MaterialTheme.colorScheme.error)
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "📅 $dateString",
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "Prod. vendidos: $productsSold",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surface,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceAround
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(text = "Ventas", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(text = formatMoney(totalSales), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(text = "Gastos", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(text = formatMoney(expenses), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.error)
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(text = "Ganancia Neta", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(text = formatMoney(netProfit), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = if (netProfit >= 0) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.error)
                    }
                }
            }
        }
    }
}

@Composable
fun CreateFairDialog(
    onDismiss: () -> Unit,
    onCreate: (name: String, dateMillis: Long, location: String) -> Unit
) {
    val context = LocalContext.current
    var name by remember { mutableStateOf("") }
    var location by remember { mutableStateOf("") }
    var dateMillis by remember { mutableStateOf(System.currentTimeMillis()) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val dateFormat = remember { SimpleDateFormat("dd/MM/yyyy", Locale("es", "ES")) }
    val calendar = remember { Calendar.getInstance().apply { timeInMillis = dateMillis } }

    val datePickerDialog = remember(context) {
        DatePickerDialog(
            context,
            { _, year, month, dayOfMonth ->
                val newCal = Calendar.getInstance().apply {
                    set(Calendar.YEAR, year)
                    set(Calendar.MONTH, month)
                    set(Calendar.DAY_OF_MONTH, dayOfMonth)
                }
                dateMillis = newCal.timeInMillis
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        )
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Crear Feria / Evento", fontWeight = FontWeight.Bold) },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it; errorMessage = null },
                    label = { Text("Nombre de la feria") },
                    placeholder = { Text("Ej: Feria de la Plaza") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = location,
                    onValueChange = { location = it },
                    label = { Text("Ubicación (Opcional)") },
                    placeholder = { Text("Ej: Plaza Central") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(text = "📅 ${dateFormat.format(Date(dateMillis))}", fontWeight = FontWeight.SemiBold)
                    OutlinedButton(onClick = { datePickerDialog.show() }) {
                        Text("Cambiar Fecha")
                    }
                }

                if (errorMessage != null) {
                    Text(text = errorMessage!!, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val trimmedName = name.trim()
                    if (trimmedName.isEmpty()) {
                        errorMessage = "Ingresa un nombre para la feria."
                    } else {
                        onCreate(trimmedName, dateMillis, location)
                    }
                }
            ) {
                Text("Crear", fontWeight = FontWeight.Bold)
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
fun FairDetailsDialog(
    fair: FairRecord,
    allSales: List<SaleRecord>,
    allExpenses: List<ExpenseRecord>,
    onDismiss: () -> Unit,
    onUpdateItems: (List<FairItem>) -> Unit
) {
    val dateFormat = remember { SimpleDateFormat("dd/MM/yyyy", Locale("es", "ES")) }
    var itemsList by remember { mutableStateOf(fair.itemsTaken) }
    var newProductName by remember { mutableStateOf("") }
    var newProductQtyText by remember { mutableStateOf("") }

    val targetCal = remember(fair.dateMillis) { Calendar.getInstance().apply { timeInMillis = fair.dateMillis } }
    val fairSales = remember(allSales, fair.dateMillis) {
        allSales.filter { sale ->
            val saleCal = Calendar.getInstance().apply { timeInMillis = sale.timestamp }
            saleCal.get(Calendar.YEAR) == targetCal.get(Calendar.YEAR) &&
                    saleCal.get(Calendar.DAY_OF_YEAR) == targetCal.get(Calendar.DAY_OF_YEAR)
        }
    }
    val fairExpenses = remember(allExpenses, fair.name, fair.dateMillis) {
        allExpenses.filter { exp ->
            exp.eventName.equals(fair.name, ignoreCase = true) ||
                    (Calendar.getInstance().apply { timeInMillis = exp.timestamp }.let {
                        it.get(Calendar.YEAR) == targetCal.get(Calendar.YEAR) &&
                                it.get(Calendar.DAY_OF_YEAR) == targetCal.get(Calendar.DAY_OF_YEAR)
                    })
        }
    }

    val totalSalesAmount = fairSales.sumOf { it.totalAmount }
    val totalSalesCost = fairSales.sumOf { it.totalManufacturingCost }
    val grossProfit = totalSalesAmount - totalSalesCost
    val totalExpensesAmount = fairExpenses.sumOf { it.amount }
    val netProfit = grossProfit - totalExpensesAmount
    val productsSold = fairSales.sumOf { sale -> sale.items.sumOf { it.quantity } }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column {
                Text(text = fair.name, fontWeight = FontWeight.Bold)
                if (fair.location.isNotBlank()) {
                    Text(text = "📍 ${fair.location}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Text(text = "📅 ${dateFormat.format(Date(fair.dateMillis))}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
            }
        },
        text = {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(400.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Resumen de la feria
                item {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(text = "Resumen de Resultados", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimaryContainer)
                            Text(text = "• Dinero recaudado: ${formatMoney(totalSalesAmount)}", style = MaterialTheme.typography.bodySmall)
                            Text(text = "• Productos vendidos: $productsSold unidades", style = MaterialTheme.typography.bodySmall)
                            Text(text = "• Gastos registrados: ${formatMoney(totalExpensesAmount)}", style = MaterialTheme.typography.bodySmall)
                            Text(text = "• Ganancia neta: ${formatMoney(netProfit)}", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                        }
                    }
                }

                // Preparar feria / Stock llevado
                item {
                    Text(text = "📦 Preparar Feria (Stock llevado)", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                }

                items(itemsList) { item ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(text = "• ${item.productName}", style = MaterialTheme.typography.bodyMedium)
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(text = "${item.quantityTaken} un.", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                            IconButton(onClick = {
                                itemsList = itemsList.filter { it != item }
                                onUpdateItems(itemsList)
                            }) {
                                Icon(imageVector = Icons.Default.Delete, contentDescription = "Eliminar", tint = MaterialTheme.colorScheme.error)
                            }
                        }
                    }
                }

                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = newProductName,
                            onValueChange = { newProductName = it },
                            label = { Text("Artículo") },
                            placeholder = { Text("Ej: Remeras") },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = newProductQtyText,
                            onValueChange = { newProductQtyText = it },
                            label = { Text("Cant.") },
                            placeholder = { Text("20") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.width(80.dp)
                        )
                        Button(
                            onClick = {
                                val qty = newProductQtyText.toIntOrNull() ?: 0
                                if (newProductName.isNotBlank() && qty > 0) {
                                    itemsList = itemsList + FairItem(newProductName.trim(), qty)
                                    onUpdateItems(itemsList)
                                    newProductName = ""
                                    newProductQtyText = ""
                                }
                            }
                        ) {
                            Text("Agregar")
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Cerrar", fontWeight = FontWeight.Bold)
            }
        }
    )
}
