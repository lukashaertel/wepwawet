package eu.metatools.voronois.screens

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.InputAdapter
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.OrthographicCamera
import com.badlogic.gdx.maps.MapObject
import com.badlogic.gdx.maps.objects.PolygonMapObject
import com.badlogic.gdx.maps.objects.RectangleMapObject
import com.badlogic.gdx.maps.objects.TextureMapObject
import com.badlogic.gdx.maps.tiled.TiledMap
import com.badlogic.gdx.maps.tiled.TiledMapTileLayer
import com.badlogic.gdx.maps.tiled.renderers.OrthogonalTiledMapRenderer
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.scenes.scene2d.Stage
import com.badlogic.gdx.scenes.scene2d.ui.Label
import com.badlogic.gdx.utils.viewport.ScreenViewport
import com.badlogic.gdx.utils.viewport.Viewport
import eu.metatools.voronois.*
import eu.metatools.voronois.game.*
import eu.metatools.voronois.tools.*
import ktx.actors.onClick
import ktx.actors.plus
import ktx.async.ktxAsync
import ktx.scene2d.button
import ktx.scene2d.label
import java.util.*
import kotlin.math.floor
import kotlin.properties.Delegates

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

class AdvancedMap(val game: WeltraumGefecht, val source: String, val tileSize: Vector2) {
    /**
     * The tiled map this map is based on.
     */
    var tiledMap by Delegates.notNull<TiledMap>()

    /**
     * The renderer for the map.
     */
    var tiledRenderer by Delegates.notNull<OrthogonalTiledMapRenderer>()

    /**
     * All tile map layers.
     */
    val layers get() = List(tiledMap.layers.size()) { tiledMap.layers[it] }.filterIsInstance<TiledMapTileLayer>()

    val tilesets get() = tiledMap.tileSets

    val overlay get() = layers.firstOrNull { it.properties["overlay"] as? Boolean ?: false }

    val user get() = layers.firstOrNull { it.properties["user"] as? Boolean ?: false }

    /**
     * All objects.
     */
    val objects get() = tiledMap.layers.flatMap { it.objects }

    /**
     * Maximum width of the layers.
     */
    val width get() = layers.map { it.width }.max() ?: 0

    /**
     * Maximum height of the layers.
     */
    val height get() = layers.map { it.height }.max() ?: 0

    /**
     * Loads the map and intitializes the renderer.
     */
    suspend fun load() {
        tiledMap = game.storage.load(source)
        tiledRenderer = ExtendedOrthogonalTiledMapRenderer(tiledMap)
    }

    /**
     * Gets the property stack of the tile, i.e., all values of a property from top tile to bottom tile, where only
     * non-null values are regarded.
     */
    fun propertyStack(x: Int, y: Int, property: String) =
            (layers.size - 1 downTo 0).mapNotNull {
                layers[it].getCell(x, y)?.let {
                    it.tile.properties[property]
                }
            }

    /**
     * Gets the stack of tile types.
     */
    fun typeStack(x: Int, y: Int) =
            propertyStack(x, y, "type")

    /**
     * Gets the stack of regions.
     */
    fun regionStack(x: Int, y: Int) =
            propertyStack(x, y, "region")

    /**
     * Gets all spawn area objects.
     */
    val spawnAreas get() = objects.filterIsInstance<PolygonMapObject>()

    /**
     * Gets the number of all spawn areas.
     */
    val spawnAreaCount get() = spawnAreas.size

    /**
     * Picks a random location in the spawn area.
     */
    fun spawnAreaPick(id: Int, random: Random = Random()) =
            spawnAreas[id].polygon.let {
                // Initialize result vector.
                val r = Vector2()
                for (i in 0 until 100) {
                    // Set vector to center of rectangle.
                    it.boundingRectangle.getCenter(r)

                    if (i == 99)
                        break

                    // Randomize within bounds.
                    it.boundingRectangle.x =
                            (it.boundingRectangle.x + it.boundingRectangle.width * random.nextGaussian() / 2.0).toFloat()
                    it.boundingRectangle.y =
                            (it.boundingRectangle.y + it.boundingRectangle.height * random.nextGaussian() / 2.0).toFloat()

                    if (r in it)
                        break
                }

                // Return result vector.
                val x = floor(r.x / objectSize).toInt()
                val y = floor(r.y / objectSize).toInt()
                x to y
            }

