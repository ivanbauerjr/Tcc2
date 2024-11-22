import android.annotation.SuppressLint
import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import java.net.InetAddress
import java.net.UnknownHostException


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RedesProximasScreen() {
    val context = LocalContext.current
    // Estado para armazenar o domínio inserido
    var domain by remember { mutableStateOf(TextFieldValue("")) }
    // Estado para armazenar o resultado da resolução
    var result by remember { mutableStateOf("") }
    // Estado para controlar se está carregando
    var isLoading by remember { mutableStateOf(false) }
    // Estado para armazenar o DNS ativo
    var activeDNS by remember { mutableStateOf("") }

    // Obter o DNS ativo quando a tela for carregada
    LaunchedEffect(Unit) {
        activeDNS = getActiveDNS(context)
    }

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Resolução de DNS") })
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Campo de entrada para o domínio
            TextField(
                value = domain,
                onValueChange = { domain = it },
                label = { Text("Digite o domínio") },
                modifier = Modifier.fillMaxWidth()
            )

            // Botão para resolver DNS
            Button(
                onClick = {
                    isLoading = true
                    resolveDNS(domain.text) { dnsResult ->
                        result = dnsResult
                        isLoading = false
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = domain.text.isNotBlank() && !isLoading
            ) {
                if (isLoading) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp))
                } else {
                    Text("Resolver DNS")
                }
            }

            // Exibição do resultado
            Text(
                text = result,
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.fillMaxWidth()
            )

            // Exibição do DNS ativo
            Text(
                text = "DNS ativo: $activeDNS",
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}


