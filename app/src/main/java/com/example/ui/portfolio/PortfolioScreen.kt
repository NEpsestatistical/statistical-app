package com.example.ui.portfolio

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.res.painterResource
import com.example.R
import com.example.data.model.PortfolioTransaction
import com.example.ui.theme.GainGreen
import com.example.ui.theme.LossRed
import com.example.viewmodel.HoldingItem
import com.example.viewmodel.MarketViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PortfolioScreen(
    viewModel: MarketViewModel,
    prefilledSymbol: String? = null
) {
    val isLoggedIn by viewModel.isLoggedIn.collectAsState()
    val holdings by viewModel.holdings.collectAsState()
    val summary by viewModel.portfolioSummary.collectAsState()
    val transactions by viewModel.transactions.collectAsState()

    var selectedTab by remember { mutableStateOf("Holdings") } // Holdings, Transactions, Analysis
    var showAddTxDialog by remember { mutableStateOf(false) }
    var editingTx by remember { mutableStateOf<PortfolioTransaction?>(null) }
    var showImportDialog by remember { mutableStateOf(false) }
    var csvInputText by remember { mutableStateOf("") }
    var importResultMsg by remember { mutableStateOf<String?>(null) }

    if (!isLoggedIn) {
        Box(modifier = Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Portfolio Tracking requires login", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Text("Log in to track holdings, calculate FIFO P/L, and import transactions.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                Button(onClick = { /* Go to profile/auth */ }) {
                    Text("Go to Login")
                }
            }
        }
        return
    }

    LaunchedEffect(prefilledSymbol) {
        if (!prefilledSymbol.isNullOrEmpty()) {
            showAddTxDialog = true
        }
    }

    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_statistical_logo),
                    contentDescription = "Statistical Portfolio Logo",
                    tint = androidx.compose.ui.graphics.Color.Unspecified,
                    modifier = Modifier.size(36.dp)
                )
                Text("Statistical Portfolio", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                IconButton(onClick = { showImportDialog = true }) {
                    Icon(Icons.Default.FileUpload, contentDescription = "Import CSV")
                }
                Button(onClick = { editingTx = null; showAddTxDialog = true }) {
                    Icon(Icons.Default.Add, contentDescription = "Add Trade")
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Trade")
                }
            }
        }

        // Summary Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Column {
                        Text("Total Market Value", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("NPR ${String.format(Locale.US, "%,.2f", summary.totalMarketValue)}", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text("Total Investment", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("NPR ${String.format(Locale.US, "%,.2f", summary.totalInvestment)}", style = MaterialTheme.typography.titleMedium, fontFamily = FontFamily.Monospace)
                    }
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    val unColor = if (summary.totalUnrealizedPl >= 0) GainGreen else LossRed
                    Column {
                        Text("Unrealized P/L", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(
                            "${if (summary.totalUnrealizedPl >= 0) "+" else ""}${String.format(Locale.US, "%,.2f", summary.totalUnrealizedPl)} (${String.format(Locale.US, "%.2f", summary.totalUnrealizedPlPercent)}%)",
                            color = unColor,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                    val relColor = if (summary.totalRealizedPl >= 0) GainGreen else LossRed
                    Column(horizontalAlignment = Alignment.End) {
                        Text("Realized P/L", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(
                            "${if (summary.totalRealizedPl >= 0) "+" else ""}${String.format(Locale.US, "%,.2f", summary.totalRealizedPl)}",
                            color = relColor,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }

        // Subtabs
        SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
            SegmentedButton(
                selected = selectedTab == "Holdings",
                onClick = { selectedTab = "Holdings" },
                shape = SegmentedButtonDefaults.itemShape(index = 0, count = 3)
            ) { Text("Holdings (${holdings.size})") }
            SegmentedButton(
                selected = selectedTab == "Transactions",
                onClick = { selectedTab = "Transactions" },
                shape = SegmentedButtonDefaults.itemShape(index = 1, count = 3)
            ) { Text("Transactions (${transactions.size})") }
            SegmentedButton(
                selected = selectedTab == "Allocation",
                onClick = { selectedTab = "Allocation" },
                shape = SegmentedButtonDefaults.itemShape(index = 2, count = 3)
            ) { Text("Sector Allocation") }
        }

        when (selectedTab) {
            "Holdings" -> {
                if (holdings.isEmpty()) {
                    Box(modifier = Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                        Text("No active holdings. Add a buy trade to start tracking.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                } else {
                    LazyColumn(modifier = Modifier.fillMaxWidth().weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(holdings) { h ->
                            HoldingCard(holding = h)
                        }
                    }
                }
            }
            "Transactions" -> {
                if (transactions.isEmpty()) {
                    Box(modifier = Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                        Text("No transactions logged yet.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                } else {
                    LazyColumn(modifier = Modifier.fillMaxWidth().weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(transactions, key = { it.id ?: it.createdAt ?: "" }) { tx ->
                            TransactionCard(
                                tx = tx,
                                onEdit = { editingTx = tx; showAddTxDialog = true },
                                onDelete = { tx.id?.let { id -> viewModel.deleteTransaction(id) { _, _ -> viewModel.recomputePortfolio() } } }
                            )
                        }
                    }
                }
            }
            "Allocation" -> {
                LazyColumn(modifier = Modifier.fillMaxWidth().weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(summary.sectorAllocation.entries.toList()) { (sector, valUsd) ->
                        val pct = if (summary.totalMarketValue > 0) (valUsd / summary.totalMarketValue) * 100 else 0.0
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(8.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                        ) {
                            Row(
                                modifier = Modifier.padding(16.dp).fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(sector, fontWeight = FontWeight.Bold)
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text("NPR ${String.format(Locale.US, "%,.2f", valUsd)}", style = MaterialTheme.typography.bodySmall, fontFamily = FontFamily.Monospace)
                                }
                                Text("${String.format(Locale.US, "%.1f", pct)}%", fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                            }
                        }
                    }
                }
            }
        }
    }

    if (showAddTxDialog) {
        AddEditTransactionDialog(
            viewModel = viewModel,
            editingTx = editingTx,
            initialSymbol = prefilledSymbol,
            onDismiss = { showAddTxDialog = false; editingTx = null }
        )
    }

    if (showImportDialog) {
        AlertDialog(
            onDismissRequest = { showImportDialog = false },
            title = { Text("Import Transactions (CSV)") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Paste CSV data (TMS export, Meroshare, or WACC format): symbol,side,qty,price")
                    OutlinedTextField(
                        value = csvInputText,
                        onValueChange = { csvInputText = it },
                        modifier = Modifier.fillMaxWidth().height(140.dp),
                        placeholder = { Text("NABIL,buy,100,850.5\nGBIME,buy,50,220.0") }
                    )
                    if (importResultMsg != null) {
                        Text(importResultMsg!!, color = MaterialTheme.colorScheme.primary)
                    }
                }
            },
            confirmButton = {
                Button(onClick = {
                    viewModel.importTransactionsFromCsv(csvInputText) { count, err ->
                        if (err == null) {
                            importResultMsg = "Successfully imported $count transactions!"
                            csvInputText = ""
                        } else {
                            importResultMsg = err
                        }
                    }
                }) {
                    Text("Import")
                }
            },
            dismissButton = {
                TextButton(onClick = { showImportDialog = false; importResultMsg = null }) {
                    Text("Close")
                }
            }
        )
    }
}

@Composable
fun HoldingCard(holding: HoldingItem) {
    val isPos = holding.unrealizedPlAmount >= 0
    val col = if (isPos) GainGreen else LossRed

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(holding.symbol, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace, fontSize = 16.sp)
                Text("NPR ${String.format(Locale.US, "%,.2f", holding.marketValue)}", fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
            }
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Qty: ${holding.qtyHeld} | Avg: ${String.format(Locale.US, "%.2f", holding.avgCost)}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, fontFamily = FontFamily.Monospace)
                Text("LTP: ${holding.currentLtp}", style = MaterialTheme.typography.bodySmall, fontFamily = FontFamily.Monospace)
            }
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Unrealized P/L:", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(
                    "${if (isPos) "+" else ""}${String.format(Locale.US, "%.2f", holding.unrealizedPlAmount)} (${String.format(Locale.US, "%.2f", holding.unrealizedPlPercent)}%)",
                    color = col,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 13.sp
                )
            }
        }
    }
}

@Composable
fun TransactionCard(tx: PortfolioTransaction, onEdit: () -> Unit, onDelete: () -> Unit) {
    val isBuy = tx.side.lowercase() == "buy"
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier.padding(16.dp).fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(tx.symbol, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                    Surface(
                        color = if (isBuy) GainGreen.copy(alpha = 0.2f) else LossRed.copy(alpha = 0.2f),
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        Text(
                            text = tx.side.uppercase(),
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            color = if (isBuy) GainGreen else LossRed,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text("Qty: ${tx.qty} @ NPR ${tx.price}", style = MaterialTheme.typography.bodySmall, fontFamily = FontFamily.Monospace, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(tx.tradeDate, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Row {
                IconButton(onClick = onEdit) {
                    Icon(Icons.Default.Edit, contentDescription = "Edit")
                }
                IconButton(onClick = onDelete) {
                    Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error)
                }
            }
        }
    }
}

@Composable
fun AddEditTransactionDialog(
    viewModel: MarketViewModel,
    editingTx: PortfolioTransaction?,
    initialSymbol: String? = null,
    onDismiss: () -> Unit
) {
    var symbol by remember { mutableStateOf(editingTx?.symbol ?: initialSymbol ?: "") }
    var side by remember { mutableStateOf(editingTx?.side ?: "buy") }
    var qty by remember { mutableStateOf(editingTx?.qty?.toString() ?: "") }
    var price by remember { mutableStateOf(editingTx?.price?.toString() ?: "") }
    var date by remember { mutableStateOf(editingTx?.tradeDate ?: SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())) }
    var note by remember { mutableStateOf(editingTx?.note ?: "") }
    var errorMsg by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (editingTx == null) "Log Trade" else "Edit Trade") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = symbol,
                    onValueChange = { symbol = it },
                    label = { Text("Symbol (e.g. NABIL)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(selected = side == "buy", onClick = { side = "buy" }, label = { Text("Buy") })
                    FilterChip(selected = side == "sell", onClick = { side = "sell" }, label = { Text("Sell") })
                }
                OutlinedTextField(
                    value = qty,
                    onValueChange = { qty = it },
                    label = { Text("Quantity") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = price,
                    onValueChange = { price = it },
                    label = { Text("Price (NPR)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = date,
                    onValueChange = { date = it },
                    label = { Text("Date (YYYY-MM-DD)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    label = { Text("Note (Optional)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                if (errorMsg != null) {
                    Text(errorMsg!!, color = MaterialTheme.colorScheme.error)
                }
            }
        },
        confirmButton = {
            Button(onClick = {
                val q = qty.toDoubleOrNull()
                val p = price.toDoubleOrNull()
                if (symbol.isBlank() || q == null || p == null) {
                    errorMsg = "Please fill symbol, valid quantity and price"
                    return@Button
                }
                val map = mapOf(
                    "symbol" to symbol.uppercase().trim(),
                    "side" to side,
                    "qty" to q,
                    "price" to p,
                    "trade_date" to date,
                    "note" to note.ifBlank { null }
                )
                if (editingTx == null) {
                    viewModel.addTransaction(map) { success, err ->
                        if (success) onDismiss() else errorMsg = err ?: "Failed"
                    }
                } else {
                    editingTx.id?.let { id ->
                        viewModel.updateTransaction(id, map) { success, err ->
                            if (success) onDismiss() else errorMsg = err ?: "Failed"
                        }
                    }
                }
            }) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
