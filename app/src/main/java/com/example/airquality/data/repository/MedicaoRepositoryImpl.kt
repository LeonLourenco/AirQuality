package com.example.airquality.data.repository

import com.example.airquality.data.model.Medicao
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.rpc
import io.github.jan.supabase.storage.storage
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import java.util.UUID
import javax.inject.Inject

class MedicaoRepositoryImpl @Inject constructor(
    private val supabaseClient: SupabaseClient
) : MedicaoRepository {

    private val medicoesTable = supabaseClient.postgrest["medicoes"]
    private val fotosBucket = supabaseClient.storage["fotos-locais"]

    override suspend fun getMedicoes(): Result<List<Medicao>> {
        return try {
            val medicoes = supabaseClient.postgrest.rpc("get_all_medicoes").decodeList<Medicao>()
            Result.success(medicoes)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getMedicaoById(id: String): Result<Medicao?> {
        return try {
            val medicao = medicoesTable.select {
                filter {
                    eq("id", id)
                }
            }.decodeSingleOrNull<Medicao>()
            Result.success(medicao)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun addMedicao(medicao: Medicao, fotoByteArray: ByteArray?): Result<Unit> {
        return try {
            var finalMedicao = medicao
            if (fotoByteArray != null) {
                val imagePath = "public/${UUID.randomUUID()}.jpg"
                fotosBucket.upload(path = imagePath, data = fotoByteArray, upsert = false)
                val imageUrl = fotosBucket.publicUrl(path = imagePath)
                finalMedicao = medicao.copy(fotoUrl = imageUrl)
            }

            val medicaoJson = buildJsonObject {
                put("nome_local", finalMedicao.nomeLocal)
                put("localizacao", "POINT(${finalMedicao.longitude} ${finalMedicao.latitude})")
                put("data", finalMedicao.momentoMedicao?.date.toString())
                put("hora", finalMedicao.momentoMedicao?.time.toString())
                finalMedicao.co2Ppm?.let { put("co2_ppm", it) }
                finalMedicao.hchoMgM3?.let { put("hcho_mg_m3", it) }
                finalMedicao.tvocMgM3?.let { put("tvoc_mg_m3", it) }
                finalMedicao.temperaturaC?.let { put("temperatura_c", it) }
                finalMedicao.umidadePercent?.let { put("umidade_percent", it) }
                finalMedicao.descricao?.let { put("descricao", it) }
                finalMedicao.fotoUrl?.let { put("foto", it) }
            }

            medicoesTable.insert(medicaoJson)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun updateMedicao(medicao: Medicao): Result<Unit> {
        return try {
            val medicaoId = requireNotNull(medicao.id) { "ID da medição não pode ser nulo para atualização" }

            val medicaoJson = buildJsonObject {
                put("nome_local", medicao.nomeLocal)
                put("localizacao", "POINT(${medicao.longitude} ${medicao.latitude})")
                put("data", medicao.momentoMedicao?.date.toString())
                put("hora", medicao.momentoMedicao?.time.toString())
                medicao.co2Ppm?.let { put("co2_ppm", it) }
                medicao.hchoMgM3?.let { put("hcho_mg_m3", it) }
                medicao.tvocMgM3?.let { put("tvoc_mg_m3", it) }
                medicao.temperaturaC?.let { put("temperatura_c", it) }
                medicao.umidadePercent?.let { put("umidade_percent", it) }
                medicao.descricao?.let { put("descricao", it) }
                medicao.fotoUrl?.let { put("foto", it) }
            }

            medicoesTable.update(medicaoJson) {
                filter {
                    eq("id", medicaoId)
                }
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun deleteMedicao(id: String): Result<Unit> {
        return try {
            medicoesTable.delete {
                filter {
                    eq("id", id)
                }
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
