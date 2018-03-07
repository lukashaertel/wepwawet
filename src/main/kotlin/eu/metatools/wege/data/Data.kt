package eu.metatools.wege.data

import kotlinx.serialization.Serializable
import java.util.*

@Serializable
data class Identity(val id: String, val name: String)