package com.example.ui.watchlist

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.BoardItem
import com.example.ui.components.PriceChangePill
import com.example.ui.components.Sparkline
import com.example.ui.theme.GainGreen
import com.example.ui.theme.LossRed
import com.example.viewmodel.MarketViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WatchlistScreen(
    viewModel: MarketViewModel,
    onNavigateSymbol: (String) -> Unit,
    onNavigateSearch: () -> Unit
) {
    val isLoggedIn by viewModel.isLoggedIn.collectAsState()
    val watchlistSymbols by viewModel.watchlistSymbols.collectAsState()
    val board by viewModel.boardItems.collectAsState()

    var addInput by remember { mutableStateOf("") }
    var duplicateMessage by remember { mutableStateOf<String?>(null) }

    if (!isLoggedIn) {
        Box(modifier = Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Watchlist requires login", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Text("Log in to sync your personalized watchlist across devices and receive live alerts.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                Button(onClick = { /* Switch to Auth / Profile tab */ }) {
                    Text("Go to Login")
                }
            }
        }
        return
    }

    val watchItems = board.filter { watchlistSymbols.contains(it.symbol.uppercase()) }

    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("Synced Watchlist", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)

        // Add symbol input
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = addInput,
                onValueChange = { addInput = it; duplicateMessage = null },
                label = { Text("Add symbol (e.g. NABIL)") },
                singleLine = true,
                modifier = Modifier.weight(1f)
            )
            Button(
                onClick = {
                    val sym = addInput.uppercase().trim()
                    if (sym.isNotBlank()) {
                        if (watchlistSymbols.contains(sym)) {
                            duplicateMessage = "$sym is already in watchlist"
                        } else {
                            viewModel.addToWatchlist(sym)
                            addInput = ""
                        }
                    }
                },
                modifier = Modifier.height(56.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add")
            }
        }

        if (duplicateMessage != null) {
            Text(duplicateMessage!!, color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.bodySmall)
        }

        if (watchItems.isEmpty()) {
            Box(modifier = Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Your watchlist is empty.", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Button(onClick = onNavigateSearch) {
                        Text("Explore Market & Add Symbols")
                    }
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxWidth().weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(watchItems, key = { it.symbol }) { item ->
                    val sparklinePoints = viewModel.marketRepo.getSymbolSparkline(item.symbol).map { it.second }
                    val isPos = (item.percentChange ?: 0.0) >= 0

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onNavigateSymbol(item.symbol) },
                        shape = RoundedCornerShape(8.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp).fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(item.symbol, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace, fontSize = 16.sp)
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(item.sector ?: "", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
                            }

                            if (sparklinePoints.size >= 2) {
                                Sparkline(
                                    dataPoints = sparklinePoints,
                                    modifier = Modifier.width(60.dp).height(24.dp),
                                    isPositive = isPos
                                )
                            }

                            Spacer(modifier = Modifier.width(16.dp))

                            Column(horizontalAlignment = Alignment.End) {
                                Text("NPR ${item.ltp ?: "—"}", fontFamily = FontFamily.Monospace, fontWeight = FontWeight.SemiBold)
                                Spacer(modifier = Modifier.height(4.dp))
                                PriceChangePill(percentChange = item.percentChange, changeAmount = item.changeAmount)
                            }

                            Spacer(modifier = Modifier.width(8.dp))

                            IconButton(onClick = { viewModel.removeFromWatchlist(item.symbol) }) {
                                Icon(Icons.Default.Delete, contentDescription = "Remove", tint = MaterialTheme.colorScheme.error)
                            }
                        }
                    }
                }
            }
        }
    }
}
