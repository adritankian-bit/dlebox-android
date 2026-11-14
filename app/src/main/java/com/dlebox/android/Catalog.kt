package com.dlebox.android

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.net.URI
import java.text.Normalizer

@Serializable data class Game(
    val id: String, val name: String, val description: String, val category: String,
    val gameUrl: String, val sourceUrl: String? = null, val thumbnailUrl: String? = null,
    val thumbnailType: String = "generated", val thumbnailSource: String = "DleBox: diseño original",
    val thumbnailPermission: String? = null, val language: String? = null,
    val addedAt: String, val linkStatus: String, val verifiedAt: String? = null
)
@Serializable data class Catalog(val schemaVersion: Int, val version: Int, val games: List<Game>)
object CatalogCodec {
    val json = Json { ignoreUnknownKeys = true; prettyPrint = true }
    fun safeUrl(value: String): Boolean = runCatching {
        val u = URI(value)
        u.scheme == "https" && !u.host.isNullOrBlank() && u.userInfo == null && (u.port == -1 || u.port == 443)
    }.getOrDefault(false)
    fun decode(raw: String): Catalog = json.decodeFromString<Catalog>(raw).also { c ->
        require(c.schemaVersion == 1 && c.version > 0 && c.games.isNotEmpty() && c.games.size <= 5000)
        require(c.games.map { it.id }.distinct().size == c.games.size)
        require(c.games.map { canonicalUrl(it.gameUrl) }.distinct().size == c.games.size)
        c.games.forEach { g ->
            require(g.id.matches(Regex("[a-z0-9][a-z0-9-]{0,79}")))
            require(g.name.isNotBlank() && g.description.isNotBlank() && g.category.isNotBlank())
            require(safeUrl(g.gameUrl) && (g.sourceUrl == null || safeUrl(g.sourceUrl)))
            require(g.thumbnailType in setOf("generated", "official", "authorized"))
            require(g.thumbnailSource.isNotBlank())
            require(g.thumbnailUrl == null || (safeUrl(g.thumbnailUrl) && !g.thumbnailPermission.isNullOrBlank() && g.thumbnailType != "generated"))
            require(g.linkStatus in setOf("verified", "unverified", "broken"))
            java.time.LocalDate.parse(g.addedAt)
            g.verifiedAt?.let { java.time.LocalDate.parse(it) }
            require(g.linkStatus != "verified" || g.verifiedAt != null)
        }
    }
    fun canonicalUrl(value: String): String {
        val u = URI(value)
        return "${u.scheme.lowercase()}://${u.host?.lowercase()}${u.path.orEmpty().trimEnd('/')}" + (u.query?.let { "?$it" } ?: "")
    }
}
fun String.searchKey(): String = Normalizer.normalize(lowercase(java.util.Locale.ROOT), Normalizer.Form.NFD).replace(Regex("\\p{M}"), "")
fun filterGames(games: List<Game>, query: String, category: String): List<Game> {
    val q = query.trim().searchKey()
    return games.filter { (category == "Todos" || it.category == category) &&
        (q.isEmpty() || "${it.name} ${it.description} ${it.category}".searchKey().contains(q)) }
}
@Serializable data class SavedLibrary(
    val favorites: Set<String> = emptySet(), val recent: Map<String, Long> = emptyMap(),
    val snapshots: Map<String, Game> = emptyMap(), val theme: String = "auto"
) {
    fun toggle(game: Game) = copy(favorites = if (game.id in favorites) favorites - game.id else favorites + game.id, snapshots = snapshots + (game.id to game))
    fun opened(game: Game, at: Long) = copy(recent = (recent + (game.id to at)).entries.sortedByDescending { it.value }.take(200).associate { it.toPair() }, snapshots = snapshots + (game.id to game))
    fun removeRecent(id: String) = copy(recent = recent - id)
    fun orderedRecent() = recent.entries.sortedByDescending { it.value }.map { it.key }
}
