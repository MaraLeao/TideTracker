package com.example.tidetracker

import android.graphics.Color
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.tidetracker.api.RetrofitInstance
import com.example.tidetracker.model.MarineWeatherResponse
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : AppCompatActivity() {
    private lateinit var waveHeightTextView: TextView
    private lateinit var wavePeriodTextView: TextView
    private lateinit var seaLevelHeightTextView: TextView
    private lateinit var surfaceTempTextView: TextView
    private lateinit var latitudeTextView: TextView
    private lateinit var longitudeTextView: TextView

    private lateinit var chartSeaLevel: LineChart
    private lateinit var chartWaveHeight: LineChart
    private lateinit var chartSST: LineChart

    private lateinit var updateHandler: Handler
    private lateinit var updateRunnable: Runnable
    private lateinit var latitude_input: EditText
    private lateinit var longitude_input: EditText

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        waveHeightTextView = findViewById(R.id.wave_height_TextView)
        wavePeriodTextView = findViewById(R.id.wave_period_TextView)
        seaLevelHeightTextView = findViewById(R.id.sea_level_height_TextView)
        surfaceTempTextView = findViewById(R.id.surface_temperature_TextView)
        latitudeTextView = findViewById(R.id.latitude_textView)
        longitudeTextView = findViewById(R.id.longitude_textView)
        chartSeaLevel = findViewById(R.id.chart_sea_level)
        chartWaveHeight = findViewById(R.id.chart_wave_height)
        chartSST = findViewById(R.id.chart_sst)

        fetchMarineWeatherData(-7.115, -34.882) // Praia de Tambaú coordenadas
        setupPeriodicUpdate()
    }

    private fun setupPeriodicUpdate() {
        updateHandler = Handler(Looper.getMainLooper())
        updateRunnable = object : Runnable {
            override fun run() {
                fetchMarineWeatherData(-7.115, -34.882)

                updateHandler.postDelayed(this, 3600000)
            }
        }
    }

    private fun fetchMarineWeatherData(latitude: Double, longitude: Double) {
        longitudeTextView.text = longitude.toString()
        latitudeTextView.text = latitude.toString()

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

                            val currentWaveHeight = waveHeight?.firstOrNull()
                            val currentWavePeriod = wavePeriod?.firstOrNull()
                            val currentSeaLevel = seaLevel?.firstOrNull()
                            val currentSST = sst?.firstOrNull()

                            waveHeightTextView.text = "${currentWaveHeight ?: "N/A"}"
                            wavePeriodTextView.text = "${currentWavePeriod ?: "N/A"}"
                            seaLevelHeightTextView.text = "${currentSeaLevel ?: "N/A"}"
                            surfaceTempTextView.text = "${currentSST ?: "N/A"}"

                            updateChart(time, waveHeight, wavePeriod, seaLevel, sst)
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

    private fun updateChart(
        time: List<String>,
        waveHeight: List<Double>?,
        wavePeriod: List<Double>?,
        seaLevel: List<Double>?,
        sst: List<Double>?
    ) {
        // Formatar horários para exibição (apenas hora:minuto)
        val timeLabels = time.map { timeString ->
            try {
                val inputFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm", Locale.getDefault())
                val outputFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
                val date = inputFormat.parse(timeString)
                outputFormat.format(date ?: Date())
            } catch (e: Exception) {
                timeString.substring(11, 16) // Pegar apenas HH:mm
            }
        }

        // Atualizar gráfico de altura das ondas
        waveHeight?.let { updateWaveHeightChart(timeLabels, it) }

        // Atualizar gráfico de nível do mar
        seaLevel?.let { updateSeaLevelChart(timeLabels, it) }

        // Atualizar gráfico de temperatura da superfície do mar
        sst?.let { updateSSTChart(timeLabels, it) }
    }

    private fun updateWaveHeightChart(timeLabels: List<String>, waveHeight: List<Double>) {
        val entries = waveHeight.mapIndexed { index, value ->
            Entry(index.toFloat(), value.toFloat())
        }

        val dataSet = LineDataSet(entries, "Altura das Ondas (m)")
        dataSet.color = Color.parseColor("#2196F3") // Azul
        dataSet.setCircleColor(Color.parseColor("#2196F3"))
        dataSet.lineWidth = 3f
        dataSet.circleRadius = 4f
        dataSet.setDrawFilled(true)
        dataSet.fillColor = Color.parseColor("#2196F3")
        dataSet.fillAlpha = 30
        dataSet.valueTextSize = 10f

        val lineData = LineData(dataSet)
        chartWaveHeight.data = lineData

        setupChart(chartWaveHeight, timeLabels, "Altura das Ondas (m)")
    }

    private fun updateSeaLevelChart(timeLabels: List<String>, seaLevel: List<Double>) {
        val entries = seaLevel.mapIndexed { index, value ->
            Entry(index.toFloat(), value.toFloat())
        }

        val dataSet = LineDataSet(entries, "Nível do Mar (m)")
        dataSet.color = Color.parseColor("#4CAF50") // Verde
        dataSet.setCircleColor(Color.parseColor("#4CAF50"))
        dataSet.lineWidth = 3f
        dataSet.circleRadius = 4f
        dataSet.setDrawFilled(true)
        dataSet.fillColor = Color.parseColor("#4CAF50")
        dataSet.fillAlpha = 30
        dataSet.valueTextSize = 10f

        val lineData = LineData(dataSet)
        chartSeaLevel.data = lineData

        setupChart(chartSeaLevel, timeLabels, "Nível do Mar (m)")
    }

    private fun updateSSTChart(timeLabels: List<String>, sst: List<Double>) {
        val entries = sst.mapIndexed { index, value ->
            Entry(index.toFloat(), value.toFloat())
        }

        val dataSet = LineDataSet(entries, "Temperatura (°C)")
        dataSet.color = Color.parseColor("#FF5722") // Vermelho/Laranja
        dataSet.setCircleColor(Color.parseColor("#FF5722"))
        dataSet.lineWidth = 3f
        dataSet.circleRadius = 4f
        dataSet.setDrawFilled(true)
        dataSet.fillColor = Color.parseColor("#FF5722")
        dataSet.fillAlpha = 30
        dataSet.valueTextSize = 10f

        val lineData = LineData(dataSet)
        chartSST.data = lineData

        setupChart(chartSST, timeLabels, "Temperatura da Superfície do Mar (°C)")
    }

    private fun setupChart(chart: LineChart, timeLabels: List<String>, title: String) {
        // Configurar eixo X (horizontal)
        val xAxis = chart.xAxis
        xAxis.position = XAxis.XAxisPosition.BOTTOM
        xAxis.granularity = 1f
        xAxis.setDrawGridLines(true)
        xAxis.gridColor = Color.LTGRAY

        // Mostrar apenas alguns horários para não poluir
        val step = maxOf(1, timeLabels.size / 8) // Mostrar ~8 labels
        val filteredLabels = timeLabels.filterIndexed { index, _ -> index % step == 0 }
        xAxis.labelCount = filteredLabels.size
        xAxis.valueFormatter = object : IndexAxisValueFormatter() {
            override fun getFormattedValue(value: Float): String {
                val index = value.toInt()
                return if (index >= 0 && index < timeLabels.size && index % step == 0) {
                    timeLabels[index]
                } else ""
            }
        }

        // Configurar eixo Y (vertical)
        chart.axisRight.isEnabled = false
        val leftAxis = chart.axisLeft
        leftAxis.setDrawGridLines(true)
        leftAxis.gridColor = Color.LTGRAY
        leftAxis.textColor = Color.DKGRAY

        // Configurações gerais do gráfico
        chart.description.isEnabled = false // Remover descrição padrão
        chart.legend.isEnabled = true
        chart.legend.textSize = 12f
        chart.setTouchEnabled(true)
        chart.isDragEnabled = true
        chart.setScaleEnabled(true)
        chart.setPinchZoom(true)
        chart.setDrawGridBackground(false)
        chart.setBackgroundColor(Color.WHITE)

        // Adicionar margem interna
        chart.setExtraOffsets(10f, 10f, 10f, 20f)

        // Animar gráfico
        chart.animateX(1000)
        chart.invalidate() // Refresh do gráfico
    }

}
