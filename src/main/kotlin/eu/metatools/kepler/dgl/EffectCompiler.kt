package eu.metatools.kepler.dgl

import eu.metatools.kepler.Vec
import kotlin.properties.Delegates

class EffectCompiler<T, U> {
    /**
     * Compiled acceleration function, as a result of the superposition of all passed acceleration functions.
     */
    var compiledAcc: (T, U) -> Vec = { _, _ -> Vec.zero }
        private set

    /**
     * Compiled rotational acceleration function, as a result of the superposition of all passed rotational
     * acceleration functions.
     */
    var compiledAccRot: (T, U) -> Double = { _, _ -> 0.0 }
        private set

    /**
     * Individual accelerator functions.
     */
    var acc by Delegates.observable(listOf<T.(U) -> Vec>()) { _, _, t ->
        if (t.isEmpty())
            compiledAcc = { _, _ -> Vec.zero }
        else
            compiledAcc = { c, u -> t.map { it(c, u) }.reduce { a, b -> a + b } }
    }

    /**
     * Add accelerator functor.
     */
    fun addAcc(f: T.(U) -> Vec) {
        acc += f
    }

    /**
     * Individual rotational accelerator functions.
     */
    var accRot by Delegates.observable(listOf<T.(U) -> Double>()) { _, _, t ->
        if (t.isEmpty())
            compiledAccRot = { _, _ -> 0.0 }
        else
            compiledAccRot = { c, u -> t.map { it(c, u) }.sum() }
    }

    /**
     * Add rotational accelerator functor.
     */
    fun addAccRot(f: T.(U) -> Double) {
        accRot += f
    }
}