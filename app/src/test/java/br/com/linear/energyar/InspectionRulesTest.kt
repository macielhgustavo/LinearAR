package br.com.linear.energyar
import br.com.linear.energyar.data.*
import org.junit.Assert.*
import org.junit.Test
class InspectionRulesTest {
    @Test fun savingUsesOnlyAvoidablePowerAndExplicitAssumptions(){
        assertEquals(1256.64,estimatedSaving(38.4,30.0,8.0,22,.85),.001)
        assertEquals(0.0,estimatedSaving(25.0,30.0,8.0,22,.85),.001)
    }
    @Test fun identificationAloneCannotCompleteAnInspection(){
        assertFalse(InspectionState(found=true).complete)
        assertFalse(InspectionState(checked=ComponentId.entries.toSet()).complete)
        assertTrue(InspectionState(checked=ComponentId.entries.toSet(),found=true).complete)
    }
    @Test fun normalOperationNeedsAllComponentsButNoInventedFinding(){
        assertTrue(InspectionState(scenario=Scenario.NORMAL,checked=ComponentId.entries.toSet()).complete)
        assertEquals(0.0,InspectionState(scenario=Scenario.NORMAL).monthlySaving,.001)
    }
    @Test fun idleScenarioUsesIdleReferenceInsteadOfProductionReference(){
        val m=machinesFor(Scenario.IDLE).first()
        assertEquals(2.0,m.referenceKw,.001)
        assertEquals(12.0,m.powerKw,.001)
        assertEquals(ComponentId.MOTOR,Scenario.IDLE.culprit)
        assertEquals(1496.0,InspectionState(scenario=Scenario.IDLE).monthlySaving,.001)
    }
}
