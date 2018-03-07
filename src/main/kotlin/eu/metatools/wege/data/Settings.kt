package eu.metatools.wege.data

import kotlinx.serialization.Serializable

@Serializable
data class Settings(val name: String) {
    companion object {
        val default = Settings("John Doe")
    }
}