package br.com.linear.energyar.ui

import android.content.Intent
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import br.com.linear.energyar.EnergyViewModel
import br.com.linear.energyar.R
import br.com.linear.energyar.ar.InspectionLab
import br.com.linear.energyar.data.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EnergyApp(vm: EnergyViewModel=viewModel()) {
    val state by vm.state.collectAsStateWithLifecycle()
    var tab by rememberSaveable { mutableIntStateOf(0) }
    var useAR by rememberSaveable { mutableStateOf(false) }
    var machineId by rememberSaveable { mutableStateOf<String?>(null) }
    val machines=remember(state.scenario){machinesFor(state.scenario)}
    val snackbar=remember{SnackbarHostState()};val scope=rememberCoroutineScope()
    fun message(text:String){scope.launch{snackbar.showSnackbar(text)}}
    fun lab(ar:Boolean){useAR=ar;tab=2;machineId=null}
    BackHandler(enabled=tab!=0 || machineId!=null){if(machineId!=null)machineId=null else tab=0}
    MaterialTheme(colorScheme=darkColorScheme(primary=Mint,onPrimary=Ink,primaryContainer=Color(0xFF244438),onPrimaryContainer=Mint,
        secondary=Blue,onSecondary=Ink,secondaryContainer=Color(0xFF263F48),onSecondaryContainer=Mint,
        tertiary=Amber,onTertiary=Ink,background=Ink,onBackground=Color(0xFFEAF1F5),surface=Panel,onSurface=Color(0xFFEAF1F5),
        surfaceVariant=Panel,onSurfaceVariant=Muted,outline=Color(0xFF435561),outlineVariant=Color(0xFF273A45)),shapes=Shapes(medium=RoundedCornerShape(20.dp),large=RoundedCornerShape(26.dp))) {
        Scaffold(snackbarHost={SnackbarHost(snackbar)},topBar={
            Row(Modifier.fillMaxWidth().statusBarsPadding().padding(horizontal=22.dp,vertical=14.dp),verticalAlignment=Alignment.CenterVertically){
                Box(Modifier.size(38.dp).background(Mint,RoundedCornerShape(12.dp)),contentAlignment=Alignment.Center){Icon(Icons.Default.Bolt,null,tint=Ink)}
                Spacer(Modifier.width(12.dp));Column(Modifier.weight(1f)){Text("ENERGYAR",fontWeight=FontWeight.Black,style=MaterialTheme.typography.titleMedium);Eyebrow("LINEAR  /  INSPEÇÃO ENERGÉTICA")}
                StatusPill("DEMO",Blue)
            }
        },bottomBar={NavigationBar(containerColor=Ink,tonalElevation=0.dp){
            val icons=listOf(Icons.Default.SpaceDashboard,Icons.Default.PrecisionManufacturing,Icons.Default.ViewInAr,Icons.Default.TaskAlt)
            listOf("Operação","Máquinas","Inspecionar","Ações").forEachIndexed{i,t->NavigationBarItem(selected=tab==i,onClick={tab=i;machineId=null},icon={Icon(icons[i],t)},label={Text(t)},colors=NavigationBarItemDefaults.colors(indicatorColor=Mint.copy(alpha=.16f),selectedIconColor=Mint,selectedTextColor=Mint))}
        }}){padding->
            val mod=Modifier.fillMaxSize().padding(padding)
            when(tab){
                0->Operations(state,machines,mod,{vm.scenario(it)},{lab(true)},{machineId=it}, {tab=3})
                1->EquipmentList(machines,mod,{machineId=it})
                2->InspectionLab(state,useAR,{useAR=it},vm::select,vm::inspect,{id->if(vm.identify(id))message("Origem da perda localizada no cenário.")else message("Esse componente não explica a perda deste cenário. Explore os outros pontos.")},vm::scenario,{
                    if(vm.finish()){tab=3;message("Inspeção salva no aparelho.")}
                },mod)
                3->Actions(state,mod,vm::resolve,{lab(false)}, {vm.assumptions(hours=it)}, {vm.assumptions(days=it)}, {vm.assumptions(tariff=it)})
            }
            machines.firstOrNull{it.id==machineId}?.let{m->ModalBottomSheet(onDismissRequest={machineId=null},containerColor=Ink){
                Column(Modifier.verticalScroll(rememberScrollState()).padding(24.dp),verticalArrangement=Arrangement.spacedBy(18.dp)){
                    Eyebrow("EQUIPAMENTO  /  DADOS SIMULADOS");SectionTitle(m.name,m.type);StatusPill(m.status.label,m.status.color())
                    Row(horizontalArrangement=Arrangement.spacedBy(10.dp)){Stat("Potência","${m.powerKw.decimal()} kW",Modifier.weight(1f));Stat("Consumo hoje","${m.consumptionKwh.decimal()} kWh",Modifier.weight(1f),Blue)}
                    SectionTitle("Trajetória de potência","6 amostras ilustrativas • linha tracejada = referência")
                    PowerChart(m.history,m.referenceKw)
                    Text("Referência de operação: ${m.referenceKw.decimal()} kW",color=Amber)
                    Text(m.recommendation,color=Muted)
                    if(m.id=="compressor")Button(onClick={lab(true)},modifier=Modifier.fillMaxWidth()){Text("Inspecionar componentes em AR")}
                    Spacer(Modifier.height(20.dp))
                }
            }}
        }
    }
}

