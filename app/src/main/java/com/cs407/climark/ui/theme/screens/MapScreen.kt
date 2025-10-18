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
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.cs407.climark.ui.viewModels.MapViewModel
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.*
import com.google.maps.android.compose.*
import kotlinx.coroutines.launch

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
                // Step 6: Hide weather card when map is tapped
                viewModel.clearWeather()

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
            // current location marker
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

            // user-added markers
            uiState.markers.forEach { m ->
                Marker(
                    state = MarkerState(position = m),
                    title = "Marker (${m.latitude.format(2)}, ${m.longitude.format(2)})",
                    onClick = {
                        if (deleteMode) {
                            viewModel.removeMarker(m)
                            deleteMode = false
                            true // consume click
                        } else {
                            // Step 5: Fetch weather when a marker is tapped
                            viewModel.fetchWeather(m.latitude, m.longitude)
                            coroutineScope.launch {
                                cameraState.animate(CameraUpdateFactory.newLatLngZoom(m, 12f))
                            }
                            true // consume click to show weather card
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
                viewModel.clearWeather() // Step 6: hide card on FAB click
                addMode = true
                coroutineScope.launch {
                    snackbarHostState.showSnackbar("Tap anywhere on the map to add a marker")
                }
            }) {
                Icon(
                    imageVector = Icons.Filled.Add,
                    contentDescription = "Add",
                    modifier = Modifier.size(50.dp)
                )
            }

            FloatingActionButton(onClick = {
                viewModel.clearWeather() // Step 6
                deleteMode = true
                coroutineScope.launch {
                    snackbarHostState.showSnackbar("Tap a marker to delete or tap anywhere to cancel")
                }
            }) {
                Icon(
                    imageVector = Icons.Filled.Delete,
                    contentDescription = "Delete",
                    modifier = Modifier.size(50.dp)
                )
            }

            FloatingActionButton(onClick = {
                viewModel.clearWeather() // Step 6
                uiState.currentLocation?.let { location ->
                    coroutineScope.launch {
                        cameraState.animate(CameraUpdateFactory.newLatLngZoom(location, 15f))
                    }
                }
            }) {
                Icon(
                    imageVector = Icons.Filled.LocationOn,
                    contentDescription = "Your Location",
                    modifier = Modifier.size(50.dp)
                )
            }
        }

        // Weather Info Card or Loading Spinner
        if (uiState.weatherInfo != null && !uiState.isLoading) {
            Card(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(12.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFE3F2FD))
            ) {
                Column(Modifier.padding(16.dp)) {
                    Text("Weather at marker", style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(8.dp))
                    Text(uiState.weatherInfo ?: "")
                }
            }
        } else if (uiState.isLoading) {
            CircularProgressIndicator(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(16.dp)
            )
        }

        // Snackbar
        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 50.dp)
        )
    }
}

private fun Double.format(d: Int) = "%.${d}f".format(this)
