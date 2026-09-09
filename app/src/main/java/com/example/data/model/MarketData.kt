package com.example.data.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class BoardItem(
    @Json(name = "symbol") val symbol: String,
    @Json(name = "sector") val sector: String?,
    @Json(name = "ltp") val ltp: Double?,
    @Json(name = "percentChange") val percentChange: Double?,
    @Json(name = "changeAmount") val changeAmount: Double?,
    @Json(name = "qty") val qty: Double?
)

@JsonClass(generateAdapter = true)
data class MarketIndex(
    @Json(name = "date") val date: String,
    @Json(name = "value") val value: Double,
    @Json(name = "pointChange") val pointChange: Double,
    @Json(name = "percentChange") val percentChange: Double
)

@JsonClass(generateAdapter = true)
data class BoardResponse(
    @Json(name = "board") val board: List<BoardItem>?,
    @Json(name = "index") val index: MarketIndex?
)

@JsonClass(generateAdapter = true)
data class Candle(
    @Json(name = "time") val time: Long,
    @Json(name = "open") val open: Double,
    @Json(name = "high") val high: Double,
    @Json(name = "low") val low: Double,
    @Json(name = "close") val close: Double
)

@JsonClass(generateAdapter = true)
data class ChartResponse(
    @Json(name = "candles") val candles: List<Candle>?
)

@JsonClass(generateAdapter = true)
data class WatchlistItem(
    @Json(name = "id") val id: String?,
    @Json(name = "user_id") val userId: String,
    @Json(name = "symbol") val symbol: String,
    @Json(name = "created_at") val createdAt: String?
)

@JsonClass(generateAdapter = true)
data class PortfolioTransaction(
    @Json(name = "id") val id: String?,
    @Json(name = "symbol") val symbol: String,
    @Json(name = "side") val side: String, // 'buy' or 'sell'
    @Json(name = "qty") val qty: Double,
    @Json(name = "price") val price: Double,
    @Json(name = "date") val date: String?,
    @Json(name = "note") val note: String?
) {
    val tradeDate: String
        get() = date ?: ""
    val createdAt: String?
        get() = null
}

@JsonClass(generateAdapter = true)
data class PortfolioDataRow(
    @Json(name = "portfolios") val portfolios: List<PortfolioJsonObj>?,
    @Json(name = "current_portfolio_id") val currentPortfolioId: String?,
    @Json(name = "schema_version") val schemaVersion: Int?
)

@JsonClass(generateAdapter = true)
data class PortfolioJsonObj(
    @Json(name = "id") val id: String,
    @Json(name = "name") val name: String,
    @Json(name = "transactions") val transactions: List<PortfolioTransaction>?,
    @Json(name = "watchlist") val watchlist: List<Any>?,
    @Json(name = "alerts") val alerts: List<Any>?
)

@JsonClass(generateAdapter = true)
data class PriceAlert(
    @Json(name = "id") val id: String?,
    @Json(name = "user_id") val userId: String,
    @Json(name = "symbol") val symbol: String,
    @Json(name = "target_price") val targetPrice: Double,
    @Json(name = "direction") val direction: String, // 'above' or 'below'
    @Json(name = "triggered") val triggered: Boolean?,
    @Json(name = "created_at") val createdAt: String?
)
