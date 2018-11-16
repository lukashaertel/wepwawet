package eu.metatools.weltraumg

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.scenes.scene2d.Stage
import com.badlogic.gdx.scenes.scene2d.ui.Label
import eu.metatools.voronois.tools.StageScreen
import eu.metatools.voronois.tools.dropLeft
import eu.metatools.voronois.tools.dropRight
import eu.metatools.voronois.tools.pushScreen
import ktx.actors.onClick
import ktx.actors.plus
import ktx.scene2d.*
import java.io.File
import kotlin.properties.Delegates.notNull

class Menu(game: Game) : StageScreen<Game>(game) {
    val main by lazy { Main(game) }

    var label by notNull<Label>()

    override val stage = Stage(game.viewport).apply {
        this + container {
            pad(50f)
            setFillParent(true)
            actor = table {
                label("Savegame")
                label("Load")
                row()

                for (f in File("saves").listFiles()) {
                    label(f.nameWithoutExtension)
                    button {
                        label("Load")
                        onClick {
                            main.load = f
                            pushScreen(main)
                        }
                    }
                    row()
                }
            }
        }

        this + dropLeft {
            button {
                label = label("Start")
                onClick {
                    pushScreen(main)
                }
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