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
import kotlinx.coroutines.delay
import androidx.compose.ui.Alignment
import kotlin.math.abs
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.sp

data class NetworkDiagnosticsResult(
    val timestamp: String,
    val devicesConnected: String,
    val pingResults: String,
    val wifiStrength: String,
    val dnsResolution: String,
    val networkCongestion: String,
    val routerStatus: String
)

class NetworkDiagnosticsHistoryManager(context: Context) {
    private val sharedPreferences: SharedPreferences =
        context.getSharedPreferences("network_diagnostics_history", Context.MODE_PRIVATE)
    private val gson = Gson()
    private val key = "history"
    private val feedbackKey = "latest_feedback"

    fun saveResult(result: NetworkDiagnosticsResult) {
        val currentHistory = getHistory().toMutableList()
        currentHistory.add(0, result)

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
        sharedPreferences.edit().remove(feedbackKey).apply()
    }

    fun saveLatestFeedback(feedback: List<String>) {
        val json = gson.toJson(feedback)
        sharedPreferences.edit().putString(feedbackKey, json).apply()
    }

    fun getLatestFeedback(): List<String> {
        val json = sharedPreferences.getString(feedbackKey, "[]") ?: "[]"
        return gson.fromJson(json, object : TypeToken<List<String>>() {}.type)
    }
}


@ExperimentalMaterial3Api
@Composable
fun NetworkDiagnosticsScreen(navController: NavController) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val historyManager = remember { NetworkDiagnosticsHistoryManager(context) }

    var latestResult by remember { mutableStateOf(historyManager.getLatestResult()) }
    var isLoading by remember { mutableStateOf(false) }
    var showFeedbackDialog by remember { mutableStateOf(false) }
    var feedbackMessages by remember { mutableStateOf(emptyList<String>()) }

    var connectedDevices by remember { mutableStateOf(latestResult?.devicesConnected ?: "Aguardando diagnóstico...") }
    var wifiSignalStrength by remember { mutableStateOf(latestResult?.wifiStrength ?: "Aguardando diagnóstico...") }
    var dnsResolution by remember { mutableStateOf(latestResult?.dnsResolution ?: "Aguardando diagnóstico...") }
    var unstableConnection by remember { mutableStateOf(latestResult?.pingResults ?: "Aguardando diagnóstico...") }
    var networkCongestion by remember { mutableStateOf(latestResult?.networkCongestion ?: "Aguardando diagnóstico...") }
    var routerStatus by remember { mutableStateOf(latestResult?.routerStatus ?: "Aguardando diagnóstico...") }

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

            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item { DiagnosticResult(label = "Dispositivos Conectados", value = connectedDevices) }
                item { DiagnosticResult(label = "Intensidade do WiFi", value = wifiSignalStrength) }
                item { DiagnosticResult(label = "Resolução de DNS", value = dnsResolution) }
                item { DiagnosticResult(label = "Estabilidade da Rede", value = unstableConnection) }
                item { DiagnosticResult(label = "Congestionamento de Rede", value = networkCongestion) }
                item { DiagnosticResult(label = "Status do Roteador", value = routerStatus) }
            }

            if (isLoading) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center
                ) {
                    CircularProgressIndicator()
                    Spacer(modifier = Modifier.width(10.dp))
                    Text("Executando diagnóstico...", style = MaterialTheme.typography.bodyLarge)
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Button(
                    modifier = Modifier.weight(2f),
                    onClick = {
                        coroutineScope.launch {
                            isLoading = true

                            val newConnectedDevices = countConnectedDevices(context).toString()
                            val newWifiSignalStrength = getWifiSignalStrength(context)
                            val newDnsResolution = if (resolveDNS("google.com")) "Resolvido com sucesso" else "Falha na resolução"
                            val newUnstableConnection = analyzeUnstableConnection()
                            val newNetworkCongestion = analyzeNetworkCongestion()
                            val newRouterStatus = checkRouterStatus(context)

                            connectedDevices = newConnectedDevices
                            wifiSignalStrength = newWifiSignalStrength
                            dnsResolution = newDnsResolution
                            unstableConnection = newUnstableConnection
                            networkCongestion = newNetworkCongestion
                            routerStatus = newRouterStatus

                            val previousFeedback = historyManager.getLatestFeedback()

                            feedbackMessages = if (latestResult == null) {
                                // PRIMEIRA MEDIÇÃO: Exibir todos os resultados como feedback inicial
                                listOf(
                                    "Dispositivos Conectados: $newConnectedDevices",
                                    "Intensidade do WiFi: $newWifiSignalStrength",
                                    "Resolução de DNS: $newDnsResolution",
                                    " $newUnstableConnection",
                                    " $newNetworkCongestion",
                                    " $newRouterStatus"
                                )
                            } else {
                                // COMPARAR RESULTADOS: Exibir somente mudanças
                                generateFeedback(
                                    latestResult,
                                    newConnectedDevices,
                                    newWifiSignalStrength,
                                    newDnsResolution,
                                    newUnstableConnection,
                                    newNetworkCongestion,
                                    newRouterStatus
                                ).ifEmpty { listOf("A rede permaneceu estável.") }
                            }

                            historyManager.saveLatestFeedback(feedbackMessages)

                            if (feedbackMessages.isNotEmpty()) {
                                showFeedbackDialog = true
                            }

                            val newResult = NetworkDiagnosticsResult(
                                timestamp = getCurrentTimestamp(),
                                devicesConnected = newConnectedDevices,
                                pingResults = newUnstableConnection,
                                wifiStrength = newWifiSignalStrength,
                                dnsResolution = newDnsResolution,
                                networkCongestion = newNetworkCongestion,
                                routerStatus = newRouterStatus
                            )

                            historyManager.saveResult(newResult)
                            latestResult = newResult

                            isLoading = false
                        }
                    }
                ) {
                    Text("Realizar Diagnóstico")
                }

                Button(
                    modifier = Modifier.weight(1f),
                    onClick = { navController.navigate("NetworkDiagnosticsHistoryScreen") }
                ) {
                    Text("Histórico")
                }
            }
        }
    }

    if (showFeedbackDialog) {
        AlertDialog(
            onDismissRequest = { showFeedbackDialog = false },
            title = { Text("Diagnóstico da Rede") },
            text = {
                Column {
                    feedbackMessages.forEach { message ->
                        Text("- $message", style = MaterialTheme.typography.bodyMedium)
                    }
                }
            },
            confirmButton = {
                Button(onClick = { showFeedbackDialog = false }) {
                    Text("OK")
                }
            }
        )
    }
}

