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
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    navController: NavHostController,
    onGetUserLocation: (callback: (Double, Double) -> Unit) -> Unit
) {
    var userLocation by remember { mutableStateOf<Pair<Double, Double>?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Main Screen") })
        }
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
                Button(onClick = { navController.navigate("DNSScreen") }) {
                    Text(text = "DNS", fontSize = 18.sp)
                }
                Button(onClick = { navController.navigate("TestedeVelocidadeScreen") }) {
                    Text(text = "Teste de Velocidade", fontSize = 18.sp)
                }
                Button(onClick = { navController.navigate("RedesProximasScreen") }) {
                    Text(text = "Redes Próximas (dB)", fontSize = 18.sp)
                }
                Button(onClick = {
                    onGetUserLocation { latitude, longitude ->
                        userLocation = latitude to longitude
                    }
                }) {
                    Text(text = "Get User Location", fontSize = 18.sp)
                }
                userLocation?.let { (latitude, longitude) ->
                    Text(text = "Latitude: $latitude, Longitude: $longitude", fontSize = 16.sp)
                }
            }
        }
    }
}
