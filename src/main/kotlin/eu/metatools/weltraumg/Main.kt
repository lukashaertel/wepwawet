package eu.metatools.weltraumg

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Input
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.g2d.Batch
import com.badlogic.gdx.graphics.glutils.ShapeRenderer
import com.badlogic.gdx.scenes.scene2d.Actor
import com.badlogic.gdx.scenes.scene2d.Stage
import eu.metatools.kepler.Gravity
import eu.metatools.kepler.Local
import eu.metatools.kepler.math.DiscreteCoupling
import eu.metatools.kepler.simulator.*
import eu.metatools.kepler.tools.Vec
import eu.metatools.net.ImplicitBinding
import eu.metatools.net.jgroups.BindingChannel
import eu.metatools.net.jgroups.BindingCoder
import eu.metatools.net.jgroups.ValueReceiver
import eu.metatools.voronois.tools.StageScreen
import eu.metatools.voronois.tools.dropRight
import eu.metatools.voronois.tools.popScreen
import eu.metatools.weltraumg.Main.Companion.shapeRenderer
import eu.metatools.wepwawet.*
import ktx.actors.onClick
import ktx.actors.plus
import ktx.scene2d.button
import ktx.scene2d.label
import org.apache.commons.math3.util.FastMath.sqrt
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

interface TimeInvalidated {
    fun invalidate(currentTime: Double)
}

fun Receiver.gravity(from: Body) {
    accPos(Gravity.acc(from.pos, from.mass, pos))
}

fun Receiver.local(force: Vec, pos: Vec) {
    accPos(Local.acc(mass, rot, force))
    accRot(Local.accRot(mass, rot, force, pos - com))
}

/**
 * Root object of the game.
 */
class Root(container: Container) : Entity(container), Drawable, TimeInvalidated {
    val simulator = Simulator(object : Universal {
        override fun universal(on: Receiver, other: List<Body>, t: Double) {
            for (o in other)
                if ((o.pos - on.pos).squaredLength > 5.0 * 5.0)
                    on.gravity(o)
        }
    })

    val reg: Registration = simulator.register(object : Definition(
            bounds = 20.0,
            pos = Vec(300, 300),
            mass = 1e15) {
        override val time: Double
            get() = container.seconds()
    })

    override fun invalidate(currentTime: Double) {
        simulator.release(currentTime - 10.0)
    }

    val spawn by impulse { owner: Author ->
        create(::Player, owner)
    }

    override fun draw(batch: Batch, parentAlpha: Float) {
        batch.end()
        shapeRenderer.begin()
        shapeRenderer.transformMatrix = batch.transformMatrix.cpy()
        shapeRenderer.projectionMatrix = batch.projectionMatrix.cpy()
        shapeRenderer.color = Color.WHITE

        val x = reg.pos.x.toFloat()
        val y = reg.pos.y.toFloat()

        // Render ship triangle
        shapeRenderer.circle(x, y, reg.bounds.toFloat())

        shapeRenderer.end()
        batch.begin()
    }

}


const val shipFactor = 1e14

// TODO: Nachladen über serilalisierung

/**
 * A player, spawn new one with 'B', move with 'ASDW', shoot at mouse cursor with left mouse button.
 */
class Player(container: Container, owner: Author) : Entity(container), Drawable {

    val all get() = container.findAuto<Root>()

    val reg: Registration = all!!.simulator.register(object : Definition(
            bounds = 5.0,
            mass = 1.0 * shipFactor,
            pos = Vec(50, 50),
            posDot = Vec.right * 10
    ) {
        override val time: Double
            get() = container.seconds()

        override fun local(on: Receiver, t: Double) {
            on.local(Vec.right * 30.0 * prograde[t] * shipFactor, Vec.zero)
            on.local(Vec.down * lateral[t] * shipFactor, Vec.left)
            on.local(Vec.up * lateral[t] * shipFactor, Vec.right)
        }
    })

    override fun draw(batch: Batch, parentAlpha: Float) {
        batch.end()
        shapeRenderer.begin()
        shapeRenderer.transformMatrix = batch.transformMatrix.cpy()
        shapeRenderer.projectionMatrix = batch.projectionMatrix.cpy()
        shapeRenderer.color = Color.WHITE

        val x = reg.pos.x.toFloat()
        val y = reg.pos.y.toFloat()
        val rot = reg.rot

        // Render ship triangle
        val p1 = (Vec.left + Vec.up).times(reg.bounds / sqrt(2.0)).rotate(rot)
        val p2 = (Vec.right).times(reg.bounds).rotate(rot)
        val p3 = (Vec.left + Vec.down).times(reg.bounds / sqrt(2.0)).rotate(rot)
        shapeRenderer.triangle(
                x + p1.x.toFloat(),
                y + p1.y.toFloat(),
                x + p2.x.toFloat(),
                y + p2.y.toFloat(),
                x + p3.x.toFloat(),
                y + p3.y.toFloat())

        shapeRenderer.end()
        batch.begin()
    }

