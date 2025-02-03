package com.example.tcc2

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.tcc2.models.LocationViewModel
import com.example.tcc2.models.Server
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.jsoup.Jsoup
import org.jsoup.parser.Parser
import java.math.BigDecimal
import java.math.RoundingMode
import java.net.HttpURLConnection
import java.net.URL
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.net.ssl.HttpsURLConnection
import javax.net.ssl.SSLSocketFactory
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

data class TestResult(
    val ping: String,
    val downloadSpeed: String,
    val uploadSpeed: String,
    val timestamp: String
)

class TestResultManager(context: Context) {
    private val sharedPreferences: SharedPreferences =
        context.getSharedPreferences("test_results", Context.MODE_PRIVATE)
    private val gson = Gson()

    // Chave para armazenar os resultados dos testes
    private val resultsKey = "test_results_key"

    // Função para salvar os resultados
    fun saveTestResult(testResult: TestResult) {
        // Obter os resultados antigos
        val currentResults = getTestResults().toMutableList()

        // Adicionar o novo resultado
        currentResults.add(0, testResult)  // Adiciona no início da lista

        // Manter apenas os últimos n resultados
        if (currentResults.size > 20) {
            currentResults.removeAt(currentResults.size - 1)  // Remove o resultado mais antigo
        }

        // Salvar a lista no SharedPreferences como uma string JSON
        val json = gson.toJson(currentResults)
        sharedPreferences.edit().putString(resultsKey, json).apply()
    }

    // Função para obter os últimos 10 resultados
    fun getTestResults(): List<TestResult> {
        val json = sharedPreferences.getString(resultsKey, "[]")
        val type = object : TypeToken<List<TestResult>>() {}.type
        return gson.fromJson(json, type)
    }
}

fun formatTimestamp(timestamp: String): String {
    return try {
        val date = Date(timestamp.toLong())
        val formatter = SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault())
        formatter.format(date)
    } catch (e: Exception) {
        "Invalid Timestamp"
    }
}

suspend fun performPing(hostname: String): String {
    return withContext(Dispatchers.IO) {
        try {
            val address = java.net.InetAddress.getByName(hostname)
            val results = mutableListOf<Long>()

            for (i in 1..5) {
                val startTime = System.currentTimeMillis()
                if (address.isReachable(1000)) {
                    val timeMs = System.currentTimeMillis() - startTime
                    results.add(timeMs)
                    Log.d("Ping", "Ping $i: $timeMs ms")
                } else {
                    Log.d("Ping", "Ping $i: Timeout")
                }
                delay(1000)
            }

            if (results.isNotEmpty()) {
                val average = results.average()
                "Average Ping: ${round(average, 2)}ms"
            } else {
                "Failed to get ping results."
            }
        } catch (e: Exception) {
            Log.e("Ping", "Ping execution failed", e)
            "Error: ${e.message}"
        }
    }
}

suspend fun measureLatency(serverUrl: String): Long {
    return withContext(Dispatchers.IO) {
        try {
            val startTime = System.currentTimeMillis()
            val connection = URL(serverUrl).openConnection() as HttpURLConnection
            connection.connectTimeout = 1000 // 1 second timeout
            connection.readTimeout = 1000
            connection.connect()
            val latency = System.currentTimeMillis() - startTime
            connection.disconnect()
            latency
        } catch (e: Exception) {
            Log.e("MeasureLatency", "Error measuring latency for $serverUrl", e)
            Long.MAX_VALUE // Return maximum latency in case of an error
        }
    }
}

suspend fun fetchServersWithLatency(userLat: Double, userLon: Double): List<Server> {
    val servers = fetchServers(userLat, userLon) // Fetch servers with distances calculated
    return withContext(Dispatchers.IO) {
        servers.map { server ->
            server.latency = measureLatency(server.url) // Measure latency for each server
            server
        }.sortedBy { it.latency } // Sort by latency (lowest first)
    }
}

