package eu.metatools.kepler.old

import eu.metatools.kepler.R
import eu.metatools.kepler.R2

typealias AtT<T> = (time: R) -> T

typealias AtP<T> = (pos: R2) -> T

typealias AtTAndP<T> = (time: R, pos: R2) -> T

fun <T, U> const(value: U): (T) -> U = { value }
