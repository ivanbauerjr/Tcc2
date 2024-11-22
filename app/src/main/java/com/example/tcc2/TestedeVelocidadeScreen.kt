import android.annotation.SuppressLint
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
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
fun TestedeVelocidadeScreen() {
    // Estado para armazenar o domínio inserido
    var domain by remember { mutableStateOf(TextFieldValue("")) }
    // Estado para armazenar os resultados das resoluções
    var result by remember { mutableStateOf("") }
    var resultCloudflareDNS by remember { mutableStateOf("") }
    // Estado para controlar se está carregando
    var isLoading by remember { mutableStateOf(false) }

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
                    // Resolver DNS para o domínio inserido
                    resolveDNS(domain.text) { dnsResult ->
                        result = dnsResult
                        isLoading = false
                    }
                    // Resolver DNS usando 1.1.1.1
                    resolveUsingCloudflareDNS(domain.text) { dnsResult ->
                        resultCloudflareDNS = dnsResult
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
            Text(text = "Resultado para o domínio ${domain.text}:")
            Text(text = result)
            Spacer(modifier = Modifier.height(8.dp))

            Text(text = "Resultado usando o DNS 1.1.1.1:")
            Text(text = resultCloudflareDNS)
        }
    }
}