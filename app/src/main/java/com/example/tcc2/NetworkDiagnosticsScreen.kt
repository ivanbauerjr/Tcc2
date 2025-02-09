package com.example.tcc2

import android.content.Context
import android.net.wifi.WifiManager
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import java.net.InetAddress
import androidx.navigation.NavController
import java.text.SimpleDateFormat
import java.util.*
import android.content.SharedPreferences
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

data class NetworkDiagnosticsResult(
    val timestamp: String,
    val devicesConnected: String,
    val pingResults: String,
    val wifiStrength: String,
    val dnsResolution: String
)

class NetworkDiagnosticsHistoryManager(context: Context) {
    private val sharedPreferences: SharedPreferences =
        context.getSharedPreferences("network_diagnostics_history", Context.MODE_PRIVATE)
    private val gson = Gson()
    private val key = "history"

    fun saveResult(result: NetworkDiagnosticsResult) {
        val currentHistory = getHistory().toMutableList()
        currentHistory.add(0, result) // Add newest result at the top

        // Limit to the last 10 results for history storage
        if (currentHistory.size > 10) {
            currentHistory.removeAt(currentHistory.size - 1)
        }

        val json = gson.toJson(currentHistory)
        sharedPreferences.edit().putString(key, json).apply()
    }

    fun getHistory(): List<NetworkDiagnosticsResult> {
        val json = sharedPreferences.getString(key, "[]") ?: "[]"
        return gson.fromJson(json, object : TypeToken<List<NetworkDiagnosticsResult>>() {}.type)
    }

    fun getLatestResult(): NetworkDiagnosticsResult? {
        return getHistory().firstOrNull()
    }

    fun clearHistory() {
        sharedPreferences.edit().remove(key).apply()
    }
}

@ExperimentalMaterial3Api
@Composable
fun NetworkDiagnosticsScreen(navController: NavController) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val historyManager = remember { NetworkDiagnosticsHistoryManager(context) }
    var latestResult by remember { mutableStateOf<NetworkDiagnosticsResult?>(historyManager.getLatestResult()) }

    var connectedDevices by remember { mutableStateOf(latestResult?.devicesConnected ?: "") }
    var pingResults by remember { mutableStateOf(latestResult?.pingResults ?: "") }
    var wifiSignalStrength by remember { mutableStateOf(latestResult?.wifiStrength ?: "") }
    var dnsResolution by remember { mutableStateOf(latestResult?.dnsResolution ?: "") }

    Scaffold(
        topBar = { TopAppBar(title = { Text("Diagnóstico de Rede") }) }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text("Último Resultado Obtido", style = MaterialTheme.typography.headlineMedium)

            DiagnosticResult(label = "Dispositivos Conectados", value = connectedDevices)
            DiagnosticResult(label = "Testes de Conectividade", value = pingResults)
            DiagnosticResult(label = "Intensidade do WiFi", value = wifiSignalStrength)
            DiagnosticResult(label = "Resolução de DNS", value = dnsResolution)

            Spacer(modifier = Modifier.height(20.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                Button(onClick = {
                    coroutineScope.launch {
                        val devices = countConnectedDevices(context).toString()
                        val pings = performMultiplePings()
                        val wifiSignal = getWifiSignalStrength(context)
                        val dns = if (resolveDNS("google.com")) "Resolvido com sucesso" else "Falha na resolução"

                        connectedDevices = devices
                        pingResults = pings
                        wifiSignalStrength = wifiSignal
                        dnsResolution = dns

                        val newResult = NetworkDiagnosticsResult(
                            timestamp = getCurrentTimestamp(),
                            devicesConnected = devices,
                            pingResults = pings,
                            wifiStrength = wifiSignal,
                            dnsResolution = dns
                        )

                        // Save the latest result
                        historyManager.saveResult(newResult)
                        latestResult = newResult
                    }
                }) {
                    Text("Realizar Diagnóstico")
                }

                Button(onClick = { navController.navigate("NetworkDiagnosticsHistoryScreen") }) {
                    Text("Histórico")
                }
            }
        }
    }
}

