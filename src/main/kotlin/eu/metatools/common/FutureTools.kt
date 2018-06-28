package eu.metatools.common

import org.apache.mina.core.future.IoFuture
import org.apache.mina.core.future.IoFutureListener
import kotlin.coroutines.experimental.suspendCoroutine

/**
 * Awaits the [IoFuture] using the listener to complete.
 */
suspend fun await(ioFuture: IoFuture) = suspendCoroutine<IoFuture> { c ->
    ioFuture.addListener(object : IoFutureListener<IoFuture> {
        override fun operationComplete(future: IoFuture) {
            ioFuture.removeListener(this)
            c.resume(future)
        }
    })
}