    val owner by key(owner)

    val prograde = DiscreteCoupling(0.0, invalidating = reg::notifyChange)

    val fwd by impulse { ->
        prograde[container.seconds()] = 1.0
    }

    val bwd by impulse { ->
        prograde[container.seconds()] = -1.0
    }
    val stop by impulse { ->
        prograde[container.seconds()] = 0.0
    }

    val lateral = DiscreteCoupling(0.0, invalidating = reg::notifyChange)

    val right by impulse { ->
        lateral[container.seconds()] = -1.0
    }
    val left by impulse { ->
        lateral[container.seconds()] = 1.0
    }
    val stopR by impulse { ->
        lateral[container.seconds()] = 0.0
    }
}

class Main(game: Game) : StageScreen<Game>(game) {
    companion object {
        val shapeRenderer by lazy { ShapeRenderer().apply { setAutoShapeType(true) } }
    }

    /**
     * Author, as initialized by the games settings.
     */
    private val author = Author.random()

    /**
     * History of this game screen.
     */
    private var history = History(0L, listOf())

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
    private val autosaveStream by lazy {
        DataOutputStream(FileOutputStream(File.createTempFile("autosave_", ".stream", File("saves"))))
    }
    /**
     * If given at boot, will load a previous stream.
     */
    var load: File? = null

    var save = false

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
                    val message = Call(time.timestep, time.inner, time.author, id, method, arg)
                    if (save)
                        coder.encode(autosaveStream, message)

                    history = history.copy(calls = history.calls + message)
                    channel.send(null, message)
                }
            }
        }

        // Connect network to container.
        channel.valueReceiver = object : ValueReceiver {
            override fun receive(source: Message, message: Any?) {
                synchronized(container) {
                    when (message) {
                        is Call -> {
                            history = history.copy(calls = history.calls + message)
                            container.receive(message)
                        }
                    }
                }
            }

            override fun getState(): History {
                return history
            }

            override fun setState(state: Any?) {
                history = state as History
            }
        }

        // Join cluster
        channel.connect("Cluster")

        // Reset history and entity.
        history = History(System.currentTimeMillis(), listOf())

        // Trigger getting the game state.
        channel.getState(null, 10000)

        // Initialize the root container
        root = container.init(history.init, Author.MIN_VALUE, ::Root)

        // Run all calls.
        for (call in history.calls) {
            container.receive(call)
            println("H>$call")
        }

        // If load file given, load into container.
        if (load != null && load!!.exists()) {
            val stream = DataInputStream(FileInputStream(load))
            synchronized(container) {
                while (stream.available() > 0)
                    (coder.decode(stream) as? Call)?.let { message ->
                        history = history.copy(calls = history.calls + message)
                        container.receive(message)
                    }
            }

            load = null
        }


        container.time = System.currentTimeMillis()
        root.spawn(author)
    }

    override fun hide() {
        channel.close()
    }


    val mine get() = container.findAuto<Player>(author)

    var lastFrequent: Long = -1
    var lastSometimes: Long = -1
    var lastA = false
    var lastS = false
    var lastD = false
    var lastW = false
    override fun render(delta: Float) {
        synchronized(container) {
            container.revise(System.currentTimeMillis())

            if (mine == null && Gdx.input.isKeyJustPressed(Input.Keys.B)) {
                root.spawn(author)
            }

            mine?.let {
                val nowA = Gdx.input.isKeyPressed(Input.Keys.A)
                val nowS = Gdx.input.isKeyPressed(Input.Keys.S)
                val nowD = Gdx.input.isKeyPressed(Input.Keys.D)
                val nowW = Gdx.input.isKeyPressed(Input.Keys.W)


                if (nowS && !lastS)
                    it.bwd()

                if (!nowS && lastS)
                    it.stop()

                if (nowW && !lastW)
                    it.fwd()

                if (!nowW && lastW)
                    it.stop()

                if (nowA && !lastA)
                    it.left()

                if (!nowA && lastA)
                    it.stopR()

                if (nowD && !lastD)
                    it.right()

                if (!nowD && lastD)
                    it.stopR()

                lastA = nowA
                lastS = nowS
                lastD = nowD
                lastW = nowW
            }
        }

        super.render(delta)
    }

    override val stage = Stage(game.viewport).apply {
        this + object : Actor() {

            override fun draw(batch: Batch, parentAlpha: Float) {
                synchronized(container) {
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
                        if (e is TimeInvalidated)
                            e.invalidate(container.seconds())
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