package eu.metatools.net.mina.bx

import eu.metatools.serialization.ThrowableSerializer
import kotlinx.serialization.Serializable

@Serializable
data class BxException(@Serializable(with = ThrowableSerializer::class) val throwable: Throwable, override var id: Id? = null) : Bx