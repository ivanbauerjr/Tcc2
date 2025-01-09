package com.example.tcc2

import android.util.Log
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.*
import org.jsoup.Jsoup
import org.jsoup.parser.Parser
import java.math.BigDecimal
import java.math.RoundingMode
import java.net.HttpURLConnection
import java.net.URL
import javax.net.ssl.HttpsURLConnection
import javax.net.ssl.SSLSocketFactory
import kotlin.math.*
import com.example.tcc2.models.Server
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

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



@Composable
fun TestedeVelocidadeScreen(
    modifier: Modifier = Modifier,
    onGetUserLocation: ((Double, Double) -> Unit) -> Unit
) {
    var resultText by remember { mutableStateOf("Click the button to start testing!") }
    var isTestRunning by remember { mutableStateOf(false) }
    var progress by remember { mutableStateOf(0f) }
    val coroutineScope = rememberCoroutineScope()
    var userLocation by remember { mutableStateOf<Pair<Double, Double>?>(null) }
    var closestServer by remember { mutableStateOf<Server?>(null) }
    var availableServers by remember { mutableStateOf<List<Server>>(emptyList()) }
    var isServerDialogVisible by remember { mutableStateOf(false) }

    // Fetch user location
    LaunchedEffect(Unit) {
        onGetUserLocation { lat, lon ->
            userLocation = Pair(lat, lon)
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Top
    ) {
        Text(
            text = "Location: ${
                userLocation?.let { "${it.first}, ${it.second}" } ?: "Fetching..."
            }",
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        Button(
            onClick = {
                if (userLocation != null) {
                    coroutineScope.launch {
                        isTestRunning = true
                        resultText = "Fetching servers and measuring latency..."
                        val servers = fetchServersWithLatency(userLocation!!.first, userLocation!!.second)
                        availableServers = servers.take(10) // Show the 10 best servers by latency
                        isTestRunning = false
                    }
                } else {
                    resultText = "Error: Unable to fetch user location."
                }
            },
            enabled = userLocation != null && !isTestRunning
        ) {
            Text(text = "Fetch Servers with Latency")
        }



        Spacer(modifier = Modifier.height(16.dp))

        if (closestServer != null) {
            Text(
                text = "Closest Server: ${closestServer?.name} (${closestServer?.sponsor})\nDistance: ${round(closestServer?.distance ?: 0.0, 2)} km\nURL: ${closestServer?.url}",
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.padding(bottom = 16.dp)
            )
        }

        Button(
            onClick = { isServerDialogVisible = true },
            enabled = availableServers.isNotEmpty()
        ) {
            Text("Select Server")
        }

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

        closestServer?.let { server ->
            Text(
                text = resultText,
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(16.dp))
            LinearProgressIndicator(
                progress = { progress },
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
                            val downloadSpeedResult = measureDownloadSpeed(server.url) { currentProgress ->
                                progress = currentProgress
                            }
                            val uploadSpeedResult = measureUploadSpeed(server.url) { currentProgress ->
                                progress = 0.5f + (currentProgress / 2f) // Continue progress after download
                            }
                            resultText = "Ping: $pingResult\nDownload: $downloadSpeedResult\nUpload: $uploadSpeedResult"
                            progress = 1f
                            isTestRunning = false
                        }
                    }
                },
                enabled = !isTestRunning
            ) {
                Text(text = if (isTestRunning) "Testing..." else "Start Test")
            }
        }
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


fun findClosestServer(servers: List<Server>, userLat: Double, userLon: Double): Server? {
    servers.forEach {
        it.distance = calculateDistance(userLat, userLon, it.lat, it.lon)
    }
    return servers.minByOrNull { it.distance }
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
