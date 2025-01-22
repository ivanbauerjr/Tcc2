package com.example.tcc2

import ConnectivityTestScreen
import DNSScreen
import RoteadorScreen
import TestedeVelocidadeScreen2
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController

@Composable
fun Navigation(
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
                onGetUserLocation = onGetUserLocation
            )
        }
        composable("DNSScreen") {
            DNSScreen()
        }
        composable("TestedeVelocidadeScreen") {
            TestedeVelocidadeScreen(
                onGetUserLocation = onGetUserLocation,
                modifier = Modifier,
                navController = navController
            )
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
    }
}
