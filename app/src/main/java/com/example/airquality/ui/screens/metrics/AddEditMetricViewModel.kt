package com.example.airquality.ui.screens.metrics

import android.Manifest
import android.app.Application
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import androidx.core.content.ContextCompat
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.airquality.data.model.Medicao
import com.example.airquality.data.repository.MedicaoRepository
import com.example.airquality.util.ImageUtils
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import javax.inject.Inject
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine

data class AddEditMetricUiState(
    val isLoading: Boolean = true, // Começa a carregar
    val isSaving: Boolean = false,
    val saveSuccess: Boolean = false,
    val saveError: String? = null,
    val isFetchingLocation: Boolean = false,
    val locationError: String? = null,

    // Campos do formulário
    val id: String? = null,
    val nomeLocal: String = "",
    val descricao: String = "",
    val co2: String = "",
    val tvoc: String = "",
    val hcho: String = "",
    val temperatura: String = "",
    val umidade: String = "",
    val latitude: Double? = null,
    val longitude: Double? = null,
    val fotoByteArray: ByteArray? = null,
    val fotoUrl: String? = null // Para guardar a URL existente
) {
    val isFormValid: Boolean
        get() = nomeLocal.isNotBlank() && latitude != null && longitude != null

    val screenTitle: String
        get() = if (id == null) "Nova Medição" else "Editar Medição"
}

@HiltViewModel
class AddEditMetricViewModel @Inject constructor(
    private val repository: MedicaoRepository,
    private val savedStateHandle: SavedStateHandle, // Para obter o ID da navegação
    private val fusedLocationProvider: FusedLocationProviderClient,
    private val application: Application
) : ViewModel() {

    private val _uiState = MutableStateFlow(AddEditMetricUiState())
    val uiState = _uiState.asStateFlow()

    private val metricId: String? = savedStateHandle["metricId"]

    init {
        if (metricId != null) {
            loadMedicao(metricId)
        } else {
            _uiState.update { it.copy(isLoading = false) } // Pronto para criar um novo
        }
    }

    private fun loadMedicao(id: String) {
        viewModelScope.launch {
            repository.getMedicaoById(id).onSuccess { medicao ->
                if (medicao != null) {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            id = medicao.id,
                            nomeLocal = medicao.nomeLocal,
                            descricao = medicao.descricao ?: "",
                            co2 = medicao.co2Ppm?.toString() ?: "",
                            tvoc = medicao.tvocMgM3?.toString() ?: "",
                            hcho = medicao.hchoMgM3?.toString() ?: "",
                            temperatura = medicao.temperaturaC?.toString() ?: "",
                            umidade = medicao.umidadePercent?.toString() ?: "",
                            latitude = medicao.latitude,
                            longitude = medicao.longitude,
                            fotoUrl = medicao.fotoUrl
                        )
                    }
                } else {
                    _uiState.update { it.copy(isLoading = false, saveError = "Medição não encontrada.") }
                }
            }.onFailure { error ->
                _uiState.update { it.copy(isLoading = false, saveError = error.localizedMessage) }
            }
        }
    }

    fun onNomeLocalChange(newValue: String) { _uiState.update { it.copy(nomeLocal = newValue) } }
    fun onDescricaoChange(newValue: String) { _uiState.update { it.copy(descricao = newValue) } }
    fun onCo2Change(newValue: String) { _uiState.update { it.copy(co2 = newValue) } }
    fun onTvocChange(newValue: String) { _uiState.update { it.copy(tvoc = newValue) } }
    fun onHchoChange(newValue: String) { _uiState.update { it.copy(hcho = newValue) } }
    fun onTemperaturaChange(newValue: String) { _uiState.update { it.copy(temperatura = newValue) } }
    fun onUmidadeChange(newValue: String) { _uiState.update { it.copy(umidade = newValue) } }

    fun onImageCaptured(context: Context, uri: Uri) {
        viewModelScope.launch {
            val byteArray = ImageUtils().uriToByteArray(context, uri)
            _uiState.update { it.copy(fotoByteArray = byteArray, fotoUrl = null) } // Limpa a URL antiga se uma nova foto for tirada
        }
    }

    fun onLocationCaptured() {
        viewModelScope.launch {
            _uiState.update { it.copy(isFetchingLocation = true, locationError = null) }

            if (!hasLocationPermission()) {
                _uiState.update { it.copy(isFetchingLocation = false, locationError = "Permissão de localização negada.") }
                return@launch
            }

            try {
                val location = getCurrentLocation()
                _uiState.update {
                    it.copy(
                        isFetchingLocation = false,
                        latitude = location.latitude,
                        longitude = location.longitude
                    )
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isFetchingLocation = false, locationError = "Não foi possível obter a localização: ${e.message}") }
            }
        }
    }

    private suspend fun getCurrentLocation(): android.location.Location = suspendCoroutine { continuation ->
        val cancellationTokenSource = CancellationTokenSource()
        fusedLocationProvider.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, cancellationTokenSource.token)
            .addOnSuccessListener { location ->
                if (location != null) {
                    continuation.resume(location)
                } else {
                    continuation.resumeWithException(RuntimeException("Localização não encontrada."))
                }
            }
            .addOnFailureListener { e ->
                continuation.resumeWithException(e)
            }
    }

    private fun hasLocationPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            application.applicationContext,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
    }


    fun saveOrUpdateMedicao() {
        if (!_uiState.value.isFormValid) return

        _uiState.update { it.copy(isSaving = true) }

        viewModelScope.launch {
            val currentState = _uiState.value
            val medicao = Medicao(
                id = currentState.id,
                nomeLocal = currentState.nomeLocal,
                latitude = currentState.latitude!!,
                longitude = currentState.longitude!!,
                momentoMedicao = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()),
                co2Ppm = currentState.co2.toDoubleOrNull(),
                hchoMgM3 = currentState.hcho.toDoubleOrNull(),
                tvocMgM3 = currentState.tvoc.toDoubleOrNull(),
                temperaturaC = currentState.temperatura.toDoubleOrNull(),
                umidadePercent = currentState.umidade.toDoubleOrNull(),
                descricao = currentState.descricao.takeIf { it.isNotBlank() },
                fotoUrl = currentState.fotoUrl, // Mantém a URL existente se não houver nova foto
                createdAt = null
            )

            val result = if (medicao.id == null) {
                repository.addMedicao(medicao, currentState.fotoByteArray)
            } else {
                // Para edição, ainda não estamos a lidar com a atualização da foto.
                // Uma implementação mais completa poderia apagar a foto antiga e fazer upload da nova.
                repository.updateMedicao(medicao)
            }

            result.onSuccess {
                _uiState.update { it.copy(isSaving = false, saveSuccess = true) }
            }.onFailure { error ->
                _uiState.update { it.copy(isSaving = false, saveError = error.localizedMessage) }
            }
        }
    }

    fun onSaveHandled() {
        _uiState.update { it.copy(saveSuccess = false, saveError = null) }
    }
}
