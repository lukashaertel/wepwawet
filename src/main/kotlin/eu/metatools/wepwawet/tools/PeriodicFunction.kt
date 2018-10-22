package eu.metatools.wepwawet.tools

interface PeriodicFunction {
    fun register(delay: Int, interval: Int)
    fun unregister()
}

/**
 * Creates an [IndexFunction1].
 */
inline fun periodicFunction(
        crossinline execRegister: PeriodicFunction.(Int, Int) -> Unit,
        crossinline execUnregister: PeriodicFunction.() -> Unit) =
        object : PeriodicFunction {
            override fun register(delay: Int, interval: Int) = execRegister(delay, interval)

            override fun unregister() = execUnregister()
        }
