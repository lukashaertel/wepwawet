package eu.metatools.wepwawet

import eu.metatools.rome.Action
import eu.metatools.wepwawet.Container.Periodic
import eu.metatools.wepwawet.tools.*
import kotlin.reflect.KMutableProperty
import kotlin.reflect.KMutableProperty1
import kotlin.reflect.KProperty

/**
 * Boxed value to support null values in maps.
 */
private data class Box<out T>(val value: T)

/**
 * Shared blind state. If this is true, blind is active.
 */
private val blind = ThreadLocal.withInitial { false }

/**
 * Shared tracking state. If this is true, tracking is active.
 */
private val tracking = ThreadLocal.withInitial { false }

/**
 * Current assignments.
 */
private val assigns = ThreadLocal<MutableMap<BoundProperty<*, *>, Box<Any?>>>()

/**
 * Current entity creations as set of identities.
 */
private val creates = ThreadLocal<MutableSet<Entity>>()

/**
 * Current deletes as set of deleted entities.
 */
private val deletes = ThreadLocal<MutableSet<Entity>>()

/**
 * Current registrations of periodics.
 */
private val register = ThreadLocal<MutableSet<Periodic>>()

/**
 * Current unregistration of periodics.
 */
private val unregister = ThreadLocal<MutableSet<Periodic>>()

/**
 * A property bound to it's receiver.
 */
private data class BoundProperty<T : Entity, R>(
        val receiver: T,
        val property: KMutableProperty1<T, R>)


/**
 * A undo batch.
 */
private data class EntityUndo(
        val unAssign: Map<BoundProperty<*, *>, Box<Any?>>,
        val unCreate: Set<Entity>,
        val unDelete: Set<Entity>,
        val unRegister: Set<Periodic>,
        val unUnregister: Set<Periodic>)

/**
 * Auto generated key components for entities.
 */
enum class AutoKeyMode {
    /**
     * Do not generate a key, this can be used for data only entities with no impulses.
     */
    NONE,

    /**
     * Generate key from class name.
     */
    PER_CLASS,
}

/**
 * An entity in [container] with [id].
 */
abstract class Entity(val container: Container, val autoKeyMode: AutoKeyMode = AutoKeyMode.PER_CLASS) {
    /**
     * Local impulse table for resolution of external calls.
     */
    private val table = arrayListOf<(Any?) -> Unit>()

    private val periodics = hashSetOf<Periodic>()

    /**
     * Local key table for key computation.
     */
    private val keys = when (autoKeyMode) {
        AutoKeyMode.NONE -> arrayListOf<() -> Any?>()
        AutoKeyMode.PER_CLASS -> {
            val a = javaClass.simpleName
            arrayListOf<() -> Any?>({ a })
        }
    }

    /**
     * Returns true if the entity has a key.
     */
    internal fun hasKey() = keys.isNotEmpty()

    /**
     * Computes the current entity key.
     */
    internal fun primaryKey() = keys.map { it() }

    /**
     * Computes the run action for the call to [call] on argument [arg].
     */
    internal fun runAction(call: Byte, arg: Any?): Action<Revision, *> {
        return object : Action<Revision, EntityUndo> {
            override fun exec(time: Revision): EntityUndo {
                // Clear tracker
                assigns.set(hashMapOf())
                creates.set(hashSetOf())
                deletes.set(hashSetOf())
                register.set(hashSetOf())
                unregister.set(hashSetOf())

                // Activate tracking
                tracking.set(true)

                // Call block
                table[call.toInt()](arg)

                // Deactivate tracking
                tracking.set(false)

                // Return undo
                return EntityUndo(assigns.get(), creates.get(), deletes.get(), register.get(), unregister.get())
            }

            override fun undo(time: Revision, carry: EntityUndo) {
                // Activate blind
                blind.set(true)

                for ((k, v) in carry.unAssign)
                    @Suppress("unchecked_cast")
                    (k.property as KMutableProperty1<Entity, Any?>).set(k.receiver, v.value)

                for (i in carry.unCreate)
                    container.unregisterEntity(i)

                for (e in carry.unDelete)
                    container.registerEntity(e)

                for (p in carry.unRegister) {
                    container.unregisterPeriodic(p)
                    periodics.remove(p)
                }

                for (p in carry.unUnregister) {
                    container.restorePeriodic(p)
                    periodics.add(p)
                }

                // Deactivate blind
                blind.set(false)
            }
        }
    }

