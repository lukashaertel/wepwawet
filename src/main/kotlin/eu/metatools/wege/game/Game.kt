package eu.metatools.wege.game

import eu.metatools.wege.tools.*

val baseMovement = 40

val baseCost = mapOf(
        "road" to 1,
        "ground" to 5,
        "flora" to 5,
        "water" to 10,
        "wood" to 10,
        "hill" to 10,
        "stone" to 30,
        "highland" to 100
)

data class Paint(
        val origin: Structure,
        val factors: Map<String, Float>,
        val range: Float,
        val used: Float) : Prop {
    override val cost get() = (used * 100).toInt()
}

interface IfStructure {
    fun test(structure: Structure): Boolean
}

interface ExtraRange {
    val amount: Float
}

interface PopSpeed {
    val units: Float
}

interface ScienceSpeed {
    val units: Float
}

interface ReduceCost {
    val factors: Map<String, Float>
}

sealed class Update {
    override fun toString() = this::class.simpleName ?: ""
}

object HorsebackRiding : Update(), ExtraRange, ReduceCost {
    override val amount: Float
        get() = 5f

    override val factors = mapOf("ground" to 0.7f, "flora" to 0.7f)
}

object Wheele : Update(), ExtraRange {
    override val amount: Float
        get() = 5f
}

object Sailing : Update(), ReduceCost, IfStructure {
    override fun test(structure: Structure) =
            structure is Town && structure.slots.any { it is Shipyard }

    override val factors = mapOf("water" to 0.5f)
}


data class Player(var updates: List<Update>)

sealed class Slot {
    override fun toString() = this::class.simpleName ?: ""
}

object Caravansary : Slot(), ExtraRange, PopSpeed {
    override val amount: Float
        get() = 5f
    override val units: Float
        get() = 1f / (5 * 60)
}

object Granary : Slot(), PopSpeed {
    override val units: Float
        get() = 1f / (3 * 60)
}

object Shipyard : Slot(), ReduceCost {
    override val factors = mapOf("water" to 0.25f)
}

object Mason : Slot()

object Library : Slot(), ScienceSpeed {
    override val units: Float
        get() = 1f / (2 * 60)
}

object Academy : Slot(), ScienceSpeed {
    override val units: Float
        get() = 1f / (30)
}

sealed class Structure(val player: Player)

class Town(player: Player, var pop: Int, var slots: List<Slot>) : Structure(player) {
    override fun toString() = "Town(Player=$player, pop=$pop, slots=$slots)"
}

class Lumbermill(player: Player) : Structure(player)


class Stoneworks(player: Player) : Structure(player)

class Shrine(player: Player) : Structure(player)


fun Town.painterFrom(origin: XY, typer: (XY) -> String?): Init<Paint> {
    val extraRange = slots.filterIsInstance<ExtraRange>() + player.updates.filterIsInstance<ExtraRange>()
    val reduceCost = slots.filterIsInstance<ReduceCost>() + player.updates.filterIsInstance<ReduceCost>()

    val activeExtraRange = extraRange.filter {
        when (it) {
            is IfStructure -> it.test(this)
            else -> true
        }
    }

    val activeReduceCost = reduceCost.filter {
        when (it) {
            is IfStructure -> it.test(this)
            else -> true
        }
    }

    val factors = activeReduceCost.fold(emptyMap<String, Float>()) { i, r ->
        r.factors.entries.fold(i) { j, (t, f) ->
            (j - t + (t to (j[t] ?: 1f) * f))
        }
    }
    val range = activeExtraRange.sumByDouble { it.amount.toDouble() }.toFloat()

    return Init(origin, Paint(this, factors, baseMovement + range, 0f), InfluencePaint(typer))
}

class InfluencePaint(val typer: (XY) -> String?) : Distributor<Paint> {
    override fun valid(from: Paint, at: XY): Boolean {
        return from.range > from.used
    }

    override fun next(from: Paint, at: XY, to: XY): Paint {
        val type = typer(to)
        val cost = baseCost[type] ?: 100
        val factor = from.factors[type] ?: 1.0f
        val distance = at manLen to
        return Paint(from.origin, from.factors, from.range, from.used + cost * factor * distance)
    }

    override val skipVisited: Boolean
        get() = false

}