package com.example.tidetracker
import com.example.tidetracker.api.RetrofitInstance
import com.example.tidetracker.model.MarineWeatherResponse

import android.os.Bundle
import android.util.Log
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import android.widget.Toast
import android.os.Handler
import android.os.Looper

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        fetchMarineWeatherData(-7.115, -34.882) // Praia de Tambaú coordenadas
    }

    private fun fetchMarineWeatherData(latitude : Double, longitude: Double) {

        RetrofitInstance.api.getMarineWeather(latitude, longitude)
            .enqueue(object : Callback<MarineWeatherResponse> {
                override fun onResponse(
                    call: Call<MarineWeatherResponse>,
                    response: Response<MarineWeatherResponse>
                ) {
                    if (response.isSuccessful) {
                        val data = response.body()
                        data?.let {
                            val time = it.hourly.time
                            val waveHeight = it.hourly.wave_height
                            val wavePeriod = it.hourly.wave_period
                            val seaLevel = it.hourly.sea_level_height_msl
                            val sst = it.hourly.sea_surface_temperature

                            if (time.isNotEmpty()) {
                                var hasValidData = false

                                for (i in time.indices) {
                                    val timeValue = time[i]
                                    val waveHeightValue = waveHeight?.getOrNull(i)
                                    val wavePeriodValue = wavePeriod?.getOrNull(i)
                                    val seaLevelValue = seaLevel?.getOrNull(i)
                                    val sstValue = sst?.getOrNull(i)

                                    if (waveHeightValue != null || wavePeriodValue != null ||
                                        seaLevelValue != null || sstValue != null) {
                                        hasValidData = true
                                    }

                                    Log.d("MainActivity",
                                        "Hora: $timeValue, " +
                                                "Altura da onda: ${waveHeightValue ?: "N/A"} m, " +
                                                "Período: ${wavePeriodValue ?: "N/A"} s, " +
                                                "Maré: ${seaLevelValue ?: "N/A"} m, " +
                                                "SST: ${sstValue ?: "N/A"} °C"
                                    )
                                }

                                if (!hasValidData) {
                                    Log.w("MainActivity", "No valid marine data available for this location")
                                    Handler(Looper.getMainLooper()).post {
                                        Toast.makeText(applicationContext,
                                            "Dados marinhos não disponíveis para esta localização.",
                                            Toast.LENGTH_LONG).show()
                                    }
                                } else {
                                    Log.d("MainActivity", "Valid marine data found!")
                                }
                            } else {
                                Log.w("MainActivity", "No time data received")
                            }
                        }
                    } else {
                        Log.e("MainActivity", "Response error: ${response.code()}")
                    }
                }

                override fun onFailure(call: Call<MarineWeatherResponse>, t: Throwable) {
                    Log.e("MainActivity", "API call error", t)
                }
            })
    }
}
