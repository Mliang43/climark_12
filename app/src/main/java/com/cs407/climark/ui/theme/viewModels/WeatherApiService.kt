package com.cs407.climark.ui.viewModels

import retrofit2.http.GET
import retrofit2.http.Query

data class WeatherApiResponse(
    val latitude: Double,
    val longitude: Double,
    val timezone: String,
    val daily: DailyData
)

data class DailyData(
    val time: List<String>,
    val temperature_2m_max: List<Double>,
    val temperature_2m_min: List<Double>
)

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
