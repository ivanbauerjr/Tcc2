package com.example.tcc2.models

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ScanResultManager(context: Context) {
    private val sharedPreferences = context.getSharedPreferences("scan_results", Context.MODE_PRIVATE)
    private val gson = Gson()

    fun saveScanResult(devices: List<String>) {
        val timestamp = SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault()).format(Date())
        val result = ScanResult(timestamp, devices)

        val previousResults = getScanResults().toMutableList()
        previousResults.add(0, result) // Adiciona o novo escaneamento no topo

        sharedPreferences.edit().putString("scan_history", gson.toJson(previousResults)).apply()
    }

    fun getScanResults(): List<ScanResult> {
        val json = sharedPreferences.getString("scan_history", null) ?: return emptyList()
        val type = object : TypeToken<List<ScanResult>>() {}.type
        return gson.fromJson(json, type)
    }

    fun clearHistory() {
        sharedPreferences.edit().remove("scan_history").apply()
    }
}

data class ScanResult(
    val timestamp: String,
    val devices: List<String>
)
