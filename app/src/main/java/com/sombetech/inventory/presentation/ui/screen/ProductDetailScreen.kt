@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
package com.sombetech.inventory.presentation.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.sombetech.inventory.data.model.Product
import com.sombetech.inventory.data.model.StockStatus
import com.sombetech.inventory.data.model.Transaction
import com.sombetech.inventory.presentation.ui.theme.*
import com.sombetech.inventory.presentation.viewmodel.ProductDetailViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val timeFormat = SimpleDateFormat("MMM d, HH:mm", Locale.getDefault())

private val ADJUST_TYPES = listOf(
    Triple("in",         "Stock In",   Color(0xFF22C55E)),
    Triple("out",        "Stock Out",  Color(0xFFEF4444)),
    Triple("adjustment", "Adjust",     ClayPrimary),
)

@Composable
fun ProductDetailScreen(navController: NavController, vm: ProductDetailViewModel) {
    val state by vm.state.collectAsStateWithLifecycle()
    val snackbarHost = remember { SnackbarHostState() }

    LaunchedEffect(state.adjustSuccess) {
        if (state.adjustSuccess) {
            snackbarHost.showSnackbar("Stock updated successfully")
            vm.clearSuccess()
        }
    }

    Scaffold(
        topBar = {
            ClayTopBar(
                title        = state.product?.displayName ?: "Product",
                subtitle     = "Product Detail",
                onBack       = { navController.popBackStack() },
                isOffline    = state.isOffline,
                isRefreshing = state.isRefreshing,
            )
        },
        containerColor = ClayBackground,
        snackbarHost = {
            SnackbarHost(snackbarHost) { data ->
                Box(
                    modifier = Modifier
                        .padding(16.dp)
                        .clayShadow(Color(0xFF22C55E), borderRadius = 20.dp, shadowAlpha = 0.20f, blurRadius = 16.dp, offsetY = 6.dp)
                        .clip(RoundedCornerShape(20.dp))
                        .background(Brush.linearGradient(listOf(Color(0xFF22C55E), Color(0xFF16A34A))))
                        .padding(horizontal = 20.dp, vertical = 12.dp),
                ) {
                    Text(data.visuals.message, color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                }
            }
        },
    ) { padding ->
        if (state.loading) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = ClayPrimary)
            }
            return@Scaffold
        }

        val product = state.product ?: run {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Box(Modifier.size(64.dp).clip(CircleShape).background(Color(0xFFFFE4E6)), contentAlignment = Alignment.Center) {
                        Icon(Icons.Default.ErrorOutline, null, Modifier.size(36.dp), tint = Color(0xFFEF4444))
                    }
                    Text("Product not found", fontWeight = FontWeight.Bold, color = Color(0xFF1E293B))
                }
            }
            return@Scaffold
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            item { ClayProductInfoCard(product) }
            item { ClayStockAdjustCard(product, state.adjusting, vm::adjustStock) }

            state.error?.let { err ->
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clayShadow(Color(0xFFEF4444), borderRadius = 20.dp, shadowAlpha = 0.15f, blurRadius = 14.dp, offsetY = 5.dp)
                            .clip(RoundedCornerShape(20.dp))
                            .background(Color(0xFFFEE2E2))
                            .padding(14.dp),
                    ) {
                        Text(err, color = Color(0xFF991B1B), style = MaterialTheme.typography.bodySmall)
                    }
                }
            }

            if (state.transactions.isNotEmpty()) {
                item {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Box(Modifier.size(8.dp).clip(CircleShape).background(ClayPrimary))
                        Text("Transaction History", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = Color(0xFF1E293B))
                    }
                }
                items(state.transactions.take(10)) { tx -> ClayTransactionRow(tx) }
            }

            item { Spacer(Modifier.height(8.dp)) }
        }
    }
}

// â”€â”€ Product info card â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

