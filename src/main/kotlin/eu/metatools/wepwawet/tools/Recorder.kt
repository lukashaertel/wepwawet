package eu.metatools.wepwawet.tools

import eu.metatools.serialization.LinearInterpolator
import eu.metatools.wepwawet.Container
import eu.metatools.wepwawet.Entity
import java.util.*

/**
 * Records values that are given by [record] at the [container] time. Drops all that are older than [length].
 */
class Recorder<T : Any>(val container: Container, val length: Int, val point: Boolean = false) {
    companion object {
        /**
         * Interpolator object, one per thread.
         */
        private val interpolator: ThreadLocal<LinearInterpolator> = ThreadLocal.withInitial { LinearInterpolator() }
    }

    /**
     * Internal store of values.
     */
    private val store = TreeMap<Long, T>()

    /**
     * Does the interpolation for two map entries and a time.
     */
    private fun interpolate(lower: Map.Entry<Long, T>, higher: Map.Entry<Long, T>, time: Long): T {
        val t = (time - lower.key).toDouble() / (higher.key - lower.key)
        return interpolator.get().interpolate(lower.value, t, higher.value)
    }

    /**
     * Records a value at the [container]s current time.
     */
    fun record(t: T) {
        store.put(container.time, t)
        store.headMap(container.time - length).clear()
    }

    /**
     * Extrapolate/interpolate at the given time.
     */
    fun exin(time: Long): T? {
        // TODO: Include higher order interpolation
        when (store.size) {
            0 -> return null
            1 -> return store.values.single()
            else -> {
                // Get next lower value
                val l = store.floorEntry(time)

                // If point interpolation, the floor entry is the lower value, which might be null.
                if (point)
                    return l?.value

                // Get next higher value
                val r = store.ceilingEntry(time)

                // Catch corner cases
                if (l == null)
                    return interpolate(r, store.higherEntry(r.key), time)
                else if (r == null)
                    return interpolate(store.lowerEntry(l.key), l, time)
                if (l.key == r.key)
                    return l.value

                // Interpolate inner
                return interpolate(l, r, time)
            }
        }
    }
}

/**
 * Creates a recorder for the [Container] of the receiving entity.
 */
fun <T : Any> Entity.recorder(length: Int, point: Boolean = false) =
        Recorder<T>(container, length, point)

/**
 * Handles records to a double from any number type.
 */
@JvmName("recordDoubleFrom")
fun Recorder<Double>.recordFrom(n: Number) = record(n.toDouble())

/**
 * Handles records to a float from any number type.
 */
@JvmName("recordFloatFrom")
fun Recorder<Float>.recordFrom(n: Number) = record(n.toFloat())