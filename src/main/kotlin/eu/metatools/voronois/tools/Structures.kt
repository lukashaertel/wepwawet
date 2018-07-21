package eu.metatools.voronois.tools

import java.util.*

/**
 * A point.
 */
typealias XY = Pair<Int, Int>

/**
 * Returns the Euclidean length between two points.
 */
infix fun XY.eucLen(other: XY): Double {
    val a = (other.first - first).toDouble()
    val b = (other.second - second).toDouble()
    return Math.sqrt(a * a + b * b)
}

/**
 * RetruReturnsns the Manhattan length between two points.
 */
infix fun XY.manLen(other: XY): Int {
    val a = Math.abs(other.first - first)
    val b = Math.abs(other.second - second)
    return a + b
}

/**
 * Points left and right, above and below.
 */
fun XY.cross() = listOf(
        XY(first - 1, second),
        XY(first, second - 1),
        XY(first + 1, second),
        XY(first, second + 1)
)

/**
 * Points around.
 */
fun XY.around() = listOf(
        XY(first - 1, second - 1),
        XY(first, second - 1),
        XY(first + 1, second - 1),
        XY(first + 1, second),
        XY(first + 1, second + 1),
        XY(first, second + 1),
        XY(first - 1, second + 1),
        XY(first - 1, second))

/**
 * Points around including self.
 */
fun XY.nine() = listOf(
        XY(first, second),
        XY(first - 1, second - 1),
        XY(first, second - 1),
        XY(first + 1, second - 1),
        XY(first + 1, second),
        XY(first + 1, second + 1),
        XY(first, second + 1),
        XY(first - 1, second + 1),
        XY(first - 1, second))

/**
 * A size.
 */
typealias Size = Pair<Int, Int>


/**
 * A two dimensional data field, based on an array.
 */
data class Field<T>(val data: Array<T>, val width: Int) {
    companion object {
        /**
         * Creates a field from an initial value.
         */
        inline fun <reified T> create(size: Size, initial: T) =
                Field(Array(size.first * size.second) {
                    initial
                }, size.first)


        /**
         * Creates field from an initializer.
         */
        inline fun <reified T> create(size: Size, initialize: (XY) -> T) =
                Field(Array(size.first * size.second) {
                    initialize(XY(it % size.first, it / size.first))
                }, size.first)

    }

    /**
     * The height including incomplete rows.
     */
    val height
        get() =
            if (data.size % width == 0)
                data.size / width
            else
                (data.size / width) + 1

    /**
     * The length of the data.
     */
    val size
        get() =
            data.size

    /**
     * True if the index is contained.
     */
    operator fun contains(at: Int): Boolean {
        if (at < 0) return false
        if (at >= data.size) return false
        return true
    }

    /**
     * Gets the value at the index.
     */
    operator fun get(at: Int): T? {
        if (at < 0) return null
        if (at >= data.size) return null
        return data[at]
    }

    /**
     * Sets the value at the index.
     */
    operator fun set(at: Int, value: T): Boolean {
        if (at < 0) return false
        if (at >= data.size) return false
        data[at] = value
        return true
    }

    /**
     * True if the point is contained.
     */
    operator fun contains(xy: XY): Boolean {
        if (xy.first < 0) return false
        if (xy.first >= width) return false
        val index = xy.second * width + xy.first
        if (index < 0) return false
        if (index >= data.size) return false
        return true
    }

    /**
     * Gets the value at the point.
     */
    operator fun get(xy: XY): T? {
        if (xy.first < 0) return null
        if (xy.first >= width) return null
        val index = xy.second * width + xy.first
        if (index < 0) return null
        if (index >= data.size) return null
        return data[index]
    }

    /**
     * Sets the value at the point.
     */
    operator fun set(xy: XY, value: T): Boolean {
        if (xy.first < 0) return false
        if (xy.first >= width) return false
        val index = xy.second * width + xy.first
        if (index < 0) return false
        if (index >= data.size) return false
        data[index] = value
        return true
    }

    /**
     * Prints the field to standard out.
     * @param busyStrings If true, strings are not stored ahead of time.
     */
    fun print(busyStrings: Boolean = true) {
        if (busyStrings)
            printBusyStrings()
        else
            printLazyStrings()
    }

    /**
     * Calculates padding ahead, then prints using that padding. [toString] called multiple times, less storage.
     */
    private fun printBusyStrings() {
        val length = data.fold(0) { w, d ->
            maxOf(w, d.toString().length)
        }

        for ((i, d) in data.withIndex()) {
            print(d.toString().padStart(length))
            if (i % width == width - 1)
                println()
            else
                print(", ")
        }
    }

    /**
     * Calculates strings ahead, then padding on strings, then prints using that padding. [toString] called once, more
     * storage.
     */
    private fun printLazyStrings() {
        val strings = data.map { it.toString() }
        val length = strings.fold(0) { w, s ->
            maxOf(w, s.length)
        }

        for ((i, s) in strings.withIndex()) {
            print(s.padStart(length))
            if (i % width == width - 1)
                println()
            else
                print(", ")
        }
    }

    /**
     * Returns a list of lists, where each list is a row.
     */
    fun rows() = (0 until height).map {
        val start = it * width
        val end = minOf(size, (it + 1) * width)
        data.toList().subList(start, end)
    }

    /**
     * Returns all points mapped to their value.
     */
    fun points() = data.mapIndexed { i, d ->
        XY(i % width, i / width) to d
    }

    /**
     * Returns all points associated by their position.
     */
    fun pointsMap() =
            points().associate { it }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Field<*>

        if (!Arrays.equals(data, other.data)) return false
        if (width != other.width) return false

        return true
    }

    override fun hashCode(): Int {
        var result = Arrays.hashCode(data)
        result = 31 * result + width
        return result
    }

    override fun toString(): String {
        return rows().toString()
    }
}
