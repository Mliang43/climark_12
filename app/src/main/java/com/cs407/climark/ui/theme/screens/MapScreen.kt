package com.cs407.climark.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.cs407.climark.R
import com.cs407.climark.ui.viewModels.MapViewModel
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.*
import kotlinx.coroutines.launch
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MapScreen(viewModel: MapViewModel = viewModel()) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val defaultLocation = LatLng(43.0731, -89.4012)
    var addMode by remember { mutableStateOf(false) }
    var deleteMode by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    val cameraState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(defaultLocation, 12f)
    }

    LaunchedEffect(Unit) { viewModel.initializeLocationClient(context) }

    LaunchedEffect(uiState.currentLocation) {
        uiState.currentLocation?.let { location ->
            cameraState.animate(CameraUpdateFactory.newLatLngZoom(location, 15f))
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        viewModel.updateLocationPermission(granted)
        if (granted) viewModel.getCurrentLocation(context)
    }

    LaunchedEffect(Unit) {
        val granted = ContextCompat.checkSelfPermission(
            context, Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        if (granted) {
            viewModel.updateLocationPermission(true)
            viewModel.getCurrentLocation(context)
        } else permissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
    }

    Box(modifier = Modifier.fillMaxSize()) {

        GoogleMap(
            modifier = Modifier.fillMaxSize(),
            cameraPositionState = cameraState,
            properties = MapProperties(isMyLocationEnabled = uiState.locationPermissionGranted),
            onMapClick = { latLng ->
                viewModel.clearWeather() // Hide weather card when map tapped
                when {
                    addMode -> {
                        viewModel.addMarker(latLng)
                        addMode = false
                    }
                    deleteMode -> {
                        viewModel.removeMarker(latLng)
                        deleteMode = false
                    }
                }
            }
        ) {
            // Current location marker
            uiState.currentLocation?.let { loc ->
                MarkerComposable(
                    state = MarkerState(position = loc),
                    content = {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .background(Color.Blue, shape = CircleShape)
                                .border(3.dp, Color.White, CircleShape)
                        )
                    }
                )
            }

            // User-added markers
            uiState.markers.forEach { m ->
                Marker(
                    state = MarkerState(position = m),
                    title = "Marker (${m.latitude.format(2)}, ${m.longitude.format(2)})",
                    onClick = {
                        if (deleteMode) {
                            viewModel.removeMarker(m)
                            deleteMode = false
                            true
                        } else {
                            // Fetch weather when marker tapped
                            viewModel.fetchWeather(m.latitude, m.longitude)
                            coroutineScope.launch {
                                cameraState.animate(CameraUpdateFactory.newLatLngZoom(m, 12f))
                            }
                            true
                        }
                    }
                )
            }
        }

        // Floating Action Buttons
        Column(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(bottom = 120.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            FloatingActionButton(onClick = {
                viewModel.clearWeather()
                addMode = true
                coroutineScope.launch {
                    snackbarHostState.showSnackbar("Tap anywhere on the map to add a marker")
                }
            }) {
                Icon(Icons.Filled.Add, contentDescription = "Add", modifier = Modifier.size(50.dp))
            }

            FloatingActionButton(onClick = {
                viewModel.clearWeather()
                deleteMode = true
                coroutineScope.launch {
                    snackbarHostState.showSnackbar("Tap a marker to delete or tap anywhere to cancel")
                }
            }) {
                Icon(Icons.Filled.Delete, contentDescription = "Delete", modifier = Modifier.size(50.dp))
            }

            FloatingActionButton(onClick = {
                viewModel.clearWeather()
                uiState.currentLocation?.let { location ->
                    coroutineScope.launch {
                        cameraState.animate(CameraUpdateFactory.newLatLngZoom(location, 15f))
                    }
                }
            }) {
                Icon(Icons.Filled.LocationOn, contentDescription = "Your Location", modifier = Modifier.size(50.dp))
            }
        }

        // ✅ New Weather Info Card with Forecast Icons
        if (uiState.isLoading) {
            CircularProgressIndicator(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(16.dp)
            )
        } else if (uiState.weatherData != null && uiState.currentLocation != null) {
            // take non-null locals (safe because of the if condition)
            val current = uiState.currentLocation!!
            val weather = uiState.weatherData!!
            val daily = weather.daily
            Card(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(12.dp)
                    .fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFF5F9FF)),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
            ) {
                Column(Modifier.padding(12.dp)) {
                    Text(
                        text = "Location: %.2f°N %.2f°E".format(current.latitude, current.longitude),
                        style = MaterialTheme.typography.titleMedium,
                        color = Color.Black
                    )
                    Spacer(Modifier.height(8.dp))

                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        val days = daily.time.take(5)
                        val maxTemps = daily.temperature_2m_max.take(5)
                        val minTemps = daily.temperature_2m_min.take(5)
                        val precip = daily.precipitation_probability_mean?.take(5) ?: List(5) { 0.0 }

                        items(days.indices.toList()) { i ->
                            WeatherDayCard(
                                day = days[i],
                                maxTemp = maxTemps[i],
                                minTemp = minTemps[i],
                                precipitation = precip[i]
                            )
                        }
                    }
                }
            }
        }

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 50.dp)
        )
    }
}

private fun Double.format(d: Int) = "%.${d}f".format(this)

// ✅ Card for each weather day
@Composable
fun WeatherDayCard(
    day: String,
    maxTemp: Double,
    minTemp: Double,
    precipitation: Double
) {
    val iconRes = when {
        precipitation > 60 -> R.drawable.thunderstorm
        precipitation in 40.0..60.0 -> R.drawable.rainy
        precipitation in 20.0..40.0 -> R.drawable.partlycloudy
        precipitation in 5.0..20.0 -> R.drawable.foggy
        precipitation in 1.0..5.0 -> R.drawable.drizzle
        else -> R.drawable.sunny
    }

    Card(
        modifier = Modifier
            .width(85.dp)
            .padding(4.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(6.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(day.takeLast(2).uppercase(), style = MaterialTheme.typography.bodySmall, color = Color.Gray)
            Spacer(Modifier.height(4.dp))
            Icon(
                painter = painterResource(id = iconRes),
                contentDescription = "Weather Icon",
                tint = Color.Unspecified,
                modifier = Modifier.size(36.dp)
            )
            Spacer(Modifier.height(4.dp))
            Text("${precipitation.toInt()}%", style = MaterialTheme.typography.bodySmall)
            Spacer(Modifier.height(2.dp))
            Text("${maxTemp.toInt()}°", style = MaterialTheme.typography.bodyMedium, color = Color.Black)
            Text("${minTemp.toInt()}°", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
        }
    }
}
