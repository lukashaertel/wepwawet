package eu.metatools.statem

import eu.metatools.common.asSafe
import eu.metatools.common.iff
import kotlin.reflect.KClass
import kotlin.reflect.KProperty

/**
 * Effect caused by a transition.
 */
sealed class Effect<S, in I, out E>

/**
 * Status mutation.
 * @param S The status type.
 * @param I The input type.
 * @param E The type of the events.
 * @param block The block that mutates the status.
 */
data class Mutate<S, in I, out E>(val block: S.(I) -> S) : Effect<S, I, E>()

/**
 * Emission variations.
 * @param S The status type.
 * @param I The input type.
 * @param E The type of the events.
 */
sealed class Emit<S, in I, out E> : Effect<S, I, E>() {
    /**
     * Gets the values optionally taking the input into account.
     */
    abstract fun get(status: S, input: I): List<E>
}

/**
 * Emits a pre defined value.
 */
private data class EmitValue<S, in I, out E>(val value: E) : Emit<S, I, E>() {
    override fun get(status: S, input: I): List<E> {
        return listOf(value)
    }
}

/**
 * Emits a lazily computed value.
 */
private data class EmitLazy<S, in I, out E>(val value: Lazy<E>) : Emit<S, I, E>() {
    override fun get(status: S, input: I): List<E> {
        return listOf(value.value)
    }
}

/**
 * Emits a value computed based on the input.
 */
private data class EmitFrom<S, in I, out E>(val block: S.(I) -> E) : Emit<S, I, E>() {
    override fun get(status: S, input: I): List<E> {
        return listOf(block(status, input))
    }
}


/**
 * Emits pre defined values.
 */
private data class EmitManyValue<S, in I, out E>(val values: List<E>) : Emit<S, I, E>() {
    override fun get(status: S, input: I): List<E> {
        return values
    }
}

/**
 * Emits lazily computed values.
 */
private data class EmitManyLazy<S, in I, out E>(val values: Lazy<List<E>>) : Emit<S, I, E>() {
    override fun get(status: S, input: I): List<E> {
        return values.value
    }
}

/**
 * Emits values computed based on the input.
 */
private data class EmitManyFrom<S, in I, out E>(val block: S.(I) -> List<E>) : Emit<S, I, E>() {
    override fun get(status: S, input: I): List<E> {
        return block(status, input)
    }
}

// TODO: State with status type assertion.

/**
 * Base class for state machine definition.
 * @param S The status type.
 * @param I The input type.
 * @param E The type of the events.
 */
abstract class StateMachine<S, I : Any, E> {
    /**
     * A state in the state machine.
     * @property name  The name of the state.
     * @property default Default if input was sent that is not handled by transitions.
     * @property enter Enter effects.
     * @property exit Exit effects.
     * @property next The mapping of input to events and next state.
     * @property nextGuard The mapping of input guard to events and next state.
     * @property nextType The mapping of input type to events and next state.
     * @property nextTypeGuard The mapping of input type guard to events and next state.
     */
    inner class State(
            val name: String,
            var default: Pair<List<Effect<S, I, E>>, Lazy<State>>?,
            val enter: List<Effect<S, Unit, E>>,
            val exit: List<Effect<S, Unit, E>>,
            val next: Map<Any, Pair<List<Effect<S, Any, E>>, Lazy<State>>>,
            val nextGuard: List<Pair<S.(I) -> Boolean, Pair<MutableList<Effect<S, I, E>>, Lazy<State>>>>,
            val nextType: List<Pair<KClass<*>, Pair<MutableList<Effect<S, Any, E>>, Lazy<State>>>>,
            val nextTypeGuard: List<Pair<Pair<KClass<*>, S.(Any) -> Boolean>, Pair<MutableList<Effect<S, Any, E>>, Lazy<State>>>>)

