package com.example.data.api

import com.example.data.model.BoardResponse
import com.example.data.model.ChartResponse
import retrofit2.http.GET
import retrofit2.http.Query
import retrofit2.http.Url

interface NepseApiService {
    @GET("all")
    suspend fun getBoardData(): BoardResponse

    @GET
    suspend fun getChartData(@Url url: String = "https://nepsechart.bharatiaashish43.workers.dev/", @Query("symbol") symbol: String): ChartResponse
}