suspend fun measureDownloadSpeed(
    serverUrl: String,
    onProgressUpdate: (Float) -> Unit
): String {
    return withContext(Dispatchers.IO) {
        val bufferSize = 150 * 1024 // 150 KB buffer
        val duration = 8_000L // Test duration in ms
        val startTime = System.currentTimeMillis()
        var downloadedBytes = 0L

        // List of files to download
        val filePaths = listOf(
            "/random4000x4000.jpg",
            "/random3000x3000.jpg",
            "/random2000x2000.jpg",
            "/random1000x1000.jpg"
        )

        // Ensure the number of workers matches the number of file paths
        val jobs = filePaths.mapIndexed { index, filePath ->
            launch {
                try {
                    val baseUrl = serverUrl.substringBeforeLast("/")
                    val downloadUrl = baseUrl + filePath
                    Log.d("DownloadWorker-$index", "Downloading from: $downloadUrl")

                    while (System.currentTimeMillis() - startTime < duration) {
                        val connection = URL(downloadUrl).openConnection() as HttpURLConnection
                        connection.connect()

                        if (connection.responseCode == HttpURLConnection.HTTP_OK) {
                            connection.inputStream.use { input ->
                                val buffer = ByteArray(bufferSize)
                                var bytesRead: Int
                                while (input.read(buffer).also { bytesRead = it } != -1) {
                                    synchronized(this) {
                                        downloadedBytes += bytesRead
                                    }
                                }
                            }
                        } else {
                            Log.e("DownloadWorker-$index", "Failed to download from $downloadUrl")
                        }
                    }
                } catch (e: Exception) {
                    Log.e("DownloadWorker-$index", "Error in worker", e)
                }
            }
        }

        // Wait for all workers to complete
        jobs.joinAll()

        val endTime = System.currentTimeMillis()
        val elapsedTimeSeconds = (endTime - startTime) / 1000.0

        // Calculate download speed in Mbps
        val downloadSpeedMbps = (downloadedBytes * 8) / (1_000_000.0 * elapsedTimeSeconds)
        onProgressUpdate(1f) // Update progress to 100% at the end
        "Download Speed: ${round(downloadSpeedMbps, 2)} Mbps"
    }
}


suspend fun fetchServers(userLat: Double, userLon: Double): List<Server> {
    val url = "https://www.speedtest.net/speedtest-servers-static.php"
    return withContext(Dispatchers.IO) {
        try {
            val connection = URL(url).openConnection() as HttpsURLConnection
            connection.sslSocketFactory = SSLSocketFactory.getDefault() as SSLSocketFactory
            connection.connect()
            val response = connection.inputStream.bufferedReader().use { it.readText() }

            val doc = Jsoup.parse(response, "", Parser.xmlParser())
            val servers = doc.select("server").map {
                val server = Server(
                    url = it.attr("url"),
                    lat = it.attr("lat").toDouble(),
                    lon = it.attr("lon").toDouble(),
                    name = it.attr("name"),
                    sponsor = it.attr("sponsor")
                )
                server.distance = calculateDistance(userLat, userLon, server.lat, server.lon)
                server
            }
            servers.sortedBy { it.distance } // Sort by distance
        } catch (e: Exception) {
            Log.e("FetchServers", "Error fetching servers", e)
            emptyList()
        }
    }
}

suspend fun measureUploadSpeed(serverUrl: String, onProgressUpdate: (Float) -> Unit): String {
    return withContext(Dispatchers.IO) {
        val buffer = ByteArray(150 * 1024) { 'x'.code.toByte() } // 150 KB buffer
        val duration = 8_000L // Test duration in ms
        val startTime = System.currentTimeMillis()
        var uploadedBytes = 0

        val jobs = (1..4).map { // Simulate 4 workers
            launch {
                try {
                    while (System.currentTimeMillis() - startTime < duration) {
                        val connection = URL(serverUrl).openConnection() as HttpURLConnection
                        connection.doOutput = true
                        connection.requestMethod = "POST"
                        connection.setRequestProperty("Connection", "Keep-Alive")
                        connection.outputStream.write(buffer)

                        synchronized(this) {
                            uploadedBytes += buffer.size
                        }
                    }
                } catch (e: Exception) {
                    Log.e("UploadSpeedWorker", "Error in worker", e)
                }
            }
        }

        jobs.joinAll() // Wait for all workers to complete
        val elapsedTimeSeconds = (System.currentTimeMillis() - startTime) / 1000.0
        val uploadSpeedMbps = (uploadedBytes * 8) / (1_000_000.0 * elapsedTimeSeconds)
        "Upload Speed: ${round(uploadSpeedMbps, 2)} Mbps"
    }
}

