package eu.metatools.wege.screens

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.scenes.scene2d.Stage
import eu.metatools.wege.tools.StageScreen
import eu.metatools.wege.WeltraumGefecht
import eu.metatools.wege.tools.dropLeft
import eu.metatools.wege.tools.dropRight
import eu.metatools.wege.tools.pushScreen
import ktx.actors.onClick
import ktx.actors.plus
import ktx.scene2d.button
import ktx.scene2d.image
import ktx.scene2d.label

class Menu(game: WeltraumGefecht) : StageScreen<WeltraumGefecht>(game) {
    val browse by lazy { Browse(game) }

    val options by lazy { Options(game) }

    val playground by lazy { Voronois(game) }

    override val stage = Stage(game.viewport).apply {
        this + dropLeft {
            button {
                label("Voronois")
                onClick {
                    pushScreen(playground)
                }
            }
            button {
                label("Games")
                onClick { pushScreen(browse) }
            }
            button {
                label("Options")
                onClick { pushScreen(options) }
            }
        }

        this + dropRight {
            button {
                image("tree-minus")
                label("Quit")
                onClick { Gdx.app.exit() }
            }
        }
    }
}