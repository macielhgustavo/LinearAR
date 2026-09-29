package br.com.linear.energyar
import br.com.linear.energyar.data.*
import org.junit.Assert.*
import org.junit.Test
class EnergyRepositoryTest {
    @Test fun normalCompressorMatchesReference() {
        val compressor = EnergyRepository.machines(false).first()
        assertEquals(MachineStatus.NORMAL, compressor.status)
        assertEquals(0.0, compressor.deviationPercent, 0.001)
    }
    @Test fun anomalyChangesPowerWithoutInventingAccumulatedEnergy() {
        val normal = EnergyRepository.machines(false)
        val anomaly = EnergyRepository.machines(true)
        assertEquals(MachineStatus.CRITICAL, anomaly.first().status)
        assertEquals(28.0, anomaly.first().deviationPercent, 0.001)
        assertEquals(normal.sumOf { it.consumptionKwh }, anomaly.sumOf { it.consumptionKwh }, 0.001)
        assertEquals(normal.drop(1), anomaly.drop(1))
    }
    @Test fun warningThresholdAndDailyCostAreConsistent() {
        val machines = EnergyRepository.machines(false)
        assertEquals(MachineStatus.WARNING, machines.last().status)
        assertEquals(916.0, machines.sumOf { it.consumptionKwh }, 0.001)
        assertEquals(778.60, machines.sumOf { it.consumptionKwh } * EnergyRepository.tariff, 0.001)
    }
}
