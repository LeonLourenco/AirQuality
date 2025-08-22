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
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException
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

    // Estados para data/hora
    private val dataMedicaoObj: LocalDate? = null,
    private val horaMedicaoObj: LocalTime? = null,
    val dataString: String = "",
    val horaString: String = "",
    val isDataInvalida: Boolean = false,
    val isHoraInvalida: Boolean = false
) {
    val isFormValid: Boolean
        get() = nomeLocal.isNotBlank() &&
                latitude != null && longitude != null &&
                dataMedicaoObj != null && !isDataInvalida &&
                horaMedicaoObj != null && !isHoraInvalida

    val dataMedicao: LocalDate? get() = dataMedicaoObj
    val horaMedicao: LocalTime? get() = horaMedicaoObj

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
    private val dateFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy")
    private val timeFormatter = DateTimeFormatter.ofPattern("HH:mm")

    init {
        if (metricId != null) {
            loadMedicao(metricId)
        } else {
            val now = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())
            val data = now.date
            val hora = LocalTime(now.hour, now.minute)
            _uiState.update {
                it.copy(
                    isLoading = false,
                    dataMedicaoObj = data,
                    horaMedicaoObj = hora,
                    dataString = java.time.LocalDate.of(data.year, data.month, data.dayOfMonth).format(dateFormatter),
                    horaString = java.time.LocalTime.of(hora.hour, hora.minute).format(timeFormatter)
                )
            }
        }
    }

    private fun loadMedicao(id: String) {
        viewModelScope.launch {
            repository.getMedicaoById(id).onSuccess { medicao ->
                if (medicao != null) {
                    val (lat, lon) = parseLocationString(medicao.localizacao)
                    val data = medicao.data
                    val hora = medicao.hora

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
                            dataMedicaoObj = data,
                            horaMedicaoObj = hora,
                            dataString = if (data != null) java.time.LocalDate.of(data.year, data.month, data.dayOfMonth).format(dateFormatter) else "",
                            horaString = if (hora != null) java.time.LocalTime.of(hora.hour, hora.minute).format(timeFormatter) else ""
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
            _uiState.update { it.copy(fotoByteArray = byteArray, fotoUrl = null) }
        }
    }

    fun onLocationSelected(latitude: Double, longitude: Double) {
        _uiState.update { it.copy(latitude = latitude, longitude = longitude) }
    }

    fun onDataStringChange(texto: String) {
        _uiState.update { it.copy(dataString = texto) }
        try {
            val data = java.time.LocalDate.parse(texto, dateFormatter)
            _uiState.update {
                it.copy(
                    dataMedicaoObj = LocalDate(data.year, data.monthValue, data.dayOfMonth),
                    isDataInvalida = false
                )
            }
        } catch (e: DateTimeParseException) {
            _uiState.update { it.copy(dataMedicaoObj = null, isDataInvalida = true) }
        }
    }

    fun onHoraStringChange(texto: String) {
        _uiState.update { it.copy(horaString = texto) }
        try {
            val hora = java.time.LocalTime.parse(texto, timeFormatter)
            _uiState.update {
                it.copy(
                    horaMedicaoObj = LocalTime(hora.hour, hora.minute),
                    isHoraInvalida = false
                )
            }
        } catch (e: DateTimeParseException) {
            _uiState.update { it.copy(horaMedicaoObj = null, isHoraInvalida = true) }
        }
    }

    fun onDataChange(data: LocalDate) {
        _uiState.update {
            it.copy(
                dataMedicaoObj = data,
                dataString = java.time.LocalDate.of(data.year, data.month, data.dayOfMonth).format(dateFormatter),
                isDataInvalida = false
            )
        }
    }

    fun onHoraChange(hora: LocalTime) {
        _uiState.update {
            it.copy(
                horaMedicaoObj = hora,
                horaString = java.time.LocalTime.of(hora.hour, hora.minute).format(timeFormatter),
                isHoraInvalida = false
            )
        }
    }

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
                data = currentState.dataMedicao,
                hora = currentState.horaMedicao,
                co2Ppm = currentState.co2.toDoubleOrNull(),
                hchoMgM3 = currentState.hcho.toDoubleOrNull(),
                tvocMgM3 = currentState.tvoc.toDoubleOrNull(),
                temperaturaC = currentState.temperatura.toDoubleOrNull(),
                umidadePercent = currentState.umidade.toDoubleOrNull(),
                descricao = currentState.descricao.takeIf { it.isNotBlank() },
                foto = currentState.fotoUrl
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