    /**
     * Gets the field at screen.
     */
    fun fieldAtScreen(viewport: Viewport, screenX: Int, screenY: Int): XY {
        val world = viewport.unproject(Vector2(screenX.toFloat(), screenY.toFloat()))
        val x = floor(world.x / objectSize).toInt()
        val y = floor(world.y / objectSize).toInt()
        return x to y
    }

    /**
     * Renders the map using the viewport.
     */
    fun render(viewport: Viewport) {
        viewport.apply()
        tiledRenderer.setView(viewport.camera as OrthographicCamera)
        tiledRenderer.render()
    }
}

/**
 * Displays a map with computed overlay by cost based distribution.
 */
class Voronois(game: WeltraumGefecht) : StageScreen<WeltraumGefecht>(game) {
    override val processor = object : InputAdapter() {
        override fun mouseMoved(screenX: Int, screenY: Int): Boolean {
            if (!initialized)
                return false
            val (x, y) = map.fieldAtScreen(viewport, screenX, screenY)

            valueLabel.setText("$x, $y, types = ${map.typeStack(x, y)}, regions = ${map.regionStack(x, y)}")

            structures[x to y]?.let {
                infoLabel.setText(it.toString())
            }

            return true
        }

        override fun scrolled(amount: Int): Boolean {
            viewport.unitsPerPixel *= Math.pow(1.1, amount.toDouble()).toFloat()
            viewport.update(Gdx.graphics.width, Gdx.graphics.height)
            return true
        }

        override fun touchDragged(screenX: Int, screenY: Int, pointer: Int): Boolean {
            viewport.camera.translate(
                    -Gdx.input.deltaX.toFloat() * viewport.unitsPerPixel,
                    Gdx.input.deltaY.toFloat() * viewport.unitsPerPixel, 0f)
            return true
        }
    }

    var map = AdvancedMap(game, "WorldTwo.tmx", Vector2(16f, 16f))

    var valueLabel by Delegates.notNull<Label>()

    var infoLabel by Delegates.notNull<Label>()

    override val stage = Stage(game.viewport).apply {
        this + topLeft {
            label("Info")
            infoLabel = label("...")
        }

        this + dropLeft {
            label("Value")
            valueLabel = label("...")

        }

        this + dropRight {
            button {
                label("Back")
                onClick { popScreen() }
            }
        }
    }

    var initialized = false

    val viewport = ScreenViewport().apply {
        unitsPerPixel = 6f / objectSize
    }

    val players = listOf(
            Player(listOf(HorsebackRiding)),
            Player(listOf(Sailing)))

    fun Player.index() = players.indexOf(this)

    var structures = mapOf(
            (10 to 10) to Town(players[0], 1, listOf()),
            (14 to 44) to Town(players[1], 1, listOf()),
            (30 to 35) to Town(players[1], 1, listOf(Shipyard)))

    var propfield by Delegates.notNull<Field<Paint?>>()

    init {
        ktxAsync {
            map.load()


            val colors = listOf(
                    Color.RED, Color.GOLD, Color.GREEN,
                    Color.BLUE, Color.PURPLE, Color.PINK,
                    Color.GRAY)

            val painters = structures.entries.map { s ->
                s.value.painterFrom(s.key) { (x, y) ->
                    map.typeStack(x, y)
                            .filterIsInstance<String>()
                            .firstOrNull()
                }
            }

            propfield = propField(painters, map.width to map.height, XY::cross).await()

            map.overlay?.let {
                for ((p, v) in propfield.points())
                    if (v == null)
                        it.setCell(p.first, p.second, null)
                    else {
                        val cell = it.getCell(p.first, p.second) ?: continue
                        val factor = v.used / v.range
                        val inv = 1f - factor * factor * factor * factor
                        it.setCell(p.first, p.second, ColoredCell(cell, colors[v.origin.player.index()], inv))
                    }
            }

            map.user?.let {
                for ((p, t) in structures) {
                    it.setCell(p.first, p.second, TiledMapTileLayer.Cell().apply {
                        tile = map.tilesets.getTileSet("Blowharder").getTile(1540)
                    })
                }
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
        map.render(viewport)

        // Render stage.
        super.render(delta)
    }
}