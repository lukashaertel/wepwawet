package eu.metatools.forum

import eu.metatools.common.Exchange
import eu.metatools.kontor.KontorCluster
import eu.metatools.kontor.tools.sendAll
import eu.metatools.kontor.tools.sendOnly
import eu.metatools.kontor.tools.toProsumer
import kotlinx.coroutines.experimental.CommonPool
import kotlinx.coroutines.experimental.CompletableDeferred
import kotlinx.coroutines.experimental.channels.Channel
import kotlinx.coroutines.experimental.launch
import kotlinx.coroutines.experimental.runBlocking
import java.nio.charset.Charset
import kotlin.reflect.KClass
import kotlinx.serialization.json.JSON.Companion.stringify
import kotlinx.serialization.json.JSON.Companion.parse
import kotlinx.serialization.Serializable
import kotlinx.serialization.serializer

@Serializable
data class RawEntered(val identityJson: String)

@Serializable
data class RawRetired(val identityJson: String)

@Serializable
data class RawHosted(val roomJson: String)

@Serializable
data class RawUnhosted(val roomJson: String)


@Serializable
class RawPeerJoining(val roomJson: String, val identityJson: String)

@Serializable
class RawPeerJoined(val roomJson: String, val identityJson: String)

@Serializable
class RawPeerLeft(val roomJson: String, val identityJson: String)

@Serializable
class RawMutating(val roomJson: String, val identityJson: String, val mutationJson: String)

@Serializable
class RawMutated(val roomJson: String, val stateJson: String)

@Serializable
class RawGo(val roomJson: String, val stateJson: String)

@Serializable
class RawState(val roomsJson: List<String>, val identitiesJson: List<String>)

/**
 * Gateway implementation that uses a [eu.metatools.kontor.KontorCluster] to maintain lobby behaviour.
 */
