package com.dlebox.android

import org.junit.Assert.*
import org.junit.Test
import kotlinx.serialization.encodeToString

class LibraryTest {
    private val game = Game("waffle", "Waffle", "Intercambia letras", "Palabras", "https://wafflegame.net/", addedAt = "2026-11-14", linkStatus = "verified", verifiedAt = "2026-11-14")
    private fun raw(games: List<Game> = listOf(game)) = CatalogCodec.json.encodeToString(Catalog(1,1,games))
    @Test fun catalogRoundTrip() { assertEquals(game, CatalogCodec.decode(raw()).games.first()) }
    @Test fun duplicateIds() { assertThrows(IllegalArgumentException::class.java) { CatalogCodec.decode(raw(listOf(game,game))) } }
    @Test fun duplicateUrls() { assertThrows(IllegalArgumentException::class.java) { CatalogCodec.decode(raw(listOf(game,game.copy(id="other")))) } }
    @Test fun unsafeUrls() { listOf("http://a.com", "javascript:alert(1)", "https://u:p@a.com", "https://").forEach { assertFalse(CatalogCodec.safeUrl(it)) } }
    @Test fun search() { assertEquals(1,filterGames(listOf(game),"LETRAS","Todos").size);assertTrue(filterGames(listOf(game),"xyz","Todos").isEmpty()) }
    @Test fun accents() { assertEquals("geografia", "Geografía".searchKey()) }
    @Test fun category() { assertTrue(filterGames(listOf(game),"","Historia").isEmpty());assertEquals(1,filterGames(listOf(game),"","Palabras").size) }
    @Test fun favoritesSurviveSerialization() {
        val saved=SavedLibrary().toggle(game)
        val restored=CatalogCodec.json.decodeFromString<SavedLibrary>(CatalogCodec.json.encodeToString(saved))
        assertTrue(game.id in restored.favorites); assertEquals(game,restored.snapshots[game.id]);assertTrue(restored.toggle(game).favorites.isEmpty())
    }
    @Test fun recentOrderingAndRemoval() {
        val other=game.copy(id="other")
        val saved=SavedLibrary().opened(game,1).opened(other,2).opened(game,3)
        assertEquals(listOf("waffle","other"),saved.orderedRecent());assertEquals(listOf("other"),saved.removeRecent("waffle").orderedRecent())
    }
    @Test fun historyBounded() {
        var saved=SavedLibrary()
        repeat(250) { saved=saved.opened(game.copy(id="game-$it"),it.toLong()) }
        assertEquals(200,saved.recent.size)
    }
}
