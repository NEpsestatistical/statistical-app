package com.example.viewmodel

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.BoardItem
import com.example.data.model.Candle
import com.example.data.model.MarketIndex
import com.example.data.model.PortfolioJsonObj
import com.example.data.model.PortfolioTransaction
import com.example.data.model.PriceAlert
import com.example.data.model.WatchlistItem
import com.example.data.repository.MarketRepository
import com.example.data.repository.SupabaseRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Locale

data class HoldingItem(
    val symbol: String,
    val sector: String,
    val qtyHeld: Double,
    val avgCost: Double,
    val currentLtp: Double,
    val marketValue: Double,
    val totalInvestment: Double,
    val unrealizedPlAmount: Double,
    val unrealizedPlPercent: Double,
    val realizedPl: Double
)

data class PortfolioSummary(
    val totalMarketValue: Double = 0.0,
    val totalInvestment: Double = 0.0,
    val totalUnrealizedPl: Double = 0.0,
    val totalUnrealizedPlPercent: Double = 0.0,
    val totalRealizedPl: Double = 0.0,
    val sectorAllocation: Map<String, Double> = emptyMap() // sector -> market value
)

class MarketViewModel(application: Application) : AndroidViewModel(application) {
    val marketRepo = MarketRepository()
    val supabaseRepo = SupabaseRepository(application)

    private val _boardItems = MutableStateFlow<List<BoardItem>>(emptyList())
    val boardItems: StateFlow<List<BoardItem>> = _boardItems.asStateFlow()

    private val _marketIndex = MutableStateFlow<MarketIndex?>(null)
    val marketIndex: StateFlow<MarketIndex?> = _marketIndex.asStateFlow()

    private val _isLoadingMarket = MutableStateFlow(false)
    val isLoadingMarket: StateFlow<Boolean> = _isLoadingMarket.asStateFlow()

    private val _marketError = MutableStateFlow<String?>(null)
    val marketError: StateFlow<String?> = _marketError.asStateFlow()

    private val _lastUpdatedTime = MutableStateFlow(System.currentTimeMillis())
    val lastUpdatedTime: StateFlow<Long> = _lastUpdatedTime.asStateFlow()

    // Auth state
    private val _isLoggedIn = MutableStateFlow(supabaseRepo.isLoggedIn())
    val isLoggedIn: StateFlow<Boolean> = _isLoggedIn.asStateFlow()

    private val _currentUserEmail = MutableStateFlow(supabaseRepo.currentUserEmail)
    val currentUserEmail: StateFlow<String?> = _currentUserEmail.asStateFlow()

    private val _authError = MutableStateFlow<String?>(null)
    val authError: StateFlow<String?> = _authError.asStateFlow()

    // Watchlist
    private val _watchlistSymbols = MutableStateFlow<Set<String>>(emptySet())
    val watchlistSymbols: StateFlow<Set<String>> = _watchlistSymbols.asStateFlow()

    // Portfolio
    private val _transactions = MutableStateFlow<List<PortfolioTransaction>>(emptyList())
    val transactions: StateFlow<List<PortfolioTransaction>> = _transactions.asStateFlow()

    val holdings = MutableStateFlow<List<HoldingItem>>(emptyList())
    val portfolioSummary = MutableStateFlow(PortfolioSummary())

    // Price Alerts
    private val _priceAlerts = MutableStateFlow<List<PriceAlert>>(emptyList())
    val priceAlerts: StateFlow<List<PriceAlert>> = _priceAlerts.asStateFlow()

    val triggeredAlerts = MutableStateFlow<List<String>>(emptyList()) // messages for triggered alerts

    // Settings
    private val prefs = application.getSharedPreferences("statistical_portfolio_prefs", android.content.Context.MODE_PRIVATE)
    private val _autoRefreshIntervalSeconds = MutableStateFlow(prefs.getInt("auto_refresh_sec", 60))
    val autoRefreshIntervalSeconds: StateFlow<Int> = _autoRefreshIntervalSeconds.asStateFlow()

    private val _isDarkMode = MutableStateFlow(prefs.getBoolean("dark_mode", true))
    val isDarkMode: StateFlow<Boolean> = _isDarkMode.asStateFlow()

