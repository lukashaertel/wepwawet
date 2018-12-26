package eu.metatools.kepler


import com.panayotis.gnuplot.JavaPlot
import com.panayotis.gnuplot.plot.AbstractPlot
import com.panayotis.gnuplot.style.Smooth
import com.panayotis.gnuplot.terminal.GNUPlotTerminal
import java.io.InputStream
import java.util.concurrent.Semaphore
import kotlin.concurrent.thread
import kotlin.math.max
import kotlin.math.min

/**
 * Plotting DSL.
 */
interface PlotDSL {
    /**
     * Plot key.
     */
    fun plotKey(key: JavaPlot.Key)

    /**
     * Set the current range.
     */
    fun range(from: Double, to: Double)

    /**
     * Sets the title to use now.
     */
    fun title(string: String)

    /**
     * Sets the smoothing value to use now.
     */
    fun smooth(smooth: Smooth)

    /**
     * Plot a function.
     */
    fun add(function: (Double) -> Double)

    /**
     * Plot a vector function.
     */
    fun addMulti(labels: Map<Int, String>, function: (Double) -> DoubleArray)
}

fun PlotDSL.addMulti(labels: List<String>, function: (Double) -> DoubleArray) =
        addMulti(labels.withIndex().associate { (it.index + 1) to it.value }, function)

fun PlotDSL.addMulti(function: (Double) -> DoubleArray) =
        addMulti(emptyMap(), function)

/**
 * Extend of plot outside of the value domains.
 */
data class Extend(val left: Double, val top: Double, val right: Double, val bottom: Double) {
    companion object {
        val default = Extend(0.0, 0.3, 0.0, 0.1)
    }
}

/**
 * GNU plot terminal notifying before and after output.
 */
private class NotifyingTerminal(val on: GNUPlotTerminal,
                                val beforeOutput: () -> Unit,
                                val afterOutput: () -> Unit) : GNUPlotTerminal {
    override fun getOutputFile(): String? {
        return on.outputFile
    }

    override fun processOutput(stdout: InputStream?): String? {
        beforeOutput()
        val output = on.processOutput(stdout)
        afterOutput()
        return output
    }

    override fun getType(): String? {
        return on.type
    }

    override fun hashCode(): Int {
        return on.hashCode()
    }

    override fun equals(other: Any?): Boolean {
        return on == other
    }
}

/**
 * Shared plot-lock, only one plotter can be generating at the moment.
 */
private val plotLock = Semaphore(1)

/**
 * Start a plot configuration and plots after configuring. Non-blocking, run in non-daemon thread.
 */
fun plot(resolution: Int = 1024, extend: Extend = Extend.default, config: PlotDSL.() -> Unit) {
    // Create plot to configure.
    val plot = JavaPlot()
    plot.setKey(JavaPlot.Key.TOP_LEFT)

    // Store min and max for boundary adjustment.
    var currentMinX = Double.MAX_VALUE
    var currentMaxX = Double.MIN_VALUE
    var currentMinY = Double.MAX_VALUE
    var currentMaxY = Double.MIN_VALUE

    // Configure it.
    config(object : PlotDSL {
        /**
         * Current points, defaults to 0..1.
         */
        var currentPoints = (0 until resolution).map { it.toDouble() / resolution }

        /**
         * Current title.
         */
        var currentTitle: String = ""

        /**
         * Current smoothing.
         */
        var currentSmooth: Smooth = Smooth.UNIQUE


        override fun plotKey(key: JavaPlot.Key) {
            plot.setKey(key)
        }

        override fun range(from: Double, to: Double) {
            currentPoints = (0 until resolution).map { it.toDouble() / resolution }.map { from + (to - from) * it }
        }

        override fun title(string: String) {
            currentTitle = string
        }

        override fun smooth(smooth: Smooth) {
            currentSmooth = smooth
        }

        override fun add(function: (Double) -> Double) {
            // Materialize all values, memorize min and max for axis.
            val materialized = currentPoints.map { x ->
                currentMinX = min(currentMinX, x)
                currentMaxX = max(currentMaxX, x)

                val y = function(x)

                currentMinY = min(currentMinY, y)
                currentMaxY = max(currentMaxY, y)

                doubleArrayOf(x, y)
            }

            // Add plot of function.
            plot.addPlot(materialized.toTypedArray())

            // Apply current styles.
            applyPlotStyle()
        }

        override fun addMulti(labels: Map<Int, String>, function: (Double) -> DoubleArray) {
            // Track safe access index.
            var count = Int.MAX_VALUE

            // Materialize all values, memorize min and max for axis.
            val materialized = currentPoints.map { x ->
                currentMinX = min(currentMinX, x)
                currentMaxX = max(currentMaxX, x)

                val ys = function(x)

                for (y in ys) {
                    currentMinY = min(currentMinY, y)
                    currentMaxY = max(currentMaxY, y)
                }

                count = min(count, ys.size)

                DoubleArray(ys.size + 1) {
                    if (it == 0) x else ys[it - 1]
                }
            }

            val t = currentTitle

            for (i in 1..count) {
                currentTitle = labels[i] ?: "$t[$i]"
                plot.addPlot(materialized.map { doubleArrayOf(it[0], it[i]) }.toTypedArray())
                applyPlotStyle()
            }

            currentTitle = t
        }

        /**
         * Applies the plot style to the last plot.
         */
        private fun applyPlotStyle() {
            (plot.plots.lastOrNull() as AbstractPlot?)?.apply {
                setTitle(currentTitle)
                setSmooth(currentSmooth)
            }
        }
    })

    // No plots added.
    if (currentMinY == Double.MAX_VALUE)
        return

    // Compute margins.
    val diffX = (currentMaxX - currentMinX)
    val diffY = (currentMaxY - currentMinY)

    // Assign new boundaries
    plot.getAxis("x").setBoundaries(currentMinX - diffX * extend.left, currentMaxX + diffX * extend.right)
    plot.getAxis("y").setBoundaries(currentMinY - diffY * extend.bottom, currentMaxY + diffY * extend.top)

    // Display plot in thread.
    thread {
        // Acquire entrance to the plotter.
        plotLock.acquire()

        // On completion of plotting, release the plotter.
        plot.terminal = NotifyingTerminal(plot.terminal, {}, {
            plotLock.release()
        })

        // Plot everything.
        plot.plot()
    }
}