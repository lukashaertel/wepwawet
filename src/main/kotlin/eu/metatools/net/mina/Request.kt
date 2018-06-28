package eu.metatools.net.mina

import eu.metatools.common.await
import eu.metatools.net.mina.bx.Bx
import eu.metatools.net.mina.bx.Id
import eu.metatools.net.mina.bx.PendingStore
import kotlinx.coroutines.experimental.CompletableDeferred
import kotlinx.coroutines.experimental.withTimeoutOrNull
import org.apache.mina.core.session.IoSession
import java.util.*

/**
 * Sends a request via this session, awaiting the result of type O.
 */
suspend inline fun <reified O : Any> request(session: IoSession, request: Bx): O {
    // Create unique ID and completable deferred.
    val deferred = CompletableDeferred<Any?>()
    val id = Id(UUID.randomUUID())

    // Get pending store and register the completable deferred.
    val pendingStore = PendingStore.get(session)
    pendingStore[id] = deferred

    // Override existing ID in case the request is reused.
    request.id = id

    // Send and await request.
    try {
        await(session.write(request))
    } catch (t: Throwable) {
        pendingStore.remove(id)
        deferred.completeExceptionally(t)
    }

    // Await completion of the deferred.
    return deferred.await() as O
}

/**
 * Sends a request via this session, awaiting the result.
 */
@JvmName("requestAny")
suspend inline fun request(session: IoSession, request: Bx): Bx {
    // Create unique ID and completable deferred.
    val deferred = CompletableDeferred<Any?>()
    val id = Id(UUID.randomUUID())

    // Get pending store and register the completable deferred.
    val pendingStore = PendingStore.get(session)
    pendingStore[id] = deferred

    // Override existing ID in case the request is reused.
    request.id = id

    // Send and await request.
    try {
        await(session.write(request))
    } catch (t: Throwable) {
        pendingStore.remove(id)
        deferred.completeExceptionally(t)
    }

    // Await completion of the deferred.
    return deferred.await() as Bx
}

/**
 * Sends a request via this session, awaiting the result of type O. Returns null after timeout.
 */
suspend inline fun <reified O : Any> request(session: IoSession, request: Bx, time: Int): O? {
    // Create unique ID and completable deferred.
    val deferred = CompletableDeferred<Any?>()
    val id = Id(UUID.randomUUID())

    // Get pending store and register the completable deferred.
    val pendingStore = PendingStore.get(session)
    pendingStore[id] = deferred

    // Override existing ID in case the request is reused.
    request.id = id

    // Send and await request.
    try {
        await(session.write(request))
    } catch (t: Throwable) {
        pendingStore.remove(id)
        deferred.completeExceptionally(t)
    }

    // Await completion of the deferred.
    return withTimeoutOrNull(time) {
        deferred.await() as O
    }
}

/**
 * Sends a request via this session, awaiting the result. Returns null after timeout.
 */
@JvmName("requestAny")
suspend inline fun request(session: IoSession, request: Bx, time: Int): Bx? {
    // Create unique ID and completable deferred.
    val deferred = CompletableDeferred<Any?>()
    val id = Id(UUID.randomUUID())

    // Get pending store and register the completable deferred.
    val pendingStore = PendingStore.get(session)
    pendingStore[id] = deferred

    // Override existing ID in case the request is reused.
    request.id = id

    // Send and await request.
    try {
        await(session.write(request))
    } catch (t: Throwable) {
        pendingStore.remove(id)
        deferred.completeExceptionally(t)
    }

    // Await completion of the deferred.
    return withTimeoutOrNull(time) {
        deferred.await() as Bx
    }
}