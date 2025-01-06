package com.example.tcc2

import DNSScreen
import TestedeVelocidadeScreen
import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController


//enum class Screen {
//    Main,
//    DNS
//}

@Composable
fun Navigation() {
    val navController = rememberNavController()
    NavHost(
        navController = navController,
        startDestination = "MainScreen"
    ) {
        composable("MainScreen") {
            MainScreen(navController)
        }
        composable("DNSScreen") {
            DNSScreen()
        }
        composable("TestedeVelocidadeScreen") {
            TestedeVelocidadeScreen()
        }
        composable("RedesProximasScreen") {
            RedesProximasScreen()
        }
    }
}