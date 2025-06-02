package com.example.tidetracker

import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class LocalsActivity : AppCompatActivity() {
    private lateinit var viewLocals: LinearLayout
    private lateinit var listLocals: MutableList<Localidade>
    private lateinit var btnAddLoc: Button
    private lateinit var sharedPreferences: SharedPreferences
    private lateinit var btn_x : ImageButton
    private val gson = Gson()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.locals_activity)

        viewLocals =  findViewById(R.id.viewLocals)
        btnAddLoc = findViewById<Button>(R.id.btn_addLoc)
        listLocals = mutableListOf()
        btn_x = findViewById(R.id.btn_x)

        sharedPreferences = getSharedPreferences("locals_prefs", MODE_PRIVATE)

        btnAddLoc.setOnClickListener {
            dialogAddLocal()
        }

        btn_x.setOnClickListener {
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
        }

        loadLocals()
    }

    private fun loadLocals() {
        val localsJson = sharedPreferences.getString("localidades", "[]")
        val type = object : TypeToken<List<Localidade>>() {}.type
        val localsSaved: List<Localidade> = gson.fromJson(localsJson, type)

        listLocals.clear()
        listLocals.addAll(localsSaved)

        showLocals()
    }

    private fun saveLocals() {
        val localsJson = gson.toJson(listLocals)
        sharedPreferences.edit()
            .putString("localidades", localsJson)
            .apply()
    }

    private fun dialogAddLocal () {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_add_local, null)

        val etNome = dialogView.findViewById<EditText>(R.id.et_nome)
        val etLatitude = dialogView.findViewById<EditText>(R.id.et_latitude)
        val etLongitude = dialogView.findViewById<EditText>(R.id.et_longitude)

        AlertDialog.Builder(this)
            .setTitle("Adicionar Localidade")
            .setView(dialogView)
            .setPositiveButton("Adicionar") {_, _ ->
                val nome = etNome.text.toString().trim()
                val latitudeStr = etLatitude.text.toString().trim()
                val longitudeStr = etLongitude.text.toString().trim()

                if (nome.isNotEmpty() && latitudeStr.isNotEmpty() && longitudeStr.isNotEmpty()) {
                    try {
                        val latitude = latitudeStr.toDouble()
                        val longitude = longitudeStr.toDouble()

                        val novaLocalidade = Localidade(nome, latitude, longitude)
                        listLocals.add(novaLocalidade)

                        saveLocals()
                        showLocals()

                        Toast.makeText(this, "Localidade adicionada!", Toast.LENGTH_SHORT).show()
                        Log.d("LocalActivity", "Lista de locais salvos: ${listLocals}")
                    } catch (e: NumberFormatException) {
                        Toast.makeText(this, "Latitude e longitude devem ser números válidos", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    Toast.makeText(this, "Preencha todos os campos", Toast.LENGTH_SHORT).show()
                }

            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun showLocals() {
        viewLocals.removeAllViews()

        if(listLocals.isEmpty()) {
            val tvEmpty = TextView(this)
            tvEmpty.text = "Nenhuma localidade cadastrada"
            tvEmpty.gravity = Gravity.CENTER
            tvEmpty.setPadding(16, 31, 16, 32)
            tvEmpty.setTextColor(ContextCompat.getColor(this, android.R.color.darker_gray))
            viewLocals.addView((tvEmpty))
            return
        }

        listLocals.forEachIndexed { index, localidade ->
            val itemView = LayoutInflater.from(this).inflate(R.layout.item_local, viewLocals, false)

            itemView.setOnClickListener {
                val intent = Intent(this@LocalsActivity, MainActivity::class.java)
                intent.putExtra("latitude", localidade.latitude)
                intent.putExtra("longitude", localidade.longitude)
                intent.putExtra("name", localidade.nome)
                startActivity(intent)

            }

            val tvNome = itemView.findViewById<TextView>(R.id.tv_nome_localidade)
            val tvLatitude = itemView.findViewById<TextView>(R.id.tv_latitude)
            val tvLongitude = itemView.findViewById<TextView>(R.id.tv_longitude)
            val btnEdit = itemView.findViewById<ImageButton>(R.id.btn_editar)
            val btnDelete = itemView.findViewById<ImageButton>(R.id.btn_excluir)

            tvNome.text = localidade.nome
            tvLatitude.text = String.format("%.4f", localidade.latitude)
            tvLongitude.text = String.format("%.4f", localidade.longitude)

            btnEdit.setOnClickListener {
                editLocal(index)
            }

            btnDelete.setOnClickListener {
                deleteLocal(index)
            }

            viewLocals.addView(itemView)

        }
    }


    private fun editLocal(index: Int) {
        val local = listLocals[index]

        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_add_local, null)

        val etNome = dialogView.findViewById<EditText>(R.id.et_nome)
        val etLatitude = dialogView.findViewById<EditText>(R.id.et_latitude)
        val etLongitude = dialogView.findViewById<EditText>(R.id.et_longitude)

        etNome.setText(local.nome)
        etLatitude.setText(local.latitude.toString())
        etLongitude.setText(local.longitude.toString())

        AlertDialog.Builder(this)
            .setTitle("Editar Localidade")
            .setView(dialogView)
            .setPositiveButton("Salvar") { _, _ ->
                val nome = etNome.text.toString().trim()
                val latitudeStr = etLatitude.text.toString().trim()
                val longitudeStr = etLongitude.text.toString().trim()

                if (nome.isNotEmpty() && latitudeStr.isNotEmpty() && longitudeStr.isNotEmpty()) {
                    try {
                        val latitude = latitudeStr.toDouble()
                        val longitude = longitudeStr.toDouble()

                        listLocals[index] = Localidade(nome, latitude, longitude)

                        saveLocals()
                        showLocals()

                        Toast.makeText(this, "Localidade atualizada!", Toast.LENGTH_SHORT).show()
                    } catch (e: NumberFormatException) {
                        Toast.makeText(this, "Latitude e longitude devem ser números válidos", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    Toast.makeText(this, "Preencha todos os campos", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun deleteLocal(index: Int) {
        val localidade = listLocals[index]

        AlertDialog.Builder(this)
            .setTitle("Excluir Localidade")
            .setMessage("Deseja realmente excluir a localidade \"${localidade.nome}\"?")
            .setPositiveButton("Sim") { _, _ ->
                listLocals.removeAt(index)

                saveLocals()
                showLocals()

                Toast.makeText(this, "Localidade excluída!", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("Não", null)
            .show()
    }

}

data class Localidade(
    var nome: String,
    var latitude: Double,
    var longitude: Double
)