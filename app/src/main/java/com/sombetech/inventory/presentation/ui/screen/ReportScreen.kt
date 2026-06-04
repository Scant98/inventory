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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sombetech.inventory.data.model.*
import com.sombetech.inventory.presentation.ui.theme.*
import com.sombetech.inventory.presentation.viewmodel.ReportViewModel

private fun Double.fmtMoney(): String = "${"$"}${"%.2f".format(this)}"
private fun Double.fmtK(): String = if (this >= 1000) "${"$"}${"%.1f".format(this / 1000)}k" else fmtMoney()

@Composable
fun ReportScreen(vm: ReportViewModel) {
    val state by vm.state.collectAsStateWithLifecycle()
    var tab by remember { mutableIntStateOf(0) }

    Scaffold(
        topBar = {
            ClayTopBar(title = "Reports", onRefresh = { vm.load() }, isOffline = state.isOffline)
        },
        containerColor = ClayBackground,
    ) { padding ->
        if (state.loading) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    CircularProgressIndicator(color = ClayPrimary)
                    Text("Generating report…", color = Color(0xFF64748B), fontSize = 14.sp)
                }
            }
            return@Scaffold
        }

        state.error?.let { err ->
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Box(Modifier.size(56.dp).clip(CircleShape).background(Color(0xFFFFE4E6)), contentAlignment = Alignment.Center) {
                        Icon(Icons.Default.ErrorOutline, null, tint = Color(0xFFEF4444), modifier = Modifier.size(30.dp))
                    }
                    Text(err, color = Color(0xFF991B1B), textAlign = TextAlign.Center, modifier = Modifier.padding(horizontal = 24.dp))
                    Box(
                        modifier = Modifier.clip(RoundedCornerShape(20.dp)).background(Brush.linearGradient(listOf(ClayPrimary, ClayPrimaryEnd))).clickable { vm.load() }.padding(horizontal = 24.dp, vertical = 10.dp),
                    ) { Text("Retry", color = Color.White, fontWeight = FontWeight.Bold) }
                }
            }
            return@Scaffold
        }

        val report = state.report ?: return@Scaffold

        Column(Modifier.fillMaxSize().padding(padding)) {
            // Tab selector
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp)
                    .clayShadow(ClayPrimary, borderRadius = 28.dp, shadowAlpha = 0.10f, blurRadius = 12.dp, offsetY = 5.dp)
                    .clip(RoundedCornerShape(28.dp)).background(Color.White).padding(4.dp),
            ) {
                listOf("Sales", "Inventory").forEachIndexed { i, label ->
                    Box(
                        modifier = Modifier.weight(1f).clip(RoundedCornerShape(24.dp))
                            .background(if (tab == i) Brush.linearGradient(listOf(ClayPrimary, ClayPrimaryEnd)) else Brush.linearGradient(listOf(Color.Transparent, Color.Transparent)))
                            .clickable { tab = i }.padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(label, color = if (tab == i) Color.White else Color(0xFF94A3B8), fontSize = 14.sp, fontWeight = if (tab == i) FontWeight.Bold else FontWeight.Normal)
                    }
                }
            }

            LazyColumn(
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                if (tab == 0) {
                    // ── SALES ──────────────────────────────────────────────────
                    item { SalesSummaryCard(report.sales) }
                    item { PeriodComparisonCard(report.sales) }
                    item { TopProductsCard(report.sales.topProducts) }
                    item { CategoryRevenueCard(report.sales.byCategory) }
                } else {
                    // ── INVENTORY ──────────────────────────────────────────────
                    item { InventorySummaryCard(report.inventory) }
                    item { StockMovementCard(report.inventory.stockMovement) }
                    item { BrandBreakdownCard(report.inventory.byBrand) }
                    item { CategoryInventoryCard(report.inventory.byCategory) }
                }
                item { Spacer(Modifier.height(8.dp)) }
            }
        }
    }
}

// ── Sales summary card ────────────────────────────────────────────────────────

