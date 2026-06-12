package com.example.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class WeatherResponse(
    val cityName: String,
    val weather: WeatherDetail,
    val scenery: SceneryDetail
)

@JsonClass(generateAdapter = true)
data class WeatherDetail(
    val condition: String,
    val tempCelsius: Double,
    val feelsLikeCelsius: Double,
    val humidity: Int,
    val windSpeedKph: Double,
    val description: String,
    val hourly: List<HourlyForecast>,
    val forecast: List<DailyForecast>
)

@JsonClass(generateAdapter = true)
data class HourlyForecast(
    val time: String,
    val temp: Double,
    val condition: String
)

@JsonClass(generateAdapter = true)
data class DailyForecast(
    val day: String,
    val high: Double,
    val low: Double,
    val condition: String
)

@JsonClass(generateAdapter = true)
data class SceneryDetail(
    val skyColorStart: String,
    val skyColorEnd: String,
    val landmarks: List<LandmarkItem>
)

@JsonClass(generateAdapter = true)
data class LandmarkItem(
    val name: String,
    val type: String, // landmark, building, nature
    val x: Double, // relative horizontal percentage (5.0 to 95.0)
    val width: Double, // width percentage (6.0 to 22.0)
    val height: Double, // height percentage (15.0 to 85.0)
    val depth: Double, // depth level (1.0 to 5.0)
    val colorHex: String,
    val shapeType: String // tower, box, dome, arch, spire, pyramid
)
