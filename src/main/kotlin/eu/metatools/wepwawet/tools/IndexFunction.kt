package eu.metatools.wepwawet.tools

/**
 * A function that also supports indexing.
 */
abstract class IndexFunction0<in I, out R> {
    /**
     * Executes the function without an index.
     */
    protected abstract fun exec(): R

    /**
     * Executes the function with an index.
     */
    protected abstract fun exec(index: I): R

    /**
     * Invokes the function without an index.
     */
    operator fun invoke(): R {
        return exec()
    }

    /**
     * Binds an index for later use.
     */
    operator fun get(index: I): () -> R {
        return { -> exec(index) }
    }
}


/**
 * Creates an [IndexFunction0].
 */
@JvmName("indexFunction0")
inline fun <I, R> indexFunction(
        crossinline execNoIndex: IndexFunction0<I, R>.() -> R,
        crossinline execIndex: IndexFunction0<I, R>.(I) -> R) =
        object : IndexFunction0<I, R>() {
            override fun exec(): R = execNoIndex()

            override fun exec(index: I) = execIndex(index)
        }

/**
 * A function that also supports indexing.
 */
abstract class IndexFunction1<in A, in I, out R> {
    /**
     * Executes the function without an index.
     */
    protected abstract fun exec(arg: A): R

    /**
     * Executes the function with an index.
     */
    protected abstract fun exec(arg: A, index: I): R

    /**
     * Invokes the function without an index.
     */
    operator fun invoke(arg: A): R {
        return exec(arg)
    }

    /**
     * Binds an index for later use.
     */
    operator fun get(index: I): (A) -> R {
        return { arg: A -> exec(arg, index) }
    }
}

/**
 * Creates an [IndexFunction1].
 */
@JvmName("indexFunction1")
inline fun <A, I, R> indexFunction(
        crossinline execNoIndex: IndexFunction1<A, I, R>.(A) -> R,
        crossinline execIndex: IndexFunction1<A, I, R>.(A, I) -> R) =
        object : IndexFunction1<A, I, R>() {
            override fun exec(arg: A) = execNoIndex(arg)

            override fun exec(arg: A, index: I) = execIndex(arg, index)
        }

/**
 * A function that also supports indexing.
 */
abstract class IndexFunction2<in A1, in A2, in I, out R> {
    /**
     * Executes the function without an index.
     */
    protected abstract fun exec(arg1: A1, arg2: A2): R

    /**
     * Executes the function with an index.
     */
    protected abstract fun exec(arg1: A1, arg2: A2, index: I): R

    /**
     * Invokes the function without an index.
     */
    operator fun invoke(arg1: A1, arg2: A2): R {
        return exec(arg1, arg2)
    }

    /**
     * Binds an index for later use.
     */
    operator fun get(index: I): (A1, A2) -> R {
        return { arg1: A1, arg2: A2 -> exec(arg1, arg2, index) }
    }
}

/**
 * Creates an [IndexFunction2].
 */
@JvmName("indexFunction2")
inline fun <A1, A2, I, R> indexFunction(
        crossinline execNoIndex: IndexFunction2<A1, A2, I, R>.(A1, A2) -> R,
        crossinline execIndex: IndexFunction2<A1, A2, I, R>.(A1, A2, I) -> R) =
        object : IndexFunction2<A1, A2, I, R>() {
            override fun exec(arg1: A1, arg2: A2) = execNoIndex(arg1, arg2)

            override fun exec(arg1: A1, arg2: A2, index: I) = execIndex(arg1, arg2, index)
        }


/**
 * A function that also supports indexing.
 */
abstract class IndexFunction3<in A1, in A2, in A3, in I, out R> {
    /**
     * Executes the function without an index.
     */
    protected abstract fun exec(arg1: A1, arg2: A2, arg3: A3): R

    /**
     * Executes the function with an index.
     */
    protected abstract fun exec(arg1: A1, arg2: A2, arg3: A3, index: I): R

    /**
     * Invokes the function without an index.
     */
    operator fun invoke(arg1: A1, arg2: A2, arg3: A3): R {
        return exec(arg1, arg2, arg3)
    }

    /**
     * Binds an index for later use.
     */
    operator fun get(index: I): (A1, A2, A3) -> R {
        return { arg1: A1, arg2: A2, arg3: A3 -> exec(arg1, arg2, arg3, index) }
    }
}

/**
 * Creates an [IndexFunction3].
 */
