@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
package com.sombetech.inventory.presentation.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.sombetech.inventory.data.model.Product
import com.sombetech.inventory.data.model.StockStatus
import com.sombetech.inventory.presentation.ui.theme.*
import com.sombetech.inventory.presentation.viewmodel.ProductsViewModel
import com.sombetech.inventory.presentation.viewmodel.StockFilter

@Composable
fun ProductsScreen(navController: NavController, vm: ProductsViewModel) {
    val state by vm.state.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            ClayTopBar(
                title        = "Products",
                isOffline    = state.isOffline,
                isRefreshing = state.isRefreshing,
            )
        },
        containerColor = ClayBackground,
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            // â”€â”€ Search bar â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp)
                    .clayShadow(shadowColor = ClayPrimary, borderRadius = 28.dp, shadowAlpha = 0.12f, blurRadius = 14.dp, offsetY = 6.dp)
                    .clip(RoundedCornerShape(28.dp))
                    .background(Color.White),
            ) {
                OutlinedTextField(
                    value = state.query,
                    onValueChange = vm::setQuery,
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("Search name or SKUâ€¦", color = Color(0xFFADB5BD)) },
                    leadingIcon = { Icon(Icons.Default.Search, null, tint = ClayPrimary) },
                    singleLine = true,
                    shape = RoundedCornerShape(28.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color.Transparent,
                        unfocusedBorderColor = Color.Transparent,
                        cursorColor = ClayPrimary,
                    ),
                )
            }

            // â”€â”€ Filter chips â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .padding(bottom = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                StockFilter.values().forEach { filter ->
                    val selected = state.stockFilter == filter
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(if (selected) ClayPrimary else Color.White)
                            .clickable { vm.setFilter(filter) }
                            .padding(horizontal = 14.dp, vertical = 7.dp),
                    ) {
                        Text(
                            text = when (filter) {
                                StockFilter.ALL -> "All"
                                StockFilter.LOW -> "Low Stock"
                                StockFilter.OUT -> "Out of Stock"
                            },
                            color = if (selected) Color.White else Color(0xFF64748B),
                            fontSize = 13.sp,
                            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                        )
                    }
                }
            }

            if (state.loading) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = ClayPrimary)
                }
                return@Scaffold
            }

            // â”€â”€ Product list â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
            LazyColumn(
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                if (state.filtered.isEmpty()) {
                    item {
                        Box(Modifier.fillMaxWidth().padding(top = 48.dp), contentAlignment = Alignment.Center) {
                            Text("No products found", color = Color(0xFF94A3B8))
                        }
                    }
                }
                items(state.filtered, key = { it.id }) { product ->
                    ClayProductCard(product) { navController.navigate("product/${product.id}") }
                }
                item { Spacer(Modifier.height(8.dp)) }
            }
        }
    }
}

@Composable
fun ClayProductCard(product: Product, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clayShadow(shadowColor = Color(0xFF64748B), borderRadius = 24.dp, shadowAlpha = 0.13f, blurRadius = 16.dp, offsetY = 7.dp)
            .clip(RoundedCornerShape(24.dp))
            .background(Color.White)
            .clickable(onClick = onClick)
            .padding(16.dp),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            // Name + price row
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Top) {
                Column(Modifier.weight(1f)) {
                    Text(product.displayName, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold, color = Color(0xFF1E293B))
                    Text(product.sku, style = MaterialTheme.typography.labelSmall, color = Color(0xFF94A3B8))
                }
                Text(
                    "${"$"}${"%.2f".format(product.price)}",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = ClayPrimary,
                )
            }

            // Category + stock chip
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(ClayPurpLight)
                        .padding(horizontal = 10.dp, vertical = 4.dp),
                ) {
                    Text(
                        product.categoryName.ifEmpty { "â€”" },
                        style = MaterialTheme.typography.labelSmall,
                        color = Color(0xFF7C3AED),
                        fontWeight = FontWeight.SemiBold,
                    )
                }
                ClayStockChip(product)
            }

            // Stock bar
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Stock: ${product.stock} ${product.unit}", style = MaterialTheme.typography.labelSmall, color = Color(0xFF94A3B8))
                    Text("Min: ${product.minStock}", style = MaterialTheme.typography.labelSmall, color = Color(0xFF94A3B8))
                }
                LinearProgressIndicator(
                    progress = { product.stockFraction },
                    modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)),
                    color = stockBarColor(product.stockStatus),
                    trackColor = stockBarColor(product.stockStatus).copy(alpha = 0.18f),
                )
            }
        }
    }
}

@Composable
private fun ClayStockChip(product: Product) {
    val (label, bg, fg) = when (product.stockStatus) {
        StockStatus.OK  -> Triple("In Stock",     Color(0xFFDCFCE7), Color(0xFF15803D))
        StockStatus.LOW -> Triple("Low Stock",    Color(0xFFFEF3C7), Color(0xFFB45309))
        StockStatus.OUT -> Triple("Out of Stock", Color(0xFFFFE4E6), Color(0xFFB91C1C))
    }
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(bg)
            .padding(horizontal = 10.dp, vertical = 4.dp),
    ) {
        Text(label, style = MaterialTheme.typography.labelSmall, color = fg, fontWeight = FontWeight.Bold)
    }
}

fun stockBarColor(status: StockStatus): Color = when (status) {
    StockStatus.OK  -> Color(0xFF22C55E)
    StockStatus.LOW -> Color(0xFFF97316)
    StockStatus.OUT -> Color(0xFFEF4444)
}

// Keep the old name as alias so ProductDetailScreen can still call it
@Composable
fun ProductCard(product: Product, onClick: () -> Unit) = ClayProductCard(product, onClick)

