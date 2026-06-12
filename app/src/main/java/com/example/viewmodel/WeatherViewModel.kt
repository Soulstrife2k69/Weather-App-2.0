package com.example.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.database.RecentCity
import com.example.database.RecentCityDatabase
import com.example.model.WeatherResponse
import com.example.repository.WeatherRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

sealed interface WeatherUiState {
    object Idle : WeatherUiState
    object Loading : WeatherUiState
    data class Success(val data: WeatherResponse) : WeatherUiState
    data class Error(val message: String) : WeatherUiState
}

class WeatherViewModel(
    application: Application,
    private val repository: WeatherRepository
) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow<WeatherUiState>(WeatherUiState.Idle)
    val uiState: StateFlow<WeatherUiState> = _uiState.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    val recentCities: StateFlow<List<RecentCity>> = repository.recentCities
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    fun onSearchQueryChanged(query: String) {
        _searchQuery.value = query
    }

    fun searchCity(city: String) {
        if (city.isBlank()) return
        
        viewModelScope.launch {
            _uiState.value = WeatherUiState.Loading
            try {
                val response = repository.fetchDioramaWeather(city)
                _uiState.value = WeatherUiState.Success(response)
                repository.saveCity(response.cityName)
            } catch (e: Exception) {
                _uiState.value = WeatherUiState.Error(e.localizeError().message ?: "An unexpected error occurred")
            }
        }
    }

    fun deleteRecentCity(cityName: String) {
        viewModelScope.launch {
            repository.removeCity(cityName)
        }
    }

    fun clearSearchHistory() {
        viewModelScope.launch {
            repository.clearHistory()
        }
    }

    private fun Throwable.localizeError(): Throwable {
        // Can unpack network errors here if needed
        return this
    }

    class Factory(
        private val application: Application,
        private val repository: WeatherRepository
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(WeatherViewModel::class.java)) {
                return WeatherViewModel(application, repository) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}
