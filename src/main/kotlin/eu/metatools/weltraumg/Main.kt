package eu.metatools.weltraumg

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Input
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.Pixmap
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.Batch
import com.badlogic.gdx.graphics.g2d.TextureRegion
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

/**
 * A player, spawn new one with 'B', move with 'ASDW', shoot at mouse cursor with left mouse button.
 */
class Player(container: Container, owner: Author) : Entity(container), Drawable {
    companion object {
        val texture by lazy { TextureRegion(Texture("rarr.png")) }
    }

    val all get() = container.findAuto<Root>()


    override fun draw(batch: Batch, parentAlpha: Float) {
        val pos = body.pos(container.seconds())
        val rot = body.rot(container.seconds())
        batch.draw(texture,
                pos.x.toFloat(), pos.y.toFloat(),
                texture.regionWidth / 2.0f, texture.regionHeight / 2.0f,
                texture.regionWidth.toFloat(), texture.regionHeight.toFloat(),
                1.0f, 1.0f,
                Math.toDegrees(rot).toFloat())
    }

    val owner by key(owner)

    val body = ComplexBody(container.seconds(), Vec.zero, 0.0, Vec.zero, 0.0, 1.0 / 50)

    val prograde = Settable(0.0, body)

    val lateral = Settable(0.0, body)

    init {
        body.masses = listOf(constT(Vec.zero to 0.1))
        body.accelerators = listOf(
                accLocal(body, constT(Vec.zero), constT(Vec.right), prograde * 3.0),
                accLocal(body, constT(Vec.right * 0.1), constT(Vec.up), lateral * 0.5),
                accLocal(body, constT(Vec.left * 0.1), constT(Vec.down), lateral * 0.5))
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