    /**
     * Tracks setting of a property, must be called before assigned.
     */
    private fun trackSet(property: KMutableProperty1<Entity, Any?>) {
        // Check tracking validity
        if (!tracking.get() && !blind.get())
            throw IllegalStateException("Setting from non-tracking or blind area.")

        // Put previous value, without overwriting.
        if (!blind.get())
            assigns.get().computeIfAbsent(BoundProperty(this, property), { Box(property.get(this)) })
    }

    /**
     * Creates an entity with the given constructor and registers it with the container.
     */
    protected fun <E : Entity> create(constructor: (Container) -> E): E {
        // Validate call location
        if (!tracking.get() && !blind.get())
            throw IllegalStateException("Creating from non-tracking or blind area.")

        // Execute and add to container
        val e = constructor(container)
        container.registerEntity(e)

        // Track create if not blind
        if (!blind.get())
            creates.get().add(e)

        return e
    }

    /**
     * Creates an entity with the given constructor and registers it with the container.
     */
    protected fun <E : Entity, A> create(
            constructor: (Container, A) -> E, arg: A) =
            create { constructor(it, arg) }

    /**
     * Creates an entity with the given constructor and registers it with the container.
     */
    protected fun <E : Entity, A1, A2> create(
            constructor: (Container, A1, A2) -> E, arg1: A1, arg2: A2) =
            create { constructor(it, arg1, arg2) }

    /**
     * Creates an entity with the given constructor and registers it with the container.
     */
    protected fun <E : Entity, A1, A2, A3> create(
            constructor: (Container, A1, A2, A3) -> E, arg1: A1, arg2: A2, arg3: A3) =
            create { constructor(it, arg1, arg2, arg3) }

    /**
     * Creates an entity with the given constructor and registers it with the container.
     */
    protected fun <E : Entity, A1, A2, A3, A4> create(
            constructor: (Container, A1, A2, A3, A4) -> E, arg1: A1, arg2: A2, arg3: A3, arg4: A4) =
            create { constructor(it, arg1, arg2, arg3, arg4) }

    /**
     * Deletes the entity.
     */
    protected fun delete(entity: Entity) {
        // Validate call location
        if (!tracking.get() && !blind.get())
            throw IllegalStateException("Deleting from non-tracking or blind area.")

        // Track delete and unregister if not blind
        if (!blind.get()) {
            deletes.get().add(entity)
            for (p in periodics)
                unregister.get().add(p)
        }

        // Unregister all periodics
        for (p in periodics)
            container.unregisterPeriodic(p)

        // Execute and remove
        container.unregisterEntity(entity)
    }

    /**
     * Formats the members for [toString].
     */
    open fun toStringMembers() = ""

    override fun toString() = toStringMembers().let {
        if (it.isNotEmpty())
            "[${javaClass.simpleName}, key=${primaryKey()}, $it]"
        else
            "[${javaClass.simpleName}, key=${primaryKey()}]"
    }

    /**
     * A proxy for an entity, i.e., to be resolved by the container.
     */
    private data class EntityProxy(val id: List<Any?>)

    /**
     * Tries to convert to a proxy or returns identity.
     */
    private fun tryToProxy(any: Any?) =
            if (any is Entity)
                toProxy(any)
            else
                any

    /**
     * Tries to convert from a proxy or returns identity.
     */
    private fun tryFromProxy(any: Any?) =
            if (any is EntityProxy)
                fromProxy(any)
            else
                any


    /**
     * Converts to a proxy.
     */
    private fun toProxy(entity: Entity) =
            EntityProxy(entity.primaryKey())

    /**
     * Converts from a proxy.
     */
    private fun fromProxy(entityProxy: EntityProxy) =
            container.find(entityProxy.id)

    /**
     * A tracking property identifying the entity.
     */
    private class Key<T>(initial: T, val delta: (T, T) -> Unit) : Delegate<Entity, T> {
        /**
         * Internal storage field.
         */
        internal var status = initial

        override fun getValue(r: Entity, p: KProperty<*>): T {
            return status
        }

