package com.dlebox.android

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.SubcomposeAsyncImage

private data class ArtStyle(val color: Color, val icon: ImageVector, val family: Int)
private fun style(category: String): ArtStyle = when(category) {
    "Geografía" -> ArtStyle(Color(0xFF487F91), Icons.Outlined.Public, 0)
    "Cine y series" -> ArtStyle(Color(0xFF9A674C), Icons.Outlined.Movie, 1)
    "Música" -> ArtStyle(Color(0xFF85648F), Icons.Outlined.MusicNote, 2)
    "Matemáticas" -> ArtStyle(Color(0xFF487D74), Icons.Outlined.Functions, 3)
    "Historia" -> ArtStyle(Color(0xFF987747), Icons.Outlined.HourglassBottom, 4)
    "Deportes" -> ArtStyle(Color(0xFF5E8250), Icons.Outlined.SportsSoccer, 5)
    "Videojuegos" -> ArtStyle(Color(0xFF6574A3), Icons.Outlined.SportsEsports, 6)
    "Ciencia y naturaleza" -> ArtStyle(Color(0xFF5C876D), Icons.Outlined.Science, 0)
    "Mesa y cartas" -> ArtStyle(Color(0xFF9B656A), Icons.Outlined.Casino, 5)
    "Visual y estimación" -> ArtStyle(Color(0xFF9C7A46), Icons.Outlined.Palette, 2)
    "Habilidad" -> ArtStyle(Color(0xFF9D715C), Icons.Outlined.Flag, 6)
    "Cultura general" -> ArtStyle(Color(0xFF71819A), Icons.Outlined.Psychology, 4)
    "Conexiones" -> ArtStyle(Color(0xFF8B789B), Icons.Outlined.Link, 2)
    "Crucigramas" -> ArtStyle(Color(0xFF697C91), Icons.Outlined.GridOn, 7)
    "Letras" -> ArtStyle(Color(0xFF8D855A), Icons.Outlined.Abc, 7)
    "Colecciones" -> ArtStyle(Color(0xFF6F8080), Icons.Outlined.Apps, 6)
    "Lógica", "Misterio" -> ArtStyle(Color(0xFF6F778D), Icons.Outlined.Extension, 3)
    else -> ArtStyle(Color(0xFF66845D), Icons.Outlined.Spellcheck, 7)
}

@Composable fun Thumbnail(game: Game, modifier: Modifier = Modifier) {
    if(game.thumbnailUrl != null) SubcomposeAsyncImage(model = game.thumbnailUrl, contentDescription = "Miniatura de ${game.name}", contentScale = ContentScale.Crop, modifier = modifier,
        loading = { GameGraphic(game, Modifier.fillMaxSize()) }, error = { GameGraphic(game, Modifier.fillMaxSize()) })
    else GameGraphic(game, modifier)
}