    private val _showWalkthrough = MutableStateFlow(prefs.getBoolean("show_walkthrough", false))
    val showWalkthrough: StateFlow<Boolean> = _showWalkthrough.asStateFlow()

    // Recently viewed symbols (session local)
    private val _recentlyViewed = MutableStateFlow<List<String>>(emptyList())
    val recentlyViewed: StateFlow<List<String>> = _recentlyViewed.asStateFlow()

    private var autoRefreshJob: Job? = null
    private var allPortfolios: MutableList<PortfolioJsonObj> = mutableListOf()
    private var currentPortfolioId: String? = null
    private var saveJob: Job? = null

    init {
        refreshMarketData(force = true)
        if (supabaseRepo.isLoggedIn()) {
            fetchUserDataParallel()
        }
        startAutoRefreshTimer()
    }

    fun refreshMarketData(force: Boolean = false) {
        viewModelScope.launch {
            _isLoadingMarket.value = true
            _marketError.value = null
            val result = marketRepo.fetchBoardData(force)
            result.fold(
                onSuccess = { (board, index) ->
                    _boardItems.value = board
                    _marketIndex.value = index
                    _lastUpdatedTime.value = System.currentTimeMillis()
                    _isLoadingMarket.value = false
                    checkPriceAlerts(board)
                    recomputePortfolio()
                },
                onFailure = { err ->
                    _marketError.value = err.message ?: "Failed to load market data"
                    _isLoadingMarket.value = false
                }
            )
        }
    }

    private fun startAutoRefreshTimer() {
        autoRefreshJob?.cancel()
        val interval = _autoRefreshIntervalSeconds.value
        if (interval <= 0) return
        autoRefreshJob = viewModelScope.launch {
            while (true) {
                delay(interval * 1000L)
                refreshMarketData(force = true)
            }
        }
    }

    fun setAutoRefreshInterval(seconds: Int) {
        _autoRefreshIntervalSeconds.value = seconds
        prefs.edit().putInt("auto_refresh_sec", seconds).apply()
        startAutoRefreshTimer()
    }

    fun setDarkMode(dark: Boolean) {
        _isDarkMode.value = dark
        prefs.edit().putBoolean("dark_mode", dark).apply()
    }

    fun dismissWalkthrough() {
        _showWalkthrough.value = false
        prefs.edit().putBoolean("show_walkthrough", false).apply()
    }

    // Auth methods
    fun login(email: String, pass: String, onResult: (Boolean, String?) -> Unit) {
        viewModelScope.launch {
            _authError.value = null
            val res = supabaseRepo.login(email, pass)
            res.fold(
                onSuccess = {
                    _isLoggedIn.value = true
                    _currentUserEmail.value = supabaseRepo.currentUserEmail
                    fetchUserDataParallel()
                    onResult(true, null)
                },
                onFailure = { err ->
                    val msg = err.message ?: "Login failed"
                    _authError.value = msg
                    onResult(false, msg)
                }
            )
        }
    }

    fun signup(email: String, pass: String, onResult: (Boolean, String?) -> Unit) {
        viewModelScope.launch {
            _authError.value = null
            val res = supabaseRepo.signup(email, pass)
            res.fold(
                onSuccess = {
                    _isLoggedIn.value = true
                    _currentUserEmail.value = supabaseRepo.currentUserEmail
                    _showWalkthrough.value = true
                    prefs.edit().putBoolean("show_walkthrough", true).apply()
                    fetchUserDataParallel()
                    onResult(true, null)
                },
                onFailure = { err ->
                    val msg = err.message ?: "Signup failed"
                    _authError.value = msg
                    onResult(false, msg)
                }
            )
        }
    }

    fun logout() {
        supabaseRepo.clearSession()
        _isLoggedIn.value = false
        _currentUserEmail.value = null
        _watchlistSymbols.value = emptySet()
        _transactions.value = emptyList()
        holdings.value = emptyList()
        portfolioSummary.value = PortfolioSummary()
        _priceAlerts.value = emptyList()
        allPortfolios.clear()
        currentPortfolioId = null
    }

    fun resetPassword(email: String, onResult: (Boolean, String?) -> Unit) {
        viewModelScope.launch {
            val res = supabaseRepo.resetPassword(email)
            res.fold(
                onSuccess = { onResult(true, null) },
                onFailure = { err -> onResult(false, err.message) }
            )
        }
    }

