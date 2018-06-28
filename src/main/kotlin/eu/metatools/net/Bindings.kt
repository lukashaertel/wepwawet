package eu.metatools.net

import kotlinx.serialization.KSerializer
import kotlinx.serialization.internal.*
import kotlinx.serialization.serializer
import kotlin.reflect.KClass

sealed class Binding<T : Any> {
    abstract val kClass: KClass<T>
    abstract val serializer: KSerializer<T>

    companion object {
        val PRIMITIVE_BINDINGS = listOf(
                ExplicitBinding(Long::class, LongSerializer),
                ExplicitBinding(Boolean::class, BooleanSerializer),
                ExplicitBinding(Unit::class, UnitSerializer),
                ExplicitBinding(Short::class, ShortSerializer),
                ExplicitBinding(Char::class, CharSerializer),
                ExplicitBinding(String::class, StringSerializer),
                ExplicitBinding(Byte::class, ByteSerializer),
                ExplicitBinding(Float::class, FloatSerializer),
                ExplicitBinding(Int::class, IntSerializer))
    }
}

data class ImplicitBinding<T : Any>(
        override val kClass: KClass<T>) : Binding<T>() {

    override val serializer
        get() =
            kClass.serializer()
}

data class ExplicitBinding<T : Any>(
        override val kClass: KClass<T>,
        override val serializer: KSerializer<T>) : Binding<T>()
