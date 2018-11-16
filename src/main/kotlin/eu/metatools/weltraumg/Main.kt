package eu.metatools.weltraumg

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Input
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.Pixmap
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.Batch
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.scenes.scene2d.Actor
import com.badlogic.gdx.scenes.scene2d.Stage
import eu.metatools.common.randomTrue
import eu.metatools.net.ImplicitBinding
import eu.metatools.net.jgroups.BindingChannel
import eu.metatools.net.jgroups.BindingCoder
import eu.metatools.net.jgroups.ValueReceiver
import eu.metatools.voronois.tools.StageScreen
import eu.metatools.voronois.tools.dropLeft
import eu.metatools.voronois.tools.dropRight
import eu.metatools.voronois.tools.popScreen
import eu.metatools.wepwawet.*
import kotlinx.serialization.Serializable
import ktx.actors.onClick
import ktx.actors.plus
import ktx.scene2d.button
import ktx.scene2d.label
import org.jgroups.Global
import org.jgroups.Message
import java.io.*
import kotlin.properties.Delegates.notNull


/**
 * Something that's drawable.
 */
interface Drawable {
    fun draw(batch: Batch, parentAlpha: Float)
}

/**
 * A periodic call, called from the main game loop.
 */
interface Periodic {
    fun call()
}

/**
 * Called every frame.
 */
interface Always : Periodic

/**
 * Called multiple times per second.
 */
interface Frequent : Periodic

/**
 * Called every second.
 */
interface Sometimes : Periodic


/**
 * Integer-position.
 */
@Serializable
data class Pos(val x: Int, val y: Int) {
    companion object {
        val left = Pos(-1, 0)
        val right = Pos(1, 0)
        val up = Pos(0, 1)
        val down = Pos(0, -1)
    }

    operator fun plus(other: Pos) = Pos(x + other.x, y + other.y)
}

/**
 * Root object of the game.
 */
class Root(container: Container) : Entity(container) {
    var shotPower by prop(0)

    val restock by impulse { ->
        if (shotPower < 100)
            shotPower += 1
    }
    val deplete by impulse { ->
        shotPower -= 10
    }
    val spawn by impulse { owner: Author, start: Pos ->
        create(::Player, owner, start)
    }
}

/**
 * A player, spawn new one with 'B', move with 'ASDW', shoot at mouse cursor with left mouse button.
 */
class Player(container: Container, owner: Author, start: Pos) : Entity(container), Drawable, Frequent {
    val all get() = container.findAuto<Root>()

    override fun call() {
        all?.let {
            if (owner == container.author)
                it.restock()
        }
    }

    override fun draw(batch: Batch, parentAlpha: Float) {
        val src = when (color) {
            0 -> Color.RED
            1 -> Color.YELLOW
            2 -> Color.GREEN
            3 -> Color.CYAN
            4 -> Color.BLUE
            else -> Color.PURPLE
        }
        batch.color = Color.RED.cpy().lerp(src, health.toFloat() / 100f)

        batch.draw(Main.white, pos.x.toFloat() - 10f, pos.y.toFloat() - 10f, 20f, 20f)
    }

    val owner by key(owner)

    var pos by prop(start)

    var health by prop(100)

    var color by prop(0)

    val right by impulse { ->
        pos += Pos.right
    }

    val left by impulse { ->
        pos += Pos.left
    }

    val up by impulse { ->
        pos += Pos.up
    }

    val down by impulse { ->
        pos += Pos.down
    }

    val damage by impulse { ->
        health -= 10
        if (health <= 0)
            delete()
    }

    val changeColor by impulse { ->
        color = (color + 1) % 6
    }


    val shoot by impulse { dx: Int, dy: Int ->
        all?.let {
            if (it.shotPower > 0) {
                it.deplete()

                val f = Math.sqrt(dx.toDouble() * dx.toDouble() + dy.toDouble() * dy.toDouble())

                if (f != 0.0)
                    create(::Bullet, container.rev().asMs(), owner, pos, Pos(
                            (100.0 * dx.toDouble() / f).toInt(),
                            (100.0 * dy.toDouble() / f).toInt()))
            }
        }
    }
}

/**
 * A bullet, hit tests with non-owner player in every frame.
 */
class Bullet(container: Container, start: Long, owner: Author, val pos: Pos, val vel: Pos) : Entity(container), Drawable, Always {
    val start by key(start)

    val owner by key(owner)

    val destroy by impulse { ->
        delete()
    }

    val dt get() = (container.rev().asMs() - start) / 1000.0f

    val px get() = pos.x.toFloat() + dt * vel.x.toFloat()
    val py get() = pos.y.toFloat() + dt * vel.y.toFloat()

    override fun draw(batch: Batch, parentAlpha: Float) {
        batch.color = Color.YELLOW
        batch.draw(Main.white, px - 2.5f, py - 2.5f, 5f, 5f)
    }

    override fun call() {
        if (start + 20000 < container.rev().asMs())
            destroy()

        for (e in container.index.values)
            if (e is Player)
                if (e.owner != owner
                        && Math.abs(e.pos.x - px) < 10
                        && Math.abs(e.pos.y - py) < 10) {
                    e.damage()
                    destroy()
                    break
                }

    }
}

