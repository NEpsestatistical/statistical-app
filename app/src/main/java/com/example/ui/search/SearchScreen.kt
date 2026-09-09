package com.example.ui.search

import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Sort
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
import com.example.viewmodel.MarketViewModel
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.collectLatest
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen(
    viewModel: MarketViewModel,
    initialSectorFilter: String? = null,
    onNavigateSymbol: (String) -> Unit
) {
    val board by viewModel.boardItems.collectAsState()
    val recentlyViewed by viewModel.recentlyViewed.collectAsState()

    var searchQuery by remember { mutableStateOf("") }
    var selectedSector by remember { mutableStateOf<String?>(initialSectorFilter) }
    var sortBy by remember { mutableStateOf("symbol") } // symbol, ltp, change
    var sortAscending by remember { mutableStateOf(true) }

    LaunchedEffect(initialSectorFilter) {
        if (initialSectorFilter != null) {
            selectedSector = initialSectorFilter
        }
    }

    val sectors = remember(board) {
        board.mapNotNull { it.sector }.distinct().sorted()
    }

    val filteredBoard = remember(board, searchQuery, selectedSector, sortBy, sortAscending) {
        var list = board.filter { item ->
            val matchQuery = searchQuery.isBlank() ||
                    item.symbol.contains(searchQuery, ignoreCase = true) ||
                    (item.sector?.contains(searchQuery, ignoreCase = true) == true)
            val matchSector = selectedSector == null || item.sector.equals(selectedSector, ignoreCase = true)
            matchQuery && matchSector
        }

        list = when (sortBy) {
            "symbol" -> if (sortAscending) list.sortedBy { it.symbol } else list.sortedByDescending { it.symbol }
            "ltp" -> if (sortAscending) list.sortedBy { it.ltp ?: 0.0 } else list.sortedByDescending { it.ltp ?: 0.0 }
            "change" -> if (sortAscending) list.sortedBy { it.percentChange ?: 0.0 } else list.sortedByDescending { it.percentChange ?: 0.0 }
            else -> list
        }
        list
    }

    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("Market Explore & Search", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)

        // Search Input
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            label = { Text("Search symbols or sectors (e.g. NABIL, Banks)") },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        // Recently Viewed Row
        if (recentlyViewed.isNotEmpty()) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("Recently Viewed", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(recentlyViewed) { sym ->
                        Surface(
                            modifier = Modifier.clickable { onNavigateSymbol(sym) },
                            shape = RoundedCornerShape(16.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant
                        ) {
                            Text(
                                text = sym,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp
                            )
                        }
                    }
                }
            }
        }

        // Sector Filter Chips
        Row(
            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FilterChip(
                selected = selectedSector == null,
                onClick = { selectedSector = null },
                label = { Text("All Sectors") }
            )
            sectors.forEach { sec ->
                FilterChip(
                    selected = selectedSector == sec,
                    onClick = { selectedSector = if (selectedSector == sec) null else sec },
                    label = { Text(sec) }
                )
            }
        }

        // Sort Header Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextButton(onClick = {
                if (sortBy == "symbol") sortAscending = !sortAscending
                else { sortBy = "symbol"; sortAscending = true }
            }) {
                Text("Symbol ${if (sortBy == "symbol") (if (sortAscending) "▲" else "▼") else ""}")
            }
            TextButton(onClick = {
                if (sortBy == "ltp") sortAscending = !sortAscending
                else { sortBy = "ltp"; sortAscending = false }
            }) {
                Text("LTP ${if (sortBy == "ltp") (if (sortAscending) "▲" else "▼") else ""}")
            }
            TextButton(onClick = {
                if (sortBy == "change") sortAscending = !sortAscending
                else { sortBy = "change"; sortAscending = false }
            }) {
                Text("% Chg ${if (sortBy == "change") (if (sortAscending) "▲" else "▼") else ""}")
            }
        }

        // Market Table List
        LazyColumn(
            modifier = Modifier.fillMaxWidth().weight(1f),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(filteredBoard, key = { it.symbol }) { item ->
                Card(
                    modifier = Modifier.fillMaxWidth().clickable { onNavigateSymbol(item.symbol) },
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
                        Column(horizontalAlignment = Alignment.End) {
                            Text("NPR ${item.ltp ?: "—"}", fontFamily = FontFamily.Monospace, fontWeight = FontWeight.SemiBold)
                            Spacer(modifier = Modifier.height(4.dp))
                            PriceChangePill(percentChange = item.percentChange, changeAmount = item.changeAmount)
                        }
                    }
                }
            }
        }
    }
}
