import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.*
import android.net.wifi.WifiManager
import android.net.wifi.WifiInfo
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.Inet6Address
import java.net.InetAddress

@Composable
fun RoteadorScreen() {
    val context = LocalContext.current
    var ssid by remember { mutableStateOf("Carregando...") }
    var gatewayIpv6 by remember { mutableStateOf("Carregando...") }
    var gatewayIpv4 by remember { mutableStateOf("Carregando...") }
    var connectionStatus by remember { mutableStateOf("Carregando...") }
    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        coroutineScope.launch {
            val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            val activeNetwork: Network? = connectivityManager.activeNetwork
            val networkCapabilities: NetworkCapabilities? = connectivityManager.getNetworkCapabilities(activeNetwork)

            if (networkCapabilities?.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) == true) {
                ssid = getWifiSSID(context, connectivityManager) ?: "Desconhecido"
            }

            val linkProperties: LinkProperties? = connectivityManager.getLinkProperties(activeNetwork)

            val gatewayv4 = linkProperties?.routes?.firstOrNull {
                it.gateway is InetAddress && !it.gateway!!.isLoopbackAddress && it.destination.toString() == "0.0.0.0/0"
            }?.gateway?.hostAddress

            var gatewayv6 = linkProperties?.routes?.firstOrNull {
                it.gateway is Inet6Address && it.destination.toString() == "::/0"
            }?.gateway?.hostAddress

            if (gatewayv6 == null) {
                gatewayv6 = linkProperties?.linkAddresses?.firstOrNull { it.address is Inet6Address }?.address?.hostAddress
            }

            gatewayIpv6 = gatewayv6 ?: "Desconhecido"
            gatewayIpv4 = gatewayv4 ?: "Desconhecido"

            connectionStatus = withContext(Dispatchers.IO) {
                if (gatewayIpv4 != "Desconhecido" && isHostReachable(gatewayIpv4)) "Acessível" else "Não acessível"
            }
        }
    }

    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.Top,
        horizontalAlignment = Alignment.Start
    ) {
        Text(text = "Configurações do Roteador", fontSize = 35.sp, style = MaterialTheme.typography.titleLarge)
        Spacer(modifier = Modifier.height(40.dp))
        Text(text = "Nome da Rede (SSID): $ssid", fontSize = 22.sp)
        Spacer(modifier = Modifier.height(20.dp))
        Text(text = "Gateway IPv4: $gatewayIpv4", fontSize = 22.sp)
        Text(text = "Gateway IPv6: $gatewayIpv6", fontSize = 22.sp)
        Spacer(modifier = Modifier.height(20.dp))
        Text(text = "Status da Conexão: $connectionStatus", fontSize = 22.sp)
        Spacer(modifier = Modifier.height(40.dp))
        Button(
            onClick = { openRouterPage(context, gatewayIpv4) },
            enabled = gatewayIpv4 != "Desconhecido" && gatewayIpv4.isNotEmpty(),
            modifier = Modifier.align(Alignment.CenterHorizontally)
        ) {
            Text("Abrir Página do Roteador", fontSize = 22.sp)
        }
    }
}

fun openRouterPage(context: Context, gateway: String) {
    try {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse("http://$gateway"))
        context.startActivity(intent)
    } catch (e: Exception) {
        e.printStackTrace()
    }
}

fun getWifiSSID(context: Context, connectivityManager: ConnectivityManager): String? {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        val request = NetworkRequest.Builder()
            .addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
            .build()

        var ssidResult: String? = null
        val networkCallback = object : ConnectivityManager.NetworkCallback(FLAG_INCLUDE_LOCATION_INFO) {
            override fun onCapabilitiesChanged(network: Network, networkCapabilities: NetworkCapabilities) {
                val wifiInfo = networkCapabilities.transportInfo as? WifiInfo
                ssidResult = wifiInfo?.ssid?.removeSurrounding("\"")
                connectivityManager.unregisterNetworkCallback(this)
            }
        }
        connectivityManager.requestNetwork(request, networkCallback)
        connectivityManager.registerNetworkCallback(request, networkCallback)

        repeat(20) {
            if (ssidResult != null) return ssidResult
            Thread.sleep(250)
        }
        connectivityManager.unregisterNetworkCallback(networkCallback)
        return null
    }
    return "Não disponível"
}

fun isHostReachable(host: String): Boolean {
    return try {
        val address = InetAddress.getByName(host)
        address.isReachable(5000)
    } catch (e: Exception) {
        false
    }
}