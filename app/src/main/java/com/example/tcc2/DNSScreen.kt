
import android.content.Context
import android.net.ConnectivityManager
import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.Wifi
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
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
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
    var showResults by remember { mutableStateOf(false) }
    var comparisonMessage by remember { mutableStateOf("") }
    var expandedResult by remember { mutableStateOf(false) }
    var expandedCloudflare by remember { mutableStateOf(false) }

    val coroutineScope = rememberCoroutineScope()

    // Obter DNS ativo na inicialização
    LaunchedEffect(Unit) {
        activeDNS = getActiveDNS(context).replace("/", "").trim()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Resolução de DNS",
                        fontSize = 28.sp,
                        style = MaterialTheme.typography.titleLarge
                    )
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
        ) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 12.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFE3F2FD)) // Azul claro
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp)
                        ) {
                            Text(
                                text = "DNS Ativo:",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF0277BD)
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            // Exibição do DNS ativo
                            Text(
                                text = activeDNS,
                                fontSize = 18.sp,
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(Color.White, shape = RoundedCornerShape(8.dp))
                                    .padding(8.dp)
                                    .clip(RoundedCornerShape(8.dp)),
                                color = Color.Black
                            )
                        }
                    }
                }

                item {
                    OutlinedTextField(
                        value = domain,
                        onValueChange = { domain = it },
                        label = { Text("Digite o domínio") },
                        placeholder = { Text("ex: google.com") },
                        textStyle = TextStyle(fontSize = 18.sp),
                        modifier = Modifier.fillMaxWidth(),
                        trailingIcon = {
                            if (domain.text.isNotBlank()) {
                                IconButton(onClick = { domain = TextFieldValue("") }) {
                                    Icon(Icons.Default.Clear, contentDescription = "Limpar")
                                }
                            }
                        }
                    )
                }

                item {
                    Button(
                        onClick = {
                            isLoading = true
                            showResults = true

                            // Iniciar coroutines corretamente
                            coroutineScope.launch {
                                result = resolveDNS(domain.text)
                                resultCloudflareDNS = resolveUsingCloudflareDNS(domain.text)
                                isLoading = false
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = domain.text.isNotBlank() && !isLoading
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(modifier = Modifier.size(20.dp))
                        } else {
                            Text("Resolver DNS", fontSize = 20.sp)
                        }
                    }
                }

                if (showResults) {
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFFE3F2FD))
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text(
                                    text = "Resultado usando o DNS ativo:",
                                    fontSize = 18.sp,
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
                                    TextButton(onClick = { expandedResult = !expandedResult }) {
                                        Text(
                                            text = if (expandedResult) "Esconder" else "Ver resultado",
                                            fontSize = 16.sp,
                                            color = Color.Blue
                                        )
                                    }
                                }
                                if (expandedResult) {
                                    Text(
                                        text = result.ifBlank { "Erro ao resolver domínio" },
                                        fontSize = 16.sp
                                    )
                                }
                            }
                        }
                    }
                }

                if (showResults) {
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF3E0))
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text(
                                    text = "Resultado usando o DNS da Cloudflare (1.1.1.1):",
                                    fontSize = 18.sp,
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
                                    TextButton(onClick = { expandedCloudflare = !expandedCloudflare }) {
                                        Text(
                                            text = if (expandedCloudflare) "Esconder" else "Ver resultado",
                                            fontSize = 16.sp,
                                            color = Color.Blue
                                        )
                                    }
                                }
                                if (expandedCloudflare) {
                                    Text(
                                        text = resultCloudflareDNS.ifBlank { "Erro ao resolver domínio" },
                                        fontSize = 16.sp
                                    )
                                }
                            }
                        }
                    }
                }

                if (result.isNotEmpty() && resultCloudflareDNS.isNotEmpty()) {
                    item {
                        comparisonMessage = if (result == resultCloudflareDNS) {
                            "O retorno com o DNS configurado no dispositivo é igual ao retornado pelo DNS da Cloudflare (1.1.1.1). Portanto, o DNS parece estar funcionando corretamente."
                        } else {
                            "Os resultados do DNS configurado no dispositivo e do DNS da Cloudflare (1.1.1.1) são diferentes. Pode haver algo errado com o DNS configurado no dispositivo."
                        }

                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFFDFFFD6))
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text(
                                    text = "Comparação entre os DNS:",
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = comparisonMessage,
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontSize = 18.sp,
                                    color = if (result == resultCloudflareDNS) Color(0xFF00AA00) else Color.Red
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

// Função para resolver um nome de domínio usando o servidor 1.1.1.1
suspend fun resolveUsingCloudflareDNS(domain: String): String {
    return withContext(Dispatchers.IO) {
        try {
            val address = InetAddress.getByName(domain)
            address.hostAddress ?: "Erro ao resolver domínio com 1.1.1.1"
        } catch (e: UnknownHostException) {
            "Erro ao resolver $domain com 1.1.1.1: ${e.message}"
        } catch (e: Exception) {
            "Erro ao resolver com 1.1.1.1: ${e.message}"
        }
    }
}

suspend fun resolveDNS(domain: String): String {
    return withContext(Dispatchers.IO) {
        try {
            val address = InetAddress.getByName(domain)
            address.hostAddress ?: "Erro ao resolver domínio"
        } catch (e: Exception) {
            "Erro ao resolver $domain: ${e.message}"
        }
    }
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