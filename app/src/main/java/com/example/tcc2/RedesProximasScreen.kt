package com.example.tcc2

import android.annotation.SuppressLint
import android.content.Context
import android.net.wifi.ScanResult
import android.net.wifi.WifiManager
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

class WifiListActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            RedesProximasScreen()
        }
    }
}

@Composable
fun RedesProximasScreen() {
    // Cria uma instância do WifiAnalyzer
    val wifiAnalyzer = WifiAnalyzer(LocalContext.current)

    // Tenta obter as redes Wi-Fi
    val wifiNetworks = try {
        wifiAnalyzer.analyzeWifiNetworks()
    } catch (e: SecurityException) {
        // Caso a permissão não seja concedida, exibe um erro
        Toast.makeText(LocalContext.current, "Permissão não concedida para acessar redes Wi-Fi", Toast.LENGTH_LONG).show()
        emptyList<Pair<Any, Int>>()
    }

    // Exibe a lista de redes Wi-Fi com LazyColumn
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.padding(16.dp)
    ) {
        Text(
            text = "Redes Wi-Fi Disponíveis",
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.padding(bottom = 16.dp)
        )
        LazyColumn {
            items(wifiNetworks) { (networkName, signalStrength) ->
                WifiNetworkItem(networkName = networkName.toString(), signalStrength = signalStrength)
            }
        }
    }
}

@Composable
fun WifiNetworkItem(networkName: String, signalStrength: Int) {
    // Exibe cada item da lista
    Column(modifier = Modifier.padding(bottom = 12.dp)) {
        Text(
            text = networkName,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Bold,
            color = Color.Black
        )
        Text(
            text = "Intensidade do sinal: $signalStrength dBm",
            style = MaterialTheme.typography.bodyMedium,
            color = Color.Gray
        )
    }
}

class WifiAnalyzer(private val context: Context) {

    // Metodo para obter a lista de redes Wi-Fi disponíveis e suas intensidades de sinal
    @SuppressLint("NewApi", "MissingPermission")
    fun analyzeWifiNetworks(): MutableList<Pair<Any, Int>> {
        val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager

        // Realiza a varredura das redes Wi-Fi disponíveis
        val scanResults: List<ScanResult> = wifiManager.scanResults

        // Lista para armazenar os nomes das redes Wi-Fi e suas intensidades de sinal
        val wifiNetworks: MutableList<Pair<Any, Int>> = mutableListOf()

        // Itera sobre os resultados da varredura e adiciona as redes Wi-Fi à lista
        for (result in scanResults) {
            val networkName = result.wifiSsid ?: "<Rede Desconhecida>" // Substitui nulo por uma string padrão
            val signalStrength = result.level // Intensidade do sinal da rede Wi-Fi
            wifiNetworks.add(Pair(networkName, signalStrength))
        }

        // Retorna a lista de redes Wi-Fi e suas intensidades de sinal
        return wifiNetworks
    }
}