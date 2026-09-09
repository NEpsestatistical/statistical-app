package com.example.data.repository

import android.util.Log
import com.example.data.api.NepseApiService
import com.example.data.model.BoardItem
import com.example.data.model.Candle
import com.example.data.model.MarketIndex
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import java.util.concurrent.TimeUnit

class MarketRepository {
    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .addInterceptor(HttpLoggingInterceptor().apply { level = HttpLoggingInterceptor.Level.BODY })
        .build()

    private val moshi = Moshi.Builder()
        .add(KotlinJsonAdapterFactory())
        .build()

    private val retrofit = Retrofit.Builder()
        .baseUrl("https://shiny-term-f599.bharatiaashish43.workers.dev/")
        .client(client)
        .addConverterFactory(MoshiConverterFactory.create(moshi))
        .build()

    private val api = retrofit.create(NepseApiService::class.java)

    private var cachedBoard: List<BoardItem> = emptyList()
    private var cachedIndex: MarketIndex? = null
    private var lastFetchTime: Long = 0L
    private val ttlMs: Long = 50_000L // ~50s TTL

    // Session observed index & board snapshots for sparklines
    private val indexSnapshots = mutableListOf<Pair<Long, Double>>() // time -> value
    private val symbolHistorySnapshots = mutableMapOf<String, MutableList<Pair<Long, Double>>>() // symbol -> list of (time, ltp)

    private val chartCache = mutableMapOf<String, List<Candle>>()

    suspend fun fetchBoardData(forceRefresh: Boolean = false): Result<Pair<List<BoardItem>, MarketIndex?>> = withContext(Dispatchers.IO) {
        val now = System.currentTimeMillis()
        if (!forceRefresh && cachedBoard.isNotEmpty() && (now - lastFetchTime < ttlMs)) {
            return@withContext Result.success(Pair(cachedBoard, cachedIndex))
        }

        var attempts = 0
        var lastException: Exception? = null
        while (attempts < 3) {
            try {
                val response = api.getBoardData()
                val rawBoard = response.board ?: emptyList()
                // CRITICAL: deduplicate board by symbol (keep first occurrence)
                val deduped = rawBoard.distinctBy { it.symbol.uppercase().trim() }
                
                cachedBoard = deduped
                cachedIndex = response.index
                lastFetchTime = System.currentTimeMillis()

                // Record snapshots for sparklines
                response.index?.let { idx ->
                    indexSnapshots.add(Pair(System.currentTimeMillis(), idx.value))
                    if (indexSnapshots.size > 50) indexSnapshots.removeAt(0)
                }
                deduped.forEach { item ->
                    if (item.ltp != null) {
                        val list = symbolHistorySnapshots.getOrPut(item.symbol) { mutableListOf() }
                        list.add(Pair(System.currentTimeMillis(), item.ltp))
                        if (list.size > 30) list.removeAt(0)
                    }
                }

                return@withContext Result.success(Pair(deduped, response.index))
            } catch (e: Exception) {
                lastException = e
                attempts++
                Log.w("MarketRepo", "Fetch attempt $attempts failed: ${e.message}")
                if (attempts < 3) {
                    delay(1000L * attempts) // exponential backoff
                }
            }
        }
        // If cached data exists, return stale cache on failure rather than hard crash
        if (cachedBoard.isNotEmpty()) {
            return@withContext Result.success(Pair(cachedBoard, cachedIndex))
        }
        Result.failure(lastException ?: Exception("Failed to load market board"))
    }

    suspend fun getChartData(symbol: String, forceRefresh: Boolean = false): Result<List<Candle>> = withContext(Dispatchers.IO) {
        val upperSymbol = symbol.uppercase().trim()
        if (!forceRefresh && chartCache.containsKey(upperSymbol)) {
            return@withContext Result.success(chartCache[upperSymbol]!!)
        }

        var attempts = 0
        var lastException: Exception? = null
        while (attempts < 3) {
            try {
                val res = api.getChartData(symbol = upperSymbol)
                val candles = res.candles ?: emptyList()
                chartCache[upperSymbol] = candles
                return@withContext Result.success(candles)
            } catch (e: Exception) {
                lastException = e
                attempts++
                Log.w("MarketRepo", "Chart fetch attempt $attempts for $upperSymbol failed: ${e.message}")
                if (attempts < 3) {
                    delay(1000L * attempts)
                }
            }
        }
        if (chartCache.containsKey(upperSymbol)) {
            return@withContext Result.success(chartCache[upperSymbol]!!)
        }
        Result.failure(lastException ?: Exception("Failed to load chart data for $upperSymbol"))
    }

    fun getIndexSnapshots(): List<Pair<Long, Double>> = indexSnapshots.toList()
    fun getSymbolSparkline(symbol: String): List<Pair<Long, Double>> = symbolHistorySnapshots[symbol.uppercase().trim()]?.toList() ?: emptyList()
}
