package com.example.airquality.ui.components

import android.Manifest
import android.net.Uri
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel

@Composable
fun CameraScreen(
    viewModel: CameraViewModel = hiltViewModel(),
    onImageCaptured: (Uri) -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    // Lida com o resultado da captura da imagem
    uiState.capturedImageUri?.let { uri ->
        // LaunchedEffect garante que a navegação ocorra apenas uma vez.
        LaunchedEffect(uri) {
            onImageCaptured(uri)
            viewModel.onImageProcessed() // Limpa o estado para evitar re-navegação
        }
    }

    PermissionHandler(
        permissions = listOf(Manifest.permission.CAMERA),
        onPermissionsGranted = {
            Scaffold { paddingValues ->
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                ) {
                    CameraPreview(
                        modifier = Modifier.fillMaxSize(),
                        imageCapture = viewModel.imageCapture // Usa a instância do ViewModel
                    )

                    Button(
                        onClick = { viewModel.takePhoto(context) },
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(24.dp),
                        enabled = !uiState.isTakingPicture
                    ) {
                        Text("Capturar e Analisar")
                    }

                    if (uiState.isTakingPicture) {
                        CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                    }
                }
            }
        },
        onPermissionsDenied = { requester ->
            // UI para quando a permissão for negada (pode ser mais elaborada)
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Button(onClick = { requester() }) {
                    Text("Conceder Permissão da Câmara")
                }
            }
        }
    )
}