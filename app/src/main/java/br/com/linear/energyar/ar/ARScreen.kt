package br.com.linear.energyar.ar

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import android.opengl.Matrix
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.google.ar.core.Frame
import com.google.ar.core.Config
import com.google.ar.core.Plane
import com.google.ar.core.TrackingState
import io.github.sceneview.Scene
import io.github.sceneview.ar.ARScene
import io.github.sceneview.ar.arcore.createAnchorOrNull
import io.github.sceneview.ar.arcore.isValid
import io.github.sceneview.ar.node.AnchorNode
import io.github.sceneview.math.Position
import io.github.sceneview.rememberCameraNode
import io.github.sceneview.rememberEngine
import io.github.sceneview.rememberModelLoader
import io.github.sceneview.rememberMaterialLoader
import io.github.sceneview.rememberNodes
import io.github.sceneview.rememberOnGestureListener
import br.com.linear.energyar.data.*
import br.com.linear.energyar.ui.*

@Composable
fun InspectionLab(s:InspectionState,useAR:Boolean,onAR:(Boolean)->Unit,onSelect:(ComponentId)->Unit,onCheck:(ComponentId)->Unit,onIdentify:(ComponentId)->Unit,onScenario:(Scenario)->Unit,onFinish:()->Unit,modifier:Modifier) {
    var mode by rememberSaveable{mutableStateOf(VisualMode.ASSEMBLED)}
    var angle by rememberSaveable{mutableFloatStateOf(0f)}
    var scale by rememberSaveable{mutableFloatStateOf(.55f)}
    var reset by remember{mutableIntStateOf(0)}
    var showGuide by rememberSaveable{mutableStateOf(s.records.isEmpty())}
    Column(modifier.padding(horizontal=16.dp),verticalArrangement=Arrangement.spacedBy(8.dp)){
        Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically){
            Column(Modifier.weight(1f)){Eyebrow("INSPEÇÃO GUIADA",Mint);Text("Compressor 01",style=MaterialTheme.typography.titleLarge)}
            StatusPill("${s.checked.size}/4",if(s.complete)Mint else Blue)
            IconButton(onClick={showGuide=true}){Icon(Icons.Default.HelpOutline,"Como inspecionar")}
        }
        Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(8.dp)){
            FilterChip(selected=!useAR,onClick={onAR(false)},label={Text("Explorar 3D")},leadingIcon={Icon(Icons.Default.ViewInAr,null,Modifier.size(16.dp))})
            FilterChip(selected=useAR,onClick={onAR(true)},label={Text("AR ao vivo")},leadingIcon={Icon(Icons.Default.CameraAlt,null,Modifier.size(16.dp))})
        }
        Box(Modifier.weight(1f).fillMaxWidth().clip(RoundedCornerShape(24.dp)).background(Brush.radialGradient(listOf(Color(0xFF24434B),Ink)))){
            key(useAR,reset){InspectionViewport(s,useAR,mode,scale,angle,onSelect,Modifier.fillMaxSize())}
            Row(Modifier.align(Alignment.BottomEnd).padding(10.dp).background(Ink.copy(alpha=.8f),RoundedCornerShape(50)),verticalAlignment=Alignment.CenterVertically){
                IconButton(onClick={angle=(angle+45)%360}){Icon(Icons.Default.RotateRight,"Girar modelo 45 graus",tint=Mint)}
                IconButton(onClick={scale=(scale-.1f).coerceAtLeast(.25f)}){Icon(Icons.Default.Remove,"Diminuir modelo")}
                IconButton(onClick={scale=(scale+.1f).coerceAtMost(1.1f)}){Icon(Icons.Default.Add,"Aumentar modelo")}
                IconButton(onClick={reset++;angle=0f;scale=.55f}){Icon(Icons.Default.RestartAlt,"Reposicionar modelo")}
            }
        }
        Row(Modifier.horizontalScroll(rememberScrollState()),horizontalArrangement=Arrangement.spacedBy(8.dp)){
            VisualMode.entries.forEach{m->FilterChip(selected=mode==m,onClick={mode=m},label={Text(m.label)})}
        }
        Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(6.dp)){
            ComponentId.entries.forEachIndexed{i,id->
                val checked=id in s.checked
                OutlinedButton(onClick={onSelect(id)},modifier=Modifier.weight(1f),contentPadding=PaddingValues(horizontal=3.dp,vertical=8.dp),colors=ButtonDefaults.outlinedButtonColors(contentColor=if(s.selected==id)Mint else Muted)){
                    if(checked)Icon(Icons.Default.Check,null,Modifier.size(14.dp))else Text("${i+1}")
                    Spacer(Modifier.width(3.dp));Text(listOf("Tanque","Motor","Saída","Pressão")[i],style=MaterialTheme.typography.labelSmall,maxLines=1)
                }
            }
        }
        if(s.selected!=null)ComponentPanel(s,onCheck,onIdentify)
        else Row(Modifier.fillMaxWidth().padding(vertical=8.dp),verticalAlignment=Alignment.CenterVertically){
            Icon(Icons.Default.TouchApp,null,tint=Mint);Spacer(Modifier.width(10.dp));Text("Toque nos componentes ou nos pontos numerados para começar.",color=Muted,style=MaterialTheme.typography.bodySmall)
        }
        if(s.complete)Button(onClick=onFinish,modifier=Modifier.fillMaxWidth()){Icon(Icons.Default.Save,null);Spacer(Modifier.width(8.dp));Text("Finalizar e salvar inspeção")}
        else Text(if(s.scenario.culprit!=null && !s.found)"Inspecione os 4 pontos e identifique a origem da perda." else "Confira os 4 componentes para concluir a inspeção.",color=Muted,style=MaterialTheme.typography.labelSmall,modifier=Modifier.padding(bottom=8.dp))
    }
    if(showGuide)AlertDialog(onDismissRequest={showGuide=false},title={Text("Sua missão")},text={Column(verticalArrangement=Arrangement.spacedBy(12.dp)){
        Text(s.scenario.headline,fontWeight=androidx.compose.ui.text.font.FontWeight.Bold)
        Text("1. Explore o compressor e selecione os quatro componentes.\n\n2. Use Raio X para acompanhar o fluxo e Desmontar para separar as peças.\n\n3. Marque os pontos inspecionados e identifique a origem da perda. Salve o relatório ao concluir.")
        Text("Tudo é simulado. A câmera não detecta vazamentos reais.",color=Muted)
    }},confirmButton={TextButton(onClick={showGuide=false}){Text("Começar")}},dismissButton={TextButton(onClick={onScenario(Scenario.NORMAL);showGuide=false}){Text("Treinar operação normal")}})
}