    /**
     * Configurator for event emitting.
     * @param I The input type.
     * @param E The type of the events.
     * @param events The event list to write to.
     */
    protected abstract inner class Extra<I, in E>(
            private val events: MutableList<Effect<S, I, E>>) {

        /**
         * This transition mutates the user status..
         * @param block The mutation to apply.
         * @return Returns self for chaining.
         */
        infix fun mutate(block: S.(I) -> S) = also {
            events += Mutate(block)
        }

        /**
         * This transition emits [value].
         * @param value The value to emit.
         * @return Returns self for chaining.
         */
        infix fun emit(value: E) = also {
            events += EmitValue(value)
        }

        /**
         * This transition emits [value].
         * @param value The lazy value to emit.
         * @return Returns self for chaining.
         */
        infix fun emit(value: Lazy<E>) = also {
            events += EmitLazy(value)
        }

        /**
         * This transition emits the result of [block] on the input.
         * @param block The function computing the event from the input.
         * @return Returns self for chaining.
         */
        infix fun emit(block: S.(I) -> E) = also {
            events += EmitFrom(block)
        }

        /**
         * This transition emits [values].
         * @param values The values to emit.
         * @return Returns self for chaining.
         */
        infix fun emitMany(values: List<E>) = also {
            events += EmitManyValue(values)
        }

        /**
         * This transition emits [values].
         * @param values The lazy values to emit.
         * @return Returns self for chaining.
         */
        infix fun emitMany(values: Lazy<List<E>>) = also {
            events += EmitManyLazy(values)
        }

        /**
         * This transition emits the result of [block] on the input.
         * @param block The function computing the events from the input.
         * @return Returns self for chaining.
         */
        infix fun emitMany(block: S.(I) -> List<E>) = also {
            events += EmitManyFrom(block)
        }

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
         * This transition emits the result of [block] on the input. This method is the same as [emit] for generic
         * overload security.
         * @param block The generator of the lazy value to emit.
         * @return Returns self for chaining.
         */
        infix fun emitFrom(block: S.(I) -> E) = emit(block)

        /**
         * This transition emits [values]. This method is the same as [emitMany] for generic overload security.
         * @param values The values to emit.
         * @return Returns self for chaining.
         */
        infix fun emitManyValue(values: List<E>) = also {
            events += EmitManyValue(values)
        }

        /**
         * This transition emits [values]. This method is the same as [emitMany] for generic overload security.
         * @param values The lazy values to emit.
         * @return Returns self for chaining.
         */
        infix fun emitManyLazy(values: Lazy<List<E>>) = also {
            events += EmitManyLazy(values)
        }

        /**
         * This transition emits the result of [block] on the input. This method is the same as [emitMany] for generic
         * overload security.
         * @param block The function computing the events from the input.
         * @return Returns self for chaining.
         */
        infix fun emitManyFrom(block: S.(I) -> List<E>) = also {
            events += EmitManyFrom(block)
        }
    }

    /**
     * EDSL specializer for non-transition extras.
     */
    protected inner class NTExtra<I, in E>(events: MutableList<Effect<S, I, E>>) : Extra<I, E>(events) {
        /**
         * Helper for when multiple emits are used that are inconvenient in infix form.
         */
        inline infix fun then(block: Extra<I, E>.() -> Unit) {
            block(this)
        }
    }

    /**
     * EDSL specializer for transition extras.
     */
    protected inner class TExtra<I, in E>(events: MutableList<Effect<S, I, E>>) : Extra<I, E>(events) {
        /**
         * Helper for when multiple emits are used that are inconvenient in infix form.
         */
        inline infix fun andThen(block: Extra<I, E>.() -> Unit) {
            block(this)
        }
    }

    /**
     * Shorthand for  creating an identity mutation expecting the current status to be equal to a value.
     */
    protected infix fun <I, E> Extra<I, E>.assertStatus(status: S) =
            mutate {
                also {
                    check(this == status, { "Status should be $status, was $this" })
                }
            }

    /**
     * Shorthand for creating an identity mutation that asserts that the given [assertion] is true, assuming non-null
     * results are failures.
     */
    protected infix fun <I, E> Extra<I, E>.assertNull(assertion: S.() -> Any?) =
            mutate {
                also {
                    val message = assertion()
                    check(message == null, { message!! })
                }
            }

    /**
     * Shorthand for creating an identity mutation that asserts that the given [assertion] is true.
     */
    protected infix fun <I, E> Extra<I, E>.assert(assertion: S.() -> Boolean) =
            mutate { also { check(assertion()) } }

