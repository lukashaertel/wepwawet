package eu.metatools.common

/**
 * Returns all pairs in a list that is of even length.
 */
fun <E> List<E>.pairs() = (0 until size / 2).map {
    get(it * 2 + 0) to get(it * 2 + 1)
}

/**
 * Returns splits of the given length.
 */
fun <E> List<E>.splits(length: Int) =
        if (size % length == 0)
            (0 until (size / length)).map {
                subList(it * length, minOf(size, (it + 1) * length))
            }
        else
            (0 until (size / length) + 1).map {
                subList(it * length, minOf(size, (it + 1) * length))
            }