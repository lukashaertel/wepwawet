package eu.metatools.wege.net

import eu.metatools.common.except
import eu.metatools.net.*
import eu.metatools.statem.*
import kotlinx.coroutines.experimental.delay
import kotlinx.coroutines.experimental.launch
import java.net.InetAddress


object Net : StateMachine<Status, NetInput, NetEvent>() {
    val offline: State by init {
        // Entering net state machine to browse.
        on(ActionBrowse) goto ::browse andThen {
            emit(StageEnterBrowser)
            emit(NetOutStartPeer)
        }

        defaultLoop emit { StageUnhandled(it) }
    }

    val browse: State by state {
        // Stopping net state machine on leaving browser.
        on(ActionBack) goto ::offline andThen {
            emit(StageLeaveBrowser)
            emit(NetOutStopPeer)
        }

        // Hosting a game from the browser.
        on(ActionHost) goto ::hosting andThen {
            emit(StageEnterProcessing)

            // Set host to localhost.
            mutate { copy(host = InetAddress.getLocalHost()) }

            emit(NetOutStopPeer)
            emit(NetOutStartHost)
        }

        // Joining a host.
        on<ActionJoin>() goto ::joining andThen {
            emit(StageEnterProcessing)

            // Set host to target.
            mutate { copy(host = it.address) }

            emit { NetOutConnectPeer(it.address) }
        }

        // Periodically update hosts.
        on(ActionPeriodic) goto self andThen {
            emit(NetOutUpdateHosts)
        }

        defaultLoop emit { StageUnhandled(it) }
    }

    val hosting: State by state {
        // About to host, net control was able to bind.
        on(NetInBound) goto ::host andThen {
            emit(StageEnterHost)
        }

        // About to host, binding failed.
        on(NetInFail) goto ::browse andThen {
            emit(StageLeaveProcessing)
            emit(NetOutStopHost)
        }

        defaultLoop emit { StageUnhandled(it) }
    }

    val joining: State by state {
        // About to join, control could connect.
        on(NetInConnected) goto ::joined andThen {
            emit(StageEnterPeer)
        }

        // About to join, control could not connect.
        on(NetInFail) goto ::browse andThen {
            emit(StageLeaveProcessing)
        }

        // About to join, server declined.
        on<NetInRemovePlayer> { it.player == self } goto ::browse andThen {
            emit(StageLeaveProcessing)
        }

        defaultLoop emit { StageUnhandled(it) }
    }

    val host: State by state {
        // Leaving host to browser.
        on(ActionBack) goto ::browse andThen {
            // Remove all players for all players.
            emitMany { addresses.flatMap { a -> players.map { p -> NetOutRemovePlayer(a, p) } } }

            // Update the status
            mutate { copy(data = emptyMap()) }

            // Push to UI and net control.
            emit(StageLeaveHost)
            emit(NetOutStopHost)
        }

        // Game info requested from browser.
        on<NetInInquire>() goto self andThen {
            emit { NetOutDescribe(it.address) }
        }

        // Banned player tries to join.
        on<NetInAddPlayer> { it.player in banned } goto self andThen {
            emit { NetOutRemovePlayer(it.address, it.player) }
        }

        // Player joins.
        on<NetInAddPlayer> { it.player !in banned } goto self andThen {
            // Add player to status.
            mutate { addPlayer(it.player, it.address) }

            // Send already connected players to new player.
            emitMany { players.map { p -> NetOutAddPlayer(it.address, p) } }
            // Send new player to already connected players.
            emitMany { data.except(it.player).values.map { d -> NetOutAddPlayer(d.address, it.player) } }

            emit { StageAddPlayer(it.player) }
        }

        // Player leaves that is not the server.
        on<NetInRemovePlayer> { it.player != self } goto self andThen {
            // Remove all connected players for removed player.
            emitMany { players.map { p -> NetOutRemovePlayer(it.address, p) } }
            // Send removed player to already connected players.
            emitMany { data.except(it.player).values.map { d -> NetOutRemovePlayer(d.address, it.player) } }

            // Remove player from status.
            mutate { removePlayer(it.player) }

            emit { StageRemovePlayer(it.player) }
        }

        // Player leaves that is the server.
        on<NetInRemovePlayer> { it.player == self } goto ::browse andThen {
            // Remove all players for all players.
            emitMany { addresses.flatMap { a -> players.map { p -> NetOutRemovePlayer(a, p) } } }

            // Update the status
            mutate { copy(data = emptyMap()) }

            // Push to UI and net control.
            emit(StageLeaveHost)
            emit(NetOutStopHost)
        }

        on<ActionMessage>() goto self andThen {
            // Send messages to all other players.
            emitMany { f -> addresses.map { NetOutMessage(it, self, f.message) } }
            emit { StageMessage(self, it.message) }
        }

        // Room message sent.
        on<NetInMessage>() goto self andThen {
            // Send messages to all other players.
            emitMany { f -> addresses.map { NetOutMessage(it, f.player, f.message) } }
            emit { StageMessage(it.player, it.message) }
        }

        defaultLoop emit { StageUnhandled(it) }
    }


