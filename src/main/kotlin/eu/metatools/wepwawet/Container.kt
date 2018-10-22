package eu.metatools.wepwawet

import eu.metatools.common.pmod
import eu.metatools.rome.Action
import eu.metatools.rome.Repo

/**
 * Undo in at container level, includes entity lookup.
 */
private data class ContainerUndo(
        val entity: Entity,
        val action: Action<Revision, *>,
        val carry: Any?)

/**
 * Exchange manager and change tracker.
 */
abstract class Container(val author: Byte) {
    companion object {
        val MATCH_ANY = Any()
    }

    /**
     * Substitute outer time.
     */
    private var subTime: Long? = null

    /**
     * Substitute inner time.
     */
    private var subInner: Short? = null

    /**
     * Substitute author.
     */
    private var subAuthor: Byte? = null

    /**
     * Run block in substituted time.
     */
    private inline fun <T> subIn(time: Long?, inner: Short?, author: Byte?, block: () -> T): T {
        val prevSubTime = subTime
        val prevSubInner = subInner
        val prevSubAuthor = subAuthor
        subTime = time
        subInner = inner
        subAuthor = author

        val r = block()

        subTime = prevSubTime
        subInner = prevSubInner
        subAuthor = prevSubAuthor
        return r
    }

    /**
     * Backing for the current insert time.
     */
    private var timeBacking: Long = 0

    /**
     * Backing for the inner insert time.
     */
    private var innerBacking: Short = 0

    /**
     * Calculates the next open inner revision time for the given [time].
     */
    internal fun openInner(time: Long): Short {
        // Get revision sub set for the time region
        val area = repo.revisions.subSet(
                Revision(time, Short.MIN_VALUE, author),
                Revision(time, Short.MAX_VALUE, author))

        if (area.isEmpty())
            return 0

        // Select the highest inner value to append to it.
        val last = area.mapNotNull {
            if (it.author == author)
                it.inner
            else
                null
        }.max()

        return last?.inc() ?: 0
    }

    /**
     * Calculates the next open revision for the given [time].
     */
    internal fun openTime(time: Long) =
            Revision(time, openInner(time), author)

    /**
     * Current insert time.
     */
    var time
        get() = timeBacking
        set(value) {
            timeBacking = value
            innerBacking = openInner(value)
        }

    /**
     * Increments the inner call counter.
     */
    internal fun incInner() {
        if (innerBacking == Short.MAX_VALUE)
            throw IllegalStateException("Maximum call number is exceeded.")
        innerBacking++
    }

    /**
     * Computes a new [Revision] object representing the current time and authorship (may be substituted during an
     * execution).
     */
    fun rev() =
            Revision(subTime ?: timeBacking, subInner ?: innerBacking, subAuthor ?: author)


    /**
     * Repository for change rollback.
     */
    internal val repo = Repo<Revision>().also { it.softUpper = Revision(0, Short.MAX_VALUE, Byte.MAX_VALUE) }

    /**
     * Map of primary key to entities.
     */
    private val indexBacking = hashMapOf<List<Any?>, Entity>()

    /**
     * Gets the index of the container.
     */
    val index get() = indexBacking.toMap()

    /**
     * Registers an entity with the index.
     */
    internal fun registerEntity(entity: Entity) {
        if (entity.hasKey())
            if (indexBacking.put(entity.primaryKey(), entity) != null)
                throw IllegalStateException("Entity $entity has overlapping key.")
    }

    /**
     * Unregisters an entity from the index.
     */
    internal fun unregisterEntity(entity: Entity) {
        if (entity.hasKey())
            if (!indexBacking.remove(entity.primaryKey(), entity))
                throw IllegalStateException("Entity $entity has internal mutation on key.")
    }

    /**
     * Gets the entity for the ID.
     */
    fun find(id: List<Any?>) =
            indexBacking[id]

    /**
     * Finds all matching entities, where [MATCH_ANY] entries are arbitrary.
     */
    fun match(id: List<Any?>) =
            indexBacking.filterKeys {
                // Same size and same positions have matching values
                id.size == it.size && (id zip it).all { (a, b) -> a === MATCH_ANY || a == b }
            }

