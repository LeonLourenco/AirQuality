package com.example.airquality.data.model

import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDateTime
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Medicao(
    @SerialName("id")
    val id: String? = null,

    @SerialName("nome_local")
    val nomeLocal: String,

    @SerialName("latitude")
    val latitude: Double? = null,

    @SerialName("longitude")
    val longitude: Double? = null,

    @SerialName("momento_medicao")
    val momentoMedicao: LocalDateTime? = null,

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

    @SerialName("foto")
    val fotoUrl: String? = null,

    @SerialName("created_at")
    val createdAt: Instant? = null
)
