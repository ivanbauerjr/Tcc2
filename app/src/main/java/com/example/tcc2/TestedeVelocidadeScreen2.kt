
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TestedeVelocidadeScreen2() {
    var downloadSpeed by remember { mutableStateOf<String?>(null) }
    var uploadSpeed by remember { mutableStateOf<String?>(null) }
    var isTesting by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Teste de Velocidade de Internet",
            //style = MaterialTheme.typography.h5,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        if (isTesting) {
            CircularProgressIndicator()
        } else {
            Button(onClick = {
                isTesting = true
                performSpeedTest(
                    onDownloadSpeed = { speed ->
                        downloadSpeed = speed
                    },
                    onUploadSpeed = { speed ->
                        uploadSpeed = speed
                        isTesting = false
                    }
                )
            }) {
                Text(text = "Iniciar Teste")
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text(text = "Velocidade de Download: ${downloadSpeed ?: "Aguardando..."}")
        Text(text = "Velocidade de Upload: ${uploadSpeed ?: "Aguardando..."}")
    }
}

fun performSpeedTest(
    onDownloadSpeed: (String) -> Unit,
    onUploadSpeed: (String) -> Unit
) {
    //val testUrl = "https://ipv4.download.thinkbroadband.com/100MB.zip"
    //val testUrl = "https://ipv4.download.thinkbroadband.com/10MB.zip"
    val testUrl = "https://proof.ovh.net/files/100Mb.dat"
    val client = OkHttpClient()

    // Teste de Download
    CoroutineScope(Dispatchers.IO).launch {
        val startTime = System.currentTimeMillis()
        try {
            val request = Request.Builder().url(testUrl).build()
            val response = client.newCall(request).execute()

            val endTime = System.currentTimeMillis()
            val fileSize = response.body?.contentLength() ?: 0L // Tamanho do arquivo em bytes

            // Calcula a velocidade em Mbps
            val timeTakenSeconds = (endTime - startTime) / 1000.0
            val speedMbps = (fileSize * 8) / (timeTakenSeconds * 1_000_000) // Convertendo para Mbps

            withContext(Dispatchers.Main) {
                onDownloadSpeed(String.format("%.2f Mbps", speedMbps))
            }
        } catch (e: Exception) {
            withContext(Dispatchers.Main) {
                onDownloadSpeed("Erro")
            }
        }

        // Teste de Upload (simulado com pequeno payload)
        val uploadUrl = "https://httpbin.org/post"
        val payload = ByteArray(1_000_000) { 0 } // 1 MB de dados para upload
        val requestBody = payload.toRequestBody()

        val uploadStartTime = System.currentTimeMillis()
        try {
            val request = Request.Builder()
                .url(uploadUrl)
                .post(requestBody)
                .build()

            val response = client.newCall(request).execute()
            val uploadEndTime = System.currentTimeMillis()

            // Calcula a velocidade de upload
            val timeTakenSeconds = (uploadEndTime - uploadStartTime) / 1000.0
            val speedMbps = (payload.size * 8) / (timeTakenSeconds * 1_000_000) // Convertendo para Mbps

            withContext(Dispatchers.Main) {
                onUploadSpeed(String.format("%.2f Mbps", speedMbps))
            }
        } catch (e: Exception) {
            withContext(Dispatchers.Main) {
                onUploadSpeed("Erro")
            }
        }
    }
}
