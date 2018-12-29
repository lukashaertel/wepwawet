package eu.metatools.kepler.dgl

import eu.metatools.kepler.Gravity
import eu.metatools.kepler.Vec
import eu.metatools.kepler.addMulti
import eu.metatools.kepler.plot
import org.apache.commons.math3.ode.AbstractIntegrator
import org.apache.commons.math3.ode.nonstiff.DormandPrince853Integrator

interface Simulation<T> {
    fun integrate(integrator: AbstractIntegrator, from: T, t: Double): T
}


fun main(args: Array<String>) {
    val nbs = NBodySimulation(3)
    nbs.effects.addAcc {
        it.second.fold(Vec.zero) { l, r ->
            l + Gravity.acc(r.pos, 1e+16, pos)
        }
    }


    val int = DormandPrince853Integrator(1.0e-8, 100.0, 1.0e-10, 1.0e-10)
    val initial = listOf(
            Context(0.0, Vec(100.0, 0.0), 0.0, Vec.left * 20.0, 0.0),
            Context(0.0, Vec(100.0, 100.0), 0.0, Vec.right * 20.0, 0.0),
            Context(0.0, Vec(50.0, 50.0), 0.0, Vec.down * 20.0, 0.0))
    val cbi = ContinuousIntegrator(nbs, int, initial)

    val f = { t: Double -> cbi.integrate(t) }
    val g = { t: Double ->
        val (a, b, c) = f(t)
        doubleArrayOf(
                a.pos.x,
                a.pos.y,
                b.pos.x,
                b.pos.y,
                c.pos.x,
                c.pos.y)
    }


    plot {
        range(0.0, 10.0)
        addMulti(listOf("Xa", "Ya", "Xb", "Yb", "Xc", "Yc"), g)
    }
}