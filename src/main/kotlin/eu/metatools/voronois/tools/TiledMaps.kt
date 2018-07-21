package eu.metatools.voronois.tools

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.g2d.Batch
import com.badlogic.gdx.maps.MapObject
import com.badlogic.gdx.maps.objects.TextureMapObject
import com.badlogic.gdx.maps.tiled.TiledMap
import com.badlogic.gdx.maps.tiled.TiledMapTile
import com.badlogic.gdx.maps.tiled.TiledMapTileLayer
import com.badlogic.gdx.maps.tiled.TiledMapTileLayer.Cell
import com.badlogic.gdx.maps.tiled.renderers.BatchTiledMapRenderer
import com.badlogic.gdx.maps.tiled.renderers.OrthogonalTiledMapRenderer

/**
 * Wrapper for cells that can have colors and alpha.
 */
class ColoredCell(var cell: Cell, val color: Color? = null, val alpha: Float? = null) : Cell() {
    override fun getFlipVertically(): Boolean {
        return cell.flipVertically
    }

    override fun setFlipHorizontally(flipHorizontally: Boolean): ColoredCell {
        cell = cell.setFlipHorizontally(flipHorizontally)
        return this
    }

    override fun getRotation(): Int {
        return cell.rotation
    }

    override fun setRotation(rotation: Int): ColoredCell {
        cell = cell.setRotation(rotation)
        return this
    }

    override fun setFlipVertically(flipVertically: Boolean): ColoredCell {
        cell = cell.setFlipVertically(flipVertically)
        return this
    }

    override fun getTile(): TiledMapTile {
        return cell.tile
    }

    override fun setTile(tile: TiledMapTile?): ColoredCell {
        cell = cell.setTile(tile)
        return this
    }

    override fun getFlipHorizontally(): Boolean {
        return cell.flipHorizontally
    }
}

/**
 * Renders tiled maps, supports [ColoredCell] and renders sprites.
 */
