package com.example.tidetracker

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.ImageButton
import android.widget.LinearLayout
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
import com.github.mikephil.charting.formatter.ValueFormatter
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.text.SimpleDateFormat
import java.util.*
import android.view.View

class MainActivity : AppCompatActivity() {
    private lateinit var waveHeightTextView: TextView
    private lateinit var wavePeriodTextView: TextView
    private lateinit var seaLevelHeightTextView: TextView
    private lateinit var surfaceTempTextView: TextView
    private lateinit var latitudeTextView: TextView
    private lateinit var longitudeTextView: TextView
    private lateinit var name: TextView
    private lateinit var bodyLayout: LinearLayout

    private lateinit var chartSeaLevel: LineChart
    private lateinit var chartSeaLevelDay: LineChart
    private lateinit var chartWaveHeight: LineChart
    private lateinit var chartWaveHeightDay: LineChart
    private lateinit var chartSST: LineChart
    private lateinit var chartSSTDay: LineChart

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
        chartSeaLevelDay = findViewById(R.id.chart_sea_level_day)
        chartWaveHeight = findViewById(R.id.chart_wave_height)
        chartWaveHeightDay = findViewById(R.id.chart_wave_height_day)
        chartSST = findViewById(R.id.chart_sst)
        chartSSTDay = findViewById(R.id.chart_sst_day)
        bodyLayout = findViewById<LinearLayout>(R.id.bodyProjectLayout)

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
                            // VERSÃO SIMPLIFICADA - apenas o essencial
                            Log.d("API_DEBUG", "=== TESTE SIMPLES ===")
                            Log.d("API_DEBUG", "Coordenadas: $latitude, $longitude")


                            var hasValidData = true

                            Log.w("MainActivity", "${it.hourly.wave_height}")
                            it.hourly.wave_height?.forEach { value ->
                                if (value == null) {
                                    hasValidData = false
                                }
                            }

                            val informPrincipal = findViewById<LinearLayout>(R.id.informPrincipal)
                            val informSecond = findViewById<LinearLayout>(R.id.informSecond)

