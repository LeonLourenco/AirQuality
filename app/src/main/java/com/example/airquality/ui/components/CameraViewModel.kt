package com.example.airquality.ui.components

import android.content.Context
import android.net.Uri
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.io.File
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.concurrent.Executor
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import javax.inject.Inject

// Estado da UI para a tela da câmera
data class CameraUiState(
    val isTakingPicture: Boolean = false,
    val capturedImageUri: Uri? = null,
    val error: String? = null
)

@HiltViewModel
class CameraViewModel @Inject constructor() : ViewModel() {

    private val _uiState = MutableStateFlow(CameraUiState())
    val uiState = _uiState.asStateFlow()

    // A instância do ImageCapture é criada aqui e partilhada com a UI.
    val imageCapture: ImageCapture = ImageCapture.Builder().build()

    // Usamos um executor em segundo plano para não bloquear a UI.
    private val cameraExecutor: Executor = Executors.newSingleThreadExecutor()

    fun takePhoto(context: Context) {
        _uiState.update { it.copy(isTakingPicture = true, error = null) }

        // Define o ficheiro de saída
        val photoFile = File(
            context.filesDir,
            "IMG_${SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(System.currentTimeMillis())}.jpg"
        )
        val outputOptions = ImageCapture.OutputFileOptions.Builder(photoFile).build()

        imageCapture.takePicture(
            outputOptions,
            cameraExecutor,
            object : ImageCapture.OnImageSavedCallback {
                override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                    _uiState.update {
                        it.copy(isTakingPicture = false, capturedImageUri = output.savedUri)
                    }
                }

                override fun onError(exception: ImageCaptureException) {
                    _uiState.update {
                        it.copy(isTakingPicture = false, error = "Falha ao capturar imagem: ${exception.message}")
                    }
                }
            }
        )
    }

    fun onImageProcessed() {
        _uiState.update { it.copy(capturedImageUri = null) }
    }

    override fun onCleared() {
        super.onCleared()
        cameraExecutor.let { (it as ExecutorService).shutdown() }
    }
}