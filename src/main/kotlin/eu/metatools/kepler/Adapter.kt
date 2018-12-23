package eu.metatools.kepler

/**
 * Default order of derivation.
 */
const val order = 2

/*
 * One parameter.
 */

/**
 * Implicit conversion invoke.
 */
operator fun <T> ((DS) -> T).invoke(
        p0: Double) =
        invoke(DS(1, order, 0, p0))

/**
 * Implicit conversion invoke.
 */
operator fun <T> ((DSVec) -> T).invoke(
        p0: Vec) =
        invoke(
                DSVec(
                        DS(2, order, 0, p0.x),
                        DS(2, order, 1, p0.y)))

/*
 * Two parameters.
 */

/**
 * Implicit conversion invoke.
 */
operator fun <T> ((DS, DS) -> T).invoke(
        p0: Double,
        p1: Double) =
        invoke(
                DS(2, order, 0, p0),
                DS(2, order, 1, p1))

/**
 * Implicit conversion invoke.
 */
operator fun <T> ((DSVec, DS) -> T).invoke(
        p0: Vec,
        p1: Double) =
        invoke(
                DSVec(
                        DS(3, order, 0, p0.x),
                        DS(3, order, 1, p0.y)),
                DS(3, order, 2, p1))

/**
 * Implicit conversion invoke.
 */
operator fun <T> ((DS, DSVec) -> T).invoke(
        p0: Double,
        p1: Vec) =
        invoke(
                DS(3, order, 0, p0),
                DSVec(
                        DS(3, order, 1, p1.x),
                        DS(3, order, 2, p1.y)))

/**
 * Implicit conversion invoke.
 */
operator fun <T> ((DSVec, DSVec) -> T).invoke(
        p0: Vec,
        p1: Vec) =
        invoke(
                DSVec(
                        DS(4, order, 0, p0.x),
                        DS(4, order, 1, p0.y)),
                DSVec(
                        DS(4, order, 2, p1.x),
                        DS(4, order, 3, p1.y)))

/*
 * Three parameters.
 */

/**
 * Implicit conversion invoke.
 */
operator fun <T> ((DS, DS, DS) -> T).invoke(
        p0: Double,
        p1: Double,
        p2: Double) =
        invoke(
                DS(3, order, 0, p0),
                DS(3, order, 1, p1),
                DS(3, order, 2, p2))

/**
 * Implicit conversion invoke.
 */
operator fun <T> ((DSVec, DS, DS) -> T).invoke(
        p0: Vec,
        p1: Double,
        p2: Double) =
        invoke(
                DSVec(
                        DS(4, order, 0, p0.x),
                        DS(4, order, 1, p0.y)),
                DS(4, order, 2, p1),
                DS(4, order, 3, p2))

/**
 * Implicit conversion invoke.
 */
operator fun <T> ((DS, DSVec, DS) -> T).invoke(
        p0: Double,
        p1: Vec,
        p2: Double) =
        invoke(
                DS(4, order, 0, p0),
                DSVec(
                        DS(4, order, 1, p1.x),
                        DS(4, order, 2, p1.y)),
                DS(4, order, 3, p2))


operator fun <T> ((DSVec, DSVec, DS) -> T).invoke(
        p0: Vec,
        p1: Vec,
        p2: Double) =
        invoke(
                DSVec(
                        DS(5, order, 0, p0.x),
                        DS(5, order, 1, p0.y)),
                DSVec(
                        DS(5, order, 2, p1.x),
                        DS(5, order, 3, p1.y)),
                DS(5, order, 4, p2))

/**
 * Implicit conversion invoke.
 */
operator fun <T> ((DS, DS, DSVec) -> T).invoke(
        p0: Double,
        p1: Double,
        p2: Vec) =
        invoke(
                DS(4, order, 0, p0),
                DS(4, order, 1, p1),
                DSVec(
                        DS(4, order, 2, p2.x),
                        DS(4, order, 3, p2.y)))

/**
 * Implicit conversion invoke.
 */
operator fun <T> ((DSVec, DS, DSVec) -> T).invoke(
        p0: Vec,
        p1: Double,
        p2: Vec) =
        invoke(
                DSVec(
                        DS(5, order, 0, p0.x),
                        DS(5, order, 1, p0.y)),
                DS(5, order, 2, p1),
                DSVec(
                        DS(5, order, 3, p2.x),
                        DS(5, order, 4, p2.y)))

/**
 * Implicit conversion invoke.
 */
operator fun <T> ((DS, DSVec, DSVec) -> T).invoke(
        p0: Double,
        p1: Vec,
        p2: Vec) =
        invoke(
                DS(5, order, 0, p0),
                DSVec(
                        DS(5, order, 1, p1.x),
                        DS(5, order, 2, p1.y)),
                DSVec(
                        DS(5, order, 3, p2.x),
                        DS(5, order, 4, p2.y)))

