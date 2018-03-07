package eu.metatools.common

import kotlinx.coroutines.experimental.sync.Mutex
import kotlinx.coroutines.experimental.sync.withLock

// TODO: Validate, this is one of the more unstable things I fear.
class Exchange<T> {
    private val set = Mutex(false)
    private val notSet = Mutex(true)
    private var cause: Throwable? = null

    private var holder = listOf<T>()

    suspend fun set(value: T) {
        cause?.let { throw it }

        set.lock()
        cause?.let { throw it }

        holder = listOf(value)
        notSet.unlock()
    }

    fun close(cause: Throwable = IllegalStateException()) {
        this.cause = cause
        set.unlock()
        notSet.unlock()
    }

    suspend fun peek(block: suspend (T) -> Unit) {
        if (cause == null && notSet.tryLock()) {
            block(holder[0])
            notSet.unlock()
        }
    }

    suspend fun reset() {
        cause?.let { throw it }

        notSet.lock()
        cause?.let { throw it }

        holder = listOf()
        set.unlock()
    }

    fun isPresent(): Boolean {
        return cause == null && set.isLocked
    }

    suspend fun get(): T {
        cause?.let { throw it }

        notSet.withLock {
            cause?.let { throw it }
            return holder[0]
        }
    }
}