class ExtendedOrthogonalTiledMapRenderer(map: TiledMap) : OrthogonalTiledMapRenderer(map) {
    override fun renderTileLayer(layer: TiledMapTileLayer) {
        val batchColor = batch.color

        val layerWidth = layer.width
        val layerHeight = layer.height

        val layerTileWidth = layer.tileWidth * unitScale
        val layerTileHeight = layer.tileHeight * unitScale

        val layerOffsetX = layer.renderOffsetX * unitScale
        // offset in tiled is y down, so we flip it
        val layerOffsetY = -layer.renderOffsetY * unitScale

        val col1 = Math.max(0, ((viewBounds.x - layerOffsetX) / layerTileWidth).toInt())
        val col2 = Math.min(layerWidth,
                ((viewBounds.x + viewBounds.width + layerTileWidth - layerOffsetX) / layerTileWidth).toInt())

        val row1 = Math.max(0, ((viewBounds.y - layerOffsetY) / layerTileHeight).toInt())
        val row2 = Math.min(layerHeight,
                ((viewBounds.y + viewBounds.height + layerTileHeight - layerOffsetY) / layerTileHeight).toInt())

        var y = row2 * layerTileHeight + layerOffsetY
        val xStart = col1 * layerTileWidth + layerOffsetX
        val vertices = this.vertices

        for (row in row2 downTo row1) {
            var x = xStart
            for (col in col1 until col2) {
                val cell = layer.getCell(col, row)
                if (cell == null) {
                    x += layerTileWidth
                    continue
                }
                val override = (cell as? ColoredCell)?.color ?: batchColor
                val opacity = ((cell as? ColoredCell)?.alpha ?: 1f) * layer.opacity
                val overrideFloat = Color.toFloatBits(override.r, override.g, override.b, override.a * opacity)
                val tile = cell.tile

                if (tile != null) {
                    val flipX = cell.flipHorizontally
                    val flipY = cell.flipVertically
                    val rotations = cell.rotation

                    val region = tile.textureRegion

                    val x1 = x + tile.offsetX * unitScale
                    val y1 = y + tile.offsetY * unitScale
                    val x2 = x1 + region.regionWidth * unitScale
                    val y2 = y1 + region.regionHeight * unitScale

                    val u1 = region.u
                    val v1 = region.v2
                    val u2 = region.u2
                    val v2 = region.v

                    vertices[Batch.X1] = x1
                    vertices[Batch.Y1] = y1
                    vertices[Batch.C1] = overrideFloat
                    vertices[Batch.U1] = u1
                    vertices[Batch.V1] = v1

                    vertices[Batch.X2] = x1
                    vertices[Batch.Y2] = y2
                    vertices[Batch.C2] = overrideFloat
                    vertices[Batch.U2] = u1
                    vertices[Batch.V2] = v2

                    vertices[Batch.X3] = x2
                    vertices[Batch.Y3] = y2
                    vertices[Batch.C3] = overrideFloat
                    vertices[Batch.U3] = u2
                    vertices[Batch.V3] = v2

                    vertices[Batch.X4] = x2
                    vertices[Batch.Y4] = y1
                    vertices[Batch.C4] = overrideFloat
                    vertices[Batch.U4] = u2
                    vertices[Batch.V4] = v1

                    if (flipX) {
                        var temp = vertices[Batch.U1]
                        vertices[Batch.U1] = vertices[Batch.U3]
                        vertices[Batch.U3] = temp
                        temp = vertices[Batch.U2]
                        vertices[Batch.U2] = vertices[Batch.U4]
                        vertices[Batch.U4] = temp
                    }
                    if (flipY) {
                        var temp = vertices[Batch.V1]
                        vertices[Batch.V1] = vertices[Batch.V3]
                        vertices[Batch.V3] = temp
                        temp = vertices[Batch.V2]
                        vertices[Batch.V2] = vertices[Batch.V4]
                        vertices[Batch.V4] = temp
                    }
                    if (rotations != 0) {
                        when (rotations) {
                            TiledMapTileLayer.Cell.ROTATE_90 -> {
                                val tempV = vertices[Batch.V1]
                                vertices[Batch.V1] = vertices[Batch.V2]
                                vertices[Batch.V2] = vertices[Batch.V3]
                                vertices[Batch.V3] = vertices[Batch.V4]
                                vertices[Batch.V4] = tempV

                                val tempU = vertices[Batch.U1]
                                vertices[Batch.U1] = vertices[Batch.U2]
                                vertices[Batch.U2] = vertices[Batch.U3]
                                vertices[Batch.U3] = vertices[Batch.U4]
                                vertices[Batch.U4] = tempU
                            }
                            TiledMapTileLayer.Cell.ROTATE_180 -> {
                                var tempU = vertices[Batch.U1]
                                vertices[Batch.U1] = vertices[Batch.U3]
                                vertices[Batch.U3] = tempU
                                tempU = vertices[Batch.U2]
                                vertices[Batch.U2] = vertices[Batch.U4]
                                vertices[Batch.U4] = tempU
                                var tempV = vertices[Batch.V1]
                                vertices[Batch.V1] = vertices[Batch.V3]
                                vertices[Batch.V3] = tempV
                                tempV = vertices[Batch.V2]
                                vertices[Batch.V2] = vertices[Batch.V4]
                                vertices[Batch.V4] = tempV
                            }
                            TiledMapTileLayer.Cell.ROTATE_270 -> {
                                val tempV = vertices[Batch.V1]
                                vertices[Batch.V1] = vertices[Batch.V4]
                                vertices[Batch.V4] = vertices[Batch.V3]
                                vertices[Batch.V3] = vertices[Batch.V2]
                                vertices[Batch.V2] = tempV

                                val tempU = vertices[Batch.U1]
                                vertices[Batch.U1] = vertices[Batch.U4]
                                vertices[Batch.U4] = vertices[Batch.U3]
                                vertices[Batch.U3] = vertices[Batch.U2]
                                vertices[Batch.U2] = tempU
                            }
                        }
                    }
                    batch.draw(region.texture, vertices, 0, BatchTiledMapRenderer.NUM_VERTICES)
                }
                x += layerTileWidth
            }
            y -= layerTileHeight
        }
    }

    override fun renderObject(mapObject: MapObject) {
        if (mapObject is TextureMapObject) {
            batch.draw(mapObject.textureRegion,
                    mapObject.x, mapObject.y,
                    mapObject.originX,
                    mapObject.originY,
                    mapObject.textureRegion.regionWidth.toFloat(),
                    mapObject.textureRegion.regionHeight.toFloat(),
                    mapObject.scaleX, mapObject.scaleY,
                    mapObject.rotation)

        }
    }
}