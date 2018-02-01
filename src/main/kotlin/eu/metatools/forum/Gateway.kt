package eu.metatools.forum

import com.google.common.base.Optional
import kotlinx.coroutines.experimental.channels.Channel
import kotlinx.coroutines.experimental.sync.Mutex

/**
 * Explains changes in lobby state.
 */
interface LobbyMessage<R, I>

/**
 * Explains changes in room state.
 */
interface RoomMessage<I, S>

/**
 * * From [Gateway.enter]
 * * For Lobby peer
 *
 * This message announces an identity entering the lobby.
 */
data class Entered<R, I>(val identity: I) : LobbyMessage<R, I>

/**
 * * From [Lobby.retire]
 * * For Lobby peer
 *
 * This message announces an identity leaving the lobby.
 */
data class Retired<R, I>(val identity: I) : LobbyMessage<R, I>

/**
 * * From [Lobby.host]
 * * For Lobby peer
 *
 * This message announces a host creating a room.
 */
data class Hosted<R, I>(val room: R) : LobbyMessage<R, I>

/**
 * * From [Host.leave]
 * * For Lobby peer
 *
 * This message announces a room being abandoned.
 */
data class Unhosted<R, I>(val room: R) : LobbyMessage<R, I>

/**
 * * From [Lobby.join]
 * * From [Lobby.host]
 * * For Room host
 * * For Room peer
 *
 * This message announces that joining or hosting occurred.
 */
class SelfEntered<I, S> : RoomMessage<I, S>

/**
 * * From [Lobby.join] and initial
 * * For Room host
 * * For Room peer
 *
 * This message announces that a peer is now in the room.
 */
data class PeerEntered<I, S>(val identity: I) : RoomMessage<I, S>

/**
 * * From [Peer.leave]
 * * From [Host.kick]
 * * For Room host
 * * For Room peer
 *
 * This message announces that a peer left the room.
 */
data class PeerLeft<I, S>(val identity: I) : RoomMessage<I, S>

/**
 * * From [Peer.leave]
 * * For Room host
 * * For Room peer
 *
 * This message announces that leaving occurred (be it by leaving yourself or the host closing the room).
 */
class SelfLeft<I, S> : RoomMessage<I, S>

/**
 * * From [Peer.mutate] and initial
 * * For Room host
 * * For Room peer
 *
 * This message announces a mutation properly changing the game state.
 */
data class Updated<I, S>(val state: S) : RoomMessage<I, S>

/**
 * * From [Host.start]
 * * For Room host
 * * For Room peer
 *
 * This message announces that consensus if found and the room transitions to a game.
 */
data class Go<I, S>(val state: S) : RoomMessage<I, S>

/**
 * The lobby maintains rooms and other peers waiting for rooms.
 */
interface Lobby<R, I, S, M> {
    /**
     * Join the room as a participant. The resulting object is only valid after [SelfEntered].
     */
    fun join(room: R): Peer<I, S, M>

    /**
     * Host a room as an administrator. The resulting object is only valid after [SelfEntered].
     */
    fun host(room: R, rules: Rules<I, S, M>): Host<I, S, M>

    /**
     * Retires from the lobby
     */
    fun retire()

    /**
     * Obtains the next lobby messages to handle.
     */
    fun messages(): Channel<LobbyMessage<R, I>>
}

/**
 * Rules define how a room is maintained in order of accessibility and peers mutating room state. Rules define how
 * a room is described.
 */
interface Rules<I, S, M> {
    /**
     * Accept an identity for change of room participants.
     */
    fun bounce(identity: I): Boolean

    /**
     * Describe the room.
     */
    fun describe(): S

    /**
     * Enact a change of the room state.
     * @return True if description has changed.
     */
    fun enact(identity: I, mutation: M): Boolean
}

/**
 * Peers may propose mutating the room and leave the room.
 */
interface Peer<I, S, M> {
    /**
     * Propose a change of the room state.
     */
    fun mutate(mutation: M)

    /**
     * Leave the room.
     */
    fun leave()

    /**
     * Obtains the next room messages to handle.
     */
    fun messages(): Channel<RoomMessage<I, S>>
}

/**
 * Hosts are peers, they may also disbar peers and signal the start of the actual -- post room -- progression.
 */
interface Host<I, S, M> : Peer<I, S, M> {
    /**
     * Forcibly remove the participant with the given identity.
     */
    fun kick(identity: I)

    /**
     * Send start signal, this will cause the lobby to game state.
     */
    fun start()
}

/**
 * Gateways maintain joining a lobby.
 */
interface Gateway<R, I, S, M> {
    /**
     * Enter as the given identity.
     */
    fun enter(identity: I): Lobby<R, I, S, M>
}
