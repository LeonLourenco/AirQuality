package com.example.airquality.ui.screens.map

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.airquality.data.model.Medicao
import com.example.airquality.data.repository.MedicaoRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class MapUiState(
    val medicoes: List<Medicao> = emptyList(),
    val selectedMedicao: Medicao? = null,
    val isLoading: Boolean = false,
    val error: String? = null
)

@HiltViewModel
class MapViewModel @Inject constructor(
    private val repository: MedicaoRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(MapUiState())
    val uiState: StateFlow<MapUiState> = _uiState.asStateFlow()

    init {
        loadMedicoes()
    }

    /**
     * Função pública para ser chamada pela UI para atualizar os dados.
     */
    fun refresh() {
        loadMedicoes()
    }

    private fun loadMedicoes() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            repository.getMedicoes().onSuccess { medicoes ->
                _uiState.update { it.copy(isLoading = false, medicoes = medicoes) }
            }.onFailure { error ->
                _uiState.update { it.copy(isLoading = false, error = error.localizedMessage) }
            }
        }
    }

    fun selectMedicao(medicao: Medicao?) {
        _uiState.update { it.copy(selectedMedicao = medicao) }
    }
}
