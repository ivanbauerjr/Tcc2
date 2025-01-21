
import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.LinkProperties
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.net.Uri
import android.net.wifi.WifiInfo
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.InetAddress

@Composable
fun RoteadorScreen() {
    val context = LocalContext.current
    var ssid by remember { mutableStateOf("Carregando...") }
    var gateway by remember { mutableStateOf("Carregando...") }
    var connectionStatus by remember { mutableStateOf("Carregando...") }

    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        coroutineScope.launch {
            val connectivityManager =
                context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

            val activeNetwork: Network? = connectivityManager.activeNetwork
            val networkCapabilities: NetworkCapabilities? = connectivityManager.getNetworkCapabilities(activeNetwork)

            networkCapabilities?.let { capabilities ->
                // Se a conexão for Wi-Fi, obter informações de SSID
                if (capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)) {
                    ssid = getWifiSSID(context) ?: "Desconhecido"

                }
            }

            // Obter LinkProperties para o gateway
            val linkProperties: LinkProperties? = connectivityManager.getLinkProperties(activeNetwork)

            // Obter o Gateway (Roteador)
            val gatewayIp = linkProperties?.routes?.firstOrNull { it.gateway != null }?.gateway?.hostAddress
            gateway = gatewayIp ?: "Desconhecido"

            // Testar se o Gateway está acessível
            connectionStatus = withContext(Dispatchers.IO) {
                if (gatewayIp != null && isHostReachable(gatewayIp)) "Acessível" else "Não acessível"
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(text = "Configurações do Roteador", style = MaterialTheme.typography.titleLarge)
        Spacer(modifier = Modifier.height(16.dp))
        Text(text = "Nome da Rede (SSID): $ssid")
        Text(text = "Gateway (Roteador): $gateway")
        Text(text = "Status da Conexão: $connectionStatus")
        Spacer(modifier = Modifier.height(24.dp))
        Button(
            onClick = {
                // Só abrir o navegador se o gateway for válido
                if (gateway != "Desconhecido" && gateway.isNotEmpty()) {
                    openRouterPage(context, gateway)
                } else {
                    // Exibir uma mensagem ou lógica para o caso de o gateway não ser válido
                    // Exemplo: Snackbar ou Toast
                }
            }
        ) {
            Text("Abrir Configuração do Roteador")
        }
    }
}

fun openRouterPage(context: Context, gateway: String) {
    try {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse("http://$gateway"))
        context.startActivity(intent)
    } catch (e: Exception) {
        // Tratar erros ao tentar abrir a página
        e.printStackTrace()
    }
}

//@SuppressLint("NewApi")
//fun getWifiSSID(context: Context): String? {
//    val connectivityManager =
//        context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
//    val activeNetwork: Network? = connectivityManager.activeNetwork
//    val networkCapabilities = connectivityManager.getNetworkCapabilities(activeNetwork)
//
//    return if (networkCapabilities != null && networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)) {
//        val wifiInfo = networkCapabilities.transportInfo as? android.net.wifi.WifiInfo
//        wifiInfo?.ssid?.removePrefix("\"")?.removeSuffix("\"") // Remove as aspas
//    } else {
//        null
//    }
//}

fun getWifiSSID(context: Context): String? {
    val ssidList = mutableListOf<String>()
    val connectivityManager = context.getSystemService(ConnectivityManager::class.java)

    val request = NetworkRequest.Builder()
        .addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
        .build()

    val networkCallback = @RequiresApi(Build.VERSION_CODES.S)
    object : ConnectivityManager.NetworkCallback(FLAG_INCLUDE_LOCATION_INFO) {
        override fun onCapabilitiesChanged(network: Network, networkCapabilities: NetworkCapabilities) {
            val wifiInfo = networkCapabilities.transportInfo as WifiInfo
            ssidList.add(wifiInfo.ssid)
            connectivityManager.unregisterNetworkCallback(this)
        }
    }

    connectivityManager.requestNetwork(request, networkCallback)
    connectivityManager.registerNetworkCallback(request, networkCallback)

    repeat(20) { // Espera 5 segundos no total (20 x 250ms)
        if (ssidList.isNotEmpty()) return ssidList.first()
        Thread.sleep(250)
    }

    connectivityManager.unregisterNetworkCallback(networkCallback)
    return null
}



fun isHostReachable(host: String): Boolean {
    return try {
        val address = InetAddress.getByName(host)
        address.isReachable(5000) // Tempo limite de 5 segundos
    } catch (e: Exception) {
        false
    }
}