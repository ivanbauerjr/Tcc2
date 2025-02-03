package com.example.tcc2

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import kotlin.reflect.KFunction2

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    navController: NavHostController,
    onLocationDetermined: KFunction2<Double, Double, Unit>,
) {
    Scaffold(
        topBar = { TopAppBar(title = { Text("Tela Principal") }) }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentAlignment = androidx.compose.ui.Alignment.Center
        ) {
            Column(
                horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Button(onClick = { navController.navigate("NetworkDiagnosticsScreen") }) {
                    Text(text = "Diagnóstico de Rede", fontSize = 18.sp)
                }
                Button(onClick = { navController.navigate("TestedeVelocidadeScreen") }) {
                    Text(text = "Teste de Velocidade", fontSize = 18.sp)
                }
                Button(onClick = { navController.navigate("AdvancedOptionsScreen") }) {
                    Text(text = "Opções Avançadas", fontSize = 18.sp)
                }
            }
        }
    }
}