        @Suppress("unchecked_cast")
        override fun setValue(r: Entity, p: KProperty<*>, value: T) {
            // Skip non-mutation
            if (value == status) return

            // Remove from old identity
            r.container.unregisterEntity(r)

            // Track set
            r.trackSet(p as KMutableProperty1<Entity, Any?>)

            // Write status
            val prev = status
            status = value

            // Add to new identity
            r.container.registerEntity(r)

            // Execute delta
            delta(prev, value)
        }
    }

    /**
     * Creates and registers a key. If [T] is an entity, the [primaryKey] of the value will be used instead.
     */
    protected fun <T> key(initial: T, delta: (T, T) -> Unit) = Provider { r: Entity, _ ->
        // Return key, also add the internal getter to the key providers
        Key(initial, delta).also {
            r.keys.add {
                it.status.let {
                    if (it is Entity)
                        it.primaryKey()
                    else
                        it
                }
            }
        }
    }

    protected fun <T> key(initial: T) =
            key(initial) { _, _ -> }

    /**
     * A tracking property.
     */
    private class Property<T>(initial: T, val delta: (T, T) -> Unit) : Delegate<Entity, T> {
        /**
         * Internal storage field.
         */
        internal var status = initial

        override fun getValue(r: Entity, p: KProperty<*>): T {
            return status
        }

        @Suppress("unchecked_cast")
        override fun setValue(r: Entity, p: KProperty<*>, value: T) {
            // Skip non-mutation
            if (value == status) return

            // Track set
            r.trackSet(p as KMutableProperty1<Entity, Any?>)

            // Write status
            val prev = status
            status = value

            // Execute delta
            delta(prev, value)
        }
    }

    /**
     * Creates a tracking property with a delta reactor.
     */
    protected fun <T> prop(initial: T, delta: (T, T) -> Unit): Delegate<Entity, T> =
            Property(initial, delta)

    /**
     * Creates a tracking property with a delta reactor.
     */
    protected fun <T> prop(initial: T, new: (T) -> Unit): Delegate<Entity, T> =
            prop(initial) { _, y -> new(y) }

    /**
     * Creates a tracking property.
     */
    protected fun <T> prop(initial: T) =
            prop(initial) { _, _ -> }

    /**
     * Creates a single entity container that can be nullable. On value change, non-contained entities will be
     * removed. When not given explicitly, the property will be initialized with null.
     */
    protected fun <T : Entity> holdOptional(initial: T? = null, delta: (T?, T?) -> Unit) =
            prop(initial) { x, y ->
                delta(x, y)
                if (!blind.get())
                    if (x != null)
                        delete(x)
            }


    private class Pulse<in R : Entity>(
            val block: () -> Unit) : Delegate<R, PeriodicFunction> {
        var periodic: Periodic? = null

        override fun getValue(r: R, p: KProperty<*>) = periodicFunction({ delay, interval ->
            println("Registering ${p.name}, $delay $interval at ${r.container.rev()}")

            if (!tracking.get())
                throw IllegalStateException("Cannot register periodic from outside impulse.")

            // Do not register twice
            if (periodic == null) {
                // Register a new periodic
                r.container.registerPeriodic(r.container.rev(), delay, interval, block).let {
                    // Assign it for un-registration
                    periodic = it

                    // Add to local set of periodics for deletes.
                    r.periodics.add(it)

                    // Track registration
                    register.get().add(it)
                }
            }
        }, {
            println("Unregistering ${p.name}, at ${r.container.rev()}")

            if (!tracking.get())
                throw IllegalStateException("Cannot unregister periodic from outside impulse.")

            // Only apply if present
            periodic?.let {
                // Unregister the periodic
                r.container.unregisterPeriodic(it)

                // Remove from local set of periodics for deletes.
                r.periodics.remove(it)

                // Reset it
                periodic = null

                // Track un-registration
                unregister.get().add(it)
            }

        })

    }

    protected fun <R : Entity> R.pulse(block: () -> Unit) = Provider { r: R, p ->
        if (p is KMutableProperty<*>)
            throw IllegalArgumentException("Pulses cannot be mutable fields.")

        Pulse<R> {
            if (tracking.get())
                throw IllegalStateException("Pulse body called from a tracking method.")

            // Activate blind mode, everyone shares the pulse so block is safe.
            blind.set(true)

            // Invoke block.
            block()

            // Reset tracking.
            blind.set(false)
        }
    }

