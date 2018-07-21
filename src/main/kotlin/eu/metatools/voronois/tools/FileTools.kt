package eu.metatools.voronois.tools

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.files.FileHandle
import kotlin.properties.Delegates
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty

/**
 * Configurator for read and write operations.
 */
interface ConfigReadWrite<T> {
    /**
     * Configures the read operation.
     */
    fun read(reader: FileHandle.() -> T)

    /**
     * Configures the write operation.
     */
    fun write(writer: FileHandle.(T) -> Unit)
}

/**
 * Implementation of [ConfigReadWrite], storing the values passed to the functions.
 */
private class ConfigReadWriteStorer<T> : ConfigReadWrite<T> {
    var reader by Delegates.notNull<FileHandle.() -> T>()
    var writer by Delegates.notNull<FileHandle.(T) -> Unit>()

    override fun read(reader: (FileHandle) -> T) {
        this.reader = reader
    }

    override fun write(writer: (FileHandle, T) -> Unit) {
        this.writer = writer
    }

}

/**
 * Delegates reading and writing of a local file to read and write operations. Defaults the value if none is present.
 */
fun <T> localFile(location: String, default: () -> T, config: ConfigReadWrite<T>.() -> Unit): ReadWriteProperty<Any, T> {
    // Configure using the config.
    val configReadWrite = ConfigReadWriteStorer<T>().apply(config)

    // Return newly created read/write property.
    return object : ReadWriteProperty<Any, T> {
        /**
         * The file handle, lazily created as it requires context.
         */
        val handle by lazy { Gdx.files.local(location) }

        /**
         * The last modified value of the file so it does not read every call of get.
         */
        var lastModified: Long = -1

        /**
         * The state store, initially empty.
         */
        var state = listOf<T>()

        override fun getValue(thisRef: Any, property: KProperty<*>): T {
            // If file does not exist, create with default.
            if (!handle.exists()) {
                val value = default()
                configReadWrite.writer(handle, value)
                lastModified = handle.lastModified()
                state = listOf(value)
            }

            // If file is newer than state, read.
            if (handle.lastModified() != lastModified) {
                lastModified = handle.lastModified()
                state = listOf(configReadWrite.reader(handle))
            }

            // Return state
            return state[0]
        }

        override fun setValue(thisRef: Any, property: KProperty<*>, value: T) {
            // Write file and store time and value.
            configReadWrite.writer(handle, value)
            lastModified = handle.lastModified()
            state = listOf(configReadWrite.reader(handle))
        }

    }
}