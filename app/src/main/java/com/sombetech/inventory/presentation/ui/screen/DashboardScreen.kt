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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
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
import com.sombetech.inventory.data.model.Product
import com.sombetech.inventory.data.model.Stats
import com.sombetech.inventory.presentation.ui.theme.*
import com.sombetech.inventory.presentation.viewmodel.DashboardViewModel

@Composable
fun DashboardScreen(vm: DashboardViewModel) {
    val state by vm.state.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            ClayTopBar(
                title        = "Dashboard",
                onRefresh    = { vm.load() },
                isOffline    = state.isOffline,
                isRefreshing = state.isRefreshing,
            )
        },
        containerColor = ClayBackground,
    ) { padding ->
        if (state.loading) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = ClayPrimary)
            }
            return@Scaffold
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            // â”€â”€ KPI grid â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
            item {
                state.stats?.let { stats ->
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            ClayStatCard(
                                modifier     = Modifier.weight(1f),
                                title        = "Products",
                                value        = stats.totalProducts.toString(),
                                icon         = Icons.Default.Inventory,
                                iconColor    = Color(0xFF3B82F6),
                                gradient     = Brush.linearGradient(listOf(ClayBlueLight, ClayBlueDark)),
                                shadowColor  = Color(0xFF3B82F6),
                            )
                            ClayStatCard(
                                modifier     = Modifier.weight(1f),
                                title        = "Inv. Value",
                                value        = "$${"%,.0f".format(stats.totalValue)}",
                                icon         = Icons.Default.AttachMoney,
                                iconColor    = Color(0xFF22C55E),
                                gradient     = Brush.linearGradient(listOf(ClayGreenLight, ClayGreenDark)),
                                shadowColor  = Color(0xFF22C55E),
                            )
                        }
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            ClayStatCard(
                                modifier     = Modifier.weight(1f),
                                title        = "Low / Out",
                                value        = "${stats.lowStockCount} / ${stats.outOfStockCount}",
                                icon         = Icons.Default.Warning,
                                iconColor    = Color(0xFFF97316),
                                gradient     = Brush.linearGradient(listOf(ClayAmberLight, ClayAmberDark)),
                                shadowColor  = Color(0xFFF97316),
                            )
                            ClayStatCard(
                                modifier     = Modifier.weight(1f),
                                title        = "Today Rev.",
                                value        = "$${"%,.0f".format(stats.todayRevenue)}",
                                icon         = Icons.Default.ShoppingCart,
                                iconColor    = Color(0xFFA855F7),
                                gradient     = Brush.linearGradient(listOf(ClayPurpLight, ClayPurpDark)),
                                shadowColor  = Color(0xFFA855F7),
                            )
                        }
                    }
                }
            }

            // â”€â”€ Today summary â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
            state.stats?.let { stats ->
                item { ClayTodaySummaryCard(stats) }
            }

            // â”€â”€ Low stock section â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
            if (state.lowStockProducts.isNotEmpty()) {
                item { SectionHeader("Low Stock", Color(0xFFF97316)) }
                items(state.lowStockProducts) { ClayLowStockRow(it) }
            }

            // â”€â”€ Out of stock section â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
            if (state.outOfStockProducts.isNotEmpty()) {
                item { SectionHeader("Out of Stock", Color(0xFFEF4444)) }
                items(state.outOfStockProducts) { ClayOutOfStockRow(it) }
            }

            // â”€â”€ Error â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
            state.error?.let { err ->
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clayShadow(shadowColor = Color(0xFFEF4444), borderRadius = 20.dp, shadowAlpha = 0.15f, offsetY = 6.dp)
                            .clip(RoundedCornerShape(20.dp))
                            .background(Color(0xFFFEE2E2))
                            .padding(14.dp),
                    ) {
                        Text(err, color = Color(0xFF991B1B), style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }
    }
}

// â”€â”€ Shared clay top bar â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

@Composable
internal fun ClayTopBar(
    title: String,
    subtitle: String = "Inventory Pro",
    onBack: (() -> Unit)? = null,
    onRefresh: (() -> Unit)? = null,
    isOffline: Boolean = false,
    isRefreshing: Boolean = false,
) {
    Column {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(Brush.horizontalGradient(listOf(ClayPrimary, ClayPrimaryEnd)))
                .statusBarsPadding()
                .padding(horizontal = 16.dp, vertical = 14.dp),
        ) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    if (onBack != null) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(Color.White.copy(alpha = 0.18f))
                                .clickable(onClick = onBack),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = Color.White, modifier = Modifier.size(18.dp))
                        }
                    }
                    Column {
                        Text(subtitle, color = Color.White.copy(alpha = 0.70f), fontSize = 12.sp)
                        Text(title, color = Color.White, fontSize = if (onBack != null) 18.sp else 22.sp, fontWeight = FontWeight.ExtraBold, maxLines = 1)
                    }
                }
                if (onRefresh != null) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.18f))
                            .clickable(onClick = onRefresh),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(Icons.Default.Refresh, "Refresh", tint = Color.White, modifier = Modifier.size(20.dp))
                    }
                }
            }
        }

        if (isOffline) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF1E293B))
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(Icons.Default.WifiOff, null, tint = Color.White.copy(0.75f), modifier = Modifier.size(13.dp))
                Spacer(Modifier.width(6.dp))
                Text(
                    "Offline · Showing cached data",
                    color    = Color.White.copy(0.85f),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                )
            }
        }

        if (isRefreshing) {
            LinearProgressIndicator(
                modifier   = Modifier.fillMaxWidth().height(2.dp),
                color      = ClayPrimary,
                trackColor = ClayPrimary.copy(alpha = 0.15f),
            )
        }
    }
}

