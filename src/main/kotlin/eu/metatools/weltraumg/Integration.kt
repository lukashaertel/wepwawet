package eu.metatools.weltraumg

import eu.metatools.wepwawet.*
import kotlinx.serialization.Serializable


/**
 * Transferred call.
 */
@Serializable
data class Call(val time: Time,
                val inner: Inner,
                val author: Author,
                val ids: List<Any?>,
                val method: Method,
                val arg: Any?)

/**
 * History of all calls.
 */
@Serializable
data class History(val calls: List<Call>)

/**
 * Receives the call in the container.
 */
fun Container.receive(call: Call) {
    receive(Revision(call.time, call.inner, call.author), call.ids, call.method, call.arg)
}

/**
 * Get the time of the revision in ms.
 */
fun Revision.asMs() = time