@JvmName("indexFunction3")
inline fun <A1, A2, A3, I, R> indexFunction(
        crossinline execNoIndex: IndexFunction3<A1, A2, A3, I, R>.(A1, A2, A3) -> R,
        crossinline execIndex: IndexFunction3<A1, A2, A3, I, R>.(A1, A2, A3, I) -> R) =
        object : IndexFunction3<A1, A2, A3, I, R>() {
            override fun exec(arg1: A1, arg2: A2, arg3: A3) = execNoIndex(arg1, arg2, arg3)

            override fun exec(arg1: A1, arg2: A2, arg3: A3, index: I) = execIndex(arg1, arg2, arg3, index)
        }


/**
 * A function that also supports indexing.
 */
abstract class IndexFunction4<in A1, in A2, in A3, in A4, in I, out R> {
    /**
     * Executes the function without an index.
     */
    protected abstract fun exec(arg1: A1, arg2: A2, arg3: A3, arg4: A4): R

    /**
     * Executes the function with an index.
     */
    protected abstract fun exec(arg1: A1, arg2: A2, arg3: A3, arg4: A4, index: I): R

    /**
     * Invokes the function without an index.
     */
    operator fun invoke(arg1: A1, arg2: A2, arg3: A3, arg4: A4): R {
        return exec(arg1, arg2, arg3, arg4)
    }

    /**
     * Binds an index for later use.
     */
    operator fun get(index: I): (A1, A2, A3, A4) -> R {
        return { arg1: A1, arg2: A2, arg3: A3, arg4: A4 -> exec(arg1, arg2, arg3, arg4, index) }
    }
}

/**
 * Creates an [IndexFunction4].
 */
@JvmName("indexFunction4")
inline fun <A1, A2, A3, A4, I, R> indexFunction(
        crossinline execNoIndex: IndexFunction4<A1, A2, A3, A4, I, R>.(A1, A2, A3, A4) -> R,
        crossinline execIndex: IndexFunction4<A1, A2, A3, A4, I, R>.(A1, A2, A3, A4, I) -> R) =
        object : IndexFunction4<A1, A2, A3, A4, I, R>() {
            override fun exec(arg1: A1, arg2: A2, arg3: A3, arg4: A4) = execNoIndex(arg1, arg2, arg3, arg4)

            override fun exec(arg1: A1, arg2: A2, arg3: A3, arg4: A4, index: I) = execIndex(arg1, arg2, arg3, arg4, index)
        }

operator fun <I, R> IndexFunction0<I, R>?.invoke() =
        this?.invoke()

operator fun <A, I, R> IndexFunction1<A, I, R>?.invoke(arg: A) =
        this?.invoke(arg)

operator fun <A1, A2, I, R> IndexFunction2<A1, A2, I, R>?.invoke(arg1: A1, arg2: A2) =
        this?.invoke(arg1, arg2)

operator fun <A1, A2, A3, I, R> IndexFunction3<A1, A2, A3, I, R>?.invoke(arg1: A1, arg2: A2, arg3: A3) =
        this?.invoke(arg1, arg2, arg3)

operator fun <A1, A2, A3, A4, I, R> IndexFunction4<A1, A2, A3, A4, I, R>?.invoke(arg1: A1, arg2: A2, arg3: A3, arg4: A4) =
        this?.invoke(arg1, arg2, arg3, arg4)

operator fun <I, R> IndexFunction0<I, R>?.get(index: I): () -> R? =
        if (this == null)
            { -> null }
        else
            { -> this[index]() }

operator fun <A, I, R> IndexFunction1<A, I, R>?.get(index: I): (A) -> R? =
        if (this == null)
            { _ -> null }
        else
            { arg -> this[index](arg) }

operator fun <A1, A2, I, R> IndexFunction2<A1, A2, I, R>?.get(index: I): (A1, A2) -> R? =
        if (this == null)
            { _, _ -> null }
        else
            { arg1, arg2 -> this[index](arg1, arg2) }

operator fun <A1, A2, A3, I, R> IndexFunction3<A1, A2, A3, I, R>?.get(index: I): (A1, A2, A3) -> R? =
        if (this == null)
            { _, _, _ -> null }
        else
            { arg1, arg2, arg3 -> this[index](arg1, arg2, arg3) }

operator fun <A1, A2, A3, A4, I, R> IndexFunction4<A1, A2, A3, A4, I, R>?.get(index: I): (A1, A2, A3, A4) -> R? =
        if (this == null)
            { _, _, _, _ -> null }
        else
            { arg1, arg2, arg3, arg4 -> this[index](arg1, arg2, arg3, arg4) }