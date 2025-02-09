package com.example.tcc2

import android.content.Context
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import kotlin.reflect.KFunction2
import android.net.wifi.WifiManager
import android.os.Build
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.wifi.WifiInfo
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavHostController
import androidx.compose.ui.platform.LocalContext
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.filled.Build
import com.example.tcc2.ui.theme.ActionButton


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    navController: NavHostController,
    onLocationDetermined: KFunction2<Double, Double, Unit>,
) {
    Scaffold(
        topBar = { TopAppBar(title = { Text("Monitor de Rede") }) }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.Top, // Keeps items at the top
            horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally
        ) {
            // Display WiFi SSID and Network Status
            NetworkInfoCard()

            Spacer(modifier = Modifier.height(24.dp)) // Space between card and buttons

            // Buttons
            ActionButton("Diagnóstico de Rede", Icons.Default.Build, "NetworkDiagnosticsScreen", navController)
            ActionButton("Velocidade da Internet", Icons.Default.Speed, "TestedeVelocidadeScreen", navController)
            ActionButton("Opções Avançadas", Icons.Default.Settings, "AdvancedOptionsScreen", navController)
        }
    }
}

@Composable
fun NetworkInfoCard() {
    val context = LocalContext.current
    var ssid by remember { mutableStateOf("Carregando...") }

    LaunchedEffect(Unit) {
        ssid = getWifiSSID(context) ?: "Desconhecido"
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        shape = RoundedCornerShape(12.dp) // Rounded edges for a modern look
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally
        ) {
            Text("Rede Conectada", fontSize = 22.sp, fontWeight = FontWeight.Bold)
            Text(ssid, fontSize = 18.sp, color = Color.Gray)
        }
    }
}

fun getWifiSSID(context: Context): String? {
    val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    val activeNetwork = connectivityManager.activeNetwork
    val networkCapabilities = connectivityManager.getNetworkCapabilities(activeNetwork)

    if (networkCapabilities?.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) == true) {
        val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
        val wifiInfo: WifiInfo? = wifiManager.connectionInfo

        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            wifiInfo?.ssid?.removeSurrounding("\"") // Removes surrounding quotes
        } else {
            wifiInfo?.ssid
        }
    }
    return "Desconhecido"
}
