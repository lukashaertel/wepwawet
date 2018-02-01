package eu.metatools.wege.tools

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.InputMultiplexer
import com.badlogic.gdx.InputProcessor
import com.badlogic.gdx.scenes.scene2d.Stage
import ktx.app.KtxScreen
import ktx.scene2d.KHorizontalGroup
import ktx.scene2d.horizontalGroup

/**
 * Adds a horizontal group aligned left bottom.
 */
inline fun dropLeft(init: KHorizontalGroup.() -> Unit) = horizontalGroup {
    pad(10f)
    space(10f)
    left()
    bottom()
    setFillParent(true)
    init()
}

/**
 * Adds a horizontal group aligned right bottom.
 */
inline fun dropRight(init: KHorizontalGroup.() -> Unit) = horizontalGroup {
    pad(10f)
    space(10f)
    right()
    bottom()
    setFillParent(true)
    init()
}

/**
 * Stage-based screen, can set [processor] to handle inputs.
 */
abstract class StageScreen : KtxScreen {
    /**
     * The stage this screen displays.
     */
    abstract val stage: Stage

    /**
     * Optional input processor.
     */
    open val processor: InputProcessor? = null

    override fun show() {
        if (processor != null)
            Gdx.input.inputProcessor = InputMultiplexer(stage, processor)
        else
            Gdx.input.inputProcessor = stage
    }

    override fun render(delta: Float) {
        stage.act(delta)
        stage.draw()
    }

    override fun dispose() {
        stage.dispose()
    }
}
