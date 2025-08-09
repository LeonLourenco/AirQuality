package com.example.airquality.data.model

import kotlinx.datetime.Instant
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Medicao(
    @SerialName("id")
    val id: String? = null,

    @SerialName("nome_local")
    val nomeLocal: String,

    // [MODIFICADO] Substitui lat/lon por um único campo de localização
    @SerialName("localizacao")
    val localizacao: String,

    @SerialName("co2_ppm")
    val co2Ppm: Double? = null,

    @SerialName("hcho_mg_m3")
    val hchoMgM3: Double? = null,

    @SerialName("tvoc_mg_m3")
    val tvocMgM3: Double? = null,

    @SerialName("temperatura_c")
    val temperaturaC: Double? = null,

    @SerialName("umidade_percent")
    val umidadePercent: Double? = null,

    @SerialName("descricao")
    val descricao: String? = null,

    @SerialName("created_at")
    val createdAt: Instant? = null,

    @SerialName("foto")
    val foto: String? = null
)