package com.dlebox.android

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.browser.customtabs.CustomTabColorSchemeParams
import androidx.browser.customtabs.CustomTabsIntent

/** true significa que Android aceptó el lanzamiento, no que la web completó la carga. */
fun openBrowser(context: Context, url: String, external: Boolean, dark: Boolean, toolbar: Int): Boolean {
    if(!CatalogCodec.safeUrl(url)) return false
    return try {
        fun externalIntent() = context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)).addCategory(Intent.CATEGORY_BROWSABLE))
        if(external) externalIntent() else try {
            CustomTabsIntent.Builder().setShowTitle(true)
                .setDefaultColorSchemeParams(CustomTabColorSchemeParams.Builder().setToolbarColor(toolbar).build())
                .setColorScheme(if(dark) CustomTabsIntent.COLOR_SCHEME_DARK else CustomTabsIntent.COLOR_SCHEME_LIGHT)
                .build().launchUrl(context,Uri.parse(url))
        } catch (_: ActivityNotFoundException) { externalIntent() }
        true
    } catch (_: Exception) { false }
}

fun recordSuccessfulLaunch(game: Game, launch: () -> Boolean, record: (Game) -> Unit): Boolean {
    if(!CatalogCodec.safeUrl(game.gameUrl) || !launch()) return false
    record(game)
    return true
}
