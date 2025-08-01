package com.example.airquality.util

import android.content.Context
import android.net.Uri
import java.io.IOException

class ImageUtils {
    /**
     * Lê o conteúdo de uma Uri e converte-o para um ByteArray.
     * @param context O contexto da aplicação para aceder ao ContentResolver.
     * @param uri A Uri do ficheiro a ser lido.
     * @return Um ByteArray contendo os dados do ficheiro, ou null em caso de erro.
     */
    fun uriToByteArray(context: Context, uri: Uri): ByteArray? {
        return try {
            // Usa o ContentResolver para abrir um stream de input para a Uri
            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                // Lê todos os bytes do stream e retorna
                inputStream.readBytes()
            }
        } catch (e: IOException) {
            // Em caso de erro de leitura, imprime o erro e retorna null
            e.printStackTrace()
            null
        }
    }
}