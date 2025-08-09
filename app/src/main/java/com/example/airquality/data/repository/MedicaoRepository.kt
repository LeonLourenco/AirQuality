package com.example.airquality.data.repository

import com.example.airquality.data.model.Medicao

/**
 * Interface que define o contrato para operações de dados com as medições.
 * Isso permite desacoplar a implementação (ex: Supabase) das camadas de ViewModel.
 */
interface MedicaoRepository {

    /**
     * Busca todas as medições da fonte de dados.
     * Retorna um Result contendo a lista de medições ou um erro.
     */
    suspend fun getMedicoes(): Result<List<Medicao>>

    /**
     * Busca uma única medição pelo seu ID.
     * Retorna um Result contendo a medição ou um erro.
     */
    suspend fun getMedicaoById(id: String): Result<Medicao?>

    /**
     * Adiciona uma nova medição e, opcionalmente, uma foto associada.
     * @param medicao O objeto de medição a ser salvo.
     * @param fotoByteArray Um array de bytes da foto a ser feito o upload.
     * Retorna um Result de sucesso ou falha.
     */
    suspend fun addMedicao(medicao: Medicao, fotoByteArray: ByteArray?): Result<Unit>

    /**
     * Atualiza uma medição existente e, opcionalmente, sua foto.
     * @param medicao O objeto de medição com os dados atualizados.
     * @param fotoByteArray Um array de bytes da nova foto, ou nulo se a foto não mudou.
     * Retorna um Result de sucesso ou falha.
     */
    suspend fun updateMedicao(medicao: Medicao, fotoByteArray: ByteArray?): Result<Unit>

    /**
     * Deleta uma medição pelo seu ID.
     * Retorna um Result de sucesso ou falha.
     */
    suspend fun deleteMedicao(id: String): Result<Unit>
}