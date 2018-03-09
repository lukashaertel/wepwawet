package eu.metatools.common

import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerialContext
import kotlinx.serialization.internal.*
import kotlinx.serialization.serializer

val primitiveSerialContext = SerialContext().apply {
    registerSerializer(Long::class, LongSerializer)
    registerSerializer(Boolean::class, BooleanSerializer)
    registerSerializer(Unit::class, UnitSerializer)
    registerSerializer(Short::class, ShortSerializer)
    registerSerializer(Char::class, CharSerializer)
    registerSerializer(String::class, StringSerializer)
    registerSerializer(Byte::class, ByteSerializer)
    registerSerializer(Float::class, FloatSerializer)
    registerSerializer(Int::class, IntSerializer)
}

fun Any.objectSerializer() =
        this::class.serializer().asSafe<KSerializer<Any>>()