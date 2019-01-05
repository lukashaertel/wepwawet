package eu.metatools.kepler.simulator

import java.util.*

/**
 * Registered and simulated body.
 */
interface Registration : Body {
    /**
     * Returns the value track of the registration, if supported.
     */
    val track: SortedMap<Double, Status> get() = throw NotImplementedError("Track not supported.")

    /**
     * Notifies a change at the given time.
     */
    fun notifyChange(t: Double? = null)

    /**
     * Unregisters the object at the given time or the definitions time source if null given.
     */
    fun unregister(t: Double? = null)
}

/**
 * Gets the minimum track key or [Double.NaN].
 */
val Registration.trackMin: Double
    get() = track.firstKey() ?: Double.NaN

/**
 * Gets the maximum track key or [Double.NaN].
 */
val Registration.trackMax: Double
    get() = track.lastKey() ?: Double.NaN
