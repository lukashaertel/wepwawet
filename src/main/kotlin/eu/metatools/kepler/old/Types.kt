package eu.metatools.kepler.old

import eu.metatools.kepler.Vec

typealias AtT<T> = (time: Double) -> T

typealias AtP<T> = (pos: Vec) -> T

typealias AtTAndP<T> = (time: Double, pos: Vec) -> T

fun <T, U> const(value: U): (T) -> U = { value }
