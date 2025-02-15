
import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.InetSocketAddress
import java.net.Socket
import java.net.URL

// Função para adicionar o protocolo HTTP automaticamente
fun getUrlWithProtocol(url: String): String {
    return when {
        url.startsWith("http://") || url.startsWith("https://") -> url
        url.startsWith("www.") -> "http://$url"
        else -> "http://$url" // Adiciona "http://" por padrão se o protocolo não estiver presente
    }
}

suspend fun testHttpConnectivity(url: String): String {
    return withContext(Dispatchers.IO) {
        try {
            // Formatar a URL para garantir que o protocolo está presente
            val formattedUrl = getUrlWithProtocol(url)
            val connection = URL(formattedUrl).openConnection() as HttpURLConnection
            connection.requestMethod = "GET"
            connection.connectTimeout = 10000
            connection.readTimeout = 10000
            connection.connect()

            // Código de resposta da requisição HTTP
            val responseCode = connection.responseCode
            val responseMessage = connection.responseMessage

            // Exibindo o código e a mensagem de resposta para depuração
            println("HTTP Response Code: $responseCode")
            println("HTTP Response Message: $responseMessage")

            // Retorna a mensagem de acordo com o código HTTP
            when (responseCode) {
                HttpURLConnection.HTTP_OK -> {
                    "Conexão bem-sucedida! Código: $responseCode"
                }
                HttpURLConnection.HTTP_MOVED_TEMP, HttpURLConnection.HTTP_MOVED_PERM -> {
                    "Redirecionamento detectado. Código: $responseCode"
                }
                HttpURLConnection.HTTP_NOT_FOUND -> {
                    "Recurso não encontrado. Código: $responseCode"
                }
                HttpURLConnection.HTTP_INTERNAL_ERROR -> {
                    "Erro interno no servidor. Código: $responseCode"
                }
                HttpURLConnection.HTTP_BAD_REQUEST -> {
                    "Requisição malformada. Código: $responseCode"
                }
                HttpURLConnection.HTTP_UNAUTHORIZED -> {
                    "Não autorizado. Código: $responseCode"
                }
                HttpURLConnection.HTTP_FORBIDDEN -> {
                    "Proibido. Código: $responseCode"
                }
                HttpURLConnection.HTTP_CLIENT_TIMEOUT -> {
                    "Tempo de requisição esgotado. Código: $responseCode"
                }
                HttpURLConnection.HTTP_BAD_GATEWAY -> {
                    "Erro de gateway. Código: $responseCode"
                }
                HttpURLConnection.HTTP_UNAVAILABLE -> {
                    "Serviço indisponível. Código: $responseCode"
                }
                else -> {
                    "Código: $responseCode"
                }
            }
        } catch (e: Exception) {
            Log.e("HttpTest", "Erro ao testar a conexão HTTP", e) // Logando o erro
            "Erro ao testar a conexão HTTP."
        }
    }
}

fun savePingResult(context: Context, host: String, pingTime: Long) {
    val sharedPreferences: SharedPreferences = context.getSharedPreferences("PingResults", Context.MODE_PRIVATE)
    sharedPreferences.edit().putLong(host, pingTime).apply()
}

fun getPreviousPingResult(context: Context, host: String): Long? {
    val sharedPreferences: SharedPreferences = context.getSharedPreferences("PingResults", Context.MODE_PRIVATE)
    return if (sharedPreferences.contains(host)) sharedPreferences.getLong(host, -1) else null
}

suspend fun testTcpConnectivity(host: String, port: String?): String {
    // Verificar se a porta foi fornecida
    if (port.isNullOrEmpty()) {
        // Exibir mensagem solicitando para fornecer a porta
        return "Por favor, determine a porta."
    }

    return withContext(Dispatchers.IO) {
        try {
            // Tentar estabelecer uma conexão TCP com o host e a porta
            val socket = Socket()
            val address = InetSocketAddress(host, port.toInt())
            socket.connect(address, 5000) // Tempo de conexão de 5 segundos
            socket.close() // Fechar a conexão
            "Conexão TCP bem-sucedida!"
        } catch (e: Exception) {
            // Se houver uma exceção (ex: falha de conexão), mostrar o erro
            "Erro ao conectar: ${e.message}"
        }
    }
}

