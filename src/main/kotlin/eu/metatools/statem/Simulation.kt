package eu.metatools.statem


/**
 * A state machine simulation taking inputs of type [I].
 * @param S The status type.
 * @param I The input type.
 */
interface Simulation<S, in I> {
    /**
     * The current user status.
     */
    var status: S

    /**
     * The current state.
     */
    val at: String

    /**
     * Sends an input to the simulation.
     * @param input The input to send.
     */
    fun send(input: I)

    /**
     * Notifies a receiver that the last state is left and invalidates further transitions.
     */
    fun stop()
}

/**
 * Sends all the inputs.
 */
fun <S, I> Simulation<S, I>.send(inputs: Iterable<I>) {
    for (i in inputs)
        send(i)
}

/**
 * Sends all the inputs.
 */
fun <S, I> Simulation<S, I>.send(inputs: Sequence<I>) {
    for (i in inputs)
        send(i)
}

/**
 * Sends all the inputs.
 */
fun <S, I> Simulation<S, I>.send(vararg inputs: I) {
    for (i in inputs)
        send(i)
}