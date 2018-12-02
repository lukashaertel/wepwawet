package eu.metatools.kepler


const val zero: R = 0.0

const val epsilon: R = 0.0001

val epsilonX = Vec(epsilon, zero)

val epsilonY = Vec(zero, epsilon)

val AtT<R>.ddt
    @JvmName("ddtR")
    get(): AtT<R> = { t ->
        (this(t) - this(t - epsilon)) / epsilon
    }

val AtP<R>.ddx
    @JvmName("ddxR")
    get(): AtP<R> = { t ->
        (this(t) - this(t - epsilonX)) / epsilon
    }

val AtP<R>.ddy
    @JvmName("ddyR")
    get(): AtP<R> = { t ->
        (this(t) - this(t - epsilonY)) / epsilon
    }

val <T : NumberLike<T>> AtT<T>.ddt
    get(): AtT<T> = { t ->
        (this(t) - this(t - epsilon)) / epsilon
    }

val <T : NumberLike<T>> AtP<T>.ddx
    get(): AtP<T> = { t ->
        (this(t) - this(t - epsilonX)) / epsilon
    }

val <T : NumberLike<T>> AtP<T>.ddy
    get(): AtP<T> = { t ->
        (this(t) - this(t - epsilonY)) / epsilon
    }