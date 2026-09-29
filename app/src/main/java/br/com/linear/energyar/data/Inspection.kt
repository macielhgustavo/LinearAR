package br.com.linear.energyar.data

enum class Scenario(val label: String, val headline: String, val power: Double, val reference: Double, val culprit: ComponentId?) {
    NORMAL("Normal", "Operação dentro da referência", 30.0, 30.0, null),
    LEAK("Vazamento", "Investigue uma perda na rede de ar", 38.4, 30.0, ComponentId.VALVE),
    IDLE("Ociosidade", "Investigue energia fora de produção", 12.0, 2.0, ComponentId.MOTOR)
}
enum class ComponentId(val title: String, val subtitle: String, val asset: String, val explanation: String) {
    TANK("Reservatório", "Armazenamento de ar", "tank", "Armazena o ar comprimido e estabiliza a pressão da rede. A inspeção é visual e educativa; não representa uma medição de pressão."),
    MOTOR("Motor e cabeçote", "Conversão de energia", "motor", "O motor aciona o conjunto compressor. Operar durante períodos sem produção pode manter um gasto evitável. Compare o estado operacional com a potência."),
    VALVE("Válvula de saída", "Distribuição e vedação", "valve", "A saída conecta o reservatório à rede. Neste cenário educativo, partículas vermelhas representam uma fuga de ar; não são detecção real pela câmera."),
    GAUGE("Manômetro", "Indicação de pressão", "gauge", "Permite observar a pressão no reservatório. O mostrador 3D é ilustrativo. Pressão baixa pode motivar uma verificação da rede, sem concluir um diagnóstico.")
}
enum class VisualMode(val label: String) { ASSEMBLED("Operação"), XRAY("Raio X"), EXPLODED("Desmontar") }
data class InspectionRecord(val id: String, val timestamp: Long, val scenario: Scenario, val observedKw: Double, val projectedMonthlySaving: Double, val tariff: Double, val hours: Double, val days: Int, val resolved: Boolean = false)
data class InspectionState(
    val scenario: Scenario = Scenario.LEAK,
    val checked: Set<ComponentId> = emptySet(),
    val selected: ComponentId? = null,
    val found: Boolean = false,
    val hours: Double = 8.0, val days: Int = 22, val tariff: Double = 0.85,
    val records: List<InspectionRecord> = emptyList()
) {
    val complete: Boolean get() = checked.size == ComponentId.entries.size && (scenario.culprit == null || found)
    val extraKw: Double get() = (scenario.power - scenario.reference).coerceAtLeast(0.0)
    val monthlySaving: Double get() = estimatedSaving(scenario.power, scenario.reference, hours, days, tariff)
}
fun estimatedSaving(power: Double, reference: Double, hours: Double, days: Int, tariff: Double): Double =
    (power - reference).coerceAtLeast(0.0) * hours.coerceIn(0.0, 24.0) * days.coerceIn(0, 31) * tariff.coerceAtLeast(0.0)
fun machinesFor(scenario: Scenario): List<Machine> = EnergyRepository.machines(scenario == Scenario.LEAK).map {
    if(it.id == "compressor") it.copy(powerKw = scenario.power, referenceKw = scenario.reference,
        history = listOf(scenario.reference, scenario.reference * 1.05, scenario.power * .9, scenario.power, scenario.power * .96, scenario.power)) else it
}
fun Scenario.finding() = when(this) {
    Scenario.NORMAL -> "Nenhuma perda adicionada ao cenário. Manter acompanhamento."
    Scenario.LEAK -> "Fuga simulada na válvula de saída. Verificar vedação e conexões da rede."
    Scenario.IDLE -> "Motor operando sem produção no cenário. Revisar programação de desligamento."
}
