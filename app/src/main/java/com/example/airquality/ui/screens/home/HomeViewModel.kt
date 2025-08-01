package com.example.airquality.ui.screens.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.airquality.data.model.Medicao
import com.example.airquality.data.repository.MedicaoRepository
import com.patrykandpatrick.vico.core.entry.ChartEntryModelProducer
import com.patrykandpatrick.vico.core.entry.entryOf
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class HomeUiState(
    val totalMedicoes: Int = 0,
    val mediaCo2: Double = 0.0,
    val maxTvoc: Double = 0.0,
    val insightText: String = "Analisando dados...",
    val isLoading: Boolean = false,
    // Produtor de modelo para o gráfico de tendência de CO2
    val co2TrendProducer: ChartEntryModelProducer = ChartEntryModelProducer()
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val repository: MedicaoRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init {
        loadAndProcessData()
    }

    /**
     * Função pública para ser chamada pela UI para atualizar os dados.
     */
    fun refresh() {
        loadAndProcessData()
    }

    private fun loadAndProcessData() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            repository.getMedicoes().onSuccess { medicoes ->
                processData(medicoes)
            }.onFailure {
                _uiState.update { it.copy(isLoading = false, insightText = "Falha ao carregar dados.") }
            }
        }
    }

    private fun processData(medicoes: List<Medicao>) {
        if (medicoes.isEmpty()) {
            _uiState.update { HomeUiState(isLoading = false, insightText = "Nenhuma medição registada.") }
            return
        }

        val total = medicoes.size
        val mediaCo2 = medicoes.mapNotNull { it.co2Ppm }.average()
        val maxTvoc = medicoes.mapNotNull { it.tvocMgM3 }.maxOrNull() ?: 0.0

        // Prepara os dados para o gráfico Vico
        val tendenciaCo2Entries = medicoes
            .sortedBy { it.createdAt }
            .mapNotNull { it.co2Ppm }
            .mapIndexed { index, value -> entryOf(index.toFloat(), value.toFloat()) }

        val insight = generateInsight(mediaCo2)

        _uiState.update {
            it.copy(
                isLoading = false,
                totalMedicoes = total,
                mediaCo2 = mediaCo2,
                maxTvoc = maxTvoc,
                insightText = insight
            )
        }
        // Atualiza o produtor do gráfico com as novas entradas
        _uiState.value.co2TrendProducer.setEntries(tendenciaCo2Entries)
    }

    private fun generateInsight(averageCo2: Double): String {
        return when {
            averageCo2 > 1500 -> "Atenção: A média de CO2 está muito elevada, indicando ventilação inadequada. Recomenda-se abrir janelas."
            averageCo2 > 1000 -> "Alerta: A média de CO2 está acima do ideal para ambientes internos. Considere melhorar a circulação de ar."
            averageCo2 > 600 -> "Níveis de CO2 estão aceitáveis, mas podem ser melhorados com ventilação periódica."
            else -> "A qualidade do ar em relação ao CO2 está em níveis ótimos."
        }
    }
}
