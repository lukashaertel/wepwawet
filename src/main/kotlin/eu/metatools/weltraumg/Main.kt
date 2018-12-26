package eu.metatools.weltraumg

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Input
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.g2d.Batch
import com.badlogic.gdx.graphics.glutils.ShapeRenderer
import com.badlogic.gdx.scenes.scene2d.Actor
import com.badlogic.gdx.scenes.scene2d.Stage
import eu.metatools.kepler.*
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
import org.apache.commons.math3.ode.nonstiff.DormandPrince853Integrator
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
 * Root object of the game.
 */
class Root(container: Container) : Entity(container) {
    val spawn by impulse { owner: Author ->
        create(::Player, owner)
    }
}

val star = Triple(Vec(400, 400), 1e+16, 15)

val tracer = 60.0

interface TimeInvalidated {
    fun invalidate(currentTime: Double)
}


/**
 * A player, spawn new one with 'B', move with 'ASDW', shoot at mouse cursor with left mouse button.
 */
class Player(container: Container, owner: Author) : Entity(container), Drawable, TimeInvalidated {

    val all get() = container.findAuto<Root>()

    val sbs: SingleBodySimulation = SingleBodySimulation().apply {
        addAcc {
            // Constant gravity effect.
            Gravity.acc(Vec(400.0, 400.0), 1e+16, pos)
        }

        addAcc {
            Local.acc(1.0, rot, Vec.right * 30.0 * prograde[t])

        }

        addAcc {
            Local.acc(1.0, rot, Vec.up * lateral[t])
        }

        addAcc {
            Local.acc(1.0, rot, Vec.down * lateral[t])
        }

        addAccRot {
            Local.accRot(1.0, rot, Vec.right * 30.0 * prograde[t], Vec.zero)
        }

        addAccRot {
            Local.accRot(1.0, rot, Vec.up * lateral[t], Vec.right)
        }

        addAccRot {
            Local.accRot(1.0, rot, Vec.down * lateral[t], Vec.left)
        }
    }

    val dpi = DormandPrince853Integrator(1.0e-8, 100.0, 1.0e-10, 1.0e-10)

    val cbi = ContinuousBodyIntegrator(sbs, dpi, Context(container.seconds(), Vec(400.0, 300.0), 0.0, Vec(90.0, 0.0), 0.0))

    override fun invalidate(currentTime: Double) {
        cbi.drop(currentTime - tracer)
    }

    override fun draw(batch: Batch, parentAlpha: Float) {
        batch.end()
        shapeRenderer.begin()
        shapeRenderer.transformMatrix = batch.transformMatrix.cpy()
        shapeRenderer.projectionMatrix = batch.projectionMatrix.cpy()
        shapeRenderer.color = Color.WHITE

        val ct = container.seconds()
        val (t, pos, rot, vel, velRot) = cbi.integrate(ct)
        val x = pos.x.toFloat()
        val y = pos.y.toFloat()

        // Render ship triangle
        val p1 = (Vec.left + Vec.up).times(10.0).rotate(rot)
        val p2 = (Vec.right).times(15.0).rotate(rot)
        val p3 = (Vec.left + Vec.down).times(10.0).rotate(rot)
        shapeRenderer.triangle(
                x + p1.x.toFloat(),
                y + p1.y.toFloat(),
                x + p2.x.toFloat(),
                y + p2.y.toFloat(),
                x + p3.x.toFloat(),
                y + p3.y.toFloat())

        // Render calculated positions
        val samples = cbi.calculated(from = ct - tracer).values
        samples.windowed(2) { l ->
            val (l1, l2) = l
            shapeRenderer.line(
                    l1.pos.x.toFloat(), l1.pos.y.toFloat(),
                    l2.pos.x.toFloat(), l2.pos.y.toFloat(),
                    Color.GREEN, Color.GREEN)
        }

        shapeRenderer.end()
        batch.begin()
    }

    val owner by key(owner)

    val prograde = Coupling(0.0, invalidating = cbi::reset)

    val lateral = Coupling(0.0, invalidating = cbi::reset)

    val right by impulse { ->
        lateral[container.seconds()] = -1.0
    }
    val left by impulse { ->
        lateral[container.seconds()] = 1.0
    }
    val stopR by impulse { ->
        lateral[container.seconds()] = 0.0
    }

    val fwd by impulse { ->
        prograde[container.seconds()] = 1.0
    }

    val bwd by impulse { ->
        prograde[container.seconds()] = -1.0
    }
    val stop by impulse { ->
        prograde[container.seconds()] = 0.0
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

                    batch.end()
                    shapeRenderer.begin()
                    shapeRenderer.projectionMatrix = batch.projectionMatrix.cpy()
                    shapeRenderer.transformMatrix = batch.transformMatrix.cpy()
                    shapeRenderer.color = Color.WHITE
                    shapeRenderer.circle(star.first.x.toFloat(), star.first.y.toFloat(), star.third.toFloat())

                    shapeRenderer.end()
                    batch.begin()

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