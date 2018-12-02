package eu.metatools.kepler

import java.util.*

fun linearInterp(x0: R, y0: R, x1: R, y1: R, x: R) =
        when (x0) {
            x1 -> y0
            else -> (y0 * (x1 - x) + y1 * (x - x0)) / (x1 - x0)
        }

fun quadraticInterp(x0: R, y0: R, x1: R, y1: R, x2: R, y2: R, x: R) =
        when {
            x0 == x1 -> linearInterp(x1, y1, x2, y2, x)
            x1 == x2 -> linearInterp(x0, y0, x1, y1, x)
            else -> y0 * (x - x1) * (x - x2) / ((x0 - x1) * (x0 - x2)) +
                    y1 * (x - x0) * (x - x2) / ((x1 - x0) * (x1 - x2)) +
                    y2 * (x - x0) * (x - x1) / ((x2 - x0) * (x2 - x1))
        }

fun linearInterp(x0: R, y0: R2, x1: R, y1: R2, x: R) =
        when (x0) {
            x1 -> y0
            else -> (y0 * (x1 - x) + y1 * (x - x0)) / (x1 - x0)
        }

fun quadraticInterp(x0: R, y0: R2, x1: R, y1: R2, x2: R, y2: R2, x: R) =
        when {
            x0 == x1 -> linearInterp(x1, y1, x2, y2, x)
            x1 == x2 -> linearInterp(x0, y0, x1, y1, x)
            else -> y0 * (x - x1) * (x - x2) / ((x0 - x1) * (x0 - x2)) +
                    y1 * (x - x0) * (x - x2) / ((x1 - x0) * (x1 - x2)) +
                    y2 * (x - x0) * (x - x1) / ((x2 - x0) * (x2 - x1))
        }


inline fun <K, V, R> NavigableMap<K, V>.liftTwo(k: K, process: (K, V, K, V, K) -> R): R {
    // Next higher and lower entry
    val nhe = ceilingEntry(k)
    val nle = floorEntry(k)

    return if (nhe == null && nle == null) {
        throw IllegalStateException("empty")
    } else if (nhe == null) {
        // Next next lower entry
        val nnle = lowerEntry(nle.key)

        // If only one, interpolate between single entry, otherwise between both.
        if (nnle == null)
            process(nle.key, nle.value, nle.key, nle.value, k)
        else
            process(nnle.key, nnle.value, nle.key, nle.value, k)
    } else if (nle == null) {
        // Next next higher entry
        val nnhe = higherEntry(nhe.key)

        // If only one, interpolate between single entry, otherwise between both.
        if (nnhe == null)
            process(nhe.key, nhe.value, nhe.key, nhe.value, k)
        else
            process(nhe.key, nhe.value, nnhe.key, nnhe.value, k)
    } else {
        // Both present
        process(nle.key, nle.value, nhe.key, nhe.value, k)
    }
}

inline fun <K, V, R> NavigableMap<K, V>.liftThree(k: K, process: (K, V, K, V, K, V, K) -> R): R {
    val nhe = ceilingEntry(k)
    val nle = floorEntry(k)

    return if (nhe == null && nle == null) {
        throw IllegalStateException("empty")
    } else if (nhe == null) {
        val nnle = lowerEntry(nle.key)

        if (nnle == null)
            process(nle.key, nle.value, nle.key, nle.value, nle.key, nle.value, k)
        else {
            val nnnle = lowerEntry(nnle.key)

            if (nnnle == null)
                process(nnle.key, nnle.value, nnle.key, nnle.value, nle.key, nle.value, k)
            else
                process(nnnle.key, nnnle.value, nnle.key, nnle.value, nle.key, nle.value, k)
        }
    } else if (nle == null) {
        val nnhe = higherEntry(nhe.key)

        if (nnhe == null)
            process(nhe.key, nhe.value, nhe.key, nhe.value, nhe.key, nhe.value, k)
        else {
            val nnnhe = higherEntry(nnhe.key)

            if (nnnhe == null)
                process(nhe.key, nhe.value, nnhe.key, nnhe.value, nnhe.key, nnhe.value, k)
            else
                process(nhe.key, nhe.value, nnhe.key, nnhe.value, nnnhe.key, nnnhe.value, k)
        }
    } else {

        val nnle = lowerEntry(nle.key)
        if (nnle == null) {
            val nnhe = higherEntry(nhe.key)

            if (nnhe == null)
                process(nle.key, nle.value, nle.key, nle.value, nhe.key, nhe.value, k)
            else
                process(nle.key, nle.value, nhe.key, nhe.value, nnhe.key, nnhe.value, k)
        } else
            process(nnle.key, nnle.value, nle.key, nle.value, nhe.key, nhe.value, k)
    }
}