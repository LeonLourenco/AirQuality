package com.example.airquality.ui.screens.metrics

import android.content.Context
import android.net.Uri
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.airquality.data.model.Medicao
import com.example.airquality.data.repository.MedicaoRepository
import com.example.airquality.util.ImageUtils
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.math.roundToLong

// --- Data Class do Estado da UI ---
data class AddEditMetricUiState(
    val isLoading: Boolean = true,
    val isSaving: Boolean = false,
    val saveSuccess: Boolean = false,
    val saveError: String? = null,

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
    val fotoUrl: String? = null
) {
    // Validação do formulário, agora só exige nome e localização.
    val isFormValid: Boolean
        get() = nomeLocal.isNotBlank() && latitude != null && longitude != null

    val screenTitle: String
        get() = if (id == null) "Nova Medição" else "Editar Medição"
}


// --- ViewModel ---
@HiltViewModel
class AddEditMetricViewModel @Inject constructor(
    private val repository: MedicaoRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val _uiState = MutableStateFlow(AddEditMetricUiState())
    val uiState = _uiState.asStateFlow()

    private val metricId: String? = savedStateHandle["metricId"]

    init {
        if (metricId != null) {
            loadMedicao(metricId)
        } else {
            // Pronto para criar uma nova medição, sem necessidade de carregar
            _uiState.update { it.copy(isLoading = false) }
        }
    }

    private fun loadMedicao(id: String) {
        viewModelScope.launch {
            repository.getMedicaoById(id).onSuccess { medicao ->
                if (medicao != null) {
                    // [MODIFICADO] Extrai lat/lon da string de localização
                    val (lat, lon) = parseLocationString(medicao.localizacao)

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
                            latitude = lat,
                            longitude = lon,
                            fotoUrl = medicao.foto
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

    // --- Handlers de Mudança de Estado ---

    fun onNomeLocalChange(newValue: String) { _uiState.update { it.copy(nomeLocal = newValue) } }
    fun onDescricaoChange(newValue: String) { _uiState.update { it.copy(descricao = newValue) } }
    fun onCo2Change(newValue: String) { _uiState.update { it.copy(co2 = newValue) } }
    fun onTvocChange(newValue: String) { _uiState.update { it.copy(tvoc = newValue) } }
    fun onHchoChange(newValue: String) { _uiState.update { it.copy(hcho = newValue) } }
    fun onTemperaturaChange(newValue: String) { _uiState.update { it.copy(temperatura = newValue) } }
    fun onUmidadeChange(newValue: String) { _uiState.update { it.copy(umidade = newValue) } }

    /**
     * Chamado quando uma nova imagem é capturada pela câmera.
     */
    fun onImageCaptured(context: Context, uri: Uri) {
        viewModelScope.launch {
            val byteArray = ImageUtils().uriToByteArray(context, uri)
            _uiState.update { it.copy(fotoByteArray = byteArray, fotoUrl = null) }
        }
    }

    /**
     * Chamado quando uma localização é selecionada e retornada pela MapSelectionScreen.
     */
    fun onLocationSelected(latitude: Double, longitude: Double) {
        _uiState.update {
            it.copy(latitude = latitude, longitude = longitude)
        }
    }


    /**
     * Salva ou atualiza a medição no repositório.
     */
    fun saveOrUpdateMedicao() {
        if (!_uiState.value.isFormValid) return

        _uiState.update { it.copy(isSaving = true) }

        viewModelScope.launch {
            val currentState = _uiState.value
            val locationString = "POINT(${currentState.longitude} ${currentState.latitude})"

            val medicao = Medicao(
                id = currentState.id,
                nomeLocal = currentState.nomeLocal,
                localizacao = locationString,
                co2Ppm = currentState.co2.toDoubleOrNull(),
                hchoMgM3 = currentState.hcho.toDoubleOrNull(),
                tvocMgM3 = currentState.tvoc.toDoubleOrNull(),
                temperaturaC = currentState.temperatura.toDoubleOrNull(),
                umidadePercent = currentState.umidade.toDoubleOrNull(),
                descricao = currentState.descricao.takeIf { it.isNotBlank() },
                foto = currentState.fotoUrl
            )

            // A lógica de decisão permanece, mas agora a correção será no repositório
            val result = if (medicao.id == null) {
                repository.addMedicao(medicao, currentState.fotoByteArray)
            } else {
                // ✅ ESTA É A CHAMADA CORRETA
                repository.updateMedicao(medicao, currentState.fotoByteArray)
            }

            result.onSuccess {
                _uiState.update { it.copy(isSaving = false, saveSuccess = true) }
            }.onFailure { error ->
                _uiState.update { it.copy(isSaving = false, saveError = error.localizedMessage) }
            }
        }
    }

    /**
     * Limpa os estados de sucesso/erro após a navegação.
     */
    fun onSaveHandled() {
        _uiState.update { it.copy(saveSuccess = false, saveError = null) }
    }

    /**
     * Função auxiliar para converter a string "POINT(lon lat)" em um Par de Doubles.
     */
    private fun parseLocationString(location: String?): Pair<Double?, Double?> {
        if (location == null) return Pair(null, null)
        return try {
            val coords = location.removePrefix("POINT(").removeSuffix(")").split(" ")
            val lon = coords[0].toDouble()
            val lat = coords[1].toDouble()
            Pair(lat, lon)
        } catch (e: Exception) {
            // Retorna nulo se a string estiver mal formatada
            Pair(null, null)
        }
    }
}