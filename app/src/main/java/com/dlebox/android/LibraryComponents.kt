package com.dlebox.android

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable fun GameCard(game: Game, favorite: Boolean, play: () -> Unit, toggle: () -> Unit, detail: () -> Unit) {
    val colors=MaterialTheme.colorScheme
    Card(onClick=play, shape=RoundedCornerShape(12.dp), border=BorderStroke(1.dp,colors.outlineVariant.copy(alpha=.55f)), colors=CardDefaults.cardColors(containerColor=colors.background), modifier=Modifier.testTag("game-${game.id}")) {
        Box {
            Thumbnail(game,Modifier.fillMaxWidth().aspectRatio(1.6f))
            IconButton(onClick=detail,modifier=Modifier.align(Alignment.TopEnd).size(48.dp)) {
                Icon(Icons.Outlined.Info,"Información de ${game.name}",Modifier.size(18.dp),tint=colors.onSurfaceVariant)
            }
        }
        Row(Modifier.fillMaxWidth().padding(start=12.dp,top=5.dp,bottom=7.dp,end=2.dp),verticalAlignment=Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(game.name,fontWeight=FontWeight.SemiBold,style=MaterialTheme.typography.titleSmall,maxLines=2,overflow=TextOverflow.Ellipsis)
                Text(game.category,style=MaterialTheme.typography.labelSmall,color=colors.onSurfaceVariant,maxLines=1,overflow=TextOverflow.Ellipsis)
            }
            IconButton(onClick=toggle,modifier=Modifier.size(48.dp).semantics { stateDescription=if(favorite) "Guardado" else "Sin guardar" }) {
                Icon(if(favorite) Icons.Outlined.Favorite else Icons.Outlined.FavoriteBorder,"Favorito: ${game.name}",Modifier.size(21.dp),tint=if(favorite)colors.primary else colors.onSurfaceVariant)
            }
        }
    }
}

fun relativeOpened(at: Long, now: Long = System.currentTimeMillis()): String {
    val minutes=((now-at).coerceAtLeast(0))/60_000
    return when {
        minutes < 1 -> "Ahora mismo"
        minutes < 60 -> "Hace $minutes min"
        minutes < 1440 -> "Hace ${minutes/60} h"
        minutes < 2880 -> "Ayer"
        minutes < 10080 -> "Hace ${minutes/1440} días"
        else -> Instant.ofEpochMilli(at).atZone(ZoneId.systemDefault()).format(DateTimeFormatter.ofPattern("d MMM", Locale.forLanguageTag("es")))
    }
}

@Composable fun RecentRow(game: Game, at: Long, play: () -> Unit, remove: () -> Unit, detail: () -> Unit) {
    Row(Modifier.fillMaxWidth().clickable(onClick=play).testTag("recent-${game.id}").padding(start=20.dp,end=8.dp,top=8.dp,bottom=8.dp),verticalAlignment=Alignment.CenterVertically) {
        Box(Modifier.size(64.dp).clip(RoundedCornerShape(9.dp)).clickable(onClick=detail)) { Thumbnail(game,Modifier.fillMaxSize()) }
        Column(Modifier.weight(1f).padding(horizontal=12.dp)) {
            Text(game.name,style=MaterialTheme.typography.titleSmall,fontWeight=FontWeight.SemiBold,maxLines=1,overflow=TextOverflow.Ellipsis)
            Text(game.category,style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant,maxLines=1,overflow=TextOverflow.Ellipsis)
            Text(relativeOpened(at),style=MaterialTheme.typography.labelSmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
        }
        IconButton(onClick=remove) { Icon(Icons.Outlined.Close,"Eliminar ${game.name} del historial",Modifier.size(20.dp)) }
    }
}

@Composable fun EmptyLibrary(icon: ImageVector, title: String, text: String, action: String, onAction: () -> Unit, modifier: Modifier = Modifier) {
    Column(modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(36.dp),verticalArrangement=Arrangement.Center,horizontalAlignment=Alignment.CenterHorizontally) {
        Icon(icon,null,Modifier.size(42.dp),tint=MaterialTheme.colorScheme.primary)
        Spacer(Modifier.height(16.dp))
        Text(title,style=MaterialTheme.typography.titleMedium,fontWeight=FontWeight.SemiBold)
        Spacer(Modifier.height(8.dp))
        Text(text,style=MaterialTheme.typography.bodyMedium,color=MaterialTheme.colorScheme.onSurfaceVariant,textAlign=androidx.compose.ui.text.style.TextAlign.Center)
        Spacer(Modifier.height(16.dp))
        OutlinedButton(onClick=onAction) { Text(action) }
    }
}
