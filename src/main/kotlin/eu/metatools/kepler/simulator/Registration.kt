package eu.metatools.kepler.simulator

/**
 * Registered and simulated body.
 */
interface Registration : Body {
    /**
     * Notifies a change at the given time.
     */
    fun notifyChange(t: Double? = null)

    /**
     * Unregisters the object at the given time or the definitions time source if null given.
     */
    fun unregister(t: Double? = null)
}