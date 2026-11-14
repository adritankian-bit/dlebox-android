package com.dlebox.android

import android.app.Application
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import java.net.HttpURLConnection
import java.net.URL
import java.io.File

private val android.content.Context.libraryStore by preferencesDataStore("library")
class LibraryViewModel(app: Application) : AndroidViewModel(app) {
    private val store = app.libraryStore
    private val key = stringPreferencesKey("library_v1")
    val error = MutableStateFlow<String?>(null)
    val saved = store.data.map { CatalogCodec.json.decodeFromString<SavedLibrary>(it[key] ?: "{}") }
        .catch { error.value = "No se pudieron leer los datos locales. Reinicia DleBox."; emit(SavedLibrary()) }
        .stateIn(viewModelScope, SharingStarted.Eagerly, SavedLibrary())
    val games = MutableStateFlow<List<Game>>(emptyList())
    val refreshing = MutableStateFlow(false)
    val status = MutableStateFlow("Cargando catálogo…")
    private var currentVersion = 0
    private val cache = File(app.filesDir, "catalog.json")
    init { viewModelScope.launch {
        withContext(Dispatchers.IO) {
            val bundledRaw = runCatching { app.assets.open("games.json").bufferedReader().use { it.readText() } }.getOrNull()
            val cachedRaw = runCatching { android.util.AtomicFile(cache).openRead().bufferedReader().use { it.readText() } }.getOrNull()
            val selected = chooseCatalog(bundledRaw, cachedRaw)
            if (selected != null) {
                games.value = selected.games; currentVersion = selected.version
                status.value = "Catálogo local · ${selected.games.size} juegos"
            } else status.value = "No se pudo leer el catálogo. Prueba a actualizarlo."
        }
        refresh()
    } }
    fun dismissError() { error.value = null }
    private fun update(change: (SavedLibrary) -> SavedLibrary) { viewModelScope.launch {
        runCatching { store.edit { p ->
            val previous = CatalogCodec.json.decodeFromString<SavedLibrary>(p[key] ?: "{}")
            val next = change(previous)
            val keep = next.favorites + next.recent.keys
            p[key] = CatalogCodec.json.encodeToString(next.copy(snapshots = next.snapshots.filterKeys { it in keep }))
        } }.onFailure { error.value = "No se ha podido guardar el cambio. Vuelve a intentarlo." }
    } }
    fun toggle(g: Game) = update { it.toggle(g) }
    fun opened(g: Game) = update { it.opened(g, System.currentTimeMillis()) }
    fun removeRecent(id: String) = update { it.removeRecent(id) }
    fun clearRecent() = update { it.clearRecent() }
    fun theme(value: String) = update { it.copy(theme = value) }
    fun refresh() {
        if (refreshing.value) return
        refreshing.value = true
        viewModelScope.launch {
            runCatching { withContext(Dispatchers.IO) {
                val connection = URL(BuildConfig.CATALOG_URL).openConnection() as HttpURLConnection
                connection.connectTimeout = 10000; connection.readTimeout = 15000
                connection.instanceFollowRedirects = false
                try {
                    require(connection.responseCode == 200) { "HTTP ${connection.responseCode}" }
                    val bytes = connection.inputStream.use { input ->
                        val out = java.io.ByteArrayOutputStream()
                        val buffer = ByteArray(8192)
                        while (true) {
                            val count = input.read(buffer)
                            if (count == -1) break
                            require(out.size() + count <= 2_000_000) { "Catálogo demasiado grande" }
                            out.write(buffer, 0, count)
                        }
                        out.toByteArray()
                    }
                    require(bytes.size <= 2_000_000) { "Catálogo demasiado grande" }
                    val raw = bytes.toString(Charsets.UTF_8)
                    val catalog = CatalogCodec.decode(raw)
                    require(catalog.version >= currentVersion) { "Versión antigua" }
                    val atomic = android.util.AtomicFile(cache)
                    val stream = atomic.startWrite()
                    try { stream.write(bytes); atomic.finishWrite(stream) } catch (e: Exception) { atomic.failWrite(stream); throw e }
                    currentVersion = catalog.version; games.value = catalog.games
                    status.value = "Actualizado · ${catalog.games.size} juegos · v${catalog.version}"
                } finally { connection.disconnect() }
            } }.onFailure { status.value = if(games.value.isEmpty()) "No se pudo recuperar el catálogo. Vuelve a intentarlo." else "Sin actualización · catálogo local disponible" }
            refreshing.value = false
        }
    }
}