@Composable
fun DiagnosticResult(label: String, value: String) {
    Card(modifier = Modifier.fillMaxWidth().padding(4.dp), elevation = CardDefaults.cardElevation(4.dp)) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(text = label, style = MaterialTheme.typography.bodyLarge)
            Text(text = if (value.isNotEmpty()) value else "Aguardando diagnóstico...",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary)
        }
    }
}

fun countConnectedDevices(context: Context): Int {
    val wifiManager = context.getSystemService(Context.WIFI_SERVICE) as WifiManager
    val ipInt = wifiManager.connectionInfo.ipAddress

    val ipBytes = byteArrayOf(
        (ipInt and 0xFF).toByte(),
        (ipInt shr 8 and 0xFF).toByte(),
        (ipInt shr 16 and 0xFF).toByte(),
        (ipInt shr 24 and 0xFF).toByte()
    )

    val ipAddress = InetAddress.getByAddress(ipBytes).hostAddress ?: return 0
    val subnet = ipAddress.substringBeforeLast(".")

    val devices = mutableListOf<String>()
    runBlocking {
        val jobs = (1..254).map { i ->
            async(Dispatchers.IO) {
                val ip = "$subnet.$i"
                try {
                    val address = InetAddress.getByName(ip)
                    if (address.isReachable(300)) {
                        devices.add(ip)
                    }
                } catch (e: Exception) {
                }
            }
        }
        jobs.awaitAll()
    }
    return devices.size
}

suspend fun performMultiplePings(): String {
    return withContext(Dispatchers.IO) {
        val testHosts = listOf("8.8.8.8", "1.1.1.1", "google.com", "facebook.com")
        val results = mutableListOf<String>()

        for (host in testHosts) {
            val process = Runtime.getRuntime().exec("ping -c 1 $host")
            val exitCode = process.waitFor()
            results.add("$host: ${if (exitCode == 0) "Sucesso" else "Falhou"}")
        }
        results.joinToString("\n")
    }
}

fun getWifiSignalStrength(context: Context): String {
    val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
    val rssi = wifiManager.connectionInfo.rssi
    return when {
        rssi > -50 -> "Ótima ($rssi dBm)"
        rssi > -60 -> "Boa ($rssi dBm)"
        rssi > -70 -> "Moderada ($rssi dBm)"
        rssi > -80 -> "Fraca ($rssi dBm)"
        else -> "Muito fraca ($rssi dBm)"
    }
}

suspend fun resolveDNS(domain: String): Boolean {
    return withContext(Dispatchers.IO) {
        try {
            InetAddress.getByName(domain)
            true
        } catch (e: Exception) {
            false
        }
    }
}

fun getCurrentTimestamp(): String {
    val sdf = SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault())
    return sdf.format(Date())
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NetworkDiagnosticsHistoryScreen() {
    val context = LocalContext.current
    val historyManager = remember { NetworkDiagnosticsHistoryManager(context) }
    var history by remember { mutableStateOf(historyManager.getHistory()) }
    val coroutineScope = rememberCoroutineScope()

    Scaffold(
        topBar = { TopAppBar(title = { Text("Histórico de Diagnóstico") }) }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Button(
                onClick = {
                    coroutineScope.launch {
                        historyManager.clearHistory()
                        history = emptyList()
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Limpar Histórico")
            }

            if (history.isEmpty()) {
                Text("Nenhum histórico encontrado.", style = MaterialTheme.typography.bodyLarge)
            } else {
                LazyColumn {
                    items(history) { result ->
                        Card(modifier = Modifier.fillMaxWidth().padding(4.dp)) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text("Data: ${result.timestamp}")
                                Text("Dispositivos Conectados: ${result.devicesConnected}")
                                Text("WiFi: ${result.wifiStrength}")
                                Text("DNS: ${result.dnsResolution}")
                                Text("Ping: ${result.pingResults}")
                            }
                        }
                    }
                }
            }

        }
    }
}