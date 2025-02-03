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
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Button(onClick = { navController.navigate("DNSScreen") }) {
                Text(text = "Sistema de Nomes de Domínio (DNS)", fontSize = 18.sp)
            }
            Button(onClick = { navController.navigate("RedesProximasScreen") }) {
                Text(text = "Redes WiFi Próximas", fontSize = 18.sp)
            }
            Button(onClick = { navController.navigate("RoteadorScreen") }) {
                Text(text = "Configurações do Roteador", fontSize = 18.sp)
            }
            Button(onClick = { navController.navigate("ConnectivityTestScreen") }) {
                Text(text = "Teste de Conectividade", fontSize = 18.sp)
            }
            Button(onClick = { navController.navigate("NetworkScanScreen") }) {
                Text(text = "Escaneamento da Rede", fontSize = 18.sp)
            }
        }
    }
}