/**
 * Implicit conversion invoke.
 */
operator fun <T> ((DSVec, DSVec, DSVec) -> T).invoke(
        p0: Vec,
        p1: Vec,
        p2: Vec) =
        invoke(
                DSVec(
                        DS(6, order, 0, p0.x),
                        DS(6, order, 1, p0.y)),
                DSVec(
                        DS(6, order, 2, p1.x),
                        DS(6, order, 3, p1.y)),
                DSVec(
                        DS(6, order, 4, p2.x),
                        DS(6, order, 5, p2.y)))

/*
 * Four parameters.
 */

/**
 * Implicit conversion invoke.
 */
operator fun <T> ((DS, DS, DS, DS) -> T).invoke(
        p0: Double,
        p1: Double,
        p2: Double,
        p3: Double) =
        invoke(
                DS(4, order, 0, p0),
                DS(4, order, 1, p1),
                DS(4, order, 2, p2),
                DS(4, order, 3, p3))

/**
 * Implicit conversion invoke.
 */
operator fun <T> ((DSVec, DS, DS, DS) -> T).invoke(
        p0: Vec,
        p1: Double,
        p2: Double,
        p3: Double) =
        invoke(
                DSVec(
                        DS(5, order, 0, p0.x),
                        DS(5, order, 1, p0.y)),
                DS(5, order, 2, p1),
                DS(5, order, 3, p2),
                DS(5, order, 4, p3))

/**
 * Implicit conversion invoke.
 */
operator fun <T> ((DS, DSVec, DS, DS) -> T).invoke(
        p0: Double,
        p1: Vec,
        p2: Double,
        p3: Double) =
        invoke(
                DS(5, order, 0, p0),
                DSVec(
                        DS(5, order, 1, p1.x),
                        DS(5, order, 2, p1.y)),
                DS(5, order, 3, p2),
                DS(5, order, 4, p3))

/**
 * Implicit conversion invoke.
 */
operator fun <T> ((DSVec, DSVec, DS, DS) -> T).invoke(
        p0: Vec,
        p1: Vec,
        p2: Double,
        p3: Double) =
        invoke(
                DSVec(
                        DS(6, order, 0, p0.x),
                        DS(6, order, 1, p0.y)),
                DSVec(
                        DS(6, order, 2, p1.x),
                        DS(6, order, 3, p1.y)),
                DS(6, order, 4, p2),
                DS(6, order, 5, p3))

/**
 * Implicit conversion invoke.
 */
operator fun <T> ((DS, DS, DSVec, DS) -> T).invoke(
        p0: Double,
        p1: Double,
        p2: Vec,
        p3: Double) =
        invoke(
                DS(5, order, 0, p0),
                DS(5, order, 1, p1),
                DSVec(
                        DS(5, order, 2, p2.x),
                        DS(5, order, 3, p2.y)),
                DS(5, order, 4, p3))

/**
 * Implicit conversion invoke.
 */
operator fun <T> ((DSVec, DS, DSVec, DS) -> T).invoke(
        p0: Vec,
        p1: Double,
        p2: Vec,
        p3: Double) =
        invoke(
                DSVec(
                        DS(6, order, 0, p0.x),
                        DS(6, order, 1, p0.y)),
                DS(6, order, 2, p1),
                DSVec(
                        DS(6, order, 3, p2.x),
                        DS(6, order, 4, p2.y)),
                DS(6, order, 5, p3))

/**
 * Implicit conversion invoke.
 */
operator fun <T> ((DS, DSVec, DSVec, DS) -> T).invoke(
        p0: Double,
        p1: Vec,
        p2: Vec,
        p3: Double) =
        invoke(
                DS(6, order, 0, p0),
                DSVec(
                        DS(6, order, 1, p1.x),
                        DS(6, order, 2, p1.y)),
                DSVec(
                        DS(6, order, 3, p2.x),
                        DS(6, order, 4, p2.y)),
                DS(6, order, 5, p3))

/**
 * Implicit conversion invoke.
 */
operator fun <T> ((DSVec, DSVec, DSVec, DS) -> T).invoke(
        p0: Vec,
        p1: Vec,
        p2: Vec,
        p3: Double) =
        invoke(
                DSVec(
                        DS(7, order, 0, p0.x),
                        DS(7, order, 1, p0.y)),
                DSVec(
                        DS(7, order, 2, p1.x),
                        DS(7, order, 3, p1.y)),
                DSVec(
                        DS(7, order, 4, p2.x),
                        DS(7, order, 5, p2.y)),
                DS(7, order, 6, p3))

