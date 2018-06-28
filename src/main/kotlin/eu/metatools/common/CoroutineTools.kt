package eu.metatools.common

import kotlinx.coroutines.experimental.*
import kotlinx.coroutines.experimental.channels.Channel
import kotlinx.coroutines.experimental.channels.ReceiveChannel
import kotlin.coroutines.experimental.CoroutineContext


/**
 * Launches in the [context] a consumer feeding into [block].
 */
@Deprecated("Old coroutine method")
fun <T> ReceiveChannel<T>.launchConsumer(context: CoroutineContext, block: suspend (T) -> Unit) =
        launch(context) {
            while (isActive)
                block(receive())
        }

/**
 * [launchConsumer] with [CommonPool].
 */
@Deprecated("Old coroutine method")
fun <T> ReceiveChannel<T>.launchConsumer(block: suspend (T) -> Unit) =
        launchConsumer(CommonPool, block)


/**
 * Consumes from a channel of [T] all elements of type [U], returns a second channel where only non-[U] elements are
 * left over. Consumption is done in a [Job].
 * @param T The type of the input elements
 * @param U The type of elements to consume in the block
 * @receiver The channel to consume from
 * @param context The context to run the consumer job in
 * @param block The block to run for the elements of type [U]
 * @return Returns a [Job] paired with a [ReceiveChannel] providing all remaining elements.
 */
@Deprecated("Old coroutine method")
inline fun <T, reified U : T> ReceiveChannel<T>.choose(
        context: CoroutineContext, noinline block: suspend (U) -> Unit) =
        Channel<T>().let { otherwise ->
            launchConsumer(context) {
                if (it is U)
                    block(it)
                else
                    otherwise.send(it)
            } to otherwise
        }

/**
 * Consumes from a channel of [T] all elements of type [U], returns a second channel where only non-[U] elements are
 * left over. Consumption is done in a [Job].
 * @param T The type of the input elements
 * @param U The type of elements to consume in the block
 * @receiver The channel to consume from
 * @param block The block to run for the elements of type [U]
 * @return Returns a [Job] paired with a [ReceiveChannel] providing all remaining elements.
 */
@Deprecated("Old coroutine method")
inline infix fun <T, reified U : T> ReceiveChannel<T>.choose(noinline block: suspend (U) -> Unit) =
        choose(CommonPool, block)

/**
 * Consumes all elements from a channel of [T], only for those that are of type [U], executes the block. If non-[U]
 * elements are of interest, [choose] should be used.
 */
@Deprecated("Old coroutine method")
inline fun <T, reified U : T> ReceiveChannel<T>.pick(
        context: CoroutineContext, noinline block: suspend (U) -> Unit) =
        launchConsumer(context) {
            if (it is U)
                block(it)
        }

/**
 * Consumes all elements from a channel of [T], only for those that are of type [U], executes the block.
 */
@Deprecated("Old coroutine method")
inline infix fun <T, reified U : T> ReceiveChannel<T>.pick(noinline block: suspend (U) -> Unit) =
        pick(CommonPool, block)

/**
 * From a channel reads all items discarding them.
 */
@Deprecated("Old coroutine method")
fun ReceiveChannel<*>.discardRemaining(context: CoroutineContext) =
        launchConsumer(context) { }

/**
 * From a channel reads all items discarding them.
 */
@Deprecated("Old coroutine method")
fun ReceiveChannel<*>.discardRemaining() =
        launchConsumer { }


@Deprecated("Old coroutine method")
inline fun <T, reified U : T> Pair<Job, ReceiveChannel<T>>.choose(
        context: CoroutineContext, noinline block: suspend (U) -> Unit) =
        second.choose(context, block)

@Deprecated("Old coroutine method")
inline infix fun <T, reified U : T> Pair<Job, ReceiveChannel<T>>.choose(noinline block: suspend (U) -> Unit) =
        second.choose(CommonPool, block)

@Deprecated("Old coroutine method")
inline fun <T, reified U : T> Pair<Job, ReceiveChannel<T>>.pick(
        context: CoroutineContext, noinline block: suspend (U) -> Unit) =
        second.pick(context, block)

@Deprecated("Old coroutine method")
inline infix fun <T, reified U : T> Pair<Job, ReceiveChannel<T>>.pick(noinline block: suspend (U) -> Unit) =
        second.pick(block)

/**
 * Reduces the deferred parts using a reducer.
 */
fun <T> asyncReduce(first: Deferred<T>, second: Deferred<T>, reducer: suspend (T, T) -> T) =
        async {
            // Apply reducer on the results of both deferred values.
            reducer(first.await(), second.await())
        }

/**
 * Reduce list of deferred parts using a reducer.
 */
fun <T> asyncReduce(list: List<Deferred<T>>, reducer: suspend (T, T) -> T): Deferred<T> {
    // If list is empty, return null.
    if (list.isEmpty())
        error("Reducing empty list")

    // If one element, return it.
    if (list.size == 1)
        return list[0]

    // If two elements, return primitive async reduce.
    if (list.size == 2)
        return asyncReduce(list[0], list[1], reducer)

    if (list.size % 2 == 0) {
        // List can be decomposed pairwise, reduce all items pairwise.
        return asyncReduce(list.pairs().map { (a, b) -> asyncReduce(a, b, reducer) }, reducer)
    } else {
        // List has non-pair element, decomposed into pairwise part and extraneous element.
        val init = list.subList(0, list.size - 1)
        val last = list[list.size - 1]

        // Reduce pairwise part and combine with extraneous element.
        return asyncReduce(asyncReduce(init, reducer), last, reducer)
    }
}

suspend fun <T> Iterable<Deferred<T>>.awaitAll() {
    for (d in this)
        d.await()
}