package com.example.tcc2.models

import android.app.Application
import android.content.Context
import android.net.wifi.WifiManager
import android.text.format.Formatter
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

class NetworkScanViewModel(application: Application) : AndroidViewModel(application) {
    private val _uiState = MutableStateFlow(NetworkScanState())
    val uiState: StateFlow<NetworkScanState> = _uiState
    private val scanResultManager = ScanResultManager(application)

    init {
        val phoneIp = getPhoneIpAddress()
        val history = scanResultManager.getScanResults()

        _uiState.value = _uiState.value.copy(
            phoneIp = phoneIp,
            scanHistory = history
        )
    }

    fun startScan() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isScanning = true)

            val subnet = getSubnet()
            scanNetwork(subnet) { results ->
                val history = scanResultManager.getScanResults()
                val lastScan = history.firstOrNull()?.devices ?: emptyList() // Obtém a última execução do histórico

                val addedDevices = results - lastScan
                val removedDevices = lastScan - results

                val statusMessage = when {
                    addedDevices.isNotEmpty() && removedDevices.isNotEmpty() ->
                        "Dispositivos adicionados desde o último escaneamento: ${addedDevices.joinToString()}. Dispositivos removidos desde o último escaneamento: ${removedDevices.joinToString()}."
                    addedDevices.isNotEmpty() -> "Novo(s) dispositivo(s) encontrado(s): ${addedDevices.joinToString()}."
                    removedDevices.isNotEmpty() -> "Dispositivo(s) removido(s) desde o último escaneamento: ${removedDevices.joinToString()}."
                    else -> "Nenhuma alteração detectada em relação ao último escaneamento."
                }

                // Salva no histórico
                scanResultManager.saveScanResult(results)

                _uiState.value = _uiState.value.copy(
                    devices = results,
                    lastDevices = results,
                    statusMessage = statusMessage,
                    isScanning = false,
                    scanHistory = scanResultManager.getScanResults()
                )
            }
        }

    }

    fun clearHistory() {
        scanResultManager.clearHistory()
        _uiState.value = _uiState.value.copy(scanHistory = emptyList())
    }

    private fun getSubnet(): String {
        val wifiManager = getApplication<Application>().getSystemService(Context.WIFI_SERVICE) as WifiManager
        val ipAddress = Formatter.formatIpAddress(wifiManager.connectionInfo.ipAddress)
        return ipAddress.substringBeforeLast(".") // e.g., "192.168.15"
    }

    private fun getPhoneIpAddress(): String? {
        val wifiManager = getApplication<Application>().getSystemService(Context.WIFI_SERVICE) as WifiManager
        return Formatter.formatIpAddress(wifiManager.connectionInfo.ipAddress)
    }

    private fun pingHost(ip: String): Boolean {
        return try {
            val process = Runtime.getRuntime().exec("/system/bin/ping -c 1 -W 1 $ip")
            process.waitFor() == 0
        } catch (e: Exception) {
            false
        }
    }

    private fun scanNetwork(subnet: String, onResult: (List<String>) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            val devices = mutableListOf<String>()
            val mutex = Mutex()

            coroutineScope {
                val jobs = (1..254).map { i ->
                    launch {
                        val ip = "$subnet.$i"
                        if (pingHost(ip)) {
                            mutex.withLock { devices.add(ip) }
                        }
                    }
                }
                jobs.joinAll() // Aguarda todas as tarefas terminarem
            }

            withContext(Dispatchers.Main) {
                onResult(devices)
            }
        }
    }

}


data class NetworkScanState(
    val devices: List<String> = emptyList(),
    val lastDevices: List<String> = emptyList(),
    val phoneIp: String? = null,
    val isScanning: Boolean = false,
    val statusMessage: String? = null,
    val scanHistory: List<ScanResult> = emptyList()
)
