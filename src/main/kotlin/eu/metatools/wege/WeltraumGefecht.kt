package eu.metatools.wege

import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Application
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3ApplicationConfiguration
import com.badlogic.gdx.maps.tiled.TmxMapLoader
import com.badlogic.gdx.utils.viewport.ScreenViewport
import eu.metatools.wege.data.*
import eu.metatools.wege.screens.Loading
import eu.metatools.wege.screens.Menu
import eu.metatools.wege.tools.ExGame
import eu.metatools.wege.tools.localFile
import kotlinx.serialization.json.JSON
import kotlinx.serialization.serializer
import ktx.async.assets.AssetStorage
import ktx.async.enableKtxCoroutines
import ktx.async.ktxAsync
import ktx.scene2d.Scene2DSkin
import java.util.*
import kotlin.properties.Delegates

/** ApplicationListener implementation. */
class WeltraumGefecht : ExGame<WeltraumGefecht>() {
    val viewport = ScreenViewport().apply {
        unitsPerPixel = 1f / 2f
    }

    var storage by Delegates.notNull<AssetStorage>()

    var identity by localFile("uuid", UUID::randomUUID) {
        read {
            UUID.fromString(readString("utf-8"))
        }
        write {
            writeString(it.toString(), false, "utf-8")
        }
    }


    var settings by localFile("settings.json", { Settings.default }) {
        read {
            JSON.parse(readString("utf-8"))
        }
        write {
            writeString(JSON.stringify(it), false, "utf-8")
        }
    }

    val loading by lazy { Loading(this) }

    val menu by lazy { Menu(this) }

    override fun create() {
        super.create()
        enableKtxCoroutines(asynchronousExecutorConcurrencyLevel = 1)
        storage = AssetStorage()
        storage.setLoader(TmxMapLoader(), "tmx")

        ktxAsync {
            // Indicate loading, no resources explicitly needed
            pushScreen(loading)

            // Load main resource
            Scene2DSkin.defaultSkin = storage.load("ui/skin/uiskin.json")

            // Go to main menu
            replaceScreen(menu)

        }
    }


    override fun resize(width: Int, height: Int) {
        super.resize(width, height)
        viewport.update(width, height, true)
    }

    override fun dispose() {
        super.dispose()
        storage.dispose()
    }
}

fun main(args: Array<String>) {
    Lwjgl3Application(WeltraumGefecht(), Lwjgl3ApplicationConfiguration().apply {
        // Config goes here
    })
}