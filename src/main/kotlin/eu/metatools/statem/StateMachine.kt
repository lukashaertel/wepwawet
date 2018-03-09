package eu.metatools.statem

import kotlin.reflect.KProperty

/**
 * Base class for state machine definition.
 * @param I The input type.
 * @param E The type of the events.
 */
abstract class StateMachine<I, E> {
    /**
     * A state in the state machine.
     */
    inner class State(
            /**
             * The name of the state.
             */
            val name: String,

            /**
             * Default if input was sent that is not handled by transitions.
             */
            var default: Pair<List<Lazy<E>>, Lazy<State>>?,

            /**
             * The mapping of input to events and next state.
             */
            val next: Map<I, Pair<List<Lazy<E>>, Lazy<State>>>,

            /**
             * The mapping of input guard to events and next state.
             */
            val nextGuard: List<Triple<(I) -> Boolean, List<Lazy<E>>, Lazy<State>>>)

    /**
     * Configurator for event emitting.
     * @param E The type of the events.
     * @param events The event list to write to.
     */
    protected inner class Extra<in E>(
            private val events: MutableList<Lazy<E>>) {
        /**
         * This transition emits [event].
         * @param event The value to emit.
         * @return Returns self for chaining.
         */
        infix fun emit(event: E) = emit(lazyOf(event))

        /**
         * This transition emits [event].
         * @param event The lazy value to emit.
         * @return Returns self for chaining.
         */
        infix fun emit(event: Lazy<E>): Extra<E> {
            events += event
            return this
        }

        /**
         * This transition emits [event].
         * @param event The generator of the lazy value to emit.
         * @return Returns self for chaining.
         */
        infix fun emit(event: () -> E) = emit(lazy(event))

        /**
         * This transition emits [event]. This method is the same as [emit] for generic overload security.
         * @param event The value to emit.
         * @return Returns self for chaining.
         */
        infix fun emitValue(event: E) = emit(event)

        /**
         * This transition emits [event]. This method is the same as [emit] for generic overload security.
         * @param event The lazy value to emit.
         * @return Returns self for chaining.
         */
        infix fun emitLazy(event: Lazy<E>) = emit(event)

        /**
         * This transition emits [event]. This method is the same as [emit] for generic overload security.
         * @param event The generator of the lazy value to emit.
         * @return Returns self for chaining.
         */
        infix fun emitBy(event: () -> E) = emit(lazy(event))

    }

    /**
     * Configurator for state transitions.
     * @param self Getter on the current state.
     */
    protected inner class StateBuilder(val self: Lazy<State>) {
        /**
         * Marker class for default rule.
         */
        inner class By internal constructor() {
            /**
             * When input could not be handled in the current state, goes to the given state.
             * @receiver The input on which to react.
             * @param state The state to go to.
             * @return Returns emitter configuration.
             */
            infix fun default(state: Lazy<State>): Extra<E> {
                val events = arrayListOf<Lazy<E>>()
                default = events to state
                return Extra(events)
            }

            /**
             * When input could not be handled in the current state, goes to the given state.
             * @receiver The input on which to react.
             * @param state The state to go to.
             * @return Returns emitter configuration.
             */
            infix fun default(state: KProperty<State>) =
                    default(lazy { state.call() })
        }

        /**
         * Marker class for explicit rule.
         */
        inner class Explicit internal constructor(val value: I) {
            /**
             * When value is equal to the input in the current state, goes to the given state.
             * @param state The state to go to.
             * @return Returns emitter configuration.
             */
            infix fun goTo(state: Lazy<State>): Extra<E> {
                val events = arrayListOf<Lazy<E>>()
                next[value] = events to state
                return Extra(events)
            }

            /**
             * When value is equal on the input in the current state, goes to the given state.
             * @param state The state to go to.
             * @return Returns emitter configuration.
             */
            infix fun goTo(state: KProperty<State>) =
                    goTo(lazy { state.call() })
        }

        /**
         * Marker class for guard rule.
         */
        inner class Guard internal constructor(val guard: (I) -> Boolean) {
            /**
             * When [guard] is matched on the input in the current state, goes to the given state.
             * @param state The state to go to.
             * @return Returns emitter configuration.
             */
            infix fun goTo(state: Lazy<State>): Extra<E> {
                val events = arrayListOf<Lazy<E>>()
                nextGuard += Triple(guard, events, state)
                return Extra(events)
            }

            /**
             * When [guard] is matched on the input in the current state, goes to the given state.
             * @param state The state to go to.
             * @return Returns emitter configuration.
             */
            infix fun goTo(state: KProperty<State>) =
                    goTo(lazy { state.call() })
        }

        /**
         * Marker object for default rule.
         */
        val by = By()

        /**
         * Begins an explicit clause.
         */
        fun on(value: I) =
                Explicit(value)

        /**
         * Begins a guard clause.
         */
        fun on(guard: (I) -> Boolean) =
                Guard(guard)

        /**
         * Begins an explicit clause. This method is the same as [on] for generic overload security.
         */
        fun onValue(value: I) =
                Explicit(value)

        /**
         * Begins a guard clause. This method is the same as [on] for generic overload security.
         */
        fun onGuard(guard: (I) -> Boolean) =
                Guard(guard)

        /**
         * Store for the default rule.
         */
        internal var default: Pair<MutableList<Lazy<E>>, Lazy<State>>? = null

        /**
         * Transition map.
         */
        internal val next = hashMapOf<I, Pair<MutableList<Lazy<E>>, Lazy<State>>>()

        /**
         * Transition map.
         */
        internal val nextGuard = arrayListOf<Triple<(I) -> Boolean, MutableList<Lazy<E>>, Lazy<State>>>()


        /**
         * Shorthand for ```by default self```.
         */
        val defaultLoop: Extra<E>
            get() =
                by default self
    }