    /**
     * Creates a single entity container that can be nullable. On value change, non-contained entities will be
     * removed. When not given explicitly, the property will be initialized with null.
     */
    protected fun <T : Entity> holdOptional(initial: T? = null, new: (T?) -> Unit) =
            holdOptional(initial) { _, y -> new(y) }

    /**
     * Creates a single entity container that can be nullable. On value change, non-contained entities will be
     * removed. When not given explicitly, the property will be initialized with null.
     */
    protected fun <T : Entity> holdOptional(initial: T? = null) =
            holdOptional(initial) { _, _ -> }

    /**
     * Creates a single entity container. On value change, non-contained entities will be removed.
     */
    protected fun <T : Entity> holdOne(initial: T, delta: (T, T) -> Unit) =
            prop(initial) { x, y ->
                delta(x, y)
                if (!blind.get())
                    delete(x)
            }

    /**
     * Creates a single entity container. On value change, non-contained entities will be removed.
     */
    protected fun <T : Entity> holdOne(initial: T, new: (T) -> Unit) =
            holdOne(initial) { _, y -> new(y) }

    /**
     * Creates a single entity container. On value change, non-contained entities will be removed.
     */
    protected fun <T : Entity> holdOne(initial: T) =
            holdOne(initial) { _, _ -> }

    /**
     * Creates a many object container. On value change, non-contained entities will be removed. When not given
     * explicitly, the property will be initialized with an empty list.
     */
    protected fun <T : Entity> holdMany(initial: List<T> = listOf(), delta: (List<T>, List<T>) -> Unit) =
            prop(initial) { xs, ys ->
                delta(xs, ys)
                if (!blind.get())
                    for (x in xs)
                        if (x !in ys)
                            delete(x)
            }

    /**
     * Creates a many object container. On value change, non-contained entities will be removed. When not given
     * explicitly, the property will be initialized with an empty list.
     */
    protected fun <T : Entity> holdMany(initial: List<T> = listOf(), new: (List<T>) -> Unit) =
            holdMany(initial) { _, y -> new(y) }

    /**
     * Creates a many object container. On value change, non-contained entities will be removed. When not given
     * explicitly, the property will be initialized with an empty list.
     */
    protected fun <T : Entity> holdMany(initial: List<T> = listOf()) =
            holdMany(initial) { _, _ -> }

    /**
     * An impulse without arguments.
     */
    private class Impulse0<in R : Entity>(
            val call: Byte,
            val block: R.() -> Unit) : Delegate<R, IndexFunction0<Double, Unit>> {
        override fun getValue(r: R, p: KProperty<*>) = indexFunction<Double, Unit>({ ->
            // Check that key is present
            if (!r.hasKey())
                throw IllegalStateException("Cannot call impulse entity without key.")

            // Compute key
            val key = r.primaryKey()

            if (tracking.get())
                r.block()
            else {
                r.container.receive(r.container.rev(), key, call, Unit)
                r.container.dispatch(r.container.rev(), key, call, Unit)
                r.container.incInner()
            }
        }, { delay ->
            if (tracking.get())
                throw IllegalStateException("Cannot delay from within impulse.")

            val prev = r.container.time
            r.container.time += (delay * 1000.0).toInt()

            val key = r.primaryKey()
            r.container.receive(r.container.rev(), key, call, Unit)
            r.container.dispatch(r.container.rev(), key, call, Unit)
            r.container.incInner()

            r.container.time = prev
        })
    }

    /**
     * Creates and registers an impulse.
     */
    @Suppress("unchecked_cast")
    @JvmName("impulse0")
    protected fun <R : Entity> R.impulse(block: R.() -> Unit) = Provider { r: R, p ->
        if (p is KMutableProperty<*>)
            throw IllegalArgumentException("Impulses cannot be mutable fields.")

        r.table.add { _ ->
            block()
        }

        Impulse0((r.table.size - 1).toByte(), block)
    }

