package br.com.linear.energyar.ui
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import br.com.linear.energyar.data.*
val Mint=Color(0xFF7BF0B7)
val Ink=Color(0xFF0C1319)
val Panel=Color(0xFF17232D)
val Muted=Color(0xFF9AADB9)
val Amber=Color(0xFFFFCA7A)
val Coral=Color(0xFFFF8A7B)
val Blue=Color(0xFF7CCFFF)
fun MachineStatus.color()=when(this){MachineStatus.NORMAL->Mint;MachineStatus.WARNING->Amber;MachineStatus.CRITICAL->Coral}
@Composable fun Eyebrow(text: String, color: Color=Muted) { Text(text,style=MaterialTheme.typography.labelSmall,letterSpacing=1.8.sp,color=color,fontWeight=FontWeight.Bold) }
@Composable fun SectionTitle(title:String,subtitle:String?=null) { Column(verticalArrangement=Arrangement.spacedBy(5.dp)){Text(title,style=MaterialTheme.typography.titleLarge,fontWeight=FontWeight.Bold);subtitle?.let{Text(it,color=Muted,style=MaterialTheme.typography.bodySmall)}} }
@Composable fun Stat(label:String,value:String,modifier:Modifier=Modifier,color:Color=Mint) {
    Column(modifier.background(Panel,RoundedCornerShape(20.dp)).padding(16.dp),verticalArrangement=Arrangement.spacedBy(7.dp)) {
        Text(label,color=Muted,style=MaterialTheme.typography.labelMedium)
        Text(value,color=color,style=MaterialTheme.typography.titleLarge,fontWeight=FontWeight.Bold)
    }
}
@Composable fun StatusPill(text:String,color:Color=Mint) {
    Row(Modifier.background(color.copy(alpha=.12f),RoundedCornerShape(100.dp)).padding(horizontal=10.dp,vertical=6.dp),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(6.dp)) {
        Box(Modifier.size(5.dp).background(color,RoundedCornerShape(50)))
        Text(text,color=color,style=MaterialTheme.typography.labelSmall,fontWeight=FontWeight.Bold)
    }
}
@Composable fun PowerChart(values:List<Double>,reference:Double,modifier:Modifier=Modifier) {
    Canvas(modifier.fillMaxWidth().height(110.dp)) {
        val max=(values.maxOrNull()?:1.0).coerceAtLeast(reference)*1.25
        for(i in 1..3)drawLine(Color(0xFF2A3945),Offset(0f,size.height*i/4),Offset(size.width,size.height*i/4))
        val y=size.height-(reference/max*size.height).toFloat()
        drawLine(Amber.copy(alpha=.7f),Offset(0f,y),Offset(size.width,y),strokeWidth=2f,pathEffect=androidx.compose.ui.graphics.PathEffect.dashPathEffect(floatArrayOf(8f,8f)))
        val points=values.mapIndexed{i,v->Offset(i*size.width/(values.size-1).coerceAtLeast(1),size.height-(v/max*size.height).toFloat())}
        val line=Path().apply{points.forEachIndexed{i,p->if(i==0)moveTo(p.x,p.y)else lineTo(p.x,p.y)}}
        val area=Path().apply{moveTo(0f,size.height);points.forEach{lineTo(it.x,it.y)};lineTo(size.width,size.height);close()}
        drawPath(area,androidx.compose.ui.graphics.Brush.verticalGradient(listOf(Mint.copy(alpha=.22f),Color.Transparent)))
        drawPath(line,Mint,style=androidx.compose.ui.graphics.drawscope.Stroke(4f))
        points.lastOrNull()?.let{drawCircle(Mint,5f,it)}
    }
}
