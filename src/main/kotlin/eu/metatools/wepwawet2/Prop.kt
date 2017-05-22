package eu.metatools.wepwawet2

import kotlin.reflect.KProperty

/**
 * A property within an entity.
 */
data class Prop<in R : Entity, T>(val propId: PropId, val mutable: Boolean) {
    companion object {
        fun <R : Entity, T> doGet(r: R, propId: PropId): T {
            return r.node.getValue(propId)
        }

        fun <R : Entity, T> doSet(r: R, propId: PropId, t: T) {
            r.node.setValue(propId, t)
        }
    }

    operator fun getValue(r: R, p: KProperty<*>): T {
        // Get from raw entity
        return doGet(r, propId)
    }

    operator fun setValue(r: R, p: KProperty<*>, t: T) {
        // Strict error on immutable mutation
        if (!mutable) error("Trying to mutate an immutable field")

        // Set on raw entity
        doSet(r, propId, t)
    }
}