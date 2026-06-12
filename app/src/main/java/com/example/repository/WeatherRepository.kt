package com.example.repository

import com.example.api.*
import com.example.database.RecentCity
import com.example.database.RecentCityDao
import com.example.model.WeatherResponse
import com.example.BuildConfig
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

class WeatherRepository(private val recentCityDao: RecentCityDao) {

    val recentCities: Flow<List<RecentCity>> = recentCityDao.getRecentCities()

    private val moshi = Moshi.Builder()
        .addLast(KotlinJsonAdapterFactory())
        .build()
    
    private val weatherAdapter = moshi.adapter(WeatherResponse::class.java)

    suspend fun saveCity(cityName: String) {
        recentCityDao.insertCity(RecentCity(cityName = cityName, searchTime = System.currentTimeMillis()))
    }

    suspend fun removeCity(cityName: String) {
        recentCityDao.deleteCity(cityName)
    }

    suspend fun clearHistory() {
        recentCityDao.clearAll()
    }

    suspend fun fetchDioramaWeather(city: String): WeatherResponse = withContext(Dispatchers.IO) {
        val apiKey = BuildConfig.GEMINI_API_KEY
        if (apiKey.isEmpty() || apiKey == "MY_GEMINI_API_KEY") {
            throw IllegalStateException("Gemini API Key is missing. Please configure it in the Secrets Panel in AI Studio Dashboard.")
        }

        val systemPrompt = "You are a highly precise weather forecaster and landscape architect designed to return structured JSON."
        val mainPrompt = """
            Create a highly detailed, realistic current weather report, 5-hour forecast, 5-day forecast, and a 3D miniature horizontal block-styled landmark diorama scene representing the city/region of: "$city".

            Provide 4 to 7 detailed landmarks, nature points or distinctive building shapes that capture the iconic visual character of the location.
            For each shape, supply:
            - name: The specific iconic landmark (e.g. "Colosseum", "Eiffel Tower", "Fuji Torii Gate", "Empire State", "Giza Pyramids", "Typical Parisian Flats", "Skyline Skyscraper") or scenery descriptor.
            - type: "landmark" or "building" or "nature"
            - x: relative horizontal percentage index across screen canvas (from 5.0 to 95.0) where this building is centered. Scatter them naturally, avoiding drawing them directly on top of each other.
            - width: width percentage of the block (from 6.0 to 22.0)
            - height: height percentage of the block (from 15.0 to 85.0).
            - depth: depth level (from 1.0 to 5.0) where 1.0 is background layer and 5.0 is extreme foreground. This determines draw layering.
            - colorHex: A gorgeous, representative premium solid hex color (e.g., "#2E4F4F", "#D27685", "#70624E") adapted slightly to the location's native material (sandstone, concrete, steel) and modulated by current weather ambiance.
            - shapeType: One of "tower", "box", "dome", "arch", "spire", "pyramid".

            Also specify climate weather info including skyColorStart (ambient gradient sky top hex) and skyColorEnd (ambient gradient sky horizon hex) representing the current climate/weather of $city (e.g., deep warm/blue for Sunny, charcoal/deep indigo for Night, ash-gray/silver for Cloudy, atmospheric orange-indigo/rose for Sunset/Sunrise if desired).

            Return ONLY a valid, strict JSON object matching the following structure:
            {
              "cityName": "Paris, France",
              "weather": {
                "condition": "Sunny", 
                "tempCelsius": 22.0,
                "feelsLikeCelsius": 23.0,
                "humidity": 60,
                "windSpeedKph": 12.5,
                "description": "Clear beautiful skies over Paris.",
                "hourly": [
                  {"time": "12:00", "temp": 20.0, "condition": "Sunny"},
                  {"time": "13:00", "temp": 22.0, "condition": "Sunny"},
                  {"time": "14:00", "temp": 23.0, "condition": "Sunny"},
                  {"time": "15:00", "temp": 22.0, "condition": "Sunny"},
                  {"time": "16:00", "temp": 21.0, "condition": "Sunny"}
                ],
                "forecast": [
                  {"day": "Mon", "high": 24.0, "low": 14.0, "condition": "Sunny"},
                  {"day": "Tue", "high": 23.0, "low": 15.0, "condition": "Cloudy"},
                  {"day": "Wed", "high": 21.0, "low": 13.0, "condition": "Rainy"},
                  {"day": "Thu", "high": 22.0, "low": 12.0, "condition": "Sunny"},
                  {"day": "Fri", "high": 25.0, "low": 14.0, "condition": "Sunny"}
                ]
              },
              "scenery": {
                "skyColorStart": "#4BB4E8",
                "skyColorEnd": "#BBE6FA",
                "landmarks": [
                  {
                    "name": "Eiffel Tower",
                    "type": "landmark",
                    "x": 30.0,
                    "width": 12.0,
                    "height": 75.0,
                    "depth": 3.0,
                    "colorHex": "#8D6E63",
                    "shapeType": "tower"
                  }
                ]
              }
            }
        """.trimIndent()

        val request = GenerateContentRequest(
            contents = listOf(Content(parts = listOf(Part(text = mainPrompt)))),
            generationConfig = GenerationConfig(
                responseMimeType = "application/json",
                temperature = 0.2f
            ),
            systemInstruction = Content(parts = listOf(Part(text = systemPrompt)))
        )

        val response = RetrofitClient.service.generateContent(apiKey, request)
        val responseText = response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text
            ?: throw IllegalStateException("Received empty response from weather synthesis engine.")

        val extractedJson = cleanJsonString(responseText)
        weatherAdapter.fromJson(extractedJson) 
            ?: throw IllegalStateException("Failed to parse weather diorama data. Content response was not in expected schema.")
    }

    private fun cleanJsonString(input: String): String {
        val startIndex = input.indexOf("{")
        val endIndex = input.lastIndexOf("}")
        return if (startIndex != -1 && endIndex != -1 && endIndex > startIndex) {
            input.substring(startIndex, endIndex + 1)
        } else {
            input
        }
    }
}
