package eu.metatools.kepler.tools

import java.util.*


/**
 * Skips a specific item in a list get based.
 */
fun <E> List<E>.skipItem(n: Int) = object : AbstractList<E>() {
    override fun get(index: Int): E =
            if (index >= n)
                this@skipItem[index + 1]
            else
                this@skipItem[index]

    override val size: Int
        get() =
            if (this@skipItem.size > n)
                this@skipItem.size - 1
            else
                this@skipItem.size

}

/**
 * Safe access [get] with wrap-around.
 */
fun <T> List<T>.getIn(i: Int) = get(((i % size) + size) % size)