fun calculateDistance(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
    val R = 6371 // Earth's radius in km
    val dLat = Math.toRadians(lat2 - lat1)
    val dLon = Math.toRadians(lon2 - lon1)
    val a = sin(dLat / 2).pow(2) +
            cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) * sin(dLon / 2).pow(2)
    val c = 2 * atan2(sqrt(a), sqrt(1 - a))
    return R * c
}

fun round(value: Double, places: Int): Double {
    return try {
        BigDecimal(value).setScale(places, RoundingMode.HALF_UP).toDouble()
    } catch (ex: Exception) {
        0.0
    }
}

@ExperimentalMaterial3Api
@Composable
fun TestedeVelocidadeScreen(
    locationViewModel: LocationViewModel,
    onGetUserLocation: ((Double, Double) -> Unit) -> Unit,
    navController: NavController
) {
    var isTestRunning by remember { mutableStateOf(false) }
    var progress by remember { mutableStateOf(0f) }
    val coroutineScope = rememberCoroutineScope()
    var userLocation by remember { mutableStateOf<Pair<Double, Double>?>(null) }
    var closestServer by remember { mutableStateOf<Server?>(null) }
    var availableServers by remember { mutableStateOf<List<Server>>(emptyList()) }
    var isFetchingLocation by remember { mutableStateOf(false) }
    var isServerDialogVisible by remember { mutableStateOf(false) }
    var locationErrorMessage by remember { mutableStateOf<String?>(null) }
    val location by locationViewModel.location.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    val testResultManager = TestResultManager(LocalContext.current)

    var showResultDialog by remember { mutableStateOf(false) }
    var testResultState by remember { mutableStateOf<TestResult?>(null) }

    LaunchedEffect(Unit) {
        isFetchingLocation = true
        onGetUserLocation { lat, lon ->
            userLocation = Pair(lat, lon)
            isFetchingLocation = false
            locationErrorMessage = null
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Medição de Velocidade") }
            )
        },
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            verticalArrangement = Arrangement.Top
        ) {
            // Exibe a localização do usuário
            Text(
                text = location?.let { "Localização: ${it.first}, ${it.second}" } ?: "Localização indisponível",
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            locationErrorMessage?.let {
                Text(
                    text = it,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
            }

            Button(
                onClick = {
                    coroutineScope.launch {
                        isTestRunning = true
                        snackbarHostState.showSnackbar("Buscando servidores, aguarde...")

                        val servers = fetchServersWithLatency(userLocation!!.first, userLocation!!.second)
                        availableServers = servers.take(5)

                        if (availableServers.isNotEmpty()) {
                            closestServer = availableServers.first()
                            snackbarHostState.showSnackbar("Melhor servidor selecionado: ${closestServer?.name}")
                        } else {
                            snackbarHostState.showSnackbar("Nenhum servidor disponível!")
                        }

                        isTestRunning = false
                    }
                },
                enabled = userLocation != null && !isTestRunning
            ) {
                Text(text = "Buscar servidores", fontSize = 18.sp)
            }

            Spacer(modifier = Modifier.height(16.dp))

            closestServer?.let { server ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("Servidor Selecionado", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                        Text("${server.name} (${server.sponsor})", fontSize = 16.sp)
                        Text("Latência: ${server.latency} ms", fontSize = 16.sp)
                        Text("Distância: ${round(server.distance, 2)} km", fontSize = 16.sp)
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Button(
                    onClick = { isServerDialogVisible = true },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Selecionar outro servidor", fontSize = 16.sp)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            closestServer?.let { server ->
                Button(
                    onClick = {
                        isTestRunning = true
                        progress = 0f
                        coroutineScope.launch {
                            snackbarHostState.showSnackbar("Iniciando teste de velocidade...")

                            val serverHost = URL(closestServer?.url).host
                            val pingResult = performPing(serverHost)
                            val downloadSpeedResult =
                                measureDownloadSpeed(server.url) { currentProgress ->
                                    progress = currentProgress
                                }
                            val uploadSpeedResult =
                                measureUploadSpeed(server.url) { currentProgress ->
                                    progress = 0.5f + (currentProgress / 2f)
                                }

                            testResultState = TestResult(
                                ping = pingResult,
                                downloadSpeed = downloadSpeedResult,
                                uploadSpeed = uploadSpeedResult,
                                timestamp = formatTimestamp(System.currentTimeMillis().toString())
                            )

                            testResultManager.saveTestResult(testResultState!!)

                            showResultDialog = true

                            progress = 1f
                            isTestRunning = false
                        }
                    },
                    enabled = !isTestRunning
                ) {
                    Text(text = if (isTestRunning) "Medindo..." else "Começar Teste", fontSize = 18.sp)
                }
            }

            Spacer(modifier = Modifier.height(50.dp))

            Button(
                onClick = { navController.navigate("HistoricoVelocidadeScreen") },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Text(text = "Histórico de Testes", fontSize = 18.sp)
            }
        }
    }

    if (isServerDialogVisible) {
        AlertDialog(
            onDismissRequest = { isServerDialogVisible = false },
            title = { Text("Escolha um servidor") },
            text = {
                Column {
                    availableServers.sortedBy { it.latency }.forEach { server ->
                        TextButton(
                            onClick = {
                                closestServer = server
                                isServerDialogVisible = false
                            }
                        ) {
                            Text(
                                text = "${server.name} (${server.sponsor}) - Latência: ${server.latency} ms",
                                fontSize = 14.sp
                            )
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { isServerDialogVisible = false }) {
                    Text("Cancelar")
                }
            }
        )
    }

    if (showResultDialog && testResultState != null) {
        AlertDialog(
            onDismissRequest = { showResultDialog = false },
            title = { Text("Resultado do Teste de Velocidade") },
            text = {
                Column {
                    Text("Ping: ${testResultState?.ping}")
                    Text("Download: ${testResultState?.downloadSpeed}")
                    Text("Upload: ${testResultState?.uploadSpeed}")
                    Text("Data: ${testResultState?.timestamp}")
                }
            },
            confirmButton = {
                TextButton(onClick = { showResultDialog = false }) {
                    Text("OK")
                }
            }
        )
    }
}


@Composable
fun HistoricoVelocidadeScreen() {
    val context = LocalContext.current
    val testResultManager = remember { TestResultManager(context) }
    val results = remember { mutableStateOf(testResultManager.getTestResults()) }
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text("Histórico de Testes de Velocidade", fontSize = 20.sp, modifier = Modifier.padding(bottom = 16.dp))

        Button(onClick = {
            context.getSharedPreferences("test_results", Context.MODE_PRIVATE).edit().clear().apply()
            results.value = emptyList() // Atualiza o estado para refletir a remoção dos dados
            coroutineScope.launch {
                snackbarHostState.showSnackbar("Histórico limpo com sucesso!")
            }
        }) {
            Text("Limpar Histórico")
        }

        Spacer(modifier = Modifier.height(16.dp))

        LazyColumn(modifier = Modifier.fillMaxSize()) {
            items(results.value.size) { index ->
                val result = results.value[index]
                Card(modifier = Modifier.padding(8.dp)) {
                    Column(modifier = Modifier.padding(8.dp)) {
                        Text("Data: ${result.timestamp}", fontSize = 16.sp)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(result.ping, fontSize = 16.sp)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(result.downloadSpeed, fontSize = 16.sp)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(result.uploadSpeed, fontSize = 16.sp)
                    }
                }
            }
        }

        SnackbarHost(hostState = snackbarHostState, modifier = Modifier.align(Alignment.CenterHorizontally))
    }
}

