package com.cs407.climark.ui.viewModels

import android.annotation.SuppressLint
import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.maps.model.LatLng
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Query

data class WeatherInfo(
    val temperature: Double?,
    val description: String?
)

data class MapState(
    val currentLocation: LatLng? = null,
    val locationPermissionGranted: Boolean = false,
    val isLoading: Boolean = false,
    val weather: WeatherInfo? = null,
    val error: String? = null
)

interface WeatherService {
    @GET("weather")
    suspend fun getWeather(
        @Query("lat") lat: Double,
        @Query("lon") lon: Double,
        @Query("appid") key: String,
        @Query("units") units: String = "metric"
    ): WeatherResponse
}

data class WeatherResponse(
    val main: Main,
    val weather: List<WeatherDesc>
)
data class Main(val temp: Double)
data class WeatherDesc(val description: String)

class MapViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(MapState())
    val uiState = _uiState.asStateFlow()

    private var fusedLocationClient: FusedLocationProviderClient? = null

    private val retrofit = Retrofit.Builder()
        .baseUrl("https://api.openweathermap.org/data/2.5/")
        .addConverterFactory(GsonConverterFactory.create())
        .build()
    private val service = retrofit.create(WeatherService::class.java)

    fun initializeLocationClient(context: Context) {
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)
    }

    fun updateLocationPermission(granted: Boolean) {
        _uiState.value = _uiState.value.copy(locationPermissionGranted = granted)
    }

    @SuppressLint("MissingPermission")
    fun getCurrentLocation(context: Context) {
        val client = fusedLocationClient ?: LocationServices.getFusedLocationProviderClient(context)
        _uiState.value = _uiState.value.copy(isLoading = true)

        client.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, null)
            .addOnSuccessListener { loc ->
                if (loc != null) {
                    val latLng = LatLng(loc.latitude, loc.longitude)
                    _uiState.value = _uiState.value.copy(
                        currentLocation = latLng,
                        isLoading = false
                    )
                    fetchWeather(latLng)
                } else {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = "Location unavailable"
                    )
                }
            }
            .addOnFailureListener {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = "Error getting location: ${it.message}"
                )
            }
    }

    private fun fetchWeather(latLng: LatLng) {
        viewModelScope.launch {
            try {
                val response = service.getWeather(
                    latLng.latitude,
                    latLng.longitude,
                    "YOUR_OPENWEATHERMAP_API_KEY"  // 🔑 Replace with real key
                )
                _uiState.value = _uiState.value.copy(
                    weather = WeatherInfo(
                        temperature = response.main.temp,
                        description = response.weather.firstOrNull()?.description
                    )
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(error = "Weather fetch failed: ${e.message}")
            }
        }
    }
}
