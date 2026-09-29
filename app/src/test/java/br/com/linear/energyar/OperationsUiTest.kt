package br.com.linear.energyar

import android.app.Application
import android.graphics.Bitmap
import android.graphics.Canvas
import androidx.activity.ComponentActivity
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import br.com.linear.energyar.data.*
import br.com.linear.energyar.ui.EnergyApp
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File

@RunWith(RobolectricTestRunner::class)
@Config(sdk=[34], qualifiers="w411dp-h891dp-xxhdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class OperationsUiTest {
    @get:Rule val compose=createAndroidComposeRule<ComponentActivity>()
    private fun freshApp(): Application {
        val app=RuntimeEnvironment.getApplication()
        app.getSharedPreferences("energyar_inspections_v2",0).edit().clear().commit()
        return app
    }
    private fun capture(name: String) {
        compose.runOnIdle {
            val view=compose.activity.window.decorView
            val bitmap=Bitmap.createBitmap(view.width,view.height,Bitmap.Config.ARGB_8888)
            view.draw(Canvas(bitmap))
            File("build/ui-check/$name.png").outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG,100,it) }
            bitmap.recycle()
        }
    }
    @Test fun operationsAndActionsRenderAndNavigate(){
        val vm=EnergyViewModel(freshApp())
        compose.setContent { EnergyApp(vm) }
        compose.onNodeWithText("Iniciar inspeção em AR").assertExists()
        File("build/ui-check").mkdirs()
        capture("operations")
        compose.onNodeWithText("Ações",useUnmergedTree=true).performClick()
        compose.onNodeWithText("SIMULADOR DE ECONOMIA").assertExists()
        capture("actions")
        compose.onNodeWithText("Máquinas",useUnmergedTree=true).performClick()
        compose.onNodeWithText("Seu parque industrial").assertExists()
        compose.onNodeWithText("Compressor 01").performClick()
        compose.onNodeWithText("Trajetória de potência").assertExists()
    }
    @Test fun completedInspectionSurvivesRestartAndCanBeResolved(){
        val app=freshApp();val vm=EnergyViewModel(app)
        assertFalse(vm.finish())
        ComponentId.entries.forEach(vm::inspect)
        assertFalse(vm.identify(ComponentId.TANK))
        assertFalse(vm.finish())
        assertTrue(vm.identify(ComponentId.VALVE))
        assertTrue(vm.finish())
        val restored=EnergyViewModel(app)
        assertEquals(1,restored.state.value.records.size)
        val r=restored.state.value.records.first()
        assertFalse(r.resolved)
        restored.resolve(r.id)
        val final=EnergyViewModel(app).state.value
        assertTrue(final.records.first().resolved)
        assertEquals(Scenario.NORMAL,final.scenario)
        assertEquals(0.0,final.monthlySaving,.001)
    }
}
