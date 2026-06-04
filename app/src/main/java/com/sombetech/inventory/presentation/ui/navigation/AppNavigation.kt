@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
package com.sombetech.inventory.presentation.ui.navigation

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.mutableStateOf
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.sombetech.inventory.presentation.ui.theme.ClayPrimary
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Inventory
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.sombetech.inventory.presentation.ui.screen.SplashScreen
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.sombetech.inventory.InventoryApplication
import com.sombetech.inventory.presentation.ui.screen.AlertsScreen
import com.sombetech.inventory.presentation.ui.screen.DashboardScreen
import com.sombetech.inventory.presentation.ui.screen.OrdersScreen
import com.sombetech.inventory.presentation.ui.screen.ProductDetailScreen
import com.sombetech.inventory.presentation.ui.screen.ProductsScreen
import com.sombetech.inventory.presentation.viewmodel.DashboardViewModel
import com.sombetech.inventory.presentation.ui.screen.ReportScreen
import com.sombetech.inventory.presentation.viewmodel.ReportViewModel
import com.sombetech.inventory.presentation.viewmodel.OrdersViewModel
import com.sombetech.inventory.presentation.viewmodel.ProductDetailViewModel
import com.sombetech.inventory.presentation.viewmodel.ProductsViewModel

sealed class Screen(val route: String, val label: String, val icon: ImageVector) {
    object Dashboard : Screen("dashboard", "Dashboard", Icons.Default.Dashboard)
    object Products  : Screen("products",  "Products",  Icons.Default.Inventory)
    object Orders    : Screen("orders",    "Orders",    Icons.Default.ShoppingCart)
    object Alerts    : Screen("alerts",    "Alerts",    Icons.Default.Notifications)
    object Reports   : Screen("reports",   "Reports",   Icons.Default.BarChart)
}

private val bottomNavItems = listOf(Screen.Dashboard, Screen.Products, Screen.Orders, Screen.Alerts, Screen.Reports)

@Composable
fun AppNavigation() {
    var splashDone by remember { mutableStateOf(false) }

    AnimatedContent(
        targetState = splashDone,
        transitionSpec = { fadeIn(tween(500)) togetherWith fadeOut(tween(300)) },
        label = "splash_transition",
    ) { done ->
        if (!done) {
            SplashScreen { splashDone = true }
            return@AnimatedContent
        }
        MainContent()
    }
}

@Composable
private fun MainContent() {
    val context = LocalContext.current
    val container = remember { (context.applicationContext as InventoryApplication).container }
    val repo = container.inventoryRepository

    val navController = rememberNavController()

    val dashboardVm: DashboardViewModel = viewModel(factory = DashboardViewModel.Factory(repo))
    val dashState by dashboardVm.state.collectAsStateWithLifecycle()
    val alertCount = dashState.recentAlerts.size + dashState.outOfStockProducts.size

    Scaffold(
        bottomBar = { BottomNav(navController, alertCount) }
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Dashboard.route,
            modifier = Modifier.padding(padding),
            enterTransition    = { slideInHorizontally  { it / 4 } + fadeIn()  },
            exitTransition     = { slideOutHorizontally { -it / 4 } + fadeOut() },
            popEnterTransition = { slideInHorizontally  { -it / 4 } + fadeIn() },
            popExitTransition  = { slideOutHorizontally { it / 4 } + fadeOut() },
        ) {
            composable(Screen.Dashboard.route) {
                DashboardScreen(dashboardVm)
            }
            composable(Screen.Products.route) {
                val vm: ProductsViewModel = viewModel(factory = ProductsViewModel.Factory(repo))
                ProductsScreen(navController, vm)
            }
            composable(Screen.Orders.route) {
                val vm: OrdersViewModel = viewModel(factory = OrdersViewModel.Factory(repo))
                OrdersScreen(vm)
            }
            composable(Screen.Alerts.route) {
                AlertsScreen(dashState)
            }
            composable(Screen.Reports.route) {
                val vm: ReportViewModel = viewModel(factory = ReportViewModel.Factory(repo))
                ReportScreen(vm)
            }
            composable("product/{productId}") { entry ->
                val productId = entry.arguments?.getString("productId") ?: ""
                val vm: ProductDetailViewModel = viewModel(
                    viewModelStoreOwner = entry,
                    factory = ProductDetailViewModel.Factory(repo, productId),
                )
                ProductDetailScreen(navController, vm)
            }
        }
    }
}

@Composable
private fun BottomNav(navController: NavController, alertCount: Int) {
    val backStack by navController.currentBackStackEntryAsState()
    val current = backStack?.destination

    NavigationBar(
        modifier = Modifier.shadow(
            elevation = 24.dp,
            shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp),
            ambientColor = ClayPrimary.copy(alpha = 0.20f),
            spotColor = ClayPrimary.copy(alpha = 0.20f),
        ),
        containerColor = Color.White,
        tonalElevation = 0.dp,
    ) {
        bottomNavItems.forEach { screen ->
            val selected = current?.hierarchy?.any { it.route == screen.route } == true
            NavigationBarItem(
                selected = selected,
                onClick = {
                    navController.navigate(screen.route) {
                        popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                        launchSingleTop = true
                        restoreState = true
                    }
                },
                icon = {
                    if (screen == Screen.Alerts && alertCount > 0) {
                        BadgedBox(badge = {
                            Badge(containerColor = Color(0xFFEF4444)) {
                                Text(if (alertCount > 9) "9+" else alertCount.toString())
                            }
                        }) { Icon(screen.icon, screen.label) }
                    } else {
                        Icon(screen.icon, screen.label)
                    }
                },
                label = { Text(screen.label) },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = ClayPrimary,
                    selectedTextColor = ClayPrimary,
                    indicatorColor = ClayPrimary.copy(alpha = 0.12f),
                    unselectedIconColor = Color(0xFF94A3B8),
                    unselectedTextColor = Color(0xFF94A3B8),
                ),
            )
        }
    }
}
