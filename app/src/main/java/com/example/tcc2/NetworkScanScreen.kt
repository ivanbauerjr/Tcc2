
import android.content.Context
import android.net.wifi.WifiManager
import android.text.format.Formatter
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.InetAddress

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NetworkScanScreen(context: Context) {
    var devices by remember { mutableStateOf<List<String>>(emptyList()) }
    var isScanning by remember { mutableStateOf(false) }
    var phoneIpAddress by remember { mutableStateOf<String?>(null) }
    var lastDevices by remember { mutableStateOf<List<String>>(emptyList()) }
    var statusMessage by remember { mutableStateOf<String?>(null) }

    // Get the phone's IP address
    LaunchedEffect(Unit) {
        phoneIpAddress = getPhoneIpAddress(context)
    }

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Escaneador de Rede") })
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Button(onClick = {
                    isScanning = true
                    val subnet = getSubnet(context)
                    scanNetwork(subnet) { results ->
                        val addedDevices = results - lastDevices
                        val removedDevices = lastDevices - results

                        statusMessage = when {
                            addedDevices.isNotEmpty() && removedDevices.isNotEmpty() ->
                                "Dispositivos adicionados desde o último escaneamento: ${addedDevices.joinToString()}. Dispositivos removidos desde o último escaneamento: ${removedDevices.joinToString()}"
                            addedDevices.isNotEmpty() -> "Novo(s) dispositivo(s) encontrado(s) desde o último escaneamento: ${addedDevices.joinToString()}"
                            removedDevices.isNotEmpty() -> "Dispositivo(s) removido(s) desde o último escaneamento: ${removedDevices.joinToString()}"
                            else -> "Nenhuma alteração detectada desde o último escaneamento."
                        }

                        lastDevices = results
                        devices = results
                        isScanning = false
                    }
                }) {
                    Text(text = if (isScanning) "Escaneamento em andamento..." else "Iniciar escaneamento da rede", fontSize = 18.sp)
                }



                if (devices.isNotEmpty()) {
                    Text(text = "Dispositivos encontrados: ${devices.size}", fontSize = 18.sp)

                    // Display phone's IP address first
                    phoneIpAddress?.let { phoneIp ->
                        if (devices.contains(phoneIp)) {
                            Text(text = "Seu dispositivo: $phoneIp", fontSize = 16.sp)
                        }
                    }

                    // Display all other devices except the phone's IP
                    devices.filter { it != phoneIpAddress }.forEach { ip ->
                        Text(text = ip, fontSize = 16.sp)
                    }
                } else if (!isScanning) {
                    Text(text = "Nenhum dispositivo encontrado.", fontSize = 16.sp)
                }

                statusMessage?.let {
                    Box(
                        modifier = Modifier.fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(text = it, fontSize = 16.sp, color = Color.Red, textAlign = TextAlign.Center)
                    }
                }
            }
        }
    }
}

fun getSubnet(context: Context): String {
    val wifiManager = context.getSystemService(Context.WIFI_SERVICE) as WifiManager
    val ipAddress = Formatter.formatIpAddress(wifiManager.connectionInfo.ipAddress)
    return ipAddress.substringBeforeLast(".") // e.g., "192.168.15"
}

fun getPhoneIpAddress(context: Context): String {
    val wifiManager = context.getSystemService(Context.WIFI_SERVICE) as WifiManager
    return Formatter.formatIpAddress(wifiManager.connectionInfo.ipAddress)
}

fun scanNetwork(subnet: String, onResult: (List<String>) -> Unit) {
    CoroutineScope(Dispatchers.IO).launch {
        val devices = mutableListOf<String>()

        // Ping the subnet to find devices
        val jobs = (1..254).map { i ->
            async {
                val ip = "$subnet.$i"
                try {
                    val address = InetAddress.getByName(ip)
                    if (address.isReachable(200)) {
                        ip // Add IP to the list
                    } else null
                } catch (e: Exception) {
                    null
                }
            }
        }

        devices.addAll(jobs.awaitAll().filterNotNull())

        withContext(Dispatchers.Main) {
            onResult(devices)
        }
    }
}
