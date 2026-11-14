package com.dlebox.android

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.datastore.preferences.preferencesDataStoreFile
import androidx.datastore.preferences.core.*
import androidx.lifecycle.ViewModelProvider
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.By
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.Until
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first
import kotlinx.serialization.encodeToString
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

class DeviceTest {
    @get:Rule val ui = createAndroidComposeRule<MainActivity>()
    private val device get() = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
    private fun shot(name: String, compose: Boolean=true) {
        if(compose) ui.waitForIdle()
        device.waitForIdle(2000)
        android.os.SystemClock.sleep(450)
        device.executeShellCommand("mkdir -p /sdcard/Download/dlebox-check")
        device.executeShellCommand("screencap -p /sdcard/Download/dlebox-check/$name.png")
    }
    private fun search(value: String) {
        ui.onNodeWithTag("nav-0").performClick()
        ui.onNodeWithTag("search").performTextReplacement(value)
        ui.onNodeWithTag("search").performImeAction()
    }
    private fun favorite(name: String) {
        // La acción semántica comprueba también la ruta accesible y evita que una
        // animación tardía del IME del emulador desplace el punto de pulsación.
        ui.onNodeWithContentDescription("Favorito: $name").performSemanticsAction(androidx.compose.ui.semantics.SemanticsActions.OnClick) { it() }
    }
    @Test fun visualJourneyAndBrowserRoundTrip() {
        val vm=ViewModelProvider(ui.activity)[LibraryViewModel::class.java]
        ui.waitUntil(30000) { vm.games.value.size >= 200 }
        ui.runOnIdle { vm.theme("light");vm.clearRecent();vm.saved.value.favorites.mapNotNull { vm.saved.value.snapshots[it] }.forEach(vm::toggle) }
        ui.waitUntil(5000) { vm.saved.value.theme=="light" && vm.saved.value.favorites.isEmpty() }
        ui.onNodeWithTag("explore-grid").assertExists()
        shot("01-explorar-claro")
        // El scroll sobrevive al cambio de sección.
        ui.onNodeWithTag("explore-grid").performScrollToIndex(70)
        val position=ui.onNodeWithTag("explore-grid").fetchSemanticsNode().config[androidx.compose.ui.semantics.SemanticsProperties.VerticalScrollAxisRange].value()
        ui.onNodeWithTag("nav-1").performClick()
        ui.onNodeWithTag("search").assertDoesNotExist()
        ui.onNodeWithText("Tus próximos imprescindibles").assertExists()
        shot("04-favoritos-vacios")
        ui.onNodeWithText("Ir a Explorar").performClick()
        assertEquals(position,ui.onNodeWithTag("explore-grid").fetchSemanticsNode().config[androidx.compose.ui.semantics.SemanticsProperties.VerticalScrollAxisRange].value(),.01f)
        ui.onNodeWithTag("categories").performScrollToNode(hasText("Geografía"))
        ui.onAllNodesWithText("Geografía",useUnmergedTree=true).onFirst().performClick()
        ui.onNodeWithTag("explore-grid").performScrollToIndex(0)
        shot("02-categoria-geografia")
        ui.onNodeWithTag("categories").performScrollToNode(hasText("Todos"))
        ui.onNodeWithText("Todos").performClick()
        search("Waffle")
        ui.onNodeWithTag("game-waffle").assertIsDisplayed()
        ui.onNodeWithContentDescription("Información de Wordle").assertDoesNotExist()
        shot("03-busqueda-waffle")
        favorite("Waffle")
        ui.waitUntil(15000) { "waffle" in vm.saved.value.favorites }
        ui.activityRule.scenario.recreate()
        ui.waitUntil(15000) { ui.onAllNodesWithTag("game-waffle").fetchSemanticsNodes().isNotEmpty() }
        ui.onNodeWithContentDescription("Favorito: Waffle").assert(SemanticsMatcher.expectValue(androidx.compose.ui.semantics.SemanticsProperties.StateDescription,"Guardado"))
        listOf("Globle","FilmLink","Bandle").forEach { name ->
            search(name)
            favorite(name)
        }
        ui.waitUntil(5000) { vm.saved.value.favorites.size==4 }
        ui.onNodeWithTag("nav-1").performClick()
        ui.onNodeWithTag("search").assertDoesNotExist()
        shot("05-favoritos-varios")
        val localGames=vm.games.value.filter { it.id in setOf("waffle","globle","filmlink") }
        ui.runOnIdle { localGames.forEach(vm::opened) }
        ui.waitUntil(5000) { vm.saved.value.recent.size==3 }
        ui.onNodeWithTag("nav-2").performClick()
        ui.onNodeWithTag("recent-list").assertExists()
        ui.onNodeWithTag("search").assertDoesNotExist()
        shot("06-recientes-varios")
        ui.onNodeWithContentDescription("Borrar todo el historial").performClick()
        ui.onNodeWithText("Cancelar").performClick()
        assertEquals(3,vm.saved.value.recent.size)
        ui.onNodeWithContentDescription("Eliminar Globle del historial").performClick()
        ui.waitUntil(5000) { "globle" !in vm.saved.value.recent }
        ui.onNodeWithContentDescription("Borrar todo el historial").performClick()
        ui.onNodeWithText("Borrar historial").performClick()
        ui.waitUntil(5000) { vm.saved.value.recent.isEmpty() }
        assertEquals(4,vm.saved.value.favorites.size)
        shot("07-recientes-vacios")
        search("")
        ui.onNodeWithTag("explore-grid").performScrollToIndex(0)
        ui.runOnIdle { vm.theme("dark") }
        ui.waitUntil(5000) { vm.saved.value.theme=="dark" }
        shot("08-explorar-oscuro")
        ui.runOnIdle { vm.theme("light") }
        search("Waffle")
        ui.onNodeWithTag("game-waffle").performSemanticsAction(androidx.compose.ui.semantics.SemanticsActions.OnClick) { it() }
        val app=InstrumentationRegistry.getInstrumentation().targetContext.packageName
        assertTrue("Android debe mostrar un navegador",device.wait(Until.hasObject(By.pkg("com.android.chrome")),15000))
        repeat(6) {
            val consent=device.wait(Until.findObject(By.text(java.util.regex.Pattern.compile("(?i)(Use without an account|Continue without an account|Accept & continue|No thanks|Not now|Got it)"))),4000)
            if(consent!=null) {
                try { consent.click();device.waitForIdle(3000) }
                catch (_: androidx.test.uiautomator.StaleObjectException) { /* Chrome cambió de pantalla; se vuelve a consultar. */ }
            }
        }
        assertNotEquals(app,device.currentPackageName)
        assertTrue("El navegador debe llegar al destino original",device.wait(Until.hasObject(By.textContains("wafflegame.net")),20000))
        shot("09-juego-navegador",false)
        for(attempt in 0..3) {
            if(device.hasObject(By.pkg(app))) break
            device.pressBack()
            if(device.wait(Until.hasObject(By.pkg(app)),5000)) break
        }
        ui.waitUntil(15000) { ui.onAllNodesWithTag("game-waffle").fetchSemanticsNodes().isNotEmpty() }
        ui.onNodeWithTag("search").assertTextContains("Waffle")
        ui.onNodeWithTag("game-waffle").assertIsDisplayed()
        assertTrue("waffle" in vm.saved.value.recent)
        shot("10-regreso-al-catalogo")
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