    /**
     * Finds all matching entities of type [T], where [MATCH_ANY] entries are arbitrary. The [AutoKeyMode] will be
     * automatically filled.
     */
    @JvmName("matchWithType")
    inline fun <reified T : Entity> match(id: List<Any?>): Map<List<Any?>, T> {
        val result = hashMapOf<List<Any?>, T>()
        for ((k, v) in index)
            if (v is T)
                result.put(k, v)

        // No items, return immediately
        if (result.isEmpty())
            return result

        // Get first value and select by it's auto key mode.
        val first = result.values.first()
        val fullId = when (first.autoKeyMode) {
            AutoKeyMode.NONE -> id
            AutoKeyMode.PER_CLASS -> listOf(T::class.java.simpleName) + id
        }

        // Remove mismatching entries
        result.entries.iterator().apply {
            while (hasNext()) {
                val (itemId, _) = next()

                // Same size and same positions have matching values
                if (fullId.size != itemId.size)
                    remove()
                else if ((fullId zip itemId).any { (a, b) -> a !== MATCH_ANY && a != b })
                    remove()
            }
        }

        // Return the assignments
        return result
    }

    /**
     * Dispatches the [call] on [id] with argument [arg].
     */
    abstract fun dispatch(time: Revision, id: List<Any?>, call: Byte, arg: Any?)

    /**
     * Handles an external [call] on [id] with argument [arg].
     */
    fun receive(time: Revision, id: List<Any?>, call: Byte, arg: Any?) {
        // Insert into repository
        repo.insert(object : Action<Revision, ContainerUndo?> {
            override fun exec(time: Revision): ContainerUndo? {
                return subIn(time.time, time.inner, time.author) {
                    // Resolve entity, if not present, don't do anything
                    val target = find(id)

                    if (target == null) {
                        null
                    } else {
                        // Otherwise execute nested action and return composed undo
                        val nestedAction = target.runAction(call, arg)
                        val nestedUndo = nestedAction.exec(time)
                        ContainerUndo(target, nestedAction, nestedUndo)
                    }
                }
            }

            override fun undo(time: Revision, carry: ContainerUndo?) {
                subIn(time.time, time.inner, time.author) {
                    // If Exec was successful, undo
                    if (carry != null) {
                        @Suppress("unchecked_cast")
                        (carry.action as Action<Revision, Any?>).undo(time, carry.carry)
                    }
                }
            }

            override fun toString() = "$id.$call($arg)"
        }, time)
    }

    /**
     * Initializes the backing repository with the container, this is then the root for exchanging calls.
     */
    fun <E : Entity> init(constructor: (Container) -> E): E {
        return constructor(this).also {
            registerEntity(it)
        }
    }

    /**
     * Periodic registry.
     * @property key The associated key of this periodic
     * @property origin The origin of the time sequence
     * @property interval The interval of the time sequence
     * @property block The block to execute
     */
    internal data class Periodic(val key: Any, val origin: Long, val interval: Int, var last: Long?, val block: () -> Unit) {
        private fun firstOffset(current: Long) =
                (origin - current) pmod interval

        fun allBetween(current: Long, to: Long) =
                generateSequence(current + firstOffset(current)) {
                    it + interval
                }.takeWhile {
                    it < to
                }

    }

    /**
     * Table of periodic calls. These are all an effect of local impulses and are not exchanged.
     */
    private val periodic = mutableMapOf<Any, Periodic>()

    /**
     * Registers a periodic block.
     * @param origin The origin of the time sequence
     * @param interval The interval of the time sequence
     * @param block The block to run
     * @return Returns the registry entry.
     */
    internal fun registerPeriodic(at: Revision, delay: Int, interval: Int, block: () -> Unit) =
            Any().let { k ->
                Periodic(k, at.time + delay, interval, null, block).also {
                    periodic[k] = it
                }
            }

    /**
     * Restores a periodic registry entry.
     */
    internal fun restorePeriodic(restore: Periodic) {
        periodic[restore.key] = restore
    }

    /**
     * Unregisters a periodic block.
     */
    internal fun unregisterPeriodic(unregister: Periodic) {
        periodic.remove(unregister.key)
    }

    fun revise(targetTime: Long) {
        for (p in periodic.values) {
            for (t in p.allBetween(p.last ?: p.origin, targetTime))
                subIn(t, null, Byte.MAX_VALUE, p.block)
            p.last = targetTime
        }

        time = targetTime
        repo.softUpper = Revision(targetTime, Short.MAX_VALUE, Byte.MAX_VALUE)
    }
}

inline fun <reified T> Container.findAuto(vararg remaining: Any?): T? {
    val key = listOf(T::class.simpleName) + remaining
    return find(key) as T?
}

/**
 * Gets the entity for the ID.
 */
fun Container.findBy(vararg id: Any?) =
        find(id.toList())