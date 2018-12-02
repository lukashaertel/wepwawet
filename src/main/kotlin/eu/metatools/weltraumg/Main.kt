package eu.metatools.weltraumg

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Input
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.Pixmap
import com.badlogic.gdx.graphics.Texture
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

val star = ConstantBody(
        const(Vec.zero to 1e+16),
        const(Vec(400.0, 400.0)),
        const(0.0),
        const(Vec.zero),
        const(0.0),
        const(Vec.zero to 0.0))

val pred = 20
val predTime = 0.5

interface HasComplexBody {
    val body: ComplexBody
}

/**
 * A player, spawn new one with 'B', move with 'ASDW', shoot at mouse cursor with left mouse button.
 */
class Player(container: Container, owner: Author) : Entity(container), Drawable, HasComplexBody {

    val all get() = container.findAuto<Root>()


    override fun draw(batch: Batch, parentAlpha: Float) {
        batch.end()
        shapeRenderer.begin()
        shapeRenderer.transformMatrix = batch.transformMatrix.cpy()
        shapeRenderer.projectionMatrix = batch.projectionMatrix.cpy()
        shapeRenderer.color = Color.WHITE

        val ct = container.seconds()

        var lx = 0f
        var ly = 0f
        for (i in 0..pred) {
            val pos = body.pos(ct + i * predTime)
            val rot = body.rot(ct + i * predTime)
            val x = pos.x.toFloat()
            val y = pos.y.toFloat()

            if (i == 0) {
                val p1 = (Vec.left + Vec.up).times(20.0).rotate(rot)
                val p2 = (Vec.right).times(20.0).rotate(rot)
                val p3 = (Vec.left + Vec.down).times(20.0).rotate(rot)
                shapeRenderer.triangle(
                        x + p1.x.toFloat(),
                        y + p1.y.toFloat(),
                        x + p2.x.toFloat(),
                        y + p2.y.toFloat(),
                        x + p3.x.toFloat(),
                        y + p3.y.toFloat())
            } else {
                val ofx = Vec((x - lx).toDouble(), (y - ly).toDouble()).normal().normalized().times(5.0)
                shapeRenderer.line(lx, ly, x, y, Color.GREEN, Color.GREEN)
                shapeRenderer.line(lx, ly, lx + ofx.x.toFloat(), ly + ofx.y.toFloat(), Color.GREEN, Color.GREEN)
            }

            lx = x
            ly = y
        }

        shapeRenderer.end()
        batch.begin()
    }

    val owner by key(owner)

    override val body = ComplexBody(container.seconds(), Vec.zero, 0.0, Vec.right * 10.0, 0.0, 1.0 / 32.0)

    val prograde = Settable(0.0, body)

    val lateral = Settable(0.0, body)

    init {
        body.masses = listOf(const(Vec.zero to 1.0))
        body.accelerators = listOf(
                accGravity(star, body),
                accLocal(body, const(Vec.zero), const(Vec.right), { t -> prograde(t) * 30.0 }),
                accLocal(body, const(Vec.right * 0.1), const(Vec.up), { t -> lateral(t) * 8.0 }),
                accLocal(body, const(Vec.left * 0.1), const(Vec.down), { t -> lateral(t) * 8.0 }))
    }

    val right by impulse { ->
        lateral.set(container.seconds(), -1.0)
    }
    val left by impulse { ->
        lateral.set(container.seconds(), 1.0)
    }
    val stopR by impulse { ->
        lateral.set(container.seconds(), 0.0)
    }

    val fwd by impulse { ->
        prograde.set(container.seconds(), 1.0)
    }

    val bwd by impulse { ->
        prograde.set(container.seconds(), -1.0)
    }
    val stop by impulse { ->
        prograde.set(container.seconds(), 0.0)
    }
}

class Main(game: Game) : StageScreen<Game>(game) {
    companion object {
        val white by lazy {
            Texture(Pixmap(1, 1, Pixmap.Format.RGB888).apply {
                drawPixel(0, 0, Color.WHITE.toIntBits())
            })
        }
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
                    val starpos = star.pos(container.seconds())
                    shapeRenderer.color = Color.WHITE
                    shapeRenderer.circle(starpos.x.toFloat(), starpos.y.toFloat(), 50f)

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
                        if (e is HasComplexBody)
                            e.body.invalidateTo(container.seconds() - 10.0)
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