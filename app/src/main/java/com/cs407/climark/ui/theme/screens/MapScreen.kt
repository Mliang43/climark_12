package com.cs407.climark.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MyLocation
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
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MarkerComposable
import com.google.maps.android.compose.MarkerState
import com.google.maps.android.compose.rememberCameraPositionState

@Composable
fun MapScreen(viewModel: MapViewModel = viewModel()) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { result ->
        val granted = result[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
                result[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        viewModel.updateLocationPermission(granted)
        if (granted) viewModel.getCurrentLocation(context)
    }

    LaunchedEffect(Unit) {
        val fine = ContextCompat.checkSelfPermission(
            context, Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        if (fine) {
            viewModel.updateLocationPermission(true)
            viewModel.getCurrentLocation(context)
        } else {
            launcher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
        }
    }

    val default = LatLng(43.0731, -89.4012)
    val camera = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(default, 12f)
    }

    // Animate camera when location updates
    LaunchedEffect(uiState.currentLocation) {
        uiState.currentLocation?.let {
            camera.animate(CameraUpdateFactory.newLatLngZoom(it, 14f))
        }
    }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(onClick = {
                val t = uiState.currentLocation ?: default
                // move() is not suspend → safe here
                camera.move(CameraUpdateFactory.newLatLngZoom(t, 14f))
            }) {
                Icon(Icons.Default.MyLocation, contentDescription = "Center")
            }
        }
    ) { pad ->
        Box(Modifier.fillMaxSize().padding(pad)) {
            GoogleMap(
                modifier = Modifier.fillMaxSize(),
                cameraPositionState = camera
            ) {
                uiState.currentLocation?.let { loc ->
                    MarkerComposable(state = MarkerState(loc)) {
                        Box(
                            Modifier
                                .size(40.dp)
                                .background(Color(0xFF1976D2), CircleShape)
                                .border(3.dp, Color.White, CircleShape)
                        )
                    }
                }
            }

            uiState.weather?.let { w ->
                Card(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(16.dp)
                        .fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color.White)
                ) {
                    Column(
                        Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("Temperature: ${w.temperature?.toInt()}°C")
                        Text("Condition: ${w.description}")
                    }
                }
            }
        }
    }
}
