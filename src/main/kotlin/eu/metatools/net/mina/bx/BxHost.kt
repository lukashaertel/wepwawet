package eu.metatools.net.mina.bx

import kotlinx.coroutines.experimental.launch
import org.apache.mina.core.service.IoHandler
import org.apache.mina.core.session.IdleStatus
import org.apache.mina.core.session.IoSession

/**
 * A binary communication host, handles messages via "process", exceptions while processing will be forwarded to the
 * requester.
 */
abstract class BxHost(
        val fallback: IoHandler?) : IoHandler {

    /**
     * Processes the message.
     * @param bx The message to process.
     * @return The return value.
     */
    abstract suspend fun process(bx: Bx): Bx

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

        launch {
            try {
                // Compute response and write.
                val response = process(message)

                // Transfer UUID.
                response.id = message.id

                // Write response to origin.
                session.write(response)
            } catch (site: Throwable) {
                // Initialize exception.
                val response = BxException(site)

                // Transfer UUID.
                response.id = message.id

                // On exception, write error response.
                session.write(response)
            }
        }
    }


    override fun sessionCreated(session: IoSession) {
        fallback?.sessionCreated(session)
    }

    override fun sessionOpened(session: IoSession) {
        fallback?.sessionOpened(session)
    }

    override fun sessionIdle(session: IoSession, status: IdleStatus) {
        fallback?.sessionIdle(session, status)
    }

    override fun sessionClosed(session: IoSession) {
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