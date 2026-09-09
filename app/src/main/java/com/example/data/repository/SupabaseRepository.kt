package com.example.data.repository

import android.content.Context
import android.util.Log
import com.example.data.api.SupabaseApiService
import com.example.data.api.SupabaseAuthRequest
import com.example.data.api.SupabaseRecoverRequest
import com.example.data.model.PortfolioDataRow
import com.example.data.model.PortfolioJsonObj
import com.example.data.model.PortfolioTransaction
import com.example.data.model.PriceAlert
import com.example.data.model.WatchlistItem
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import java.util.concurrent.TimeUnit

class SupabaseRepository(context: Context) {
    private val prefs = context.getSharedPreferences("statistical_portfolio_prefs", Context.MODE_PRIVATE)

    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .addInterceptor(HttpLoggingInterceptor().apply { level = HttpLoggingInterceptor.Level.BODY })
        .build()

    private val moshi = Moshi.Builder()
        .add(KotlinJsonAdapterFactory())
        .build()

    private val retrofit = Retrofit.Builder()
        .baseUrl("https://bfuatuhoosiwaugcxhjt.supabase.co/")
        .client(client)
        .addConverterFactory(MoshiConverterFactory.create(moshi))
        .build()

    private val api = retrofit.create(SupabaseApiService::class.java)
    private val anonKey = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6ImJmdWF0dWhvb3Npd2F1Z2N4aGp0Iiwicm9sZSI6ImFub24iLCJpYXQiOjE3ODYyNzIxNTksImV4cCI6MjEwMTg0ODE1OX0.0eGEG6MDysnAx8P08xdf9hQxzBTl1erQRNxFOh4ZQDI"

    var currentUserId: String? = prefs.getString("user_id", null)
        private set
    var currentUserEmail: String? = prefs.getString("user_email", null)
        private set
    var accessToken: String? = prefs.getString("access_token", null)
        private set

    fun isLoggedIn(): Boolean = !currentUserId.isNullOrEmpty() && !accessToken.isNullOrEmpty()

    private fun saveSession(userId: String, email: String?, token: String?) {
        currentUserId = userId
        currentUserEmail = email
        accessToken = token
        prefs.edit()
            .putString("user_id", userId)
            .putString("user_email", email)
            .putString("access_token", token)
            .apply()
    }

    fun clearSession() {
        currentUserId = null
        currentUserEmail = null
        accessToken = null
        prefs.edit().clear().apply()
    }

