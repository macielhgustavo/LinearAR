package br.com.linear.energyar
import android.app.Application
import androidx.lifecycle.AndroidViewModel
import br.com.linear.energyar.data.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.UUID
class EnergyViewModel(application: Application) : AndroidViewModel(application) {
    private val store = InspectionStore(application)
    private val _state = MutableStateFlow(store.load())
    val state = _state.asStateFlow()
    private fun update(transform: (InspectionState) -> InspectionState) { _state.value=transform(_state.value);store.save(_state.value) }
    fun scenario(value: Scenario) = update { it.copy(scenario=value, checked=emptySet(), selected=null, found=false) }
    fun select(id: ComponentId) = update { it.copy(selected=id) }
    fun inspect(id: ComponentId) = update { it.copy(checked=it.checked+id) }
    fun identify(id: ComponentId): Boolean {
        val correct = _state.value.scenario.culprit == id
        if(correct) update { it.copy(found=true, checked=it.checked+id) }
        return correct
    }
    fun assumptions(hours: Double? = null, days: Int? = null, tariff: Double? = null) = update {
        it.copy(hours=hours?.coerceIn(1.0,24.0) ?: it.hours,days=days?.coerceIn(1,31) ?: it.days,tariff=tariff?.coerceIn(.1,3.0) ?: it.tariff)
    }
    fun finish(): Boolean {
        val s=_state.value
        if(!s.complete) return false
        update { it.copy(records=listOf(InspectionRecord(UUID.randomUUID().toString(),System.currentTimeMillis(),s.scenario,s.scenario.power,s.monthlySaving,s.tariff,s.hours,s.days,s.scenario==Scenario.NORMAL))+it.records,checked=emptySet(),selected=null,found=false) }
        return true
    }
    fun resolve(id: String) = update { s -> s.copy(records=s.records.map { if(it.id==id) it.copy(resolved=true) else it },scenario=Scenario.NORMAL,checked=emptySet(),found=false,selected=null) }
}
