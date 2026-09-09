package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import com.example.ui.auth.AuthScreen
import com.example.ui.dashboard.DashboardScreen
import com.example.ui.portfolio.PortfolioScreen
import com.example.ui.search.SearchScreen
import com.example.ui.settings.SettingsScreen
import com.example.ui.symbol.SymbolDetailScreen
import com.example.ui.theme.StatisticalPortfolioTheme
import com.example.ui.watchlist.WatchlistScreen
import com.example.viewmodel.MarketViewModel

sealed class Screen(val route: String, val title: String, val icon: ImageVector) {
    object Dashboard : Screen("dashboard", "Dashboard", Icons.Default.Home)
    object Search : Screen("search", "Explore", Icons.Default.Search)
    object Watchlist : Screen("watchlist", "Watchlist", Icons.Default.Star)
    object Portfolio : Screen("portfolio", "Portfolio", Icons.Default.BarChart)
    object Settings : Screen("settings", "Settings", Icons.Default.Settings)
}

class MainActivity : ComponentActivity() {
    private val viewModel: MarketViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        handleIntent(intent)
        setContent {
            val isDarkMode by viewModel.isDarkMode.collectAsState()
            val isLoggedIn by viewModel.isLoggedIn.collectAsState()
            val triggeredAlerts by viewModel.triggeredAlerts.collectAsState()

            var currentTab by remember { mutableStateOf<String>("dashboard") }
            var detailSymbol by remember { mutableStateOf<String?>(null) }
            var sectorFilter by remember { mutableStateOf<String?>(null) }
            var prefilledTradeSymbol by remember { mutableStateOf<String?>(null) }
            var isGuest by remember { mutableStateOf(false) }

            // Triggered alert snackbar / dialog
            val snackbarHostState = remember { SnackbarHostState() }
            LaunchedEffect(triggeredAlerts) {
                if (triggeredAlerts.isNotEmpty()) {
                    val alertMsg = triggeredAlerts.first()
                    snackbarHostState.showSnackbar(alertMsg)
                    viewModel.dismissTriggeredAlert(alertMsg)
                }
            }

            StatisticalPortfolioTheme(darkTheme = isDarkMode) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    if (!isLoggedIn && !isGuest && detailSymbol == null) {
                        AuthScreen(
                            viewModel = viewModel,
                            onGuestContinue = { isGuest = true }
                        )
                    } else if (detailSymbol != null) {
                        SymbolDetailScreen(
                            symbol = detailSymbol!!,
                            viewModel = viewModel,
                            onBack = { detailSymbol = null },
                            onOpenLogTrade = { sym ->
                                detailSymbol = null
                                prefilledTradeSymbol = sym
                                currentTab = "portfolio"
                            }
                        )
                    } else {
                        Scaffold(
                            snackbarHost = { SnackbarHost(snackbarHostState) },
                            bottomBar = {
                                NavigationBar {
                                    val items = listOf(
                                        Screen.Dashboard,
                                        Screen.Search,
                                        Screen.Watchlist,
                                        Screen.Portfolio,
                                        Screen.Settings
                                    )
                                    items.forEach { screen ->
                                        NavigationBarItem(
                                            icon = { Icon(screen.icon, contentDescription = screen.title) },
                                            label = { Text(screen.title) },
                                            selected = currentTab == screen.route,
                                            onClick = {
                                                sectorFilter = null
                                                prefilledTradeSymbol = null
                                                currentTab = screen.route
                                            }
                                        )
                                    }
                                }
                            }
                        ) { innerPadding ->
                            Box(modifier = Modifier.padding(innerPadding)) {
                                when (currentTab) {
                                    "dashboard" -> DashboardScreen(
                                        viewModel = viewModel,
                                        onNavigateSymbol = { detailSymbol = it },
                                        onNavigateWatchlist = { currentTab = "watchlist" },
                                        onNavigateSearchWithSector = { sec ->
                                            sectorFilter = sec
                                            currentTab = "search"
                                        }
                                    )
                                    "search" -> SearchScreen(
                                        viewModel = viewModel,
                                        initialSectorFilter = sectorFilter,
                                        onNavigateSymbol = { detailSymbol = it }
                                    )
                                    "watchlist" -> WatchlistScreen(
                                        viewModel = viewModel,
                                        onNavigateSymbol = { detailSymbol = it },
                                        onNavigateSearch = { currentTab = "search" }
                                    )
                                    "portfolio" -> PortfolioScreen(
                                        viewModel = viewModel,
                                        prefilledSymbol = prefilledTradeSymbol
                                    )
                                    "settings" -> SettingsScreen(viewModel = viewModel)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    override fun onNewIntent(intent: android.content.Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(intent: android.content.Intent) {
        val uri = intent.data
        if (uri != null) {
            val fragment = uri.fragment
            val query = uri.query
            val token = extractParam(fragment ?: query, "access_token")
            if (!token.isNullOrEmpty()) {
                viewModel.handleOAuthCallback(token) { _, _ -> }
            }
        }
    }

    private fun extractParam(source: String?, param: String): String? {
        if (source == null) return null
        val pairs = source.split("&")
        for (pair in pairs) {
            val kv = pair.split("=")
            if (kv.size == 2 && kv[0] == param) {
                return try {
                    java.net.URLDecoder.decode(kv[1], "UTF-8")
                } catch (e: Exception) {
                    kv[1]
                }
            }
        }
        return null
    }
}
