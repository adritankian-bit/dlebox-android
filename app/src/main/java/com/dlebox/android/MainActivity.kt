package com.dlebox.android

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.browser.customtabs.CustomTabColorSchemeParams
import androidx.browser.customtabs.CustomTabsIntent
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.OpenInNew
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.SubcomposeAsyncImage
import java.text.DateFormat
import java.util.Date

private val Green = Color(0xFF6AAA64)
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) { super.onCreate(savedInstanceState); setContent { DleBox() } }
}
@OptIn(ExperimentalMaterial3Api::class)
@Composable fun DleBox(vm: LibraryViewModel = viewModel()) {
    val library by vm.saved.collectAsStateWithLifecycle()
    val games by vm.games.collectAsStateWithLifecycle()
    val updating by vm.refreshing.collectAsStateWithLifecycle()
    val status by vm.status.collectAsStateWithLifecycle()
    val storageError by vm.error.collectAsStateWithLifecycle()
    val dark = library.theme == "dark" || (library.theme == "auto" && isSystemInDarkTheme())
    val colors = if (dark) darkColorScheme(primary = Color(0xFFA1D29A), background = Color(0xFF171917), surface = Color(0xFF202320), secondaryContainer = Color(0xFF354B32))
        else lightColorScheme(primary = Color(0xFF396E34), background = Color.White, surface = Color(0xFFF6F6F6), secondaryContainer = Color(0xFFE5EFDF), onSurface = Color(0xFF222222))
    MaterialTheme(colorScheme = colors) {
        val context = LocalContext.current
        var tab by rememberSaveable { mutableIntStateOf(0) }
        var query by rememberSaveable { mutableStateOf("") }
        var category by rememberSaveable { mutableStateOf("Todos") }
        var settings by rememberSaveable { mutableStateOf(false) }
        var detailId by rememberSaveable { mutableStateOf<String?>(null) }
        var failed by remember { mutableStateOf<Game?>(null) }
        var browserError by remember { mutableStateOf<String?>(null) }
        val all = library.snapshots + games.associateBy { it.id }
        fun openUrl(url: String, external: Boolean = false): Boolean {
            return try {
                if (external) context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)).addCategory(Intent.CATEGORY_BROWSABLE))
                else try {
                    CustomTabsIntent.Builder().setShowTitle(true)
                        .setDefaultColorSchemeParams(CustomTabColorSchemeParams.Builder().setToolbarColor(colors.surface.toArgb()).build())
                        .setColorScheme(if (dark) CustomTabsIntent.COLOR_SCHEME_DARK else CustomTabsIntent.COLOR_SCHEME_LIGHT)
                        .build().launchUrl(context, Uri.parse(url))
                } catch (_: ActivityNotFoundException) {
                    context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)).addCategory(Intent.CATEGORY_BROWSABLE))
                }
                true
            } catch (_: Exception) { false }
        }
        fun play(game: Game, external: Boolean = false) {
            if (openUrl(game.gameUrl, external)) { vm.opened(game); failed = null } else failed = game
        }
        val scrolls = listOf(rememberLazyGridState(), rememberLazyGridState(), rememberLazyGridState())
        BackHandler(settings || detailId != null || tab != 0 || query.isNotEmpty() || category != "Todos") {
            when { detailId != null -> detailId = null; settings -> settings = false; tab != 0 -> tab = 0; query.isNotEmpty() -> query = ""; else -> category = "Todos" }
        }
        Scaffold(containerColor = colors.background, bottomBar = {
            NavigationBar(containerColor = colors.background, tonalElevation = 0.dp) {
                listOf("Explorar" to Icons.Outlined.GridView, "Favoritos" to Icons.Outlined.FavoriteBorder, "Recientes" to Icons.Outlined.History).forEachIndexed { i, pair ->
                    NavigationBarItem(selected = tab == i, onClick = { tab = i }, icon = { Icon(pair.second, null) }, label = { Text(pair.first) })
                }
            }
        }) { padding ->
            Column(Modifier.padding(padding).fillMaxSize()) {
                Row(Modifier.fillMaxWidth().padding(start = 20.dp, end = 8.dp, top = 20.dp), verticalAlignment = Alignment.CenterVertically) {
                    Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                        "DLE".forEachIndexed { i, c -> Box(Modifier.size(30.dp).background(if(i == 1) Color(0xFFC9B458) else Green, RoundedCornerShape(3.dp)), contentAlignment = Alignment.Center) { Text(c.toString(), color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp) } }
                    }
                    Text("Box", fontSize = 28.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(start = 3.dp).weight(1f))
                    IconButton(onClick = { settings = true }) { Icon(Icons.Outlined.Settings, "Ajustes") }
                }
                Text("Tu biblioteca de juegos diarios", color = colors.onSurfaceVariant, fontSize = 14.sp, modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp))
                OutlinedTextField(value = query, onValueChange = { query = it }, placeholder = { Text("Buscar un juego…") }, leadingIcon = { Icon(Icons.Outlined.Search, null) },
                    trailingIcon = { if(query.isNotEmpty()) IconButton(onClick = { query = "" }) { Icon(Icons.Outlined.Close, "Borrar búsqueda") } },
                    singleLine = true, shape = RoundedCornerShape(14.dp), modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp))
                LazyRow(modifier = Modifier.testTag("categories"), contentPadding = PaddingValues(horizontal = 20.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(listOf("Todos") + all.values.map { it.category }.distinct().sorted()) { cat ->
                        FilterChip(selected = category == cat, onClick = { category = cat }, label = { Text(cat) })
                    }
                }
                val source = when(tab) { 1 -> library.favorites.mapNotNull { all[it] }.sortedBy { it.name }; 2 -> library.orderedRecent().mapNotNull { all[it] }; else -> games }
                val visible = remember(source, query, category) { filterGames(source, query, category) }
                Row(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp)) {
                    Text(listOf("Descubre tu próximo reto", "Tus favoritos", "Últimas partidas")[tab], fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                    Text("${visible.size}", color = colors.onSurfaceVariant)
                }
                if (updating && games.isEmpty()) LinearProgressIndicator(Modifier.fillMaxWidth())
                if(visible.isEmpty()) Box(Modifier.fillMaxSize().padding(32.dp), contentAlignment = Alignment.Center) {
                    Text(if(games.isEmpty()) "Cargando tu biblioteca…" else when { query.isNotEmpty() || category != "Todos" -> "No hay coincidencias. Prueba otra búsqueda o categoría."; tab == 1 -> "Guarda los juegos que te gustan con el corazón."; tab == 2 -> "Aquí aparecerán los juegos que abras."; else -> "No hay juegos disponibles." }, color = colors.onSurfaceVariant)
                } else LazyVerticalGrid(columns = GridCells.Fixed(2), state = scrolls[tab], contentPadding = PaddingValues(20.dp, 8.dp, 20.dp, 20.dp), horizontalArrangement = Arrangement.spacedBy(14.dp), verticalArrangement = Arrangement.spacedBy(18.dp)) {
                    items(visible, key = { it.id }) { game ->
                        Card(onClick = { play(game) }, shape = RoundedCornerShape(12.dp), colors = CardDefaults.cardColors(containerColor = colors.surface)) {
                            Thumbnail(game, Modifier.fillMaxWidth().aspectRatio(1.2f))
                            Column(Modifier.padding(start = 10.dp, end = 6.dp, top = 10.dp)) {
                                Text(game.name, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                Text(game.category, fontSize = 12.sp, color = colors.onSurfaceVariant)
                                if(tab == 2) Text(library.recent[game.id]?.let { DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT).format(Date(it)) }.orEmpty(), fontSize = 11.sp, maxLines = 1)
                                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                                    if(tab == 2) IconButton(onClick = { vm.removeRecent(game.id) }) { Icon(Icons.Outlined.DeleteOutline, "Eliminar ${game.name} del historial", Modifier.size(20.dp)) }
                                    IconButton(onClick = { detailId = game.id }) { Icon(Icons.Outlined.Info, "Información de ${game.name}", Modifier.size(20.dp)) }
                                    IconButton(onClick = { vm.toggle(game) }, modifier = Modifier.semantics { stateDescription = if(game.id in library.favorites) "Guardado" else "Sin guardar" }) { Icon(if(game.id in library.favorites) Icons.Outlined.Favorite else Icons.Outlined.FavoriteBorder, "Favorito: ${game.name}", Modifier.size(20.dp), tint = if(game.id in library.favorites) colors.primary else colors.onSurfaceVariant) }
                                }
                            }
                        }
                    }
                }
            }
        }
        val detail = all[detailId]
        if(detail != null) ModalBottomSheet(onDismissRequest = { detailId = null }) {
            Column(Modifier.verticalScroll(rememberScrollState()).padding(24.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Thumbnail(detail, Modifier.fillMaxWidth().height(180.dp))
                Text(detail.name, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                Text("${detail.category} · ${detail.language ?: "Idioma no confirmado"}", color = colors.onSurfaceVariant)
                Text(detail.description)
                Button(onClick = { play(detail) }, modifier = Modifier.fillMaxWidth()) { Text("Jugar") }
                OutlinedButton(onClick = { vm.toggle(detail) }, modifier = Modifier.fillMaxWidth()) { Text(if(detail.id in library.favorites) "Quitar de favoritos" else "Añadir a favoritos") }
                TextButton(onClick = { play(detail, true) }) { Icon(Icons.AutoMirrored.Outlined.OpenInNew, null); Text(" Abrir en navegador") }
                detail.sourceUrl?.let { url -> TextButton(onClick = { if(!openUrl(url)) browserError = "No se pudo abrir la fuente. Comprueba que tienes un navegador instalado." }) { Text("Consultar ficha en Listdle") } }
                Text("Miniatura: ${detail.thumbnailSource}", style = MaterialTheme.typography.bodySmall)
                Text("Enlace: ${detail.linkStatus} · ${detail.verifiedAt.orEmpty()}", style = MaterialTheme.typography.bodySmall)
                Text("Si la página falla, comprueba tu conexión y vuelve a abrirla con Jugar o Abrir en navegador.", style = MaterialTheme.typography.bodySmall)
            }
        }
        if(settings) ModalBottomSheet(onDismissRequest = { settings = false }) {
            Column(Modifier.verticalScroll(rememberScrollState()).padding(24.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Text("Ajustes", style = MaterialTheme.typography.headlineMedium)
                Text("Apariencia", fontWeight = FontWeight.SemiBold)
                Row { listOf("auto" to "Sistema", "light" to "Claro", "dark" to "Oscuro").forEach { (value, label) -> FilterChip(library.theme == value, { vm.theme(value) }, { Text(label) }, modifier = Modifier.padding(end = 8.dp)) } }
                OutlinedButton(onClick = vm::refresh, enabled = !updating) { Text(if(updating) "Actualizando…" else "Actualizar catálogo") }
                Text(status, style = MaterialTheme.typography.bodySmall)
                Text("DleBox ${BuildConfig.VERSION_NAME} · ${if(BuildConfig.DEBUG) "Compilación de prueba" else "Release"}")
                Text("Biblioteca independiente. Los juegos pertenecen a sus autores y requieren Internet. Listdle se utiliza como fuente de descubrimiento. No existe afiliación con los juegos ni con Listdle.")
                Text("Favoritos e historial se guardan en este dispositivo. Sin cuentas, anuncios ni analíticas. La copia de seguridad automática de DleBox está desactivada. El navegador administra sus propias cookies y datos.", style = MaterialTheme.typography.bodySmall)
                TextButton(onClick = { if(!openUrl(BuildConfig.REPOSITORY_URL)) browserError = "No se pudo abrir GitHub. Comprueba tu navegador." }) { Text("Abrir repositorio GitHub") }
            }
        }
        if(failed != null) AlertDialog(onDismissRequest = { failed = null }, title = { Text("No se pudo abrir el juego") }, text = { Text("Comprueba que tienes un navegador instalado. Puedes volver a intentarlo o abrir el enlace externamente.") }, confirmButton = { TextButton(onClick = { failed?.let { play(it) } }) { Text("Reintentar") } }, dismissButton = { TextButton(onClick = { failed?.let { play(it, true) } }) { Text("Navegador") } })
        val message = storageError ?: browserError
        if(message != null) AlertDialog(onDismissRequest = { vm.dismissError(); browserError = null }, title = { Text("No se completó la acción") }, text = { Text(message) }, confirmButton = { TextButton(onClick = { vm.dismissError(); browserError = null }) { Text("Entendido") } })
    }
}

@Composable fun Thumbnail(game: Game, modifier: Modifier = Modifier) {
    if(game.thumbnailUrl != null) SubcomposeAsyncImage(model = game.thumbnailUrl, contentDescription = "Miniatura de ${game.name}", contentScale = ContentScale.Crop, modifier = modifier, loading = { Graphic(game, Modifier.fillMaxSize()) }, error = { Graphic(game, Modifier.fillMaxSize()) })
    else Graphic(game, modifier)
}
@Composable private fun Graphic(game: Game, modifier: Modifier) {
    val palettes = listOf(Color(0xFFE4EDDF), Color(0xFFF0EACF), Color(0xFFE1E8EC), Color(0xFFE8E1DA), Color(0xFFE7E3ED))
    val index = (game.id.hashCode().toLong() and 0x7fffffff).toInt()
    Box(modifier.background(palettes[index % palettes.size]).semantics { contentDescription = "Tarjeta gráfica de ${game.name}; no es una captura" }) {
        Column(Modifier.fillMaxSize().padding(14.dp), verticalArrangement = Arrangement.SpaceBetween) {
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                repeat(5) { i -> Box(Modifier.size(12.dp).background(if((index shr i) and 1 == 1) Green else Color(0xFFC9B458), RoundedCornerShape(2.dp))) }
            }
            Text(game.name, fontSize = 23.sp, lineHeight = 26.sp, fontWeight = FontWeight.Bold, color = Color(0xFF283326), maxLines = 2, overflow = TextOverflow.Ellipsis)
            Text("RETO DIARIO", fontSize = 9.sp, letterSpacing = 2.sp, color = Color(0xFF4C594A))
        }
    }
}
