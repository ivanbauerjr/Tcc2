package com.example.tcc2

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.text.style.TextAlign
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

@Composable
fun ServerSelectionDialog(
    servers: List<Server>,
    onServerSelected: (Server) -> Unit,
    onDismissRequest: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismissRequest,
        title = { Text("Select a Server") },
        text = {
            Column {
                servers.sortedBy { it.latency }.forEach { server -> // Sort by latency
                    TextButton(onClick = {
                        onServerSelected(server)
                        onDismissRequest()
                    }) {
                        Text(
                            text = "${server.name} (${server.sponsor}) - " +
                                    "Latency: ${server.latency} ms, " +
                                    "Distance: ${round(server.distance, 2)} km"
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismissRequest) {
                Text("Cancel")
            }
        }
    )
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

@Composable
fun TestedeVelocidadeScreen(
    locationViewModel: LocationViewModel,
    onGetUserLocation: ((Double, Double) -> Unit) -> Unit,
    navController: NavController
) {
    var resultText by remember { mutableStateOf("Click the button to start testing!") }
    var isTestRunning by remember { mutableStateOf(false) }
    var progress by remember { mutableStateOf(0f) }
    val coroutineScope = rememberCoroutineScope()
    var userLocation by remember { mutableStateOf<Pair<Double, Double>?>(null) }
    var closestServer by remember { mutableStateOf<Server?>(null) }
    var availableServers by remember { mutableStateOf<List<Server>>(emptyList()) }
    var isServerDialogVisible by remember { mutableStateOf(false) }
    var isFetchingLocation by remember { mutableStateOf(false) }
    var locationErrorMessage by remember { mutableStateOf<String?>(null) }
    val location by locationViewModel.location.collectAsState()

    val testResultManager = TestResultManager(LocalContext.current)

    // Fetch user location when the screen loads
    LaunchedEffect(Unit) {
        isFetchingLocation = true
        onGetUserLocation { lat, lon ->
            userLocation = Pair(lat, lon)
            isFetchingLocation = false
            locationErrorMessage = null
        }
    }

    // UI content
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Top
    ) {
        Text(
            text = location?.let { "Location: ${it.first}, ${it.second}" } ?: "Location unavailable",
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        // Show an error message if fetching location failed
        locationErrorMessage?.let {
            Text(
                text = it,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(bottom = 16.dp)
            )
        }

        // Button to fetch location if it's not available
        if (userLocation == null && !isFetchingLocation) {
            Button(
                onClick = {
                    isFetchingLocation = true
                    onGetUserLocation { lat, lon ->
                        userLocation = Pair(lat, lon)
                        isFetchingLocation = false
                        locationErrorMessage = null
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(text = "Get Location", fontSize = 18.sp)
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Fetch servers with latency once location is available
        Button(
            onClick = {
                if (userLocation != null) {
                    coroutineScope.launch {
                        isTestRunning = true
                        resultText = "Fetching servers and measuring latency..."
                        val servers = fetchServersWithLatency(userLocation!!.first, userLocation!!.second)
                        availableServers = servers.take(5) // Show the 5 best servers by latency
                        isTestRunning = false
                        resultText = "Select a server to proceed."
                    }
                } else {
                    resultText = "Error: Unable to fetch user location."
                }
            },
            enabled = userLocation != null && !isTestRunning
        ) {
            Text(text = "Fetch Servers with Latency", fontSize = 18.sp)
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Display selected server details
        if (closestServer != null) {
            Text(
                text = "Closest Server: ${closestServer?.name} (${closestServer?.sponsor})\nDistance: ${
                    round(
                        closestServer?.distance ?: 0.0,
                        2
                    )
                } km\nURL: ${closestServer?.url}",
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.padding(bottom = 16.dp)
            )
        }

        // Button to select a server
        Button(
            onClick = { isServerDialogVisible = true },
            enabled = availableServers.isNotEmpty()
        ) {
            Text("Select Server", fontSize = 18.sp)
        }

        // Server selection dialog
        if (isServerDialogVisible) {
            ServerSelectionDialog(
                servers = availableServers,
                onServerSelected = { server ->
                    closestServer = server
                    resultText = "Selected Server: ${server.name} (${server.sponsor})"
                },
                onDismissRequest = { isServerDialogVisible = false }
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Speed testing section
        closestServer?.let { server ->
            Text(
                text = resultText,
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(16.dp))
            LinearProgressIndicator(
                progress = progress,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp),
            )
            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = {
                    if (!isTestRunning) {
                        isTestRunning = true
                        resultText = "Testing..."
                        progress = 0f
                        coroutineScope.launch {
                            val pingResult = performPing("8.8.8.8")
                            val downloadSpeedResult =
                                measureDownloadSpeed(server.url) { currentProgress ->
                                    progress = currentProgress
                                }
                            val uploadSpeedResult =
                                measureUploadSpeed(server.url) { currentProgress ->
                                    progress =
                                        0.5f + (currentProgress / 2f) // Continue progress after download
                                }
                            resultText =
                                "Ping: $pingResult\nDownload: $downloadSpeedResult\nUpload: $uploadSpeedResult"
                            progress = 1f
                            isTestRunning = false

                            // Adiciona o resultado à lista de testes
                            val newResult = TestResult(
                                ping = pingResult,
                                downloadSpeed = downloadSpeedResult,
                                uploadSpeed = uploadSpeedResult,
                                timestamp = formatTimestamp(
                                    System.currentTimeMillis().toString()
                                )
                            )
                            // Salvando o resultado
                            testResultManager.saveTestResult(newResult)
                        }
                    }
                },
                enabled = !isTestRunning
            ) {
                Text(text = if (isTestRunning) "Testing..." else "Start Test", fontSize = 18.sp)
            }
        }
        Spacer(modifier = Modifier.height(50.dp))
        // Botão para exibir o histórico de até 10 resultados anteriores
        Button(
            onClick = { navController.navigate("HistoricoVelocidadeScreen") },
            modifier = Modifier
                .fillMaxSize()  // Preenche a tela inteira
                .padding(16.dp)  // Aplica o padding de 16dp em torno do botão
                .wrapContentHeight(Alignment.Bottom)  // Alinha o botão na parte inferior da tela
                .padding(bottom = 16.dp)  // Dá um padding extra na parte inferior
            //colors = ButtonDefaults.buttonColors(Color(0xFF87CEEB) // Azul claro)
        ) {
            Text(
                text = "Histórico de Testes",
                fontSize = 18.sp
            )
        }
    }
}


@Composable
fun HistoricoVelocidadeScreen() {
    val testResultManager = TestResultManager(context = LocalContext.current)

    // Estado para armazenar os resultados
    var results = remember { testResultManager.getTestResults() }

    val context = LocalContext.current
    var showToast by remember { mutableStateOf(false) }

    // Layout principal
    Column(modifier = Modifier.padding(16.dp)) {
        Spacer(modifier = Modifier.height(10.dp))
        // Botão para limpar o histórico
        Button(onClick = {
            context.getSharedPreferences("test_results", Context.MODE_PRIVATE).edit().clear()
                .apply()
            results = testResultManager.getTestResults()
            showToast = true
        }) {
            Text("Limpar Histórico")
        }

        // LazyColumn para exibir os resultados
        LazyColumn(modifier = Modifier.fillMaxWidth()) {
            results.forEach { result -> // Itera sobre os resultados e exibe cada um
                item {
                    Card(
                        modifier = Modifier.padding(8.dp),
                        //backgroundColor = Color.LightGray
                    ) {
                        Column(modifier = Modifier.padding(8.dp)) {
                            Text(
                                text = "Data: ${result.timestamp}",
                                fontSize = 16.sp,
                                textAlign = TextAlign.Start
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = result.ping,
                                fontSize = 16.sp,
                                textAlign = TextAlign.Start
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = result.downloadSpeed,
                                fontSize = 16.sp,
                                textAlign = TextAlign.Start
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = result.uploadSpeed,
                                fontSize = 16.sp,
                                textAlign = TextAlign.Start
                            )

                        }
                    }
                }
            }

            // Exibição do Toast
            if (showToast) {
                Toast.makeText(context, "Histórico limpo", Toast.LENGTH_SHORT).show()
                showToast = false
            }
        }
    }
}

