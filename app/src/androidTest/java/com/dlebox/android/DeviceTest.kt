package com.dlebox.android

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.datastore.preferences.preferencesDataStoreFile
import androidx.datastore.preferences.core.*
import androidx.lifecycle.ViewModelProvider
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first
import kotlinx.serialization.encodeToString
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

class DeviceTest {
    @get:Rule val ui = createAndroidComposeRule<MainActivity>()
    @Test fun searchFilterFavoriteAndRecent() {
        ui.waitUntil(20000) { ui.onAllNodesWithContentDescription("Información de Waffle").fetchSemanticsNodes().isNotEmpty() }
        ui.onNode(hasSetTextAction()).performTextInput("Waffle")
        ui.onNodeWithContentDescription("Información de Waffle").assertExists()
        ui.onNodeWithContentDescription("Información de Wordle").assertDoesNotExist()
        ui.onNodeWithContentDescription("Favorito: Waffle").performClick()
        ui.waitUntil(5000) { ui.onAllNodes(SemanticsMatcher.expectValue(androidx.compose.ui.semantics.SemanticsProperties.StateDescription,"Guardado")).fetchSemanticsNodes().isNotEmpty() }
        ui.activityRule.scenario.recreate()
        ui.waitUntil(10000) { ui.onAllNodesWithContentDescription("Favorito: Waffle").fetchSemanticsNodes().isNotEmpty() }
        ui.onNodeWithContentDescription("Favorito: Waffle").assert(SemanticsMatcher.expectValue(androidx.compose.ui.semantics.SemanticsProperties.StateDescription,"Guardado"))
        ui.onNodeWithText("Favoritos").performClick()
        ui.onNodeWithContentDescription("Información de Waffle").assertExists()
        ui.onNodeWithText("Explorar").performClick()
        ui.onNodeWithContentDescription("Borrar búsqueda").performClick()
        ui.onNodeWithTag("categories").performScrollToNode(hasText("Palabras"))
        ui.onAllNodesWithText("Palabras",useUnmergedTree=true).onFirst().performClick()
        ui.onNodeWithContentDescription("Información de Wordle").assertExists()
        val vm = ViewModelProvider(ui.activity)[LibraryViewModel::class.java]
        ui.runOnIdle { vm.opened(vm.games.value.first { it.id == "waffle" }) }
        ui.onNodeWithText("Recientes").performClick()
        ui.waitUntil(5000) { ui.onAllNodesWithContentDescription("Eliminar Waffle del historial").fetchSemanticsNodes().isNotEmpty() }
        ui.onNodeWithContentDescription("Eliminar Waffle del historial").performClick()
        ui.waitUntil(5000) { ui.onAllNodesWithContentDescription("Eliminar Waffle del historial").fetchSemanticsNodes().isEmpty() }
    }
    @Test fun dataStoreSurvivesReopening(): Unit = runBlocking {
        val context=InstrumentationRegistry.getInstrumentation().targetContext
        val file=context.preferencesDataStoreFile("persistence-test-${System.nanoTime()}")
        val key=stringPreferencesKey("saved")
        val game=Game("test-game","Test","Descripción","Lógica","https://example.com/",addedAt="2026-11-14",linkStatus="unverified")
        val original=SavedLibrary().toggle(game).opened(game,1234)
        val job=SupervisorJob()
        val first=PreferenceDataStoreFactory.create(scope=CoroutineScope(Dispatchers.IO+job),produceFile={file})
        first.edit { it[key]=CatalogCodec.json.encodeToString(original) }
        job.cancelAndJoin()
        val nextJob=SupervisorJob()
        val reopened=PreferenceDataStoreFactory.create(scope=CoroutineScope(Dispatchers.IO+nextJob),produceFile={file})
        val restored=CatalogCodec.json.decodeFromString<SavedLibrary>(reopened.data.first()[key]!!)
        assertEquals(original,restored)
        nextJob.cancelAndJoin()
        file.delete()
        Unit
    }
}
