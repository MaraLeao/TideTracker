package com.example.tidetracker.api

import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.Query
import com.example.tidetracker.model.MarineWeatherResponse

interface MarineWeatherApi {
    @GET("v1/marine")
    fun getMarineWeather(
        @Query("latitude") latitude: Double,
        @Query("longitude") longitude: Double,
        @Query("hourly") hourly: String = "wave_height,wave_period,sea_level_height_msl,sea_surface_temperature"
    ): Call<MarineWeatherResponse>
}