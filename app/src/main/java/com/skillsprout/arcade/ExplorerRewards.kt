package com.skillsprout.arcade

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

enum class Decoration(val stars:Int,val symbol:String,val label:Int) {
    SCARF(0,"❧",R.string.decoration_scarf), SUNFLOWER(5,"✿",R.string.decoration_sunflower),
    STAR(15,"★",R.string.decoration_star), COMPASS(30,"✥",R.string.decoration_compass);
    val id get()=name.lowercase()
    companion object {
        fun available(id:String,stars:Int)=entries.firstOrNull {it.id==id && stars>=it.stars} ?: SCARF
    }
}

@Composable fun ExplorerAvatar(child:Child,modifier:Modifier=Modifier) {
    val names=listOf(R.string.avatar_leaf,R.string.avatar_sun,R.string.avatar_star,R.string.avatar_wave)
    val index=child.avatar.coerceIn(0,3)
    val label=stringResource(names[index])
    Text(listOf("❧","☀","★","≋")[index],modifier.background(GameColors[index],CircleShape).padding(12.dp).semantics {contentDescription=label},
        color=Forest,fontSize=24.sp,fontWeight=FontWeight.Bold)
}

@Composable fun ExplorerPip(child:Child,stars:Int,modifier:Modifier=Modifier) {
    val decoration=Decoration.available(child.decoration,stars)
    Box(modifier) {
        Pip(Modifier.fillMaxSize())
        if(decoration!=Decoration.SCARF) {
            val label=stringResource(decoration.label)
            Text(decoration.symbol,Modifier.align(Alignment.BottomEnd).background(Peach,CircleShape).padding(12.dp).semantics {contentDescription=label},
                color=Forest,fontSize=30.sp)
        }
    }
}
