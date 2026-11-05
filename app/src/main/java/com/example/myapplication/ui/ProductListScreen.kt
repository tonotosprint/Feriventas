package com.example.myapplication.ui

import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedIconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import kotlinx.coroutines.launch
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.myapplication.R
import com.example.myapplication.model.PaymentMethod
import com.example.myapplication.model.Product
import com.example.myapplication.model.SaleItem
import com.example.myapplication.model.SaleRecord
import com.example.myapplication.model.UserRole
import com.example.myapplication.util.GitHubReleaseInfo
import com.example.myapplication.util.GitHubUpdateChecker
import com.example.myapplication.viewmodel.AuthViewModel
import com.example.myapplication.viewmodel.ProductViewModel
import com.example.myapplication.viewmodel.SalesViewModel
import java.io.File
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale

private val moneyFormatSymbols = DecimalFormatSymbols(Locale.US).apply {
    groupingSeparator = '.'
    decimalSeparator = ','
}

private val moneyFormatter = DecimalFormat("$ #,##0.00", moneyFormatSymbols)

fun formatMoney(amount: Double): String {
    return moneyFormatter.format(amount)
}

fun parseUserMoneyInput(text: String): Double? {
    val trimmed = text.trim().removePrefix("$").trim()
    if (trimmed.isEmpty()) return null

    val cleaned = if (trimmed.contains('.') && trimmed.contains(',')) {
        trimmed.replace(".", "").replace(',', '.')
    } else if (trimmed.contains(',')) {
        trimmed.replace(',', '.')
    } else if (trimmed.count { it == '.' } > 1) {
        trimmed.replace(".", "")
    } else {
        trimmed
    }
    return cleaned.toDoubleOrNull()
}

