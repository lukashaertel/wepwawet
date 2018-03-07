package eu.metatools.wege.tools

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.InputMultiplexer
import com.badlogic.gdx.InputProcessor
import com.badlogic.gdx.scenes.scene2d.Stage
import ktx.app.KtxScreen
import ktx.scene2d.KHorizontalGroup
import ktx.scene2d.horizontalGroup

/**
 * Adds a horizontal group aligned left top.
 */
inline fun topLeft(init: KHorizontalGroup.() -> Unit) = horizontalGroup {
    pad(10f)
    space(10f)
    left()
    top()
    setFillParent(true)
    init()
}

/**
 * Adds a horizontal group aligned right top.
 */
inline fun topRight(init: KHorizontalGroup.() -> Unit) = horizontalGroup {
    pad(10f)
    space(10f)
    right()
    top()
    setFillParent(true)
    init()
}


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

// TODO: Replace KTX screens with actual screens, not being able to pass parameters is sucky. Also there is a need for
// stacks of screens

/**
 * Stage-based screen, can set [processor] to handle inputs.
 */
abstract class StageScreen<G : ExGame<G>>(game: G) : ExScreen<G>(game) {
    /**
     * The stage this screen displays.
     */
    abstract val stage: Stage

    /**
     * Optional input processor.
     */
    open val processor: InputProcessor? = null

    override fun enter() {
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