// â”€â”€ Section header â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

@Composable
private fun SectionHeader(label: String, dotColor: Color) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Box(Modifier.size(8.dp).clip(CircleShape).background(dotColor))
        Text(label, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = Color(0xFF1E293B))
    }
}

// â”€â”€ Clay stat card â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

@Composable
private fun ClayStatCard(
    modifier: Modifier,
    title: String,
    value: String,
    icon: ImageVector,
    iconColor: Color,
    gradient: Brush,
    shadowColor: Color,
) {
    Box(
        modifier = modifier
            .clayShadow(shadowColor = shadowColor, borderRadius = 28.dp, shadowAlpha = 0.22f, blurRadius = 18.dp, offsetY = 8.dp)
            .clip(RoundedCornerShape(28.dp))
            .background(gradient)
            .padding(16.dp),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(iconColor.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(icon, null, tint = iconColor, modifier = Modifier.size(24.dp))
            }
            Text(value, fontSize = 22.sp, fontWeight = FontWeight.ExtraBold, color = Color(0xFF1E293B), lineHeight = 26.sp)
            Text(title, style = MaterialTheme.typography.labelMedium, color = Color(0xFF64748B))
        }
    }
}

// â”€â”€ Today summary card â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

@Composable
private fun ClayTodaySummaryCard(stats: Stats) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clayShadow(shadowColor = ClayPrimary, borderRadius = 28.dp, shadowAlpha = 0.28f, blurRadius = 22.dp, offsetY = 10.dp)
            .clip(RoundedCornerShape(28.dp))
            .background(Brush.linearGradient(listOf(ClayPrimary, ClayPrimaryEnd)))
            .padding(20.dp),
    ) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Column {
                Text("Today's Sales", color = Color.White.copy(alpha = 0.75f), fontSize = 12.sp, fontWeight = FontWeight.Medium)
                Text(
                    "${"$"}${"%.2f".format(stats.todayRevenue)}",
                    color = Color.White,
                    fontSize = 28.sp,
                    fontWeight = FontWeight.ExtraBold,
                    lineHeight = 32.sp,
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                Text("Orders", color = Color.White.copy(alpha = 0.75f), fontSize = 12.sp, fontWeight = FontWeight.Medium)
                Text(
                    stats.todayOrders.toString(),
                    color = Color.White,
                    fontSize = 28.sp,
                    fontWeight = FontWeight.ExtraBold,
                    lineHeight = 32.sp,
                )
            }
        }
    }
}

// â”€â”€ Low stock row â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

@Composable
private fun ClayLowStockRow(product: Product) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clayShadow(shadowColor = Color(0xFFF97316), borderRadius = 20.dp, shadowAlpha = 0.14f, blurRadius = 14.dp, offsetY = 6.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(Brush.linearGradient(listOf(ClayOrangeLight, ClayOrangeDark)))
            .padding(14.dp),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text(product.displayName, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold, color = Color(0xFF1E293B), modifier = Modifier.weight(1f))
                Text(
                    "${product.stock}/${product.minStock} ${product.unit}",
                    style = MaterialTheme.typography.labelMedium,
                    color = Color(0xFFF97316),
                    fontWeight = FontWeight.Bold,
                )
            }
            LinearProgressIndicator(
                progress = { product.stockFraction },
                modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)),
                color = Color(0xFFF97316),
                trackColor = Color(0xFFF97316).copy(alpha = 0.2f),
            )
        }
    }
}

// â”€â”€ Out of stock row â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

@Composable
private fun ClayOutOfStockRow(product: Product) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clayShadow(shadowColor = Color(0xFFEF4444), borderRadius = 20.dp, shadowAlpha = 0.14f, blurRadius = 14.dp, offsetY = 6.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(Brush.linearGradient(listOf(ClayRedLight, ClayRedDark)))
            .padding(14.dp),
    ) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(product.displayName, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold, color = Color(0xFF1E293B))
                Text(product.sku, style = MaterialTheme.typography.labelSmall, color = Color(0xFF64748B))
            }
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(Color(0xFFEF4444))
                    .padding(horizontal = 10.dp, vertical = 4.dp),
            ) {
                Text("OUT", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.ExtraBold)
            }
        }
    }
}

