package eu.metatools.voronois.data

import kotlinx.serialization.Serializable

@Serializable
data class Identity(val id: String, val name: String)