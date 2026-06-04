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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sombetech.inventory.data.model.Order
import com.sombetech.inventory.data.model.Product
import com.sombetech.inventory.presentation.ui.theme.*
import com.sombetech.inventory.presentation.viewmodel.CartItem
import com.sombetech.inventory.presentation.viewmodel.OrdersViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val dateFormat = SimpleDateFormat("MMM d, HH:mm", Locale.getDefault())

@Composable
fun OrdersScreen(vm: OrdersViewModel) {
    val state by vm.state.collectAsStateWithLifecycle()
    val snackbarHost = remember { SnackbarHostState() }
    var showProductPicker by remember { mutableStateOf(false) }

    LaunchedEffect(state.lastOrder) {
        state.lastOrder?.let {
            snackbarHost.showSnackbar("${it.orderNumber} — $${"%.2f".format(it.total)}")
            vm.clearLastOrder()
        }
    }
    LaunchedEffect(state.error) {
        state.error?.let { snackbarHost.showSnackbar("Error: $it") }
    }

    Scaffold(
        topBar = { ClayTopBar(title = "Point of Sale", isOffline = state.isOffline, isRefreshing = state.isRefreshing) },
        containerColor = ClayBackground,
        snackbarHost = {
            SnackbarHost(snackbarHost) { data ->
                Box(
                    modifier = Modifier
                        .padding(16.dp)
                        .clayShadow(ClayPrimary, borderRadius = 20.dp, shadowAlpha = 0.20f, blurRadius = 16.dp, offsetY = 6.dp)
                        .clip(RoundedCornerShape(20.dp))
                        .background(Brush.linearGradient(listOf(ClayPrimary, ClayPrimaryEnd)))
                        .padding(horizontal = 20.dp, vertical = 12.dp),
                ) {
                    Text(data.visuals.message, color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                }
            }
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                ClayCartSection(
                    cart        = state.cart,
                    subtotal    = state.cartSubtotal,
                    tax         = state.cartTax,
                    total       = state.cartTotal,
                    placing     = state.placing,
                    onAddItem   = { showProductPicker = true },
                    onRemove    = vm::removeFromCart,
                    onIncrement = vm::incrementCartItem,
                    onDecrement = vm::decrementCartItem,
                    onClear     = vm::clearCart,
                    onPlace     = vm::placeOrder,
                )
            }

            item {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.padding(top = 4.dp),
                ) {
                    Box(Modifier.size(8.dp).clip(CircleShape).background(ClayPrimary))
                    Text("Order History", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = Color(0xFF1E293B))
                }
            }

            if (state.orders.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier.fillMaxWidth()
                            .clayShadow(Color(0xFF94A3B8), borderRadius = 24.dp, shadowAlpha = 0.10f, blurRadius = 14.dp, offsetY = 5.dp)
                            .clip(RoundedCornerShape(24.dp))
                            .background(Color.White)
                            .padding(24.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text("No orders yet", color = Color(0xFF94A3B8), fontWeight = FontWeight.Medium)
                    }
                }
            }

            items(state.orders, key = { it.id }) { order -> ClayOrderRow(order) }
            item { Spacer(Modifier.height(8.dp)) }
        }
    }

    if (showProductPicker) {
        ClayProductPickerSheet(
            products  = state.products,
            onDismiss = { showProductPicker = false },
            onSelect  = { product, qty -> vm.addToCart(product, qty); showProductPicker = false },
        )
    }
}

// ── Cart section ──────────────────────────────────────────────────────────────