    fun handleOAuthCallback(token: String, onResult: (Boolean, String?) -> Unit) {
        viewModelScope.launch {
            _authError.value = null
            val res = supabaseRepo.handleOAuthCallback(token)
            res.fold(
                onSuccess = {
                    _isLoggedIn.value = true
                    _currentUserEmail.value = supabaseRepo.currentUserEmail
                    fetchUserDataParallel()
                    onResult(true, null)
                },
                onFailure = { err ->
                    val msg = err.message ?: "Google Sign-In failed"
                    _authError.value = msg
                    onResult(false, msg)
                }
            )
        }
    }

    // Parallel fetch on login
    private fun fetchUserDataParallel() {
        viewModelScope.launch {
            launch { fetchWatchlist() }
            launch { fetchPortfolioData() }
            launch { fetchAlerts() }
        }
    }

    // Watchlist methods
    suspend fun fetchWatchlist() {
        val res = supabaseRepo.fetchWatchlist()
        res.onSuccess { list ->
            _watchlistSymbols.value = list.map { it.symbol.uppercase() }.toSet()
        }
    }

    fun addToWatchlist(symbol: String) {
        if (!_isLoggedIn.value) return
        val upper = symbol.uppercase().trim()
        viewModelScope.launch {
            val res = supabaseRepo.addToWatchlist(upper)
            res.onSuccess {
                _watchlistSymbols.value = _watchlistSymbols.value + upper
            }
        }
    }

    fun removeFromWatchlist(symbol: String) {
        if (!_isLoggedIn.value) return
        val upper = symbol.uppercase().trim()
        viewModelScope.launch {
            val res = supabaseRepo.removeFromWatchlist(upper)
            res.onSuccess {
                _watchlistSymbols.value = _watchlistSymbols.value - upper
            }
        }
    }

    // Transactions & Portfolio JSONB Storage & FIFO
    suspend fun fetchPortfolioData() {
        val res = supabaseRepo.fetchPortfolioData()
        res.onSuccess { row ->
            if (row != null && !row.portfolios.isNullOrEmpty()) {
                allPortfolios = row.portfolios.toMutableList()
                currentPortfolioId = row.currentPortfolioId ?: allPortfolios.first().id
                val active = allPortfolios.find { it.id == currentPortfolioId } ?: allPortfolios.first()
                _transactions.value = active.transactions ?: emptyList()
            } else {
                val newId = java.util.UUID.randomUUID().toString()
                allPortfolios = mutableListOf(
                    PortfolioJsonObj(
                        id = newId,
                        name = "Main Portfolio",
                        transactions = emptyList(),
                        watchlist = emptyList(),
                        alerts = emptyList()
                    )
                )
                currentPortfolioId = newId
                _transactions.value = emptyList()
            }
            recomputePortfolio()
        }
        res.onFailure {
            val newId = java.util.UUID.randomUUID().toString()
            allPortfolios = mutableListOf(
                PortfolioJsonObj(
                    id = newId,
                    name = "Main Portfolio",
                    transactions = emptyList(),
                    watchlist = emptyList(),
                    alerts = emptyList()
                )
            )
            currentPortfolioId = newId
            _transactions.value = emptyList()
            recomputePortfolio()
        }
    }

    private fun scheduleSavePortfolio() {
        if (!_isLoggedIn.value) return
        saveJob?.cancel()
        saveJob = viewModelScope.launch {
            delay(400L) // 400ms debounce
            val activeIndex = allPortfolios.indexOfFirst { it.id == currentPortfolioId }
            val updatedTxs = _transactions.value
            if (activeIndex >= 0) {
                val current = allPortfolios[activeIndex]
                allPortfolios[activeIndex] = current.copy(transactions = updatedTxs)
            } else if (allPortfolios.isNotEmpty()) {
                val current = allPortfolios.first()
                currentPortfolioId = current.id
                allPortfolios[0] = current.copy(transactions = updatedTxs)
            } else {
                val newId = java.util.UUID.randomUUID().toString()
                currentPortfolioId = newId
                allPortfolios.add(
                    PortfolioJsonObj(
                        id = newId,
                        name = "Main Portfolio",
                        transactions = updatedTxs,
                        watchlist = emptyList(),
                        alerts = emptyList()
                    )
                )
            }
            supabaseRepo.upsertPortfolioData(allPortfolios, currentPortfolioId)
        }
    }

