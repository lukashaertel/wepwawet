package eu.metatools.weltraumg

import eu.metatools.wepwawet.*
import kotlinx.serialization.Serializable


/**
 * Transferred call.
 */
@Serializable
data class Call(val timestep: Timestep,
                val inner: Inner,
                val author: Author,
                val ids: List<Any?>,
                val method: Method,
                val arg: Any?)

/**
 * History of all calls.
 */
@Serializable
data class History(val init: Timestep, val calls: List<Call>)

/**
 * Receives the call in the container.
 */
fun Container.receive(call: Call) {
    receive(Revision(call.timestep, call.inner, call.author), call.ids, call.method, call.arg)
}

const val offset = 1544981350000L

/**
 * Get the time of the revision in s.
 */
fun Container.seconds() = (rev().timestep - offset).toDouble() / 1000.0