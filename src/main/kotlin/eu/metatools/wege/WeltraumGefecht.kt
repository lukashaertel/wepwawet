package eu.metatools.wege

import com.badlogic.gdx.Screen
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Application
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3ApplicationConfiguration
import com.badlogic.gdx.utils.viewport.ScreenViewport
import ktx.app.*
import ktx.async.assets.AssetStorage
import ktx.async.enableKtxCoroutines
import ktx.async.ktxAsync
import ktx.scene2d.*
import kotlin.properties.Delegates

import com.badlogic.gdx.maps.tiled.*
import eu.metatools.wege.screens.*

/** ApplicationListener implementation. */
class WeltraumGefecht : KtxGame<Screen>() {
    val viewport = ScreenViewport().apply {
        unitsPerPixel = 1f / 2f
    }

    var storage by Delegates.notNull<AssetStorage>()

    override fun create() {
        enableKtxCoroutines(asynchronousExecutorConcurrencyLevel = 1)
        storage = AssetStorage()
        storage.setLoader(TmxMapLoader(), "tmx")

        ktxAsync {
            // Indicate loading, no resources explicitly needed
            addScreen(Loading(this@WeltraumGefecht))
            setScreen<Loading>()

            // Load main resource
            Scene2DSkin.defaultSkin = storage.load("ui/skin/uiskin.json")

            // Add screens depending on main resource
            addScreen(Browse(this@WeltraumGefecht))
            addScreen(Create(this@WeltraumGefecht))
            addScreen(Menu(this@WeltraumGefecht))
            addScreen(Voronois(this@WeltraumGefecht))

            // Go to menu screen
            setScreen<Menu>()
        }
    }

    override fun resize(width: Int, height: Int) {
        super.resize(width, height)
        viewport.update(width, height, true)
    }

    override fun dispose() {
        storage.dispose()
    }
}

fun main(args: Array<String>) {
    Lwjgl3Application(WeltraumGefecht(), Lwjgl3ApplicationConfiguration().apply {
        // Config goes here
    })
}