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
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.LocalTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant
import kotlinx.datetime.toLocalDateTime
import javax.inject.Inject

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
    val fotoUrl: String? = null,

    // Campos para guardar a data e hora selecionadas
    val dataMedicao: LocalDate? = null,
    val horaMedicao: LocalTime? = null
) {
    // Validação do formulário agora checa todos os campos obrigatórios
    val isFormValid: Boolean
        get() = nomeLocal.isNotBlank() &&
                latitude != null &&
                longitude != null &&
                dataMedicao != null &&
                horaMedicao != null

    val screenTitle: String
        get() = if (id == null) "Nova Medição" else "Editar Medição"
}

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
            // Ao criar uma nova medição, já preenche com data e hora atuais
            val now = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())
            _uiState.update {
                it.copy(
                    isLoading = false,
                    dataMedicao = now.date,
                    horaMedicao = LocalTime(now.hour, now.minute) // Ignora segundos para simplicidade
                )
            }
        }
    }

    private fun loadMedicao(id: String) {
        viewModelScope.launch {
            repository.getMedicaoById(id).onSuccess { medicao ->
                if (medicao != null) {
                    val (lat, lon) = parseLocationString(medicao.localizacao)

                    // Extrai data e hora do `createdAt` que vem do banco
                    val dateTime = medicao.createdAt?.toLocalDateTime(TimeZone.currentSystemDefault())

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
                            fotoUrl = medicao.foto,
                            dataMedicao = dateTime?.date,
                            horaMedicao = dateTime?.let { dt -> LocalTime(dt.hour, dt.minute) }
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

    // Handlers de Mudança de Estado
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
            _uiState.update { it.copy(fotoByteArray = byteArray, fotoUrl = null) }
        }
    }

    fun onLocationSelected(latitude: Double, longitude: Double) {
        _uiState.update { it.copy(latitude = latitude, longitude = longitude) }
    }

    // Handlers para atualizar data e hora a partir dos Pickers
    fun onDataChange(data: LocalDate) {
        _uiState.update { it.copy(dataMedicao = data) }
    }

    fun onHoraChange(hora: LocalTime) {
        _uiState.update { it.copy(horaMedicao = hora) }
    }

    fun saveOrUpdateMedicao() {
        if (!_uiState.value.isFormValid) return

        _uiState.update { it.copy(isSaving = true) }

        viewModelScope.launch {
            val currentState = _uiState.value
            val locationString = "POINT(${currentState.longitude} ${currentState.latitude})"

            // Combina a data e hora selecionadas pelo usuário
            val momentoMedicao = LocalDateTime(currentState.dataMedicao!!, currentState.horaMedicao!!)

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
                foto = currentState.fotoUrl,
                // Converte o LocalDateTime local para um Instant (UTC) para salvar no banco
                createdAt = momentoMedicao.toInstant(TimeZone.currentSystemDefault())
            )

            val result = if (medicao.id == null) {
                repository.addMedicao(medicao, currentState.fotoByteArray)
            } else {
                repository.updateMedicao(medicao, currentState.fotoByteArray)
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

    private fun parseLocationString(location: String?): Pair<Double?, Double?> {
        if (location == null) return Pair(null, null)
        return try {
            val coords = location.removePrefix("POINT(").removeSuffix(")").split(" ")
            val lon = coords[0].toDouble()
            val lat = coords[1].toDouble()
            Pair(lat, lon)
        } catch (e: Exception) {
            Pair(null, null)
        }
    }
}