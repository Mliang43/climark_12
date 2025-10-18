package com.cs407.climark.ui.viewModels

import android.annotation.SuppressLint
import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.model.LatLng
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

// --------- Retrofit imports ---------
import retrofit2.http.GET
import retrofit2.http.Query

// --------- Retrofit data models ---------
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

// --------- Retrofit API service ---------
interface WeatherApiService {
    @GET("v1/forecast")
    suspend fun getWeatherData(
        @Query("latitude") latitude: Double,
        @Query("longitude") longitude: Double,
        @Query("daily") daily: String = "temperature_2m_max,temperature_2m_min,precipitation_probability_mean",
        @Query("timezone") timezone: String = "auto",
        @Query("past_days") pastDays: Int = 3,
        @Query("forecast_days") forecastDays: Int = 4
    ): WeatherApiResponse
}

// --------- Retrofit instance ---------
object RetrofitInstance {
    private const val BASE_URL = "https://api.open-meteo.com/"
    val weatherApi: WeatherApiService by lazy {
        retrofit2.Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(retrofit2.converter.gson.GsonConverterFactory.create())
            .build()
            .create(WeatherApiService::class.java)
    }
}

// --------- Map state ---------
data class MapState(
    val currentLocation: LatLng? = null,
    val locationPermissionGranted: Boolean = false,
    val isLoading: Boolean = false,
    val weatherData: WeatherApiResponse? = null,
    val markers: List<LatLng> = emptyList(),
    val error: String? = null
)

// --------- ViewModel ---------
class MapViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(MapState())
    val uiState = _uiState.asStateFlow()

    private var fusedLocationClient: FusedLocationProviderClient? = null

    fun initializeLocationClient(context: Context) {
        if (fusedLocationClient == null) {
            fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)
        }
    }

    fun updateLocationPermission(granted: Boolean) {
        _uiState.value = _uiState.value.copy(locationPermissionGranted = granted)
    }

    fun clearWeather() {
        _uiState.value = _uiState.value.copy(weatherData = null)
    }

    fun addMarker(latLng: LatLng) {
        val updatedMarkers = _uiState.value.markers + latLng
        _uiState.value = _uiState.value.copy(markers = updatedMarkers)
    }

    fun removeMarker(latLng: LatLng) {
        val updatedMarkers = _uiState.value.markers.filterNot { it == latLng }
        _uiState.value = _uiState.value.copy(markers = updatedMarkers)
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
                    LatLng(43.0731, -89.4012) // fallback: Madison

                _uiState.value = _uiState.value.copy(
                    currentLocation = coords,
                    isLoading = false
                )
            }?.addOnFailureListener { e ->
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = e.message
                )
            }
        }
    }

    fun fetchWeather(latitude: Double, longitude: Double) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            try {
                val response = RetrofitInstance.weatherApi.getWeatherData(latitude, longitude)
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    weatherData = response
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = e.message
                )
            }
        }
    }
}
