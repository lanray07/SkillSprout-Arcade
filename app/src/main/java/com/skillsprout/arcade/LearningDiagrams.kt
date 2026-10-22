package com.skillsprout.arcade

import android.graphics.Paint
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

enum class AnswerShape { TRIANGLE, CIRCLE, SQUARE }

// Resolve from the option value, never its display position: options are shuffled in GameScreen.
fun answerShape(visual:String,option:String):AnswerShape? = if(visual!="choice-shapes") null else when(option) {
    "Triangle"->AnswerShape.TRIANGLE; "Circle"->AnswerShape.CIRCLE; "Square"->AnswerShape.SQUARE; else->null
}

data class ClockDiagram(val hour:Int,val minute:Int)
fun clockDiagram(visual:String):ClockDiagram? {
    val match=Regex("clock:([0-9]{1,2}):([0-9]{2})").matchEntire(visual) ?: return null
    val hour=match.groupValues[1].toInt();val minute=match.groupValues[2].toInt()
    return if(hour in 1..12 && minute in 0..59) ClockDiagram(hour,minute) else null
}

data class FruitDiagram(val apples:Int,val pears:Int)
fun fruitDiagram(visual:String):FruitDiagram? {
    val match=Regex("fruit:([0-9]{1,2}):([0-9]{1,2})").matchEntire(visual) ?: return null
    val apples=match.groupValues[1].toInt();val pears=match.groupValues[2].toInt()
    return if(apples in 1..10 && pears in 1..10) FruitDiagram(apples,pears) else null
}

@Composable private fun FruitSymbol(apple:Boolean) {
    Canvas(Modifier.size(30.dp).clearAndSetSemantics {}) {
        scale(size.width/32f,size.height/32f,pivot=Offset.Zero) {
            val fruit=Path().apply {
                if(apple) {
                    moveTo(16f,10f);cubicTo(5f,3f,0f,17f,9f,28f)
                    cubicTo(12f,31f,14f,28f,16f,28f);cubicTo(19f,28f,21f,31f,24f,27f)
                    cubicTo(32f,16f,27f,3f,16f,10f)
                } else {
                    moveTo(16f,7f);cubicTo(10f,7f,13f,14f,7f,19f)
                    cubicTo(-1f,30f,31f,33f,25f,20f);cubicTo(19f,14f,22f,7f,16f,7f)
                }
                close()
            }
            drawPath(fruit,if(apple) Color(0xFFCC4939) else Color(0xFFB9D56C))
            drawPath(fruit,Forest,style=Stroke(1.3f))
            drawLine(Forest,Offset(16f,9f),Offset(18f,2f),2f,StrokeCap.Round)
            drawOval(Forest,Offset(19f,3f),Size(7f,3f))
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable private fun FruitPictogram(fruit:FruitDiagram) {
    val description=stringResource(R.string.diagram_fruit_description,fruit.apples,fruit.pears)
    Column(Modifier.fillMaxWidth().padding(vertical=12.dp).testTag("learning-fruit").semantics(mergeDescendants=true) {contentDescription=description},verticalArrangement=Arrangement.spacedBy(8.dp)) {
        for(apple in listOf(true,false)) {
            Text(stringResource(if(apple) R.string.diagram_apples else R.string.diagram_pears),modifier=Modifier.clearAndSetSemantics {})
            FlowRow(horizontalArrangement=Arrangement.spacedBy(6.dp),verticalArrangement=Arrangement.spacedBy(6.dp)) {
                repeat(if(apple) fruit.apples else fruit.pears) {FruitSymbol(apple)}
            }
        }
        Text(stringResource(R.string.diagram_fruit_key),modifier=Modifier.clearAndSetSemantics {})
    }
}

@Composable fun AnswerShapeDiagram(shape:AnswerShape) {
    // The button already speaks its answer label. Do not announce the decorative shape twice.
    Canvas(Modifier.size(52.dp).clearAndSetSemantics {}) {
        val side=min(size.width,size.height)*0.72f
        val left=(size.width-side)/2;val top=(size.height-side)/2
        val outline=Stroke(3.dp.toPx())
        when(shape) {
            AnswerShape.CIRCLE->drawCircle(Forest,side/2,center,style=outline)
            AnswerShape.SQUARE->drawRect(Forest,Offset(left,top),Size(side,side),style=outline)
            AnswerShape.TRIANGLE->drawPath(Path().apply {
                moveTo(size.width/2,top);lineTo(left+side,top+side);lineTo(left,top+side);close()
            },Forest,style=outline)
        }
    }
}

@Composable fun LearningDiagram(visual:String) {
    val fruit=fruitDiagram(visual)
    if(fruit!=null) {FruitPictogram(fruit);return}
    val clock=clockDiagram(visual)
    if(clock!=null) {
        val description=stringResource(R.string.diagram_clock_description,clock.hour,clock.minute)
        Canvas(Modifier.fillMaxWidth().height(220.dp).testTag("learning-clock").semantics {contentDescription=description}) {
            val radius=min(size.width,size.height)*0.45f
            drawCircle(Color.White,radius);drawCircle(Forest,radius,style=Stroke(3.dp.toPx()))
            val text=Paint(Paint.ANTI_ALIAS_FLAG).apply {color=android.graphics.Color.rgb(25,63,50);textSize=19.dp.toPx();textAlign=Paint.Align.CENTER}
            for(number in 1..12) {
                val angle=number*Math.PI/6-Math.PI/2
                val x=center.x+cos(angle).toFloat()*radius*0.8f
                val y=center.y+sin(angle).toFloat()*radius*0.8f-(text.ascent()+text.descent())/2
                drawContext.canvas.nativeCanvas.drawText(number.toString(),x,y,text)
            }
            fun hand(angle:Double,length:Float,width:Float) {
                drawLine(Forest,center,Offset(center.x+cos(angle).toFloat()*length,center.y+sin(angle).toFloat()*length),width,StrokeCap.Round)
            }
            hand((clock.hour%12+clock.minute/60.0)*Math.PI/6-Math.PI/2,radius*0.48f,7.dp.toPx())
            hand(clock.minute*Math.PI/30-Math.PI/2,radius*0.67f,4.dp.toPx())
            drawCircle(Forest,5.dp.toPx())
        }
    } else if(visual=="sandwich:equal-halves") {
        val description=stringResource(R.string.diagram_sandwich_description)
        Canvas(Modifier.fillMaxWidth().height(190.dp).testTag("learning-sandwich").semantics {contentDescription=description}) {
            val side=min(size.width,size.height)*0.74f
            val left=(size.width-side)/2;val top=(size.height-side)/2;val gap=4.dp.toPx()
            val pieces=listOf(
                Path().apply {moveTo(left-gap,top-gap);lineTo(left+side-gap,top-gap);lineTo(left-gap,top+side-gap);close()},
                Path().apply {moveTo(left+side+gap,top+gap);lineTo(left+side+gap,top+side+gap);lineTo(left+gap,top+side+gap);close()}
            )
            // Congruent right triangles: the split is visibly equal, with a clear gap.
            pieces.forEach {path->drawPath(path,Color(0xFFFFE6AD));drawPath(path,Color(0xFF80501F),style=Stroke(4.dp.toPx()))}
        }
    }
}
