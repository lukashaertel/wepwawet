package eu.metatools.wege.screens

import com.badlogic.gdx.scenes.scene2d.Stage
import com.badlogic.gdx.scenes.scene2d.ui.Label
import eu.metatools.net.*
import eu.metatools.wege.WeltraumGefecht
import eu.metatools.wege.tools.StageScreen
import eu.metatools.wege.tools.dropLeft
import eu.metatools.wege.tools.dropRight
import eu.metatools.wege.tools.popScreen
import kotlinx.coroutines.experimental.runBlocking
import ktx.actors.onClick
import ktx.actors.plus
import ktx.scene2d.*

class Host(game: WeltraumGefecht) : StageScreen<WeltraumGefecht>(game) {
    lateinit var players: KVerticalGroup

    override val stage = Stage(game.viewport).apply {
        this + container {
            pad(50f)
            setFillParent(true)
            actor = table {
                label("Player")
                row()
                players = verticalGroup()
            }
        }
        this + dropRight {
            button {
                label("Back")
                onClick {
                }
            }
        }
    }


    override fun render(delta: Float) {
        super.render(delta)
    }
}

class Peer(game: WeltraumGefecht) : StageScreen<WeltraumGefecht>(game) {
    lateinit var players: KVerticalGroup

    override val stage = Stage(game.viewport).apply {
        this + container {
            pad(50f)
            setFillParent(true)
            actor = table {
                label("Player")
                row()
                players = verticalGroup()
            }
        }
        this + dropRight {
            button {
                label("Back")
                onClick {
                }
            }
        }
    }

    override fun render(delta: Float) {
        super.render(delta)

    }
}

class Browse(game: WeltraumGefecht) : StageScreen<WeltraumGefecht>(game) {
    private val config = Config(classMapper {

    })

    private val peer = Peer(config)

    private var browse: BrowseChannel? = null

    override fun enter() {
        super.enter()
        browse = peer.browse()
    }

    override fun exit() {
        super.exit()
        browse?.cancel()
    }

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
                    val h = Host<Unit>(config)
                    h.start()
                    h.bind()
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
        peer.update(0)
        browse?.let {
            for (g in generateSequence { it.poll() }) when (g) {
                is BrowseAdd -> games.apply {
                    this + horizontalGroup {
                        name = g.inetAddress.toString()
                        label(g.inetAddress.toString())
                        button {
                            label("Join")
                            onClick {
                            }
                        }
                    }
                }
                is BrowseRemove ->
                    games.findActor<KHorizontalGroup>(g.inetAddress.toString())?.let {
                        games.removeActor(it)
                    }
            }
        }
    }
}