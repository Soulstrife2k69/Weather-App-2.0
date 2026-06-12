package com.example

import android.app.Application
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.database.RecentCityDatabase
import com.example.model.LandmarkItem
import com.example.model.WeatherResponse
import com.example.repository.WeatherRepository
import com.example.ui.DioramaCanvas
import com.example.ui.getHighlightedColor
import com.example.ui.getShadedColor
import com.example.ui.parseHexColor
import com.example.viewmodel.WeatherUiState
import com.example.viewmodel.WeatherViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MaterialTheme(
                colorScheme = darkColorScheme(
                    primary = Color(0xFF38BDF8), // Ocean Blue
                    secondary = Color(0xFF0EA5E9),
                    tertiary = Color(0xFFF1F5F9),
                    background = Color(0xFF020617), // Deepest twilight navy
                    surface = Color(0xFF0F172A),
                    onBackground = Color(0xFFF8FAFC),
                    onSurface = Color(0xFFF8FAFC)
                )
            ) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    WeatherDioramaApp()
                }
            }
        }
    }
}

@Composable
fun WeatherDioramaApp() {
    val context = LocalContext.current
    val application = context.applicationContext as Application

    // Instantiate Room Database and Repository
    val database = remember { RecentCityDatabase.getDatabase(context) }
    val repository = remember { WeatherRepository(database.recentCityDao()) }

    // Instantiate ViewModel
    val viewModel: WeatherViewModel = viewModel(
        factory = WeatherViewModel.Factory(application, repository)
    )

    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val recentCities by viewModel.recentCities.collectAsStateWithLifecycle()

    var selectedLandmark by remember { mutableStateOf<LandmarkItem?>(null) }

    // Whenever city changes, reset the tapped selected landmark info
    LaunchedEffect(uiState) {
        selectedLandmark = null
    }

    // Default Preset Locations corresponding to distinct iconic environments in prompt description
    val presets = listOf(
        "Paris", "Tokyo", "Cairo", "New York", "Sydney", "Reykjavik"
    )

    // Trigger initial search for Paris if idle
    LaunchedEffect(Unit) {
        viewModel.searchCity("Paris")
    }

    Scaffold(
        modifier = Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeDrawing),
        topBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Place,
                    contentDescription = "Place Icon",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(34.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = "Diorama Weather",
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Text(
                        text = "Interactive 3D Perspective Scenery",
                        fontSize = 11.sp,
                        color = Color.White.copy(alpha = 0.55f)
                    )
                }
            }
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 1. Search Bar Input
            item {
                Column {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { viewModel.onSearchQueryChanged(it) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("city_search_bar"),
                        placeholder = { Text("Search location (e.g. Cairo, Sydney)") },
                        leadingIcon = {
                            Icon(Icons.Default.Search, contentDescription = "SearchIcon")
                        },
                        trailingIcon = {
                            if (searchQuery.isNotEmpty()) {
                                IconButton(onClick = { viewModel.onSearchQueryChanged("") }) {
                                    Icon(Icons.Default.Close, contentDescription = "Clear")
                                }
                            }
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(16.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = Color.White.copy(alpha = 0.2f),
                            focusedContainerColor = MaterialTheme.colorScheme.surface,
                            unfocusedContainerColor = MaterialTheme.colorScheme.surface
                        )
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        Button(
                            onClick = { viewModel.searchCity(searchQuery) },
                            enabled = searchQuery.isNotBlank(),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.testTag("search_button"),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary,
                                disabledContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)
                            )
                        ) {
                            Text("Generate Scene", color = Color.Black, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            // 2. Preset Quick Chips
            item {
                Column {
                    Text(
                        text = "PRESET CLIMATES & LANDMARKS",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        letterSpacing = 1.sp,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )

                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(presets) { preset ->
                            SuggestionChip(
                                onClick = {
                                    viewModel.onSearchQueryChanged(preset)
                                    viewModel.searchCity(preset)
                                },
                                label = { Text(preset, fontWeight = FontWeight.Medium) },
                                colors = SuggestionChipDefaults.suggestionChipColors(
                                    labelColor = Color.White,
                                    containerColor = MaterialTheme.colorScheme.surface
                                ),
                                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.12f)),
                                shape = RoundedCornerShape(10.dp)
                            )
                        }
                    }
                }
            }

            // Main UI Switch for API results
            when (val state = uiState) {
                is WeatherUiState.Idle -> {
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                        ) {
                            Text(
                                text = "Enter a city to explore its diorama atmosphere.",
                                modifier = Modifier.padding(24.dp),
                                textAlign = TextAlign.Center,
                                color = Color.White.copy(alpha = 0.7f)
                            )
                        }
                    }
                }

                is WeatherUiState.Loading -> {
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            shape = RoundedCornerShape(20.dp)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(48.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                                Spacer(modifier = Modifier.height(20.dp))
                                Text(
                                    text = "Synthesizing Weather Ambiance...",
                                    fontWeight = FontWeight.Medium,
                                    color = Color.White
                                )
                                Text(
                                    text = "Drawing 3D geometries and sky gradients...",
                                    fontSize = 12.sp,
                                    color = Color.White.copy(alpha = 0.5f),
                                    modifier = Modifier.padding(top = 4.dp)
                                )
                            }
                        }
                    }
                }

                is WeatherUiState.Error -> {
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFF450A0A)),
                            border = BorderStroke(1.dp, Color(0xFFEF4444)),
                            shape = RoundedCornerShape(20.dp)
                        ) {
                            Column(
                                modifier = Modifier.padding(24.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Info,
                                    contentDescription = "ErrorIcon",
                                    tint = Color(0xFFF87171),
                                    modifier = Modifier.size(40.dp)
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                                Text(
                                    text = "Synthesis Stopped",
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFFFCA5A5)
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = state.message,
                                    color = Color(0xFFFECACA),
                                    fontSize = 14.sp,
                                    textAlign = TextAlign.Center
                                )

                                if (state.message.contains("API Key is missing")) {
                                    Spacer(modifier = Modifier.height(16.dp))
                                    Card(
                                        colors = CardDefaults.cardColors(containerColor = Color(0x33000000)),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Column(modifier = Modifier.padding(12.dp)) {
                                            Text(
                                                text = "How to add Gemini API Key:",
                                                fontSize = 13.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                color = Color.White
                                            )
                                            Text(
                                                text = "1. Locate the Secrets panel in the AI Studio sidebar.\n" +
                                                        "2. Add a secret key named GEMINI_API_KEY.\n" +
                                                        "3. Paste your valid Gemini API Key from Google AI Studio.\n" +
                                                        "4. The builder will automatically hot-reload and instantiate the service.",
                                                fontSize = 11.sp,
                                                color = Color.White.copy(alpha = 0.8f),
                                                modifier = Modifier.padding(top = 4.dp),
                                                lineHeight = 16.sp
                                            )

                                            // Highlight caution warning from skill guidelines!
                                            Text(
                                                text = "⚠️ APK Key Warning: Please carry caution when sharing this debug build as keys located in BuildConfig can be decompiled.",
                                                fontSize = 10.sp,
                                                color = Color(0xFFFCD34D),
                                                fontWeight = FontWeight.Medium,
                                                modifier = Modifier.padding(top = 8.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                is WeatherUiState.Success -> {
                    val data = state.data

                    // 3. DIORAMA CARD SCREEN
                    item {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(270.dp)
                                .testTag("diorama_canvas_card"),
                            shape = RoundedCornerShape(24.dp),
                            border = BorderStroke(1.5.dp, Color.White.copy(alpha = 0.15f))
                        ) {
                            Box(modifier = Modifier.fillMaxSize()) {
                                DioramaCanvas(
                                    weatherData = data,
                                    selectedLandmark = selectedLandmark,
                                    onLandmarkSelected = { selectedLandmark = it }
                                )

                                // City Title & Condition Overlay on sky
                                Column(
                                    modifier = Modifier
                                        .align(Alignment.TopStart)
                                        .padding(20.dp)
                                        .background(Color.Black.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
                                        .padding(horizontal = 12.dp, vertical = 6.dp)
                                ) {
                                    Text(
                                        text = data.cityName,
                                        fontSize = 18.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.padding(top = 2.dp)
                                    ) {
                                        Text(
                                            text = getConditionEmoji(data.weather.condition),
                                            fontSize = 14.sp
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = "${data.weather.condition} • ${data.weather.tempCelsius}°C",
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Medium,
                                            color = Color.White.copy(alpha = 0.9f)
                                        )
                                    }
                                }

                                // Interactive Prompt to Tap Diorama Landmarks
                                Box(
                                    modifier = Modifier
                                        .align(Alignment.BottomEnd)
                                        .padding(14.dp)
                                        .background(Color.Black.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                                        .padding(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            Icons.Default.Info,
                                            contentDescription = "info",
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(12.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = "Tap landmarks below",
                                            fontSize = 9.sp,
                                            color = Color.White
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // 4. SELECTED LANDMARK SPECS CARD
                    item {
                        AnimatedVisibility(
                            visible = selectedLandmark != null,
                            enter = expandVertically() + fadeIn(),
                            exit = shrinkVertically() + fadeOut()
                        ) {
                            selectedLandmark?.let { item ->
                                val baseCol = parseHexColor(item.colorHex, Color.Gray)
                                Card(
                                    modifier = Modifier.fillMaxWidth().testTag("landmark_detail_card"),
                                    shape = RoundedCornerShape(16.dp),
                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(16.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        // Miniature model outline color
                                        Box(
                                            modifier = Modifier
                                                .size(45.dp)
                                                .background(baseCol, RoundedCornerShape(12.dp))
                                                .border(2.dp, Color.White, RoundedCornerShape(12.dp)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = when (item.shapeType.lowercase()) {
                                                    "tower" -> "🗼"
                                                    "dome" -> "🕌"
                                                    "arch" -> "⛩️"
                                                    "pyramid" -> "🔺"
                                                    "spire" -> "🔸"
                                                    else -> "🏢"
                                                },
                                                fontSize = 20.sp
                                            )
                                        }

                                        Spacer(modifier = Modifier.width(16.dp))

                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = item.name,
                                                fontSize = 16.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = Color.White
                                            )
                                            Text(
                                                text = "Type: ${item.type.replaceFirstChar { it.uppercase() }} • Shape: ${item.shapeType.replaceFirstChar { it.uppercase() }}",
                                                fontSize = 12.sp,
                                                color = Color.White.copy(alpha = 0.6f)
                                            )
                                            Text(
                                                text = "3D Depth Layer: ${item.depth} (Overlaps other forms)",
                                                fontSize = 11.sp,
                                                color = MaterialTheme.colorScheme.primary
                                            )
                                        }

                                        IconButton(onClick = { selectedLandmark = null }) {
                                            Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White.copy(alpha = 0.5f))
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // 5. CURRENT WEATHER REPORT DETAILS
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            shape = RoundedCornerShape(20.dp)
                        ) {
                            Column(modifier = Modifier.padding(20.dp)) {
                                Text(
                                    text = "WEATHER REPORT",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary,
                                    letterSpacing = 1.sp
                                )

                                Spacer(modifier = Modifier.height(10.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text(
                                            text = "${data.weather.tempCelsius}°C",
                                            fontSize = 44.sp,
                                            fontWeight = FontWeight.Black,
                                            color = Color.White
                                        )
                                        Text(
                                            text = "Feels like ${data.weather.feelsLikeCelsius}°C",
                                            fontSize = 14.sp,
                                            color = Color.White.copy(alpha = 0.6f)
                                        )
                                    }

                                    Column(horizontalAlignment = Alignment.End) {
                                        Text(
                                            text = getConditionEmoji(data.weather.condition),
                                            fontSize = 40.sp
                                        )
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = data.weather.condition,
                                            fontSize = 16.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(16.dp))

                                HorizontalDivider(color = Color.White.copy(alpha = 0.08f))

                                Spacer(modifier = Modifier.height(16.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    WeatherParamItem(
                                        icon = "💧",
                                        label = "HUMIDITY",
                                        value = "${data.weather.humidity}%"
                                    )
                                    WeatherParamItem(
                                        icon = "💨",
                                        label = "WIND SPEED",
                                        value = "${data.weather.windSpeedKph} km/h"
                                    )
                                }

                                Spacer(modifier = Modifier.height(12.dp))

                                Text(
                                    text = data.weather.description,
                                    fontSize = 13.sp,
                                    color = Color.White.copy(alpha = 0.8f),
                                    lineHeight = 18.sp,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(Color.White.copy(alpha = 0.03f), RoundedCornerShape(10.dp))
                                        .padding(12.dp)
                                )
                            }
                        }
                    }

                    // 6. 5-HOUR FORECAST SECTION
                    item {
                        Column {
                            Text(
                                text = "5-HOUR FORECAST",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                                letterSpacing = 1.sp,
                                modifier = Modifier.padding(bottom = 10.dp)
                            )

                            LazyRow(
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                items(data.weather.hourly) { hour ->
                                    Card(
                                        shape = RoundedCornerShape(16.dp),
                                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.04f)),
                                        modifier = Modifier.width(85.dp)
                                    ) {
                                        Column(
                                            modifier = Modifier.padding(12.dp),
                                            horizontalAlignment = Alignment.CenterHorizontally
                                        ) {
                                            Text(
                                                text = hour.time,
                                                fontSize = 12.sp,
                                                color = Color.White.copy(alpha = 0.6f)
                                            )
                                            Spacer(modifier = Modifier.height(6.dp))
                                            Text(
                                                text = getConditionEmoji(hour.condition),
                                                fontSize = 24.sp
                                            )
                                            Spacer(modifier = Modifier.height(6.dp))
                                            Text(
                                                text = "${hour.temp}°",
                                                fontSize = 15.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = Color.White
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // 7. 5-DAY TRANSIT FORECAST
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            shape = RoundedCornerShape(20.dp)
                        ) {
                            Column(modifier = Modifier.padding(20.dp)) {
                                Text(
                                    text = "5-DAY FORECAST",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary,
                                    letterSpacing = 1.sp
                                )

                                Spacer(modifier = Modifier.height(14.dp))

                                data.weather.forecast.forEachIndexed { i, day ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 10.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(
                                            text = day.day,
                                            fontSize = 15.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White,
                                            modifier = Modifier.width(55.dp)
                                        )

                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier.width(110.dp)
                                        ) {
                                            Text(
                                                text = getConditionEmoji(day.condition),
                                                fontSize = 18.sp
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(
                                                text = day.condition,
                                                fontSize = 13.sp,
                                                color = Color.White.copy(alpha = 0.8f),
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }

                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text(
                                                text = "${day.high.toInt()}°",
                                                fontSize = 14.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = Color.White
                                            )
                                            Spacer(modifier = Modifier.width(10.dp))
                                            Text(
                                                text = "${day.low.toInt()}°",
                                                fontSize = 14.sp,
                                                color = Color.White.copy(alpha = 0.4f)
                                            )
                                        }
                                    }

                                    if (i < data.weather.forecast.size - 1) {
                                        HorizontalDivider(color = Color.White.copy(alpha = 0.05f))
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // 8. SEARCH PERSISTED HISTORY (ROOM DATABASE)
            item {
                Column(modifier = Modifier.padding(top = 10.dp, bottom = 24.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "SEARCH HISTORY",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            letterSpacing = 1.sp
                        )

                        if (recentCities.isNotEmpty()) {
                            TextButton(
                                onClick = { viewModel.clearSearchHistory() },
                                colors = ButtonDefaults.textButtonColors(contentColor = Color(0xFFF87171))
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Delete, contentDescription = "clear_all", modifier = Modifier.size(13.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Clear", fontSize = 11.sp)
                                }
                            }
                        }
                    }

                    if (recentCities.isEmpty()) {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.4f)),
                            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.03f))
                        ) {
                            Text(
                                text = "Your search history is empty. Try looking up Cairo or Riyadh!",
                                modifier = Modifier.padding(16.dp),
                                fontSize = 12.sp,
                                textAlign = TextAlign.Center,
                                color = Color.White.copy(alpha = 0.45f)
                            )
                        }
                    } else {
                        Column(
                            verticalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            recentCities.forEach { city ->
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            viewModel.onSearchQueryChanged(city.cityName)
                                            viewModel.searchCity(city.cityName)
                                        },
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(
                                                imageVector = Icons.Default.Place,
                                                contentDescription = "location_history",
                                                tint = Color.White.copy(alpha = 0.35f),
                                                modifier = Modifier.size(16.dp)
                                            )
                                            Spacer(modifier = Modifier.width(10.dp))
                                            Text(
                                                text = city.cityName,
                                                fontSize = 14.sp,
                                                color = Color.White,
                                                fontWeight = FontWeight.Medium
                                            )
                                        }

                                        IconButton(
                                            onClick = { viewModel.deleteRecentCity(city.cityName) },
                                            modifier = Modifier.size(24.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Close,
                                                contentDescription = "delete_one",
                                                tint = Color.White.copy(alpha = 0.4f),
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// --- Parameter Presentation Components ---

@Composable
fun WeatherParamItem(icon: String, label: String, value: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(vertical = 4.dp)
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .background(Color.White.copy(alpha = 0.05f), RoundedCornerShape(10.dp)),
            contentAlignment = Alignment.Center
        ) {
            Text(text = icon, fontSize = 18.sp)
        }
        Spacer(modifier = Modifier.width(10.dp))
        Column {
            Text(
                text = label,
                fontSize = 10.sp,
                color = Color.White.copy(alpha = 0.4f),
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.5.sp
            )
            Text(
                text = value,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        }
    }
}

// Map condition texts to colorful dynamic emojis securely and cleanly!
fun getConditionEmoji(condition: String): String {
    val clean = condition.lowercase()
    return when {
        clean.contains("sunny") || clean.contains("clear") -> "☀️"
        clean.contains("cloud") || clean.contains("overcast") -> "☁️"
        clean.contains("storm") || clean.contains("thunder") -> "⛈️"
        clean.contains("rain") || clean.contains("drizzle") || clean.contains("shower") -> "🌧️"
        clean.contains("snow") || clean.contains("ice") || clean.contains("freeze") -> "❄️"
        clean.contains("wind") || clean.contains("breeze") -> "💨"
        clean.contains("fog") || clean.contains("mist") || clean.contains("haze") -> "🌫️"
        else -> "⛅"
    }
}