    /**
     * Shorthand for creating an identity mutation that asserts that the given [assertion] is true, uses [message] to
     * generate the error.
     */
    protected fun <I, E> Extra<I, E>.assert(message: S.() -> Any, assertion: S.() -> Boolean) =
            mutate { also { check(assertion(), { message() }) } }

    /**
     * Shorthand for creating an identity mutation that asserts that the given [assertion] is true, uses the given
     * static [message].
     */
    protected fun <I, E> Extra<I, E>.assert(message: Any, assertion: S.() -> Boolean) =
            mutate { also { check(assertion(), { message }) } }

    /**
     * Wrapper for [Extra.emitLazy] on a [lazy] computation with [event] as the generator.
     */
    protected infix fun <I, E> Extra<I, E>.emitLazy(event: () -> E) =
            emitLazy(lazy(event))

    /**
     * Wrapper for [Extra.emitManyLazy] on a [lazy] computation with [events] as the generator.
     */
    protected infix fun <I, E> Extra<I, E>.emitManyLazy(events: () -> List<E>) =
            emitManyLazy(lazy(events))

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
            infix fun default(state: Lazy<State>): TExtra<I, E> {
                val events = arrayListOf<Effect<S, I, E>>()
                default = events to state
                return TExtra(events)
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
        inner class Explicit<J : I>(val value: J) {
            /**
             * When value is equal to the input in the current state, goes to the given state.
             * @param state The state to go to.
             * @return Returns emitter configuration.
             */
            infix fun goto(state: Lazy<State>): TExtra<J, E> {
                val events = arrayListOf<Effect<S, J, E>>()
                val eventsArb = events.asSafe<MutableList<Effect<S, Any, E>>>()
                next[value] = eventsArb to state
                return TExtra(events)
            }

            /**
             * When value is equal on the input in the current state, goes to the given state.
             * @param state The state to go to.
             * @return Returns emitter configuration.
             */
            infix fun goto(state: KProperty<State>) =
                    goto(lazy { state.call() })
        }

        /**
         * Marker class for guard rule.
         */
        inner class Guard(val guard: S.(I) -> Boolean) {
            /**
             * When [guard] is matched on the input in the current state, goes to the given state.
             * @param state The state to go to.
             * @return Returns emitter configuration.
             */
            infix fun goto(state: Lazy<State>): TExtra<I, E> {
                val events = arrayListOf<Effect<S, I, E>>()
                nextGuard += guard to (events to state)
                return TExtra(events)
            }

            /**
             * When [guard] is matched on the input in the current state, goes to the given state.
             * @param state The state to go to.
             * @return Returns emitter configuration.
             */
            infix fun goto(state: KProperty<State>) =
                    goto(lazy { state.call() })
        }

        /**
         * Marker class for type rule.
         */
        inner class Type<J : I>(val type: KClass<J>) {
            /**
             * When [guard] is matched on the input in the current state, goes to the given state.
             * @param state The state to go to.
             * @return Returns emitter configuration.
             */
            infix fun goto(state: Lazy<State>): TExtra<J, E> {
                val events = arrayListOf<Effect<S, J, E>>()
                val eventsArb = events.asSafe<MutableList<Effect<S, Any, E>>>()
                nextType += type to (eventsArb to state)
                return TExtra(events)
            }

            /**
             * When [guard] is matched on the input in the current state, goes to the given state.
             * @param state The state to go to.
             * @return Returns emitter configuration.
             */
            infix fun goto(state: KProperty<State>) =
                    goto(lazy { state.call() })
        }

        /**
         * Marker class for type guard rule.
         */
        inner class TypeGuard<J : I>(val type: KClass<J>, val guard: S.(J) -> Boolean) {
            /**
             * When [guard] is matched on the input in the current state, goes to the given state.
             * @param state The state to go to.
             * @return Returns emitter configuration.
             */
            infix fun goto(state: Lazy<State>): TExtra<J, E> {
                val events = arrayListOf<Effect<S, J, E>>()
                val guardArb = guard.asSafe<S.(Any) -> Boolean>()
                val eventsArb = events.asSafe<MutableList<Effect<S, Any, E>>>()
                nextTypeGuard += (type to guardArb) to (eventsArb to state)
                return TExtra(events)
            }

            /**
             * When [guard] is matched on the input in the current state, goes to the given state.
             * @param state The state to go to.
             * @return Returns emitter configuration.
             */
            infix fun goto(state: KProperty<State>) =
                    goto(lazy { state.call() })
        }

        /**
         * Aggregates effects on entering a state.
         */
        val onEnter
            get(): NTExtra<Unit, E> {
                return NTExtra(enter)
            }

        /**
         * Aggregates effects on exiting a state.
         */
        val onExit
            get(): NTExtra<Unit, E> {
                return NTExtra(exit)
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
        fun on(guard: S.(I) -> Boolean) =
                Guard(guard)

        /**
         * Begins a guard clause on the type.
         */
        inline fun <reified J : I> on() =
                Type(J::class)

        /**
         * Begins a guard clause on the type and value.
         */
        inline fun <reified J : I> on(noinline guard: S.(J) -> Boolean) =
                TypeGuard(J::class, guard)

        /**
         * Begins an explicit clause. This method is the same as [on] for generic overload security.
         */
        fun onValue(value: I) =
                Explicit(value)

        /**
         * Begins a guard clause. This method is the same as [on] for generic overload security.
         */
        fun onGuard(guard: S.(I) -> Boolean) =
                Guard(guard)

        /**
         * Store for the default rule.
         */
        internal var default: Pair<MutableList<Effect<S, I, E>>, Lazy<State>>? = null

        /**
         * Enter effects.
         */
        internal val enter = arrayListOf<Effect<S, Unit, E>>()

        /**
         * Exit effects
         */
        internal val exit = arrayListOf<Effect<S, Unit, E>>()

        /**
         * Transition map.
         */
        internal val next = hashMapOf<Any, Pair<MutableList<Effect<S, Any, E>>, Lazy<State>>>()

        /**
         * Transition map.
         */
        internal val nextGuard = arrayListOf<Pair<S.(I) -> Boolean, Pair<MutableList<Effect<S, I, E>>, Lazy<State>>>>()

        /**
         * Transition map.
         */
        internal val nextType = arrayListOf<Pair<KClass<*>, Pair<MutableList<Effect<S, Any, E>>, Lazy<State>>>>()

        /**
         * Transition map.
         */
        internal val nextTypeGuard = arrayListOf<Pair<Pair<KClass<*>, S.(Any) -> Boolean>, Pair<MutableList<Effect<S, Any, E>>, Lazy<State>>>>()


        /**
         * Shorthand for ```by default self```.
         */
        val defaultLoop: TExtra<I, E>
            get() =
                by default self
    }

    /**
     * Delegate for getting a state.
     * @param state The state to return.
     */
    protected inner class StateDelegate(
            private val state: State) {
        operator fun getValue(stateMachine: StateMachine<S, I, E>, kProperty: KProperty<*>) = state
    }

    /**
     * Provider for state delegate based on being [initial] and configured by [target].
     * @param initial True if this state is initial.
     * @param target The configurator.
     */
    protected inner class StateProvider(
            private val initial: Boolean,
            private val target: StateBuilder.() -> Unit) {
        operator fun provideDelegate(stateMachine: StateMachine<S, I, E>, kProperty: KProperty<*>): StateDelegate {

            // Initialize builder.
            val builder = StateBuilder(lazy {
                @Suppress("unchecked_cast")
                kProperty.call(stateMachine) as StateMachine<S, I, E>.State
            })

            // Configure to the builder.
            target(builder)

            // Create state.
            val state = State(kProperty.name,
                    builder.default,
                    builder.enter,
                    builder.exit,
                    builder.next,
                    builder.nextGuard,
                    builder.nextType,
                    builder.nextTypeGuard)

            // Assign initial if desired.
            if (initial)
                stateMachine.initialState = state

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
    private lateinit var initialState: State

    /**
     * Starts a simulation with the given optional receiver.
     * @param initial The initial user status.
     * @param receiver The receiver or null.
     * @return Returns a simulation.
     */
    fun run(initial: S, receiver: Receiver<S, E>? = null): Simulation<S, I> {
        // Current location in the simulation and running state.
        var status = initial
        var current = initialState
        var running = true

        // Handle initial behavior.
        if (receiver == null) {
            for (e in current.enter)
                if (e is Mutate)
                    e.block(status, Unit)
        } else {
            receiver.entering(status, current.name)

            for (e in current.enter)
                if (e is Mutate) {
                    val intermediate = e.block(status, Unit)
                    receiver.changed(status, intermediate)
                    status = intermediate
                } else if (e is Emit)
                    for (f in e.get(status, Unit))
                        receiver.emit(status, f)
        }

        // Return the simulation object.
        return object : Simulation<S, I> {
            override var status: S
                get() = status
                set(value) {
                    status = value
                }

            override val at: String
                get() = current.name

            override fun send(input: I) {
                // Check that still running.
                if (!running)
                    throw IllegalStateException("Simulation is completed.")

                // Get emitted event and states.
                val next = current.next[input]
                        ?: current.nextTypeGuard
                                .filter { (p, _) -> p.first.isInstance(input) && p.second(status, input) }
                                .map { (_, t) -> t }
                                .firstOrNull()
                        ?: current.nextGuard
                                .filter { (p, _) -> p(status, input) }
                                .map { (_, t) -> t }
                                .firstOrNull()
                        ?: current.nextType
                                .filter { (p, _) -> p.isInstance(input) }
                                .map { (_, t) -> t }
                                .firstOrNull()
                        ?: current.default

                // If invalid input, throw an exception.
                if (next == null)
                    throw IllegalArgumentException("Invalid input at state ${current.name}: no definition for $input.")

                // Get actual next state.
                val nextState = next.second.value

                if (receiver == null) {
                    // No receiver defined, do not notify while advancing.

                    // State actually changed, apply all mutations from exit definitions.
                    if (current != nextState)
                        for (e in current.exit)
                            if (e is Mutate)
                                status = e.block(status, Unit)

                    // Apply all mutations from transition definitions.
                    for (e in next.first)
                        if (e is Mutate)
                            status = e.block(status, input)

                    // State actually changed, advance state and apply all mutations from enter definitions.
                    if (current != nextState) {

                        // Advance state.
                        current = nextState

                        for (e in current.enter)
                            if (e is Mutate)
                                e.block(status, Unit)
                    }
                } else {
                    // Receiver defined, notify while advancing and emit events.

                    // State actually changed, apply all mutations and emits from exit definitions.
                    if (current != nextState)
                        for (e in current.exit)
                            if (e is Mutate) {
                                val intermediate = e.block(status, Unit)
                                receiver.changed(status, intermediate)
                                status = intermediate
                            } else if (e is Emit)
                                for (f in e.get(status, Unit))
                                    receiver.emit(status, f)

                    // Notify leaving
                    receiver.leaving(status, current.name)

                    // Apply all mutations from transition definitions.
                    for (e in next.first)
                        if (e is Mutate) {
                            val intermediate = e.block(status, input)
                            receiver.changed(status, intermediate)
                            status = intermediate
                        } else if (e is Emit)
                            for (f in e.get(status, input))
                                receiver.emit(status, f)

                    // Notify entering
                    receiver.entering(status, nextState.name)

                    // State actually changed, advance state and apply all mutations from enter definitions.
                    if (current != nextState) {

                        // Advance state.
                        current = nextState

                        for (e in current.enter)
                            if (e is Mutate) {
                                val intermediate = e.block(status, Unit)
                                receiver.changed(status, intermediate)
                                status = intermediate
                            } else if (e is Emit)
                                for (f in e.get(status, Unit))
                                    receiver.emit(status, f)
                    }
                }
            }

            override fun stop() {
                if (running) {
                    running = false

                    // Handle final behavior.
                    if (receiver == null) {
                        for (e in current.exit)
                            if (e is Mutate)
                                status = e.block(status, Unit)
                    } else {
                        // State actually changed, apply all mutations and emits from exit definitions.
                        for (e in current.exit)
                            if (e is Mutate) {
                                val intermediate = e.block(status, Unit)
                                receiver.changed(status, intermediate)
                                status = intermediate
                            } else if (e is Emit)
                                for (f in e.get(status, Unit))
                                    receiver.emit(status, f)

                        // Notify leaving
                        receiver.leaving(status, current.name)
                    }
                }
            }
        }
    }
}

/**
 * Wrapper for state machines without user status.
 */
fun <I : Any, E> StateMachine<Unit, I, E>.run(receiver: Receiver<Unit, E>? = null) =
        run(Unit, receiver)

/**
 * Runs a simulation during the [block] with the optional [receiver].
 * @param I The input type.
 * @param E The type of events.
 * @receiver The state machine to run.
 * @param initial The initial user status.
 * @param receiver The receiver to notify or null.
 * @param block The block that sends events.
 * @return Returns the state the machine stopped in.
 */
inline fun <S, I : Any, E> StateMachine<S, I, E>.runWith(
        initial: S, receiver: Receiver<S, E>? = null, block: Simulation<S, I>.() -> Unit): String {
    val s = run(initial, receiver)
    block(s)
    s.stop()
    return s.at
}

/**
 * Wrapper for state machines without user status.
 */
inline fun <I : Any, E> StateMachine<Unit, I, E>.runWith(
        receiver: Receiver<Unit, E>? = null, block: Simulation<Unit, I>.() -> Unit) =
        runWith(Unit, receiver, block)

/**
 * Exception that is thrown when [validate] fails.
 * @param s The message to use.
 * @param initial The initial user status used.
 * @param inputs The inputs that were used.
 * @param path The path that was actually taken.
 * @param expect The path that was expected.
 */
class StateMachineInvalidException(s: String,
                                   val initial: Any?,
                                   val inputs: List<Any?>,
                                   val path: List<String>,
                                   val expect: List<String>) : IllegalStateException(s)

/**
 * Runs the state machine with the given [initial] user status with [inputs]. If the path taken mismatches [expect], a
 * [StateMachineInvalidException] is thrown.
 */
fun <S, I : Any, E> StateMachine<S, I, E>.validate(initial: S, inputs: List<I>, expect: List<String>) {
    // Track path.
    val t = Path<S, E>()

    // Run state machine on all inputs.
    runWith(initial, t) { send(inputs) }

    // Check if input sequences mismatch.
    if (t.path.size > expect.size)
        throw StateMachineInvalidException("Path taken is longer than expected.", initial, inputs, t.path, expect)
    if (t.path.size < expect.size)
        throw StateMachineInvalidException("Path taken is shorter than expected.", initial, inputs, t.path, expect)
    for (i in 0 until t.path.size)
        if (t.path[i] != expect[i])
            throw StateMachineInvalidException("Path diverges at step $i, where ${expect[i]} was expected but ${t.path[i]} was returned.", initial, inputs, t.path, expect)
}

/**
 * Wrapper for state machines without user status.
 */
fun <I : Any, E> StateMachine<Unit, I, E>.validate(inputs: List<I>, expect: List<String>) {
    validate(Unit, inputs, expect)
}

object Test : StateMachine<Unit, Any, Any>() {
    val initial: State by init {
        on(1) goto ::mid emit "Going to mid"
        by default ::error
    }

    val mid: State by state {
        on(1) goto ::initial emitFrom { "Going to initial" }
        on<Int> { it in 2..10 } goto self emitFrom { "Going to self because of $it" }
        by default ::error
    }

    val error: State by state {
        defaultLoop emitLazy { IllegalStateException("In error state.") }
    }
}


object Stateful : StateMachine<Int, Any, Any>() {
    val initial: State by init {
        onEnter then {
            assertStatus(0)
        }
        on("inc") goto ::op1 mutate { this + 1 }
        on("dec") goto ::op1 mutate { this - 1 }
    }

    val op1: State by state {
        on("inc") goto ::op2 mutate { this + 1 }
        on("dec") goto ::op2 mutate { this - 1 }
    }
    val op2: State by state {
        on("inc") goto ::op3 mutate { this + 1 }
        on("dec") goto ::op3 mutate { this - 1 }
    }

    val op3: State by state {
        onEnter assertNull { "Value must be greater than -3" iff { this < -3 } }
        defaultLoop emitLazy { "Ops used up" }
    }
}

fun main(args: Array<String>) {
    Stateful.runWith(0, Log) {
        send("inc")
        send("dec")
        send("dec")
        send("inc")
        send("inc")
    }
    Stateful.runWith(1, Log) {
    }
//    Test.runWith(Log) {
//        send(1, 1, 1, 2, 12)
//    }
//    Test.validate(
//            listOf(1, 1, 1, 2, 12),
//            listOf("initial", "mid", "initial", "mid", "mid", "error"))
}