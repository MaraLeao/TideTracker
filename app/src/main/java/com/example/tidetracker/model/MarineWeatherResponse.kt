package com.example.tidetracker.model

data class MarineWeatherResponse (
    val latitude: Double,
    val longitude: Double,
    val hourly: HourlyData
)

data class HourlyData(
    val time: List<String>?,
    val wave_height: List<Double?>?,
    val wave_period: List<Double?>?,
    val sea_level_height_msl: List<Double?>?,
    val sea_surface_temperature: List<Double?>?

)
