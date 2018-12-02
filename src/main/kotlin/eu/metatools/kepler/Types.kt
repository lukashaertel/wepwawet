package eu.metatools.kepler

interface NumberLike<T> {
    operator fun plus(other: T): T

    operator fun minus(other: T): T

    operator fun times(scale: R): T

    operator fun div(scale: R): T
}

typealias R = Double

typealias R2 = Vec

typealias AtT<T> = (time: R) -> T

typealias AtP<T> = (pos: R2) -> T

typealias AtTAndP<T> = (time: R, pos: R2) -> T

fun <T, U> const(value: U): (T) -> U = { value }

fun <T, U, V> ((T) -> U).expand() = { time: T, _: V -> this(time) }
