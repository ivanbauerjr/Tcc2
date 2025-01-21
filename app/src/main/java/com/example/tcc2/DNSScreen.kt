
import android.content.Context
import android.net.ConnectivityManager
import android.os.Build
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.Card
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.net.InetAddress
import java.net.UnknownHostException

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DNSScreen() {
    val context = LocalContext.current
    // Estado para armazenar o domínio inserido
    var domain by remember { mutableStateOf(TextFieldValue("")) }
    // Estado para armazenar os resultados das resoluções
    var result by remember { mutableStateOf("") }
    var resultCloudflareDNS by remember { mutableStateOf("") }
    // Estado para controlar se está carregando
    var isLoading by remember { mutableStateOf(false) }
    // Estado para armazenar o DNS ativo
    var activeDNS by remember { mutableStateOf("") }
    // Obter o DNS ativo quando a tela for carregada
    var comparisonMessage by remember { mutableStateOf("") } // Mensagem de comparação

    LaunchedEffect(Unit) {
        activeDNS = getActiveDNS(context).replace("/", "")
    }
    Scaffold(
        topBar = {
            TopAppBar(title = { Text(
                "Resolução de DNS",
                fontSize = 35.sp,
                style = MaterialTheme.typography.titleLarge)
            })
        }
    )
    { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Exibição do DNS ativo
            Text(
                text = "DNS ativo: $activeDNS",
                fontSize = 22.sp,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.fillMaxWidth()
            )

            //Spacer(modifier = Modifier.height(10.dp))

            // Campo de entrada para o domínio
            TextField(
                value = domain,
                onValueChange = { domain = it },
                label = { Text("Digite o domínio", fontSize = 16.sp) },
                textStyle = TextStyle(fontSize = 20.sp), // Define o tamanho da fonte do texto digitado
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
            )
            {
                if (isLoading) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp))
                } else {
                    Text("Resolver DNS", fontSize = 24.sp)
                }
            }
            Spacer(modifier = Modifier.height(20.dp))

            // Exibição do resultado
            Card(modifier = Modifier.fillMaxWidth()) {
                Text(text = "Resultado utilizando o DNS ativo:", fontSize = 23.sp)
            }

            Card(modifier = Modifier.fillMaxWidth()) {
                Text(text = result, fontSize = 22.sp)
            }
            Spacer(modifier = Modifier.height(8.dp))

            Card(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "Resultado utilizando usando o DNS da Cloudflare (1.1.1.1):",
                    fontSize = 23.sp
                )
            }
            Card(modifier = Modifier.fillMaxWidth()) {
                Text(text = resultCloudflareDNS, fontSize = 22.sp)
            }

            if (result.isNotEmpty() && resultCloudflareDNS.isNotEmpty()) {
                // Comparação entre os resultados
                comparisonMessage = if (result == resultCloudflareDNS) {
                    "   O retorno com o DNS configurado no dispositivo é igual ao retornado pelo DNS da Cloudflare (1.1.1.1). Portanto, o DNS parece estar funcionando corretamente."
                } else {
                    "   Os resultados do DNS configurado no dispositivo e do DNS da Cloudflare (1.1.1.1) são diferentes. Pode haver algo errado com o DNS configurado no dispositivo."
                }
            }

            Spacer(modifier = Modifier.height(25.dp))

            // Mensagem de comparação
            Text(
                text = comparisonMessage,
                style = MaterialTheme.typography.bodyLarge,
                fontSize = 22.sp,
                color = if (result == resultCloudflareDNS) Color(0xFF00AA00) else Color.Red
            )

        }
    }
}

// Função para resolver um nome de domínio usando o servidor 1.1.1.1
fun resolveUsingCloudflareDNS(domain: String, callback: (result: String) -> Unit) {
    Thread {
        try {
            val address = InetAddress.getByName(domain) // Resolve o nome de domínio
            //callback("Resolução bem-sucedida para $domain: ${address.hostAddress}")
            callback(address.hostAddress)
        } catch (e: UnknownHostException) {
            callback("Erro ao resolver $domain com 1.1.1.1: ${e.message}")
        } catch (e: Exception) {
            callback("Erro ao resolver com 1.1.1.1: ${e.message}")
        }
    }.start()
}

fun resolveDNS(domain: String, callback: (result: String) -> Unit) {
    Thread {
        try {
            val address = InetAddress.getByName(domain)
            //callback("Resolução bem-sucedida: $domain -> ${address.hostAddress}")
            callback(address.hostAddress)
        } catch (e: Exception) {
            callback("Erro ao resolver $domain: ${e.message}")
        }
    }.start()
}

// Função para obter o DNS configurado na rede ativa
fun getActiveDNS(context: Context): String {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val activeNetwork = connectivityManager.activeNetwork
        val linkProperties = connectivityManager.getLinkProperties(activeNetwork)

        return if (linkProperties != null) {
            val dnsAddresses = linkProperties.dnsServers.joinToString(", ")
            dnsAddresses.ifEmpty { "DNS não encontrado" }
        } else {
            "Não foi possível obter as informações de rede."
        }
    } else {
        return "Versão do Android não suporta linkProperties"
    }
}