package eu.metatools.statem

import java.io.PrintStream
import kotlin.reflect.KClass
import kotlin.reflect.full.cast


/**
 * Receiver for state machine events.
 * @param E The type of the events.
 */
interface Receiver<in E> {
    /**
     * Called when a state is being left.
     */
    fun leaving(state: String) {}

    /**
     * Called when a state is entering.
     */
    fun entering(state: String) {}

    /**
     * Called when an event is emitted.
     */
    fun emit(event: E) {}
}

/**
 * Combines the receivers sequentially.
 * @param E The type of events.
 * @receiver The first receiver.
 * @param other The other receiver.
 * @return Returns a new anonymous receiver.
 */
infix fun <E : F, F> Receiver<E>.and(other: Receiver<F>) = object : Receiver<E> {
    override fun leaving(state: String) {
        this@and.leaving(state)
        other.leaving(state)
    }

    override fun entering(state: String) {
        this@and.entering(state)
        other.entering(state)
    }

    override fun emit(event: E) {
        this@and.emit(event)
        other.emit(event)
    }
}

/**
 * Tracks the traversed path.
 */
class Path<in E> : Receiver<E> {
    private val backing = arrayListOf<String>()

    /**
     * The path the simulation has traversed so far.
     */
    val path get() = backing.toList()

    override fun entering(state: String) {
        backing += state
    }
}

/**
 * Throws events that are subtypes of the given exception. The companion object throws all [Throwable]s.
 */
class Throw(val type: KClass<Throwable> = Throwable::class) : Receiver<Any> {
    /**
     * The companion object throws all [Throwable]s.
     */
    companion object : Receiver<Any> {
        override fun emit(event: Any) {
            if (event is Throwable)
                throw event
        }
    }

    override fun emit(event: Any) {
        if (type.isInstance(event))
            throw type.cast(event)
    }
}

/**
 * Logs all events to the print stream via [println]. The companion object logs to stdout.
 */
class Log(val to: PrintStream = System.out) : Receiver<Any> {
    /**
     * The companion object logs to stdout.
     */
    companion object : Receiver<Any> {
        override fun emit(event: Any) {
            println(event)
        }
    }

    override fun emit(event: Any) {
        to.println(event)
    }
}

/**
 * Builder methods for [Receiver].
 */
interface ReceiverBuilder<out E> {

    /**
     * Called when a state is being left.
     */
    fun leaving(handler: (String) -> Unit)

    /**
     * Called when a state is entering.
     */
    fun entering(handler: (String) -> Unit)

    /**
     * Called when an event is emitted.
     */
    fun emit(handler: (E) -> Unit)
}

/**
 * Builds a receiver.
 */
inline fun <reified E> receiver(builder: ReceiverBuilder<E>.() -> Unit): Receiver<E> {
    // Stores for the actual methods used.
    var onLeaving: (String) -> Unit = {}
    var onEntering: (String) -> Unit = {}
    var onEmit: (E) -> Unit = {}

    // Implementation of the builder.
    val target = object : ReceiverBuilder<E> {
        override fun leaving(handler: (String) -> Unit) {
            onLeaving = handler
        }

        override fun entering(handler: (String) -> Unit) {
            onEntering = handler
        }

        override fun emit(handler: (E) -> Unit) {
            onEmit = handler
        }

    }

    // Build to implementation.
    builder(target)

    // Return the new interface.
    return object : Receiver<E> {
        override fun leaving(state: String) {
            onLeaving(state)
        }

        override fun entering(state: String) {
            onEntering(state)
        }

        override fun emit(event: E) {
            onEmit(event)
        }
    }
}