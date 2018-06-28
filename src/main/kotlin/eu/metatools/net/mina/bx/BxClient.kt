package eu.metatools.net.mina.bx

import org.apache.mina.core.service.IoHandler
import org.apache.mina.core.session.IdleStatus
import org.apache.mina.core.session.IoSession

/**
 * A binary communication client, resolves pending completable futures in the sessions.
 */
data class BxClient(val fallback: IoHandler?) : IoHandler {
    override fun messageReceived(session: IoSession, message: Any?) {
        // Null message, this cannot be handled by Bx.
        if (message == null) {
            fallback?.messageReceived(session, message)
            return
        }

        // Not a Bx message, fallback.
        if (message !is Bx) {
            fallback?.messageReceived(session, message)
            return
        }

        // Get pending store and take contained completable deferred.
        val pendingStore = PendingStore.get(session)
        val deferred = pendingStore.remove(message.id)

        // Deferred is not present, something went wrong.
        if (deferred == null)
            throw IllegalArgumentException("Deferred is not present in pending store.")

        // If response was an exception, complete exceptionally, otherwise resolve.
        if (message is BxException)
            deferred.completeExceptionally(message.throwable)
        else
            deferred.complete(message)
    }

    override fun sessionCreated(session: IoSession) {
        // Continue to fallback if present.
        fallback?.sessionCreated(session)
    }

    override fun sessionOpened(session: IoSession) {
        fallback?.sessionOpened(session)

        // Initialize pending store on the session.
        PendingStore.set(session)
    }

    override fun sessionIdle(session: IoSession, status: IdleStatus) {
        fallback?.sessionIdle(session, status)
    }

    override fun sessionClosed(session: IoSession) {
        val pendingStore = PendingStore.reset(session)
        if (pendingStore != null)
            for (v in pendingStore.values)
                v.completeExceptionally(IllegalStateException("Cannot receive resolve or reject, as session is closed"))

        fallback?.sessionClosed(session)
    }

    override fun messageSent(session: IoSession, message: Any?) {
        fallback?.messageSent(session, message)
    }

    override fun inputClosed(session: IoSession) {
        fallback?.inputClosed(session)
    }


    override fun exceptionCaught(session: IoSession, cause: Throwable) {
        fallback?.exceptionCaught(session, cause)
        cause.printStackTrace()
    }
}