package eu.metatools.net

import java.util.TreeSet
import kotlin.reflect.KClass


/**
 * Required number size to encode [ClassMapper] identities.
 */
enum class NumSize {
    BYTE,
    SHORT,
    INT
}

/**
 * Maps classes to numbers and back. Maximum size is [Int].
 */
class ClassMapper {
    /**
     * Required size to encode the number.
     */
    val numSize
        get() = when (backing.size) {
            in 0..Byte.MAX_VALUE -> NumSize.BYTE
            in 0..Short.MAX_VALUE -> NumSize.SHORT
            in 0..Int.MAX_VALUE -> NumSize.INT
            else -> error("Illegal size of backing.")
        }

    private val backing = TreeSet<KClass<*>> { a, b ->
        val aq = a.qualifiedName
        val bq = b.qualifiedName
        when {
            aq != null && bq != null -> aq.compareTo(bq)
            aq == null -> 1
            bq == null -> -1
            else -> 0
        }
    }

    /**
     * Backing for number access.
     */
    private var toNum = mapOf<KClass<*>, Int>()

    /**
     * Backing for class access.
     */
    private var toClass = listOf<KClass<*>>()

    /**
     * Adds a class to the definition.
     */
    fun put(kClass: KClass<*>) {
        backing.add(kClass)
        toNum = backing.withIndex().associate { (k, v) -> v to k }
        toClass = backing.toList()
    }

    /**
     * Gets the number for the class of the object.
     */
    operator fun get(any: Any) =
            toNum[any::class] ?: error("Type of $any not in mapper.")

    /**
     * Gets the number for the class.
     */
    operator fun get(kClass: KClass<*>) =
            toNum[kClass] ?: error("$kClass not in mapper.")

    /**
     * Gets the class for the number.
     */
    operator fun get(int: Int) =
            toClass[int]
}

/**
 * Builder methods for [ClassMapper].
 */
interface ClassMapperBuilder {
    /**
     * Adds a class to the definition.
     */
    fun put(kClass: KClass<*>)
}

/**
 * Utility method for reified adding of classes to [ClassMapperBuilder].
 */
inline fun <reified T : Any> ClassMapperBuilder.with() {
    put(T::class)
}

/**
 * Builds a class mapper using a [ClassMapperBuilder].
 */
fun classMapper(builder: ClassMapperBuilder.() -> Unit): ClassMapper {
    // Initialize result class mapper.
    val result = ClassMapper()

    // Implementation of the builder.
    val target = object : ClassMapperBuilder {
        override fun put(kClass: KClass<*>) {
            result.put(kClass)
        }
    }

    // Build to implementation.
    builder(target)

    // Return the result mapper.
    return result
}
