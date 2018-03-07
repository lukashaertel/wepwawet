package eu.metatools.wege.tools

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.Color
import ktx.app.KtxApplicationAdapter
import ktx.app.clearScreen
import java.util.*
import kotlin.collections.ArrayList

/**
 * Abstract extended game, where screens are managed as stacks.
 */
abstract class ExGame<G : ExGame<G>>(val clear: Boolean = true) : KtxApplicationAdapter {
    /**
     * The list of owned screens.
     */
    internal val owned = ArrayList<ExScreen<G>>()

    /**
     * The current screen stack.
     */
    private val screenStack = Stack<ExScreen<G>>()

    /**
     * Gets the current screen stack.
     */
    val screens get() = screenStack.toList()

    /**
     * Gets the current screen.
     */
    val currentScreen get() = screenStack.lastOrNull()

    /**
     * Replaces the top screen.
     */
    fun replaceScreen(screen: ExScreen<G>) {
        popScreen()
        pushScreen(screen)
    }

    /**
     * Pushes the screen on the screen stack.
     */
    fun pushScreen(screen: ExScreen<G>) {
        currentScreen?.exit()
        screenStack.push(screen)
        currentScreen?.resize(Gdx.graphics.width, Gdx.graphics.height)
        currentScreen?.show()
        currentScreen?.enter()
    }

    /**
     * Pops the top screen.
     */
    fun popScreen() {
        currentScreen?.exit()
        currentScreen?.hide()
        screenStack.pop()
        currentScreen?.resize(Gdx.graphics.width, Gdx.graphics.height)
        currentScreen?.enter()
    }

    override fun create() {
        currentScreen?.resize(Gdx.graphics.width, Gdx.graphics.height)
        currentScreen?.show()
    }

    override fun render() {
        if (clear)
            clearScreen(0f, 0f, 0f, 1f)

        currentScreen?.render(Gdx.graphics.deltaTime)
    }

    override fun resize(width: Int, height: Int) {
        currentScreen?.resize(width, height)
    }

    override fun pause() {
        currentScreen?.pause()
    }

    override fun resume() {
        currentScreen?.resume()
    }

    override fun dispose() {
        owned.forEach {
            try {
                it.dispose()
            } catch (exception: Throwable) {
                onScreenDisposalError(it, exception)
            }
        }
    }

    /**
     * Handles exceptions in screen disposal.
     */
    protected open fun onScreenDisposalError(screen: ExScreen<G>, exception: Throwable) {
        Gdx.app.error("KTX", "Unable to dispose of $screen.", exception)
    }
}

/**
 * Abstract base class for extended screens, has a containing game.
 */
abstract class ExScreen<G : ExGame<G>>(val game: G) {
    init {
        // Add self owned screens
        game.owned += this
    }

    open fun dispose() = Unit
    open fun show() = Unit
    open fun exit() = Unit
    open fun enter() = Unit
    open fun hide() = Unit
    open fun pause() = Unit
    open fun render(delta: Float) = Unit
    open fun resize(width: Int, height: Int) = Unit
    open fun resume() = Unit
}

/**
 * Pushes the screen to the containing game.
 */
fun <G : ExGame<G>> ExScreen<G>.pushScreen(screen: ExScreen<G>) =
        game.pushScreen(screen)

/**
 * Pops the top screen of the containing game.
 */
fun <G : ExGame<G>> ExScreen<G>.popScreen() =
        game.popScreen()

/**
 * Pops the top screen of the containing game.
 */
fun <G : ExGame<G>> ExScreen<G>.popScreenIfTop() =
        if (game.currentScreen == this)
            game.popScreen()
        else
            Unit

/**
 * Gets the parents of this screen or null if not in stack.
 */
fun <G : ExGame<G>> ExScreen<G>.parents() =
        game.screens.let {
            val ix = it.indexOf(this)
            if (ix >= 0)
                it.subList(0, ix)
            else
                null
        }

/**
 * Gets the first parent of the screen or null if not in stack.
 */
fun <G : ExGame<G>> ExScreen<G>.parent() =
        parents()?.firstOrNull()

/**
 * Gets the first parent of type [T] or null if not in stack.
 */
inline fun <reified T> ExScreen<*>.parentOfType() =
        game.screens.let {
            val ix = it.indexOf(this)
            if (ix >= 0)
                it.subList(0, ix).filterIsInstance<T>().firstOrNull()
            else
                null
        }