class Main(game: Game) : StageScreen<Game>(game) {
    companion object {
        val white by lazy {
            Texture(Pixmap(1, 1, Pixmap.Format.RGB888).apply {
                drawPixel(0, 0, Color.WHITE.toIntBits())
            })
        }
    }

    /**
     * Author, as initialized by the games settings.
     */
    private val author = game.identity

    /**
     * History of this game screen.
     */
    private var history = History(listOf())

    /**
     * Coder for the serializable classes.
     */
    private var coder by notNull<BindingCoder>()

    /**
     * Transport channel.
     */
    private var channel by notNull<BindingChannel>()

    /**
     * Connector.
     */
    private var container by notNull<Container>()

    /**
     * Root entity of the screen.
     */
    private var root by notNull<Root>()

    /**
     * Stream to save to.
     */
    private val autosaveStream = DataOutputStream(FileOutputStream(File.createTempFile("autosave_", ".stream", File("saves"))))

    /**
     * If given at boot, will load a previous stream.
     */
    var load: File? = null

    override fun show() {
        // Get coder.
        coder = BindingCoder(listOf(
                ImplicitBinding(Call::class),
                ImplicitBinding(History::class)))

        // Get and configure channel.
        channel = BindingChannel(Global.DEFAULT_PROTOCOL_STACK, coder)
        channel.discardOwnMessages = true


        // Initialize container on the given author, dispatching to the channel.
        container = object : Container(author) {
            override fun dispatch(time: Revision, id: List<Any?>, method: Method, arg: Any?) {
                synchronized(container) {
                    val message = Call(time.time, time.inner, time.author, id, method, arg)
                    coder.encode(autosaveStream, message)

                    history = history.copy(history.calls + message)
                    channel.send(null, message)
                }
            }
        }

        // Connect network to container.
        channel.valueReceiver = object : ValueReceiver {
            override fun receive(source: Message, message: Any?) {
                when (message) {
                    is Call -> {
                        synchronized(container) {
                            history = history.copy(history.calls + message)
                            container.receive(message)
                        }
                    }
                }
            }

            override fun getState(): History {
                return history
            }

            override fun setState(state: Any?) {
                synchronized(container) {
                    history = state as History
                    for (call in history.calls) {
                        container.receive(call)
                        println("H>$call")
                    }
                }
            }
        }

        // Join cluster
        channel.connect("Cluster")

        // Reset history and entity.
        history = History(listOf())
        root = container.init(::Root)

        // Trigger getting the game state.
        channel.getState(null, 10000)

        // If load file given, load into container.
        if (load != null && load!!.exists()) {
            val stream = DataInputStream(FileInputStream(load))
            synchronized(container) {
                while (stream.available() > 0)
                    (coder.decode(stream) as? Call)?.let { message ->
                        history = history.copy(history.calls + message)
                        container.receive(message)
                    }
            }
            load = null
        }
    }

    override fun hide() {
        channel.close()
    }


    val mine get() = container.findAuto<Player>(author)

    var lastFrequent: Long = -1
    var lastSometimes: Long = -1
    var lastDown = false
    override fun render(delta: Float) {
        synchronized(container) {
            container.revise(System.currentTimeMillis())

            if (mine == null && Gdx.input.isKeyJustPressed(Input.Keys.B)) {
                root.spawn(author, Pos((Math.random() * 300).toInt(), (Math.random() * 300).toInt()))
            }

            mine?.let {
                if (Gdx.input.isKeyPressed(Input.Keys.A))
                    it.left()
                if (Gdx.input.isKeyPressed(Input.Keys.D))
                    it.right()
                if (Gdx.input.isKeyPressed(Input.Keys.W))
                    it.up()
                if (Gdx.input.isKeyPressed(Input.Keys.S))
                    it.down()

                val nowDown = Gdx.input.isButtonPressed(Input.Buttons.LEFT)
                if (nowDown && !lastDown) {
                    val coords = stage.root.screenToLocalCoordinates(Vector2(Gdx.input.x.toFloat(), Gdx.input.y.toFloat()))
                    it.shoot(coords.x.toInt() - it.pos.x, coords.y.toInt() - it.pos.y);
                }
                lastDown = nowDown

            }
        }

        super.render(delta)
    }

    override val stage = Stage(game.viewport).apply {
        this + object : Actor() {

            override fun draw(batch: Batch, parentAlpha: Float) {
                synchronized(container) {
                    for (i in 0..(this@Main.root.shotPower / 10)) {
                        batch.color = Color.GREEN
                        batch.draw(white, (i * 15).toFloat(), 0f, 5f, 5f)
                    }

                    val time = System.currentTimeMillis()
                    var cfreq = false
                    var csom = false
                    if (lastFrequent + 50 < time) {
                        lastFrequent = time
                        cfreq = true
                    }
                    if (lastSometimes + 1000 < time) {
                        lastSometimes = time
                        csom = true
                    }

                    for (e in container.index.values.toList()) {
                        if (e is Drawable)
                            e.draw(batch, parentAlpha)
                        if (e is Always)
                            e.call()
                        if (cfreq && e is Frequent)
                            e.call()
                        if (csom && e is Sometimes)
                            e.call()
                    }
                }
            }
        }

        this + dropRight {
            button {
                label("Back")
                onClick { popScreen() }
            }
        }
    }
}