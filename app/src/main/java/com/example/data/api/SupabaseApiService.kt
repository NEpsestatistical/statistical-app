package com.example.data.api

import com.example.data.model.PortfolioDataRow
import com.example.data.model.PortfolioTransaction
import com.example.data.model.PriceAlert
import com.example.data.model.WatchlistItem
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.*

interface SupabaseApiService {
    @POST("auth/v1/signup")
    suspend fun signup(
        @Header("apikey") apiKey: String,
        @Body request: SupabaseAuthRequest
    ): Response<SupabaseAuthResponse>

    @POST("auth/v1/token?grant_type=password")
    suspend fun login(
        @Header("apikey") apiKey: String,
        @Body request: SupabaseAuthRequest
    ): Response<SupabaseAuthResponse>

    @POST("auth/v1/recover")
    suspend fun recoverPassword(
        @Header("apikey") apiKey: String,
        @Body request: SupabaseRecoverRequest
    ): Response<ResponseBody>

    @GET("auth/v1/user")
    suspend fun getUser(
        @Header("apikey") apiKey: String,
        @Header("Authorization") token: String
    ): Response<SupabaseUser>

    // Watchlist REST
    @GET("rest/v1/watchlist")
    suspend fun getWatchlist(
        @Header("apikey") apiKey: String,
        @Header("Authorization") token: String,
        @Query("user_id") eqUserId: String
    ): Response<List<WatchlistItem>>

    @POST("rest/v1/watchlist")
    suspend fun addToWatchlist(
        @Header("apikey") apiKey: String,
        @Header("Authorization") token: String,
        @Header("Prefer") prefer: String = "return=representation",
        @Body item: Map<String, String>
    ): Response<List<WatchlistItem>>

    @DELETE("rest/v1/watchlist")
    suspend fun removeFromWatchlist(
        @Header("apikey") apiKey: String,
        @Header("Authorization") token: String,
        @Query("user_id") userId: String,
        @Query("symbol") symbol: String
    ): Response<ResponseBody>

    // Portfolio Data REST
    @GET("rest/v1/portfolio_data")
    suspend fun getPortfolioData(
        @Header("apikey") apiKey: String,
        @Header("Authorization") token: String,
        @Query("user_id") eqUserId: String,
        @Query("select") select: String = "portfolios,current_portfolio_id,schema_version"
    ): Response<List<PortfolioDataRow>>

    @POST("rest/v1/portfolio_data")
    suspend fun upsertPortfolioData(
        @Header("apikey") apiKey: String,
        @Header("Authorization") token: String,
        @Header("Prefer") prefer: String = "resolution=merge-duplicates,on_conflict=user_id",
        @Body data: Map<String, Any?>
    ): Response<ResponseBody>

    // Price Alerts REST
    @GET("rest/v1/price_alerts")
    suspend fun getAlerts(
        @Header("apikey") apiKey: String,
        @Header("Authorization") token: String,
        @Query("user_id") eqUserId: String
    ): Response<List<PriceAlert>>

    @POST("rest/v1/price_alerts")
    suspend fun addAlert(
        @Header("apikey") apiKey: String,
        @Header("Authorization") token: String,
        @Header("Prefer") prefer: String = "return=representation",
        @Body alert: Map<String, Any?>
    ): Response<List<PriceAlert>>

    @PATCH("rest/v1/price_alerts")
    suspend fun updateAlert(
        @Header("apikey") apiKey: String,
        @Header("Authorization") token: String,
        @Header("Prefer") prefer: String = "return=representation",
        @Query("id") id: String,
        @Body alert: Map<String, Any?>
    ): Response<List<PriceAlert>>

    @DELETE("rest/v1/price_alerts")
    suspend fun deleteAlert(
        @Header("apikey") apiKey: String,
        @Header("Authorization") token: String,
        @Query("id") id: String
    ): Response<ResponseBody>
}
