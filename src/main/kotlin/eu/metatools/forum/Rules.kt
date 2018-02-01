package eu.metatools.forum

/**
 * A rule set that describes room state as a map from string to any value and changes as setting or removing those.
 */
abstract class MapRules<I> : Rules<I, Map<String, Any>, Pair<String, Any?>> {
    protected var description = mutableMapOf<String, Any>()

    override fun describe() =
            description
}

/**
 * A [MapRules] variant where some settings are limited to the host. [bounce] defaults to false.
 */
abstract class MasterRules<I>(val hostIdentity: I) : MapRules<I>() {
    override fun bounce(identity: I) =
            false

    abstract fun host(key: String): Boolean

    override fun enact(identity: I, mutation: Pair<String, Any?>): Boolean {
        // Decompose mutation
        val (k, v) = mutation

        // Check if a valid mutation
        if (host(k) && hostIdentity != identity)
            return false

        // Check if removal or set, return if description changed
        if (v == null)
            return description.remove(k) != null
        else
            return description.put(k, v) != v
    }
}

/**
 * A [MapRules] variant where some settings are limited to some identities. [bounce] defaults to false.
 */
abstract class DistributedRules<I>() : MapRules<I>() {
    override fun bounce(identity: I) =
            false

    abstract fun universal(key: String): Boolean

    abstract fun personal(identity: I, key: String): Boolean

    override fun enact(identity: I, mutation: Pair<String, Any?>): Boolean {
        // Decompose mutation
        val (k, v) = mutation

        // Check if a valid mutation
        if (!universal(k) && !personal(identity, k))
            return false

        // Check if removal or set, return if description changed
        if (v == null)
            return description.remove(k) != null
        else
            return description.put(k, v) != v
    }
}