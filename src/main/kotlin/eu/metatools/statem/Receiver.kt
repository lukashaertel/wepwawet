package eu.metatools.statem

import java.io.PrintStream
import kotlin.reflect.KClass
import kotlin.reflect.full.cast


/**
 * Receiver for state machine events.
 * @param S The status type.
 * @param E The type of the events.
 */
interface Receiver<in S, in E> {
    /**
     * Called when the status is changed.
     */
    fun changed(from: S, to: S) {}

    /**
     * Called when a state is being left.
     */
    fun leaving(status: S, state: String) {}

    /**
     * Called when a state is entering.
     */
    fun entering(status: S, state: String) {}

    /**
     * Called when an event is emitted.
     */
    fun emit(status: S, event: E) {}
}

/**
 * Combines the receivers sequentially.
 * @param E The type of events.
 * @receiver The first receiver.
 * @param other The other receiver.
 * @return Returns a new anonymous receiver.
 */
infix fun <S, E : F, F> Receiver<S, E>.and(other: Receiver<S, F>) = object : Receiver<S, E> {
    override fun changed(from: S, to: S) {
        this@and.changed(from, to)
        other.changed(from, to)
    }

    override fun leaving(status: S, state: String) {
        this@and.leaving(status, state)
        other.leaving(status, state)
    }

    override fun entering(status: S, state: String) {
        this@and.entering(status, state)
        other.entering(status, state)
    }

    override fun emit(status: S, event: E) {
        this@and.emit(status, event)
        other.emit(status, event)
    }
}

/**
 * Tracks the traversed path.
 */
class Path<in S, in E> : Receiver<S, E> {
    private val backing = arrayListOf<String>()

    /**
     * The path the simulation has traversed so far.
     */
    val path get() = backing.toList()

    override fun entering(status: S, state: String) {
        backing += state
    }
}

/**
 * Throws events that are subtypes of the given exception. The companion object throws all [Throwable]s.
 */
class Throw(val type: KClass<Throwable> = Throwable::class) : Receiver<Any, Any> {
    /**
     * The companion object throws all [Throwable]s.
     */
    companion object : Receiver<Any, Any> {
        override fun emit(status: Any, event: Any) {
            if (event is Throwable)
                throw event
        }
    }

    override fun emit(status: Any, event: Any) {
        if (type.isInstance(event))
            throw type.cast(event)
    }
}

/**
 * Logs all events to the print stream via [println]. The companion object logs to stdout.
 */
class Log(val target: PrintStream = System.out) : Receiver<Any, Any> {
    /**
     * The companion object logs to stdout.
     */
    companion object : Receiver<Any, Any> {
        override fun changed(from: Any, to: Any) {
            println("[$from -> $to]")
        }

        override fun emit(status: Any, event: Any) {
            println("[$status] $event")
        }
    }

    override fun changed(from: Any, to: Any) {
        target.println("[$from -> $to]")
    }

    override fun emit(status: Any, event: Any) {
        target.println("[$status] $event")
    }
}

/**
 * Builder methods for [Receiver].
 */
interface ReceiverBuilder<out S, out E> {
    /**
     * Called when user status is changed.
     */
    fun changed(handler: (S, S) -> Unit)

    /**
     * Called when a state is being left.
     */
    fun leaving(handler: (S, String) -> Unit)

    /**
     * Called when a state is entering.
     */
    fun entering(handler: (S, String) -> Unit)

    /**
     * Called when an event is emitted.
     */
    fun emit(handler: (S, E) -> Unit)
}

/**
 * Builds a receiver.
 */
inline fun <reified S, reified E> receiver(builder: ReceiverBuilder<S, E>.() -> Unit): Receiver<S, E> {
    // Stores for the actual methods used.
    var onChanged: (S, S) -> Unit = { _, _ -> }
    var onLeaving: (S, String) -> Unit = { _, _ -> }
    var onEntering: (S, String) -> Unit = { _, _ -> }
    var onEmit: (S, E) -> Unit = { _, _ -> }

    // Implementation of the builder.
    val target = object : ReceiverBuilder<S, E> {
        override fun changed(handler: (S, S) -> Unit) {
            onChanged = handler
        }

        override fun leaving(handler: (S, String) -> Unit) {
            onLeaving = handler
        }

        override fun entering(handler: (S, String) -> Unit) {
            onEntering = handler
        }

        override fun emit(handler: (S, E) -> Unit) {
            onEmit = handler
        }

    }

    // Build to implementation.
    builder(target)

    // Return the new interface.
    return object : Receiver<S, E> {
        override fun changed(from: S, to: S) {
            onChanged(from, to)
        }

        override fun leaving(status: S, state: String) {
            onLeaving(status, state)
        }

        override fun entering(status: S, state: String) {
            onEntering(status, state)
        }

        override fun emit(status: S, event: E) {
            onEmit(status, event)
        }
    }
}