@Composable
private fun Operations(s:InspectionState,machines:List<Machine>,modifier:Modifier,onScenario:(Scenario)->Unit,onAR:()->Unit,onMachine:(String)->Unit,onActions:()->Unit){
    val total=machines.sumOf{it.consumptionKwh}
    LazyColumn(modifier,contentPadding=PaddingValues(22.dp),verticalArrangement=Arrangement.spacedBy(22.dp)){
        item{Column(verticalArrangement=Arrangement.spacedBy(8.dp)){Eyebrow("CENTRAL DE OPERAÇÕES");Text("Encontre a perda.\nEntenda o impacto.",style=MaterialTheme.typography.headlineLarge,fontWeight=FontWeight.Bold);Text("Explore um equipamento, investigue componentes e transforme a inspeção em uma ação.",color=Muted)}}
        item{Column(Modifier.fillMaxWidth().background(Brush.linearGradient(listOf(Color(0xFF193C3F),Panel)),RoundedCornerShape(28.dp)).padding(22.dp),verticalArrangement=Arrangement.spacedBy(12.dp)){
            Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween){Eyebrow("MISSÃO ATIVA",Mint);StatusPill(s.scenario.label,if(s.scenario==Scenario.NORMAL)Mint else Coral)}
            Image(painterResource(R.drawable.compressor_preview),"Modelo do compressor com motor, reservatório e válvula",Modifier.fillMaxWidth().height(165.dp))
            Text(s.scenario.headline,style=MaterialTheme.typography.titleLarge,fontWeight=FontWeight.Bold)
            Text("4 componentes • circuito interno • exploração em AR",color=Muted,style=MaterialTheme.typography.bodySmall)
            Button(onClick=onAR,modifier=Modifier.fillMaxWidth(),contentPadding=PaddingValues(15.dp)){Text("Iniciar inspeção em AR",Modifier.weight(1f));Icon(Icons.AutoMirrored.Filled.ArrowForward,null)}
        }}
        item{ScenarioPicker(s.scenario,onScenario)}
        item{Row(horizontalArrangement=Arrangement.spacedBy(12.dp)){Stat("Potencial mensal",s.monthlySaving.currency(),Modifier.weight(1f));Stat("Potência evitável","${s.extraKw.decimal()} kW",Modifier.weight(1f),Coral)}}
        item{Text("Estimativa simulada: ${s.hours.decimal()} h/dia × ${s.days} dias × ${s.tariff.currency()}/kWh. Ajuste as premissas na aba Ações.",color=Muted,style=MaterialTheme.typography.bodySmall)}
        item{SectionTitle("Pulso da operação","Valores ilustrativos de hoje")}
        item{Row(horizontalArrangement=Arrangement.spacedBy(12.dp)){Stat("Consumo acumulado","${total.decimal()} kWh",Modifier.weight(1f),Blue);Stat("Custo estimado",(total*s.tariff).currency(),Modifier.weight(1f),Amber)}}
        items(machines,key={it.id}){MachineRow(it){onMachine(it.id)}}
        item{OutlinedButton(onClick=onActions,modifier=Modifier.fillMaxWidth()){Text("Ver inspeções e plano de ação")}}
        item{Text("Modo educativo. A câmera posiciona objetos virtuais; não mede energia nem detecta falhas em equipamentos reais.",color=Muted,style=MaterialTheme.typography.bodySmall)}
    }
}
@Composable fun ScenarioPicker(selected:Scenario,onChange:(Scenario)->Unit){
    Column(verticalArrangement=Arrangement.spacedBy(9.dp)){
        Eyebrow("CENÁRIO DE TREINAMENTO")
        Row(Modifier.horizontalScroll(rememberScrollState()),horizontalArrangement=Arrangement.spacedBy(8.dp)){
            Scenario.entries.forEach{s->FilterChip(selected=selected==s,onClick={onChange(s)},label={Text(s.label)},leadingIcon={Icon(when(s){Scenario.NORMAL->Icons.Default.CheckCircle;Scenario.LEAK->Icons.Default.Air;Scenario.IDLE->Icons.Default.Schedule},null,Modifier.size(18.dp))})}
        }
    }
}
@Composable private fun MachineRow(m:Machine,onClick:()->Unit){
    Card(onClick=onClick,modifier=Modifier.fillMaxWidth(),colors=CardDefaults.cardColors(containerColor=Panel)){
        Row(Modifier.padding(18.dp),verticalAlignment=Alignment.CenterVertically){
            Box(Modifier.size(46.dp).background(m.status.color().copy(alpha=.1f),RoundedCornerShape(14.dp)),contentAlignment=Alignment.Center){Icon(Icons.Default.PrecisionManufacturing,null,tint=m.status.color())}
            Spacer(Modifier.width(14.dp));Column(Modifier.weight(1f)){Text(m.name,fontWeight=FontWeight.Bold);Text(m.status.label,color=m.status.color(),style=MaterialTheme.typography.bodySmall)}
            Text("${m.powerKw.decimal()} kW",fontWeight=FontWeight.Bold)
        }
    }
}
@Composable private fun EquipmentList(machines:List<Machine>,modifier:Modifier,onClick:(String)->Unit){
    LazyColumn(modifier,contentPadding=PaddingValues(22.dp),verticalArrangement=Arrangement.spacedBy(16.dp)){
        item{SectionTitle("Seu parque industrial","3 equipamentos • dados simulados")}
        items(machines,key={it.id}){m->MachineRow(m){onClick(m.id)}}
        item{Text("O compressor possui um gêmeo 3D educativo com componentes interativos. Injetora e extrusora possuem acompanhamento de dados nesta versão.",color=Muted)}
    }
}