@Composable
private fun ClayProductInfoCard(product: Product) {
    val stockColor = stockBarColor(product.stockStatus)
    val stockGradient = when (product.stockStatus) {
        StockStatus.OK  -> Brush.linearGradient(listOf(ClayGreenLight, ClayGreenDark))
        StockStatus.LOW -> Brush.linearGradient(listOf(ClayAmberLight, ClayAmberDark))
        StockStatus.OUT -> Brush.linearGradient(listOf(ClayRedLight,   ClayRedDark))
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clayShadow(Color(0xFF64748B), borderRadius = 28.dp, shadowAlpha = 0.13f, blurRadius = 18.dp, offsetY = 8.dp)
            .clip(RoundedCornerShape(28.dp))
            .background(Color.White)
            .padding(20.dp),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            // â”€â”€ Name / price row â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Top) {
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(product.displayName, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.ExtraBold, color = Color(0xFF1E293B))
                    Text(product.sku, style = MaterialTheme.typography.bodySmall, color = Color(0xFF94A3B8))
                    Spacer(Modifier.height(4.dp))
                    // Variant chips
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
                        if (product.size.isNotBlank()) {
                            Box(Modifier.clip(RoundedCornerShape(16.dp)).background(Color(0xFFF1F5F9)).padding(horizontal = 10.dp, vertical = 4.dp)) {
                                Text("Sz ${product.size}", style = MaterialTheme.typography.labelSmall, color = Color(0xFF334155), fontWeight = FontWeight.Bold)
                            }
                        }
                        if (product.productType.isNotBlank()) {
                            Box(Modifier.clip(RoundedCornerShape(16.dp)).background(ClayBlueLight).padding(horizontal = 10.dp, vertical = 4.dp)) {
                                Text(product.productType, style = MaterialTheme.typography.labelSmall, color = Color(0xFF1D4ED8), fontWeight = FontWeight.SemiBold)
                            }
                        }
                        if (product.gender.isNotBlank()) {
                            Box(Modifier.clip(RoundedCornerShape(16.dp)).background(ClayPurpLight).padding(horizontal = 10.dp, vertical = 4.dp)) {
                                Text(product.gender.replaceFirstChar { it.uppercaseChar() }, style = MaterialTheme.typography.labelSmall, color = Color(0xFF7C3AED), fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }
                    if (product.color.isNotBlank()) {
                        Text("Color: ${product.color}", style = MaterialTheme.typography.bodySmall, color = Color(0xFF64748B))
                    }
                    Spacer(Modifier.height(2.dp))
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(ClayPurpLight)
                            .padding(horizontal = 10.dp, vertical = 4.dp),
                    ) {
                        Text(product.categoryName.ifEmpty { "â€”" }, style = MaterialTheme.typography.labelSmall, color = Color(0xFF7C3AED), fontWeight = FontWeight.SemiBold)
                    }
                }
                Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text("$${product.price}", fontSize = 22.sp, fontWeight = FontWeight.ExtraBold, color = ClayPrimary)
                    Text("Cost: $${product.cost}", style = MaterialTheme.typography.labelSmall, color = Color(0xFF94A3B8))
                }
            }

            // â”€â”€ Stock status banner â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clayShadow(stockColor, borderRadius = 20.dp, shadowAlpha = 0.16f, blurRadius = 14.dp, offsetY = 6.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(stockGradient)
                    .padding(16.dp),
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Column {
                            Text("Current Stock", style = MaterialTheme.typography.labelMedium, color = Color(0xFF64748B))
                            Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text("${product.stock}", fontSize = 32.sp, fontWeight = FontWeight.ExtraBold, color = stockColor, lineHeight = 36.sp)
                                Text(product.unit, style = MaterialTheme.typography.bodyMedium, color = stockColor.copy(alpha = 0.75f), modifier = Modifier.padding(bottom = 4.dp))
                            }
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text("Min. Stock", style = MaterialTheme.typography.labelMedium, color = Color(0xFF64748B))
                            Text("${product.minStock} ${product.unit}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = Color(0xFF1E293B))
                        }
                    }
                    LinearProgressIndicator(
                        progress = { product.stockFraction },
                        modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)),
                        color = stockColor,
                        trackColor = stockColor.copy(alpha = 0.20f),
                    )
                }
            }

            // â”€â”€ Description â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
            if (product.description.isNotEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(ClayBackground)
                        .padding(12.dp),
                ) {
                    Text(product.description, style = MaterialTheme.typography.bodySmall, color = Color(0xFF64748B), lineHeight = 18.sp)
                }
            }
        }
    }
}

