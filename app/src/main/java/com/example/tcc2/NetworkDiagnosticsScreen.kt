package com.example.tcc2

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.wifi.WifiManager
import android.util.Log
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.InetAddress

@ExperimentalMaterial3Api
@Composable
fun NetworkDiagnosticsScreen() {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var connectionType by remember { mutableStateOf("Carregando...") }
    var internetAccess by remember { mutableStateOf("Carregando...") }
    var dnsResolution by remember { mutableStateOf("Carregando...") }
    var wifiSignalStrength by remember { mutableStateOf("Carregando...") }
    var gatewayReachability by remember { mutableStateOf("Carregando...") }

    LaunchedEffect(Unit) {
        coroutineScope.launch {
            connectionType = getConnectionType(context)
            internetAccess = if (isInternetAvailable()) "Disponível" else "Indisponível"
            dnsResolution = if (resolveDNS("google.com")) "Resolvido com sucesso" else "Falha na resolução"
            wifiSignalStrength = getWifiSignalStrength(context)
            gatewayReachability = if (isGatewayReachable(context)) "Acessível" else "Não acessível"
        }
    }

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
            Text("Resultados do Diagnóstico", style = MaterialTheme.typography.headlineMedium)

            DiagnosticResult(label = "Tipo de Conexão", value = connectionType)
            DiagnosticResult(label = "Acesso à Internet", value = internetAccess)
            DiagnosticResult(label = "Resolução de DNS", value = dnsResolution)
            DiagnosticResult(label = "Intensidade do WiFi", value = wifiSignalStrength)
            DiagnosticResult(label = "Acessibilidade do Gateway", value = gatewayReachability)

            Spacer(modifier = Modifier.height(20.dp))
            Button(onClick = {
                coroutineScope.launch {
                    connectionType = getConnectionType(context)
                    internetAccess = if (isInternetAvailable()) "Disponível" else "Indisponível"
                    dnsResolution = if (resolveDNS("google.com")) "Resolvido com sucesso" else "Falha na resolução"
                    wifiSignalStrength = getWifiSignalStrength(context)
                    gatewayReachability = if (isGatewayReachable(context)) "Acessível" else "Não acessível"
                }
            }) {
                Text("Atualizar Diagnóstico")
            }
        }
    }
}

@Composable
fun DiagnosticResult(label: String, value: String) {
    Card(modifier = Modifier.fillMaxWidth().padding(4.dp), elevation = CardDefaults.cardElevation(4.dp)) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(text = label, style = MaterialTheme.typography.bodyLarge)
            Text(text = value, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary)
        }
    }
}

fun getConnectionType(context: Context): String {
    val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    val network = connectivityManager.activeNetwork ?: return "Sem Conexão"
    val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return "Sem Conexão"

    return when {
        capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> "WiFi"
        capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> "Dados Móveis"
        capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> "Ethernet"
        else -> "Desconhecido"
    }
}

suspend fun isInternetAvailable(): Boolean {
    return withContext(Dispatchers.IO) {
        try {
            val address = InetAddress.getByName("8.8.8.8")
            !address.equals("")
        } catch (e: Exception) {
            false
        }
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

suspend fun isGatewayReachable(context: Context): Boolean {
    return withContext(Dispatchers.IO) {
        try {
            val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
            val gatewayIp = wifiManager.dhcpInfo.gateway
            val gatewayAddress = InetAddress.getByAddress(byteArrayOf(
                (gatewayIp and 0xFF).toByte(),
                (gatewayIp shr 8 and 0xFF).toByte(),
                (gatewayIp shr 16 and 0xFF).toByte(),
                (gatewayIp shr 24 and 0xFF).toByte()
            ))
            gatewayAddress.isReachable(3000)
        } catch (e: Exception) {
            false
        }
    }
}
