package com.example.myapplication.ui

import android.content.Intent
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedIconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import com.example.myapplication.model.Product
import com.example.myapplication.util.LabelSize
import com.example.myapplication.util.ProductLabelPdfGenerator
import java.io.File

@Composable
fun PrintLabelDialog(
    businessName: String,
    product: Product,
    allProducts: List<Product> = emptyList(),
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var selectedSize by remember { mutableStateOf(LabelSize.MEDIUM) }

    // Mapa de cantidad de etiquetas por producto para Hoja A4
    val multiProductMap = remember(allProducts, product) {
        mutableStateMapOf<String, Int>().apply {
            put(product.id, 21) // Por defecto, 21 etiquetas del producto seleccionado
            allProducts.filter { it.id != product.id && !it.isTemporary }.forEach { p ->
                put(p.id, 0)
            }
        }
    }

    val totalA4Labels = remember(multiProductMap.toMap()) {
        multiProductMap.values.sum()
    }

    val sanitizedName = remember(product.name) { product.name.replace(Regex("[^a-zA-Z0-9]"), "_") }
    val pdfFileName = remember(selectedSize, product.name) {
        if (selectedSize == LabelSize.FULL_A4) "etiquetas_A4_multiples.pdf" else "etiqueta_$sanitizedName.pdf"
    }

    fun buildPdfFile(): File? {
        return if (selectedSize == LabelSize.FULL_A4) {
            val labelItems = allProducts.mapNotNull { p ->
                val qty = multiProductMap[p.id] ?: 0
                if (qty > 0) Pair(p, qty) else null
            }
            ProductLabelPdfGenerator.generateMultiProductA4Pdf(
                context = context,
                businessName = businessName,
                labelItems = if (labelItems.isNotEmpty()) labelItems else listOf(Pair(product, 21))
            )
        } else {
            ProductLabelPdfGenerator.generateProductLabelPdf(
                context = context,
                businessName = businessName,
                product = product,
                labelSize = selectedSize
            )
        }
    }

    val createPdfLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/pdf")
    ) { uri ->
        if (uri != null) {
            try {
                val pdfFile = buildPdfFile()
                if (pdfFile != null && pdfFile.exists()) {
                    context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                        outputStream.write(pdfFile.readBytes())
                    }
                    Toast.makeText(context, "¡Etiqueta PDF guardada con éxito!", Toast.LENGTH_SHORT).show()
                    onDismiss()
                } else {
                    Toast.makeText(context, "Error al generar la etiqueta PDF.", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(context, "Error al guardar la etiqueta.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Imprimir / Generar Etiqueta",
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Configura el formato para: ${product.name}",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold
                )

                // Tamaño de la Etiqueta
                Text(
                    text = "Tamaño de la Etiqueta:",
                    style = MaterialTheme.typography.labelMedium
                )
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    LabelSize.entries.forEach { size ->
                        FilterChip(
                            selected = selectedSize == size,
                            onClick = { selectedSize = size },
                            label = { Text(size.displayName) },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }

                // Si se selecciona Hoja A4, permitir seleccionar múltiples productos y cantidades
                if (selectedSize == LabelSize.FULL_A4) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.primaryContainer,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "Selecciona la cantidad de etiquetas para cada producto en la hoja A4 (Capacidad máxima recomendada: 21 etiquetas por hoja):",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.padding(10.dp)
                        )
                    }

                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(180.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        val availableProducts = if (allProducts.isNotEmpty()) allProducts.filter { !it.isTemporary } else listOf(product)
                        items(availableProducts, key = { it.id }) { p ->
                            val currentQty = multiProductMap[p.id] ?: 0
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 10.dp, vertical = 6.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = p.name,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.weight(1f)
                                    )

                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        OutlinedIconButton(
                                            onClick = {
                                                if (currentQty > 0) multiProductMap[p.id] = currentQty - 1
                                            },
                                            modifier = Modifier.size(30.dp)
                                        ) {
                                            Text("−", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                        }

                                        Text(
                                            text = "$currentQty",
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(horizontal = 6.dp)
                                        )

                                        OutlinedIconButton(
                                            onClick = {
                                                multiProductMap[p.id] = currentQty + 1
                                            },
                                            modifier = Modifier.size(30.dp)
                                        ) {
                                            Text("+", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                        }
                                    }
                                }
                            }
                        }
                    }

                    Text(
                        text = "Total etiquetas seleccionadas: $totalA4Labels / 21",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { createPdfLauncher.launch(pdfFileName) }
            ) {
                Icon(imageVector = Icons.Default.Share, contentDescription = null)
                Spacer(modifier = Modifier.width(6.dp))
                Text("💾 Guardar PDF", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            OutlinedButton(
                onClick = {
                    val pdfFile = buildPdfFile()
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
                            context.startActivity(Intent.createChooser(shareIntent, "Compartir / Imprimir Etiqueta"))
                            onDismiss()
                        } catch (e: Exception) {
                            Toast.makeText(context, "Error al compartir la etiqueta.", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            ) {
                Text("📤 Compartir / Imprimir")
            }
        }
    )
}
