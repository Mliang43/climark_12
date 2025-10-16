package com.cs407.climark.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MapScreen(viewModel: MapViewModel = viewModel()) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var addMode by remember { mutableStateOf(false) }
    var deleteMode by remember { mutableStateOf(false) }

    val cameraState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(LatLng(43.0731, -89.4012), 12f)
    }

    LaunchedEffect(Unit) { viewModel.initializeLocationClient(context) }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        viewModel.updateLocationPermission(granted)
        if (granted) viewModel.getCurrentLocation()
    }

    LaunchedEffect(Unit) {
        val granted = ContextCompat.checkSelfPermission(
            context, Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        if (granted) {
            viewModel.updateLocationPermission(true)
            viewModel.getCurrentLocation()
        } else permissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
    }

    Box(modifier = Modifier.fillMaxSize()) {
        GoogleMap(
            modifier = Modifier.fillMaxSize(),
            cameraPositionState = cameraState,
            properties = MapProperties(isMyLocationEnabled = uiState.locationPermissionGranted),
            onMapClick = { latLng ->
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
                    title = "Marker (${m.latitude.format(2)}, ${m.longitude.format(2)})"
                )
            }
        }

        // Floating buttons
        Column(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            FloatingActionButton(onClick = {
                uiState.currentLocation?.let {
                    cameraState.animate(CameraUpdateFactory.newLatLngZoom(it, 15f))
                }
            }) { Text("🎯") }

            FloatingActionButton(onClick = { addMode = true }) { Text("+") }
            FloatingActionButton(onClick = { deleteMode = true }) { Text("🗑") }
        }
    }
}

private fun Double.format(d: Int) = "%.${d}f".format(this)
