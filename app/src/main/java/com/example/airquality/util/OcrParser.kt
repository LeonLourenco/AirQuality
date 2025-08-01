package com.example.airquality.util

/**
 * Uma classe utilitária para analisar texto extraído de imagens (OCR)
 * de sensores de qualidade do ar.
 */
class OcrParser {

    /**
     * Procura num texto por padrões correspondentes a dados de sensores (CO2, HCHO, etc.)
     * e extrai os seus valores numéricos.
     *
     * @param text O texto completo obtido a partir do OCR.
     * @return Um Map<String, Double> onde a chave é o nome do sensor (ex: "CO2")
     * e o valor é o dado numérico extraído.
     */
    fun parseSensorData(text: String): Map<String, Double> {
        val data = mutableMapOf<String, Double>()

        // Expressão regular para encontrar um rótulo (CO2, HCHO, etc.) seguido por um número.
        // A regex ignora maiúsculas/minúsculas e lida com espaços e números decimais.
        val co2Regex = """CO2\s*[:\-]?\s*(\d+\.?\d*)""".toRegex(RegexOption.IGNORE_CASE)
        val hchoRegex = """HCHO\s*[:\-]?\s*(\d+\.?\d*)""".toRegex(RegexOption.IGNORE_CASE)
        val tvocRegex = """TVOC\s*[:\-]?\s*(\d+\.?\d*)""".toRegex(RegexOption.IGNORE_CASE)
        val tempRegex = """TEMP\s*[:\-]?\s*(\d+\.?\d*)""".toRegex(RegexOption.IGNORE_CASE)
        val humiRegex = """HUMI\s*[:\-]?\s*(\d+\.?\d*)""".toRegex(RegexOption.IGNORE_CASE)

        // Tenta encontrar cada padrão no texto e, se bem-sucedido, adiciona ao mapa.
        co2Regex.find(text)?.groupValues?.get(1)?.toDoubleOrNull()?.let { data["CO2"] = it }
        hchoRegex.find(text)?.groupValues?.get(1)?.toDoubleOrNull()?.let { data["HCHO"] = it }

        // --- CÓDIGO CORRIGIDO ---
        tvocRegex.find(text)?.groupValues?.get(1)?.toDoubleOrNull()?.let { data["TVOC"] = it }
        tempRegex.find(text)?.groupValues?.get(1)?.toDoubleOrNull()?.let { data["TEMP"] = it }
        // -------------------------

        humiRegex.find(text)?.groupValues?.get(1)?.toDoubleOrNull()?.let { data["HUMI"] = it }

        return data
    }
}