@Composable private fun ComponentPanel(s:InspectionState,onCheck:(ComponentId)->Unit,onIdentify:(ComponentId)->Unit){
    val id=s.selected?:return
    var expanded by remember(id){mutableStateOf(false)}
    Column(Modifier.fillMaxWidth().background(Panel,RoundedCornerShape(20.dp)).padding(14.dp),verticalArrangement=Arrangement.spacedBy(8.dp)){
        Row(verticalAlignment=Alignment.CenterVertically){Column(Modifier.weight(1f)){Eyebrow("COMPONENTE ${id.ordinal+1} / SIMULAÇÃO",Mint);Text(id.title,style=MaterialTheme.typography.titleMedium)};Text(when(id){ComponentId.TANK,ComponentId.GAUGE->if(s.scenario==Scenario.LEAK)"6,4 bar" else "7,8 bar";ComponentId.MOTOR->"${s.scenario.power.decimal()} kW";ComponentId.VALVE->if(s.scenario==Scenario.LEAK)"Fuga de ar" else "Vedação OK"},color=if(s.scenario.culprit==id)Coral else Blue,style=MaterialTheme.typography.labelLarge)}
        Text(id.explanation,color=Muted,style=MaterialTheme.typography.bodySmall,maxLines=2,modifier=Modifier.clickable{expanded=!expanded})
        Row(horizontalArrangement=Arrangement.spacedBy(8.dp)){
            FilledTonalButton(onClick={onCheck(id)},modifier=Modifier.weight(1f),contentPadding=PaddingValues(8.dp)){Text(if(id in s.checked)"✓ Inspecionado" else "Marcar inspecionado",style=MaterialTheme.typography.labelMedium)}
            if(s.scenario.culprit!=null && !s.found)OutlinedButton(onClick={onIdentify(id)},modifier=Modifier.weight(1f),contentPadding=PaddingValues(8.dp)){Text("Origem da perda",style=MaterialTheme.typography.labelMedium)}
            if(s.found && s.scenario.culprit==id)StatusPill("Localizada",Coral)
        }
    }
    if(expanded)AlertDialog(onDismissRequest={expanded=false},title={Text(id.title)},text={Text(id.explanation)},confirmButton={TextButton(onClick={expanded=false}){Text("Entendi")}})
}
private data class ScreenMarker(val id:ComponentId,val x:Int,val y:Int)

