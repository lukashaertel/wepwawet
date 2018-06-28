package eu.metatools.net

/**
 * Size of a numeric unit up to integers.
 * @property bytes Number of bytes used by the size.
 * @property range Range covered.
 */
enum class Size(val bytes: Int, val range: IntRange) {
    /**
     * Numeric unit of byte size one.
     */
    BYTE(1, Byte.MIN_VALUE..Byte.MAX_VALUE),

    /**
     * Numeric unit of byte size two.
     */
    SHORT(2, Short.MIN_VALUE..Short.MAX_VALUE),
    /**
     * Numeric unit of byte size four.
     */
    INT(4, Int.MIN_VALUE..Int.MAX_VALUE);

    companion object {
        /**
         * Returns the size capturing the given element count.
         */
        fun of(amount: Byte) = when (amount) {
            in BYTE.range -> BYTE
            else -> throw IllegalStateException("Illegal state, byte input exceeds byte range.")
        }

        /**
         * Returns the size capturing the given element count.
         */
        fun of(amount: Short) = when (amount) {
            in BYTE.range -> BYTE
            in SHORT.range -> SHORT
            else -> throw IllegalStateException("Illegal state, short input exceeds short range.")
        }

        /**
         * Returns the size capturing the given element count.
         */
        fun of(amount: Int) = when (amount) {
            in BYTE.range -> BYTE
            in SHORT.range -> SHORT
            in INT.range -> INT
            else -> throw IllegalStateException("Illegal state, int input exceeds int range.")
        }
    }
}