@Composable
private fun SalesSummaryCard(sales: SalesReport) {
    Box(
        modifier = Modifier.fillMaxWidth()
            .clayShadow(ClayPrimary, borderRadius = 28.dp, shadowAlpha = 0.28f, blurRadius = 22.dp, offsetY = 10.dp)
            .clip(RoundedCornerShape(28.dp))
            .background(Brush.linearGradient(listOf(ClayPrimary, ClayPrimaryEnd)))
            .padding(20.dp),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Top) {
                Column {
                    Text("Today's Revenue", color = Color.White.copy(alpha = 0.75f), fontSize = 13.sp)
                    Text(sales.today.revenue.fmtK(), color = Color.White, fontSize = 30.sp, fontWeight = FontWeight.ExtraBold, lineHeight = 34.sp)
                    Text("${sales.today.orders} orders · ${sales.today.unitsSold} units", color = Color.White.copy(alpha = 0.75f), fontSize = 12.sp)
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text("Week Growth", color = Color.White.copy(alpha = 0.75f), fontSize = 13.sp)
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        Icon(
                            if (sales.revenueGrowth >= 0) Icons.Default.TrendingUp else Icons.Default.TrendingDown,
                            null, tint = if (sales.revenueGrowth >= 0) Color(0xFFBBF7D0) else Color(0xFFFFCDD2), modifier = Modifier.size(20.dp),
                        )
                        Text(
                            "${if (sales.revenueGrowth >= 0) "+" else ""}${sales.revenueGrowth}%",
                            color = if (sales.revenueGrowth >= 0) Color(0xFFBBF7D0) else Color(0xFFFFCDD2),
                            fontSize = 20.sp, fontWeight = FontWeight.ExtraBold,
                        )
                    }
                    Text("vs last week", color = Color.White.copy(alpha = 0.60f), fontSize = 11.sp)
                }
            }
            HorizontalDivider(color = Color.White.copy(alpha = 0.20f))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                QuickStat("This Week", sales.thisWeek.revenue.fmtK(), "${sales.thisWeek.orders} orders")
                QuickStatDivider()
                QuickStat("This Month", sales.thisMonth.revenue.fmtK(), "${sales.thisMonth.orders} orders")
                QuickStatDivider()
                QuickStat("All Time", sales.allTime.revenue.fmtK(), "${sales.allTime.orders} orders")
            }
        }
    }
}

@Composable
private fun QuickStat(label: String, value: String, sub: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(label, color = Color.White.copy(alpha = 0.65f), fontSize = 11.sp)
        Text(value, color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.ExtraBold)
        Text(sub, color = Color.White.copy(alpha = 0.55f), fontSize = 10.sp)
    }
}

@Composable
private fun QuickStatDivider() {
    Box(Modifier.width(1.dp).height(40.dp).background(Color.White.copy(alpha = 0.20f)))
}

// ── Period comparison card ────────────────────────────────────────────────────

@Composable
private fun PeriodComparisonCard(sales: SalesReport) {
    ClayCard(title = "Avg Order Value", icon = Icons.Default.ReceiptLong) {
        val periods = listOf(
            Triple("Today", sales.today.avgOrderValue, sales.today.orders),
            Triple("This Week", sales.thisWeek.avgOrderValue, sales.thisWeek.orders),
            Triple("This Month", sales.thisMonth.avgOrderValue, sales.thisMonth.orders),
            Triple("All Time", sales.allTime.avgOrderValue, sales.allTime.orders),
        )
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            periods.forEach { (label, avg, orders) ->
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Column {
                        Text(label, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold, color = Color(0xFF1E293B))
                        Text("$orders orders", style = MaterialTheme.typography.labelSmall, color = Color(0xFF94A3B8))
                    }
                    Text(if (orders > 0) avg.fmtMoney() else "—", fontWeight = FontWeight.ExtraBold, color = ClayPrimary, fontSize = 16.sp)
                }
                if (label != "All Time") HorizontalDivider(color = Color(0xFFF1F5F9))
            }
        }
    }
}

// ── Top products card ─────────────────────────────────────────────────────────

@Composable
private fun TopProductsCard(topProducts: List<TopProduct>) {
    ClayCard(title = "Top Products by Revenue", icon = Icons.Default.Star) {
        if (topProducts.isEmpty()) {
            Box(Modifier.fillMaxWidth().padding(vertical = 16.dp), contentAlignment = Alignment.Center) {
                Text("No sales data yet", color = Color(0xFF94A3B8))
            }
        } else {
            val maxRevenue = topProducts.maxOf { it.revenue }.coerceAtLeast(0.01)
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                topProducts.take(8).forEachIndexed { i, p ->
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                Box(
                                    Modifier.size(22.dp).clip(CircleShape).background(ClayPrimary.copy(alpha = if (i == 0) 1f else 0.15f)),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Text("${i+1}", color = if (i == 0) Color.White else ClayPrimary, fontSize = 10.sp, fontWeight = FontWeight.ExtraBold)
                                }
                                Column(Modifier.weight(1f)) {
                                    Text(p.productName, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold, color = Color(0xFF1E293B), maxLines = 1)
                                    Text("${p.productType} · ${p.unitsSold} units", style = MaterialTheme.typography.labelSmall, color = Color(0xFF94A3B8))
                                }
                            }
                            Text(p.revenue.fmtK(), fontWeight = FontWeight.ExtraBold, color = ClayPrimary, fontSize = 14.sp)
                        }
                        LinearProgressIndicator(
                            progress = { (p.revenue / maxRevenue).toFloat() },
                            modifier = Modifier.fillMaxWidth().height(4.dp).clip(RoundedCornerShape(2.dp)),
                            color = ClayPrimary.copy(alpha = 0.6f + 0.4f * (1f - i / topProducts.size.toFloat())),
                            trackColor = ClayPrimary.copy(alpha = 0.08f),
                        )
                    }
                }
            }
        }
    }
}