suspend fun analyzeUnstableConnection(): String {
    return withContext(Dispatchers.IO) {
        val host = "8.8.8.8"
        val pingCount = 20 // Aumentar para capturar variações melhor
        val responseTimes = mutableListOf<Long>()
        var packetLoss = 0

        try {
            for (i in 1..pingCount) {
                val startTime = System.currentTimeMillis()
                val process = Runtime.getRuntime().exec("ping -c 1 -s 32 $host")
                val exitCode = process.waitFor()
                val elapsedTime = System.currentTimeMillis() - startTime

                if (exitCode == 0) {
                    responseTimes.add(elapsedTime)
                } else {
                    packetLoss++
                }

                delay(300) // Redução do intervalo entre pings para capturar flutuações
            }

            if (responseTimes.isEmpty()) {
                return@withContext "Erro ao coletar dados"
            }

            val avgLatency = responseTimes.average()
            val jitter = responseTimes.map { abs(it - avgLatency) }.average()
            val jitterThreshold = avgLatency * 0.25 // Limiar reduzido para melhor detecção

            return@withContext when {
                packetLoss > pingCount * 0.15 -> "Perda de pacotes alta ($packetLoss pacotes perdidos)"
                jitter > jitterThreshold -> "Conexão instável: Variação significativa nos tempos de resposta (Jitter: ${jitter.toInt()} ms)"
                avgLatency > 200 -> "Conexão lenta: Tempo médio de resposta elevado (${avgLatency.toInt()} ms)"
                else -> "Conexão estável"
            }
        } catch (e: Exception) {
            return@withContext "Erro ao analisar conexão: ${e.message}"
        }
    }
}

suspend fun analyzeNetworkCongestion(): String {
    return withContext(Dispatchers.IO) {
        val host = "8.8.8.8"
        val normalPingTimes = mutableListOf<Long>()
        val largePacketPingTimes = mutableListOf<Long>()
        val ttlTestResults = mutableListOf<Long>()

        try {
            // Ping com pacotes pequenos (56 bytes)
            for (i in 1..7) {
                val startTime = System.currentTimeMillis()
                val process = Runtime.getRuntime().exec("ping -c 1 -s 56 $host")
                val exitCode = process.waitFor()
                val elapsedTime = System.currentTimeMillis() - startTime

                if (exitCode == 0) {
                    normalPingTimes.add(elapsedTime)
                }

                delay(300)
            }

            // Ping com pacotes grandes (1000 bytes)
            for (i in 1..7) {
                val startTime = System.currentTimeMillis()
                val process = Runtime.getRuntime().exec("ping -c 1 -s 1000 $host")
                val exitCode = process.waitFor()
                val elapsedTime = System.currentTimeMillis() - startTime

                if (exitCode == 0) {
                    largePacketPingTimes.add(elapsedTime)
                }

                delay(300)
            }

            // Ping com diferentes valores de TTL
            for (ttl in 5..30 step 5) {
                val startTime = System.currentTimeMillis()
                val process = Runtime.getRuntime().exec("ping -c 1 -s 56 -t $ttl $host")
                val exitCode = process.waitFor()
                val elapsedTime = System.currentTimeMillis() - startTime

                if (exitCode == 0) {
                    ttlTestResults.add(elapsedTime)
                }

                delay(300)
            }

            if (normalPingTimes.isEmpty() || largePacketPingTimes.isEmpty()) {
                return@withContext "Erro ao coletar dados"
            }

            val avgNormalPing = normalPingTimes.average()
            val avgLargePacketPing = largePacketPingTimes.average()
            val latencyIncrease = avgLargePacketPing - avgNormalPing
            val congestionThreshold = avgNormalPing * 0.35 // 35% como limite para congestão

            val ttlVariability = ttlTestResults.maxOrNull()?.minus(ttlTestResults.minOrNull() ?: 0) ?: 0

            return@withContext when {
                avgNormalPing > 180 -> "Possível congestionamento geral: Mesmo pacotes pequenos estão demorando (${avgNormalPing.toInt()} ms)"
                latencyIncrease > congestionThreshold -> "Possível congestionamento na rede: Atraso maior para pacotes grandes (Diferença: ${latencyIncrease.toInt()} ms)"
                ttlVariability > 100 -> "Variação na rota detectada: Diferença no tempo de resposta entre diferentes saltos (${ttlVariability} ms)"
                else -> "Nenhum congestionamento detectado"
            }
        } catch (e: Exception) {
            return@withContext "Erro ao verificar congestionamento: ${e.message}"
        }
    }
}


