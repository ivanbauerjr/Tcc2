
import android.util.Log
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.InetSocketAddress
import java.net.Socket
import java.net.URL

// Função para adicionar o protocolo HTTP automaticamente
fun getUrlWithProtocol(url: String): String {
    return if (url.startsWith("http://") || url.startsWith("https://")) {
        url
    } else {
        "http://$url" // Adiciona "http://" por padrão se o protocolo não estiver presente
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
                    "Conexão bem-sucedida! Código 200."
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
                    "Não autorizado. Código 401."
                }
                HttpURLConnection.HTTP_FORBIDDEN -> {
                    "Proibido. Código 403."
                }
                HttpURLConnection.HTTP_CLIENT_TIMEOUT -> {
                    "Tempo de requisição esgotado. Código 408."
                }
                HttpURLConnection.HTTP_BAD_GATEWAY -> {
                    "Erro de gateway. Código 502."
                }
                HttpURLConnection.HTTP_UNAVAILABLE -> {
                    "Serviço indisponível. Código 503."
                }
                else -> {
                    "Outro código HTTP: $responseCode"
                }
            }
        } catch (e: Exception) {
            Log.e("HttpTest", "Erro ao testar a conexão HTTP", e) // Logando o erro
            "Erro ao testar a conexão HTTP."
        }
    }
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

//Teste de Ping
suspend fun testPing(host: String): String {
    return withContext(Dispatchers.IO) {
        try {
            // Executar o comando de ping
            val process = Runtime.getRuntime().exec("ping -c 1 $host")
            val exitCode = process.waitFor()

            // Verificar o código de saída
            if (exitCode == 0) {
                "Ping bem-sucedido para $host!"
            } else {
                "Não foi possível alcançar o host $host. Código de saída: $exitCode"
            }
        } catch (e: Exception) {
            "Erro ao realizar o ping: ${e.message}"
        }
    }
}


@Composable
fun ConnectivityTestScreen() {
    var url by remember { mutableStateOf(TextFieldValue()) }
    var host by remember { mutableStateOf("") }
    var port by remember { mutableStateOf("") }
    var resultMessage by remember { mutableStateOf("") }
    var connectionStatus by remember { mutableStateOf("") }
    var pingStatus by remember { mutableStateOf("") }
    var isTCPLoading by remember { mutableStateOf(false) }
    var isPINGLoading by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text("Teste de Conectividade")

        Spacer(modifier = Modifier.height(16.dp))

        // Campo de entrada para URL (HTTP)
        OutlinedTextField(
            value = url,
            onValueChange = { url = it },
            label = { Text("Digite a URL (HTTP)") },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Botão para testar HTTP
        val coroutineScope = rememberCoroutineScope()

        Button(
            onClick = {
                // Iniciar a operação de teste HTTP dentro de uma corrotina
                coroutineScope.launch {
                    resultMessage = testHttpConnectivity(url.text)
                }
            }
        ) {
            Text("Testar Conexão HTTP")
        }

        Text(resultMessage)

        // Campo de entrada para o Host
        OutlinedTextField(
            value = host,
            onValueChange = { host = it },
            label = { Text("Digite o Host") },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Campo de entrada para a Porta (TCP)
        OutlinedTextField(
            value = port,
            onValueChange = { port = it },
            label = { Text("Digite a Porta (TCP)") },
            modifier = Modifier.fillMaxWidth(),
            isError = port.isEmpty(), // Mostra um erro visual se estiver vazio
            //keyboardOptions = KeyboardOptions.Default.copy(keyboardType = KeyboardType.Number)
        )

        // Exibindo a mensagem de erro se a porta estiver vazia
        if (port.isEmpty()) {
            Text(
                text = "Determine a porta.",
                //color = MaterialTheme.colors.error,
                //style = MaterialTheme.typography.body2
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Botão para testar a conexão TCP
        Button(
            onClick = {
                // Dentro de uma corrotina
                coroutineScope.launch {
                    isTCPLoading = true
                    if (port.isNotEmpty()) {
                        connectionStatus = testTcpConnectivity(host, port)
                    }
                    isTCPLoading = false
                }
            },
            modifier = Modifier.fillMaxWidth(),
            enabled = port.isNotEmpty() // Desabilita o botão se a porta estiver vazia
        ) {
            Text("Testar Conexão TCP")
        }
        // Exibe o status da conexão
        if (isTCPLoading) {
            CircularProgressIndicator()
        } else {
            Text(
                text = connectionStatus,
                //style = MaterialTheme.typography.h6
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Botão para testar Ping
        Button(
            onClick = {
                coroutineScope.launch {
                    isPINGLoading = true
                    pingStatus = testPing(host).toString()
                    isPINGLoading = false
                }
            }
        ) {
            Text("Testar Ping")
        }
        if (isPINGLoading) {
            CircularProgressIndicator()
        } else {
            Text(
                text = pingStatus
                //style = MaterialTheme.typography.h6
            )
        }
    }
}