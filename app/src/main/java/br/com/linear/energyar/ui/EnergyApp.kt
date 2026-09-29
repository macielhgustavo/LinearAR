package br.com.linear.energyar.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import br.com.linear.energyar.EnergyViewModel
import br.com.linear.energyar.ar.ARScreen
import br.com.linear.energyar.data.*

private val Mint = Color(0xFF76E8B4)
private val Ink = Color(0xFF101820)
private val Panel = Color(0xFF1C2934)
private fun MachineStatus.color() = when(this) {
    MachineStatus.NORMAL -> Mint
    MachineStatus.WARNING -> Color(0xFFFFD17D)
    MachineStatus.CRITICAL -> Color(0xFFFF8E8E)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EnergyApp(vm: EnergyViewModel = viewModel()) {
    val anomaly by vm.anomaly.collectAsStateWithLifecycle()
    val machines = remember(anomaly) { vm.machines(anomaly) }
    var tab by rememberSaveable { mutableIntStateOf(0) }
    var selected by rememberSaveable { mutableStateOf<String?>(null) }
    val machine = machines.firstOrNull { it.id == selected }
    val titles = listOf("Visão geral", "Máquinas", "Realidade aumentada", "Alertas")
    MaterialTheme(colorScheme = darkColorScheme(primary = Mint, background = Ink, surface = Panel)) {
        BackHandler(enabled = selected != null || tab != 0) { if(selected != null) selected = null else tab = 0 }
        Scaffold(
            topBar = { TopAppBar(title = { Column {
                Text("ENERGYAR", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Text(machine?.name ?: titles[tab], style = MaterialTheme.typography.labelMedium, color = Mint)
            } }, navigationIcon = { if(machine != null) IconButton(onClick = { selected = null }) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, "Voltar")
            } }) },
            bottomBar = { NavigationBar {
                val icons = listOf(Icons.Default.Dashboard, Icons.Default.PrecisionManufacturing, Icons.Default.ViewInAr, Icons.Default.Notifications)
                listOf("Início", "Máquinas", "AR", "Alertas").forEachIndexed { index, label ->
                    NavigationBarItem(selected = tab == index && selected == null,
                        onClick = { tab = index; selected = null }, icon = { Icon(icons[index], label) }, label = { Text(label) })
                }
            } }
        ) { padding ->
            val mod = Modifier.fillMaxSize().padding(padding)
            if(machine != null) MachineDetail(machine, mod, onAR = { selected = null; tab = 2 })
            else when(tab) {
                0 -> Dashboard(machines, anomaly, vm::setAnomaly, mod, { selected = it }, { tab = 2 })
                1 -> LazyColumn(mod, contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    item { SimulationLabel() }
                    items(machines, key = { it.id }) { MachineCard(it) { selected = it.id } }
                }
                2 -> ARScreen(machines.first(), anomaly, vm::setAnomaly, mod)
                3 -> LazyColumn(mod, contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    item { SimulationLabel() }
                    val alerts = machines.filter { it.status != MachineStatus.NORMAL }
                    if(alerts.isEmpty()) item { Text("Nenhum alerta ativo neste cenário.") }
                    items(alerts, key = { it.id }) { m -> Card(onClick = { selected = m.id }) {
                        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(m.name, fontWeight = FontWeight.Bold)
                            Text("${m.deviationPercent.decimal()}% acima da referência", color = m.status.color())
                            Text(m.recommendation)
                        }
                    } }
                }
            }
        }
    }
}

@Composable
private fun SimulationLabel() {
    Text("DEMONSTRAÇÃO • DADOS SIMULADOS", style = MaterialTheme.typography.labelSmall, color = Mint)
}