                            if(!hasValidData) {
                                val msg = findViewById<TextView>(R.id.msg_beach_nf)
                                msg.text = "Não há praia nessa localização"
                                msg.visibility = View.VISIBLE
                                informPrincipal.visibility = View.GONE
                                informSecond.visibility = View.GONE
                                Log.w("MainActivity", "Dados insuficientes ou inexistentes para a localização")

                            } else {
                                informPrincipal.visibility = View.VISIBLE
                                informSecond.visibility = View.VISIBLE
                                val msg = findViewById<TextView>(R.id.msg_beach_nf)
                                msg.visibility = View.GONE

                                val time = it.hourly.time
                                val waveHeight = it.hourly.wave_height
                                val wavePeriod = it.hourly.wave_period
                                val seaLevel = it.hourly.sea_level_height_msl
                                val sst = it.hourly.sea_surface_temperature

                                // CORREÇÃO: Função helper para conversão segura
                                fun safeDouble(value: Any?): Double? {
                                    return try {
                                        value?.let {
                                            when (it) {
                                                is Number -> it.toDouble()
                                                is String -> it.toDoubleOrNull()
                                                else -> null
                                            }
                                        }
                                    } catch (e: Exception) {
                                        null
                                    }
                                }

                                // Acesso seguro aos primeiros valores - CORREÇÃO AQUI
                                val currentWaveHeight = waveHeight?.firstOrNull()?.let { safeDouble(it) }
                                val currentWavePeriod = wavePeriod?.firstOrNull()?.let { safeDouble(it) }
                                val currentSeaLevel = seaLevel?.firstOrNull()?.let { safeDouble(it) }
                                val currentSST = sst?.firstOrNull()?.let { safeDouble(it) }

                                // Formatação segura para exibição
                                waveHeightTextView.text = if (currentWaveHeight != null) {
                                    "${String.format("%.1f", currentWaveHeight)}m"
                                } else {
                                    "N/A"
                                }

                                wavePeriodTextView.text = if (currentWavePeriod != null) {
                                    "${String.format("%.1f", currentWavePeriod)}s"
                                } else {
                                    "N/A"
                                }

                                seaLevelHeightTextView.text = if (currentSeaLevel != null) {
                                    "${String.format("%.1f", currentSeaLevel)}m"
                                } else {
                                    "N/A"
                                }

                                surfaceTempTextView.text = if (currentSST != null) {
                                    "${String.format("%.1f", currentSST)}°C"
                                } else {
                                    "N/A"
                                }

                                // CRUCIAL: Filtrar valores null antes de passar para updateChart - CORREÇÃO
                                val validTime = time ?: emptyList()
                                val validWaveHeight = waveHeight?.mapNotNull { safeDouble(it) } ?: emptyList()
                                val validSeaLevel = seaLevel?.mapNotNull { safeDouble(it) } ?: emptyList()
                                val validSST = sst?.mapNotNull { safeDouble(it) } ?: emptyList()

                                Log.d("API_DEBUG", "Passing to chart - validWaveHeight size: ${validWaveHeight.size}")
                                Log.d("API_DEBUG", "Passing to chart - validSeaLevel size: ${validSeaLevel.size}")
                                Log.d("API_DEBUG", "Passing to chart - validSST size: ${validSST.size}")

                                // Só chama updateChart se tiver dados válidos
                                if (validWaveHeight.isNotEmpty() || validSeaLevel.isNotEmpty() || validSST.isNotEmpty()) {
                                    try {
                                        updateChart(validTime, validWaveHeight, validSeaLevel, validSST)
                                    } catch (e: Exception) {
                                        Log.e("MainActivity", "Erro ao atualizar chart", e)
                                    }
                                } else {
                                    Log.w("MainActivity", "Nenhum dado válido para o gráfico")
                                }
                            }
                        }
                    } else {
                        Log.e("MainActivity", "Response error: ${response.code()}")
                    }
                }

                override fun onFailure(call: Call<MarineWeatherResponse>, t: Throwable) {
                    Log.e("MainActivity", "API call error", t)
                    bodyLayout.visibility = View.GONE
                    val msg = findViewById<TextView>(R.id.msg_beach_nf)
                    msg.text = "Erro ao carregar dados. Verifique sua conexão ou tente novamente."
                    msg.visibility = View.VISIBLE
                }
            })
    }

    private fun updateChart(
        time: List<String>,
        waveHeight: List<Double>,
        seaLevel: List<Double>,
        sst: List<Double>
    ) {

        waveHeight?.let { updateWaveHeightChart(time, it) }
        seaLevel?.let { updateSeaLevelChart(time, it) }
        sst?.let { updateSSTChart(time, it) }

        val dailyData = filterForToday(time, waveHeight, seaLevel, sst)
        dailyData.waveHeight?.let {
            updateWaveHeightChartDay(dailyData.time, it)
        }
        dailyData.seaLevel?.let {
            updateSeaLevelChartDay(dailyData.time, it)
        }
        dailyData.sst?.let {
            updateSSTChartDay(dailyData.time, it)
        }
    }

    private fun updateWaveHeightChart(time: List<String>, waveHeight: List<Double>) {
        val entries = waveHeight.mapIndexedNotNull { index, value ->
            Entry(index.toFloat(), value.toFloat())
        }

        val dataSet = LineDataSet(entries, "Altura das Ondas (m)")
        dataSet.color = Color.parseColor("#2196F3")
        dataSet.lineWidth = 1f
        dataSet.circleRadius = 4f
        dataSet.setDrawFilled(true)
        dataSet.fillColor = Color.parseColor("#2196F3")
        dataSet.fillAlpha = 30
        dataSet.valueTextSize = 10f
        dataSet.setDrawCircles(false)
        dataSet.setDrawValues(true)
        dataSet.valueFormatter = object : ValueFormatter() {
            override fun getFormattedValue(value: Float): String? {
                return String.format("%.2f", value)
            }
        }

        val lineData = LineData(dataSet)
        chartWaveHeight.data = lineData

        setupChart(chartWaveHeight, time, false)
    }

    private fun updateWaveHeightChartDay(time: List<String>, waveHeight: List<Double>) {
        val entries = waveHeight.mapIndexedNotNull { index, value ->
            Entry(index.toFloat(), value.toFloat())
        }

        val dataSet = LineDataSet(entries, "Altura das Ondas (m)")
        dataSet.color = Color.parseColor("#2196F3")
        dataSet.lineWidth = 1f
        dataSet.circleRadius = 4f
        dataSet.setDrawFilled(true)
        dataSet.fillColor = Color.parseColor("#2196F3")
        dataSet.fillAlpha = 30
        dataSet.valueTextSize = 10f
        dataSet.setDrawCircles(false)
        dataSet.setDrawValues(true)
        dataSet.valueFormatter = object : ValueFormatter() {
            override fun getFormattedValue(value: Float): String? {
                return String.format("%.2f", value)
            }
        }

        val lineData = LineData(dataSet)
        chartWaveHeightDay.data = lineData

        setupChart(chartWaveHeightDay, time, true)
    }

    private fun updateSeaLevelChart(time: List<String>, seaLevel: List<Double>) {
        val entries = seaLevel.mapIndexedNotNull { index, value ->
            Entry(index.toFloat(), value.toFloat())
        }

        val dataSet = LineDataSet(entries, "Nível do Mar (m)")
        dataSet.color = Color.parseColor("#4CAF50")
        dataSet.lineWidth = 1f
        dataSet.circleRadius = 4f
        dataSet.setDrawFilled(true)
        dataSet.fillColor = Color.parseColor("#4CAF50")
        dataSet.fillAlpha = 30
        dataSet.valueTextSize = 10f
        dataSet.setDrawCircles(false)
        dataSet.setDrawValues(true)
        dataSet.valueFormatter = object : ValueFormatter() {
            override fun getFormattedValue(value: Float): String? {
                return String.format("%.2f", value)
            }
        }

        val lineData = LineData(dataSet)
        chartSeaLevel.data = lineData

        setupChart(chartSeaLevel, time, false)
    }

    private fun updateSeaLevelChartDay(time: List<String>, seaLevel: List<Double>) {
        val entries = seaLevel.mapIndexed { index, value ->
            Entry(index.toFloat(), value.toFloat())
        }

        val dataSet = LineDataSet(entries, "Nível do Mar (m)")
        dataSet.color = Color.parseColor("#4CAF50")
        dataSet.lineWidth = 1f
        dataSet.circleRadius = 4f
        dataSet.setDrawFilled(true)
        dataSet.fillColor = Color.parseColor("#4CAF50")
        dataSet.fillAlpha = 30
        dataSet.valueTextSize = 10f
        dataSet.setDrawCircles(false)
        dataSet.setDrawValues(true)
        dataSet.valueFormatter = object : ValueFormatter() {
            override fun getFormattedValue(value: Float): String? {
                return String.format("%.2f", value)
            }
        }

        val lineData = LineData(dataSet)
        chartSeaLevelDay.data = lineData

        setupChart(chartSeaLevelDay, time, true)
    }

    private fun updateSSTChart(time: List<String>, sst: List<Double>) {
        val entries = sst.mapIndexedNotNull { index, value ->
            Entry(index.toFloat(), value.toFloat())
        }

        val dataSet = LineDataSet(entries, "Temperatura (°C)")
        dataSet.color = Color.parseColor("#FF5722")
        dataSet.lineWidth = 1f
        dataSet.circleRadius = 4f
        dataSet.setDrawFilled(true)
        dataSet.fillColor = Color.parseColor("#FF5722")
        dataSet.fillAlpha = 30
        dataSet.valueTextSize = 10f
        dataSet.setDrawCircles(false)
        dataSet.setDrawValues(true)
        dataSet.valueFormatter = object : ValueFormatter() {
            override fun getFormattedValue(value: Float): String? {
                return String.format("%.1f", value)
            }
        }

        val lineData = LineData(dataSet)
        chartSST.data = lineData

        setupChart(chartSST, time, false)
    }

    private fun updateSSTChartDay(time: List<String>, sst: List<Double>) {
        val entries = sst.mapIndexed { index, value ->
            Entry(index.toFloat(), value.toFloat())
        }

        val dataSet = LineDataSet(entries, "Temperatura (°C)")
        dataSet.color = Color.parseColor("#FF5722")
        dataSet.lineWidth = 1f
        dataSet.circleRadius = 4f
        dataSet.setDrawFilled(true)
        dataSet.fillColor = Color.parseColor("#FF5722")
        dataSet.fillAlpha = 30
        dataSet.valueTextSize = 10f
        dataSet.setDrawCircles(false)
        dataSet.setDrawValues(true)
        dataSet.valueFormatter = object : ValueFormatter() {
            override fun getFormattedValue(value: Float): String? {
                return String.format("%.1f", value)
            }
        }

        val lineData = LineData(dataSet)
        chartSSTDay.data = lineData

        setupChart(chartSSTDay, time, true)
    }

    private fun filterForToday(
        allTime: List<String>,
        allWaveHeight: List<Double>?,
        allSeaLevel: List<Double>?,
        allSST: List<Double>?
    ): DailyFilteredData {
        val todayCalendar = Calendar.getInstance()
        val inputFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm", Locale.getDefault())

        val dailyTime = mutableListOf<String>()
        val dailyWaveHeight = mutableListOf<Double>()
        val dailySeaLevel = mutableListOf<Double>()
        val dailySST = mutableListOf<Double>()

        for (i in allTime.indices) {
            try {
                val date = inputFormat.parse(allTime[i])
                val itemCalendar = Calendar.getInstance()
                itemCalendar.time = date ?: Date()

                if (itemCalendar.get(Calendar.DAY_OF_MONTH) == todayCalendar.get(Calendar.DAY_OF_MONTH) &&
                    itemCalendar.get(Calendar.MONTH) == todayCalendar.get(Calendar.MONTH) &&
                    itemCalendar.get(Calendar.YEAR) == todayCalendar.get(Calendar.YEAR)) {

                    dailyTime.add(allTime[i])
                    allWaveHeight?.getOrNull(i)?.let { dailyWaveHeight.add(it) }
                    allSeaLevel?.getOrNull(i)?.let { dailySeaLevel.add(it) }
                    allSST?.getOrNull(i)?.let { dailySST.add(it) }
                }

            } catch (e: Exception) {
                Log.e("MainActivity", "Erro ao parsear data para filtro: ${e.message}")
            }
        }

        return DailyFilteredData(
            time = dailyTime,
            waveHeight = dailyWaveHeight.takeIf { it.isNotEmpty() },
            seaLevel = dailySeaLevel.takeIf { it.isNotEmpty() },
            sst = dailySST.takeIf { it.isNotEmpty() }
        )
    }

    data class DailyFilteredData(
        val time: List<String>,
        val waveHeight: List<Double>?,
        val seaLevel: List<Double>?,
        val sst: List<Double>?
    )

    private fun setupChart(chart: LineChart, time: List<String>, isDailyChart: Boolean) {
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

                        return if (isDailyChart) {
                            val outputFormat = SimpleDateFormat("HH", Locale.getDefault())
                            outputFormat.format(date ?: Date())

                        } else {
                            val day = calendar.get(Calendar.DAY_OF_MONTH)
                            val monthValue = calendar.get(Calendar.MONTH) + 1

                            val monthName = when (monthValue) {
                                1 -> "jan"; 2 -> "fev"; 3 -> "mar"; 4 -> "abr"
                                5 -> "mai"; 6 -> "jun"; 7 -> "jul"; 8 -> "ago"
                                9 -> "set"; 10 -> "out"; 11 -> "nov"; 12 -> "dez"
                                else -> "mês$monthValue"
                            }

                            "$day $monthName"
                        }

                    } catch (e: Exception) {
                            Log.e("Chart", "Erro ao formatar data: ${e.message}")
                            return "Data $index"
                    }
                }
                return ""
            }
        }

        if (isDailyChart) {
            xAxis.setLabelCount(12, false)
            xAxis.setAvoidFirstLastClipping(true)
        } else {
            xAxis.setLabelCount(6, false)
            xAxis.setAvoidFirstLastClipping(true)
        }

        // axis Y
        chart.axisRight.isEnabled = false
        val leftAxis = chart.axisLeft
        leftAxis.setDrawGridLines(true)
        leftAxis.gridColor = Color.LTGRAY
        leftAxis.textColor = Color.DKGRAY
        leftAxis.valueFormatter = object : ValueFormatter() {
            override fun getFormattedValue(value: Float): String? {
                return String.format("%.2f", value)

            }
        }

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

        chart.setBackgroundColor(if (isDarkTheme) Color.parseColor("#232323") else Color.parseColor("#EDEDED"))
        val textColor = if (isDarkTheme) Color.WHITE else Color.DKGRAY
        chart.legend.textColor = textColor
        chart.axisLeft.textColor = textColor
        chart.xAxis.textColor = textColor

        chart.setExtraOffsets(10f, 10f, 10f, 10f)

        chart.animateX(1000)
        chart.invalidate()
    }
}