// â”€â”€ Stock adjustment card â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

@Composable
private fun ClayStockAdjustCard(
    product: Product,
    adjusting: Boolean,
    onAdjust: (Int, String, String) -> Unit,
) {
    var quantity by remember { mutableIntStateOf(1) }
    var type     by remember { mutableStateOf("in") }
    var note     by remember { mutableStateOf("") }
    var expanded by remember { mutableStateOf(false) }

    val selectedType = ADJUST_TYPES.first { it.first == type }
    val accentColor  = selectedType.third
    val applyGradient = Brush.linearGradient(
        when (type) {
            "in"  -> listOf(Color(0xFF22C55E), Color(0xFF16A34A))
            "out" -> listOf(Color(0xFFEF4444), Color(0xFFDC2626))
            else  -> listOf(ClayPrimary, ClayPrimaryEnd)
        }
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clayShadow(accentColor, borderRadius = 28.dp, shadowAlpha = 0.14f, blurRadius = 18.dp, offsetY = 8.dp)
            .clip(RoundedCornerShape(28.dp))
            .background(Color.White)
            .padding(20.dp),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            // Header
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Box(
                    Modifier.size(38.dp).clip(CircleShape).background(accentColor.copy(alpha = 0.12f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(Icons.Default.Tune, null, tint = accentColor, modifier = Modifier.size(20.dp))
                }
                Text("Adjust Stock", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = Color(0xFF1E293B))
            }

            // â”€â”€ Type pill selector â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
            Row(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .background(ClayBackground)
                    .padding(4.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                ADJUST_TYPES.forEach { (key, label, color) ->
                    val selected = type == key
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(16.dp))
                            .background(
                                if (selected) Brush.linearGradient(listOf(color, color.copy(alpha = 0.80f)))
                                else Brush.linearGradient(listOf(Color.Transparent, Color.Transparent))
                            )
                            .clickable { type = key }
                            .padding(vertical = 9.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            label,
                            color = if (selected) Color.White else Color(0xFF94A3B8),
                            fontSize = 12.sp,
                            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                        )
                    }
                }
            }

            // â”€â”€ Qty stepper â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clayShadow(accentColor, borderRadius = 20.dp, shadowAlpha = 0.10f, blurRadius = 12.dp, offsetY = 4.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(ClayBackground)
                    .padding(14.dp),
            ) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clayShadow(Color(0xFF94A3B8), borderRadius = 22.dp, shadowAlpha = 0.12f, blurRadius = 8.dp, offsetY = 3.dp)
                            .clip(CircleShape)
                            .background(if (quantity > 1) Brush.linearGradient(listOf(accentColor, accentColor.copy(0.8f))) else Brush.linearGradient(listOf(Color(0xFFE2E8F0), Color(0xFFE2E8F0))))
                            .clickable(enabled = quantity > 1) { quantity-- },
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(Icons.Default.Remove, "âˆ’", tint = if (quantity > 1) Color.White else Color(0xFF94A3B8), modifier = Modifier.size(20.dp))
                    }
                    Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("$quantity", fontSize = 28.sp, fontWeight = FontWeight.ExtraBold, color = Color(0xFF1E293B))
                        Text("units", fontSize = 11.sp, color = Color(0xFF94A3B8))
                    }
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clayShadow(accentColor, borderRadius = 22.dp, shadowAlpha = 0.18f, blurRadius = 8.dp, offsetY = 3.dp)
                            .clip(CircleShape)
                            .background(Brush.linearGradient(listOf(accentColor, accentColor.copy(0.8f))))
                            .clickable { quantity++ },
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(Icons.Default.Add, "+", tint = Color.White, modifier = Modifier.size(20.dp))
                    }
                }
            }

            // â”€â”€ Note field â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clayShadow(Color(0xFF94A3B8), borderRadius = 20.dp, shadowAlpha = 0.08f, blurRadius = 12.dp, offsetY = 4.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(ClayBackground),
            ) {
                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    placeholder = { Text("Note (optional)â€¦", color = Color(0xFFADB5BD)) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = RoundedCornerShape(20.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = accentColor.copy(alpha = 0.5f),
                        unfocusedBorderColor = Color.Transparent,
                        cursorColor = accentColor,
                    ),
                )
            }

            // â”€â”€ Apply button â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clayShadow(accentColor, borderRadius = 24.dp, shadowAlpha = 0.22f, blurRadius = 18.dp, offsetY = 8.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .background(if (!adjusting) applyGradient else Brush.linearGradient(listOf(Color(0xFFE2E8F0), Color(0xFFE2E8F0))))
                    .then(if (!adjusting) Modifier.clickable { onAdjust(quantity, type, note) } else Modifier)
                    .padding(vertical = 15.dp),
                contentAlignment = Alignment.Center,
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    if (adjusting) {
                        CircularProgressIndicator(Modifier.size(18.dp), color = Color.White, strokeWidth = 2.dp)
                    } else {
                        Icon(Icons.Default.CheckCircle, null, tint = if (!adjusting) Color.White else Color(0xFF94A3B8), modifier = Modifier.size(20.dp))
                    }
                    Text(
                        if (adjusting) "Applyingâ€¦" else "Apply Adjustment",
                        color = if (!adjusting) Color.White else Color(0xFF94A3B8),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.ExtraBold,
                    )
                }
            }
        }
    }
}