    suspend fun signup(email: String, pass: String): Result<Pair<String?, String?>> = withContext(Dispatchers.IO) {
        try {
            val res = api.signup(anonKey, SupabaseAuthRequest(email, pass))
            if (res.isSuccessful && res.body() != null) {
                val body = res.body()!!
                val userId = body.user?.id ?: body.id
                val token = body.accessToken
                if (userId != null) {
                    saveSession(userId, email, token)
                    return@withContext Result.success(Pair(userId, email))
                }
            }
            val errBody = res.errorBody()?.string() ?: "Signup failed"
            Result.failure(Exception(errBody))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun login(email: String, pass: String): Result<Pair<String?, String?>> = withContext(Dispatchers.IO) {
        try {
            val res = api.login(anonKey, SupabaseAuthRequest(email, pass))
            if (res.isSuccessful && res.body() != null) {
                val body = res.body()!!
                val userId = body.user?.id ?: body.id
                val token = body.accessToken
                val emailVal = body.user?.email ?: email
                if (userId != null && token != null) {
                    saveSession(userId, emailVal, token)
                    return@withContext Result.success(Pair(userId, emailVal))
                }
            }
            val errBody = res.errorBody()?.string() ?: "Invalid login credentials"
            Result.failure(Exception(errBody))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun resetPassword(email: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val res = api.recoverPassword(anonKey, SupabaseRecoverRequest(email))
            if (res.isSuccessful) {
                Result.success(Unit)
            } else {
                Result.failure(Exception(res.errorBody()?.string() ?: "Password reset failed"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun handleOAuthCallback(token: String): Result<Pair<String?, String?>> = withContext(Dispatchers.IO) {
        try {
            val res = api.getUser(anonKey, "Bearer $token")
            if (res.isSuccessful && res.body() != null) {
                val user = res.body()!!
                val userId = user.id
                val email = user.email
                if (userId != null) {
                    saveSession(userId, email, token)
                    return@withContext Result.success(Pair(userId, email))
                }
            }
            Result.failure(Exception("Failed to retrieve user profile from Google OAuth token"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // Watchlist
    suspend fun fetchWatchlist(): Result<List<WatchlistItem>> = withContext(Dispatchers.IO) {
        val uid = currentUserId ?: return@withContext Result.success(emptyList())
        val token = accessToken ?: ""
        try {
            val res = api.getWatchlist(anonKey, "Bearer $token", uid)
            if (res.isSuccessful) {
                Result.success(res.body() ?: emptyList())
            } else {
                Result.failure(Exception(res.errorBody()?.string() ?: "Failed to fetch watchlist"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun addToWatchlist(symbol: String): Result<Unit> = withContext(Dispatchers.IO) {
        val uid = currentUserId ?: return@withContext Result.failure(Exception("Not logged in"))
        val token = accessToken ?: ""
        try {
            val map = mapOf("user_id" to uid, "symbol" to symbol.uppercase().trim())
            val res = api.addToWatchlist(anonKey, "Bearer $token", "return=representation", map)
            if (res.isSuccessful) {
                Result.success(Unit)
            } else {
                val err = res.errorBody()?.string() ?: ""
                if (err.contains("duplicate") || err.contains("unique") || res.code() == 409) {
                    Result.success(Unit) // Already in watchlist
                } else {
                    Result.failure(Exception(err.ifEmpty { "Failed to add to watchlist" }))
                }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun removeFromWatchlist(symbol: String): Result<Unit> = withContext(Dispatchers.IO) {
        val uid = currentUserId ?: return@withContext Result.failure(Exception("Not logged in"))
        val token = accessToken ?: ""
        try {
            val res = api.removeFromWatchlist(anonKey, "Bearer $token", uid, symbol.uppercase().trim())
            if (res.isSuccessful) {
                Result.success(Unit)
            } else {
                Result.failure(Exception(res.errorBody()?.string() ?: "Failed to remove from watchlist"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // Portfolio Data
    suspend fun fetchPortfolioData(): Result<PortfolioDataRow?> = withContext(Dispatchers.IO) {
        val uid = currentUserId ?: return@withContext Result.success(null)
        val token = accessToken ?: ""
        try {
            val res = api.getPortfolioData(anonKey, "Bearer $token", "eq.$uid")
            if (res.isSuccessful) {
                val list = res.body()
                Result.success(list?.firstOrNull())
            } else {
                Result.failure(Exception(res.errorBody()?.string() ?: "Failed to fetch portfolio data"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun upsertPortfolioData(portfolios: List<PortfolioJsonObj>, currentPortfolioId: String?): Result<Unit> = withContext(Dispatchers.IO) {
        val uid = currentUserId ?: return@withContext Result.failure(Exception("Not logged in"))
        val token = accessToken ?: ""
        try {
            val map = mapOf(
                "user_id" to uid,
                "portfolios" to portfolios,
                "current_portfolio_id" to currentPortfolioId,
                "schema_version" to 2,
                "updated_at" to java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", java.util.Locale.US).format(java.util.Date())
            )
            val res = api.upsertPortfolioData(anonKey, "Bearer $token", "resolution=merge-duplicates", map)
            if (res.isSuccessful) {
                Result.success(Unit)
            } else {
                Result.failure(Exception(res.errorBody()?.string() ?: "Failed to save portfolio data"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // Price Alerts
    suspend fun fetchAlerts(): Result<List<PriceAlert>> = withContext(Dispatchers.IO) {
        val uid = currentUserId ?: return@withContext Result.success(emptyList())
        val token = accessToken ?: ""
        try {
            val res = api.getAlerts(anonKey, "Bearer $token", uid)
            if (res.isSuccessful) {
                Result.success(res.body() ?: emptyList())
            } else {
                Result.failure(Exception(res.errorBody()?.string() ?: "Failed to fetch price alerts"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun addAlert(symbol: String, targetPrice: Double, direction: String): Result<Unit> = withContext(Dispatchers.IO) {
        val uid = currentUserId ?: return@withContext Result.failure(Exception("Not logged in"))
        val token = accessToken ?: ""
        try {
            val map = mapOf(
                "user_id" to uid,
                "symbol" to symbol.uppercase().trim(),
                "target_price" to targetPrice,
                "direction" to direction,
                "triggered" to false
            )
            val res = api.addAlert(anonKey, "Bearer $token", "return=representation", map)
            if (res.isSuccessful) {
                Result.success(Unit)
            } else {
                Result.failure(Exception(res.errorBody()?.string() ?: "Failed to add alert"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updateAlertTriggered(id: String, triggered: Boolean): Result<Unit> = withContext(Dispatchers.IO) {
        val token = accessToken ?: ""
        try {
            val map = mapOf("triggered" to triggered)
            val res = api.updateAlert(anonKey, "Bearer $token", "return=representation", "eq.$id", map)
            if (res.isSuccessful) Result.success(Unit) else Result.failure(Exception("Failed to update alert"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deleteAlert(id: String): Result<Unit> = withContext(Dispatchers.IO) {
        val token = accessToken ?: ""
        try {
            val res = api.deleteAlert(anonKey, "Bearer $token", "eq.$id")
            if (res.isSuccessful) Result.success(Unit) else Result.failure(Exception("Failed to delete alert"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
