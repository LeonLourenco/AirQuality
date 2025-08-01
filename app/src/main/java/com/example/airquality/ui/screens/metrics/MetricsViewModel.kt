package com.example.airquality.ui.screens.metrics

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

data class MetricsUiState(
    val medicoes: List<Medicao> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null
)

@HiltViewModel
class MetricsViewModel @Inject constructor(
    private val repository: MedicaoRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(MetricsUiState())
    val uiState: StateFlow<MetricsUiState> = _uiState.asStateFlow()

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
                // Ordena as medições pela data de criação para exibir as mais recentes primeiro
                val sortedMedicoes = medicoes.sortedByDescending { it.createdAt }
                _uiState.update {
                    it.copy(isLoading = false, medicoes = sortedMedicoes, error = null)
                }
            }.onFailure { error ->
                _uiState.update {
                    it.copy(isLoading = false, error = error.localizedMessage)
                }
            }
        }
    }

    fun deleteMedicao(id: String) {
        viewModelScope.launch {
            // Mostra o loading enquanto deleta
            _uiState.update { it.copy(isLoading = true) }
            repository.deleteMedicao(id).onSuccess {
                // Recarrega a lista após a exclusão para garantir que a UI esteja atualizada
                loadMedicoes()
            }.onFailure { error ->
                _uiState.update {
                    it.copy(isLoading = false, error = "Falha ao excluir: ${error.localizedMessage}")
                }
            }
        }
    }
}
