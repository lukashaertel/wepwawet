package eu.metatools.common

/**
 * Calls the block if the receiver is of type [T], otherwise does not apply the block.
 */
inline fun <reified T> Any?.applyIfIs(block: T.() -> Unit) {
    if (this is T) block(this)
}

/**
 * Composes two consumers sequentially.
 */
inline infix fun (() -> Unit).then(crossinline other: () -> Unit) = { ->
    this()
    other()
}

/**
 * Composes two consumers sequentially.
 */
inline infix fun <T> ((T) -> Unit).then(crossinline other: (T) -> Unit) = { t: T ->
    this(t)
    other(t)
}

/**
 * Composes two consumers sequentially.
 */
inline infix fun <T, U> ((T, U) -> Unit).then(crossinline other: (T, U) -> Unit) = { t: T, u: U ->
    this(t, u)
    other(t, u)
}

fun powTen(exponent: Int): Number =
        if (exponent < 0)
            (1..-exponent).fold(1.0) { x, _ -> x / 10.0 }
        else
            (1..exponent).fold(1) { x, _ -> x * 10 }

/**
 * Rounds the value with [digits] after the comma.
 */

fun Double.round(digits: Int = 0) = when (digits) {
    -2 -> Math.round(this / 100.0) * 100.0
    -1 -> Math.round(this / 10.0) * 10.0
    0 -> Math.round(this).toDouble()
    1 -> Math.round(this * 10.0) / 10.0
    2 -> Math.round(this * 100.0) / 100.0
    else -> powTen(digits).toDouble().let { Math.round(this * it) / it }
}

/**
 * Rounds the value with [digits] after the comma.
 */
fun Float.round(digits: Int = 0) = when (digits) {
    -2 -> Math.round(this / 100.0f) * 100.0f
    -1 -> Math.round(this / 10.0f) * 10.0f
    0 -> Math.round(this).toFloat()
    1 -> Math.round(this * 10.0f) / 10.0f
    2 -> Math.round(this * 100.0f) / 100.0f
    else -> powTen(digits).toFloat().let { Math.round(this * it) / it }
}

inline fun <reified T> Any?.asSafe() =
        @Suppress("unchecked_cast")
        this as T

/**
 * Generates a list of values from the receiver that is doubled until max is reached.
 */
infix fun Int.doublesTo(max: Int) = generateSequence(this) {
    if (it * 2 >= max) it * 2 else null
}.toList()

/**
 * Changes the value in a map if present.
 */
inline fun <K, V> Map<K, V>.change(key: K, block: (V) -> V): Map<K, V> {
    if (key !in this)
        return this

    val value = getValue(key)
    return this - key + (key to block(value))
}

fun <E> Iterable<E>.except(element: E) =
        filter { it != element }

fun <E> Sequence<E>.except(element: E) =
        filter { it != element }

fun <E> List<E>.except(element: E) =
        filter { it != element }

fun <K, V> Map<K, V>.except(key: K) =
        filterKeys { it != key }

infix fun <T> T.iff(condition: Boolean): T? = if (condition) this else null
inline infix fun <T> T.iff(condition: () -> Boolean): T? = if (condition()) this else null

infix fun Int.pmod(other: Int) =
        (other + (this % other)) % other

infix fun Long.pmod(other: Long) =
        (other + (this % other)) % other

infix fun Long.pmod(other: Int) =
        (other + (this % other)) % other