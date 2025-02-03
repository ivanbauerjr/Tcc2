package com.example.tcc2

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material.icons.filled.Devices
import androidx.compose.material.icons.filled.NetworkCheck
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Router
import androidx.compose.material.icons.filled.WifiFind
import com.example.tcc2.ui.theme.ActionButton


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdvancedOptionsScreen(navController: NavHostController) {
    Scaffold(
        topBar = { TopAppBar(title = { Text("Opções Avançadas") }) }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.Top,
            horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally
        ) {
            Text(
                text = "Configurações Avançadas",
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            // Buttons with the new style
            ActionButton("Sistema de Nomes de Domínio", Icons.Default.Public, "DNSScreen", navController)
            ActionButton("Redes Wi-Fi Próximas", Icons.Default.WifiFind, "RedesProximasScreen", navController)
            ActionButton("Configurações do Roteador", Icons.Default.Router, "RoteadorScreen", navController)
            ActionButton("Teste de Conectividade", Icons.Default.NetworkCheck, "ConnectivityTestScreen", navController)
            ActionButton("Escaneamento da Rede", Icons.Default.Devices, "NetworkScanScreen", navController)
        }
    }
}

//@Composable
//fun ActionButton(text: String, icon: ImageVector, route: String, navController: NavHostController) {
//    Button(
//        onClick = { navController.navigate(route) },
//        modifier = Modifier
//            .fillMaxWidth()
//            .height(80.dp) // Thicker buttons
//            .padding(vertical = 10.dp), // Spacing between buttons
//        shape = RoundedCornerShape(12.dp) // Soft rounded corners
//    ) {
//        Icon(icon, contentDescription = text, modifier = Modifier.size(32.dp)) // Bigger icon
//        Spacer(modifier = Modifier.width(12.dp))
//        Text(text, fontSize = 20.sp) // Bigger font
//    }
//}
