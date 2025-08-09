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

    // Referências para a tabela e o bucket do Supabase
    private val medicoesTable = supabaseClient.postgrest["medicoes"]
    private val fotosBucket = supabaseClient.storage["fotos_medicoes"]

    override suspend fun getMedicoes(): Result<List<Medicao>> {
        return try {
            // Chama a RPC que já converte a localização para texto
            val medicoes = supabaseClient.postgrest.rpc("get_all_medicoes").decodeList<Medicao>()
            Result.success(medicoes)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getMedicaoById(id: String): Result<Medicao?> {
        return try {
            // Chama a nova RPC para buscar por ID, que também converte a localização
            val medicao = supabaseClient.postgrest.rpc("get_medicao_by_id", buildJsonObject {
                put("p_id", id)
            }).decodeSingleOrNull<Medicao>()
            Result.success(medicao)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun addMedicao(medicao: Medicao, fotoByteArray: ByteArray?): Result<Unit> {
        return try {
            var finalMedicao = medicao

            // Se uma foto foi fornecida, faz o upload e atualiza o objeto.
            if (fotoByteArray != null) {
                val imagePath = "public/${UUID.randomUUID()}.jpg"
                val imageUrl = fotosBucket.upload(path = imagePath, data = fotoByteArray, upsert = false)
                finalMedicao = medicao.copy(foto = imageUrl)
            }
            // Insere o objeto final (com ou sem a nova URL da foto)
            medicoesTable.insert(finalMedicao)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun updateMedicao(medicao: Medicao, fotoByteArray: ByteArray?): Result<Unit> {
        return try {
            val medicaoId = requireNotNull(medicao.id) { "ID da medição não pode ser nulo para atualização" }

            var medicaoParaAtualizar = medicao

            // Se uma nova foto foi fornecida...
            if (fotoByteArray != null) {
                // Apaga a foto antiga para não deixar lixo no Storage.
                medicao.foto?.let { urlAntiga ->
                    try {
                        val nomeArquivoAntigo = urlAntiga.substringAfterLast("/")
                        fotosBucket.delete(nomeArquivoAntigo)
                    } catch (e: Exception) {
                        // Apenas loga o erro, não impede a operação principal.
                        println("Falha ao apagar foto antiga: ${e.message}")
                    }
                }

                // 2. Faz o upload da nova foto.
                val imagePath = "public/${UUID.randomUUID()}.jpg"
                val novaUrl = fotosBucket.upload(path = imagePath, data = fotoByteArray, upsert = false)

                // 3. Atualiza o objeto de medição com a URL da nova foto.
                medicaoParaAtualizar = medicao.copy(foto = novaUrl)
            }

            // 4. Atualiza os dados na tabela, usando o objeto (possivelmente já com a nova URL).
            medicoesTable.update(medicaoParaAtualizar) {
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
            // 1. Antes de deletar o registro, busca a URL da foto para apagá-la.
            val medicaoParaDeletar = getMedicaoById(id).getOrNull()
            medicaoParaDeletar?.foto?.let { fotoUrl ->
                try {
                    val nomeArquivo = fotoUrl.substringAfterLast("/")
                    fotosBucket.delete(nomeArquivo)
                } catch (e: Exception) {
                    println("Falha ao apagar foto do storage: ${e.message}")
                }
            }

            // 2. Deleta o registro da tabela.
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