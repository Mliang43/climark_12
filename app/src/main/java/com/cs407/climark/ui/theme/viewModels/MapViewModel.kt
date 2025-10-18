package com.cs407.climark.ui.viewModels

import android.annotation.SuppressLint
import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.model.LatLng
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import retrofit2.http.GET
import retrofit2.http.Query
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

// ---------- Retrofit data + service ----------

data class WeatherApiResponse(
    val latitude: Double,
    val longitude: Double,
    val timezone: String,
    val daily: DailyData
)

data class DailyData(
    val time: List<String>,
    val temperature_2m_max: List<Double>,
    val temperature_2m_min: List<Double>,
    val precipitation_probability_mean: List<Double>?
)

interface WeatherApiService {
    @GET("v1/forecast")
    suspend fun getWeatherData(
        @Query("latitude") latitude: Double,
        @Query("longitude") longitude: Double,
        @Query("daily")
        daily: String =
            "temperature_2m_max,temperature_2m_min,precipitation_probability_mean",
        @Query("timezone") timezone: String = "auto",
        @Query("past_days") pastDays: Int = 2,
        @Query("forecast_days") forecastDays: Int = 2
    ): WeatherApiResponse
}

object RetrofitInstance {
    private const val BASE_URL = "https://api.open-meteo.com/"
    val weatherApi: WeatherApiService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(WeatherApiService::class.java)
    }
}

// ---------- UI State ----------

data class MapState(
    val markers: List<LatLng> = emptyList(),
    val currentLocation: LatLng? = null,
    val locationPermissionGranted: Boolean = false,
    val isLoading: Boolean = false,
    val weatherInfo: String? = null,
    val error: String? = null
)

// ---------- ViewModel ----------

class MapViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(MapState())
    val uiState = _uiState.asStateFlow()

    private var fusedLocationClient: FusedLocationProviderClient? = null

    fun initializeLocationClient(context: Context) {
        if (fusedLocationClient == null)
            fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)
    }

    fun updateLocationPermission(granted: Boolean) {
        _uiState.value = _uiState.value.copy(locationPermissionGranted = granted)
    }

    fun clearWeather() {
        _uiState.value = _uiState.value.copy(weatherInfo = null)
    }

    @SuppressLint("MissingPermission")
    fun getCurrentLocation(context: Context) {
        initializeLocationClient(context)
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            fusedLocationClient?.lastLocation?.addOnSuccessListener { location ->
                val coords = if (location != null)
                    LatLng(location.latitude, location.longitude)
                else
                    LatLng(43.0731, -89.4012) // fallback Madison

                _uiState.value = _uiState.value.copy(currentLocation = coords, isLoading = false)
            }?.addOnFailureListener { e ->
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = e.message
                )
            }
        }
    }

    fun addMarker(latLng: LatLng) {
        val updated = _uiState.value.markers + latLng
        _uiState.value = _uiState.value.copy(markers = updated)
    }

    fun removeMarker(latLng: LatLng) {
        val updated = _uiState.value.markers.filterNot {
            it.latitude == latLng.latitude && it.longitude == latLng.longitude
        }
        _uiState.value = _uiState.value.copy(markers = updated)
    }

    // ---------- Fetch weather via Retrofit ----------
    fun fetchWeather(latitude: Double, longitude: Double) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            try {
                val response = withContext(Dispatchers.IO) {
                    RetrofitInstance.weatherApi.getWeatherData(latitude, longitude)
                }

                val daily = response.daily
                val info = buildString {
                    appendLine("Location: %.2f, %.2f".format(response.latitude, response.longitude))
                    appendLine("Days: ${daily.time.take(4).joinToString()}")
                    appendLine("Max Temps: ${daily.temperature_2m_max.take(4).joinToString()}")
                    appendLine("Min Temps: ${daily.temperature_2m_min.take(4).joinToString()}")
                    appendLine("Precipitation: ${daily.precipitation_probability_mean?.take(4)?.joinToString()}")
                }

                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    weatherInfo = info
                )

            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = "Error fetching weather: ${e.message}"
                )
            }
        }
    }
}
