
import android.content.Context
import android.net.ConnectivityManager
import android.os.Build
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
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
    var showResults by remember { mutableStateOf(false) }


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

            // Campo de entrada para o domínio com design aprimorado
            OutlinedTextField(
                value = domain,
                onValueChange = { domain = it },
                label = { Text("Digite o domínio") },
                placeholder = { Text("ex: google.com") },
                textStyle = TextStyle(fontSize = 20.sp),
                modifier = Modifier.fillMaxWidth(),
                trailingIcon = {
                    if (domain.text.isNotBlank()) {
                        IconButton(onClick = { domain = TextFieldValue("") }) {
                            Icon(Icons.Default.Clear, contentDescription = "Limpar")
                        }
                    }
                }
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
                    showResults = true // Exibe os cards após apertar o botão
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

            // Exibição do resultado do DNS ativo
            if (showResults) Card(
                modifier = Modifier.fillMaxWidth(),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFE3F2FD)) // Azul claro
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Resultado usando o DNS ativo:",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = if (result.isNotBlank()) Icons.Default.CheckCircle else Icons.Default.Warning,
                            contentDescription = "Status",
                            tint = if (result.isNotBlank()) Color(0xFF00AA00) else Color.Red
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = result.ifBlank { "Erro ao resolver domínio" },
                            fontSize = 18.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Exibição do resultado do DNS da Cloudflare
            if (showResults) Card(
                modifier = Modifier.fillMaxWidth(),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF3E0)) // Laranja claro
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Resultado usando o DNS da Cloudflare (1.1.1.1):",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = if (resultCloudflareDNS.isNotBlank()) Icons.Default.CheckCircle else Icons.Default.Warning,
                            contentDescription = "Status",
                            tint = if (resultCloudflareDNS.isNotBlank()) Color(0xFF00AA00) else Color.Red
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = resultCloudflareDNS.ifBlank { "Erro ao resolver domínio" },
                            fontSize = 18.sp
                        )
                    }
                }
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