@Composable
private fun ClayCartSection(
    cart: List<CartItem>,
    subtotal: Double, tax: Double, total: Double,
    placing: Boolean,
    onAddItem: () -> Unit,
    onRemove: (String) -> Unit,
    onIncrement: (String) -> Unit,
    onDecrement: (String) -> Unit,
    onClear: () -> Unit,
    onPlace: () -> Unit,
) {
    Box(
        modifier = Modifier.fillMaxWidth()
            .clayShadow(ClayPrimary, borderRadius = 28.dp, shadowAlpha = 0.14f, blurRadius = 18.dp, offsetY = 8.dp)
            .clip(RoundedCornerShape(28.dp))
            .background(Color.White)
            .padding(18.dp),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Box(Modifier.size(32.dp).clip(CircleShape).background(ClayPurpLight), contentAlignment = Alignment.Center) {
                        Icon(Icons.Default.ShoppingCart, null, tint = ClayPrimary, modifier = Modifier.size(18.dp))
                    }
                    Text(
                        if (cart.isEmpty()) "Cart" else "Cart (${cart.sumOf { it.quantity }})",
                        style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = Color(0xFF1E293B),
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (cart.isNotEmpty()) {
                        Box(
                            modifier = Modifier.clip(RoundedCornerShape(20.dp)).background(Color(0xFFFFE4E6))
                                .clickable(onClick = onClear).padding(horizontal = 12.dp, vertical = 6.dp),
                        ) { Text("Clear", color = Color(0xFFB91C1C), fontSize = 13.sp, fontWeight = FontWeight.SemiBold) }
                    }
                    Box(
                        modifier = Modifier
                            .clayShadow(ClayPrimary, borderRadius = 20.dp, shadowAlpha = 0.18f, blurRadius = 12.dp, offsetY = 4.dp)
                            .clip(RoundedCornerShape(20.dp))
                            .background(Brush.linearGradient(listOf(ClayPrimary, ClayPrimaryEnd)))
                            .clickable(onClick = onAddItem)
                            .padding(horizontal = 14.dp, vertical = 6.dp),
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            Icon(Icons.Default.Add, null, tint = Color.White, modifier = Modifier.size(16.dp))
                            Text("Add", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            if (cart.isNotEmpty()) {
                cart.forEach { item ->
                    Box(
                        modifier = Modifier.fillMaxWidth()
                            .clayShadow(Color(0xFF94A3B8), borderRadius = 18.dp, shadowAlpha = 0.10f, blurRadius = 10.dp, offsetY = 4.dp)
                            .clip(RoundedCornerShape(18.dp))
                            .background(ClayBackground)
                            .padding(12.dp),
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Top) {
                                Column(Modifier.weight(1f)) {
                                    Text(item.product.displayName, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold, color = Color(0xFF1E293B))
                                    if (item.product.displayDetail.isNotBlank()) {
                                        Text(item.product.displayDetail, style = MaterialTheme.typography.bodySmall, color = Color(0xFF94A3B8))
                                    }
                                }
                                Text("$${"%.2f".format(item.product.price * item.quantity)}", fontWeight = FontWeight.ExtraBold, color = ClayPrimary, fontSize = 15.sp)
                            }
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Box(
                                        modifier = Modifier.size(28.dp).clip(CircleShape)
                                            .background(if (item.quantity > 1) Brush.linearGradient(listOf(ClayPrimary, ClayPrimaryEnd)) else Brush.linearGradient(listOf(Color(0xFFE2E8F0), Color(0xFFE2E8F0))))
                                            .clickable(enabled = item.quantity > 1) { onDecrement(item.product.id) },
                                        contentAlignment = Alignment.Center,
                                    ) { Icon(Icons.Default.Remove, "−", tint = if (item.quantity > 1) Color.White else Color(0xFF94A3B8), modifier = Modifier.size(14.dp)) }
                                    Text("${item.quantity}", fontSize = 16.sp, fontWeight = FontWeight.ExtraBold, color = Color(0xFF1E293B))
                                    Box(
                                        modifier = Modifier.size(28.dp).clip(CircleShape)
                                            .background(if (item.quantity < item.product.stock) Brush.linearGradient(listOf(ClayPrimary, ClayPrimaryEnd)) else Brush.linearGradient(listOf(Color(0xFFE2E8F0), Color(0xFFE2E8F0))))
                                            .clickable(enabled = item.quantity < item.product.stock) { onIncrement(item.product.id) },
                                        contentAlignment = Alignment.Center,
                                    ) { Icon(Icons.Default.Add, "+", tint = if (item.quantity < item.product.stock) Color.White else Color(0xFF94A3B8), modifier = Modifier.size(14.dp)) }
                                    Text("× $${"%.2f".format(item.product.price)}", style = MaterialTheme.typography.bodySmall, color = Color(0xFF94A3B8))
                                }
                                Box(
                                    modifier = Modifier.size(28.dp).clip(CircleShape).background(Color(0xFFFFE4E6))
                                        .clickable { onRemove(item.product.id) },
                                    contentAlignment = Alignment.Center,
                                ) { Icon(Icons.Default.Close, "Remove", tint = Color(0xFFB91C1C), modifier = Modifier.size(14.dp)) }
                            }
                        }
                    }
                }

                Box(
                    modifier = Modifier.fillMaxWidth()
                        .clayShadow(ClayPrimary, borderRadius = 20.dp, shadowAlpha = 0.20f, blurRadius = 14.dp, offsetY = 6.dp)
                        .clip(RoundedCornerShape(20.dp))
                        .background(Brush.linearGradient(listOf(ClayPrimary, ClayPrimaryEnd)))
                        .padding(16.dp),
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Subtotal", color = Color.White.copy(alpha = 0.75f), fontSize = 13.sp)
                            Text("$${"%.2f".format(subtotal)}", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                        }
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Tax (10%)", color = Color.White.copy(alpha = 0.75f), fontSize = 13.sp)
                            Text("$${"%.2f".format(tax)}", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                        }
                        HorizontalDivider(color = Color.White.copy(alpha = 0.25f))
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Text("Total", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                            Text("$${"%.2f".format(total)}", color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.ExtraBold)
                        }
                    }
                }

                Box(
                    modifier = Modifier.fillMaxWidth()
                        .clayShadow(Color(0xFF22C55E), borderRadius = 24.dp, shadowAlpha = 0.22f, blurRadius = 16.dp, offsetY = 7.dp)
                        .clip(RoundedCornerShape(24.dp))
                        .background(if (!placing) Brush.linearGradient(listOf(Color(0xFF22C55E), Color(0xFF16A34A))) else Brush.linearGradient(listOf(Color(0xFFBBF7D0), Color(0xFFBBF7D0))))
                        .then(if (!placing) Modifier.clickable(onClick = onPlace) else Modifier)
                        .padding(vertical = 14.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                        if (placing) CircularProgressIndicator(Modifier.size(18.dp), color = Color.White, strokeWidth = 2.dp)
                        else Icon(Icons.Default.CheckCircle, null, tint = Color.White, modifier = Modifier.size(20.dp))
                        Text(if (placing) "Processing…" else "Complete Sale", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.ExtraBold)
                    }
                }
            } else {
                Box(Modifier.fillMaxWidth().padding(vertical = 12.dp), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Box(Modifier.size(52.dp).clip(CircleShape).background(ClayPurpLight), contentAlignment = Alignment.Center) {
                            Icon(Icons.Default.ShoppingCart, null, tint = ClayPrimary, modifier = Modifier.size(28.dp))
                        }
                        Text("Cart is empty", color = Color(0xFF64748B), fontWeight = FontWeight.SemiBold)
                        Text("Tap Add to pick products", color = Color(0xFF94A3B8), fontSize = 13.sp)
                    }
                }
            }
        }
    }
}

