package com.example.openmeteotest

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                WeatherScreen()
            }
        }
    }
}

@Composable
fun WeatherScreen() {
    var weatherData by remember { mutableStateOf("Loading...") }
    var isLoading by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        isLoading = true
        weatherData = fetchWeatherData()
        isLoading = false
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("Open-Meteo Weather Data", style = MaterialTheme.typography.titleLarge)
        if (isLoading) {
            CircularProgressIndicator()
        } else {
            Text(weatherData, style = MaterialTheme.typography.bodyMedium)
        }
    }
}

/**
 * Fetch weather data from Open-Meteo API using standard HttpURLConnection.
 */
suspend fun fetchWeatherData(): String = withContext(Dispatchers.IO) {
    val urlString =
        "https://api.open-meteo.com/v1/forecast?latitude=52.52&longitude=13.41&hourly=temperature_2m"

    return@withContext try {
        val url = URL(urlString)
        val connection = url.openConnection() as HttpURLConnection
        connection.requestMethod = "GET"
        connection.connectTimeout = 5000
        connection.readTimeout = 5000

        val responseCode = connection.responseCode
        val response = connection.inputStream.bufferedReader().use { it.readText() }

        connection.disconnect()
        if (responseCode == HttpURLConnection.HTTP_OK) {
            response
        } else {
            "Error: HTTP $responseCode\n$response"
        }
    } catch (e: Exception) {
        "Error fetching data: ${e.message}"
    }
}
