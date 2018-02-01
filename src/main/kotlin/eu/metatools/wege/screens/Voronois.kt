package eu.metatools.wege.screens

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Input
import com.badlogic.gdx.InputProcessor
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.OrthographicCamera
import com.badlogic.gdx.maps.MapObject
import com.badlogic.gdx.maps.objects.RectangleMapObject
import com.badlogic.gdx.maps.objects.TextureMapObject
import com.badlogic.gdx.maps.tiled.TiledMap
import com.badlogic.gdx.maps.tiled.TiledMapTileLayer
import com.badlogic.gdx.maps.tiled.renderers.OrthogonalTiledMapRenderer
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.scenes.scene2d.Stage
import com.badlogic.gdx.scenes.scene2d.ui.Label
import com.badlogic.gdx.utils.viewport.ScreenViewport
import eu.metatools.wege.*
import eu.metatools.wege.tools.*
import ktx.actors.onClick
import ktx.actors.plus
import ktx.async.ktxAsync
import ktx.scene2d.button
import ktx.scene2d.label
import kotlin.math.floor
import kotlin.properties.Delegates

/**
 * Big value for marking impassibility.
 */
val aLot = 10000

/**
 * Maximum cost.
 */
val max = 50

/**
 * The paint that will be distributed by the distance painter.
 */
data class Paint(override val cost: Cost, val color: Color) : Prop

/**
 * Paints fields with color given a cost-map.
 */
data class DistancePainter(val color: Color, val costMap: Field<Int>) : Distributor<Paint> {
    override val skipVisited: Boolean
        get() = false

    override fun valid(from: Paint, at: XY): Boolean {
        return from.cost <= max
    }

    override fun next(from: Paint, at: XY, to: XY): Paint {
        return Paint(from.cost + (at manLen to) * (costMap[to] ?: 0), from.color)
    }
}

/**
 * Initializes a distance painter at the given coordinate, with the given color and the given cost-map.
 */
fun distancePainterInit(at: XY, color: Color, costMap: Field<Int>) =
        Init(at, Paint(0, color), DistancePainter(color, costMap))

/**
 * Static size of objects in the tiled map.
 */
val objectSize = 16f

/**
 * Computes the field an object is on.
 */
fun MapObject.field() = when (this) {
    is RectangleMapObject -> rectangle.getCenter(Vector2()).let {
        floor(it.x / objectSize).toInt() to floor(it.y / objectSize).toInt()
    }
    is TextureMapObject -> floor(x / objectSize).toInt() to floor(y / objectSize).toInt()
    else -> error("Cannot handle this object type")
}

/**
 * Displays a map with computed overlay by cost based distribution.
 */
class Voronois(val game: WeltraumGefecht) : StageScreen() {
    override val processor = object : InputProcessor {
        override fun touchUp(screenX: Int, screenY: Int, pointer: Int, button: Int) = false

        override fun mouseMoved(screenX: Int, screenY: Int): Boolean {
            if (!initialized)
                return false

            overlay?.apply {
                val world = viewport.unproject(Vector2(screenX.toFloat(), screenY.toFloat()))
                val x = floor(world.x / objectSize).toInt()
                val y = floor(world.y / objectSize).toInt()

                valueLabel.setText("$x, $y, ${propfield[x to y]}")
            }

            return true
        }

        override fun keyTyped(character: Char) = false

        override fun scrolled(amount: Int): Boolean {
            viewport.unitsPerPixel *= Math.pow(1.1, amount.toDouble()).toFloat()
            viewport.update(Gdx.graphics.width, Gdx.graphics.height)
            return true
        }

        override fun keyUp(keycode: Int) = false

        override fun touchDragged(screenX: Int, screenY: Int, pointer: Int): Boolean {
            viewport.camera.translate(
                    -Gdx.input.deltaX.toFloat() * viewport.unitsPerPixel,
                    Gdx.input.deltaY.toFloat() * viewport.unitsPerPixel, 0f)
            return true
        }

        override fun keyDown(keycode: Int): Boolean {
            if (Gdx.input.isKeyJustPressed(Input.Keys.SPACE))
                overlay?.apply {
                    isVisible = !isVisible
                }
            return true
        }

        override fun touchDown(screenX: Int, screenY: Int, pointer: Int, button: Int) = false
    }

    var valueLabel by Delegates.notNull<Label>()

    override val stage = Stage(game.viewport).apply {
        this + dropLeft {
            label("Value")
            valueLabel = label("...")

        }

        this + dropRight {
            button {
                label("Back")
                onClick { game.setScreen<Menu>() }
            }
        }
    }

    var tiledMap by Delegates.notNull<TiledMap>()
    var tiledRenderer by Delegates.notNull<OrthogonalTiledMapRenderer>()
    var initialized = false

    val viewport = ScreenViewport().apply {
        unitsPerPixel = 6f / objectSize
    }

    var propfield by Delegates.notNull<Field<Paint?>>()
    val overlay
        get() =
            tiledMap.layers.firstOrNull { it.properties["overlay"] as? Boolean ?: false }

    init {
        ktxAsync {
            tiledMap = game.storage.load("World.tmx")
            tiledRenderer = ExtendedOrthogonalTiledMapRenderer(tiledMap)
            val layers = tiledMap.layers.filterIsInstance<TiledMapTileLayer>()
            val objects = tiledMap.layers.flatMap { it.objects }

            val width = layers.map { it.width }.max() ?: 0
            val height = layers.map { it.height }.max() ?: 0

            val costMap = Field.create(width to height, aLot)

            fun weightAt(x: Int, y: Int) =
                    layers.asReversed().mapNotNull {
                        val cell = it.getCell(x, y)
                        if (cell == null)
                            null
                        else
                            it.properties["weight"] as? Int
                    }.firstOrNull()

            for (y in 0 until width)
                for (x in 0 until height)
                    costMap[x to y] = weightAt(x, y) ?: aLot

            val painters = objects
                    .mapNotNull { o ->
                        (o.properties["color"] as? Color)?.let {
                            distancePainterInit(o.field(), it, costMap)
                        }
                    }

            propfield = propField(painters, width to height, XY::cross).await()

            val overlay = layers.last()
            for ((p, v) in propfield.points())
                if (v == null)
                    overlay.setCell(p.first, p.second, null)
                else {
                    val cell = overlay.getCell(p.first, p.second) ?: continue
                    val factor = v.cost.toFloat() / max.toFloat()
                    val inv = 1f - factor * factor * factor * factor
                    overlay.setCell(p.first, p.second, ColoredCell(cell, v.color, inv))
                }

            initialized = true
        }
    }

    override fun resize(width: Int, height: Int) {
        viewport.update(width, height)
    }

    override fun render(delta: Float) {
        if (!initialized)
            return

        // Render map.
        viewport.apply()
        tiledRenderer.setView(viewport.camera as OrthographicCamera)
        tiledRenderer.render()

        // Render stage.
        super.render(delta)
    }
}