// ── Category revenue card ─────────────────────────────────────────────────────

@Composable
private fun CategoryRevenueCard(categories: List<CategoryRevenue>) {
    ClayCard(title = "Revenue by Category", icon = Icons.Default.PieChart) {
        if (categories.all { it.revenue == 0.0 }) {
            Box(Modifier.fillMaxWidth().padding(vertical = 16.dp), contentAlignment = Alignment.Center) {
                Text("No sales data yet", color = Color(0xFF94A3B8))
            }
        } else {
            val catColors = listOf(Color(0xFF6366F1), Color(0xFFA855F7), Color(0xFF22C55E), Color(0xFFF97316))
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                categories.filter { it.revenue > 0 }.forEachIndexed { i, cat ->
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Row(Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                            Box(Modifier.size(10.dp).clip(CircleShape).background(catColors.getOrElse(i) { Color(0xFF94A3B8) }))
                            Column(Modifier.weight(1f)) {
                                Text(cat.name, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold, color = Color(0xFF1E293B))
                                Text("${cat.orders} orders", style = MaterialTheme.typography.labelSmall, color = Color(0xFF94A3B8))
                            }
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text(cat.revenue.fmtK(), fontWeight = FontWeight.ExtraBold, color = Color(0xFF1E293B))
                            Box(Modifier.clip(RoundedCornerShape(12.dp)).background(catColors.getOrElse(i) { Color(0xFF94A3B8) }.copy(alpha = 0.15f)).padding(horizontal = 8.dp, vertical = 2.dp)) {
                                Text("${cat.percentage}%", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = catColors.getOrElse(i) { Color(0xFF94A3B8) })
                            }
                        }
                    }
                }
            }
        }
    }
}

// ── Inventory summary card ────────────────────────────────────────────────────

@Composable
private fun InventorySummaryCard(inv: InventoryReport) {
    Box(
        modifier = Modifier.fillMaxWidth()
            .clayShadow(Color(0xFF22C55E), borderRadius = 28.dp, shadowAlpha = 0.22f, blurRadius = 20.dp, offsetY = 9.dp)
            .clip(RoundedCornerShape(28.dp))
            .background(Brush.linearGradient(listOf(ClayGreenLight, ClayGreenDark)))
            .padding(20.dp),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                InvKpi("Total Products", inv.summary.totalProducts.toString(), "${inv.summary.totalStock} units in stock")
                InvKpi("Cost Value", inv.summary.costValue.fmtK(), "at purchase price", align = TextAlign.End)
            }
            HorizontalDivider(color = Color(0xFF22C55E).copy(alpha = 0.25f))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                InvKpi("Retail Value", inv.summary.retailValue.fmtK(), "at selling price")
                InvKpi("Potential Profit", inv.summary.potentialProfit.fmtK(), "${inv.summary.profitMarginPct}% margin", align = TextAlign.End)
            }
        }
    }
}

@Composable
private fun InvKpi(label: String, value: String, sub: String, align: TextAlign = TextAlign.Start) {
    Column(horizontalAlignment = if (align == TextAlign.End) Alignment.End else Alignment.Start, verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(label, color = Color(0xFF64748B), fontSize = 12.sp)
        Text(value, color = Color(0xFF1E293B), fontSize = 22.sp, fontWeight = FontWeight.ExtraBold, lineHeight = 26.sp, textAlign = align)
        Text(sub, color = Color(0xFF64748B), fontSize = 11.sp, textAlign = align)
    }
}

// ── Stock movement card ───────────────────────────────────────────────────────

