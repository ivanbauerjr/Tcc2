package com.example.tcc2

import android.annotation.SuppressLint
import android.content.Context
import android.net.wifi.ScanResult
import android.net.wifi.WifiInfo
import android.net.wifi.WifiManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Divider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch

class WifiListActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            RedesProximasScreen()
        }
    }
}

// Função que retorna a cor com base no nível do sinal
@Composable
fun getSignalStrengthColor(rssi: Int): Color {
    return when {
        rssi > -50 -> Color.Green              // Ótima
        rssi > -70 -> Color(0xFF66BB6A)  // Boa (verde mais claro)
        rssi > -90 -> Color(0xFFFFA500)  // Fraca (Laranja)
        else -> Color.Red                     // Muito fraca
    }
}

@Composable
fun RedesProximasScreen() {
    val context = LocalContext.current
    val wifiAnalyzer = remember { WifiAnalyzer(context) }
    val coroutineScope = rememberCoroutineScope()
    var wifiNetworks by remember { mutableStateOf(emptyList<Triple<String, Int, String>>()) }
    var lastConnectedSignal by remember { mutableStateOf<Int?>(null) }
    val snackbarHostState = remember { SnackbarHostState() }

    // Função para atualizar redes Wi-Fi
    fun updateWifiNetworks() {
        coroutineScope.launch {
            try {
                val newNetworks = wifiAnalyzer.analyzeWifiNetworks().sortedByDescending { it.second }
                // Obtém a intensidade do sinal da rede conectada
                val currentConnectedSignal = wifiAnalyzer.getCurrentConnectedSignalStrength()

                // Só atualiza se os valores forem diferentes
                if (wifiNetworks != newNetworks || lastConnectedSignal != currentConnectedSignal) {
                    wifiNetworks = newNetworks
                    lastConnectedSignal = currentConnectedSignal
                    snackbarHostState.showSnackbar("Valores atualizados com sucesso!")
                }
            } catch (e: SecurityException) {
                snackbarHostState.showSnackbar("Permissão não concedida para acessar redes Wi-Fi")
            }
        }
    }

    // Carrega as redes Wi-Fi na inicialização, mas sem exibir mensagem
    LaunchedEffect(Unit) {
        wifiNetworks = wifiAnalyzer.analyzeWifiNetworks().sortedByDescending { it.second }
        lastConnectedSignal = wifiAnalyzer.getCurrentConnectedSignalStrength()
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        content = { paddingValues ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(16.dp)
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.fillMaxSize()
                ) {
                    Text(
                        text = "Redes Wi-Fi Próximas",
                        style = MaterialTheme.typography.titleLarge,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )

                    LazyColumn(
                        modifier = Modifier.weight(1f) // Ocupa o espaço restante
                    ) {
                        items(wifiNetworks) { (networkName, signalStrength, security) ->
                            WifiNetworkItem(
                                networkName = networkName,
                                signalStrength = signalStrength,
                                securityType = security
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp)) // Espaço entre a lista e o botão

                    // Botão centralizado no final
                    Button(
                        onClick = { updateWifiNetworks() },
                        modifier = Modifier.align(Alignment.CenterHorizontally)
                    ) {
                        Text(text = "Atualizar")
                    }
                }
            }
        }
    )
}

@Composable
fun WifiNetworkItem(networkName: String, signalStrength: Int, securityType: String) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp, horizontal = 16.dp)
    ) {
        Text(
            text = networkName,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = Color.Black
        )
        Text(
            text = "Intensidade do sinal: $signalStrength dBm",
            style = MaterialTheme.typography.bodyMedium,
            color = getSignalStrengthColor(signalStrength)
        )
        Text(
            text = "Segurança: $securityType",
            style = MaterialTheme.typography.bodySmall,
            color = Color.Blue
        )
        Divider(
            color = Color.Gray.copy(alpha = 0.3f),
            thickness = 1.dp,
            modifier = Modifier.padding(vertical = 8.dp)
        )
    }
}

class WifiAnalyzer(private val context: Context) {

    @SuppressLint("MissingPermission")
    fun analyzeWifiNetworks(): List<Triple<String, Int, String>> {
        val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager

        if (wifiManager.scanResults.isEmpty()) {
            throw SecurityException("Acesso a redes Wi-Fi não permitido")
        }

        return wifiManager.scanResults.map { result ->
            val networkName = result.SSID.takeIf { it.isNotEmpty() } ?: "<Rede Desconhecida>"
            val signalStrength = result.level
            val securityType = getSecurityType(result)

            Triple(networkName, signalStrength, securityType)
        }
        .filterNot { it.first == "<Rede Desconhecida>" } // Remove redes desconhecidas
        .distinctBy { it.first } // Remove redes duplicadas pelo nome (SSID)
    }

    @SuppressLint("MissingPermission")
    fun getCurrentConnectedSignalStrength(): Int? {
        val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
        val wifiInfo: WifiInfo? = wifiManager.connectionInfo
        return wifiInfo?.rssi
    }

    private fun getSecurityType(scanResult: ScanResult): String {
        return when {
            scanResult.capabilities.contains("WPA3") -> "WPA3"
            scanResult.capabilities.contains("WPA2") -> "WPA2"
            scanResult.capabilities.contains("WPA") -> "WPA"
            scanResult.capabilities.contains("WEP") -> "WEP"
            scanResult.capabilities.contains("EAP") -> "Enterprise"
            else -> "Aberta"
        }
    }
}