class KontorGateway<R : Any, I : Any, S : Any, M : Any>(
        val clusterName: String,
        val roomClass: KClass<R>,
        val identityClass: KClass<I>,
        val stateClass: KClass<S>,
        val mutationClass: KClass<M>,
        val charset: Charset = Charsets.UTF_8) : Gateway<R, I, S, M> {


    private var state = RawState(emptyList(), emptyList())

    private val lobbyMessages = Channel<LobbyMessage<R, I>>(Channel.UNLIMITED)

    private val roomMessages = Channel<RoomMessage<I, S>>(Channel.UNLIMITED)

    private val clusterHolder = Exchange<KontorCluster>()

    private val stateHolder = Exchange<Pair<R, Rules<I, S, M>?>>()

    private inner class KontorLobby(val identity: I) : Lobby<R, I, S, M> {
        private val job = launch(CommonPool) {

            // Create cluster with charset and the main serializable classes
            val cluster = KontorCluster(
                    charset,
                    this@KontorGateway::state.toProsumer(),
                    RawEntered::class,
                    RawRetired::class,
                    RawHosted::class,
                    RawUnhosted::class,
                    RawPeerJoining::class,
                    RawPeerJoined::class,
                    RawPeerLeft::class,
                    RawMutating::class,
                    RawMutated::class,
                    RawGo::class,
                    RawState::class)

            clusterHolder.set(cluster)

            // Start into cluster and discard management
            cluster.start(clusterName).join()
            cluster.management.close()

            // Send entered message
            cluster.outbound.sendAll(RawEntered(stringify(identityClass.serializer(), identity)))

            // Request current rooms and identities
            cluster.requestState()

            // Send them into the channel before networked messages
            for (i in state.identitiesJson.map { parse(identityClass.serializer(), it) })
                lobbyMessages.send(Entered<R, I>(i))

            for (r in state.roomsJson.map { parse(roomClass.serializer(), it) })
                lobbyMessages.send(Hosted<R, I>(r))

            val hostIds = hashSetOf<I>()

            // Handle all messages
            messageLoop@ while (isActive) {
                val (m, a) = cluster.inbound.receive()

                when (m) {
                // Response
                    is RawEntered -> if (m.identityJson !in state.identitiesJson) {
                        // Add identity to the state
                        state = RawState(state.roomsJson, state.identitiesJson + m.identityJson)

                        // Send entered
                        lobbyMessages.send(Entered<R, I>(parse(identityClass.serializer(), m.identityJson)))
                    }

                // Response
                    is RawRetired -> if (m.identityJson in state.identitiesJson) {
                        // Remove identity from the state
                        state = RawState(state.roomsJson, state.identitiesJson - m.identityJson)

                        // Handle retirement
                        val identityValue = parse(identityClass.serializer(), m.identityJson)
                        lobbyMessages.send(Retired<R, I>(identityValue))

                        if (identityValue == identity)
                            break@messageLoop
                    }

                // Response
                    is RawHosted -> if (m.roomJson !in state.roomsJson) {
                        // Add room to the state
                        state = RawState(state.roomsJson + m.roomJson, state.identitiesJson)

                        // Handle room hosting
                        val roomValue = parse(roomClass.serializer(), m.roomJson)
                        lobbyMessages.send(Hosted<R, I>(roomValue))

                        stateHolder.peek { (room, rules) ->
                            if (room == roomValue)
                                roomMessages.send(SelfEntered<I, S>())
                        }
                    }
                // Response
                    is RawUnhosted -> if (m.roomJson in state.roomsJson) {
                        // Remvove room from the state
                        state = RawState(state.roomsJson - m.roomJson, state.identitiesJson)

                        // Handle room unhosting
                        val roomValue = parse(roomClass.serializer(), m.roomJson)
                        lobbyMessages.send(Unhosted<R, I>(roomValue))

                        // If set up as any type, send appropriate room message
                        stateHolder.peek { (room, rules) ->
                            if (room == roomValue)
                                roomMessages.send(SelfLeft<I, S>())
                        }
                    }

                // Request
                    is RawPeerJoining -> {
                        val roomValue = parse(roomClass.serializer(), m.roomJson)
                        val identityValue = parse(identityClass.serializer(), m.identityJson)

                        // If set up as host, check bouncer rules and broadcast join
                        stateHolder.peek { (room, rules) ->
                            if (room == roomValue && rules != null) {
                                if (rules.bounce(identityValue))
                                    cluster.outbound.sendAll(RawPeerLeft(m.roomJson, m.identityJson))
                                else {
                                    cluster.outbound.sendAll(RawPeerJoined(m.roomJson, m.identityJson))

                                    // Send all existing identities
                                    for (i in hostIds)
                                        cluster.outbound.sendOnly(RawPeerJoined(
                                                m.roomJson,
                                                stringify(identityClass.serializer(), i)), a)

                                    // Send mutated to induce updated message.
                                    cluster.outbound.sendOnly(RawMutated(
                                            m.roomJson,
                                            stringify(stateClass.serializer(), rules.describe())), a)

                                    // Mutate local state
                                    hostIds += identityValue
                                }
                            }
                        }
                    }

                // Response
                    is RawPeerJoined -> {
                        val roomValue = parse(roomClass.serializer(), m.roomJson)
                        val identityValue = parse(identityClass.serializer(), m.identityJson)

                        // If set up as peer, send appropriate room message
                        stateHolder.peek { (room, rules) ->
                            if (room == roomValue)
                                if (identityValue == identity)
                                    roomMessages.send(SelfEntered<I, S>())
                                else
                                    roomMessages.send(PeerEntered<I, S>(identityValue))
                        }
                    }

                // Response
                    is RawPeerLeft -> {
                        val roomValue = parse(roomClass.serializer(), m.roomJson)
                        val identityValue = parse(identityClass.serializer(), m.identityJson)

                        // If set up as any type, send appropriate room message
                        stateHolder.peek { (room, rules) ->
                            if (room == roomValue) {
                                if (identityValue == identity)
                                    roomMessages.send(SelfLeft<I, S>())
                                else
                                    roomMessages.send(PeerLeft<I, S>(identityValue))

                                if (rules != null)
                                    if (identityValue == identity)
                                        hostIds.clear()
                                    else
                                        hostIds -= identityValue
                            }
                        }
                    }

                // Request
                    is RawMutating -> {
                        val roomValue = parse(roomClass.serializer(), m.roomJson)
                        val identityValue = parse(identityClass.serializer(), m.identityJson)
                        val mutationValue = parse(mutationClass.serializer(), m.mutationJson)

                        stateHolder.peek { (room, rules) ->
                            if (room == roomValue && rules != null)
                                if (rules.enact(identityValue, mutationValue))
                                    cluster.outbound.sendAll(RawMutated(
                                            m.roomJson,
                                            stringify(stateClass.serializer(), rules.describe())))
                        }
                    }

                // Response
                    is RawMutated -> {
                        val roomValue = parse(roomClass.serializer(), m.roomJson)
                        val stateValue = parse(stateClass.serializer(), m.stateJson)

                        stateHolder.peek { (room, rules) ->
                            if (room == roomValue)
                                roomMessages.send(Updated<I, S>(stateValue))
                        }
                    }

                // Response
                    is RawGo -> {
                        val roomValue = parse(roomClass.serializer(), m.roomJson)
                        val stateValue = parse(stateClass.serializer(), m.stateJson)

                        stateHolder.peek { (room, rules) ->
                            if (room == roomValue)
                                roomMessages.send(Go<I, S>(stateValue))
                        }

                    }
                }
            }

            cluster.stop().join()
            cluster.shutdown().join()

            // Reset holder for restart
            clusterHolder.reset()
        }


        override fun join(room: R): Peer<I, S, M> = runBlocking {
            if (stateHolder.isPresent())
                throw IllegalStateException("Already in sub state.")

            // Acquire cluster
            val cluster = clusterHolder.get()

            // Send hosted message
            cluster.outbound.sendAll(RawPeerJoining(
                    stringify(roomClass.serializer(), room),
                    stringify(identityClass.serializer(), identity)))

            // Return the peer maintainer and set state
            stateHolder.set(room to null)
            KontorPeer(room, identity)
        }

        override fun host(room: R, rules: Rules<I, S, M>): Host<I, S, M> = runBlocking {
            if (stateHolder.isPresent())
                throw IllegalStateException("Already in sub state.")

            // Acquire cluster
            val cluster = clusterHolder.get()

            // Send hosted message
            cluster.outbound.sendAll(RawHosted(stringify(roomClass.serializer(), room)))

            // Return the host maintainer and set state
            stateHolder.set(room to rules)
            KontorHost(room, identity, rules)
        }

        override fun retire() = runBlocking {
            // Acquire cluster
            val cluster = clusterHolder.get()

            // Send retired message
            cluster.outbound.sendAll(RawRetired(stringify(identityClass.serializer(), identity)))
        }


        override fun messages() = lobbyMessages
    }

    private inner class KontorPeer(val room: R, val identity: I) : Peer<I, S, M> {
        override fun mutate(mutation: M) = runBlocking {
            // Obtain the cluster
            val cluster = clusterHolder.get()

            // Broadcast mutation request
            cluster.outbound.sendAll(RawMutating(
                    stringify(roomClass.serializer(), room),
                    stringify(identityClass.serializer(), identity),
                    stringify(mutationClass.serializer(), mutation)))
        }

        override fun leave() = runBlocking {
            // Reset the state holder
            stateHolder.reset()

            // Obtain the cluster
            val cluster = clusterHolder.get()

            // Broadcast host leave
            cluster.outbound.sendAll(RawPeerLeft(
                    stringify(roomClass.serializer(), room),
                    stringify(identityClass.serializer(), identity)))
        }

        override fun messages() = roomMessages
    }

    private inner class KontorHost(val room: R, val identity: I, val rules: Rules<I, S, M>) : Host<I, S, M> {
        override fun mutate(mutation: M) = runBlocking {
            // Obtain the cluster
            val cluster = clusterHolder.get()

            // Broadcast mutation request
            cluster.outbound.sendAll(RawMutating(
                    stringify(roomClass.serializer(), room),
                    stringify(identityClass.serializer(), identity),
                    stringify(mutationClass.serializer(), mutation)))
        }

        override fun leave() = runBlocking {
            // Reset the state holder
            stateHolder.reset()

            // Obtain the cluster
            val cluster = clusterHolder.get()

            // Broadcast host leave
            cluster.outbound.sendAll(RawUnhosted(stringify(roomClass.serializer(), room)))
        }

        override fun messages() = roomMessages

        override fun kick(identity: I) = runBlocking {
            // Obtain the cluster
            val cluster = clusterHolder.get()

            // Broadcast host leave
            cluster.outbound.sendAll(RawPeerLeft(
                    stringify(roomClass.serializer(), room),
                    stringify(identityClass.serializer(), identity)))
        }

        override fun start() = runBlocking {
            // Obtain the cluster
            val cluster = clusterHolder.get()

            // Broadcast go
            cluster.outbound.sendAll(RawGo(
                    stringify(roomClass.serializer(), room),
                    stringify(stateClass.serializer(), rules.describe())))
        }
    }

    override fun enter(identity: I): Lobby<R, I, S, M> {
        // TODO: No duplicate identities
        return KontorLobby(identity)
    }
}

