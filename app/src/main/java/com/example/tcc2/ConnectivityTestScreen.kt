
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import okio.IOException
import java.net.HttpURLConnection
import java.net.InetAddress
import java.net.MalformedURLException
import java.net.Socket
import java.net.URL


@Composable
fun ConnectivityTestScreen() {
    var url by remember { mutableStateOf(TextFieldValue()) }
    var host by remember { mutableStateOf(TextFieldValue()) }
    var port by remember { mutableStateOf(TextFieldValue()) }
    var resultMessage by remember { mutableStateOf("") }

    // Função para adicionar o protocolo HTTP automaticamente
    fun getUrlWithProtocol(url: String): String {
        return if (url.startsWith("http://") || url.startsWith("https://")) {
            url
        } else {
            "http://$url" // Adiciona "http://" por padrão se o protocolo não estiver presente
        }
    }

    // Função para testar a conectividade HTTP
    fun testHttpConnectivity(url: String): Boolean {
        return try {
            // Formatar a URL para garantir que o protocolo está presente
            val formattedUrl = getUrlWithProtocol(url)
            val connection = URL(formattedUrl).openConnection() as HttpURLConnection
            connection.requestMethod = "GET"
            connection.connectTimeout = 5000
            connection.readTimeout = 5000
            connection.connect()

            // Código de resposta da requisição HTTP
            val responseCode = connection.responseCode
            val responseMessage = connection.responseMessage

            // Exibindo o código e a mensagem de resposta para depuração
            println("HTTP Response Code: $responseCode")
            println("HTTP Response Message: $responseMessage")

            // Verifica se a resposta é OK (200)
            responseCode == HttpURLConnection.HTTP_OK
        } catch (e: MalformedURLException) {
            // Se a URL estiver malformada
            println("MalformedURLException: ${e.message}")
            false
        } catch (e: IOException) {
            // Se ocorrer um erro de IO (como tempo de conexão expirado)
            println("IOException: ${e.message}")
            false
        } catch (e: Exception) {
            // Captura qualquer outro tipo de exceção
            println("Exception: ${e.localizedMessage}")
            false
        }
    }


    // Teste de conectividade TCP
    fun testTcpConnectivity(host: String, port: String): Boolean {
        return try {
            if (port.isEmpty()) {
                resultMessage = "Porta não pode ser vazia."
                return false
            }
            val portInt = port.toIntOrNull()
            if (portInt == null) {
                resultMessage = "Porta inválida."
                return false
            }
            val socket = Socket()
            socket.connect(java.net.InetSocketAddress(host, portInt), 5000)
            socket.close()
            true
        } catch (e: Exception) {
            false
        }
    }

    // Teste de ping
    fun testPing(host: String): Boolean {
        return try {
            val inetAddress = InetAddress.getByName(host)
            inetAddress.isReachable(5000)
        } catch (e: Exception) {
            false
        }
    }

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
        Button(
            onClick = {
                resultMessage = if (testHttpConnectivity(url.text)) {
                    "Conexão HTTP bem-sucedida!"
                } else {
                    "Falha na conexão HTTP."
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Testar Conexão HTTP")
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Campo de entrada para Host (TCP)
        OutlinedTextField(
            value = host,
            onValueChange = { host = it },
            label = { Text("Digite o Host (TCP)") },
            modifier = Modifier.fillMaxWidth()
        )

        // Campo de entrada para Porta (TCP)
        OutlinedTextField(
            value = port,
            onValueChange = { port = it },
            label = { Text("Digite a Porta (TCP)") },
            modifier = Modifier.fillMaxWidth(),
            //keyboardOptions = androidx.compose.ui.text.input.KeyboardOptions.Default.copy(keyboardType = KeyboardType.Number)
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Botão para testar TCP
        Button(
            onClick = {
                resultMessage = if (testTcpConnectivity(host.text, port.text.toInt().toString())) {
                    "Conexão TCP bem-sucedida!"
                } else {
                    "Falha na conexão TCP."
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Testar Conexão TCP")
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Botão para testar Ping
        Button(
            onClick = {
                resultMessage = if (testPing(host.text)) {
                    "Ping bem-sucedido!"
                } else {
                    "Falha no ping."
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Testar Ping")
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Resultado do teste
        Text(
            text = resultMessage,
            color = if (resultMessage.contains("sucesso")) Color.Green else Color.Red,
            //style = MaterialTheme.typography.body1
        )
    }
}