    fun addTransaction(txMap: Map<String, Any?>, onResult: (Boolean, String?) -> Unit) {
        if (!_isLoggedIn.value) {
            onResult(false, "Please log in to manage portfolio")
            return
        }
        val newId = java.util.UUID.randomUUID().toString()
        val sym = (txMap["symbol"] as? String)?.uppercase()?.trim() ?: ""
        val side = (txMap["side"] as? String) ?: "buy"
        val qty = (txMap["qty"] as? Number)?.toDouble() ?: 0.0
        val price = (txMap["price"] as? Number)?.toDouble() ?: 0.0
        val date = (txMap["trade_date"] as? String) ?: SimpleDateFormat("yyyy-MM-dd", Locale.US).format(java.util.Date())
        val note = (txMap["note"] as? String)

        val newTx = PortfolioTransaction(
            id = newId,
            symbol = sym,
            side = side,
            qty = qty,
            price = price,
            date = date,
            note = note
        )
        _transactions.value = _transactions.value + newTx
        recomputePortfolio()
        scheduleSavePortfolio()
        onResult(true, null)
    }

    fun updateTransaction(id: String, txMap: Map<String, Any?>, onResult: (Boolean, String?) -> Unit) {
        val sym = (txMap["symbol"] as? String)?.uppercase()?.trim()
        val side = (txMap["side"] as? String)
        val qty = (txMap["qty"] as? Number)?.toDouble()
        val price = (txMap["price"] as? Number)?.toDouble()
        val date = (txMap["trade_date"] as? String)
        val note = (txMap["note"] as? String)

        _transactions.value = _transactions.value.map { tx ->
            if (tx.id == id) {
                PortfolioTransaction(
                    id = id,
                    symbol = sym ?: tx.symbol,
                    side = side ?: tx.side,
                    qty = qty ?: tx.qty,
                    price = price ?: tx.price,
                    date = date ?: tx.date,
                    note = if (txMap.containsKey("note")) note else tx.note
                )
            } else tx
        }
        recomputePortfolio()
        scheduleSavePortfolio()
        onResult(true, null)
    }

    fun deleteTransaction(id: String, onResult: (Boolean, String?) -> Unit) {
        _transactions.value = _transactions.value.filter { it.id != id }
        recomputePortfolio()
        scheduleSavePortfolio()
        onResult(true, null)
    }

