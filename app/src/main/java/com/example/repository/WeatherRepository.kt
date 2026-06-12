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

        var apiResult: WeatherResponse? = null
        var lastError: Throwable? = null

        val modelsToTry = listOf("gemini-3.5-flash", "gemini-3.1-pro-preview")
        for (model in modelsToTry) {
            var attempt = 1
            val maxAttempts = 2
            while (attempt <= maxAttempts) {
                try {
                    val response = RetrofitClient.service.generateContent(model, apiKey, request)
                    val responseText = response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text
                        ?: throw IllegalStateException("Received empty response from weather synthesis engine.")
                    
                    val extractedJson = cleanJsonString(responseText)
                    val parsed = weatherAdapter.fromJson(extractedJson)
                        ?: throw IllegalStateException("Failed to parse weather diorama data.")
                    
                    apiResult = parsed
                    break // Success!
                } catch (e: Exception) {
                    lastError = e
                    attempt++
                    if (attempt <= maxAttempts) {
                        kotlinx.coroutines.delay(1000) // Short delay before retry
                    }
                }
            }
            if (apiResult != null) break
        }

        if (apiResult != null) {
            apiResult
        } else {
            // Log/print error and gracefully fall back to procedural local simulation
            android.util.Log.e("WeatherRepository", "Gemini API failed with error, falling back offline: ${lastError?.message}", lastError)
            generateOfflineDiorama(city)
        }
    }

    fun generateOfflineDiorama(city: String): WeatherResponse {
        val query = city.lowercase()
        val condition = when {
            query.contains("paris") -> "Sunny"
            query.contains("tokyo") -> "Cloudy"
            query.contains("cairo") -> "Sunny"
            query.contains("new york") || query.contains("ny") -> "Sunny"
            query.contains("sydney") -> "Rainy"
            query.contains("reykjavik") -> "Snowy"
            query.contains("snow") || query.contains("ice") || query.contains("freeze") -> "Snowy"
            query.contains("rain") || query.contains("drizzle") || query.contains("shower") -> "Rainy"
            query.contains("cloud") || query.contains("overcast") || query.contains("grey") -> "Cloudy"
            query.contains("storm") || query.contains("thunder") || query.contains("lightn") -> "Stormy"
            else -> {
                val hashValue = city.length % 5
                when (hashValue) {
                    0 -> "Sunny"
                    1 -> "Cloudy"
                    2 -> "Rainy"
                    3 -> "Stormy"
                    else -> "Snowy"
                }
            }
        }

        val tempCelsius = when (condition) {
            "Sunny" -> 23.5
            "Cloudy" -> 16.5
            "Rainy" -> 13.0
            "Stormy" -> 14.5
            "Snowy" -> -1.5
            else -> 17.0
        }

        val descriptionStr = when (condition) {
            "Sunny" -> "Warm sunbeams wash over the local horizon. The offline skies are perfectly clear and ambient."
            "Cloudy" -> "Gentle textured clouds float in peaceful equilibrium across the diorama block."
            "Rainy" -> "Serene rain details refresh the landscape. The atmosphere is crisp and wet."
            "Stormy" -> "Atmospheric storms pass over the city, casting a deep twilight blue sky effect."
            "Snowy" -> "Fine crisp snow carpets the landscape, reflecting soft light values."
            else -> "Simulating local weather parameters offline."
        }

        val skyColorStart = when (condition) {
            "Sunny" -> "#4BB4E8"
            "Cloudy" -> "#5C6B73"
            "Rainy" -> "#455A64"
            "Stormy" -> "#1E1B4B"
            "Snowy" -> "#78909C"
            else -> "#4BB4E8"
        }

        val skyColorEnd = when (condition) {
            "Sunny" -> "#BBE6FA"
            "Cloudy" -> "#90A4AE"
            "Rainy" -> "#78909C"
            "Stormy" -> "#312E81"
            "Snowy" -> "#ECEFF1"
            else -> "#BBE6FA"
        }

        val formattedCity = city.split(" ").joinToString(" ") { it.replaceFirstChar { c -> c.uppercase() } }

        val landmarks = mutableListOf<com.example.model.LandmarkItem>()

        if (query.contains("paris")) {
            landmarks.add(com.example.model.LandmarkItem("Eiffel Tower", "landmark", 35.0, 14.0, 78.0, 3.0, "#8D6E63", "tower"))
            landmarks.add(com.example.model.LandmarkItem("Arc de Triomphe", "landmark", 70.0, 18.0, 36.0, 2.0, "#B0BEC5", "arch"))
            landmarks.add(com.example.model.LandmarkItem("Louvre Glass Pyramid", "building", 15.0, 16.0, 24.0, 4.0, "#4FC3F7", "pyramid"))
        } else if (query.contains("tokyo")) {
            landmarks.add(com.example.model.LandmarkItem("Mount Fuji", "nature", 15.0, 40.0, 65.0, 1.0, "#37474F", "pyramid"))
            landmarks.add(com.example.model.LandmarkItem("Tokyo Tower", "landmark", 52.0, 12.0, 82.0, 3.0, "#FF7043", "tower"))
            landmarks.add(com.example.model.LandmarkItem("Kaminarimon Gate", "landmark", 78.0, 18.0, 38.0, 4.0, "#D32F2F", "arch"))
        } else if (query.contains("cairo")) {
            landmarks.add(com.example.model.LandmarkItem("Great Pyramid of Giza", "landmark", 28.0, 35.0, 60.0, 2.0, "#D7CCC8", "pyramid"))
            landmarks.add(com.example.model.LandmarkItem("Pyramid of Khafre", "landmark", 62.0, 30.0, 50.0, 1.0, "#CFD8DC", "pyramid"))
            landmarks.add(com.example.model.LandmarkItem("Cairo Tower Spire", "landmark", 86.0, 8.0, 75.0, 4.0, "#90A4AE", "tower"))
        } else if (query.contains("new york") || query.contains("ny")) {
            landmarks.add(com.example.model.LandmarkItem("Empire State Building", "landmark", 25.0, 14.0, 85.0, 1.0, "#90A4AE", "spire"))
            landmarks.add(com.example.model.LandmarkItem("One World Trade", "building", 55.0, 12.0, 80.0, 3.0, "#4FC3F7", "tower"))
            landmarks.add(com.example.model.LandmarkItem("Brooklyn Bridge Arch", "landmark", 80.0, 16.0, 45.0, 4.0, "#CFD8DC", "arch"))
        } else if (query.contains("sydney")) {
            landmarks.add(com.example.model.LandmarkItem("Sydney Opera House", "landmark", 35.0, 26.0, 38.0, 3.0, "#ECEFF1", "dome"))
            landmarks.add(com.example.model.LandmarkItem("Harbour Bridge", "landmark", 72.0, 22.0, 48.0, 2.0, "#78909C", "arch"))
        } else if (query.contains("reykjavik")) {
            landmarks.add(com.example.model.LandmarkItem("Hallgrímskirkja Spire", "landmark", 50.0, 16.0, 80.0, 2.0, "#ECEFF1", "spire"))
            landmarks.add(com.example.model.LandmarkItem("Perlan Dome", "landmark", 20.0, 18.0, 35.0, 4.0, "#26C6DA", "dome"))
        } else {
            val charSum = city.fold(0) { acc, c -> acc + c.code }
            val x1 = 18.0 + (charSum % 14)
            val x2 = 50.0 + (charSum % 12)
            val x3 = 80.0 - (charSum % 10)

            landmarks.add(com.example.model.LandmarkItem("Downtown Tower Block", "building", x1, 14.0, 75.0, 2.0, "#546E7A", "box"))
            landmarks.add(com.example.model.LandmarkItem("Regional Monument", "landmark", x2, 10.0, 80.0, 3.0, "#A1887F", "spire"))
            landmarks.add(com.example.model.LandmarkItem("Civic Canopy", "nature", x3, 22.0, 36.0, 4.0, "#388E3C", "dome"))
        }

        val hourlyList = listOf(
            com.example.model.HourlyForecast("12:00", tempCelsius - 1.0, condition),
            com.example.model.HourlyForecast("13:00", tempCelsius, condition),
            com.example.model.HourlyForecast("14:00", tempCelsius + 1.0, condition),
            com.example.model.HourlyForecast("15:00", tempCelsius, condition),
            com.example.model.HourlyForecast("16:00", tempCelsius - 1.0, condition)
        )

        val forecastList = listOf(
            com.example.model.DailyForecast("Mon", tempCelsius + 1.0, tempCelsius - 4.0, condition),
            com.example.model.DailyForecast("Tue", tempCelsius + 2.0, tempCelsius - 3.0, "Cloudy"),
            com.example.model.DailyForecast("Wed", tempCelsius - 1.0, tempCelsius - 5.0, "Rainy"),
            com.example.model.DailyForecast("Thu", tempCelsius, tempCelsius - 4.0, condition),
            com.example.model.DailyForecast("Fri", tempCelsius + 3.0, tempCelsius - 2.0, condition)
        )

        return com.example.model.WeatherResponse(
            cityName = "$formattedCity",
            weather = com.example.model.WeatherDetail(
                condition = condition,
                tempCelsius = tempCelsius,
                feelsLikeCelsius = tempCelsius + 1.0,
                humidity = 58,
                windSpeedKph = 13.8,
                description = descriptionStr,
                hourly = hourlyList,
                forecast = forecastList
            ),
            scenery = com.example.model.SceneryDetail(
                skyColorStart = skyColorStart,
                skyColorEnd = skyColorEnd,
                landmarks = landmarks
            ),
            isOfflineSimulated = true
        )
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
