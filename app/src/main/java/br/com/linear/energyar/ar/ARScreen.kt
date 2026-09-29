package br.com.linear.energyar.ar

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.google.ar.core.Frame
import com.google.ar.core.Config
import com.google.ar.core.Plane
import com.google.ar.core.TrackingState
import io.github.sceneview.ar.ARScene
import io.github.sceneview.ar.arcore.createAnchorOrNull
import io.github.sceneview.ar.arcore.isValid
import io.github.sceneview.ar.node.AnchorNode
import io.github.sceneview.node.ModelNode
import io.github.sceneview.rememberEngine
import io.github.sceneview.rememberModelLoader
import io.github.sceneview.rememberNodes
import io.github.sceneview.rememberOnGestureListener
import br.com.linear.energyar.data.Machine
import br.com.linear.energyar.data.decimal
import br.com.linear.energyar.ui.ScenarioSwitch

@Composable
fun ARScreen(machine: Machine, anomaly: Boolean, onAnomaly: (Boolean) -> Unit, modifier: Modifier) {
    val context = LocalContext.current
    var granted by remember { mutableStateOf(ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) }
    var asked by remember { mutableStateOf(false) }
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner, context) {
        val observer = LifecycleEventObserver { _, event ->
            if(event == Lifecycle.Event.ON_RESUME) {
                granted = ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }
    val permission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted = it; asked = true }
    if(!granted) {
        Column(modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Text("Visualize o compressor no ambiente", style = MaterialTheme.typography.headlineSmall)
            Text("Permita o acesso à câmera para posicionar o modelo 3D sobre uma mesa ou no chão. Use um celular compatível com ARCore e o Google Play Services para RA atualizado.")
            Button(onClick = { permission.launch(Manifest.permission.CAMERA) }) { Text("Permitir câmera") }
            if(asked) TextButton(onClick = { context.startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:${context.packageName}"))) }) { Text("Abrir permissões nas configurações") }
        }
    } else CompressorScene(machine, anomaly, onAnomaly, modifier)
}

@Composable
private fun CompressorScene(machine: Machine, anomaly: Boolean, onAnomaly: (Boolean) -> Unit, modifier: Modifier) {
    val engine = rememberEngine()
    val modelLoader = rememberModelLoader(engine)
    val nodes = rememberNodes()
    // Store the latest frame without triggering Compose on every camera update.
    val frameHolder = remember { arrayOfNulls<Frame>(1) }
    var placed by remember { mutableStateOf(false) }
    var selected by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var tracking by remember { mutableStateOf("Movimente o celular devagar para detectar uma superfície.") }
    Box(modifier) {
        ARScene(
            modifier = Modifier.fillMaxSize(), engine = engine, modelLoader = modelLoader,
            childNodes = nodes, planeRenderer = !placed,
            sessionConfiguration = { _, config ->
                config.planeFindingMode = Config.PlaneFindingMode.HORIZONTAL
                config.instantPlacementMode = Config.InstantPlacementMode.DISABLED
                config.lightEstimationMode = Config.LightEstimationMode.ENVIRONMENTAL_HDR
            },
            onSessionFailed = { error = "Não foi possível iniciar a RA. Confira a compatibilidade do aparelho e o Google Play Services para RA. ${it.javaClass.simpleName}" },
            onSessionUpdated = { _, frame ->
                frameHolder[0] = frame
                val message = if(frame.camera.trackingState != TrackingState.TRACKING) "Movimente o celular devagar e mantenha boa iluminação."
                else if(placed) "Toque no compressor para ver os dados. Use dois dedos para girar ou ajustar o tamanho."
                else "Toque na superfície marcada para posicionar o compressor."
                if(message != tracking) tracking = message
            },
            onGestureListener = rememberOnGestureListener(onSingleTapConfirmed = { event, node ->
                if(node != null && placed) selected = true
                else if(!placed) {
                    val hit = frameHolder[0]?.hitTest(event.x, event.y)?.firstOrNull {
                        val plane = it.trackable as? Plane
                        plane?.type == Plane.Type.HORIZONTAL_UPWARD_FACING && it.isValid(depthPoint = false, point = false)
                    }
                    val anchor = hit?.createAnchorOrNull()
                    if(anchor != null) {
                        var anchorNode: AnchorNode? = null
                        try {
                            anchorNode = AnchorNode(engine, anchor)
                            val model = ModelNode(modelInstance = modelLoader.createModelInstance("models/compressor.glb"), scaleToUnits = 0.5f).apply {
                                isEditable = true
                                editableScaleRange = 0.15f..1.5f
                            }
                            anchorNode.addChildNode(model)
                            nodes += anchorNode
                            placed = true
                            selected = true
                        } catch(e: Exception) {
                            anchorNode?.destroy()
                            anchor.detach()
                            error = "Falha ao carregar o compressor: ${e.javaClass.simpleName}"
                        }
                    }
                }
            })
        )
        Card(Modifier.align(Alignment.TopCenter).fillMaxWidth().padding(16.dp)) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("COMPRESSOR VIRTUAL • SIMULAÇÃO", style = MaterialTheme.typography.labelSmall)
                Text(error ?: tracking)
                if(placed) TextButton(onClick = {
                    nodes.toList().forEach { (it as? AnchorNode)?.anchor?.detach(); it.destroy() }
                    nodes.clear(); placed = false; selected = false; error = null
                }) { Text("Reposicionar modelo") }
            }
        }
        if(selected && placed) Card(Modifier.align(Alignment.BottomCenter).fillMaxWidth().padding(16.dp)) {
            Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(machine.name, Modifier.weight(1f), style = MaterialTheme.typography.titleLarge)
                    TextButton(onClick = { selected = false }) { Text("Fechar") }
                }
                Text("${machine.powerKw.decimal()} kW • ${machine.consumptionKwh.decimal()} kWh hoje", style = MaterialTheme.typography.titleMedium)
                Text("${machine.status.label} • ${machine.deviationPercent.decimal()}% acima da referência", color = if(anomaly) Color(0xFFFF8E8E) else Color(0xFF76E8B4))
                ScenarioSwitch(anomaly, onAnomaly)
                Text("Motor: aciona o conjunto compressor. Reservatório: armazena o ar comprimido.", style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}