    // FIFO Engine
    fun recomputePortfolio() {
        val txs = _transactions.value.sortedWith(compareBy({ it.tradeDate }, { it.createdAt ?: "" }))
        val boardMap = _boardItems.value.associateBy { it.symbol.uppercase() }

        data class BuyLot(val qty: Double, val price: Double)
        val symbolLots = mutableMapOf<String, ArrayDeque<BuyLot>>()
        val symbolRealizedPl = mutableMapOf<String, Double>()

        txs.forEach { tx ->
            val sym = tx.symbol.uppercase().trim()
            val lots = symbolLots.getOrPut(sym) { ArrayDeque() }
            val realized = symbolRealizedPl.getOrPut(sym) { 0.0 }

            if (tx.side.lowercase() == "buy") {
                lots.addLast(BuyLot(tx.qty, tx.price))
            } else if (tx.side.lowercase() == "sell") {
                var sellQtyToMatch = tx.qty
                var realizedGain = 0.0
                val sellPrice = tx.price

                while (sellQtyToMatch > 0 && lots.isNotEmpty()) {
                    val lot = lots.first()
                    if (lot.qty <= sellQtyToMatch) {
                        realizedGain += lot.qty * (sellPrice - lot.price)
                        sellQtyToMatch -= lot.qty
                        lots.removeFirst()
                    } else {
                        realizedGain += sellQtyToMatch * (sellPrice - lot.price)
                        val remainingQty = lot.qty - sellQtyToMatch
                        lots[0] = BuyLot(remainingQty, lot.price)
                        sellQtyToMatch = 0.0
                    }
                }
                symbolRealizedPl[sym] = realized + realizedGain
            }
        }

        val holdingList = mutableListOf<HoldingItem>()
        var totalMV = 0.0
        var totalInv = 0.0
        var totalRealPl = symbolRealizedPl.values.sum()
        val sectorAllocMap = mutableMapOf<String, Double>()

        symbolLots.forEach { (sym, lots) ->
            if (lots.isNotEmpty()) {
                val qtyHeld = lots.sumOf { it.qty }
                val totalCost = lots.sumOf { it.qty * it.price }
                val avgCost = if (qtyHeld > 0) totalCost / qtyHeld else 0.0

                val boardItem = boardMap[sym]
                val currentLtp = boardItem?.ltp ?: avgCost // fallback to cost if ltp unavailable
                val sector = boardItem?.sector ?: "Other"

                val marketValue = qtyHeld * currentLtp
                val unrealizedPl = marketValue - totalCost
                val unrealizedPct = if (totalCost > 0) (unrealizedPl / totalCost) * 100 else 0.0
                val realizedPl = symbolRealizedPl[sym] ?: 0.0

                totalMV += marketValue
                totalInv += totalCost

                sectorAllocMap[sector] = (sectorAllocMap[sector] ?: 0.0) + marketValue

                holdingList.add(
                    HoldingItem(
                        symbol = sym,
                        sector = sector,
                        qtyHeld = qtyHeld,
                        avgCost = avgCost,
                        currentLtp = currentLtp,
                        marketValue = marketValue,
                        totalInvestment = totalCost,
                        unrealizedPlAmount = unrealizedPl,
                        unrealizedPlPercent = unrealizedPct,
                        realizedPl = realizedPl
                    )
                )
            }
        }

        holdings.value = holdingList
        val totalUnrealized = totalMV - totalInv
        val totalUnrealizedPct = if (totalInv > 0) (totalUnrealized / totalInv) * 100 else 0.0

        portfolioSummary.value = PortfolioSummary(
            totalMarketValue = totalMV,
            totalInvestment = totalInv,
            totalUnrealizedPl = totalUnrealized,
            totalUnrealizedPlPercent = totalUnrealizedPct,
            totalRealizedPl = totalRealPl,
            sectorAllocation = sectorAllocMap
        )
    }

    // Price Alerts
    suspend fun fetchAlerts() {
        val res = supabaseRepo.fetchAlerts()
        res.onSuccess { _priceAlerts.value = it }
    }

    fun addAlert(symbol: String, targetPrice: Double, direction: String, onResult: (Boolean, String?) -> Unit) {
        if (!_isLoggedIn.value) {
            onResult(false, "Please log in to set price alerts")
            return
        }
        viewModelScope.launch {
            val res = supabaseRepo.addAlert(symbol, targetPrice, direction)
            res.fold(
                onSuccess = {
                    fetchAlerts()
                    onResult(true, null)
                },
                onFailure = { err -> onResult(false, err.message) }
            )
        }
    }

    fun deleteAlert(id: String) {
        viewModelScope.launch {
            supabaseRepo.deleteAlert(id)
            fetchAlerts()
        }
    }

    private fun checkPriceAlerts(board: List<BoardItem>) {
        val boardMap = board.associateBy { it.symbol.uppercase() }
        val alerts = _priceAlerts.value
        val newlyTriggered = mutableListOf<String>()

        alerts.filter { it.triggered != true }.forEach { alert ->
            val item = boardMap[alert.symbol.uppercase()]
            if (item?.ltp != null) {
                val ltp = item.ltp
                val crossed = if (alert.direction.lowercase() == "above") {
                    ltp >= alert.targetPrice
                } else {
                    ltp <= alert.targetPrice
                }
                if (crossed) {
                    newlyTriggered.add("Alert Triggered: ${alert.symbol} crossed ${alert.targetPrice} (Current LTP: $ltp)")
                    viewModelScope.launch {
                        alert.id?.let { supabaseRepo.updateAlertTriggered(it, true) }
                        fetchAlerts()
                    }
                }
            }
        }

        if (newlyTriggered.isNotEmpty()) {
            triggeredAlerts.value = triggeredAlerts.value + newlyTriggered
        }
    }