suspend fun testPing(host: String, context: Context): String {
    return withContext(Dispatchers.IO) {
        try {
            val startTime = System.currentTimeMillis()
            val process = Runtime.getRuntime().exec("ping -c 1 $host")
            val exitCode = process.waitFor()
            val reader = BufferedReader(InputStreamReader(process.inputStream))
            val output = reader.readText().trim().lowercase() // Normaliza a saída
            val endTime = System.currentTimeMillis()
            val pingTime = endTime - startTime

            return@withContext when {
                exitCode == 0 -> {
                    val previousPing = getPreviousPingResult(context, host)
                    savePingResult(context, host, pingTime)
                    if (previousPing != null) {
                        "Ping bem-sucedido para $host!\n Tempo: ${pingTime}ms.\n Variação em relação ao último teste: ${pingTime - previousPing}ms"
                    } else {
                        "Ping bem-sucedido para $host!\n Tempo: ${pingTime}ms"
                    }
                }
                exitCode == 1 -> when {
                    output.contains("0 received") || output.contains("100% packet loss") ->
                        "O host $host não respondeu ao ping."

                    else -> "Falha no ping para $host. Código de saída: $exitCode"
                }
                exitCode == 2 -> "Erro: O endereço do host parece estar incorreto."
                else -> "Falha desconhecida no ping para $host. Código de saída: $exitCode"
            }
        } catch (e: Exception) {
            "Erro ao realizar o ping: ${e.message}"
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConnectivityTestScreen(context: Context) {
    var url by remember { mutableStateOf("") }
    var tcpHost by remember { mutableStateOf("") }
    var pingHost by remember { mutableStateOf("") }
    var port by remember { mutableStateOf("") }
    var httpResult by remember { mutableStateOf("") }
    var tcpResult by remember { mutableStateOf("") }
    var pingResult by remember { mutableStateOf("") }
    var isHttpLoading by remember { mutableStateOf(false) }
    var isTcpLoading by remember { mutableStateOf(false) }
    var isPingLoading by remember { mutableStateOf(false) }

    val coroutineScope = rememberCoroutineScope()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Teste de Conectividade") }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                ConnectivityCard(title = "Teste HTTP") {
                    OutlinedTextField(
                        value = url,
                        onValueChange = { url = it },
                        label = { Text("Digite a URL") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(
                        onClick = {
                            coroutineScope.launch {
                                isHttpLoading = true
                                httpResult = testHttpConnectivity(url)
                                isHttpLoading = false
                            }
                        },
                        enabled = url.isNotEmpty(),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        if (isHttpLoading) {
                            CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.White)
                            Spacer(modifier = Modifier.width(8.dp))
                        }
                        Text("Testar HTTP")
                    }
                    Text(httpResult, Modifier.padding(top = 8.dp))
                }
            }

            item {
                ConnectivityCard(title = "Teste TCP") {
                    OutlinedTextField(
                        value = tcpHost,
                        onValueChange = { tcpHost = it },
                        label = { Text("Digite o Host") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = port,
                        onValueChange = { port = it },
                        label = { Text("Digite a Porta") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions.Default.copy(keyboardType = KeyboardType.Number)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(
                        onClick = {
                            coroutineScope.launch {
                                isTcpLoading = true
                                tcpResult = testTcpConnectivity(tcpHost, port)
                                isTcpLoading = false
                            }
                        },
                        enabled = tcpHost.isNotEmpty() && port.isNotEmpty(),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        if (isTcpLoading) {
                            CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.White)
                            Spacer(modifier = Modifier.width(8.dp))
                        }
                        Text("Testar TCP")
                    }
                    Text(tcpResult, Modifier.padding(top = 8.dp))
                }
            }

            item {
                ConnectivityCard(title = "Teste de Ping") {
                    OutlinedTextField(
                        value = pingHost,
                        onValueChange = { pingHost = it },
                        label = { Text("Digite o Host") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(
                        onClick = {
                            coroutineScope.launch {
                                isPingLoading = true
                                pingResult = testPing(pingHost, context)
                                isPingLoading = false
                            }
                        },
                        enabled = pingHost.isNotEmpty(),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        if (isPingLoading) {
                            CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.White)
                            Spacer(modifier = Modifier.width(8.dp))
                        }
                        Text("Testar Ping")
                    }
                    Text(pingResult, Modifier.padding(top = 8.dp))
                }
            }
        }
    }
}

@Composable
fun ConnectivityCard(title: String, content: @Composable ColumnScope.() -> Unit) {
    Card(
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 4.dp),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            content()
        }
    }
}