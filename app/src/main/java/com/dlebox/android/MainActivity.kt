package com.dlebox.android

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.enableEdgeToEdge
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) { super.onCreate(savedInstanceState); setContent { DleBox() } }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable fun DleBox(vm: LibraryViewModel = viewModel()) {
    val library by vm.saved.collectAsStateWithLifecycle()
    val games by vm.games.collectAsStateWithLifecycle()
    val updating by vm.refreshing.collectAsStateWithLifecycle()
    val status by vm.status.collectAsStateWithLifecycle()
    val storageError by vm.error.collectAsStateWithLifecycle()
    val dark=library.theme=="dark" || (library.theme=="auto" && isSystemInDarkTheme())
    val colors=if(dark) darkColorScheme(primary=Color(0xFFA1D29A),background=Color(0xFF171B18),surface=Color(0xFF232824),secondaryContainer=Color(0xFF354B32))
        else lightColorScheme(primary=Color(0xFF396E34),background=Color.White,surface=Color(0xFFF6F6F6),secondaryContainer=Color(0xFFE5EFDF),onSurface=Color(0xFF222222),outlineVariant=Color(0xFFE5E5E5))
    MaterialTheme(colorScheme=colors) {
        val context=LocalContext.current
        val focus=LocalFocusManager.current
        SideEffect { (context as? ComponentActivity)?.let { activity ->
            val transparent=android.graphics.Color.TRANSPARENT
            val bars=if(dark)SystemBarStyle.dark(transparent)else SystemBarStyle.light(transparent,transparent)
            activity.enableEdgeToEdge(statusBarStyle=bars,navigationBarStyle=bars)
        } }
        var tab by rememberSaveable { mutableIntStateOf(0) }
        var query by rememberSaveable { mutableStateOf("") }
        var category by rememberSaveable { mutableStateOf("Todos") }
        var order by rememberSaveable { mutableStateOf(GameOrder.NAME.name) }
        var seed by rememberSaveable { mutableIntStateOf(kotlin.random.Random.nextInt()) }
        var showOrder by remember { mutableStateOf(false) }
        var settings by rememberSaveable { mutableStateOf(false) }
        var detailId by rememberSaveable { mutableStateOf<String?>(null) }
        var failed by remember { mutableStateOf<Game?>(null) }
        var browserError by remember { mutableStateOf<String?>(null) }
        var clearHistory by remember { mutableStateOf(false) }
        val exploreScroll=rememberLazyGridState()
        val favoriteScroll=rememberLazyGridState()
        val recentScroll=rememberLazyListState()
        val categoryScroll=rememberLazyListState()
        val all=remember(games,library.snapshots) { library.snapshots+games.associateBy { it.id } }
        val favorites=remember(library.favorites,all) { library.favorites.mapNotNull { all[it] }.sortedBy { it.name.searchKey() } }
        val recents=remember(library.recent,all) { library.orderedRecent().mapNotNull { all[it] } }
        val visible=remember(games,query,category,order,library.recent,seed) { orderGames(filterGames(games,query,category),GameOrder.valueOf(order),library.recent,seed) }
        val surprise=remember(visible) { surpriseCandidates(visible) }
        fun url(value: String, external: Boolean=false) = openBrowser(context,value,external,dark,colors.surface.toArgb())
        fun play(game: Game, external: Boolean=false) {
            focus.clearFocus()
            if(recordSuccessfulLaunch(game,{url(game.gameUrl,external)},vm::opened)) failed=null else failed=game
        }
        fun explore() { focus.clearFocus();tab=0 }
        BackHandler(settings || detailId!=null || tab!=0 || query.isNotEmpty() || category!="Todos") {
            when { detailId!=null->detailId=null;settings->settings=false;tab!=0->tab=0;query.isNotEmpty()->query="";else->category="Todos" }
        }
        Scaffold(containerColor=colors.background,bottomBar={
            NavigationBar(containerColor=colors.background,tonalElevation=0.dp) {
                listOf("Explorar" to Icons.Outlined.GridView,"Favoritos" to Icons.Outlined.FavoriteBorder,"Recientes" to Icons.Outlined.History).forEachIndexed { index,pair ->
                    NavigationBarItem(selected=tab==index,onClick={focus.clearFocus();tab=index},icon={Icon(pair.second,null)},label={Text(pair.first)},modifier=Modifier.testTag("nav-$index"))
                }
            }
        }) { padding ->
            Column(Modifier.padding(padding).imePadding().fillMaxSize()) {
                Row(Modifier.fillMaxWidth().padding(start=20.dp,end=8.dp,top=4.dp),verticalAlignment=Alignment.CenterVertically) {
                    Row(horizontalArrangement=Arrangement.spacedBy(2.dp),verticalAlignment=Alignment.CenterVertically) {
                        "DLE".forEachIndexed { index,c -> Box(Modifier.size(24.dp).background(if(index==1)Color(0xFFC9B458)else Color(0xFF6AAA64),RoundedCornerShape(3.dp)),contentAlignment=Alignment.Center) { Text(c.toString(),color=Color.White,fontWeight=FontWeight.Bold,fontSize=15.sp) } }
                        Text("Box",fontSize=23.sp,fontWeight=FontWeight.Bold,modifier=Modifier.padding(start=2.dp))
                    }
                    Spacer(Modifier.weight(1f))
                    IconButton(onClick={settings=true}) { Icon(Icons.Outlined.Settings,"Ajustes",Modifier.size(22.dp)) }
                }
                if(tab==0) {
                    OutlinedTextField(value=query,onValueChange={query=it},placeholder={Text("Buscar juegos")},leadingIcon={Icon(Icons.Outlined.Search,null,Modifier.size(22.dp))},
                        trailingIcon={if(query.isNotEmpty())IconButton(onClick={query=""}) { Icon(Icons.Outlined.Close,"Borrar búsqueda") }},
                        singleLine=true,shape=RoundedCornerShape(12.dp),keyboardOptions=KeyboardOptions(imeAction=ImeAction.Search),keyboardActions=KeyboardActions(onSearch={focus.clearFocus()}),
                        modifier=Modifier.fillMaxWidth().padding(horizontal=20.dp).padding(top=2.dp).testTag("search"))
                    val categories=remember(games) { listOf("Todos")+games.map { it.category }.distinct().sortedBy { it.searchKey() } }
                    LazyRow(state=categoryScroll,modifier=Modifier.testTag("categories"),contentPadding=PaddingValues(horizontal=20.dp),horizontalArrangement=Arrangement.spacedBy(6.dp)) {
                        items(categories,key={it}) { cat -> FilterChip(selected=category==cat,onClick={category=cat;focus.clearFocus()},label={Text(cat,style=MaterialTheme.typography.labelLarge)}) }
                    }
                    Row(Modifier.fillMaxWidth().padding(start=20.dp,end=8.dp),verticalAlignment=Alignment.CenterVertically) {
                        Text("${visible.size} ${if(visible.size==1)"juego" else "juegos"}",style=MaterialTheme.typography.labelLarge,color=colors.onSurfaceVariant,maxLines=1,overflow=androidx.compose.ui.text.style.TextOverflow.Ellipsis,modifier=Modifier.weight(1f).testTag("result-count"))
                        TextButton(onClick={surprise.randomOrNull()?.let { play(it) }},enabled=surprise.isNotEmpty(),contentPadding=PaddingValues(horizontal=8.dp)) { Icon(Icons.Outlined.Shuffle,null,Modifier.size(18.dp));Spacer(Modifier.width(5.dp));Text("Sorpréndeme",style=MaterialTheme.typography.labelLarge) }
                        Box {
                            IconButton(onClick={showOrder=true}) { Icon(Icons.Outlined.Sort,"Ordenar juegos",Modifier.size(22.dp)) }
                            DropdownMenu(expanded=showOrder,onDismissRequest={showOrder=false}) {
                                GameOrder.entries.forEach { choice -> DropdownMenuItem(text={Text(choice.title)},onClick={order=choice.name;if(choice==GameOrder.RANDOM)seed=kotlin.random.Random.nextInt();showOrder=false},trailingIcon={if(order==choice.name)Icon(Icons.Outlined.Check,null)}) }
                            }
                        }
                    }
                    if(games.isEmpty()) {
                        if(updating) Box(Modifier.fillMaxSize(),contentAlignment=Alignment.Center) { CircularProgressIndicator(Modifier.size(28.dp)) }
                        else EmptyLibrary(Icons.Outlined.CloudOff,"Tu biblioteca sigue aquí","No se pudo cargar el catálogo. Comprueba tu conexión para recuperarlo.","Actualizar catálogo",vm::refresh)
                    } else if(visible.isEmpty()) EmptyLibrary(Icons.Outlined.SearchOff,"Sin coincidencias","Prueba otro nombre o explora todas las categorías.","Restablecer filtros",{query="";category="Todos"})
                    else LazyVerticalGrid(columns=GridCells.Fixed(2),state=exploreScroll,modifier=Modifier.weight(1f).testTag("explore-grid"),contentPadding=PaddingValues(20.dp,4.dp,20.dp,20.dp),horizontalArrangement=Arrangement.spacedBy(12.dp),verticalArrangement=Arrangement.spacedBy(12.dp)) {
                        items(visible,key={it.id}) { game -> GameCard(game,game.id in library.favorites,{play(game)},{vm.toggle(game)},{detailId=game.id}) }
                    }
                } else if(tab==1) {
                    SectionHeading("Favoritos","${favorites.size} guardados")
                    if(favorites.isEmpty()) EmptyLibrary(Icons.Outlined.FavoriteBorder,"Tus próximos imprescindibles","Toca el corazón de un juego para guardarlo aquí.","Ir a Explorar",::explore)
                    else LazyVerticalGrid(columns=GridCells.Fixed(2),state=favoriteScroll,modifier=Modifier.weight(1f).testTag("favorite-grid"),contentPadding=PaddingValues(20.dp,8.dp,20.dp,20.dp),horizontalArrangement=Arrangement.spacedBy(12.dp),verticalArrangement=Arrangement.spacedBy(12.dp)) {
                        items(favorites,key={it.id}) { game -> GameCard(game,true,{play(game)},{vm.toggle(game)},{detailId=game.id}) }
                    }
                } else {
                    Row(Modifier.padding(start=20.dp,end=8.dp).fillMaxWidth(),verticalAlignment=Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) { Text("Recientes",style=MaterialTheme.typography.titleLarge,fontWeight=FontWeight.SemiBold);Text("${recents.size} juegos · solo en este dispositivo",style=MaterialTheme.typography.bodySmall,color=colors.onSurfaceVariant) }
                        if(recents.isNotEmpty()) IconButton(onClick={clearHistory=true}) { Icon(Icons.Outlined.DeleteSweep,"Borrar todo el historial") }
                    }
                    if(recents.isEmpty()) EmptyLibrary(Icons.Outlined.History,"Continúa donde lo dejaste","Los juegos que abras aparecerán aquí, del más reciente al más antiguo.","Descubrir juegos",::explore)
                    else LazyColumn(state=recentScroll,modifier=Modifier.weight(1f).testTag("recent-list"),contentPadding=PaddingValues(vertical=8.dp)) {
                        items(recents,key={it.id}) { game -> RecentRow(game,library.recent[game.id] ?: 0L,{play(game)},{vm.removeRecent(game.id)},{detailId=game.id}) }
                    }
                }
            }
        }
        all[detailId]?.let { game -> ModalBottomSheet(onDismissRequest={detailId=null}) {
            Column(Modifier.verticalScroll(rememberScrollState()).padding(24.dp),verticalArrangement=Arrangement.spacedBy(12.dp)) {
                Thumbnail(game,Modifier.fillMaxWidth().height(150.dp))
                Text(game.name,style=MaterialTheme.typography.headlineSmall,fontWeight=FontWeight.Bold)
                Text("${game.category} · ${game.language ?: "Idioma no confirmado"}",color=colors.onSurfaceVariant,style=MaterialTheme.typography.bodySmall)
                Text(game.description,style=MaterialTheme.typography.bodyMedium)
                Button(onClick={play(game)},modifier=Modifier.fillMaxWidth()) { Text("Jugar") }
                OutlinedButton(onClick={vm.toggle(game)},modifier=Modifier.fillMaxWidth()) { Text(if(game.id in library.favorites)"Quitar de favoritos" else "Añadir a favoritos") }
                TextButton(onClick={play(game,true)}) { Icon(Icons.AutoMirrored.Outlined.OpenInNew,null,Modifier.size(18.dp));Text(" Abrir en navegador") }
                game.sourceUrl?.let { source -> TextButton(onClick={if(!url(source))browserError="No se pudo abrir la fuente. Comprueba que tienes un navegador instalado."}) { Text(if(source.contains("listdle.com"))"Ficha en Listdle" else "Consultar fuente del catálogo") } }
                Text(if(game.linkStatus=="verified")"Respuesta HTTP comprobada el ${game.verifiedAt}." else "Disponibilidad pendiente de comprobación: el sitio puede limitar las comprobaciones automáticas.",style=MaterialTheme.typography.bodySmall,color=colors.onSurfaceVariant)
                Text("Si la página falla, vuelve con Atrás y reintenta o abre el navegador externo.",style=MaterialTheme.typography.bodySmall,color=colors.onSurfaceVariant)
                Text("Imagen: ${game.thumbnailSource}",style=MaterialTheme.typography.labelSmall,color=colors.onSurfaceVariant)
            }
        } }
        if(settings) ModalBottomSheet(onDismissRequest={settings=false}) {
            Column(Modifier.verticalScroll(rememberScrollState()).padding(24.dp),verticalArrangement=Arrangement.spacedBy(14.dp)) {
                Text("Ajustes",style=MaterialTheme.typography.headlineSmall)
                Text("Apariencia",fontWeight=FontWeight.SemiBold)
                FlowRow(horizontalArrangement=Arrangement.spacedBy(8.dp)) { listOf("auto" to "Sistema","light" to "Claro","dark" to "Oscuro").forEach { (value,label)->FilterChip(library.theme==value,{vm.theme(value)},{Text(label)}) } }
                OutlinedButton(onClick=vm::refresh,enabled=!updating) { Text(if(updating)"Actualizando…" else "Actualizar catálogo") }
                Text(status,style=MaterialTheme.typography.bodySmall)
                Text("DleBox ${BuildConfig.VERSION_NAME} · ${if(BuildConfig.DEBUG)"Compilación de prueba" else "Release"}")
                Text("Catálogo independiente. Referencia: Listdle. Datos adicionales de Awesome Daily Minigames bajo CC0-1.0. Los juegos y sus marcas pertenecen a sus autores. Sin afiliación.",style=MaterialTheme.typography.bodyMedium)
                Text("Favoritos e historial permanecen en este dispositivo. Sin cuentas, publicidad propia ni analíticas. Copia de seguridad automática desactivada. Las webs administran sus cookies y requieren Internet.",style=MaterialTheme.typography.bodySmall)
                TextButton(onClick={if(!url(BuildConfig.REPOSITORY_URL))browserError="No se pudo abrir GitHub. Comprueba tu navegador."}) { Text("Abrir repositorio GitHub") }
            }
        }
        if(clearHistory) AlertDialog(onDismissRequest={clearHistory=false},title={Text("¿Borrar el historial?")},text={Text("Se eliminarán todos los recientes de este dispositivo. Tus favoritos se conservarán.")},confirmButton={TextButton(onClick={vm.clearRecent();clearHistory=false}) {Text("Borrar historial")}},dismissButton={TextButton(onClick={clearHistory=false}) {Text("Cancelar")}})
        if(failed!=null) AlertDialog(onDismissRequest={failed=null},title={Text("No se pudo abrir el juego")},text={Text("Comprueba el enlace y que tienes un navegador instalado. No se ha añadido al historial.")},confirmButton={TextButton(onClick={failed?.let {play(it)}}) {Text("Reintentar")}},dismissButton={TextButton(onClick={failed?.let {play(it,true)}}) {Text("Navegador")}})
        val message=storageError ?: browserError
        if(message!=null) AlertDialog(onDismissRequest={vm.dismissError();browserError=null},title={Text("No se completó la acción")},text={Text(message)},confirmButton={TextButton(onClick={vm.dismissError();browserError=null}) {Text("Entendido")}})
    }
}

@Composable private fun SectionHeading(title: String,subtitle: String) {
    Column(Modifier.fillMaxWidth().padding(horizontal=20.dp,vertical=8.dp)) {
        Text(title,style=MaterialTheme.typography.titleLarge,fontWeight=FontWeight.SemiBold)
        Text(subtitle,style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