    /**
     * An impulse with one argument. Entity arguments will be dispatched via their [primaryKey].
     */
    private class Impulse1<in R : Entity, A>(
            val call: Byte,
            val block: R.(A) -> Unit) : Delegate<R, IndexFunction1<A, Double, Unit>> {
        override fun getValue(r: R, p: KProperty<*>) = indexFunction<A, Double, Unit>({ arg ->
            // Check that key is present
            if (!r.hasKey())
                throw IllegalStateException("Cannot call impulse entity without key.")

            // Compute key
            val key = r.primaryKey()

            if (tracking.get())
                r.block(arg)
            else {
                val parg = r.tryToProxy(arg)
                r.container.receive(r.container.rev(), key, call, parg)
                r.container.dispatch(r.container.rev(), key, call, parg)
                r.container.incInner()
            }
        }, { arg, delay ->
            if (tracking.get())
                throw IllegalStateException("Cannot delay from within impulse.")

            val prev = r.container.time
            r.container.time += (delay * 1000.0).toInt()

            val key = r.primaryKey()
            val parg = r.tryToProxy(arg)
            r.container.receive(r.container.rev(), key, call, parg)
            r.container.dispatch(r.container.rev(), key, call, parg)
            r.container.incInner()

            r.container.time = prev
        })
    }

    /**
     * Creates and registers an impulse. Entity arguments will be dispatched via their [primaryKey].
     */
    @Suppress("unchecked_cast")
    @JvmName("impulse1")
    protected fun <R : Entity, A> R.impulse(block: R.(A) -> Unit) = Provider { r: R, p ->
        if (p is KMutableProperty<*>)
            throw IllegalArgumentException("Impulses cannot be mutable fields.")

        r.table.add { parg ->
            block(tryFromProxy(parg) as A)
        }

        Impulse1((r.table.size - 1).toByte(), block)
    }

    /**
     * An impulse with two arguments. Entity arguments will be dispatched via their [primaryKey].
     */
    private class Impulse2<in R : Entity, A1, A2>(
            val call: Byte,
            val block: R.(A1, A2) -> Unit) : Delegate<R, IndexFunction2<A1, A2, Double, Unit>> {
        override fun getValue(r: R, p: KProperty<*>) = indexFunction<A1, A2, Double, Unit>({ arg1, arg2 ->
            // Check that key is present
            if (!r.hasKey())
                throw IllegalStateException("Cannot call impulse entity without key.")

            // Compute key
            val key = r.primaryKey()

            if (tracking.get())
                r.block(arg1, arg2)
            else {
                val parg = listOf(r.tryToProxy(arg1), r.tryToProxy(arg2))
                r.container.receive(r.container.rev(), key, call, parg)
                r.container.dispatch(r.container.rev(), key, call, parg)
                r.container.incInner()
            }
        }, { arg1, arg2, delay ->
            if (tracking.get())
                throw IllegalStateException("Cannot delay from within impulse.")

            val prev = r.container.time
            r.container.time += (delay * 1000.0).toInt()

            val key = r.primaryKey()
            val parg = listOf(r.tryToProxy(arg1), r.tryToProxy(arg2))
            r.container.receive(r.container.rev(), key, call, parg)
            r.container.dispatch(r.container.rev(), key, call, parg)
            r.container.incInner()

            r.container.time = prev
        })
    }

    /**
     * Creates and registers an impulse. Entity arguments will be dispatched via their [primaryKey].
     */
    @Suppress("unchecked_cast")
    @JvmName("impulse2")
    protected fun <R : Entity, A1, A2> R.impulse(block: R.(A1, A2) -> Unit) = Provider { r: R, p ->
        if (p is KMutableProperty<*>)
            throw IllegalArgumentException("Impulses cannot be mutable fields.")

        r.table.add { parg ->
            val arg = parg as List<*>
            val arg1 = tryFromProxy(arg[0]) as A1
            val arg2 = tryFromProxy(arg[1]) as A2
            block(arg1, arg2)
        }

        Impulse2((r.table.size - 1).toByte(), block)
    }