/** Arte vectorial original. No replica capturas, logotipos ni recursos de los juegos. */
@Composable private fun GameGraphic(game: Game, modifier: Modifier) {
    val art = style(game.category)
    val seed = game.id.hashCode().toLong().and(0x7fffffff).toInt()
    val dark = MaterialTheme.colorScheme.background.luminance() < .4f
    val paper = if(dark) Color(0xFF202622) else Color(0xFFF5F4EE)
    val ink = if(dark) Color(0xFFE0E8DE) else Color(0xFF26352E)
    val tint = art.color.copy(alpha = if(dark) .38f else .15f)
    val initials = game.name.split(Regex("[^\\p{L}\\p{N}]+")).filter { it.isNotBlank() }.let {
        if(it.size > 1) it.take(2).joinToString("") { word -> word.take(1) } else game.name.take(2)
    }.uppercase(java.util.Locale.ROOT)
    Box(modifier.background(paper).semantics { contentDescription = "Ilustración original de ${game.name}, no es una captura" }) {
        Canvas(Modifier.fillMaxSize()) {
            val w=size.width; val h=size.height
            val shift=(seed % 5)*w*.025f
            when(art.family) {
                0 -> { // globo / órbitas y coordenadas
                    val center=Offset(w*.68f-shift,h*.55f)
                    drawCircle(tint,h*.62f,center)
                    repeat(3) { i -> drawOval(art.color.copy(alpha=.25f), Offset(center.x-h*(.18f+i*.1f),center.y-h*.56f), Size(h*(.36f+i*.2f),h*1.12f), style=Stroke(2.dp.toPx())) }
                    drawLine(art.color.copy(alpha=.25f),Offset(w*.18f,h*.55f),Offset(w,h*.55f),2.dp.toPx())
                }
                1 -> { // fotogramas
                    drawRect(tint,Offset(w*.39f,0f),Size(w*.61f,h))
                    repeat(6) { i -> drawRect(art.color.copy(alpha=.32f),Offset(w*.45f+i*w*.105f,h*.1f),Size(w*.055f,h*.09f));drawRect(art.color.copy(alpha=.32f),Offset(w*.45f+i*w*.105f,h*.82f),Size(w*.055f,h*.09f)) }
                }
                2 -> { // ondas / conexiones
                    repeat(9) { i -> val x=w*(.32f+i*.085f);val height=h*(.2f+((seed shr (i%12)) and 7)*.085f);drawLine(tint,Offset(x,(h-height)/2),Offset(x,(h+height)/2),9.dp.toPx()) }
                }
                3 -> { // estructura lógica / ecuaciones
                    repeat(4) { i -> repeat(3) { j ->
                        val x=w*.4f+i*w*.16f;val y=h*.14f+j*h*.3f
                        if((seed+i+j)%3==0) drawCircle(tint,h*.095f,Offset(x,y)) else drawRect(tint,Offset(x-h*.08f,y-h*.08f),Size(h*.16f,h*.16f))
                    } }
                }
                4 -> { // línea temporal
                    drawLine(tint,Offset(w*.38f,h*.2f),Offset(w*.95f,h*.8f),12.dp.toPx())
                    repeat(5) { i -> drawCircle(art.color.copy(alpha=.3f),7.dp.toPx(),Offset(w*(.4f+i*.13f),h*(.22f+i*.14f))) }
                }
                5 -> { // campo / tablero
                    drawRoundRect(tint,Offset(w*.4f,h*.12f),Size(w*.65f,h*.76f),androidx.compose.ui.geometry.CornerRadius(10.dp.toPx()))
                    drawCircle(art.color.copy(alpha=.3f),h*.26f,Offset(w*.72f,h*.5f),style=Stroke(2.dp.toPx()))
                    drawLine(art.color.copy(alpha=.3f),Offset(w*.72f,h*.12f),Offset(w*.72f,h*.88f),2.dp.toPx())
                }
                6 -> { // recorrido / píxeles
                    repeat(12) { i -> val x=w*(.35f+(i%4)*.18f);val y=h*(.14f+(i/4)*.3f);if((seed+i)%3!=0)drawRect(tint,Offset(x,y),Size(h*.2f,h*.2f)) }
                }
                else -> { // letras y crucigramas
                    repeat(4) { i -> repeat(3) { j ->
                        val x=w*(.42f+i*.15f);val y=h*(.12f+j*.29f)
                        drawRoundRect(if((seed shr ((i+j)%16)) and 1==1) tint else art.color.copy(alpha=.055f),Offset(x,y),Size(h*.24f,h*.24f),androidx.compose.ui.geometry.CornerRadius(3.dp.toPx()))
                    } }
                }
            }
            drawRect(art.color,Offset(0f,0f),Size(4.dp.toPx(),h))
        }
        Icon(art.icon, null, tint=art.color.copy(alpha=.72f), modifier=Modifier.align(Alignment.CenterEnd).padding(end=((seed%3)*5+14).dp).size(54.dp))
        Text(initials, color=ink, fontWeight=FontWeight.ExtraBold, fontSize=34.sp, modifier=Modifier.align(Alignment.CenterStart).padding(start=18.dp))
    }
}
