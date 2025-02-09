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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
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
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.text.style.TextAlign


class ServerManager(context: Context) {
    private val sharedPreferences: SharedPreferences =
        context.getSharedPreferences("server_prefs", Context.MODE_PRIVATE)
    private val gson = Gson()
    private val serverKey = "last_selected_server"

    fun saveServer(server: Server) {
        val json = gson.toJson(server)
        sharedPreferences.edit().putString(serverKey, json).apply()
    }

    fun getLastSelectedServer(): Server? {
        val json = sharedPreferences.getString(serverKey, null) ?: return null
        return gson.fromJson(json, object : TypeToken<Server>() {}.type)
    }
}

data class TestResult(
    val ping: String,
    val downloadSpeed: String,
    val uploadSpeed: String,
    val timestamp: String,
    val serverInfo: String
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
                "${round(average, 2)}ms"
            } else {
                "Falha ao obter resultados de ping."
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
        "${round(downloadSpeedMbps, 2)} Mbps"
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
        "${round(uploadSpeedMbps, 2)} Mbps"
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
    val context = LocalContext.current
    var isTestRunning by remember { mutableStateOf(false) }
    val serverManager = remember { ServerManager(context) }
    var progress by remember { mutableStateOf(0f) }
    val coroutineScope = rememberCoroutineScope()
    var userLocation by remember { mutableStateOf<Pair<Double, Double>?>(null) }
    var closestServer by remember { mutableStateOf<Server?>(serverManager.getLastSelectedServer()) }
    var availableServers by remember { mutableStateOf<List<Server>>(emptyList()) }
    var isFetchingLocation by remember { mutableStateOf(false) }
    var isServerDialogVisible by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }
    val testResultManager = TestResultManager(LocalContext.current)
    val testResults = remember { mutableStateOf(testResultManager.getTestResults()) }
    var showResultDialog by remember { mutableStateOf(false) }
    var testResultState by remember { mutableStateOf<TestResult?>(null) }

    val sharedPreferences = context.getSharedPreferences("speed_test_prefs", Context.MODE_PRIVATE)
    var feedbackMessage by rememberSaveable {
        mutableStateOf(sharedPreferences.getString("feedback_message", "") ?: "")
    }

    fun saveFeedbackMessage(message: String) {
        sharedPreferences.edit().putString("feedback_message", message).apply()
        feedbackMessage = message
    }

    fun clearFeedback() {
        saveFeedbackMessage("")
    }

    LaunchedEffect(Unit) {
        isFetchingLocation = true
        onGetUserLocation { lat, lon ->
            userLocation = Pair(lat, lon)
            isFetchingLocation = false
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Velocidade da Internet") }
            )
        },
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("Localização do Usuário", style = MaterialTheme.typography.titleMedium)
                    Text(
                        userLocation?.let { "Lat: ${it.first}, Lon: ${it.second}" } ?: "Obtendo localização...",
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Center
                    )
                }
            }

            Button(
                onClick = {
                    coroutineScope.launch {
                        snackbarHostState.showSnackbar("Buscando servidores...")
                        val servers = fetchServersWithLatency(userLocation!!.first, userLocation!!.second)
                        availableServers = servers.take(5)
                        closestServer = availableServers.firstOrNull()
                        closestServer?.let { serverManager.saveServer(it)
                        snackbarHostState.showSnackbar("Busca de servidores finalizada.")
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Buscar Servidores")
            }

            closestServer?.let { server ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.Start
                    ) {
                        Text("Servidor Selecionado", style = MaterialTheme.typography.titleMedium)
                        Text("${server.name} (${server.sponsor})", style = MaterialTheme.typography.bodyMedium)
                        Text("Latência: ${server.latency} ms", style = MaterialTheme.typography.bodyMedium)
                        Text("Distância: ${round(server.distance, 2)} km", style = MaterialTheme.typography.bodyMedium)
                    }
                }

                Button(
                    onClick = { isServerDialogVisible = true },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Selecionar outro Servidor")
                }
            }

            if (isServerDialogVisible) {
                AlertDialog(
                    onDismissRequest = { isServerDialogVisible = false },
                    title = { Text("Escolha um Servidor") },
                    text = {
                        Column {
                            availableServers.sortedBy { it.latency }.forEach { server ->
                                TextButton(
                                    onClick = {
                                        closestServer = server
                                        isServerDialogVisible = false
                                    }
                                ) {
                                    Text("${server.name} (${server.sponsor}) - Latência: ${server.latency} ms")
                                }
                            }
                        }
                    },
                    confirmButton = {
                        TextButton(onClick = { isServerDialogVisible = false }) { Text("Cancelar") }
                    }
                )
            }

            Button(
                onClick = {
                    navController.navigate("HistoricoVelocidadeScreen") {
                        launchSingleTop = true
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Histórico de Testes")
            }

            Button(
                onClick = {
                    isTestRunning = true
                    progress = 0f
                    coroutineScope.launch {
                        snackbarHostState.showSnackbar("Iniciando teste de velocidade...")
                        snackbarHostState.showSnackbar("Medindo latência...")
                        val pingResult = performPing(URL(closestServer?.url).host)
                        snackbarHostState.showSnackbar("Medindo velocidade do download...")
                        val downloadSpeedResult = measureDownloadSpeed(closestServer!!.url) { progress = it }
                        snackbarHostState.showSnackbar("Medindo velocidade do upload...")
                        val uploadSpeedResult = measureUploadSpeed(closestServer!!.url) { progress = 0.5f + (it / 2f) }
                        snackbarHostState.showSnackbar("Medição finalizada.")

                        val testResult = TestResult(
                            ping = pingResult,
                            downloadSpeed = downloadSpeedResult,
                            uploadSpeed = uploadSpeedResult,
                            timestamp = formatTimestamp(System.currentTimeMillis().toString()),
                            serverInfo = "${closestServer!!.name} (${closestServer!!.sponsor})"
                        )
                        testResultManager.saveTestResult(testResult)
                        testResults.value = testResultManager.getTestResults()
                        testResultState = testResult

                        val previousTest = testResults.value.drop(1).find { it.serverInfo == testResultState?.serverInfo }
                        feedbackMessage = if (previousTest == null) {
                            "Primeira medição neste servidor.\n" +
                                    "- Ping: ${testResultState?.ping}\n" +
                                    "- Download: ${testResultState?.downloadSpeed}\n" +
                                    "- Upload: ${testResultState?.uploadSpeed}"
                        } else {
                            "Mudança desde a última medição:\n" +
                                    "- Ping: ${previousTest.ping} → ${testResultState?.ping} \n ${if (testResultState!!.ping > previousTest.ping) "O ping aumentou, sua internet pode ter ficado mais lenta" else "O ping diminuiu, sua internet pode estar mais rápida"}\n" +
                                    "- Download: ${previousTest.downloadSpeed} → ${testResultState?.downloadSpeed} \n ${if (testResultState!!.downloadSpeed > previousTest.downloadSpeed) "O download aumentou, você pode baixar os arquivos mais rapidamente" else "O download diminuiu, os arquivos podem levar mais tempo para serem baixados"}\n" +
                                    "- Upload: ${previousTest.uploadSpeed} → ${testResultState?.uploadSpeed} \n ${if (testResultState!!.uploadSpeed < previousTest.uploadSpeed) "O upload diminuiu, podendo demorar mais tempo para subir arquivos na rede" else "O upload aumentou, permitindo enviar arquivos mais rapidamente"}"
                        }
                        saveFeedbackMessage(feedbackMessage)
                        showResultDialog = true
                        progress = 1f
                        isTestRunning = false
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = !isTestRunning
            ) {
                Text(if (isTestRunning) "Testando..." else "Iniciar Teste")
            }

            if (feedbackMessage.isNotEmpty()) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 100.dp, max = 200.dp)
                        .verticalScroll(rememberScrollState()),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Text("Resultados obtidos", style = MaterialTheme.typography.titleMedium)
                        Text(feedbackMessage.replace("-", ""), style = MaterialTheme.typography.bodyMedium) //Em alguns casos está aparecendo -
                    }
                }
            }
        }
    }
}


@Composable
fun HistoricoVelocidadeScreen(clearFeedback: () -> Unit) {
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
            clearFeedback()
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
                        Text("Ping: ${result.ping}", fontSize = 16.sp)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("Download: ${result.downloadSpeed}", fontSize = 16.sp)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("Upload: ${result.uploadSpeed.replace("-", "")}", fontSize = 16.sp)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("Servidor: ${result.serverInfo}", fontSize = 16.sp)
                    }
                }
            }
        }

        SnackbarHost(hostState = snackbarHostState, modifier = Modifier.align(Alignment.CenterHorizontally))
    }
}

