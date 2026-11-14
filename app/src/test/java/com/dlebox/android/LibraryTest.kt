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
    @Test fun corruptCacheFallsBackToBundled() { assertEquals(1,chooseCatalog(raw(),"not json")!!.version) }
    @Test fun emptyRemoteCannotReplaceValidCatalog() {
        val empty=CatalogCodec.json.encodeToString(Catalog(1,9,emptyList()))
        assertEquals(1,chooseCatalog(raw(),empty)!!.version)
    }
    @Test fun highestValidCatalogWins() { assertEquals(2,chooseCatalog(raw(),CatalogCodec.json.encodeToString(Catalog(1,2,listOf(game))))!!.version) }
    @Test fun missingOrCorruptCatalogIsRecoverable() { assertNull(chooseCatalog(null,"{}")) }
    @Test fun oldSchemaAndUnknownFieldsAreCompatible() {
        val old=raw().replace("\"version\": 1","\"version\": 1, \"futureField\": true")
        assertEquals(game.id,CatalogCodec.decode(old).games.first().id)
    }
    @Test fun sortingAndRandomRemainStable() {
        val b=game.copy(id="beta",name="Beta",addedAt="2026-11-13")
        val a=game.copy(id="alpha",name="Álpha")
        val list=listOf(b,game,a)
        assertEquals("alpha",orderGames(list,GameOrder.NAME,emptyMap(),1).first().id)
        assertEquals("beta",orderGames(list,GameOrder.PLAYED,mapOf("beta" to 9),1).first().id)
        assertEquals("alpha",orderGames(list,GameOrder.NEWEST,emptyMap(),1).first().id)
        assertEquals(orderGames(list,GameOrder.RANDOM,emptyMap(),5),orderGames(list,GameOrder.RANDOM,emptyMap(),5))
    }
    @Test fun surpriseExcludesBrokenAndUnsafeLinks() {
        assertEquals(listOf(game),surpriseCandidates(listOf(game,game.copy(id="broken",linkStatus="broken"),game.copy(id="unsafe",gameUrl="javascript:alert(1)"))))
    }
    @Test fun clearingHistoryKeepsFavorites() {
        val saved=SavedLibrary().toggle(game).opened(game,10).clearRecent()
        assertTrue(saved.recent.isEmpty());assertTrue(game.id in saved.favorites)
    }
    @Test fun failedBrowserLaunchNeverRecordsHistory() {
        var recorded=false
        assertFalse(recordSuccessfulLaunch(game,{false},{recorded=true}))
        assertFalse(recorded)
        assertTrue(recordSuccessfulLaunch(game,{true},{recorded=true}))
        assertTrue(recorded)
    }
    @Test fun legacyFavoritesSurviveNewCatalogMetadata() {
        val oldJson="""{"favorites":["waffle"],"recent":{"waffle":123},"snapshots":{},"theme":"dark"}"""
        val old=CatalogCodec.json.decodeFromString<SavedLibrary>(oldJson)
        assertTrue(game.id in old.favorites);assertEquals(123L,old.recent[game.id]);assertEquals("dark",old.theme)
    }
    @Test fun relativeDatesHandleFutureAndYesterday() {
        assertEquals("Ahora mismo",relativeOpened(2000,1000))
        assertEquals("Hace 2 min",relativeOpened(0,120000))
        assertEquals("Ayer",relativeOpened(0,86400000))
    }
}
