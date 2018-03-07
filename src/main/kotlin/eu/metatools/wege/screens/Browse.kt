package eu.metatools.wege.screens

import com.badlogic.gdx.scenes.scene2d.Stage
import com.badlogic.gdx.scenes.scene2d.ui.Label
import eu.metatools.wege.WeltraumGefecht
import eu.metatools.wege.tools.StageScreen
import eu.metatools.wege.tools.dropLeft
import eu.metatools.wege.tools.dropRight
import eu.metatools.wege.tools.popScreen
import ktx.actors.onClick
import ktx.actors.plus
import ktx.scene2d.*


class Browse(game: WeltraumGefecht) : StageScreen<WeltraumGefecht>(game) {

    lateinit var games: KVerticalGroup
    lateinit var players: KVerticalGroup
    lateinit var status: Label

    override val stage = Stage(game.viewport).apply {
        this + container {
            pad(50f)
            setFillParent(true)
            actor = table {
                label("Games")
                label("Players")
                row()
                games = verticalGroup()
                players = verticalGroup()
            }
        }
        this + dropLeft {
            button {
                label("Create")
                onClick {
                }
            }
            status = label("Status")
        }
        this + dropRight {
            button {
                label("Back")
                onClick {
                    popScreen()
                }
            }
        }
    }

    override fun render(delta: Float) {
        super.render(delta)
    }
}