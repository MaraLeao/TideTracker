package com.example.tidetracker

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.ImageButton
import android.widget.TextView
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
    private lateinit var name: TextView

    private lateinit var chartSeaLevel: LineChart
    private lateinit var chartWaveHeight: LineChart
    private lateinit var chartSST: LineChart

    private lateinit var updateHandler: Handler
    private lateinit var updateRunnable: Runnable

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
        name = findViewById(R.id.name_local)
        chartSeaLevel = findViewById(R.id.chart_sea_level)
        chartWaveHeight = findViewById(R.id.chart_wave_height)
        chartSST = findViewById(R.id.chart_sst)
        val button_menu = findViewById<ImageButton>(R.id.btn_menu)
        val latitude = intent.getDoubleExtra("latitude", -7.115)
        val longitude = intent.getDoubleExtra("longitude", -34.882)
        val localName = intent.getStringExtra("name") ?: "Praia de Tambaú"

        name.text = localName

        fetchMarineWeatherData(latitude, longitude)
        setupPeriodicUpdate(latitude, longitude)

        button_menu.setOnClickListener {
            val intent = Intent(this, LocalsActivity::class.java)
            startActivity(intent)
        }
    }

    private fun setupPeriodicUpdate(latitude: Double, longitude: Double) {
        updateHandler = Handler(Looper.getMainLooper())
        updateRunnable = object : Runnable {
            override fun run() {
                fetchMarineWeatherData(latitude, longitude)

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

                            waveHeightTextView.text = "${currentWaveHeight ?: "N/A"} m"
                            wavePeriodTextView.text = "${currentWavePeriod ?: "N/A"} s"
                            seaLevelHeightTextView.text = "${currentSeaLevel ?: "N/A"} m"
                            surfaceTempTextView.text = "${currentSST ?: "N/A"} °C"

                            updateChart(time, waveHeight, seaLevel, sst)
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
        seaLevel: List<Double>?,
        sst: List<Double>?
    ) {

        waveHeight?.let { updateWaveHeightChart(time, it) }

        seaLevel?.let { updateSeaLevelChart(time, it) }

        sst?.let { updateSSTChart(time, it) }
    }

    private fun updateWaveHeightChart(time: List<String>, waveHeight: List<Double>) {
        val entries = waveHeight.mapIndexed { index, value ->
            Entry(index.toFloat(), value.toFloat())
        }

        val dataSet = LineDataSet(entries, "Altura das Ondas (m)")
        dataSet.color = Color.parseColor("#2196F3") // Azul
        dataSet.lineWidth = 3f
        dataSet.circleRadius = 4f
        dataSet.setDrawFilled(true)
        dataSet.fillColor = Color.parseColor("#2196F3")
        dataSet.fillAlpha = 30
        dataSet.valueTextSize = 10f
        dataSet.setDrawCircles(false)

        val lineData = LineData(dataSet)
        chartWaveHeight.data = lineData

        setupChart(chartWaveHeight, time)
    }

    private fun updateSeaLevelChart(time: List<String>, seaLevel: List<Double>) {
        val entries = seaLevel.mapIndexed { index, value ->
            Entry(index.toFloat(), value.toFloat())
        }

        val dataSet = LineDataSet(entries, "Nível do Mar (m)")
        dataSet.color = Color.parseColor("#4CAF50")
        dataSet.lineWidth = 3f
        dataSet.circleRadius = 4f
        dataSet.setDrawFilled(true)
        dataSet.fillColor = Color.parseColor("#4CAF50")
        dataSet.fillAlpha = 30
        dataSet.valueTextSize = 10f
        dataSet.setDrawCircles(false)

        val lineData = LineData(dataSet)
        chartSeaLevel.data = lineData

        setupChart(chartSeaLevel, time)
    }

    private fun updateSSTChart(time: List<String>, sst: List<Double>) {
        val entries = sst.mapIndexed { index, value ->
            Entry(index.toFloat(), value.toFloat())
        }

        val dataSet = LineDataSet(entries, "Temperatura (°C)")
        dataSet.color = Color.parseColor("#FF5722")
        dataSet.lineWidth = 3f
        dataSet.circleRadius = 4f
        dataSet.setDrawFilled(true)
        dataSet.fillColor = Color.parseColor("#FF5722")
        dataSet.fillAlpha = 30
        dataSet.valueTextSize = 10f
        dataSet.setDrawCircles(false)

        val lineData = LineData(dataSet)
        chartSST.data = lineData

        setupChart(chartSST, time)
    }

    private fun setupChart(chart: LineChart, time: List<String>) {
        val xAxis = chart.xAxis
        xAxis.position = XAxis.XAxisPosition.BOTTOM
        xAxis.granularity = 1f
        xAxis.setDrawGridLines(true)
        xAxis.gridColor = Color.LTGRAY
        xAxis.setDrawLabels(true)
        xAxis.textSize = 10f

        xAxis.valueFormatter = object : IndexAxisValueFormatter() {
            override fun getFormattedValue(value: Float): String {
                val index = value.toInt()
                if (index >= 0 && index < time.size) {
                    val originalTime = time[index]

                    try {
                        val inputFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm", Locale.getDefault())
                        val date = inputFormat.parse(originalTime)
                        val calendar = Calendar.getInstance()
                        calendar.time = date ?: Date()

                        val day = calendar.get(Calendar.DAY_OF_MONTH)
                        val monthValue = calendar.get(Calendar.MONTH) + 1

                        val monthName = when (monthValue) {
                            1 -> "jan"; 2 -> "fev"; 3 -> "mar"; 4 -> "abr"
                            5 -> "mai"; 6 -> "jun"; 7 -> "jul"; 8 -> "ago"
                            9 -> "set"; 10 -> "out"; 11 -> "nov"; 12 -> "dez"
                            else -> "mês$monthValue"
                        }

                        return "$day $monthName"

                    } catch (e: Exception) {
                        Log.e("Chart", "Erro ao formatar data: ${e.message}")
                        return "Data $index"
                    }
                }
                return ""
            }
        }

        xAxis.setLabelCount(6, false)
        xAxis.setAvoidFirstLastClipping(true)
        xAxis.spaceMin = 0.5f
        xAxis.spaceMax = 0.5f


        // axis Y
        chart.axisRight.isEnabled = false
        val leftAxis = chart.axisLeft
        leftAxis.setDrawGridLines(true)
        leftAxis.gridColor = Color.LTGRAY
        leftAxis.textColor = Color.DKGRAY

        // General configuration
        chart.description.isEnabled = false
        chart.legend.isEnabled = true
        chart.legend.textSize = 12f
        chart.setTouchEnabled(true)
        chart.isDragEnabled = true
        chart.setScaleEnabled(true)
        chart.setPinchZoom(true)
        chart.setDrawGridBackground(false)

        val isDarkTheme = resources.configuration.uiMode and
                android.content.res.Configuration.UI_MODE_NIGHT_MASK ==
                android.content.res.Configuration.UI_MODE_NIGHT_YES

        chart.setBackgroundColor(if (isDarkTheme) Color.parseColor("#0c0c1c") else Color.parseColor("#d8d9e6"))
        val textColor = if (isDarkTheme) Color.WHITE else Color.DKGRAY
        chart.legend.textColor = textColor
        chart.axisLeft.textColor = textColor
        chart.xAxis.textColor = textColor

        chart.setExtraOffsets(10f, 10f, 10f, 60f)

        chart.animateX(1000)
        chart.invalidate()
    }
}