suspend fun checkRouterStatus(context: Context): String {
    return withContext(Dispatchers.IO) {
        val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
        val gatewayIp = wifiManager.dhcpInfo.gateway
        val routerAddress = InetAddress.getByAddress(byteArrayOf(
            (gatewayIp and 0xFF).toByte(),
            (gatewayIp shr 8 and 0xFF).toByte(),
            (gatewayIp shr 16 and 0xFF).toByte(),
            (gatewayIp shr 24 and 0xFF).toByte()
        ))

        return@withContext if (routerAddress.isReachable(3000)) {
            "Roteador funcionando normalmente"
        } else {
            "Problema no roteador"
        }
    }
}

@Composable
fun DiagnosticResult(label: String, value: String) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(4.dp),
        elevation = CardDefaults.cardElevation(4.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(text = label, style = MaterialTheme.typography.bodyLarge)
            Text(text = value,
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

fun getWifiSignalStrength(context: Context): String {
    val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
    val rssi = wifiManager.connectionInfo.rssi
    return when {
        rssi > -50 -> "Ótima ($rssi dBm)"
        rssi > -70 -> "Boa ($rssi dBm)"
        rssi > -90 -> "Fraca ($rssi dBm)"
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

fun extractSignalQuality(signalStrength: String): String {
    return when {
        signalStrength.contains("Ótima", ignoreCase = true) -> "Ótima"
        signalStrength.contains("Boa", ignoreCase = true) -> "Boa"
        signalStrength.contains("Fraca", ignoreCase = true) -> "Fraca"
        signalStrength.contains("Muito fraca", ignoreCase = true) -> "Muito fraca"
        else -> signalStrength // Se não for identificado, retorna como está
    }
}

fun generateFeedback(
    lastResult: NetworkDiagnosticsResult?,
    newDevices: String,
    newWifi: String,
    newDns: String,
    newUnstable: String,
    newCongestion: String,
    newRouter: String
): List<String> {
    val feedback = mutableListOf<String>()

    lastResult?.let {
        // Dispositivos conectados
        if (it.devicesConnected != newDevices) {
            val diff = newDevices.toInt() - it.devicesConnected.toInt()
            if (diff > 0) feedback.add("A quantidade de dispositivos conectados aumentou (+$diff).")
            else feedback.add("A quantidade de dispositivos conectados diminuiu (${diff * -1}).")
        }

        // Intensidade do WiFi - Apenas se houver alteração significativa
        if (it.wifiStrength != newWifi) {
            val previousQuality = extractSignalQuality(it.wifiStrength)
            val newQuality = extractSignalQuality(newWifi)

            if (previousQuality != newQuality) {
                feedback.add("A intensidade do sinal mudou de '$previousQuality' para '$newQuality'.")
            }
        }

        // Conexão Instável
        if (it.pingResults != newUnstable) {
            feedback.add("A conexão mudou: '$newUnstable'.")
        }

        // Congestionamento de Rede
        if (it.networkCongestion != newCongestion) {
            feedback.add("O status mudou: '$newCongestion'.")
        }

        // Status do Roteador
        if (it.routerStatus != newRouter) {
            feedback.add("O status do roteador mudou: '$newRouter'.")
        }
    }

    return feedback
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NetworkDiagnosticsHistoryScreen() {
    val context = LocalContext.current
    val historyManager = remember { NetworkDiagnosticsHistoryManager(context) }
    var history by remember { mutableStateOf(historyManager.getHistory()) }
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()

    Scaffold(
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) } // Coloca o SnackbarHost no Scaffold
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
                .padding(paddingValues) // Garante que não sobreponha a UI
        ) {
            Text("Histórico de Diagnóstico", fontSize = 20.sp, modifier = Modifier.padding(bottom = 16.dp))

            Button(onClick = {
                    coroutineScope.launch {
                        historyManager.clearHistory()
                        history = emptyList()
                        snackbarHostState.showSnackbar("Histórico limpo com sucesso!")
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
                                Text("Intensidade do sinal: ${result.wifiStrength}")
                                Text("DNS: ${result.dnsResolution}")
                                Text(" ${result.pingResults}")
                                Text(" ${result.networkCongestion}")
                                Text(" ${result.routerStatus}")
                            }
                        }
                    }
                }
            }

        }
    }
}