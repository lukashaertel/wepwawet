package eu.metatools.wege.screens

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.scenes.scene2d.Stage
import eu.metatools.wege.tools.StageScreen
import eu.metatools.wege.WeltraumGefecht
import eu.metatools.wege.tools.dropLeft
import eu.metatools.wege.tools.dropRight
import ktx.actors.onClick
import ktx.actors.plus
import ktx.scene2d.button
import ktx.scene2d.image
import ktx.scene2d.label

class Menu(val game: WeltraumGefecht) : StageScreen() {
    override val stage = Stage(game.viewport).apply {
        this + dropLeft {
            button {
                label("Voronois")
                onClick { game.setScreen<Voronois>() }
            }
            button {
                label("Create game")
                onClick { }
            }
            button {
                label("Enter game")
                onClick { game.setScreen<Browse>() }
            }
            button {
                label("Ship editor")
                onClick { }
            }
            button {
                label("Options")
                onClick { }
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