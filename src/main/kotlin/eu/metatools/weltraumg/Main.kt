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
import eu.metatools.wepwawet.Container
import eu.metatools.wepwawet.Entity
import eu.metatools.wepwawet.Revision
import eu.metatools.wepwawet.findAuto
import kotlinx.serialization.Serializable
import ktx.actors.onClick
import ktx.actors.plus
import ktx.scene2d.button
import ktx.scene2d.label
import org.jgroups.Global
import org.jgroups.Message
import java.io.*
import java.util.*
import kotlin.properties.Delegates.notNull

/**
 * Transferred call.
 */
@Serializable
data class Call(val time: Int,
                val inner: Short,
                val author: Byte,
                val ids: List<Any?>,
                val call: Byte,
                val arg: Any?)

/**
 * History of all calls.
 */
@Serializable
data class History(val calls: List<Call>)

/**
 * Variable storing the history of the game.
 */
var history = History(listOf())

fun Container.receive(call: Call) {
    receive(Revision(call.time, call.inner, call.author), call.ids, call.call, call.arg)
}

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
 * Milliseconds of year.
 */
fun msoy() = (System.currentTimeMillis() - Date(2018, 1, 1).time).toInt()

/**
 * Get the time of the revision in ms.
 */
fun Revision.asMs() = time


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
    val spawn by impulse { owner: Byte, start: Pos ->
        create(::Player, owner, start)
    }
}

/**
 * A player, spawn new one with 'B', move with 'ASDW', shoot at mouse cursor with left mouse button.
 */
class Player(container: Container, owner: Byte, start: Pos) : Entity(container), Drawable, Frequent {
    val all get() = container.findAuto<Root>()

    override fun call() {
        all?.let {
            if (owner == container.author)
                it.restock()
        }
    }

    override fun draw(batch: Batch, parentAlpha: Float) {
        batch.color = Color.RED.cpy().lerp(Color.WHITE, health.toFloat() / 100f)

        batch.draw(Main.white, pos.x.toFloat() - 10f, pos.y.toFloat() - 10f, 20f, 20f)
    }

    val owner by key(owner)

    var pos by prop(start)

    var health by prop(100)

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
            delete(this)
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
class Bullet(container: Container, start: Int, owner: Byte, val pos: Pos, val vel: Pos) : Entity(container), Drawable, Always {
    val start by key(start)

    val owner by key(owner)

    val destroy by impulse { ->
        delete(this)
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

    var author: Byte = 0

    var load: File? = null

    private var coder by notNull<BindingCoder>()

    private var channel by notNull<BindingChannel>()

    private var container by notNull<Container>()

    private var ex by notNull<Root>()

    private val autosaveStream = DataOutputStream(FileOutputStream(File.createTempFile("autosave_", ".stream", File("saves"))))

    override fun show() {
        // Get coder.
        coder = BindingCoder(listOf(
                ImplicitBinding(Call::class),
                ImplicitBinding(History::class)))

        // Get and configure channel.
        channel = BindingChannel(Global.DEFAULT_PROTOCOL_STACK, coder)
        channel.discardOwnMessages = true

        container = object : Container(author) {
            override fun dispatch(time: Revision, id: List<Any?>, call: Byte, arg: Any?) {
                synchronized(container) {
                    val message = Call(time.time, time.inner, time.author, id, call, arg)
                    coder.encode(autosaveStream, message)

                    history = history.copy(history.calls + message)
                    channel.send(null, message)
                }
            }
        }

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

        channel.connect("Chat cluster")

        ex = container.init(::Root)

        channel.getState(null, 10000)

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

    var randomly = false


    val mine get() = container.findAuto<Player>(listOf(author))

    var lastFrequent: Int = -1
    var lastSometimes: Int = -1
    var lastDown = false
    override fun render(delta: Float) {
        synchronized(container) {
            container.time = msoy()
            container.repo.softUpper = container.rev()

            if (mine == null && Gdx.input.isKeyJustPressed(Input.Keys.B)) {
                ex.spawn(author, Pos((Math.random() * 300).toInt(), (Math.random() * 300).toInt()))
            }

            mine?.let {
                if (Gdx.input.isKeyPressed(Input.Keys.A) || (randomly && randomTrue(.5)))
                    it.left()
                if (Gdx.input.isKeyPressed(Input.Keys.D) || (randomly && randomTrue(.5)))
                    it.right()
                if (Gdx.input.isKeyPressed(Input.Keys.W) || (randomly && randomTrue(.5)))
                    it.up()
                if (Gdx.input.isKeyPressed(Input.Keys.S) || (randomly && randomTrue(.5)))
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
                    for (i in 0..(ex.shotPower / 10)) {
                        batch.color = Color.GREEN
                        batch.draw(white, (i * 15).toFloat(), 0f, 5f, 5f)
                    }

                    val time = msoy()
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
        this + dropLeft {
            button {
                val l = label("Play randomly")
                onClick {
                    if (!randomly) {
                        l.setText("Stop")
                        randomly = true
                    } else {
                        l.setText("Play randomly")
                        randomly = false
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