// ── Order row ─────────────────────────────────────────────────────────────────

@Composable
private fun ClayOrderRow(order: Order) {
    Box(
        modifier = Modifier.fillMaxWidth()
            .clayShadow(Color(0xFF94A3B8), borderRadius = 22.dp, shadowAlpha = 0.11f, blurRadius = 14.dp, offsetY = 6.dp)
            .clip(RoundedCornerShape(22.dp))
            .background(Color.White)
            .padding(16.dp),
    ) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(42.dp).clip(CircleShape).background(ClayGreenLight), contentAlignment = Alignment.Center) {
                    Icon(Icons.Default.Receipt, null, tint = Color(0xFF16A34A), modifier = Modifier.size(22.dp))
                }
                Column {
                    Text(order.orderNumber, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold, color = Color(0xFF1E293B))
                    Text("${order.itemCount} items · ${dateFormat.format(Date(order.createdAt))}", style = MaterialTheme.typography.bodySmall, color = Color(0xFF94A3B8))
                }
            }
            Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("$${"%.2f".format(order.total)}", fontWeight = FontWeight.ExtraBold, color = Color(0xFF1E293B), fontSize = 16.sp)
                Box(modifier = Modifier.clip(RoundedCornerShape(20.dp)).background(ClayGreenLight).padding(horizontal = 10.dp, vertical = 3.dp)) {
                    Text(order.status, style = MaterialTheme.typography.labelSmall, color = Color(0xFF15803D), fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

// ── Product picker bottom sheet ───────────────────────────────────────────────

@Composable
private fun ClayProductPickerSheet(
    products: List<Product>,
    onDismiss: () -> Unit,
    onSelect: (Product, Int) -> Unit,
) {
    var qty      by remember { mutableIntStateOf(1) }
    var selected by remember { mutableStateOf<Product?>(null) }
    var search   by remember { mutableStateOf("") }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        modifier = Modifier.fillMaxHeight(0.88f),
        containerColor = ClayBackground,
        dragHandle = {
            Box(Modifier.padding(top = 12.dp, bottom = 8.dp)) {
                Box(Modifier.width(40.dp).height(4.dp).clip(CircleShape).background(Color(0xFFCBD5E1)))
            }
        },
    ) {
        Column(Modifier.padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Select Product", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.ExtraBold, color = Color(0xFF1E293B))

            Box(
                modifier = Modifier.fillMaxWidth()
                    .clayShadow(ClayPrimary, borderRadius = 24.dp, shadowAlpha = 0.10f, blurRadius = 12.dp, offsetY = 5.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .background(Color.White),
            ) {
                OutlinedTextField(
                    value = search,
                    onValueChange = { search = it },
                    placeholder = { Text("Brand, name, size…", color = Color(0xFFADB5BD)) },
                    leadingIcon = { Icon(Icons.Default.Search, null, tint = ClayPrimary) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = RoundedCornerShape(24.dp),
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Color.Transparent, unfocusedBorderColor = Color.Transparent, cursorColor = ClayPrimary),
                )
            }

            val filtered = products.filter {
                search.isEmpty() ||
                it.displayName.contains(search, ignoreCase = true) ||
                it.size.contains(search, ignoreCase = true) ||
                it.color.contains(search, ignoreCase = true) ||
                it.productType.contains(search, ignoreCase = true)
            }

            LazyColumn(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(filtered, key = { it.id }) { product ->
                    val isSelected = selected?.id == product.id
                    Box(
                        modifier = Modifier.fillMaxWidth()
                            .clayShadow(if (isSelected) ClayPrimary else Color(0xFF94A3B8), borderRadius = 20.dp, shadowAlpha = if (isSelected) 0.18f else 0.08f, blurRadius = 12.dp, offsetY = 5.dp)
                            .clip(RoundedCornerShape(20.dp))
                            .background(if (isSelected) Brush.linearGradient(listOf(ClayPurpLight, ClayPurpDark)) else Brush.linearGradient(listOf(Color.White, Color.White)))
                            .clickable { selected = product; qty = 1 }
                            .padding(14.dp),
                    ) {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                Text(product.displayName, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold, color = if (isSelected) ClayPrimary else Color(0xFF1E293B))
                                if (product.displayDetail.isNotBlank()) {
                                    Text(product.displayDetail, style = MaterialTheme.typography.bodySmall, color = if (isSelected) ClayPrimary.copy(alpha = 0.7f) else Color(0xFF94A3B8))
                                }
                                Text("${product.stock} ${product.unit} · $${"%.2f".format(product.price)}", style = MaterialTheme.typography.bodySmall, color = if (isSelected) ClayPrimary.copy(alpha = 0.7f) else Color(0xFF94A3B8))
                            }
                            if (isSelected) {
                                Box(Modifier.size(28.dp).clip(CircleShape).background(ClayPrimary), contentAlignment = Alignment.Center) {
                                    Icon(Icons.Default.Check, null, tint = Color.White, modifier = Modifier.size(16.dp))
                                }
                            }
                        }
                    }
                }
                item { Spacer(Modifier.height(4.dp)) }
            }

            if (selected != null) {
                Box(
                    modifier = Modifier.fillMaxWidth()
                        .clayShadow(ClayPrimary, borderRadius = 24.dp, shadowAlpha = 0.12f, blurRadius = 14.dp, offsetY = 5.dp)
                        .clip(RoundedCornerShape(24.dp))
                        .background(Color.White)
                        .padding(12.dp),
                ) {
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        val canDec = qty > 1
                        val canInc = selected != null && qty < (selected?.stock ?: 0)
                        Box(
                            modifier = Modifier.size(44.dp)
                                .clayShadow(ClayPrimary, borderRadius = 22.dp, shadowAlpha = 0.14f, blurRadius = 10.dp, offsetY = 4.dp)
                                .clip(CircleShape)
                                .background(if (canDec) Brush.linearGradient(listOf(ClayPrimary, ClayPrimaryEnd)) else Brush.linearGradient(listOf(Color(0xFFE2E8F0), Color(0xFFE2E8F0))))
                                .clickable(enabled = canDec) { qty-- },
                            contentAlignment = Alignment.Center,
                        ) { Icon(Icons.Default.Remove, "−", tint = if (canDec) Color.White else Color(0xFF94A3B8), modifier = Modifier.size(20.dp)) }
                        Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("$qty", fontSize = 24.sp, fontWeight = FontWeight.ExtraBold, color = Color(0xFF1E293B))
                            Text("units", fontSize = 11.sp, color = Color(0xFF94A3B8))
                        }
                        Box(
                            modifier = Modifier.size(44.dp)
                                .clayShadow(ClayPrimary, borderRadius = 22.dp, shadowAlpha = 0.18f, blurRadius = 10.dp, offsetY = 4.dp)
                                .clip(CircleShape)
                                .background(if (canInc) Brush.linearGradient(listOf(ClayPrimary, ClayPrimaryEnd)) else Brush.linearGradient(listOf(Color(0xFFE2E8F0), Color(0xFFE2E8F0))))
                                .clickable(enabled = canInc) { qty++ },
                            contentAlignment = Alignment.Center,
                        ) { Icon(Icons.Default.Add, "+", tint = if (canInc) Color.White else Color(0xFF94A3B8), modifier = Modifier.size(20.dp)) }
                    }
                }
            }

            Box(
                modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)
                    .clayShadow(ClayPrimary, borderRadius = 28.dp, shadowAlpha = 0.22f, blurRadius = 18.dp, offsetY = 8.dp)
                    .clip(RoundedCornerShape(28.dp))
                    .background(if (selected != null) Brush.linearGradient(listOf(ClayPrimary, ClayPrimaryEnd)) else Brush.linearGradient(listOf(Color(0xFFE2E8F0), Color(0xFFE2E8F0))))
                    .then(if (selected != null) Modifier.clickable { selected?.let { onSelect(it, qty) } } else Modifier)
                    .padding(vertical = 16.dp),
                contentAlignment = Alignment.Center,
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.AddShoppingCart, null, tint = if (selected != null) Color.White else Color(0xFF94A3B8), modifier = Modifier.size(20.dp))
                    Text(
                        if (selected != null) "Add to Cart  $${"%.2f".format((selected?.price ?: 0.0) * qty)}" else "Add to Cart",
                        color = if (selected != null) Color.White else Color(0xFF94A3B8),
                        fontSize = 16.sp, fontWeight = FontWeight.ExtraBold,
                    )
                }
            }
        }
    }
}