@Composable private fun InspectionViewport(s:InspectionState,useAR:Boolean,mode:VisualMode,scale:Float,angle:Float,onSelect:(ComponentId)->Unit,modifier:Modifier){
    val context=LocalContext.current
    var granted by remember{mutableStateOf(ContextCompat.checkSelfPermission(context,Manifest.permission.CAMERA)==PackageManager.PERMISSION_GRANTED)}
    val lifecycle=LocalLifecycleOwner.current
    DisposableEffect(lifecycle){val observer=LifecycleEventObserver{_,e->if(e==Lifecycle.Event.ON_RESUME)granted=ContextCompat.checkSelfPermission(context,Manifest.permission.CAMERA)==PackageManager.PERMISSION_GRANTED};lifecycle.lifecycle.addObserver(observer);onDispose{lifecycle.lifecycle.removeObserver(observer)}}
    val permission=rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()){granted=it}
    if(useAR && !granted){Column(modifier.padding(20.dp),verticalArrangement=Arrangement.spacedBy(10.dp),horizontalAlignment=Alignment.CenterHorizontally){
        Spacer(Modifier.weight(1f));Icon(Icons.Default.CameraAlt,null,tint=Mint,modifier=Modifier.size(38.dp));Text("Posicione o compressor no seu ambiente")
        Button(onClick={permission.launch(Manifest.permission.CAMERA)}){Text("Permitir câmera")}
        TextButton(onClick={context.startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,Uri.parse("package:${context.packageName}")))}){Text("Configurações de permissão")};Spacer(Modifier.weight(1f))
    };return}
    val engine=rememberEngine();val models=rememberModelLoader(engine);val materials=rememberMaterialLoader(engine)
    val nodes=rememberNodes()
    var error by remember{mutableStateOf<String?>(null)}
    val rig=remember(engine){runCatching{CompressorRig(engine,models,materials)}.getOrElse{error="Falha no modelo 3D: ${it.javaClass.simpleName}";null}}
    val frameHolder=remember{arrayOfNulls<Frame>(1)}
    var placed by remember{mutableStateOf(!useAR)}
    var tracking by remember{mutableStateOf("Movimente o celular e toque em uma superfície.")}
    var viewport by remember{mutableStateOf(IntSize.Zero)}
    var markers by remember{mutableStateOf(emptyList<ScreenMarker>())}
    val latestState by rememberUpdatedState(s);val latestMode by rememberUpdatedState(mode)
    val latestScale by rememberUpdatedState(scale);val latestAngle by rememberUpdatedState(angle)
    val latestSelect by rememberUpdatedState(onSelect)
    val start=remember{System.nanoTime()};val lastProjection=remember{longArrayOf(0)}
    val viewMatrix=remember{FloatArray(16)};val projection=remember{FloatArray(16)};val combined=remember{FloatArray(16)};val point=remember{FloatArray(4)}
    DisposableEffect(rig,useAR){if(!useAR && rig!=null){rig.root.position=Position(0f,-.3f,0f);nodes.add(rig.root)};onDispose{nodes.filterIsInstance<AnchorNode>().forEach{it.anchor?.detach()}}}
    Box(modifier.onSizeChanged{viewport=it}){
        if(rig!=null){
            if(useAR)ARScene(modifier=Modifier.fillMaxSize(),engine=engine,modelLoader=models,materialLoader=materials,childNodes=nodes,planeRenderer=!placed,
                sessionConfiguration={_,c->c.planeFindingMode=Config.PlaneFindingMode.HORIZONTAL;c.instantPlacementMode=Config.InstantPlacementMode.DISABLED;c.lightEstimationMode=Config.LightEstimationMode.ENVIRONMENTAL_HDR},
                onSessionFailed={error="RA indisponível neste aparelho ou Google Play Services para RA desatualizado. Use Explorar 3D para continuar."},
                onSessionUpdated={_,f->
                    frameHolder[0]=f
                    val message=if(f.camera.trackingState!=TrackingState.TRACKING)"Melhore a iluminação e movimente o celular devagar."else if(placed)"Compressor ancorado • toque nos pontos de inspeção"else "Toque na superfície detectada para posicionar."
                    if(message!=tracking)tracking=message
                    if(placed){
                        rig.animate((System.nanoTime()-start)/1e9f,latestMode,latestState.scenario,latestState.selected,latestScale,latestAngle)
                        if(f.timestamp-lastProjection[0]>100_000_000 && viewport.width>0 && f.camera.trackingState==TrackingState.TRACKING){
                            lastProjection[0]=f.timestamp;f.camera.getViewMatrix(viewMatrix,0);f.camera.getProjectionMatrix(projection,0,.05f,100f);Matrix.multiplyMM(combined,0,projection,0,viewMatrix,0)
                            markers=rig.markers.mapNotNull{(id,n)->val p=n.worldPosition;Matrix.multiplyMV(point,0,combined,0,floatArrayOf(p.x,p.y,p.z,1f),0)
                                if(point[3]<=0f)null else{val x=(point[0]/point[3]+1f)*viewport.width/2;val y=(1f-point[1]/point[3])*viewport.height/2
                                    if(x<16||x>viewport.width-16||y<20||y>viewport.height-55)null else ScreenMarker(id,x.toInt()-16,y.toInt()-35)}
                            }
                        }
                    }else markers=emptyList()
                },onGestureListener=rememberOnGestureListener(onSingleTapConfirmed={event,node->
                    val id=rig.picked(node)
                    if(id!=null)latestSelect(id)
                    else if(!placed){
                        val hit=frameHolder[0]?.hitTest(event.x,event.y)?.firstOrNull{val p=it.trackable as? Plane;p?.type==Plane.Type.HORIZONTAL_UPWARD_FACING&&it.isValid(depthPoint=false,point=false)}
                        hit?.createAnchorOrNull()?.let{a->try{val anchor=AnchorNode(engine,a);rig.root.position=Position(0f);anchor.addChildNode(rig.root);nodes.add(anchor);placed=true}catch(e:Exception){a.detach();error="Não foi possível ancorar o modelo. Reposicione e tente novamente."}}
                    }
                }))
            else{
                val camera=rememberCameraNode(engine){position=Position(1.1f,.85f,1.6f);lookAt(Position(0f,.1f,0f))}
                Scene(modifier=Modifier.fillMaxSize(),engine=engine,modelLoader=models,materialLoader=materials,cameraNode=camera,childNodes=nodes,isOpaque=false,
                    onFrame={rig.animate((System.nanoTime()-start)/1e9f,latestMode,latestState.scenario,latestState.selected,latestScale,latestAngle)},
                    onGestureListener=rememberOnGestureListener(onSingleTapConfirmed={_,node->rig.picked(node)?.let{latestSelect(it)}}))
            }
        }
        Text(error ?: if(useAR)tracking else "Arraste para explorar • toque nas peças",color=Color.White,style=MaterialTheme.typography.labelSmall,modifier=Modifier.align(Alignment.TopStart).padding(12.dp).background(Ink.copy(alpha=.8f),RoundedCornerShape(8.dp)).padding(8.dp))
        markers.forEach{m->Box(Modifier.offset{IntOffset(m.x,m.y)}.size(30.dp).background(if(s.selected==m.id)Mint else Panel,RoundedCornerShape(50)).border(1.dp,Mint,RoundedCornerShape(50)).clickable{onSelect(m.id)},contentAlignment=Alignment.Center){Text("${m.id.ordinal+1}",color=if(s.selected==m.id)Ink else Mint)}}
        if(s.scenario==Scenario.LEAK && placed)StatusPillOverlay("● Fuga de ar simulada",Modifier.align(Alignment.BottomStart).padding(12.dp),Coral)
        if(s.scenario==Scenario.IDLE && placed)StatusPillOverlay("● Motor ativo sem produção",Modifier.align(Alignment.BottomStart).padding(12.dp),Amber)
    }
}
@Composable private fun StatusPillOverlay(text:String,modifier:Modifier,color:Color){Text(text,color=color,style=MaterialTheme.typography.labelSmall,modifier=modifier.background(Ink.copy(alpha=.85f),RoundedCornerShape(12.dp)).padding(10.dp))}
