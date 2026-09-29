package br.com.linear.energyar.data

import java.util.Locale

enum class MachineStatus(val label: String) { NORMAL("Normal"), WARNING("Atenção"), CRITICAL("Consumo elevado") }
data class Machine(
    val id: String, val name: String, val type: String,
    val powerKw: Double, val referenceKw: Double, val consumptionKwh: Double,
    val history: List<Double>, val recommendation: String
) {
    val deviationPercent: Double get() = (powerKw / referenceKw - 1) * 100
    val status: MachineStatus get() = when {
        deviationPercent >= 20 -> MachineStatus.CRITICAL
        deviationPercent >= 10 -> MachineStatus.WARNING
        else -> MachineStatus.NORMAL
    }
}
object EnergyRepository {
    const val tariff = 0.85
    fun machines(anomaly: Boolean) = listOf(
        Machine("compressor", "Compressor 01", "Ar comprimido", if (anomaly) 38.4 else 30.0, 30.0, 214.6,
            listOf(25.0, 28.0, 31.0, 29.0, 30.0, if (anomaly) 38.4 else 30.0),
            "Inspecionar possíveis vazamentos e verificar operação em períodos ociosos. O alerta indica uma variação simulada, sem diagnóstico automático."),
        Machine("injetora", "Injetora 01", "Transformação de plástico", 42.3, 40.0, 318.5,
            listOf(37.0, 41.0, 39.0, 43.0, 40.0, 42.3), "Acompanhar o ciclo de produção e comparar com a referência de operação."),
        Machine("extrusora", "Extrusora 01", "Extrusão", 51.8, 46.0, 382.9,
            listOf(42.0, 46.0, 48.0, 47.0, 50.0, 51.8), "Verificar períodos de aquecimento e confrontar o consumo com o volume produzido.")
    )
}
fun Double.decimal() = String.format(Locale.forLanguageTag("pt-BR"), "%.1f", this)
fun Double.currency() = String.format(Locale.forLanguageTag("pt-BR"), "R$ %.2f", this)