    val joined: State by state {
        // Leaving peer to browser.
        on(ActionBack) goto ::browse andThen {
            emit { NetOutRemovePlayer(host, self) }
            emit(StageLeavePeer)
        }

        // A player was added.
        on<NetInAddPlayer>() goto self andThen {
            emit { StageAddPlayer(it.player) }
        }

        // Another player left the server.
        on<NetInRemovePlayer> { it.player != self } goto self andThen {
            emit { StageRemovePlayer(it.player) }
        }

        // This player left the server.
        on<NetInRemovePlayer> { it.player == self } goto self andThen {
            emit(StageLeavePeer)
        }

        on<ActionMessage>() goto self andThen {
            // Send messages to all other players.
            emit { NetOutMessage(host, self, it.message) }
        }

        // Room message sent.
        on<NetInMessage>() goto self andThen {
            emit { StageMessage(it.player, it.message) }
        }

        defaultLoop emit { StageUnhandled(it) }
    }

    val starting: State by state {

        defaultLoop emit { StageUnhandled(it) }
    }
}

fun main(args: Array<String>) {
    val player = readLine() ?: return
    val config = Config(classMapper {

    })
    /**
     *
    object NetOutStartPeer : NetEventControl()
    object NetOutStopPeer : NetEventControl()
    object NetOutStartHost : NetEventControl()
    object NetOutStopHost : NetEventControl()
    class NetOutConnectPeer(val address: InetAddress) : NetEventControl()
    object NetOutUpdateHosts : NetEventControl()
    data class NetOutDescribe(val address: InetAddress) : NetEventControl()
    data class NetOutAddPlayer(val address: InetAddress, val player: Player) : NetEventControl()
    data class NetOutRemovePlayer(val address: InetAddress, val player: Player) : NetEventControl()
    data class NetOutMessage(val address: InetAddress, val player: Player, val message: Message) : NetEventControl()

     */
    val peer = Peer(config)
    val host = Host<Unit>(config)
    val receiver = receiver<Status, NetEvent> {
        emit { s, e ->
            when (e) {
                NetOutStartPeer -> peer.start()
                NetOutStopPeer -> peer.stop()
                NetOutStartHost -> host.start()
                NetOutStopHost -> host.stop()
                is NetOutConnectPeer -> peer.connect(e.address)
                is NetOutUpdateHosts -> {
                }
            }
        }
    }

    val sim = Net.runWith(Status(InetAddress.getLocalHost(), player, emptySet(), emptyMap()), receiver and Log) {
        launch {
            while (isActive) {
                delay(5000)
                send(ActionPeriodic)
            }
        }

        for (i in generateSequence { readLine() }.map { it.toLowerCase() })
            when (i) {
                "back" -> send(ActionBack)
                "browse" -> send(ActionBrowse)
                "host" -> send(ActionHost)

                else -> {
                    if (i.startsWith("join "))
                        send(ActionJoin(InetAddress.getByName(i.substring(4))))
                    else
                        send(ActionMessage(i))
                }
            }
    }
}