    /**
     * An impulse with three arguments. Entity arguments will be dispatched via their [primaryKey].
     */
    private class Impulse3<in R : Entity, A1, A2, A3>(
            val call: Byte,
            val block: R.(A1, A2, A3) -> Unit) : Delegate<R, IndexFunction3<A1, A2, A3, Double, Unit>> {
        override fun getValue(r: R, p: KProperty<*>) = indexFunction<A1, A2, A3, Double, Unit>({ arg1, arg2, arg3 ->
            // Check that key is present
            if (!r.hasKey())
                throw IllegalStateException("Cannot call impulse entity without key.")

            // Compute key
            val key = r.primaryKey()

            if (tracking.get())
                r.block(arg1, arg2, arg3)
            else {
                val parg = listOf(r.tryToProxy(arg1), r.tryToProxy(arg2), r.tryToProxy(arg3))
                r.container.receive(r.container.rev(), key, call, parg)
                r.container.dispatch(r.container.rev(), key, call, parg)
                r.container.incInner()
            }
        }, { arg1, arg2, arg3, delay ->
            if (tracking.get())
                throw IllegalStateException("Cannot delay from within impulse.")

            val prev = r.container.time
            r.container.time += (delay * 1000.0).toInt()

            val key = r.primaryKey()
            val parg = listOf(r.tryToProxy(arg1), r.tryToProxy(arg2), r.tryToProxy(arg3))
            r.container.receive(r.container.rev(), key, call, parg)
            r.container.dispatch(r.container.rev(), key, call, parg)
            r.container.incInner()

            r.container.time = prev
        })
    }

    /**
     * Creates and registers an impulse. Entity arguments will be dispatched via their [primaryKey].
     */
    @Suppress("unchecked_cast")
    @JvmName("impulse3")
    protected fun <R : Entity, A1, A2, A3> R.impulse(block: R.(A1, A2, A3) -> Unit) = Provider { r: R, p ->
        if (p is KMutableProperty<*>)
            throw IllegalArgumentException("Impulses cannot be mutable fields.")

        r.table.add { parg ->
            val arg = parg as List<*>
            val arg1 = tryFromProxy(arg[0]) as A1
            val arg2 = tryFromProxy(arg[1]) as A2
            val arg3 = tryFromProxy(arg[2]) as A3
            block(arg1, arg2, arg3)
        }

        Impulse3((r.table.size - 1).toByte(), block)
    }


    /**
     * An impulse with four arguments. Entity arguments will be dispatched via their [primaryKey].
     */
    private class Impulse4<in R : Entity, A1, A2, A3, A4>(
            val call: Byte,
            val block: R.(A1, A2, A3, A4) -> Unit) : Delegate<R, IndexFunction4<A1, A2, A3, A4, Double, Unit>> {
        override fun getValue(r: R, p: KProperty<*>) = indexFunction<A1, A2, A3, A4, Double, Unit>({ arg1, arg2, arg3, arg4 ->
            // Check that key is present
            if (!r.hasKey())
                throw IllegalStateException("Cannot call impulse entity without key.")

            // Compute key
            val key = r.primaryKey()

            if (tracking.get())
                r.block(arg1, arg2, arg3, arg4)
            else {
                val parg = listOf(r.tryToProxy(arg1), r.tryToProxy(arg2), r.tryToProxy(arg3), r.tryToProxy(arg4))
                r.container.receive(r.container.rev(), key, call, parg)
                r.container.dispatch(r.container.rev(), key, call, parg)
                r.container.incInner()
            }
        }, { arg1, arg2, arg3, arg4, delay ->
            if (tracking.get())
                throw IllegalStateException("Cannot delay from within impulse.")

            val prev = r.container.time
            r.container.time += (delay * 1000.0).toInt()

            val key = r.primaryKey()
            val parg = listOf(r.tryToProxy(arg1), r.tryToProxy(arg2), r.tryToProxy(arg3), r.tryToProxy(arg4))
            r.container.receive(r.container.rev(), key, call, parg)
            r.container.dispatch(r.container.rev(), key, call, parg)
            r.container.incInner()

            r.container.time = prev
        })
    }

    /**
     * Creates and registers an impulse. Entity arguments will be dispatched via their [primaryKey].
     */
    @Suppress("unchecked_cast")
    @JvmName("impulse4")
    protected fun <R : Entity, A1, A2, A3, A4> R.impulse(block: R.(A1, A2, A3, A4) -> Unit) = Provider { r: R, p ->
        if (p is KMutableProperty<*>)
            throw IllegalArgumentException("Impulses cannot be mutable fields.")

        r.table.add { parg ->
            val arg = parg as List<*>
            val arg1 = tryFromProxy(arg[0]) as A1
            val arg2 = tryFromProxy(arg[1]) as A2
            val arg3 = tryFromProxy(arg[2]) as A3
            val arg4 = tryFromProxy(arg[3]) as A4
            block(arg1, arg2, arg3, arg4)
        }

        Impulse4((r.table.size - 1).toByte(), block)
    }

}