fun getExportFileName(businessName: String): String {
    val sanitized = businessName.trim()
        .replace(Regex("[^a-zA-Z0-9áéíóúÁÉÍÓÚñÑ_\\s-]"), "")
        .replace(Regex("\\s+"), "_")
        .ifEmpty { "emprendimiento" }
    return "productos_$sanitized.json"
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProductListScreen(
    viewModel: ProductViewModel = viewModel(),
    salesViewModel: SalesViewModel = viewModel(),
    authViewModel: AuthViewModel = viewModel(),
    userRole: UserRole = UserRole.ADMIN,
    onLogout: () -> Unit = {}
) {
    val products by viewModel.products.collectAsState()
    val savedCategories by viewModel.categories.collectAsState()
    val categoryOrder by viewModel.categoryOrder.collectAsState()
    val collapsedCategories by viewModel.collapsedCategories.collectAsState()
    val showReorderCategoriesDialog by viewModel.showReorderCategoriesDialog.collectAsState()
    val showBulkStockDialog by viewModel.showBulkStockDialog.collectAsState()
    val selectedProductForLabel by viewModel.selectedProductForLabel.collectAsState()

    val businessName by viewModel.businessName.collectAsState()
    val showBusinessNameDialog by viewModel.showBusinessNameDialog.collectAsState()
    val totalPrice by viewModel.totalPrice.collectAsState(initial = 0.0)
    val totalUnits by viewModel.totalUnits.collectAsState(initial = 0)
    val showAddDialog by viewModel.showAddDialog.collectAsState()
    val showQuickAddDialog by viewModel.showQuickAddDialog.collectAsState()
    val showEditDialog by viewModel.showEditDialog.collectAsState()
    val productToEdit by viewModel.productToEdit.collectAsState()
    val showClearDialog by viewModel.showClearConfirmDialog.collectAsState()
    val showExportDialog by viewModel.showExportDialog.collectAsState()
    val showCheckoutDialog by viewModel.showCheckoutDialog.collectAsState()
    val currentVendor by authViewModel.currentVendor.collectAsState()

    val context = LocalContext.current
    val exportFileName = remember(businessName) { getExportFileName(businessName) }

    // Estados para desplegar el buscador por Lupa 🔍 y por Letra A-Z
    var isSearchExpanded by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    var selectedLetter by remember { mutableStateOf<Char?>(null) }

    // Estado para confirmación de importación desde archivo
    var pendingImportJsonString by remember { mutableStateOf<String?>(null) }
    var pendingImportedProducts by remember { mutableStateOf<List<Product>?>(null) }
    var showImportConfirmDialog by remember { mutableStateOf(false) }
    var showCartDialog by remember { mutableStateOf(false) }

    var updateInfo by remember { mutableStateOf<GitHubReleaseInfo?>(null) }
    var isDownloadingUpdate by remember { mutableStateOf(false) }
    var downloadProgress by remember { mutableFloatStateOf(0f) }
    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        val availableUpdate = GitHubUpdateChecker.checkForUpdate(context)
        if (availableUpdate != null) {
            updateInfo = availableUpdate
        }
    }

    val lowStockCount = remember(products) {
        products.count { !it.isTemporary && it.stock in 1..3 }
    }

    // Filtrado en tiempo real por texto e índice alfabético
    val filteredProducts = remember(products, searchQuery, selectedLetter) {
        products.filter { p ->
            val matchesSearch = if (searchQuery.isBlank()) true else {
                p.name.contains(searchQuery, ignoreCase = true) ||
                        p.category.contains(searchQuery, ignoreCase = true)
            }
            val matchesLetter = if (selectedLetter == null) true else {
                p.name.trim().startsWith(selectedLetter!!, ignoreCase = true)
            }
            matchesSearch && matchesLetter
        }
    }

    // Launcher nativo para guardar/exportar archivo
    val createDocumentLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        if (uri != null) {
            try {
                val jsonString = viewModel.exportProductsJson()
                context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                    outputStream.write(jsonString.toByteArray())
                }
                Toast.makeText(context, "¡Catálogo guardado con éxito!", Toast.LENGTH_SHORT).show()
                viewModel.closeExportDialog()
            } catch (e: Exception) {
                Toast.makeText(context, "Error al guardar el archivo", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // Launcher nativo para abrir/importar archivo
    val getContentLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            try {
                val jsonString = context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }
                if (!jsonString.isNullOrEmpty()) {
                    val parsed = viewModel.parseProductsFromJson(jsonString)
                    if (parsed != null && parsed.isNotEmpty()) {
                        pendingImportJsonString = jsonString
                        pendingImportedProducts = parsed
                        showImportConfirmDialog = true
                    } else {
                        Toast.makeText(context, "El archivo seleccionado no contiene un catálogo válido.", Toast.LENGTH_LONG).show()
                    }
                }
            } catch (e: Exception) {
                Toast.makeText(context, "Error al leer el archivo seleccionado.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // Agrupamiento y ordenamiento de categorías (Por defecto alfabético A-Z, o personalizado si el usuario lo cambió)
    val groupedProducts = remember(filteredProducts, categoryOrder) {
        val map = filteredProducts
            .sortedWith(compareBy<Product> { it.position }.thenBy { it.name })
            .groupBy { it.category.trim().ifEmpty { "General" } }

        val currentCategories = map.keys
        val orderedCategories = if (categoryOrder.isNotEmpty()) {
            (categoryOrder + currentCategories.sorted()).distinct()
        } else {
            currentCategories.sorted()
        }

        orderedCategories.mapNotNull { categoryName ->
            val categoryProducts = map[categoryName]
            if (!categoryProducts.isNullOrEmpty()) {
                categoryName to categoryProducts
            } else null
        }
    }

    // Lista de letras presentes en el catálogo para el índice A-Z
    val alphabet = remember { ('A'..'Z').toList() }
    val presentLetters = remember(products) {
        products.mapNotNull { it.name.trim().firstOrNull()?.uppercaseChar() }.toSet()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(vertical = 4.dp)
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.feriventas),
                            contentDescription = "Logo Feriventas",
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = businessName.ifEmpty { "Feriventas" },
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.titleLarge
                        )
                    }
                },
                actions = {
                    // Botón Emoji Lupa 🔍 para desplegar/contraer el buscador
                    IconButton(onClick = { isSearchExpanded = !isSearchExpanded }) {
                        Text(
                            text = "🔍",
                            fontSize = 18.sp
                        )
                    }

                    // Botón para agregar un nuevo producto al catálogo (solo Admin)
                    if (userRole == UserRole.ADMIN) {
                        IconButton(onClick = { viewModel.openAddDialog() }) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = "Agregar producto al catálogo"
                            )
                        }
                    }

                    var showMenu by remember { mutableStateOf(false) }

                    IconButton(onClick = { showMenu = true }) {
                        Icon(
                            imageVector = Icons.Default.MoreVert,
                            contentDescription = "Opciones"
                        )
                    }

                    DropdownMenu(
                        expanded = showMenu,
                        onDismissRequest = { showMenu = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("👤 Perfil / Vendedor: $currentVendor (${userRole.displayName})") },
                            onClick = {
                                showMenu = false
                                authViewModel.openManageVendorsDialog()
                            },
                            leadingIcon = {
                                Icon(
                                    if (userRole == UserRole.ADMIN) Icons.Default.Lock else Icons.Default.Person,
                                    contentDescription = null
                                )
                            }
                        )

                        HorizontalDivider()
                        DropdownMenuItem(
                            text = { Text("🚪 Cerrar sesión (Cloud)") },
                            onClick = {
                                showMenu = false
                                onLogout()
                            },
                            leadingIcon = {
                                Icon(Icons.Default.Lock, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                            }
                        )

                        if (userRole == UserRole.ADMIN) {
                            HorizontalDivider()
                            DropdownMenuItem(
                                text = { Text("Nombre del emprendimiento") },
                                onClick = {
                                    showMenu = false
                                    viewModel.openBusinessNameDialog()
                                },
                                leadingIcon = {
                                    Icon(Icons.Default.Edit, contentDescription = null)
                                }
                            )

                            DropdownMenuItem(
                                text = { Text("Carga masiva de stock") },
                                onClick = {
                                    showMenu = false
                                    viewModel.openBulkStockDialog()
                                },
                                leadingIcon = {
                                    Icon(Icons.Default.AddCircle, contentDescription = null)
                                }
                            )

                            DropdownMenuItem(
                                text = { Text("Gestionar categorías") },
                                onClick = {
                                    showMenu = false
                                    viewModel.openReorderCategoriesDialog()
                                },
                                leadingIcon = {
                                    Icon(Icons.Default.List, contentDescription = null)
                                }
                            )
                        }

                        HorizontalDivider()
                        DropdownMenuItem(
                            text = { Text("🚀 Buscar actualizaciones") },
                            onClick = {
                                showMenu = false
                                coroutineScope.launch {
                                    val availableUpdate = GitHubUpdateChecker.checkForUpdate(context)
                                    if (availableUpdate != null) {
                                        updateInfo = availableUpdate
                                    } else {
                                        Toast.makeText(context, "Feriventas está actualizado a la última versión.", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            },
                            leadingIcon = {
                                Icon(Icons.Default.AddCircle, contentDescription = null)
                            }
                        )

                        DropdownMenuItem(
                            text = { Text("Exportar catálogo") },
                            onClick = {
                                showMenu = false
                                viewModel.openExportDialog()
                            },
                            leadingIcon = {
                                Icon(Icons.Default.Share, contentDescription = null)
                            }
                        )

                        if (userRole == UserRole.ADMIN) {
                            DropdownMenuItem(
                                text = { Text("Importar catálogo desde archivo") },
                                onClick = {
                                    showMenu = false
                                    getContentLauncher.launch("*/*")
                                },
                                leadingIcon = {
                                    Icon(Icons.Default.AddCircle, contentDescription = null)
                                }
                            )

                            if (products.isNotEmpty()) {
                                HorizontalDivider()
                                DropdownMenuItem(
                                    text = { Text("Vaciar catálogo", color = MaterialTheme.colorScheme.error) },
                                    onClick = {
                                        showMenu = false
                                        viewModel.openClearDialog()
                                    },
                                    leadingIcon = {
                                        Icon(
                                            imageVector = Icons.Default.Delete,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.error
                                        )
                                    }
                                )
                            }
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        },
        bottomBar = {
            TotalSummaryBottomBar(
                totalPrice = totalPrice,
                totalUnits = totalUnits,
                onQuickAdd = { viewModel.openQuickAddDialog() },
                onCheckout = { viewModel.openCheckoutDialog() },
                onCancelSale = { viewModel.cancelSale() },
                onOpenCart = { showCartDialog = true }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Buscador Desplegable por Lupa 🔍
            if (isSearchExpanded || searchQuery.isNotBlank()) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    label = { Text("Buscar producto...") },
                    placeholder = { Text("Ej: Stickers, Pin, Libreta...") },
                    singleLine = true,
                    leadingIcon = { Text("🔍", fontSize = 16.sp, modifier = Modifier.padding(start = 8.dp)) },
                    trailingIcon = {
                        IconButton(onClick = {
                            searchQuery = ""
                            isSearchExpanded = false
                        }) {
                            Icon(Icons.Default.Close, contentDescription = "Cerrar buscador")
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp),
                    shape = RoundedCornerShape(14.dp)
                )
            }

            // Chips de Filtros Activos (Letra / Texto)
            if (searchQuery.isNotBlank() || selectedLetter != null) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 2.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Filtro activo:",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    if (selectedLetter != null) {
                        FilterChip(
                            selected = true,
                            onClick = { selectedLetter = null },
                            label = { Text("Letra: $selectedLetter") },
                            trailingIcon = { Icon(Icons.Default.Close, contentDescription = null, modifier = Modifier.size(16.dp)) }
                        )
                    }

                    if (searchQuery.isNotBlank()) {
                        FilterChip(
                            selected = true,
                            onClick = { searchQuery = "" },
                            label = { Text("\"$searchQuery\"") },
                            trailingIcon = { Icon(Icons.Default.Close, contentDescription = null, modifier = Modifier.size(16.dp)) }
                        )
                    }
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                if (products.isEmpty()) {
                    EmptyStateView(
                        isAdmin = userRole == UserRole.ADMIN,
                        onAddClick = { viewModel.openAddDialog() },
                        onQuickAddClick = { viewModel.openQuickAddDialog() },
                        onImportClick = { getContentLauncher.launch("*/*") }
                    )
                } else if (filteredProducts.isEmpty()) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = "No se encontraron productos",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Intenta con otra búsqueda o selecciona otra letra.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            OutlinedButton(
                                onClick = {
                                    searchQuery = ""
                                    selectedLetter = null
                                    isSearchExpanded = false
                                },
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text("Limpiar Filtros")
                            }
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(start = 16.dp, end = 32.dp, top = 8.dp, bottom = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        if (lowStockCount > 0) {
                            item {
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = MaterialTheme.colorScheme.errorContainer,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier.padding(12.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "⚠️ Alerta: Hay $lowStockCount ${if (lowStockCount == 1) "producto con stock bajo" else "productos con stock bajo"} (≤ 3 un.).",
                                            style = MaterialTheme.typography.bodySmall,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onErrorContainer
                                        )
                                    }
                                }
                            }
                        }

                        groupedProducts.forEach { (categoryName, categoryProducts) ->
                            val isCollapsed = collapsedCategories.contains(categoryName)

                            // Encabezado del Cajón / Categoría (Clickable para contraer/expandir y guardado en ViewModel)
                            item(key = "header_$categoryName") {
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = MaterialTheme.colorScheme.secondaryContainer,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(top = 8.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .clickable {
                                            viewModel.toggleCategoryCollapse(categoryName)
                                        }
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 16.dp, vertical = 6.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(
                                                imageVector = if (isCollapsed) Icons.Default.KeyboardArrowDown else Icons.Default.KeyboardArrowUp,
                                                contentDescription = if (isCollapsed) "Expandir" else "Contraer",
                                                tint = MaterialTheme.colorScheme.onSecondaryContainer
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(
                                                text = "🗂️ $categoryName (${categoryProducts.size})",
                                                style = MaterialTheme.typography.titleMedium,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.onSecondaryContainer
                                            )
                                        }

                                        if (userRole == UserRole.ADMIN) {
                                            Row {
                                                IconButton(
                                                    onClick = { viewModel.moveCategoryUp(categoryName) },
                                                    modifier = Modifier.size(32.dp)
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Default.KeyboardArrowUp,
                                                        contentDescription = "Mover arriba",
                                                        tint = MaterialTheme.colorScheme.onSecondaryContainer
                                                    )
                                                }

                                                IconButton(
                                                    onClick = { viewModel.moveCategoryDown(categoryName) },
                                                    modifier = Modifier.size(32.dp)
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Default.KeyboardArrowDown,
                                                        contentDescription = "Mover abajo",
                                                        tint = MaterialTheme.colorScheme.onSecondaryContainer
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }

                            if (!isCollapsed) {
                                items(categoryProducts, key = { it.id }) { product ->
                                    val onInc = remember(product.id) { { viewModel.incrementQuantity(product.id) } }
                                    val onDec = remember(product.id) { { viewModel.decrementQuantity(product.id) } }
                                    val onEdit = remember(product.id) { { viewModel.openEditDialog(product) } }
                                    val onRemove = remember(product.id) { { viewModel.removeProduct(product.id) } }
                                    val onUp = remember(product.id) { { viewModel.moveProductUpInCategory(product) } }
                                    val onDown = remember(product.id) { { viewModel.moveProductDownInCategory(product) } }
                                    val onPrint = remember(product.id) { { viewModel.openPrintLabelDialog(product) } }

                                    ProductItemCard(
                                        product = product,
                                        userRole = userRole,
                                        onIncrement = onInc,
                                        onDecrement = onDec,
                                        onEdit = onEdit,
                                        onRemove = onRemove,
                                        onMoveUp = onUp,
                                        onMoveDown = onDown,
                                        onPrintLabel = onPrint
                                    )
                                }
                            }
                        }

                        item {
                            Spacer(modifier = Modifier.height(16.dp))
                        }
                    }
                }

                // Índice Alfabético Lateral (A-Z Sidebar Bar)
                if (products.isNotEmpty()) {
                    Column(
                        modifier = Modifier
                            .align(Alignment.CenterEnd)
                            .padding(end = 2.dp)
                            .background(
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.9f),
                                shape = RoundedCornerShape(topStart = 12.dp, bottomStart = 12.dp)
                            )
                            .padding(vertical = 4.dp, horizontal = 2.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(1.dp)
                    ) {
                        alphabet.forEach { letter ->
                            val isPresent = presentLetters.contains(letter)
                            val isSelected = selectedLetter == letter
                            Text(
                                text = letter.toString(),
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                fontSize = 10.sp,
                                color = when {
                                    isSelected -> MaterialTheme.colorScheme.primary
                                    isPresent -> MaterialTheme.colorScheme.onSurface
                                    else -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                                },
                                modifier = Modifier
                                    .clip(CircleShape)
                                    .clickable(enabled = isPresent) {
                                        selectedLetter = if (selectedLetter == letter) null else letter
                                    }
                                    .padding(horizontal = 4.dp, vertical = 1.dp)
                            )
                        }
                    }
                }
            }
        }
    }

    if (showBulkStockDialog && userRole == UserRole.ADMIN) {
        BulkStockDialog(
            products = products,
            onSaveBulkStock = { updates -> viewModel.updateBulkStock(updates) },
            onDismiss = { viewModel.closeBulkStockDialog() }
        )
    }

    if (selectedProductForLabel != null && userRole == UserRole.ADMIN) {
        PrintLabelDialog(
            businessName = businessName,
            product = selectedProductForLabel!!,
            allProducts = products,
            onDismiss = { viewModel.closePrintLabelDialog() }
        )
    }

    if (showQuickAddDialog) {
        QuickAddProductDialog(
            onDismiss = { viewModel.closeQuickAddDialog() },
            onAddQuickProduct = { name, price, quantity, category ->
                viewModel.addQuickSaleProduct(name, price, quantity, category)
            }
        )
    }

    if (showReorderCategoriesDialog && userRole == UserRole.ADMIN) {
        val currentCategories = remember(products, savedCategories, categoryOrder) {
            val productCategories = products.map { it.category.trim() }.filter { it.isNotEmpty() }.distinct()
            (categoryOrder + savedCategories + productCategories).distinct()
        }
        ReorderCategoriesDialog(
            categories = currentCategories,
            onMoveUp = { cat -> viewModel.moveCategoryUp(cat) },
            onMoveDown = { cat -> viewModel.moveCategoryDown(cat) },
            onDeleteCategory = { cat -> viewModel.deleteCategory(cat) },
            onDismiss = { viewModel.closeReorderCategoriesDialog() }
        )
    }

    if (showBusinessNameDialog && userRole == UserRole.ADMIN) {
        BusinessNameDialog(
            currentName = businessName,
            isFirstLaunch = businessName.isBlank(),
            onConfirm = { name ->
                viewModel.setBusinessName(name)
            },
            onDismiss = { viewModel.closeBusinessNameDialog() }
        )
    }

    if (showAddDialog && userRole == UserRole.ADMIN) {
        val existingCategories = remember(products, savedCategories) {
            (products.map { it.category.trim() } + savedCategories).filter { it.isNotEmpty() }.distinct()
        }
        AddProductDialog(
            existingCategories = existingCategories,
            onDismiss = { viewModel.closeAddDialog() },
            onAddProduct = { name, price, costPrice, category, stock, barcode ->
                viewModel.addProduct(name, price, category, stock, barcode, costPrice)
            }
        )
    }

    if (showEditDialog && productToEdit != null && userRole == UserRole.ADMIN) {
        val existingCategories = remember(products, savedCategories) {
            (products.map { it.category.trim() } + savedCategories).filter { it.isNotEmpty() }.distinct()
        }
        EditProductDialog(
            product = productToEdit!!,
            existingCategories = existingCategories,
            onDismiss = { viewModel.closeEditDialog() },
            onConfirmEdit = { id, name, price, costPrice, category, stock, barcode ->
                viewModel.updateProduct(id, name, price, category, stock, barcode, costPrice)
            }
        )
    }

    if (showExportDialog) {
        ExportProductsDialog(
            onSaveFile = {
                createDocumentLauncher.launch(exportFileName)
            },
            onShareFile = {
                val jsonString = viewModel.exportProductsJson()
                shareProductsFile(context, jsonString, exportFileName)
                viewModel.closeExportDialog()
            },
            onDismiss = { viewModel.closeExportDialog() }
        )
    }

    if (showImportConfirmDialog && pendingImportJsonString != null && pendingImportedProducts != null && userRole == UserRole.ADMIN) {
        ImportConfirmDialog(
            importedProductsCount = pendingImportedProducts!!.size,
            onConfirm = { replaceExisting ->
                val success = viewModel.importProductsFromJson(pendingImportJsonString!!, replaceExisting)
                if (success) {
                    Toast.makeText(context, "¡Catálogo importado correctamente!", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(context, "Error al importar catálogo.", Toast.LENGTH_SHORT).show()
                }
                showImportConfirmDialog = false
                pendingImportJsonString = null
                pendingImportedProducts = null
            },
            onDismiss = {
                showImportConfirmDialog = false
                pendingImportJsonString = null
                pendingImportedProducts = null
            }
        )
    }

    if (showCheckoutDialog) {
        val selectedProducts = remember(products) { products.filter { it.quantity > 0 } }
        val currentVendor by authViewModel.currentVendor.collectAsState()
        CheckoutDialog(
            subtotalPrice = totalPrice,
            onDismiss = { viewModel.closeCheckoutDialog() },
            onConfirmSale = { paymentMethod, amountPaid, changeGiven, discountAmount, finalTotal ->
                val saleItems = selectedProducts.map { p ->
                    SaleItem(
                        productId = p.id,
                        productName = p.name,
                        price = p.price,
                        costPrice = p.costPrice,
                        quantity = p.quantity,
                        category = p.category
                    )
                }
                val saleRecord = SaleRecord(
                    items = saleItems,
                    subtotalAmount = totalPrice,
                    discountAmount = discountAmount,
                    totalAmount = finalTotal,
                    paymentMethod = paymentMethod,
                    amountPaid = amountPaid,
                    changeGiven = changeGiven,
                    vendorName = currentVendor
                )
                salesViewModel.addSale(saleRecord)
                // Descontar del stock disponible automáticamente
                viewModel.deductStockForSale(saleItems)
                viewModel.resetQuantities()
                viewModel.closeCheckoutDialog()

                val totalCostSold = saleItems.sumOf { it.totalManufacturingCost }
                val netProfit = finalTotal - totalCostSold

                val toastMessage = buildString {
                    append("¡Venta registrada de ${formatMoney(finalTotal)} (Vendedor: $currentVendor)!\n")
                    append("Monto real de venta: ${formatMoney(finalTotal)}\n")
                    append("Ganancia neta: ${formatMoney(netProfit)}")
                    if (paymentMethod == PaymentMethod.CASH && changeGiven > 0) {
                        append("\nVuelto: ${formatMoney(changeGiven)}")
                    }
                }
                Toast.makeText(context, toastMessage, Toast.LENGTH_LONG).show()
            }
        )
    }

    val showManageVendorsDialog by authViewModel.showManageVendorsDialog.collectAsState()
    val vendors by authViewModel.vendors.collectAsState()
    val adminVendors by authViewModel.adminVendors.collectAsState()
    val showAdminPinPrompt by authViewModel.showAdminPinPrompt.collectAsState()
    val pendingAdminVendor by authViewModel.pendingAdminVendor.collectAsState()

    val showAdminResetNoticeDialog by authViewModel.showAdminResetNoticeDialog.collectAsState()
    val resetNoticeEmail by authViewModel.resetNoticeEmail.collectAsState()

    if (showManageVendorsDialog) {
        ManageVendorsDialog(
            vendors = vendors,
            currentVendor = currentVendor,
            adminVendors = adminVendors,
            isAdmin = (userRole == UserRole.ADMIN),
            onDismiss = { authViewModel.closeManageVendorsDialog() },
            onSelectVendor = { authViewModel.setCurrentVendor(it) },
            onAddVendor = { name, isAdmin -> authViewModel.addVendor(name, isAdmin) },
            onDeleteVendor = { authViewModel.deleteVendor(it) }
        )
    }

    if (showAdminPinPrompt && pendingAdminVendor != null) {
        AdminPinPromptDialog(
            targetVendor = pendingAdminVendor!!,
            onConfirm = { pin -> authViewModel.confirmAdminPinPrompt(pin) },
            onResetPin = { authViewModel.requestAdminPinResetEmail(context) },
            onDismiss = { authViewModel.cancelAdminPinPrompt() }
        )
    }

    if (showAdminResetNoticeDialog) {
        AdminResetNoticeDialog(
            email = resetNoticeEmail,
            onDismiss = { authViewModel.closeAdminResetNoticeDialog() }
        )
    }

    if (showCartDialog) {
        val cartItems = remember(products) { products.filter { it.quantity > 0 } }
        ViewCartDialog(
            cartItems = cartItems,
            totalPrice = totalPrice,
            onIncrement = { id -> viewModel.incrementQuantity(id) },
            onDecrement = { id -> viewModel.decrementQuantity(id) },
            onRemoveItem = { id -> viewModel.removeProductFromCart(id) },
            onClearCart = { viewModel.resetQuantities() },
            onCheckout = {
                showCartDialog = false
                viewModel.openCheckoutDialog()
            },
            onDismiss = { showCartDialog = false }
        )
    }

    if (updateInfo != null) {
        UpdateAvailableDialog(
            releaseInfo = updateInfo!!,
            isDownloading = isDownloadingUpdate,
            downloadProgress = downloadProgress,
            onConfirmUpdate = {
                isDownloadingUpdate = true
                coroutineScope.launch {
                    val success = GitHubUpdateChecker.downloadAndInstallApk(
                        context = context,
                        downloadUrl = updateInfo!!.downloadUrl,
                        onProgress = { p -> downloadProgress = p }
                    )
                    isDownloadingUpdate = false
                    if (!success) {
                        Toast.makeText(context, "Error al descargar la actualización de GitHub.", Toast.LENGTH_LONG).show()
                    }
                }
            },
            onDismiss = { updateInfo = null }
        )
    }

    if (showClearDialog && userRole == UserRole.ADMIN) {
        AlertDialog(
            onDismissRequest = { viewModel.closeClearDialog() },
            title = { Text(text = "¿Vaciar catálogo?") },
            text = { Text(text = "Se eliminarán todos los productos creados.") },
            confirmButton = {
                TextButton(
                    onClick = { viewModel.clearAllProducts() }
                ) {
                    Text(text = "Vaciar", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { viewModel.closeClearDialog() }
                ) {
                    Text(text = "Cancelar")
                }
            }
        )
    }
}

@Composable
fun QuickAddProductDialog(
    onDismiss: () -> Unit,
    onAddQuickProduct: (name: String, price: Double, quantity: Int, category: String) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var priceText by remember { mutableStateOf("") }
    var quantityText by remember { mutableStateOf("1") }
    var category by remember { mutableStateOf("Varios") }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "⚡ Artículo Rápido / Sin Catálogo",
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Agrega un producto directamente a esta venta sin guardarlo permanentemente:",
                    style = MaterialTheme.typography.bodyMedium
                )

                OutlinedTextField(
                    value = name,
                    onValueChange = {
                        name = it
                        errorMessage = null
                    },
                    label = { Text("Nombre / Descripción") },
                    placeholder = { Text("Ej: Producto Varios, Recargo, Servicio") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )

                OutlinedTextField(
                    value = priceText,
                    onValueChange = {
                        priceText = it
                        errorMessage = null
                    },
                    label = { Text("Precio por unidad ($)") },
                    placeholder = { Text("Ej: 500") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )

                OutlinedTextField(
                    value = quantityText,
                    onValueChange = {
                        quantityText = it
                        errorMessage = null
                    },
                    label = { Text("Cantidad") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
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
                    val p = parseUserMoneyInput(priceText)
                    val q = quantityText.trim().toIntOrNull() ?: 1
                    when {
                        p == null || p < 0 -> {
                            errorMessage = "Por favor ingresa un precio válido."
                        }
                        q <= 0 -> {
                            errorMessage = "La cantidad debe ser al menos 1."
                        }
                        else -> {
                            onAddQuickProduct(name, p, q, category)
                        }
                    }
                }
            ) {
                Text("Agregar a la Venta", fontWeight = FontWeight.Bold)
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
fun ReorderCategoriesDialog(
    categories: List<String>,
    onMoveUp: (String) -> Unit,
    onMoveDown: (String) -> Unit,
    onDeleteCategory: (String) -> Unit = {},
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Gestionar Categorías",
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "Reordena las categorías o elimina las que ya no utilices:",
                    style = MaterialTheme.typography.bodyMedium
                )
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(260.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    items(categories) { categoryName ->
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 6.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = categoryName,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.weight(1f)
                                )

                                Row {
                                    IconButton(
                                        onClick = { onMoveUp(categoryName) },
                                        modifier = Modifier.size(36.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.KeyboardArrowUp,
                                            contentDescription = "Mover arriba"
                                        )
                                    }

                                    IconButton(
                                        onClick = { onMoveDown(categoryName) },
                                        modifier = Modifier.size(36.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.KeyboardArrowDown,
                                            contentDescription = "Mover abajo"
                                        )
                                    }

                                    if (categoryName != "General") {
                                        IconButton(
                                            onClick = { onDeleteCategory(categoryName) },
                                            modifier = Modifier.size(36.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Delete,
                                                contentDescription = "Borrar categoría",
                                                tint = MaterialTheme.colorScheme.error
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = onDismiss) {
                Text("Listo", fontWeight = FontWeight.Bold)
            }
        }
    )
}

fun shareProductsFile(context: Context, jsonString: String, fileName: String = "productos_catalogo.json") {
    try {
        val cacheDir = File(context.cacheDir, "exports").apply { mkdirs() }
        val file = File(cacheDir, fileName).apply {
            writeText(jsonString)
        }
        val fileUri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "application/json"
            putExtra(Intent.EXTRA_STREAM, fileUri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(shareIntent, "Compartir Catálogo de Productos"))
    } catch (e: Exception) {
        Toast.makeText(context, "Error al compartir el archivo", Toast.LENGTH_SHORT).show()
    }
}

@Composable
fun BusinessNameDialog(
    currentName: String,
    isFirstLaunch: Boolean,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var nameInput by remember { mutableStateOf(currentName) }

    AlertDialog(
        onDismissRequest = {
            if (!isFirstLaunch) {
                onDismiss()
            }
        },
        title = {
            Text(
                text = if (isFirstLaunch) "¡Bienvenido a Feriventas!" else "Nombre del Emprendimiento",
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = if (isFirstLaunch)
                        "Por favor, ingresa el nombre de tu emprendimiento para personalizar la aplicación:"
                    else
                        "Actualiza el nombre de tu emprendimiento:",
                    style = MaterialTheme.typography.bodyMedium
                )
                OutlinedTextField(
                    value = nameInput,
                    onValueChange = { nameInput = it },
                    label = { Text("Nombre del emprendimiento") },
                    placeholder = { Text("Ej: Mi Emprendimiento") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (nameInput.isNotBlank()) {
                        onConfirm(nameInput.trim())
                    }
                },
                enabled = nameInput.isNotBlank()
            ) {
                Text("Guardar")
            }
        },
        dismissButton = {
            if (!isFirstLaunch) {
                TextButton(onClick = onDismiss) {
                    Text("Cancelar")
                }
            }
        }
    )
}

@Composable
fun EmptyStateView(
    isAdmin: Boolean,
    onAddClick: () -> Unit,
    onQuickAddClick: () -> Unit,
    onImportClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Surface(
            shape = CircleShape,
            color = MaterialTheme.colorScheme.primaryContainer,
            modifier = Modifier.size(96.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = Icons.Default.ShoppingCart,
                    contentDescription = null,
                    modifier = Modifier.size(48.dp),
                    tint = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        }
        Spacer(modifier = Modifier.height(24.dp))
        Text(
            text = "No hay productos en la lista",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Agrega tu primer producto o agrega un artículo rápido para cobrar inmediatamente.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(20.dp))
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Button(
                onClick = onQuickAddClick,
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(imageVector = Icons.Default.AddCircle, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("⚡ Ítem Rápido / Varios", fontWeight = FontWeight.Bold)
            }

            if (isAdmin) {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedButton(
                        onClick = onAddClick,
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(imageVector = Icons.Default.Add, contentDescription = null)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Crear Producto")
                    }

                    OutlinedButton(
                        onClick = onImportClick,
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(imageVector = Icons.Default.AddCircle, contentDescription = null)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Importar")
                    }
                }
            }
        }
    }
}

@Composable
fun ProductItemCard(
    product: Product,
    userRole: UserRole,
    onIncrement: () -> Unit,
    onDecrement: () -> Unit,
    onEdit: () -> Unit,
    onRemove: () -> Unit,
    onMoveUp: () -> Unit = {},
    onMoveDown: () -> Unit = {},
    onPrintLabel: () -> Unit = {}
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = product.name,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        if (product.isTemporary) {
                            Spacer(modifier = Modifier.width(6.dp))
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = MaterialTheme.colorScheme.tertiaryContainer
                            ) {
                                Text(
                                    text = "Rápido",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onTertiaryContainer,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Column(
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        Text(
                            text = "Precio: ${formatMoney(product.price)}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        if (!product.isTemporary) {
                            if (product.stock in 1..3) {
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = MaterialTheme.colorScheme.errorContainer
                                ) {
                                    Text(
                                        text = "⚠️ Stock Bajo (${product.stock} un.)",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onErrorContainer,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            } else {
                                Text(
                                    text = "Stock: ${if (product.stock > 0) "${product.stock} un." else "Sin límite"}",
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.SemiBold,
                                    color = if (product.stock > 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary
                                )
                            }
                        }
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                    if (userRole == UserRole.ADMIN && !product.isTemporary) {
                        IconButton(
                            onClick = onMoveUp,
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.KeyboardArrowUp,
                                contentDescription = "Subir producto"
                            )
                        }

                        IconButton(
                            onClick = onMoveDown,
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.KeyboardArrowDown,
                                contentDescription = "Bajar producto"
                            )
                        }

                        IconButton(
                            onClick = onPrintLabel,
                            modifier = Modifier.size(32.dp)
                        ) {
                            Text(
                                text = "🖨️",
                                fontSize = 16.sp
                            )
                        }

                        IconButton(
                            onClick = onEdit,
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Edit,
                                contentDescription = "Editar producto",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }

                    if (userRole == UserRole.ADMIN || product.isTemporary) {
                        IconButton(
                            onClick = onRemove,
                            colors = IconButtonDefaults.iconButtonColors(
                                contentColor = MaterialTheme.colorScheme.error
                            ),
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "Eliminar producto"
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Subtotal
                Text(
                    text = "Subtotal: ${formatMoney(product.totalCost)}",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary
                )

                // Contador de cantidad (+ / -)
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedIconButton(
                        onClick = onDecrement,
                        modifier = Modifier.size(36.dp),
                        enabled = product.quantity > 0
                    ) {
                        Text(
                            text = "−",
                            fontWeight = FontWeight.Bold,
                            fontSize = 20.sp
                        )
                    }

                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.primaryContainer,
                        modifier = Modifier.padding(horizontal = 4.dp)
                    ) {
                        Text(
                            text = "${product.quantity}",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                        )
                    }

                    val canAdd = product.stock == 0 || product.quantity < product.stock || product.isTemporary

                    FilledIconButton(
                        onClick = onIncrement,
                        modifier = Modifier.size(36.dp),
                        enabled = canAdd
                    ) {
                        Text(
                            text = "+",
                            fontWeight = FontWeight.Bold,
                            fontSize = 20.sp
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ViewCartDialog(
    cartItems: List<Product>,
    totalPrice: Double,
    onIncrement: (String) -> Unit,
    onDecrement: (String) -> Unit,
    onRemoveItem: (String) -> Unit,
    onClearCart: () -> Unit,
    onCheckout: () -> Unit,
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
                Text(
                    text = "🛒 Carrito de Compras",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )
                if (cartItems.isNotEmpty()) {
                    TextButton(
                        onClick = onClearCart,
                        contentPadding = PaddingValues(horizontal = 8.dp)
                    ) {
                        Text(
                            text = "Vaciar",
                            color = MaterialTheme.colorScheme.error,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1
                        )
                    }
                }
            }
        },
        text = {
            if (cartItems.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "El carrito de compras está vacío.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 320.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(cartItems, key = { it.id }) { product ->
                        Card(
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant
                            ),
                            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = product.name,
                                        fontWeight = FontWeight.Bold,
                                        style = MaterialTheme.typography.bodyMedium,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = "Precio: ${formatMoney(product.price * product.quantity)}",
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }

                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                                ) {
                                    OutlinedIconButton(
                                        onClick = { onDecrement(product.id) },
                                        modifier = Modifier.size(30.dp)
                                    ) {
                                        Text("−", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                    }

                                    Surface(
                                        shape = RoundedCornerShape(6.dp),
                                        color = MaterialTheme.colorScheme.primaryContainer,
                                        modifier = Modifier.padding(horizontal = 2.dp)
                                    ) {
                                        Text(
                                            text = "${product.quantity}",
                                            fontWeight = FontWeight.Bold,
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                        )
                                    }

                                    val canAdd = product.stock == 0 || product.quantity < product.stock || product.isTemporary
                                    FilledIconButton(
                                        onClick = { onIncrement(product.id) },
                                        modifier = Modifier.size(30.dp),
                                        enabled = canAdd
                                    ) {
                                        Text("+", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                    }

                                    IconButton(
                                        onClick = { onRemoveItem(product.id) },
                                        modifier = Modifier.size(30.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Delete,
                                            contentDescription = "Quitar del carrito",
                                            tint = MaterialTheme.colorScheme.error,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            if (cartItems.isNotEmpty()) {
                Button(
                    onClick = {
                        onDismiss()
                        onCheckout()
                    },
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Cobrar ${formatMoney(totalPrice)}",
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Cerrar")
            }
        }
    )
}

@Composable
fun AdminResetNoticeDialog(
    email: String,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Lock, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Seguridad de Administrador", fontWeight = FontWeight.Bold)
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "Por razones de seguridad, se ha enviado un correo con las instrucciones de recuperación a la cuenta del propietario:",
                    style = MaterialTheme.typography.bodyMedium
                )
                Text(
                    text = "✉️ $email",
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    style = MaterialTheme.typography.bodyLarge
                )
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.6f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "⚠️ IMPORTANTE: Si no encuentras el correo en tu bandeja de entrada en unos instantes, por favor REVISA TU CARPETA DE SPAM / CORREO NO DESEADO.",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        modifier = Modifier.padding(10.dp)
                    )
                }
            }
        },
        confirmButton = {
            Button(onClick = onDismiss) {
                Text("Entendido", fontWeight = FontWeight.Bold)
            }
        }
    )
}

@Composable
fun TotalSummaryBottomBar(
    totalPrice: Double,
    totalUnits: Int,
    onQuickAdd: () -> Unit,
    onCheckout: () -> Unit,
    onCancelSale: () -> Unit = {},
    onOpenCart: () -> Unit = {}
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surfaceColorAtElevation(8.dp),
        shadowElevation = 8.dp,
        shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .clickable { onOpenCart() }
                    .padding(vertical = 2.dp, horizontal = 4.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "Total Acumulado",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(text = "🛒", fontSize = 12.sp)
                }
                Text(
                    text = formatMoney(totalPrice),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "$totalUnits ${if (totalUnits == 1) "unidad" else "unidades"}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (totalUnits > 0) {
                    IconButton(
                        onClick = onOpenCart,
                        modifier = Modifier.size(38.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.ShoppingCart,
                            contentDescription = "Ver Carrito",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }

                    OutlinedIconButton(
                        onClick = onCancelSale,
                        modifier = Modifier.size(38.dp),
                        colors = IconButtonDefaults.outlinedIconButtonColors(
                            contentColor = MaterialTheme.colorScheme.error
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Cancelar Venta / Limpiar Carrito",
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                }

                OutlinedButton(
                    onClick = onQuickAdd,
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 8.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("⚡ Rápido", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }

                Button(
                    onClick = onCheckout,
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                    shape = RoundedCornerShape(12.dp),
                    enabled = totalUnits > 0
                ) {
                    Icon(imageVector = Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Cobrar", fontWeight = FontWeight.Bold, fontSize = 14.sp, maxLines = 1, softWrap = false)
                }
            }
        }
    }
}

@Composable
fun AddProductDialog(
    existingCategories: List<String>,
    onDismiss: () -> Unit,
    onAddProduct: (name: String, price: Double, costPrice: Double, category: String, stock: Int, barcode: String) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var priceText by remember { mutableStateOf("") }
    var costPriceText by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("General") }
    var stockText by remember { mutableStateOf("0") }
    var barcodeText by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val defaultChips = remember(existingCategories) {
        (listOf("General") + existingCategories).distinct().filter { it.isNotBlank() }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Agregar Producto al Catálogo",
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = {
                        name = it
                        errorMessage = null
                    },
                    label = { Text("Nombre del producto") },
                    placeholder = { Text("Ej: Sticker Holo, Pin55 x3") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = priceText,
                    onValueChange = {
                        priceText = it
                        errorMessage = null
                    },
                    label = { Text("Precio de venta por unidad ($)") },
                    placeholder = { Text("Ej: 400,00 o 1.000") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = costPriceText,
                    onValueChange = {
                        costPriceText = it
                        errorMessage = null
                    },
                    label = { Text("Precio de fabricación o compra ($)") },
                    placeholder = { Text("Ej: 200,00 (Opcional)") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = stockText,
                    onValueChange = {
                        stockText = it
                        errorMessage = null
                    },
                    label = { Text("Stock disponible (Opcional, 0 = ilimitado)") },
                    placeholder = { Text("Ej: 50") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = category,
                    onValueChange = {
                        category = it
                        errorMessage = null
                    },
                    label = { Text("Cajón / Categoría") },
                    placeholder = { Text("Ej: Stickers, Pins, Remeras") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                // Chips de categorías guardadas
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    items(defaultChips) { chipName ->
                        SuggestionChip(
                            onClick = { category = chipName },
                            label = { Text(chipName) }
                        )
                    }
                }

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
            TextButton(
                onClick = {
                    val trimmedName = name.trim()
                    val parsedPrice = parseUserMoneyInput(priceText)
                    val parsedCostPrice = parseUserMoneyInput(costPriceText) ?: 0.0
                    val parsedStock = stockText.trim().toIntOrNull() ?: 0
                    val trimmedCategory = category.trim().ifEmpty { "General" }

                    when {
                        trimmedName.isEmpty() -> {
                            errorMessage = "Por favor ingresa un nombre."
                        }
                        parsedPrice == null || parsedPrice < 0 -> {
                            errorMessage = "Por favor ingresa un precio de venta válido."
                        }
                        parsedCostPrice < 0 -> {
                            errorMessage = "Por favor ingresa un precio de costo válido."
                        }
                        else -> {
                            onAddProduct(trimmedName, parsedPrice, parsedCostPrice, trimmedCategory, parsedStock, barcodeText)
                        }
                    }
                }
            ) {
                Text("Agregar", fontWeight = FontWeight.Bold)
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
fun EditProductDialog(
    product: Product,
    existingCategories: List<String>,
    onDismiss: () -> Unit,
    onConfirmEdit: (id: String, name: String, price: Double, costPrice: Double, category: String, stock: Int, barcode: String) -> Unit
) {
    var name by remember { mutableStateOf(product.name) }
    var priceText by remember { mutableStateOf(product.price.toString()) }
    var costPriceText by remember { mutableStateOf(product.costPrice.toString()) }
    var category by remember { mutableStateOf(product.category) }
    var stockText by remember { mutableStateOf(product.stock.toString()) }
    var barcodeText by remember { mutableStateOf(product.barcode) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val defaultChips = remember(existingCategories) {
        (listOf("General") + existingCategories).distinct().filter { it.isNotBlank() }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Editar Producto",
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = {
                        name = it
                        errorMessage = null
                    },
                    label = { Text("Nombre del producto") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = priceText,
                    onValueChange = {
                        priceText = it
                        errorMessage = null
                    },
                    label = { Text("Precio de venta por unidad ($)") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = costPriceText,
                    onValueChange = {
                        costPriceText = it
                        errorMessage = null
                    },
                    label = { Text("Precio de fabricación o compra ($)") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = stockText,
                    onValueChange = {
                        stockText = it
                        errorMessage = null
                    },
                    label = { Text("Stock disponible (0 = ilimitado)") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = category,
                    onValueChange = {
                        category = it
                        errorMessage = null
                    },
                    label = { Text("Cajón / Categoría") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    items(defaultChips) { chipName ->
                        SuggestionChip(
                            onClick = { category = chipName },
                            label = { Text(chipName) }
                        )
                    }
                }

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
            TextButton(
                onClick = {
                    val trimmedName = name.trim()
                    val parsedPrice = parseUserMoneyInput(priceText) ?: priceText.toDoubleOrNull()
                    val parsedCostPrice = parseUserMoneyInput(costPriceText) ?: costPriceText.toDoubleOrNull() ?: 0.0
                    val parsedStock = stockText.trim().toIntOrNull() ?: 0
                    val trimmedCategory = category.trim().ifEmpty { "General" }

                    when {
                        trimmedName.isEmpty() -> {
                            errorMessage = "Por favor ingresa un nombre."
                        }
                        parsedPrice == null || parsedPrice < 0 -> {
                            errorMessage = "Por favor ingresa un precio de venta válido."
                        }
                        parsedCostPrice < 0 -> {
                            errorMessage = "Por favor ingresa un precio de costo válido."
                        }
                        else -> {
                            onConfirmEdit(product.id, trimmedName, parsedPrice, parsedCostPrice, trimmedCategory, parsedStock, barcodeText)
                        }
                    }
                }
            ) {
                Text("Guardar Cambios", fontWeight = FontWeight.Bold)
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
fun ExportProductsDialog(
    onSaveFile: () -> Unit,
    onShareFile: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(text = "Exportar Catálogo", fontWeight = FontWeight.Bold)
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = "Selecciona una opción para guardar o compartir tu catálogo:",
                    style = MaterialTheme.typography.bodyMedium
                )

                Button(
                    onClick = onSaveFile,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(imageVector = Icons.Default.Share, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("💾 Guardar como archivo")
                }

                OutlinedButton(
                    onClick = onShareFile,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(imageVector = Icons.Default.Share, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("📤 Compartir archivo (WhatsApp / Mail)")
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
fun ImportConfirmDialog(
    importedProductsCount: Int,
    onConfirm: (replaceExisting: Boolean) -> Unit,
    onDismiss: () -> Unit
) {
    var replaceExisting by remember { mutableStateOf(true) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(text = "Importar Catálogo", fontWeight = FontWeight.Bold)
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.primaryContainer,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Se encontraron $importedProductsCount productos en el archivo.",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.padding(16.dp)
                    )
                }

                Text(
                    text = "¿Cómo deseas importar estos productos?",
                    style = MaterialTheme.typography.bodyMedium
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Checkbox(
                        checked = replaceExisting,
                        onCheckedChange = { replaceExisting = it }
                    )
                    Text(
                        text = "Reemplazar catálogo actual",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(replaceExisting) }
            ) {
                Text("Importar", fontWeight = FontWeight.Bold)
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
fun CheckoutDialog(
    subtotalPrice: Double,
    onDismiss: () -> Unit,
    onConfirmSale: (paymentMethod: PaymentMethod, amountPaid: Double, changeGiven: Double, discountAmount: Double, finalTotal: Double) -> Unit
) {
    var paymentMethod by remember { mutableStateOf(PaymentMethod.CASH) }
    var selectedDiscountPct by remember { mutableIntStateOf(0) } // 0, 5, 10, 15, 20
    var customDiscountText by remember { mutableStateOf("") }
    var amountPaidText by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val discountAmount = remember(subtotalPrice, selectedDiscountPct, customDiscountText) {
        if (selectedDiscountPct > 0) {
            subtotalPrice * (selectedDiscountPct / 100.0)
        } else {
            val customVal = parseUserMoneyInput(customDiscountText) ?: 0.0
            customVal.coerceIn(0.0, subtotalPrice)
        }
    }

    val finalTotal = (subtotalPrice - discountAmount).coerceAtLeast(0.0)

    val amountPaidParsed = parseUserMoneyInput(amountPaidText) ?: finalTotal
    val changeGiven = if (paymentMethod == PaymentMethod.CASH) {
        (amountPaidParsed - finalTotal).coerceAtLeast(0.0)
    } else 0.0

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Cobrar Venta",
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Total a Cobrar y Descuento
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.primaryContainer,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "TOTAL A COBRAR",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Text(
                            text = formatMoney(finalTotal),
                            style = MaterialTheme.typography.headlineLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        if (discountAmount > 0) {
                            Text(
                                text = "Subtotal: ${formatMoney(subtotalPrice)} | Descuento: -${formatMoney(discountAmount)}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                            )
                        }
                    }
                }

                // Selección de Descuentos
                Text(
                    text = "Aplicar Descuento:",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold
                )

                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    val presets = listOf(0, 5, 10, 15, 20)
                    items(presets) { pct ->
                        FilterChip(
                            selected = selectedDiscountPct == pct && customDiscountText.isEmpty(),
                            onClick = {
                                selectedDiscountPct = pct
                                customDiscountText = ""
                            },
                            label = { Text(if (pct == 0) "Sin Desc." else "$pct%") }
                        )
                    }
                }

                OutlinedTextField(
                    value = customDiscountText,
                    onValueChange = {
                        customDiscountText = it
                        selectedDiscountPct = 0
                    },
                    label = { Text("Descuento Monto Fijo ($)") },
                    placeholder = { Text("Ej: 200") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )

                // Selección de Medio de Pago
                Text(
                    text = "Medio de Pago:",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = paymentMethod == PaymentMethod.CASH,
                        onClick = {
                            paymentMethod = PaymentMethod.CASH
                            errorMessage = null
                        },
                        label = { Text("💵 Efectivo") },
                        modifier = Modifier.weight(1f)
                    )

                    FilterChip(
                        selected = paymentMethod == PaymentMethod.TRANSFER,
                        onClick = {
                            paymentMethod = PaymentMethod.TRANSFER
                            errorMessage = null
                        },
                        label = { Text("💳 Transferencia/MP") },
                        modifier = Modifier.weight(1f)
                    )
                }

                // Si es efectivo: Calculadora de vuelto
                if (paymentMethod == PaymentMethod.CASH) {
                    OutlinedTextField(
                        value = amountPaidText,
                        onValueChange = {
                            amountPaidText = it
                            errorMessage = null
                        },
                        label = { Text("Paga con ($)") },
                        placeholder = { Text(formatMoney(finalTotal)) },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )

                    // Presets rápidos de dinero abonado
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        val presets = listOf(finalTotal, 1000.0, 2000.0, 5000.0, 10000.0, 20000.0)
                            .filter { it >= finalTotal }.distinct()

                        items(presets) { preset ->
                            SuggestionChip(
                                onClick = {
                                    amountPaidText = formatMoney(preset).removePrefix("$").trim()
                                },
                                label = { Text(if (preset == finalTotal) "Exacto" else formatMoney(preset)) }
                            )
                        }
                    }

                    // Vuelto a Entregar
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Vuelto a entregar:",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = formatMoney(changeGiven),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }

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
                    if (paymentMethod == PaymentMethod.CASH) {
                        if (amountPaidParsed < finalTotal) {
                            errorMessage = "El monto abonado no puede ser menor al total."
                            return@Button
                        }
                    }
                    onConfirmSale(paymentMethod, amountPaidParsed, changeGiven, discountAmount, finalTotal)
                }
            ) {
                Icon(imageVector = Icons.Default.CheckCircle, contentDescription = null)
                Spacer(modifier = Modifier.width(6.dp))
                Text("Confirmar Venta", fontWeight = FontWeight.Bold)
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
fun ManageVendorsDialog(
    vendors: List<String>,
    currentVendor: String,
    adminVendors: Set<String>,
    isAdmin: Boolean,
    onDismiss: () -> Unit,
    onSelectVendor: (String) -> Unit,
    onAddVendor: (String, Boolean) -> Unit,
    onDeleteVendor: (String) -> Unit
) {
    var newVendorName by remember { mutableStateOf("") }
    var isAdminChecked by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Gestionar Vendedores y Permisos", fontWeight = FontWeight.Bold) },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(380.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Vendedor actual: $currentVendor",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary
                )

                Text(text = "Lista de Vendedores:", style = MaterialTheme.typography.labelLarge)

                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    items(vendors) { vendor ->
                        val isAdm = adminVendors.contains(vendor)
                        val isCurrent = vendor == currentVendor
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = if (isCurrent) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant
                            ),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(text = vendor, fontWeight = FontWeight.Bold)
                                    Text(
                                        text = if (isAdm) "🔑 Administrador" else "👤 Vendedor",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = if (isAdm) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }

                                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    if (!isCurrent) {
                                        TextButton(onClick = {
                                            onSelectVendor(vendor)
                                            onDismiss()
                                        }) {
                                            Text("Elegir")
                                        }
                                    }
                                    if (isAdmin && vendor != "Admin") {
                                        IconButton(onClick = { onDeleteVendor(vendor) }) {
                                            Icon(imageVector = Icons.Default.Delete, contentDescription = "Eliminar", tint = MaterialTheme.colorScheme.error)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                if (isAdmin) {
                    HorizontalDivider()

                    Text(text = "Agregar Nuevo Vendedor:", style = MaterialTheme.typography.labelLarge)

                    OutlinedTextField(
                        value = newVendorName,
                        onValueChange = { newVendorName = it; errorMessage = null },
                        label = { Text("Nombre del vendedor") },
                        placeholder = { Text("Ej: Juan o María") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Checkbox(
                            checked = isAdminChecked,
                            onCheckedChange = { isAdminChecked = it }
                        )
                        Text(text = "Permisos de Administrador", style = MaterialTheme.typography.bodyMedium)
                    }

                    if (errorMessage != null) {
                        Text(text = errorMessage!!, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        },
        confirmButton = {
            if (isAdmin) {
                Button(
                    onClick = {
                        val trimmed = newVendorName.trim()
                        if (trimmed.isEmpty()) {
                            errorMessage = "Ingresa un nombre válido."
                        } else if (vendors.contains(trimmed)) {
                            errorMessage = "El vendedor ya existe."
                        } else {
                            onAddVendor(trimmed, isAdminChecked)
                            newVendorName = ""
                            isAdminChecked = false
                        }
                    }
                ) {
                    Text("Agregar Vendedor", fontWeight = FontWeight.Bold)
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(if (isAdmin) "Cerrar" else "Aceptar")
            }
        }
    )
}

@Composable
fun AdminPinPromptDialog(
    targetVendor: String,
    onConfirm: (String) -> Boolean,
    onResetPin: () -> Unit,
    onDismiss: () -> Unit
) {
    var pinText by remember { mutableStateOf("") }
    var isPinVisible by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Contraseña de Administrador", fontWeight = FontWeight.Bold) },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "Para cambiar al perfil de administrador ($targetVendor), ingresa la contraseña de Admin:",
                    style = MaterialTheme.typography.bodyMedium
                )

                OutlinedTextField(
                    value = pinText,
                    onValueChange = { pinText = it; errorMessage = null },
                    label = { Text("Contraseña de Admin") },
                    singleLine = true,
                    visualTransformation = if (isPinVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    trailingIcon = {
                        IconButton(onClick = { isPinVisible = !isPinVisible }) {
                            Text(if (isPinVisible) "👁️" else "🙈", fontSize = 16.sp)
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                )

                if (errorMessage != null) {
                    Text(text = errorMessage!!, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                }

                TextButton(
                    onClick = {
                        onDismiss()
                        onResetPin()
                    },
                    modifier = Modifier.align(Alignment.End)
                ) {
                    Text(
                        text = "¿Olvidaste la contraseña de Admin?",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val success = onConfirm(pinText)
                    if (!success) {
                        errorMessage = "Contraseña incorrecta."
                    }
                }
            ) {
                Text("Confirmar", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar")
            }
        }
    )
}

@Preview(showBackground = true)
@Composable
fun ProductItemCardPreview() {
    MaterialTheme {
        ProductItemCard(
            product = Product(name = "Sticker Holo", price = 400.0, quantity = 3, category = "Stickers", stock = 10),
            userRole = UserRole.ADMIN,
            onIncrement = {},
            onDecrement = {},
            onEdit = {},
            onRemove = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
fun ProductListScreenPreview() {
    MaterialTheme {
        ProductListScreen(viewModel = ProductViewModel())
    }
}
