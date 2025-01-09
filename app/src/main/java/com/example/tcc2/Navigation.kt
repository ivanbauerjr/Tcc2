package com.example.tcc2

import DNSScreen
import androidx.compose.runtime.Composable
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
                onGetUserLocation = onGetUserLocation

            )
        }
        composable("RedesProximasScreen") {
            RedesProximasScreen()
        }
    }
}
