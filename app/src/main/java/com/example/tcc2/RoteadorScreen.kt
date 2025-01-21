
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
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
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
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
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
            //val gatewayIp = linkProperties?.routes?.firstOrNull { it.gateway != null }?.gateway?.hostAddress

            //ipv6?
            val gatewayv6 = linkProperties?.linkAddresses?.firstOrNull()?.address?.hostAddress

            //ipv4?
            val gatewayv4 = linkProperties?.routes?.firstOrNull {
            it.gateway is InetAddress && it.gateway!!.hostAddress?.contains(":") == false
        }?.gateway?.hostAddress

            gatewayIpv6 = gatewayv6 ?: "Desconhecido"
            gatewayIpv4 = gatewayv4 ?: "Desconhecido"

            // Testar se o Gateway está acessível
            connectionStatus = withContext(Dispatchers.IO) {
                if (isHostReachable(gatewayIpv4)) "Acessível" else "Não acessível"
            }
        }
    }



    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Top,
        horizontalAlignment = Alignment.Start
    ) {
        // Exibir as informações do roteador
        Box(
            modifier = Modifier.fillMaxWidth(),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "Configurações do Roteador",
                fontSize = 35.sp,
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(top = 16.dp)  // Adiciona um espaço acima do texto
            )
        }

        Spacer(modifier = Modifier.height(60.dp))


        Text(
            text = "Nome da Rede (SSID): $ssid",
            fontSize = 22.sp,
        )


        Spacer(modifier = Modifier.height(40.dp))

        Text(
            text = "Gateway:",
            fontSize = 22.sp
        )
        Spacer(modifier = Modifier.height(4.dp))
        // Endereço IPv4
        Text(
            text = "Endereço IPv4: $gatewayIpv4",
            fontSize = 22.sp,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.padding(start = 18.dp)
        )
        Spacer(modifier = Modifier.height(10.dp))

        // Endereço IPv6
        Text(
            text = "Endereço IPv6: $gatewayIpv6",
            fontSize = 22.sp,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.padding(start = 18.dp)
        )
        Spacer(modifier = Modifier.height(40.dp))

        Text(
            text = "Status da Conexão: $connectionStatus",
            fontSize = 22.sp,
        )

        Spacer(modifier = Modifier.height(40.dp))

        Button(
            onClick = {
                // Só abrir o navegador se o gateway for válido
                if (gatewayIpv4 != "Desconhecido" && gatewayIpv4.isNotEmpty()) {
                    openRouterPage(context, gatewayIpv4)
                } else {
                    // Exibir uma mensagem ou lógica para o caso de o gateway não ser válido
                    // Exemplo: Snackbar ou Toast
                }
            },
            modifier = Modifier.align(Alignment.CenterHorizontally) // Alinha o botão no centro da coluna
        ) {
            Text(
                "Abrir Página do Roteador",
                fontSize = 22.sp
            )
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