package eu.metatools.voronois.screens

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.g2d.BitmapFont
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.utils.Align
import eu.metatools.voronois.WeltraumGefecht
import eu.metatools.voronois.tools.ExScreen
import ktx.app.use

class Loading(game: WeltraumGefecht) : ExScreen<WeltraumGefecht>(game) {
    val font = BitmapFont()
    val batch = SpriteBatch().apply {
        color = Color.WHITE
    }

    override fun render(delta: Float) {
        batch.use {
            font.draw(batch, "Loading ...", Gdx.graphics.width / 2f - 100f, Gdx.graphics.height / 2f, 200.0f, Align.center, false)
        }
    }

    override fun dispose() {
        font.dispose()
        batch.dispose()
    }
}