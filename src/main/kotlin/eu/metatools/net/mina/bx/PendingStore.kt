package eu.metatools.net.mina.bx

import kotlinx.coroutines.experimental.CompletableDeferred
import org.apache.mina.core.session.IoSession
import java.util.concurrent.ConcurrentHashMap

/**
 * Manages the pending completable deferred futures.
 */
object PendingStore {

    fun set(ioSession: IoSession) {
        ioSession.setAttribute(PendingStore::class, ConcurrentHashMap<Id, CompletableDeferred<Any?>>())
    }

    @Suppress("unchecked_cast")
    fun reset(ioSession: IoSession) =
            ioSession.removeAttribute(PendingStore::class) as? ConcurrentHashMap<Id, CompletableDeferred<Any?>>


    @Suppress("unchecked_cast")
    fun get(ioSession: IoSession) =
            ioSession.getAttribute(PendingStore::class) as ConcurrentHashMap<Id, CompletableDeferred<Any?>>
}