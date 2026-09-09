package com.example.ui.symbol

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Candle
import com.example.ui.components.PriceChangePill
import com.example.ui.theme.GainGreen
import com.example.ui.theme.LossRed
import com.example.viewmodel.MarketViewModel
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SymbolDetailScreen(
    symbol: String,
    viewModel: MarketViewModel,
    onBack: () -> Unit,
    onOpenLogTrade: (String) -> Unit
) {
    val board by viewModel.boardItems.collectAsState()
    val boardItem = board.find { it.symbol.equals(symbol, ignoreCase = true) }
    val watchlistSymbols by viewModel.watchlistSymbols.collectAsState()
    val isWatched = watchlistSymbols.contains(symbol.uppercase())

    var candles by remember { mutableStateOf<List<Candle>>(emptyList()) }
    var isLoadingChart by remember { mutableStateOf(true) }
    var chartError by remember { mutableStateOf<String?>(null) }
    var chartType by remember { mutableStateOf("Line") } // Line or Candlestick
    var rangeTab by remember { mutableStateOf("1Y") } // 1W, 1M, 1Y, All

    var showAlertDialog by remember { mutableStateOf(false) }
    var targetPriceInput by remember { mutableStateOf(boardItem?.ltp?.toString() ?: "") }
    var alertDirection by remember { mutableStateOf("above") }

    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(symbol) {
        viewModel.addRecentlyViewed(symbol)
        isLoadingChart = true
        val res = viewModel.marketRepo.getChartData(symbol)
        res.fold(
            onSuccess = {
                candles = it
                isLoadingChart = false
            },
            onFailure = {
                chartError = it.message
                isLoadingChart = false
            }
        )
    }

    // Filter candles by range
    val filteredCandles = remember(candles, rangeTab) {
        if (candles.isEmpty()) return@remember emptyList()
        val latestTime = candles.maxOfOrNull { it.time } ?: System.currentTimeMillis() / 1000
        val cutoff = when (rangeTab) {
            "1W" -> latestTime - 7 * 24 * 3600L
            "1M" -> latestTime - 30 * 24 * 3600L
            "1Y" -> latestTime - 365 * 24 * 3600L
            else -> 0L
        }
        candles.filter { it.time >= cutoff }.sortedBy { it.time }
    }

    // Range % return
    val rangeReturn = remember(filteredCandles) {
        if (filteredCandles.size >= 2) {
            val firstClose = filteredCandles.first().close
            val lastClose = filteredCandles.last().close
            if (firstClose > 0) ((lastClose - firstClose) / firstClose) * 100 else 0.0
        } else 0.0
    }

    var touchIndex by remember { mutableStateOf<Int?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(symbol.uppercase(), fontFamily = FontFamily.Monospace) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = {
                        if (isWatched) viewModel.removeFromWatchlist(symbol)
                        else viewModel.addToWatchlist(symbol)
                    }) {
                        Icon(
                            imageVector = if (isWatched) Icons.Filled.Star else Icons.Outlined.StarBorder,
                            contentDescription = "Watchlist Star",
                            tint = if (isWatched) MaterialTheme.colorScheme.primary else LocalContentColor.current
                        )
                    }
                    IconButton(onClick = { showAlertDialog = true }) {
                        Icon(Icons.Default.Notifications, contentDescription = "Set Price Alert")
                    }
                }
            )
        }
    ) { padding ->
        val scrollState = rememberScrollState()

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(scrollState)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header Info Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(text = boardItem?.sector ?: "Sector Unavailable", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "NPR ${boardItem?.ltp ?: "—"}",
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                        PriceChangePill(percentChange = boardItem?.percentChange, changeAmount = boardItem?.changeAmount)
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text("Day Qty", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(boardItem?.qty?.let { String.format(Locale.US, "%,.0f", it) } ?: "—", fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Medium)
                        }
                        Column {
                            Text("Range Return ($rangeTab)", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            val retCol = if (rangeReturn >= 0) GainGreen else LossRed
                            Text(
                                "${if (rangeReturn >= 0) "+" else ""}${String.format(Locale.US, "%.2f", rangeReturn)}%",
                                color = retCol,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            // Chart Section
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Range Tabs
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            listOf("1W", "1M", "1Y", "All").forEach { range ->
                                FilterChip(
                                    selected = rangeTab == range,
                                    onClick = { rangeTab = range },
                                    label = { Text(range, fontSize = 12.sp) }
                                )
                            }
                        }
                        // Type Toggle
                        SingleChoiceSegmentedButtonRow {
                            SegmentedButton(
                                selected = chartType == "Line",
                                onClick = { chartType = "Line" },
                                shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2)
                            ) { Text("Line", fontSize = 12.sp) }
                            SegmentedButton(
                                selected = chartType == "Candle",
                                onClick = { chartType = "Candle" },
                                shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2)
                            ) { Text("Candle", fontSize = 12.sp) }
                        }
                    }

                    if (isLoadingChart) {
                        Box(modifier = Modifier.fillMaxWidth().height(220.dp), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator()
                        }
                    } else if (chartError != null) {
                        Box(modifier = Modifier.fillMaxWidth().height(220.dp), contentAlignment = Alignment.Center) {
                            Text("Failed to load chart: $chartError", color = MaterialTheme.colorScheme.error)
                        }
                    } else if (filteredCandles.isEmpty()) {
                        Box(modifier = Modifier.fillMaxWidth().height(220.dp), contentAlignment = Alignment.Center) {
                            Text("No history data available for selected range.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    } else {
                        // Honesty rule message if limited history
                        if (candles.size < 30 && rangeTab == "1Y") {
                            Text(
                                text = "Note: Only ${candles.size} trading days of history available for this symbol.",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.secondary
                            )
                        }

                        // Touch scrub info box
                        val activeCandle = touchIndex?.let { filteredCandles.getOrNull(it) } ?: filteredCandles.lastOrNull()
                        if (activeCandle != null) {
                            val dateStr = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date(activeCandle.time * 1000L))
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Date: $dateStr", style = MaterialTheme.typography.labelSmall, fontFamily = FontFamily.Monospace)
                                Text("O:${activeCandle.open} H:${activeCandle.high} L:${activeCandle.low} C:${activeCandle.close}", style = MaterialTheme.typography.labelSmall, fontFamily = FontFamily.Monospace)
                            }
                        }

                        // Canvas Chart
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(220.dp)
                                .pointerInput(filteredCandles) {
                                    detectDragGestures(
                                        onDragStart = { offset ->
                                            val idx = (offset.x / size.width * filteredCandles.size).toInt().coerceIn(0, filteredCandles.size - 1)
                                            touchIndex = idx
                                        },
                                        onDrag = { change, _ ->
                                            val idx = (change.position.x / size.width * filteredCandles.size).toInt().coerceIn(0, filteredCandles.size - 1)
                                            touchIndex = idx
                                        },
                                        onDragEnd = { touchIndex = null }
                                    )
                                }
                        ) {
                            val isPos = rangeReturn >= 0
                            val col = if (isPos) GainGreen else LossRed

                            Canvas(modifier = Modifier.fillMaxSize()) {
                                val minP = filteredCandles.minOfOrNull { it.low } ?: 0.0
                                val maxP = filteredCandles.maxOfOrNull { it.high } ?: 1.0
                                val pRange = if (maxP - minP == 0.0) 1.0 else maxP - minP
                                val w = size.width
                                val h = size.height

                                if (chartType == "Line") {
                                    val path = Path()
                                    filteredCandles.forEachIndexed { index, c ->
                                        val x = index.toFloat() / (filteredCandles.size.coerceAtLeast(2) - 1) * w
                                        val y = h - ((c.close - minP) / pRange).toFloat() * h
                                        if (index == 0) path.moveTo(x, y) else path.lineTo(x, y)
                                    }
                                    drawPath(path, col, style = Stroke(width = 2.5.dp.toPx()))
                                } else {
                                    // Candlestick
                                    val candleWidth = (w / filteredCandles.size).coerceAtLeast(2f) * 0.7f
                                    filteredCandles.forEachIndexed { index, c ->
                                        val x = index.toFloat() / (filteredCandles.size.coerceAtLeast(2) - 1) * w
                                        val yOpen = h - ((c.open - minP) / pRange).toFloat() * h
                                        val yClose = h - ((c.close - minP) / pRange).toFloat() * h
                                        val yHigh = h - ((c.high - minP) / pRange).toFloat() * h
                                        val yLow = h - ((c.low - minP) / pRange).toFloat() * h

                                        val isBull = c.close >= c.open
                                        val cColor = if (isBull) GainGreen else LossRed

                                        // Wick
                                        drawLine(cColor, Offset(x, yHigh), Offset(x, yLow), strokeWidth = 1.dp.toPx())
                                        // Body
                                        val topBody = minOf(yOpen, yClose)
                                        val bottomBody = maxOf(yOpen, yClose).let { if (it == topBody) it + 1f else it }
                                        drawRect(
                                            color = cColor,
                                            topLeft = Offset(x - candleWidth / 2, topBody),
                                            size = androidx.compose.ui.geometry.Size(candleWidth, bottomBody - topBody)
                                        )
                                    }
                                }

                                // Touch Indicator Line
                                touchIndex?.let { idx ->
                                    val x = idx.toFloat() / (filteredCandles.size.coerceAtLeast(2) - 1) * w
                                    drawLine(Color.White, Offset(x, 0f), Offset(x, h), strokeWidth = 1.dp.toPx())
                                }
                            }
                        }
                    }
                }
            }

            // Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Button(
                    onClick = { onOpenLogTrade(symbol) },
                    modifier = Modifier.weight(1f).height(48.dp),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("Log a Trade")
                }
                OutlinedButton(
                    onClick = { showAlertDialog = true },
                    modifier = Modifier.weight(1f).height(48.dp),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("Set Price Alert")
                }
            }
        }
    }

    if (showAlertDialog) {
        AlertDialog(
            onDismissRequest = { showAlertDialog = false },
            title = { Text("Set Price Alert for ${symbol.uppercase()}") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = targetPriceInput,
                        onValueChange = { targetPriceInput = it },
                        label = { Text("Target Price (NPR)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(selected = alertDirection == "above", onClick = { alertDirection = "above" }, label = { Text("Price Above") })
                        FilterChip(selected = alertDirection == "below", onClick = { alertDirection = "below" }, label = { Text("Price Below") })
                    }
                }
            },
            confirmButton = {
                Button(onClick = {
                    val price = targetPriceInput.toDoubleOrNull()
                    if (price != null) {
                        viewModel.addAlert(symbol, price, alertDirection) { success, err ->
                            showAlertDialog = false
                        }
                    }
                }) {
                    Text("Save Alert")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAlertDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}
