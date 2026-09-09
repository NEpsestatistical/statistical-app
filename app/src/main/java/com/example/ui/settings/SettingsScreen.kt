package com.example.ui.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.viewmodel.MarketViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(viewModel: MarketViewModel) {
    val isLoggedIn by viewModel.isLoggedIn.collectAsState()
    val email by viewModel.currentUserEmail.collectAsState()
    val autoRefreshSec by viewModel.autoRefreshIntervalSeconds.collectAsState()
    val isDarkMode by viewModel.isDarkMode.collectAsState()
    val priceAlerts by viewModel.priceAlerts.collectAsState()

    var showAboutDialog by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("Terminal Settings", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)

        // Account Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Supabase Account", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                if (isLoggedIn) {
                    Text("Logged in as: ${email ?: "User"}", style = MaterialTheme.typography.bodyMedium, fontFamily = FontFamily.Monospace)
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(onClick = { viewModel.logout() }) {
                        Text("Log Out")
                    }
                } else {
                    Text("Not logged in. Guest mode active.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }

        // Preferences Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text("Preferences", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)

                // Dark mode toggle
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Dark Terminal Theme")
                    Switch(checked = isDarkMode, onCheckedChange = { viewModel.setDarkMode(it) })
                }

                // Auto-refresh interval
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Auto-Refresh Interval")
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf(30, 60, 0).forEach { sec ->
                            FilterChip(
                                selected = autoRefreshSec == sec,
                                onClick = { viewModel.setAutoRefreshInterval(sec) },
                                label = { Text(if (sec == 0) "Off" else "${sec}s") }
                            )
                        }
                    }
                }
            }
        }

        // Manage Price Alerts
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Active Price Alerts (${priceAlerts.size})", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                if (priceAlerts.isEmpty()) {
                    Text("No active price alerts set.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        priceAlerts.forEach { alert ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text("${alert.symbol} ${alert.direction} ${alert.targetPrice}", fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Medium)
                                    Text(if (alert.triggered == true) "Status: Triggered" else "Status: Active", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                IconButton(onClick = { alert.id?.let { viewModel.deleteAlert(it) } }) {
                                    Icon(Icons.Default.Delete, contentDescription = "Delete Alert", tint = MaterialTheme.colorScheme.error)
                                }
                            }
                        }
                    }
                }
            }
        }

        // About this data card
        Card(
            modifier = Modifier.fillMaxWidth().clickable { showAboutDialog = true },
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("About This Data", fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(4.dp))
                Text("Tap to read official data sources, index caveats, and alert limitations.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }

    if (showAboutDialog) {
        AlertDialog(
            onDismissRequest = { showAboutDialog = false },
            title = { Text("About Statistical Portfolio") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("• App Name: Statistical Portfolio")
                    Text("• Market Board Feed: GET https://shiny-term-f599.bharatiaashish43.workers.dev/all")
                    Text("• Price History Feed: GET https://nepsechart.bharatiaashish43.workers.dev/")
                    Text("• Index Caveat: NEPSE Index is official close-to-close change updating once per trading day close, labeled 'as of [date]'.")
                    Text("• Price Alerts Limitation: Price alerts only fire while the app is open and refreshing in this version, not as true background push notifications.")
                }
            },
            confirmButton = {
                TextButton(onClick = { showAboutDialog = false }) {
                    Text("Close")
                }
            }
        )
    }
}
