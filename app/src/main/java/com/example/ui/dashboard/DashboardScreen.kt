package com.example.ui.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.res.painterResource
import com.example.R
import com.example.data.model.BoardItem
import com.example.ui.components.PriceChangePill
import com.example.ui.components.Sparkline
import com.example.ui.theme.GainGreen
import com.example.ui.theme.LossRed
import com.example.viewmodel.MarketViewModel
import kotlinx.coroutines.delay
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    viewModel: MarketViewModel,
    onNavigateSymbol: (String) -> Unit,
    onNavigateWatchlist: () -> Unit,
    onNavigateSearchWithSector: (String) -> Unit
) {
    val board by viewModel.boardItems.collectAsState()
    val index by viewModel.marketIndex.collectAsState()
    val isLoading by viewModel.isLoadingMarket.collectAsState()
    val error by viewModel.marketError.collectAsState()
    val lastUpdated by viewModel.lastUpdatedTime.collectAsState()
    val isLoggedIn by viewModel.isLoggedIn.collectAsState()
    val watchlistSymbols by viewModel.watchlistSymbols.collectAsState()

    var timeAgoSeconds by remember { mutableStateOf(0L) }
    LaunchedEffect(lastUpdated) {
        while (true) {
            timeAgoSeconds = (System.currentTimeMillis() - lastUpdated) / 1000
            delay(1000L)
        }
    }

    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Header & Last Updated
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_statistical_logo),
                    contentDescription = "Statistical Portfolio Logo",
                    tint = Color.Unspecified,
                    modifier = Modifier.size(36.dp)
                )
                Column {
                    Text(
                        text = "NEPSE Terminal",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Last updated ${timeAgoSeconds}s ago",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }
            IconButton(onClick = { viewModel.refreshMarketData(force = true) }) {
                Icon(Icons.Default.Refresh, contentDescription = "Refresh Market Data")
            }
        }

        if (error != null) {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
            ) {
                Row(
                    modifier = Modifier.padding(12.dp).fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(text = error!!, color = MaterialTheme.colorScheme.onErrorContainer)
                    TextButton(onClick = { viewModel.refreshMarketData(force = true) }) {
                        Text("Retry", color = MaterialTheme.colorScheme.onErrorContainer)
                    }
                }
            }
        }

        // Index Card
        index?.let { idx ->
            val isPos = idx.pointChange >= 0
            val idxColor = if (isPos) GainGreen else LossRed
            val sign = if (isPos) "+" else ""
            val indexSnapshots = viewModel.marketRepo.getIndexSnapshots().map { it.second }

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Top
                    ) {
                        Column {
                            Text(text = "NEPSE INDEX", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = String.format(Locale.US, "%.2f", idx.value),
                                style = MaterialTheme.typography.headlineLarge,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = "$sign${String.format(Locale.US, "%.2f", idx.pointChange)} ($sign${String.format(Locale.US, "%.2f", idx.percentChange)}%)",
                                color = idxColor,
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.SemiBold,
                                fontFamily = FontFamily.Monospace
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "as of ${idx.date}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    if (indexSnapshots.size >= 2) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Sparkline(
                            dataPoints = indexSnapshots,
                            modifier = Modifier.fillMaxWidth().height(40.dp),
                            isPositive = isPos
                        )
                    }
                }
            }
        }

        // Market Pulse Breadth Bar
        if (board.isNotEmpty()) {
            val gainers = board.count { (it.percentChange ?: 0.0) > 0 }
            val losers = board.count { (it.percentChange ?: 0.0) < 0 }
            val neutral = board.size - gainers - losers
            val gPct = if (board.isNotEmpty()) gainers.toFloat() / board.size else 0f
            val lPct = if (board.isNotEmpty()) losers.toFloat() / board.size else 0f

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Market Breadth", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                        Text("Gainers: $gainers | Losers: $losers | Neutral: $neutral", style = MaterialTheme.typography.bodySmall, fontFamily = FontFamily.Monospace)
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth().height(8.dp),
                        horizontalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        if (gainers > 0) Box(modifier = Modifier.weight(gPct.coerceAtLeast(0.01f)).fillMaxHeight().background(GainGreen, RoundedCornerShape(4.dp)))
                        if (neutral > 0) Box(modifier = Modifier.weight(neutral.toFloat().coerceAtLeast(1f) / board.size).fillMaxHeight().background(Color.Gray, RoundedCornerShape(4.dp)))
                        if (losers > 0) Box(modifier = Modifier.weight(lPct.coerceAtLeast(0.01f)).fillMaxHeight().background(LossRed, RoundedCornerShape(4.dp)))
                    }
                }
            }
        }

        // Watchlist at a glance mini-row (if logged in & non-empty)
        if (isLoggedIn && watchlistSymbols.isNotEmpty()) {
            val watchItems = board.filter { watchlistSymbols.contains(it.symbol.uppercase()) }.take(4)
            if (watchItems.isNotEmpty()) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Your Watchlist at a Glance", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                        TextButton(onClick = onNavigateWatchlist) {
                            Text("View All")
                        }
                    }
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(watchItems) { item ->
                            Card(
                                modifier = Modifier.width(150.dp).clickable { onNavigateSymbol(item.symbol) },
                                shape = RoundedCornerShape(8.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Text(item.symbol, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text("NPR ${item.ltp ?: "—"}", fontFamily = FontFamily.Monospace, fontSize = 14.sp)
                                    Spacer(modifier = Modifier.height(4.dp))
                                    PriceChangePill(percentChange = item.percentChange, changeAmount = item.changeAmount)
                                }
                            }
                        }
                    }
                }
            }
        }

        // Top Gainers / Top Losers
        if (board.isNotEmpty()) {
            val sortedByGain = board.sortedByDescending { it.percentChange ?: 0.0 }
            val topGainers = sortedByGain.take(5)
            val topLosers = sortedByGain.reversed().take(5)

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Top Gainers", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(topGainers) { item ->
                        MiniSymbolCard(item = item, onClick = { onNavigateSymbol(item.symbol) })
                    }
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Top Losers", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(topLosers) { item ->
                        MiniSymbolCard(item = item, onClick = { onNavigateSymbol(item.symbol) })
                    }
                }
            }
        }

        // Sector Snapshot
        if (board.isNotEmpty()) {
            val sectorMap = board.groupBy { it.sector ?: "Other" }
            val sectorAverages = sectorMap.map { (sector, items) ->
                val avgChange = items.mapNotNull { it.percentChange }.average().let { if (it.isNaN()) 0.0 else it }
                Pair(sector, avgChange)
            }.sortedByDescending { it.second }

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Sector Performance", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    sectorAverages.forEach { (sector, avgChange) ->
                        val isPos = avgChange >= 0
                        val col = if (isPos) GainGreen else LossRed
                        Card(
                            modifier = Modifier.fillMaxWidth().clickable { onNavigateSearchWithSector(sector) },
                            shape = RoundedCornerShape(8.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp).fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(sector, fontWeight = FontWeight.Medium)
                                Text(
                                    text = "${if (isPos) "+" else ""}${String.format(Locale.US, "%.2f", avgChange)}%",
                                    color = col,
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun MiniSymbolCard(item: BoardItem, onClick: () -> Unit) {
    Card(
        modifier = Modifier.width(140.dp).clickable(onClick = onClick),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(item.symbol, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
            Spacer(modifier = Modifier.height(2.dp))
            Text(item.sector ?: "", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
            Spacer(modifier = Modifier.height(6.dp))
            Text("NPR ${item.ltp ?: "—"}", fontFamily = FontFamily.Monospace, fontSize = 13.sp)
            Spacer(modifier = Modifier.height(4.dp))
            PriceChangePill(percentChange = item.percentChange, changeAmount = item.changeAmount)
        }
    }
}
