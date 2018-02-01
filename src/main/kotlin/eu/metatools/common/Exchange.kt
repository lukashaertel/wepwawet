package eu.metatools.common

import kotlinx.coroutines.experimental.sync.Mutex


class Exchange<T> {
    private val mutex = Mutex(true)

    private var holder = listOf<T>()

    suspend fun set(value: T) {
        holder = listOf(value)
        mutex.unlock()
    }

    suspend fun peek(block: suspend (T) -> Unit) {
        if (mutex.tryLock()) {
            val value = holder
            mutex.unlock()
            value.forEach { block(it) }
        }
    }

    suspend fun reset() {
        mutex.lock()
        holder = listOf()
    }

    suspend fun isPresent(): Boolean {
        return !mutex.isLocked
    }

    suspend fun get(): T {
        mutex.lock()
        val (value) = holder
        mutex.unlock()
        return value
    }
}