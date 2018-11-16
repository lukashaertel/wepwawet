package eu.metatools.wepwawet

import eu.metatools.rome.Action
import kotlin.reflect.KMutableProperty
import kotlin.reflect.KMutableProperty1
import kotlin.reflect.KProperty

/**
 * Boxed value to support null values in maps.
 */
private data class Box<out T>(val value: T)

/**
 * Shared undoing state. If this is true, undoing is active.
 */
private val undoing = ThreadLocal.withInitial { false }

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
        val unDelete: Set<Entity>)

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
     * Computes the run action for the call to [method] on argument [arg].
     */
    internal fun runAction(method: Method, arg: Any?): Action<Revision, *> {
        return object : Action<Revision, EntityUndo> {
            override fun exec(time: Revision): EntityUndo {
                // Clear tracker
                assigns.set(hashMapOf())
                creates.set(hashSetOf())
                deletes.set(hashSetOf())

                // Activate tracking
                tracking.set(true)

                // Call block
                table[method.toInt()](arg)

                // Deactivate tracking
                tracking.set(false)

                // Return undo
                return EntityUndo(assigns.get(), creates.get(), deletes.get())
            }

            override fun undo(time: Revision, carry: EntityUndo) {
                // Activate undoing
                undoing.set(true)

                for ((k, v) in carry.unAssign)
                    @Suppress("unchecked_cast")
                    (k.property as KMutableProperty1<Entity, Any?>).set(k.receiver, v.value)

                for (i in carry.unCreate)
                    container.unregisterEntity(i)

                for (e in carry.unDelete)
                    container.registerEntity(e)

                // Deactivate undoing
                undoing.set(false)
            }
        }
    }

    /**
     * Tracks setting of a property, must be called before assigned.
     */
    private fun trackSet(property: KMutableProperty1<Entity, Any?>) {
        // Check tracking validity
        if (!tracking.get() && !undoing.get())
            throw IllegalStateException("Setting from non-tracking or undoing area.")

        // Put previous value, without overwriting.
        if (!undoing.get())
            assigns.get().computeIfAbsent(BoundProperty(this, property)) { Box(property.get(this)) }
    }

    /**
     * Creates an entity with the given constructor and registers it with the container.
     */
    protected fun <E : Entity> create(constructor: (Container) -> E): E {
        // Validate call location
        if (!tracking.get() && !undoing.get())
            throw IllegalStateException("Creating from non-tracking or undoing area.")

        // Execute and add to container
        val e = constructor(container)
        container.registerEntity(e)

        // Track create if not undoing
        if (!undoing.get())
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
            constructor: (Container, A1, A2) -> E, a1: A1, a2: A2) =
            create { constructor(it, a1, a2) }

    /**
     * Creates an entity with the given constructor and registers it with the container.
     */
    protected fun <E : Entity, A1, A2, A3> create(
            constructor: (Container, A1, A2, A3) -> E, a1: A1, a2: A2, a3: A3) =
            create { constructor(it, a1, a2, a3) }

    /**
     * Creates an entity with the given constructor and registers it with the container.
     */
    protected fun <E : Entity, A1, A2, A3, A4> create(
            constructor: (Container, A1, A2, A3, A4) -> E, a1: A1, a2: A2, a3: A3, a4: A4) =
            create { constructor(it, a1, a2, a3, a4) }

    /**
     * Deletes the entity.
     */
    protected fun delete() {
        // Validate call location
        if (!tracking.get() && !undoing.get())
            throw IllegalStateException("Deleting from non-tracking or undoing area.")

        // Track delete and unregister if not undoing
        if (!undoing.get())
            deletes.get().add(this)

        // Execute and remove
        container.unregisterEntity(this)
    }

    /**
     * A proxy for an entity, i.e., to be resolved by the container.
     */
    private data class EntityProxy(val id: List<Any?>)

    /**
     * Tries to convert to a proxy or returns identity.
     */
    private fun tryToProxy(any: Any?) = if (any is Entity) toProxy(any) else any

    /**
     * Tries to convert from a proxy or returns identity.
     */
    private fun tryFromProxy(any: Any?) = if (any is EntityProxy) fromProxy(any) else any

    /**
     * Converts to a proxy.
     */
    private fun toProxy(entity: Entity) = EntityProxy(entity.primaryKey())

    /**
     * Converts from a proxy.
     */
    private fun fromProxy(entityProxy: EntityProxy) = container.find(entityProxy.id)

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
    protected fun <T> prop(initial: T, delta: (T, T) -> Unit = { _, _ -> }): Delegate<Entity, T> =
            Property(initial, delta)

    /**
     * Creates a single entity container that can be nullable. On value change, non-contained entities will be
     * removed. When not given explicitly, the property will be initialized with null.
     */
    protected fun <T : Entity> holdOptional(initial: T? = null, delta: (T?, T?) -> Unit = { _, _ -> }) =
            prop(initial) { x, y ->
                delta(x, y)
                if (!undoing.get())
                    if (x != null)
                        x.delete()
            }

    /**
     * Creates a single entity container. On value change, non-contained entities will be removed.
     */
    protected fun <T : Entity> holdOne(initial: T, delta: (T, T) -> Unit = { _, _ -> }) =
            prop(initial) { x, y ->
                delta(x, y)
                if (!undoing.get())
                    x.delete()
            }

    /**
     * Creates a many object container. On value change, non-contained entities will be removed. When not given
     * explicitly, the property will be initialized with an empty list.
     */
    protected fun <T : Entity> holdMany(initial: List<T> = listOf(), delta: (List<T>, List<T>) -> Unit = { _, _ -> }) =
            prop(initial) { xs, ys ->
                delta(xs, ys)
                if (!undoing.get())
                    for (x in xs)
                        if (x !in ys)
                            x.delete()
            }

    /**
     * An impulse without arguments.
     */
    private class Impulse0<in R : Entity>(
            val method: Method,
            val block: R.() -> Unit) : Delegate<R, () -> Unit> {
        override fun getValue(r: R, p: KProperty<*>) = { ->
            // Check that key is present
            if (!r.hasKey())
                throw IllegalStateException("Cannot call impulse entity without key.")

            // Compute key
            val key = r.primaryKey()

            if (tracking.get())
                r.block()
            else {
                r.container.receive(r.container.rev(), key, method, Unit)
                r.container.dispatch(r.container.rev(), key, method, Unit)
                r.container.incInner()
            }
        }
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

        Impulse0((r.table.size - 1).toMethod(), block)
    }

    /**
     * An impulse with one argument. Entity arguments will be dispatched via their [primaryKey].
     */
    private class Impulse1<in R : Entity, A>(
            val method: Method,
            val block: R.(A) -> Unit) : Delegate<R, (A) -> Unit> {
        override fun getValue(r: R, p: KProperty<*>) = { arg: A ->
            // Check that key is present
            if (!r.hasKey())
                throw IllegalStateException("Cannot call impulse entity without key.")

            // Compute key
            val key = r.primaryKey()

            if (tracking.get())
                r.block(arg)
            else {
                val parg = r.tryToProxy(arg)
                r.container.receive(r.container.rev(), key, method, parg)
                r.container.dispatch(r.container.rev(), key, method, parg)
                r.container.incInner()
            }
        }
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

        Impulse1((r.table.size - 1).toMethod(), block)
    }

    /**
     * An impulse with two arguments. Entity arguments will be dispatched via their [primaryKey].
     */
    private class Impulse2<in R : Entity, A1, A2>(
            val method: Method,
            val block: R.(A1, A2) -> Unit) : Delegate<R, (A1, A2) -> Unit> {
        override fun getValue(r: R, p: KProperty<*>) = { a1: A1, a2: A2 ->
            // Check that key is present
            if (!r.hasKey())
                throw IllegalStateException("Cannot call impulse entity without key.")

            // Compute key
            val key = r.primaryKey()

            if (tracking.get())
                r.block(a1, a2)
            else {
                val parg = listOf(r.tryToProxy(a1), r.tryToProxy(a2))
                r.container.receive(r.container.rev(), key, method, parg)
                r.container.dispatch(r.container.rev(), key, method, parg)
                r.container.incInner()
            }
        }
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
            val (a1, a2) = parg as List<*>
            block(tryFromProxy(a1) as A1, tryFromProxy(a2) as A2)
        }

        Impulse2((r.table.size - 1).toMethod(), block)
    }


    /**
     * An impulse with three arguments. Entity arguments will be dispatched via their [primaryKey].
     */
    private class Impulse3<in R : Entity, A1, A2, A3>(
            val method: Method,
            val block: R.(A1, A2, A3) -> Unit) : Delegate<R, (A1, A2, A3) -> Unit> {
        override fun getValue(r: R, p: KProperty<*>) = { a1: A1, a2: A2, a3: A3 ->
            // Check that key is present
            if (!r.hasKey())
                throw IllegalStateException("Cannot call impulse entity without key.")

            // Compute key
            val key = r.primaryKey()

            if (tracking.get())
                r.block(a1, a2, a3)
            else {
                val parg = listOf(r.tryToProxy(a1), r.tryToProxy(a2), r.tryToProxy(a3))
                r.container.receive(r.container.rev(), key, method, parg)
                r.container.dispatch(r.container.rev(), key, method, parg)
                r.container.incInner()
            }
        }
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
            val (a1, a2, a3) = parg as List<*>
            block(tryFromProxy(a1) as A1, tryFromProxy(a2) as A2, tryFromProxy(a3) as A3)
        }

        Impulse3((r.table.size - 1).toMethod(), block)
    }


    /**
     * An impulse with four arguments. Entity arguments will be dispatched via their [primaryKey].
     */
    private class Impulse4<in R : Entity, A1, A2, A3, A4>(
            val method: Method,
            val block: R.(A1, A2, A3, A4) -> Unit) : Delegate<R, (A1, A2, A3, A4) -> Unit> {
        override fun getValue(r: R, p: KProperty<*>) = { a1: A1, a2: A2, a3: A3, a4: A4 ->
            // Check that key is present
            if (!r.hasKey())
                throw IllegalStateException("Cannot call impulse entity without key.")

            // Compute key
            val key = r.primaryKey()

            if (tracking.get())
                r.block(a1, a2, a3, a4)
            else {
                val parg = listOf(r.tryToProxy(a1), r.tryToProxy(a2), r.tryToProxy(a3), r.tryToProxy(a4))
                r.container.receive(r.container.rev(), key, method, parg)
                r.container.dispatch(r.container.rev(), key, method, parg)
                r.container.incInner()
            }
        }
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
            val (a1, a2, a3, a4) = parg as List<*>
            block(tryFromProxy(a1) as A1, tryFromProxy(a2) as A2, tryFromProxy(a3) as A3, tryFromProxy(a4) as A4)
        }

        Impulse4((r.table.size - 1).toMethod(), block)
    }

}
