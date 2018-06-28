package eu.metatools.net

/**
 * Exception thrown on misconfiguration, where received ID cannot be mapped to a class.
 */
data class ClassesMismatching(
        val bindings: List<Binding<*>>,
        val id: Int) : IllegalStateException("Mismatching configuration, $id not in -1 to ${bindings.size}.")

/**
 * Exception thrown on misconfiguration, type to be serialized cannot be mapped to an ID.
 */
data class TypeNotRegistered(
        val bindings: List<Binding<*>>,
        val item: Any) : IllegalStateException("Illegal class, $item type not configured.")

/**
 * Exception thrown on misconfiguration, size class of message was exceeded by required size to encode.
 */
data class InsufficientCapacity(
        val available: Size,
        val required: Int) : IllegalStateException("Insufficient capacity, required $required, had ${available.range}.")