@Composable
private fun Dashboard(machines: List<Machine>, anomaly: Boolean, onAnomaly: (Boolean) -> Unit, modifier: Modifier, onMachine: (String) -> Unit, onAR: () -> Unit) {
    val total = machines.sumOf { it.consumptionKwh }
    LazyColumn(modifier, contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(18.dp)) {
        item { SimulationLabel() }
        item { Column {
            Text("Energia sob controle", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
            Text("Explore a operação e visualize o equipamento no seu ambiente.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        } }
        item { Card(Modifier.fillMaxWidth()) { Column(Modifier.padding(22.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Consumo acumulado hoje")
            Text("${total.decimal()} kWh", style = MaterialTheme.typography.headlineLarge, color = Mint, fontWeight = FontWeight.Bold)
            Text("Custo estimado: ${(total * EnergyRepository.tariff).currency()}")
            Text("Tarifa de demonstração: R$ 0,85/kWh", style = MaterialTheme.typography.labelSmall)
        } } }
        item { Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Metric("Potência total", "${machines.sumOf { it.powerKw }.decimal()} kW", Modifier.weight(1f))
            Metric("Alertas ativos", "${machines.count { it.status != MachineStatus.NORMAL }}", Modifier.weight(1f))
        } }
        item { ScenarioSwitch(anomaly, onAnomaly) }
        item { FilledTonalButton(onClick = onAR, modifier = Modifier.fillMaxWidth()) {
            Icon(Icons.Default.ViewInAr, null); Spacer(Modifier.width(10.dp)); Text("Visualizar compressor em AR")
        } }
        item { Text("Equipamentos", style = MaterialTheme.typography.titleLarge) }
        items(machines, key = { it.id }) { MachineCard(it) { onMachine(it.id) } }
    }
}

@Composable
private fun Metric(label: String, value: String, modifier: Modifier) {
    Card(modifier) { Column(Modifier.padding(16.dp)) {
        Text(label, style = MaterialTheme.typography.labelMedium)
        Text(value, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
    } }
}

@Composable
fun ScenarioSwitch(anomaly: Boolean, onAnomaly: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text("Simular consumo elevado", fontWeight = FontWeight.Medium)
            Text("Altera a potência do compressor", style = MaterialTheme.typography.bodySmall)
        }
        Switch(checked = anomaly, onCheckedChange = onAnomaly)
    }
}

@Composable
private fun MachineCard(machine: Machine, onClick: () -> Unit) {
    Card(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
        Row(Modifier.padding(18.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.PrecisionManufacturing, null, tint = machine.status.color())
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(machine.name, fontWeight = FontWeight.Bold)
                Text(machine.status.label, color = machine.status.color(), style = MaterialTheme.typography.bodySmall)
            }
            Text("${machine.powerKw.decimal()} kW", fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun MachineDetail(machine: Machine, modifier: Modifier, onAR: () -> Unit) {
    LazyColumn(modifier, contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(18.dp)) {
        item { SimulationLabel() }
        item { Text(machine.type, style = MaterialTheme.typography.headlineSmall) }
        item { Text(machine.status.label, color = machine.status.color()) }
        item { Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Metric("Potência instantânea", "${machine.powerKw.decimal()} kW", Modifier.weight(1f))
            Metric("Consumo hoje", "${machine.consumptionKwh.decimal()} kWh", Modifier.weight(1f))
        } }
        item { Text("Referência: ${machine.referenceKw.decimal()} kW • Variação: ${machine.deviationPercent.decimal()}%") }
        item { Card { Column(Modifier.padding(18.dp)) {
            Text("Histórico simulado de potência", fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(14.dp))
            PowerChart(machine.history)
            Text("Últimas 6 amostras • kW", style = MaterialTheme.typography.labelSmall)
        } } }
        item { Text("Orientação", style = MaterialTheme.typography.titleMedium); Text(machine.recommendation) }
        if(machine.id == "compressor") item { Button(onClick = onAR, modifier = Modifier.fillMaxWidth()) { Text("Abrir compressor em AR") } }
    }
}

@Composable
private fun PowerChart(values: List<Double>) {
    Canvas(Modifier.fillMaxWidth().height(130.dp)) {
        val max = (values.maxOrNull() ?: 1.0) * 1.15
        for(i in 1..3) drawLine(Color(0xFF3A4955), Offset(0f, size.height*i/4), Offset(size.width, size.height*i/4))
        val points = values.mapIndexed { i,v -> Offset(i*size.width/(values.size-1), size.height-(v/max*size.height).toFloat()) }
        val path = Path().apply { points.forEachIndexed { i,p -> if(i==0) moveTo(p.x,p.y) else lineTo(p.x,p.y) } }
        drawPath(path, Mint, style = androidx.compose.ui.graphics.drawscope.Stroke(width = 5f))
        points.forEach { drawCircle(Mint, 5f, it) }
    }
}