@Serializable
data class Room(val title: String)

@Serializable
data class Identity(val name: String)

@Serializable
data class State(val map: String)

@Serializable
data class Mutation(val newMap: String)

fun main(args: Array<String>) = runBlocking {
    val gateway = KontorGateway("game", Room::class, Identity::class, State::class, Mutation::class)

    println("Whats your name")
    val name = readLine() ?: "John doe"

    val lobby = gateway.enter(Identity(name))

    // TODO: Available rooms and identities
    val lobbyJob = launch(CommonPool) {
        while (isActive)
            for (m in lobby.messages()) when (m) {
                is Entered<Room, Identity> ->
                    println("Player ${m.identity.name} just joined")

                is Retired<Room, Identity> ->
                    println("Player ${m.identity.name} just left")

                is Hosted<Room, Identity> ->
                    println("Room ${m.room} is now available")

                is Unhosted<Room, Identity> ->
                    println("Room ${m.room} is now closed")
            }

    }

    lobbyLoop@ while (true) {
        println("What do you want to do (host, join, retire)")

        val command = readLine() ?: "nothing"

        when (command) {
            "host" -> {
                println("What's the title of the game")
                val title = readLine() ?: "The game"

                println("What map to play")
                val map = readLine() ?: "The map"

                val host = lobby.host(Room(title), object : Rules<Identity, State, Mutation> {
                    private var state: State = State(map)

                    override fun bounce(identity: Identity): Boolean {
                        return identity.name == "mingebag"
                    }

                    override fun describe(): State {
                        return state
                    }

                    override fun enact(identity: Identity, mutation: Mutation): Boolean {
                        if (identity.name == "John Doe")
                            return false

                        state = State(mutation.newMap)
                        return true
                    }
                })

                // TODO: Some duplicate messages
                // TODO: Make an actor out of this
                val hostJob = launch(CommonPool) {
                    while (isActive)
                        for (m in host.messages()) when (m) {
                            is SelfEntered<Identity, State> ->
                                println("Now you are in the game")

                            is PeerEntered<Identity, State> ->
                                println("${m.identity} is now also in the game")

                            is PeerLeft<Identity, State> ->
                                println("${m.identity} just left game")

                            is SelfLeft<Identity, State> ->
                                println("You just left game")

                            is Updated<Identity, State> ->
                                println("The map is now ${m.state.map}")

                            is Go<Identity, State> ->
                                println("Game GO!") // TODO: Go to game code.
                        }
                }


                hostLoop@ while (true) {
                    println("What do you want to do (kick, start, mutate, leave)")

                    val command = readLine() ?: "nothing"

                    when (command) {
                        "kick" -> {
                            println("Who do you want to kick.")
                            val name = readLine()
                            if (name != null)
                                host.kick(Identity(name))
                        }
                        "start" ->
                            host.start()

                        "mutate" -> {
                            println("What map to play")
                            val newMap = readLine()
                            if (newMap != null)
                                host.mutate(Mutation(newMap))
                        }

                        "leave" -> break@hostLoop
                    }
                }

                host.leave()
                hostJob.cancel()
                hostJob.join()
            }

            "join" -> {
                println("What's the title of the game you want to join")
                val title = readLine() ?: "The game"

                val peer = lobby.join(Room(title))

                val peerJob = launch(CommonPool) {
                    while (isActive)
                        for (m in peer.messages()) when (m) {
                            is SelfEntered<Identity, State> ->
                                println("Now you are in the game")

                            is PeerEntered<Identity, State> ->
                                println("${m.identity} is now also in the game")

                            is PeerLeft<Identity, State> ->
                                println("${m.identity} just left game")

                            is SelfLeft<Identity, State> ->
                                println("You just left game")

                            is Updated<Identity, State> ->
                                println("The map is now ${m.state.map}")

                            is Go<Identity, State> ->
                                println("Game GO!") // TODO: Go to game code.
                        }
                }

                peerLoop@ while (true) {
                    println("What do you want to do (mutate, leave)")

                    val command = readLine() ?: "nothing"

                    when (command) {
                        "mutate" -> {
                            println("What map to play")
                            val newMap = readLine()
                            if (newMap != null)
                                peer.mutate(Mutation(newMap))
                        }

                        "leave" -> break@peerLoop
                    }
                }

                peer.leave()
                peerJob.cancel()
                peerJob.join()
            }

            "retire" -> {
                break@lobbyLoop
            }
        }
    }

    lobby.retire()
    lobbyJob.cancel()
    lobbyJob.join()
}