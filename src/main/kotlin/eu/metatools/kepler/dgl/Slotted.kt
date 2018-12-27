package eu.metatools.kepler.dgl

import org.apache.commons.math3.util.FastMath

/**
 * Interpolates between the next closest slots.
 */
fun ContinuousIntegrator<Context>.slotted(t: Double, resolution: Double = 1.0 / 10.0): Context {
    // Get slot of lower and higher keys.
    val fe = FastMath.floor(t / resolution) * resolution
    val ce = FastMath.ceil(t / resolution) * resolution

    // Return single output if keys are equal.
    if (fe == ce)
        return integrate(fe)

    // Integrate for both (more likely to reuse).
    val a = integrate(fe)
    val b = integrate(ce)

    // Interpolate.
    return Context.lerp(t, a.t, a, b.t, b)
}

/**
 * Interpolates between the next closest slots.
 */
fun ContinuousIntegrator<List<Context>>.slotted(t: Double, resolution: Double = 1.0 / 10.0): List<Context> {
    // Get slot of lower and higher keys.
    val fe = FastMath.floor(t / resolution) * resolution
    val ce = FastMath.ceil(t / resolution) * resolution

    // Return single output if keys are equal.
    if (fe == ce)
        return integrate(fe)

    // Integrate for both (more likely to reuse).
    val a = integrate(fe)
    val b = integrate(ce)

    // Interpolate.
    return (a zip b).map { (u, v) ->
        Context.lerp(t, u.t, u, v.t, v)
    }
}