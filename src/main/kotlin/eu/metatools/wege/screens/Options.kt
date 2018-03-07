package eu.metatools.wege.screens

import com.badlogic.gdx.scenes.scene2d.Stage
import com.badlogic.gdx.scenes.scene2d.ui.TextField
import eu.metatools.wege.WeltraumGefecht
import eu.metatools.wege.data.Settings
import eu.metatools.wege.tools.StageScreen
import eu.metatools.wege.tools.dropLeft
import eu.metatools.wege.tools.dropRight
import eu.metatools.wege.tools.popScreen
import ktx.actors.onClick
import ktx.actors.plus
import ktx.scene2d.*
import kotlin.properties.Delegates

class Options(game: WeltraumGefecht) : StageScreen<WeltraumGefecht>(game) {
    var nameField by Delegates.notNull<TextField>()
    override val stage = Stage(game.viewport).apply {
        this + container {
            setFillParent(true)
            actor = table {
                label("ID")
                label(game.identity.toString())
                row()
                label("Name")
                nameField = textField(game.settings.name)
                row()
            }
        }

        this + dropLeft {
            button {
                label("Apply")
                onClick {
                    game.settings = Settings(nameField.text)
                }
            }
            button {
                label("Reset")
                onClick {
                    nameField.text = game.settings.name
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