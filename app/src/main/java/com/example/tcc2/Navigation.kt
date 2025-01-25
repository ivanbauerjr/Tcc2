package com.example.tcc2

import ConnectivityTestScreen
import DNSScreen
import RoteadorScreen
import TestedeVelocidadeScreen2
import NetworkScanScreen
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.tcc2.models.LocationViewModel

@Composable
fun Navigation(
    locationViewModel: LocationViewModel,
    onGetUserLocation: (callback: (Double, Double) -> Unit) -> Unit
) {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = "MainScreen"
    ) {
        composable("MainScreen") {
            MainScreen(
                navController = navController,
                onLocationDetermined = { lat, lon ->
                    locationViewModel.setLocation(lat, lon)
                }
            )
        }
        composable("TestedeVelocidadeScreen") {
            TestedeVelocidadeScreen(
                locationViewModel = locationViewModel,
                onGetUserLocation = onGetUserLocation,
                navController = navController
            )
        }
        composable("DNSScreen") {
            DNSScreen()
        }
        composable("TestedeVelocidadeScreen2") {
            TestedeVelocidadeScreen2()
        }
        composable("RedesProximasScreen") {
            RedesProximasScreen()
        }
        composable("RoteadorScreen") {
            RoteadorScreen()
        }
        composable("HistoricoVelocidadeScreen") {
            HistoricoVelocidadeScreen()
        }
        composable("ConnectivityTestScreen") {
            ConnectivityTestScreen()
        }
        composable("NetworkScanScreen") {
            NetworkScanScreen(context = LocalContext.current)
        }
    }
}
