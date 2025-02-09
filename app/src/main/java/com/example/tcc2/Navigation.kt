package com.example.tcc2

import ConnectivityTestScreen
import DNSScreen
import HistoricoEscaneamentoScreen
import NetworkScanScreen
import RoteadorScreen
import android.content.Context
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.tcc2.models.LocationViewModel

@ExperimentalMaterial3Api
@Composable
fun Navigation(
    locationViewModel: LocationViewModel,
    onGetUserLocation: (callback: (Double, Double) -> Unit) -> Unit
) {
    val navController = rememberNavController()

    // Criar uma instância do contexto para acessar SharedPreferences
    val context = LocalContext.current

    // Função para limpar feedback do teste de velocidade
    fun clearFeedback() {
        val sharedPreferences = context.getSharedPreferences("speed_test_prefs", Context.MODE_PRIVATE)
        sharedPreferences.edit().putString("feedback_message", "").apply()
    }

    NavHost(
        navController = navController,
        startDestination = "MainScreen"
    ) {
        composable("MainScreen") {
            MainScreen(
                navController = navController,
                onLocationDetermined = locationViewModel::setLocation
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
        composable("NetworkDiagnosticsScreen") {
            NetworkDiagnosticsScreen(navController = navController)
        }
        composable("NetworkDiagnosticsHistoryScreen") {
            NetworkDiagnosticsHistoryScreen()
        }
        composable("RedesProximasScreen") {
            RedesProximasScreen()
        }
        composable("RoteadorScreen") {
            RoteadorScreen()
        }
        composable("HistoricoVelocidadeScreen") {
            HistoricoVelocidadeScreen(clearFeedback = { clearFeedback() })
        }
        composable("ConnectivityTestScreen") {
            ConnectivityTestScreen(context = LocalContext.current)
        }
        composable("NetworkScanScreen") {
            NetworkScanScreen(navController = navController)
        }
        composable("HistoricoScanScreen") {
            HistoricoEscaneamentoScreen()
        }
        composable("AdvancedOptionsScreen") {
            AdvancedOptionsScreen(navController = navController)
        }
    }
}