// â”€â”€ Transaction row â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

@Composable
private fun ClayTransactionRow(tx: Transaction) {
    val (icon, color, bgGrad) = when (tx.type) {
        "in"          -> Triple(Icons.Default.ArrowUpward,   Color(0xFF22C55E), Brush.linearGradient(listOf(ClayGreenLight, ClayGreenDark)))
        "out", "sale" -> Triple(Icons.Default.ArrowDownward, Color(0xFFEF4444), Brush.linearGradient(listOf(ClayRedLight,   ClayRedDark)))
        else          -> Triple(Icons.Default.SwapVert,      ClayPrimary,       Brush.linearGradient(listOf(ClayPurpLight,  ClayPurpDark)))
    }
    val sign = if (tx.type == "in") "+" else "âˆ’"

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clayShadow(color, borderRadius = 20.dp, shadowAlpha = 0.10f, blurRadius = 12.dp, offsetY = 5.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(Color.White)
            .padding(14.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Box(
                Modifier.size(40.dp).clip(CircleShape).background(bgGrad),
                contentAlignment = Alignment.Center,
            ) {
                Icon(icon, null, tint = color, modifier = Modifier.size(20.dp))
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    tx.type.replaceFirstChar { it.uppercaseChar() },
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1E293B),
                )
                if (tx.note.isNotEmpty()) {
                    Text(tx.note, style = MaterialTheme.typography.bodySmall, color = Color(0xFF94A3B8))
                }
            }
            Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    "$sign${tx.quantity}",
                    fontWeight = FontWeight.ExtraBold,
                    color = color,
                    fontSize = 16.sp,
                )
                Text(timeFormat.format(Date(tx.timestamp)), style = MaterialTheme.typography.labelSmall, color = Color(0xFF94A3B8))
            }
        }
    }
}

