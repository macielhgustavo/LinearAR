package br.com.linear.energyar.ar

import androidx.compose.ui.graphics.Color
import com.google.android.filament.Engine
import io.github.sceneview.loaders.MaterialLoader
import io.github.sceneview.loaders.ModelLoader
import io.github.sceneview.math.Position
import io.github.sceneview.math.Scale
import io.github.sceneview.math.Rotation
import io.github.sceneview.node.Node
import io.github.sceneview.node.ModelNode
import io.github.sceneview.node.SphereNode
import br.com.linear.energyar.data.*
import kotlin.math.sin

class CompressorRig(engine:Engine, modelLoader:ModelLoader, materialLoader:MaterialLoader) {
    val root=Node(engine)
    val parts=linkedMapOf<ComponentId,ModelNode>()
    val markers=linkedMapOf<ComponentId,Node>()
    private val circuit=ModelNode(modelLoader.createModelInstance("models/circuit.glb"))
    private val flow=mutableListOf<Node>()
    private val leakage=mutableListOf<Node>()
    private val origins=mapOf(ComponentId.TANK to Position(0f,.43f,.32f),ComponentId.MOTOR to Position(.1f,1.19f,0f),ComponentId.VALVE to Position(.75f,.69f,0f),ComponentId.GAUGE to Position(.4f,.9f,.24f))
    private val exploded=mapOf(ComponentId.TANK to Position(0f,0f,0f),ComponentId.MOTOR to Position(-.15f,.38f,-.2f),ComponentId.VALVE to Position(.35f,.07f,0f),ComponentId.GAUGE to Position(.12f,.23f,.25f))
    init {
        val marker=materialLoader.createColorInstance(Color(0xFF7BF0B7))
        val blue=materialLoader.createColorInstance(Color(0xFF7CCFFF))
        val red=materialLoader.createColorInstance(Color(0xFFFF786A))
        ComponentId.entries.forEach{id->
            val model=ModelNode(modelLoader.createModelInstance("models/${id.asset}.glb"))
            model.name=id.name;parts[id]=model;root.addChildNode(model)
            val point=SphereNode(engine,radius=.025f,stacks=8,slices=12,materialInstance=marker).apply{position=origins.getValue(id);name=id.name}
            markers[id]=point;root.addChildNode(point)
        }
        root.addChildNode(circuit)
        repeat(16){val p=SphereNode(engine,radius=.013f,stacks=6,slices=8,materialInstance=blue);flow+=p;root.addChildNode(p)}
        repeat(9){val p=SphereNode(engine,radius=.015f,stacks=6,slices=8,materialInstance=red);leakage+=p;root.addChildNode(p)}
    }
    fun picked(node:Node?):ComponentId?=generateSequence(node){it.parent}.mapNotNull { n -> parts.entries.firstOrNull{it.value===n}?.key ?: markers.entries.firstOrNull{it.value===n}?.key }.firstOrNull()
    fun animate(seconds:Float,mode:VisualMode,scenario:Scenario,selected:ComponentId?,scale:Float,rotation:Float) {
        root.scale=Scale(scale);root.rotation=Rotation(0f,rotation,0f)
        parts.forEach{(id,node)->
            val target=if(mode==VisualMode.EXPLODED)exploded.getValue(id)else Position(0f)
            node.position=node.position+(target-node.position)*.12f
            node.isVisible=!(mode==VisualMode.XRAY && id==ComponentId.TANK)
            markers.getValue(id).position=origins.getValue(id)+node.position
            val pulse=if(id==selected)1.3f+.2f*sin(seconds*4f)else 1f
            markers.getValue(id).scale=Scale(pulse)
        }
        circuit.isVisible=mode==VisualMode.XRAY
        flow.forEachIndexed{i,node->
            node.isVisible=mode==VisualMode.XRAY
            val t=(seconds*(if(scenario==Scenario.IDLE).07f else .22f)+i/16f)%1f
            node.position=when {
                t<.3f->Position(.1f,.95f-(t/.3f)*.57f,0f)
                t<.7f->Position(.1f+(t-.3f)/.4f*.4f,.38f,0f)
                else->Position(.5f+(t-.7f)/.3f*.27f,.38f+(t-.7f)/.3f*.24f,0f)
            }
        }
        leakage.forEachIndexed{i,node->
            node.isVisible=scenario==Scenario.LEAK
            val t=(seconds*.65f+i/9f)%1f
            node.position=Position(.8f+t*.32f,.62f+sin(i*2f+seconds)*.07f*t,sin(i*1.3f)*.07f*t)+parts.getValue(ComponentId.VALVE).position
            node.scale=Scale(.4f+t*.9f)
        }
    }
}
