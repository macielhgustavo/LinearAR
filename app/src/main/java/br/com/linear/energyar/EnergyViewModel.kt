package br.com.linear.energyar
import androidx.lifecycle.ViewModel
import br.com.linear.energyar.data.EnergyRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
class EnergyViewModel : ViewModel() {
    private val _anomaly = MutableStateFlow(false)
    val anomaly = _anomaly.asStateFlow()
    fun setAnomaly(value: Boolean) { _anomaly.value = value }
    fun machines(anomaly: Boolean) = EnergyRepository.machines(anomaly)
}
