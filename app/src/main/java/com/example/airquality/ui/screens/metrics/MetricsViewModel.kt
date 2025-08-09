package com.example.airquality.ui.screens.metrics

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.airquality.data.model.Medicao
import com.example.airquality.data.repository.MedicaoRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class MetricsUiState(
    val isLoading: Boolean = false,
    val medicoes: List<Medicao> = emptyList(),
    val error: String? = null,
    val searchQuery: String = "" // Estado para guardar o texto da pesquisa
)

@HiltViewModel
class MetricsViewModel @Inject constructor(
    private val repository: MedicaoRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(MetricsUiState())
    val uiState = _uiState.asStateFlow()

    // Cache local para guardar a lista completa de medições, evitando buscas repetidas no DB
    private var allMedicoes: List<Medicao> = emptyList()

    init {
        refresh()
    }

    /**
     * Busca os dados mais recentes do repositório.
     */
    fun refresh() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            repository.getMedicoes().onSuccess { medicoes ->
                // Atualiza o cache local com os novos dados
                allMedicoes = medicoes
                // Aplica o filtro atual (pode ser uma string vazia) à nova lista
                filterMedicoes()
            }.onFailure { error ->
                _uiState.update { it.copy(isLoading = false, error = error.localizedMessage) }
            }
        }
    }

    /**
     * Atualiza o estado da query de pesquisa e aciona a filtragem.
     * @param query O novo texto digitado pelo usuário na barra de pesquisa.
     */
    fun onSearchQueryChange(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
        filterMedicoes()
    }

    /**
     * Filtra a lista de medições com base na query de pesquisa atual.
     * A UI é atualizada com a lista filtrada.
     */
    private fun filterMedicoes() {
        val query = _uiState.value.searchQuery
        val filteredList = if (query.isBlank()) {
            // Se a pesquisa estiver vazia, mostra a lista completa
            allMedicoes
        } else {
            // Se houver texto, filtra pelo nome do local (ignorando maiúsculas/minúsculas)
            allMedicoes.filter {
                it.nomeLocal.contains(query, ignoreCase = true)
            }
        }
        _uiState.update { it.copy(isLoading = false, medicoes = filteredList) }
    }


    /**
     * Deleta uma medição e atualiza a lista.
     * @param id O ID da medição a ser deletada.
     */
    fun deleteMedicao(id: String) {
        viewModelScope.launch {
            repository.deleteMedicao(id).onSuccess {
                // Após deletar com sucesso, busca os dados novamente para refletir a mudança
                refresh()
            }.onFailure { error ->
                _uiState.update { it.copy(error = error.localizedMessage) }
            }
        }
    }
}