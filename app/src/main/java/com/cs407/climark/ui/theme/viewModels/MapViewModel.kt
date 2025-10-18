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
import java.net.HttpURLConnection
import java.net.URL

data class MapState(
    val currentLocation: LatLng? = null,
    val locationPermissionGranted: Boolean = false,
    val isLoading: Boolean = false,
    val weatherInfo: String? = null,
    val error: String? = null
)

class MapViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(MapState())
    val uiState = _uiState.asStateFlow()

    private var fusedLocationClient: FusedLocationProviderClient? = null

    private fun initClient(context: Context) {
        if (fusedLocationClient == null)
            fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)
    }

    fun updateLocationPermission(granted: Boolean) {
        _uiState.value = _uiState.value.copy(locationPermissionGranted = granted)
    }

    @SuppressLint("MissingPermission")
    fun getCurrentLocation(context: Context) {
        initClient(context)
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            fusedLocationClient?.lastLocation?.addOnSuccessListener { location ->
                val coords = if (location != null)
                    LatLng(location.latitude, location.longitude)
                else
                    LatLng(43.0731, -89.4012) // fallback Madison

                _uiState.value = _uiState.value.copy(currentLocation = coords)
                fetchWeather(coords.latitude, coords.longitude)
            }?.addOnFailureListener { e ->
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = e.message
                )
            }
        }
    }

    private fun fetchWeather(latitude: Double, longitude: Double) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            try {
                val response = RetrofitInstance.weatherApi.getWeatherData(latitude, longitude)
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    weatherInfo = "Lat: ${response.latitude}, Lon: ${response.longitude}\n" +
                            "Max temps: ${response.daily.temperature_2m_max}\n" +
                            "Min temps: ${response.daily.temperature_2m_min}"
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