@Composable private fun Actions(s:InspectionState,modifier:Modifier,onResolve:(String)->Unit,onInspect:()->Unit,onHours:(Double)->Unit,onDays:(Int)->Unit,onTariff:(Double)->Unit){
    val context=LocalContext.current
    LazyColumn(modifier,contentPadding=PaddingValues(22.dp),verticalArrangement=Arrangement.spacedBy(20.dp)){
        item{SectionTitle("Da inspeção à ação","Registros salvos localmente no aparelho")}
        item{Row(horizontalArrangement=Arrangement.spacedBy(12.dp)){Stat("Inspeções","${s.records.size}",Modifier.weight(1f),Blue);Stat("Pendências","${s.records.count{!it.resolved}}",Modifier.weight(1f),Amber)}}
        item{Column(Modifier.background(Panel,RoundedCornerShape(24.dp)).padding(20.dp),verticalArrangement=Arrangement.spacedBy(12.dp)){
            Eyebrow("SIMULADOR DE ECONOMIA",Mint)
            Text(s.monthlySaving.currency(),style=MaterialTheme.typography.headlineLarge,fontWeight=FontWeight.Bold,color=Mint)
            Text("Potencial mensal para o cenário ${s.scenario.label.lowercase()}. Premissas ajustáveis; não representa economia medida.",color=Muted,style=MaterialTheme.typography.bodySmall)
            Text("Operação: ${s.hours.decimal()} horas/dia");Slider(value=s.hours.toFloat(),onValueChange={onHours(it.toDouble())},valueRange=1f..24f,steps=22)
            Text("Dias por mês: ${s.days}");Slider(value=s.days.toFloat(),onValueChange={onDays(it.toInt())},valueRange=1f..31f,steps=29)
            Text("Tarifa: ${s.tariff.currency()}/kWh");Slider(value=s.tariff.toFloat(),onValueChange={onTariff(it.toDouble())},valueRange=.1f..3f)
            Text("(${s.scenario.power.decimal()} − ${s.scenario.reference.decimal()}) kW × horas × dias × tarifa",color=Muted,style=MaterialTheme.typography.labelSmall)
        }}
        if(s.records.isEmpty())item{Column(verticalArrangement=Arrangement.spacedBy(12.dp)){Icon(Icons.Default.FactCheck,null,tint=Mint,modifier=Modifier.size(40.dp));Text("Sua primeira inspeção começa com uma pergunta: onde a energia está escapando?",color=Muted);Button(onClick=onInspect){Text("Explorar compressor em 3D")}}}
        items(s.records,key={it.id}){r->Column(Modifier.fillMaxWidth().background(Panel,RoundedCornerShape(24.dp)).padding(20.dp),verticalArrangement=Arrangement.spacedBy(10.dp)){
            Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween){Eyebrow(SimpleDateFormat("dd/MM • HH:mm",Locale.forLanguageTag("pt-BR")).format(Date(r.timestamp)));StatusPill(if(r.resolved)"Concluída" else "Ação pendente",if(r.resolved)Mint else Amber)}
            Text("Compressor 01 • ${r.scenario.label}",style=MaterialTheme.typography.titleMedium,fontWeight=FontWeight.Bold)
            Text(r.scenario.finding(),color=Muted)
            Text("${r.observedKw.decimal()} kW observados • ${r.projectedMonthlySaving.currency()}/mês de potencial",color=Mint)
            Text("Premissas: ${r.hours.decimal()} h × ${r.days} dias × ${r.tariff.currency()}/kWh",style=MaterialTheme.typography.labelSmall,color=Muted)
            if(!r.resolved)Button(onClick={onResolve(r.id)},modifier=Modifier.fillMaxWidth()){Text("Simular correção e concluir ação")}
            TextButton(onClick={
                val report="EnergyAR — Relatório educativo\nCompressor 01 | ${r.scenario.label}\n${r.scenario.finding()}\nPotência observada: ${r.observedKw.decimal()} kW\nPotencial mensal: ${r.projectedMonthlySaving.currency()}\nPremissas: ${r.hours.decimal()} h/dia, ${r.days} dias, ${r.tariff.currency()}/kWh\nEstado: ${if(r.resolved)"concluído" else "pendente"}\nTodos os dados e achados são simulados."
                context.startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).apply{type="text/plain";putExtra(Intent.EXTRA_TEXT,report)},"Compartilhar inspeção"))
            }){Icon(Icons.Default.Share,null,Modifier.size(16.dp));Spacer(Modifier.width(8.dp));Text("Compartilhar relatório")}
        }}
    }
}