    /**
     * Delegate for getting a state.
     * @param state The state to return.
     */
    protected inner class StateDelegate(
            private val state: State) {
        operator fun getValue(stateMachine: StateMachine<I, E>, kProperty: KProperty<*>) = state
    }

    /**
     * Provider for state delegate based on being [initial] and configured by [target].
     * @param initial True if this state is initial.
     * @param target The configurator.
     */
    protected inner class StateProvider(
            private val initial: Boolean,
            private val target: StateBuilder.() -> Unit) {
        operator fun provideDelegate(stateMachine: StateMachine<I, E>, kProperty: KProperty<*>): StateDelegate {

            // Initialize builder.
            val builder = StateBuilder(lazy {
                @Suppress("unchecked_cast")
                kProperty.call(stateMachine) as StateMachine<I, E>.State
            })

            // Configure to the builder.
            target(builder)

            // Create state.
            val state = State(kProperty.name, builder.default, builder.next, builder.nextGuard)

            // Assign initial if desired.
            if (initial)
                stateMachine.initial = state

            // Return the delegate responding with the created state.
            return StateDelegate(state)
        }
    }

    /**
     * Creates an initial state with the given builder.
     */
    protected fun init(target: StateBuilder.() -> Unit) = StateProvider(true, target)

    /**
     * Creates a state with the given builder.
     */
    protected fun state(target: StateBuilder.() -> Unit) = StateProvider(false, target)

    /**
     * The initial state.
     */
    private lateinit var initial: State

    /**
     * Starts a simulation with the given optional receiver.
     * @param receiver The receiver or null.
     * @return Returns a simulation.
     */
    fun run(receiver: Receiver<E>? = null): Simulation<I> {
        // Current location in the simulation.
        var current = initial
        var running = true

        // Enter the initial state.
        receiver?.entering(current.name)

        return object : Simulation<I> {

            override val at: String
                get() = current.name

            override fun send(input: I) {
                // Check that still running.
                if (!running)
                    throw IllegalStateException("Simulation is completed.")

                // Get emitted event and states.
                val next = current.next[input]
                        ?: current.nextGuard.filter { (p, _, _) -> p(input) }.map { (_, e, s) -> e to s }.firstOrNull()
                        ?: current.default

                // If invalid input, throw an exception.
                if (next == null)
                    throw IllegalArgumentException("Invalid input at state ${current.name}: no definition for $input.")

                // If receiver given, call methods.
                receiver?.apply {
                    leaving(current.name)
                    for (e in next.first)
                        emit(e.value)
                    entering(next.second.value.name)
                }

                // Advance state.
                current = next.second.value
            }

            override fun stop() {
                if (running) {
                    running = false
                    receiver?.leaving(current.name)
                }
            }
        }
    }
}

/**
 * Runs a simulation during the [block] with the optional [receiver].
 * @param I The input type.
 * @param E The type of events.
 * @receiver The state machine to run.
 * @param receiver The receiver to notify or null.
 * @param block The block that sends events.
 * @return Returns the state the machine stopped in.
 */
inline fun <I, E> StateMachine<I, E>.runWith(receiver: Receiver<E>? = null, block: Simulation<I>.() -> Unit): String {
    val s = run(receiver)
    block(s)
    s.stop()
    return s.at
}

class StateMachineInvalidException(s: String,
                                   val inputs: List<Any?>,
                                   val path: List<String>,
                                   val expect: List<String>) : IllegalStateException(s)

/**
 * Runs the state machine with the given [inputs]. If the path taken mismatches [expect], a
 * [StateMachineInvalidException] is thrown.
 */
fun <I, E> StateMachine<I, E>.validate(inputs: List<I>, expect: List<String>) {
    // Track path.
    val t = Path<E>()

    // Run state machine on all inputs.
    runWith(t) { send(inputs) }

    // Check if input sequences mismatch.
    if (t.path.size > expect.size)
        throw StateMachineInvalidException("Path taken is longer than expected.", inputs, t.path, expect)
    if (t.path.size < expect.size)
        throw StateMachineInvalidException("Path taken is shorter than expected.", inputs, t.path, expect)
    for (i in 0 until t.path.size)
        if (t.path[i] != expect[i])
            throw StateMachineInvalidException("Path diverges at step $i, where ${expect[i]} was expected but ${t.path[i]} was returned.", inputs, t.path, expect)
}

object Test : StateMachine<Int, Any>() {
    val initial: State by init {
        on(1) goTo ::mid emit "Going to mid"
        by default ::error
    }

    val mid: State by state {
        on(1) goTo ::initial emit "Going to initial"
        on { it in 2..10 } goTo self
        by default ::error
    }

    val error: State by state {
        defaultLoop emitBy { IllegalStateException("In error state.") }
    }
}

fun main(args: Array<String>) {
    Test.validate(
            listOf(1, 1, 1, 2, 12),
            listOf("initial", "mid", "initial", "mid", "mid", "error"))
}