    fun dismissTriggeredAlert(msg: String) {
        triggeredAlerts.value = triggeredAlerts.value.filter { it != msg }
    }

    fun addRecentlyViewed(symbol: String) {
        val upper = symbol.uppercase().trim()
        val list = _recentlyViewed.value.toMutableList()
        list.remove(upper)
        list.add(0, upper)
        if (list.size > 10) list.removeAt(list.size - 1)
        _recentlyViewed.value = list
    }

    // CSV Batch Import Parser
    fun importTransactionsFromCsv(csvText: String, onResult: (Int, String?) -> Unit) {
        if (!_isLoggedIn.value) {
            onResult(0, "Please log in to import transactions")
            return
        }
        viewModelScope.launch {
            try {
                val lines = csvText.lines().filter { it.isNotBlank() }
                if (lines.isEmpty()) {
                    onResult(0, "Empty CSV file")
                    return@launch
                }
                val header = lines[0].lowercase()
                var importedCount = 0
                val newTxs = mutableListOf<PortfolioTransaction>()

                // Auto-detect header type: TMS, Meroshare, WACC, or standard
                val isMeroshare = header.contains("credit") || header.contains("debit")
                val isWacc = header.contains("wacc") || header.contains("scrip")
                val isTms = header.contains("side") || header.contains("quantity") || header.contains("buy")

                for (i in 1 until lines.size) {
                    val line = lines[i]
                    val parts = line.split(",").map { it.trim().removeSurrounding("\"") }
                    if (parts.size >= 3) {
                        try {
                            // parse based on format
                            val symbol = parts[0].uppercase()
                            var side = "buy"
                            var qty = 0.0
                            var price = 0.0
                            val date = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(java.util.Date())

                            if (isTms && parts.size >= 4) {
                                side = if (parts[1].lowercase().contains("sell")) "sell" else "buy"
                                qty = parts[2].toDoubleOrNull() ?: 0.0
                                price = parts[3].toDoubleOrNull() ?: 0.0
                            } else if (isMeroshare && parts.size >= 4) {
                                val credit = parts[1].toDoubleOrNull() ?: 0.0
                                val debit = parts[2].toDoubleOrNull() ?: 0.0
                                if (credit > 0) {
                                    side = "buy"
                                    qty = credit
                                } else {
                                    side = "sell"
                                    qty = debit
                                }
                                price = parts[3].toDoubleOrNull() ?: 0.0
                            } else {
                                // Default/Wacc format: symbol, side, qty, price
                                side = if (parts.size > 1 && parts[1].lowercase().contains("sell")) "sell" else "buy"
                                qty = parts.getOrNull(2)?.toDoubleOrNull() ?: parts.getOrNull(1)?.toDoubleOrNull() ?: 0.0
                                price = parts.getOrNull(3)?.toDoubleOrNull() ?: parts.getOrNull(2)?.toDoubleOrNull() ?: 0.0
                            }

                            if (qty > 0 && price > 0 && symbol.isNotBlank()) {
                                val tx = PortfolioTransaction(
                                    id = java.util.UUID.randomUUID().toString(),
                                    symbol = symbol,
                                    side = side,
                                    qty = qty,
                                    price = price,
                                    date = date,
                                    note = "Imported CSV"
                                )
                                newTxs.add(tx)
                                importedCount++
                            }
                        } catch (e: Exception) {
                            Log.w("Import", "Failed to parse line: $line")
                        }
                    }
                }
                if (newTxs.isNotEmpty()) {
                    _transactions.value = _transactions.value + newTxs
                    recomputePortfolio()
                    scheduleSavePortfolio()
                }
                onResult(importedCount, null)
            } catch (e: Exception) {
                onResult(0, e.message ?: "Failed to parse CSV")
            }
        }
    }

    fun exportTransactionsCsv(): String {
        val sb = StringBuilder()
        sb.append("id,symbol,side,qty,price,trade_date,note\n")
        _transactions.value.forEach { tx ->
            sb.append("${tx.id},${tx.symbol},${tx.side},${tx.qty},${tx.price},${tx.tradeDate},\"${tx.note ?: ""}\"\n")
        }
        return sb.toString()
    }
}