/**
 * Implicit conversion invoke.
 */
operator fun <T> ((DS, DS, DS, DSVec) -> T).invoke(
        p0: Double,
        p1: Double,
        p2: Double,
        p3: Vec) =
        invoke(
                DS(5, order, 0, p0),
                DS(5, order, 1, p1),
                DS(5, order, 2, p2),
                DSVec(
                        DS(5, order, 3, p3.x),
                        DS(5, order, 4, p3.y)))

/**
 * Implicit conversion invoke.
 */
operator fun <T> ((DSVec, DS, DS, DSVec) -> T).invoke(
        p0: Vec,
        p1: Double,
        p2: Double,
        p3: Vec) =
        invoke(
                DSVec(
                        DS(6, order, 0, p0.x),
                        DS(6, order, 1, p0.y)),
                DS(6, order, 2, p1),
                DS(6, order, 3, p2),
                DSVec(
                        DS(6, order, 4, p3.x),
                        DS(6, order, 5, p3.y)))

/**
 * Implicit conversion invoke.
 */
operator fun <T> ((DS, DSVec, DS, DSVec) -> T).invoke(
        p0: Double,
        p1: Vec,
        p2: Double,
        p3: Vec) =
        invoke(
                DS(6, order, 0, p0),
                DSVec(
                        DS(6, order, 1, p1.x),
                        DS(6, order, 2, p1.y)),
                DS(6, order, 3, p2),
                DSVec(
                        DS(6, order, 4, p3.x),
                        DS(6, order, 5, p3.y)))

/**
 * Implicit conversion invoke.
 */
operator fun <T> ((DSVec, DSVec, DS, DSVec) -> T).invoke(
        p0: Vec,
        p1: Vec,
        p2: Double,
        p3: Vec) =
        invoke(
                DSVec(
                        DS(7, order, 0, p0.x),
                        DS(7, order, 1, p0.y)),
                DSVec(
                        DS(7, order, 2, p1.x),
                        DS(7, order, 3, p1.y)),
                DS(7, order, 4, p2),
                DSVec(
                        DS(7, order, 5, p3.x),
                        DS(7, order, 6, p3.y)))

/**
 * Implicit conversion invoke.
 */
operator fun <T> ((DS, DS, DSVec, DSVec) -> T).invoke(
        p0: Double,
        p1: Double,
        p2: Vec,
        p3: Vec) =
        invoke(
                DS(6, order, 0, p0),
                DS(6, order, 1, p1),
                DSVec(
                        DS(6, order, 2, p2.x),
                        DS(6, order, 3, p2.y)),
                DSVec(
                        DS(6, order, 4, p3.x),
                        DS(6, order, 5, p3.y)))

/**
 * Implicit conversion invoke.
 */
operator fun <T> ((DSVec, DS, DSVec, DSVec) -> T).invoke(
        p0: Vec,
        p1: Double,
        p2: Vec,
        p3: Vec) =
        invoke(
                DSVec(
                        DS(7, order, 0, p0.x),
                        DS(7, order, 1, p0.y)),
                DS(7, order, 2, p1),
                DSVec(
                        DS(7, order, 3, p2.x),
                        DS(7, order, 4, p2.y)),
                DSVec(
                        DS(7, order, 5, p3.x),
                        DS(7, order, 6, p3.y)))

/**
 * Implicit conversion invoke.
 */
operator fun <T> ((DS, DSVec, DSVec, DSVec) -> T).invoke(
        p0: Double,
        p1: Vec,
        p2: Vec,
        p3: Vec) =
        invoke(
                DS(7, order, 0, p0),
                DSVec(
                        DS(7, order, 1, p1.x),
                        DS(7, order, 2, p1.y)),
                DSVec(
                        DS(7, order, 3, p2.x),
                        DS(7, order, 4, p2.y)),
                DSVec(
                        DS(7, order, 5, p3.x),
                        DS(7, order, 6, p3.y)))

/**
 * Implicit conversion invoke.
 */
operator fun <T> ((DSVec, DSVec, DSVec, DSVec) -> T).invoke(
        p0: Vec,
        p1: Vec,
        p2: Vec,
        p3: Vec) =
        invoke(
                DSVec(
                        DS(8, order, 0, p0.x),
                        DS(8, order, 1, p0.y)),
                DSVec(
                        DS(8, order, 2, p1.x),
                        DS(8, order, 3, p1.y)),
                DSVec(
                        DS(8, order, 4, p2.x),
                        DS(8, order, 5, p2.y)),
                DSVec(
                        DS(8, order, 6, p3.x),
                        DS(8, order, 7, p3.y)))
