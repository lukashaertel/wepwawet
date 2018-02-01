package eu.metatools.wege.screens

import com.badlogic.gdx.scenes.scene2d.Stage
import eu.metatools.wege.tools.StageScreen
import eu.metatools.wege.WeltraumGefecht
import eu.metatools.wege.tools.dropRight
import ktx.actors.onClick
import ktx.actors.plus
import ktx.scene2d.*

class Browse(val game: WeltraumGefecht) : StageScreen() {
    override val stage = Stage(game.viewport).apply {
        this + container {
            setFillParent(true)
            actor = verticalGroup {
                horizontalGroup {
                    space(40f)
                    label("Name") { width = 300f }
                    label("Players") { width = 100f }
                    label("Join") { width = 100f }
                }

                horizontalGroup {
                    space(40f)
                    label("Stupid game") { width = 300f }
                    label("0/10") { width = 100f }
                    button {
                        width = 100f
                        label("Join")
                    }
                }

                horizontalGroup {
                    space(40f)
                    label("Stupid game") { width = 300f }
                    label("0/10") { width = 100f }
                    button {
                        width = 100f
                        label("Join")
                    }
                }
                horizontalGroup {
                    space(40f)
                    label("Stupid game") { width = 300f }
                    label("0/10") { width = 100f }
                    button {
                        width = 100f
                        label("Join")
                    }
                }
            }
        }
        this + dropRight {
            button {
                label("Back")
                onClick { game.setScreen<Menu>() }
            }
        }
    }
}