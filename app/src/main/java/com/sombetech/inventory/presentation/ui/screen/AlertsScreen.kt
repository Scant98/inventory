@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
package com.sombetech.inventory.presentation.ui.screen

import androidx.compose.foundation.background
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sombetech.inventory.data.model.Product
import com.sombetech.inventory.data.model.StockStatus
import com.sombetech.inventory.presentation.ui.theme.*
import com.sombetech.inventory.presentation.viewmodel.DashboardUiState

@Composable
fun AlertsScreen(dashState: DashboardUiState) {
    var tab by remember { mutableIntStateOf(0) }

    Scaffold(
        topBar = { ClayTopBar(title = "Stock Alerts") },
        containerColor = ClayBackground,
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            // â”€â”€ Clay tab row â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp)
                    .clayShadow(shadowColor = ClayPrimary, borderRadius = 28.dp, shadowAlpha = 0.10f, blurRadius = 12.dp, offsetY = 5.dp)
                    .clip(RoundedCornerShape(28.dp))
                    .background(Color.White)
                    .padding(4.dp),
            ) {
                ClayTabButton(
                    label = "Low Stock",
                    count = dashState.lowStockProducts.size,
                    selected = tab == 0,
                    selectedColor = Color(0xFFF97316),
                    modifier = Modifier.weight(1f),
                    onClick = { tab = 0 },
                )
                ClayTabButton(
                    label = "Out of Stock",
                    count = dashState.outOfStockProducts.size,
                    selected = tab == 1,
                    selectedColor = Color(0xFFEF4444),
                    modifier = Modifier.weight(1f),
                    onClick = { tab = 1 },
                )
            }

            LazyColumn(
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                val listItems = if (tab == 0) dashState.lowStockProducts else dashState.outOfStockProducts

                if (listItems.isEmpty()) {
                    item {
                        Box(Modifier.fillMaxWidth().padding(top = 64.dp), contentAlignment = Alignment.Center) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                Box(
                                    Modifier.size(64.dp).clip(CircleShape).background(Color(0xFFDCFCE7)),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Icon(Icons.Default.CheckCircle, null, Modifier.size(36.dp), tint = Color(0xFF22C55E))
                                }
                                Text(
                                    if (tab == 0) "No low stock items" else "All products in stock",
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color(0xFF64748B),
                                )
                            }
                        }
                    }
                } else {
                    items(listItems, key = { it.id }) { product ->
                        ClayAlertCard(product, isOutOfStock = tab == 1)
                    }
                }

                // Live real-time alerts
                if (tab == 0 && dashState.recentAlerts.isNotEmpty()) {
                    item {
                        Spacer(Modifier.height(4.dp))
                        Text("Live Alerts", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = Color(0xFF1E293B))
                    }
                    items(dashState.recentAlerts, key = { "alert-${it.productId}" }) { alert ->
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clayShadow(shadowColor = Color(0xFFF97316), borderRadius = 20.dp, shadowAlpha = 0.12f, blurRadius = 12.dp, offsetY = 5.dp)
                                .clip(RoundedCornerShape(20.dp))
                                .background(Brush.linearGradient(listOf(ClayOrangeLight, ClayOrangeDark)))
                                .padding(14.dp),
                        ) {
                            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    Modifier.size(36.dp).clip(CircleShape).background(Color(0xFFF97316).copy(alpha = 0.15f)),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Icon(Icons.Default.Notifications, null, tint = Color(0xFFF97316), modifier = Modifier.size(20.dp))
                                }
                                Column(Modifier.weight(1f)) {
                                    Text(alert.productName, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = Color(0xFF1E293B))
                                    Text("${alert.stock} left Â· min ${alert.minStock} Â· ${alert.sku}", style = MaterialTheme.typography.bodySmall, color = Color(0xFF64748B))
                                }
                            }
                        }
                    }
                }

                item { Spacer(Modifier.height(8.dp)) }
            }
        }
    }
}

@Composable
private fun ClayTabButton(
    label: String,
    count: Int,
    selected: Boolean,
    selectedColor: Color,
    modifier: Modifier,
    onClick: () -> Unit,
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(24.dp))
            .background(if (selected) selectedColor else Color.Transparent)
            .padding(vertical = 10.dp),
        contentAlignment = Alignment.Center,
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(
                label,
                color = if (selected) Color.White else Color(0xFF94A3B8),
                fontSize = 13.sp,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
            )
            if (count > 0) {
                Box(
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(if (selected) Color.White.copy(alpha = 0.25f) else selectedColor.copy(alpha = 0.15f))
                        .padding(horizontal = 7.dp, vertical = 2.dp),
                ) {
                    Text(count.toString(), color = if (selected) Color.White else selectedColor, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun ClayAlertCard(product: Product, isOutOfStock: Boolean) {
    val accentColor   = if (isOutOfStock) Color(0xFFEF4444) else Color(0xFFF97316)
    val gradientStart = if (isOutOfStock) ClayRedLight else ClayOrangeLight
    val gradientEnd   = if (isOutOfStock) ClayRedDark  else ClayOrangeDark

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clayShadow(shadowColor = accentColor, borderRadius = 24.dp, shadowAlpha = 0.14f, blurRadius = 16.dp, offsetY = 7.dp)
            .clip(RoundedCornerShape(24.dp))
            .background(Brush.linearGradient(listOf(gradientStart, gradientEnd)))
            .padding(16.dp),
    ) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Row(Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(14.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier.size(44.dp).clip(CircleShape).background(accentColor.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        if (isOutOfStock) Icons.Default.Error else Icons.Default.Warning,
                        null,
                        tint = accentColor,
                        modifier = Modifier.size(24.dp),
                    )
                }
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(product.displayName, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold, color = Color(0xFF1E293B))
                    Text(product.sku, style = MaterialTheme.typography.labelSmall, color = Color(0xFF94A3B8))
                    if (!isOutOfStock) {
                        Spacer(Modifier.height(2.dp))
                        Text("${product.stock} / ${product.minStock} ${product.unit}", style = MaterialTheme.typography.bodySmall, color = accentColor, fontWeight = FontWeight.SemiBold)
                        Spacer(Modifier.height(4.dp))
                        LinearProgressIndicator(
                            progress = { product.stockFraction },
                            modifier = Modifier.width(140.dp).height(6.dp).clip(RoundedCornerShape(3.dp)),
                            color = accentColor,
                            trackColor = accentColor.copy(alpha = 0.2f),
                        )
                    } else {
                        Text("Out of stock", style = MaterialTheme.typography.bodySmall, color = accentColor, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(ClayPurpLight)
                    .padding(horizontal = 10.dp, vertical = 5.dp),
            ) {
                Text(product.categoryName.ifEmpty { "â€”" }, style = MaterialTheme.typography.labelSmall, color = Color(0xFF7C3AED), fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