@Composable
private fun StockMovementCard(sm: StockMovement) {
    ClayCard(title = "Stock Movement", icon = Icons.Default.SwapVert) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
            MovementStat("Received", sm.totalReceived, Color(0xFF22C55E), Icons.Default.ArrowUpward)
            Box(Modifier.width(1.dp).height(52.dp).background(Color(0xFFF1F5F9)))
            MovementStat("Sold", sm.totalSold, ClayPrimary, Icons.Default.ShoppingCart)
            Box(Modifier.width(1.dp).height(52.dp).background(Color(0xFFF1F5F9)))
            MovementStat("Adjusted", sm.totalAdjusted, Color(0xFFF97316), Icons.Default.Tune)
        }
    }
}

@Composable
private fun MovementStat(label: String, value: Int, color: Color, icon: ImageVector) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Box(Modifier.size(40.dp).clip(CircleShape).background(color.copy(alpha = 0.12f)), contentAlignment = Alignment.Center) {
            Icon(icon, null, tint = color, modifier = Modifier.size(20.dp))
        }
        Text("$value", fontSize = 20.sp, fontWeight = FontWeight.ExtraBold, color = Color(0xFF1E293B))
        Text(label, fontSize = 11.sp, color = Color(0xFF94A3B8))
    }
}

// ── Brand breakdown ────────────────────────────────────────────────────────────

@Composable
private fun BrandBreakdownCard(brands: List<BrandInventory>) {
    ClayCard(title = "Inventory by Brand", icon = Icons.Default.Store) {
        val maxRetail = brands.maxOfOrNull { it.retailValue }?.coerceAtLeast(0.01) ?: 1.0
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            brands.take(8).forEachIndexed { i, b ->
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(b.brand, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold, color = Color(0xFF1E293B))
                            Text("${b.products} SKUs · ${b.totalStock} units", style = MaterialTheme.typography.labelSmall, color = Color(0xFF94A3B8))
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text(b.retailValue.fmtK(), fontWeight = FontWeight.ExtraBold, color = ClayPrimary, fontSize = 14.sp)
                            Text("cost: ${b.costValue.fmtK()}", style = MaterialTheme.typography.labelSmall, color = Color(0xFF94A3B8))
                        }
                    }
                    LinearProgressIndicator(
                        progress = { (b.retailValue / maxRetail).toFloat() },
                        modifier = Modifier.fillMaxWidth().height(5.dp).clip(RoundedCornerShape(2.dp)),
                        color = ClayPrimary.copy(alpha = 0.5f + 0.5f * ((brands.size - i).toFloat() / brands.size)),
                        trackColor = ClayPrimary.copy(alpha = 0.08f),
                    )
                }
            }
        }
    }
}

// ── Category inventory card ───────────────────────────────────────────────────

@Composable
private fun CategoryInventoryCard(categories: List<CategoryRevenue>) {
    ClayCard(title = "Inventory by Category", icon = Icons.Default.Category) {
        val maxVal = categories.maxOfOrNull { it.revenue }?.coerceAtLeast(0.01) ?: 1.0
        val catColors = listOf(Color(0xFF3B82F6), Color(0xFFA855F7))
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            categories.forEachIndexed { i, cat ->
                Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                            Box(Modifier.size(10.dp).clip(CircleShape).background(catColors.getOrElse(i) { Color(0xFF94A3B8) }))
                            Text(cat.name, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold, color = Color(0xFF1E293B))
                        }
                        Text(cat.revenue.fmtK(), fontWeight = FontWeight.ExtraBold, color = Color(0xFF1E293B), fontSize = 15.sp)
                    }
                    LinearProgressIndicator(
                        progress = { (cat.revenue / maxVal).toFloat() },
                        modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)),
                        color = catColors.getOrElse(i) { Color(0xFF94A3B8) },
                        trackColor = catColors.getOrElse(i) { Color(0xFF94A3B8) }.copy(alpha = 0.15f),
                    )
                }
            }
        }
    }
}

// ── Reusable clay card container ──────────────────────────────────────────────

@Composable
private fun ClayCard(title: String, icon: ImageVector, content: @Composable ColumnScope.() -> Unit) {
    Box(
        modifier = Modifier.fillMaxWidth()
            .clayShadow(Color(0xFF64748B), borderRadius = 24.dp, shadowAlpha = 0.12f, blurRadius = 16.dp, offsetY = 7.dp)
            .clip(RoundedCornerShape(24.dp))
            .background(Color.White)
            .padding(16.dp),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(34.dp).clip(CircleShape).background(ClayPurpLight), contentAlignment = Alignment.Center) {
                    Icon(icon, null, tint = ClayPrimary, modifier = Modifier.size(18.dp))
                }
                Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = Color(0xFF1E293B))
            }
            HorizontalDivider(color = Color(0xFFF1F5F9))
            content()
        }
    }
}
