package eu.metatools.wege.net

import eu.metatools.common.change
import java.net.InetAddress


typealias Player = String
typealias Message = String
typealias Description = String

data class Data(val ready: Boolean, val address: InetAddress)


data class Status(
        val host: InetAddress,
        val self: Player,
        val banned: Set<Player>,
        val data: Map<Player, Data>) {

    val players get() = data.keys

    val addresses get() = data.values.map(Data::address)

    fun addPlayer(player: Player, target: InetAddress) =
            copy(data = data + (player to Data(false, target)))

    fun readyPlayer(player: Player) =
            copy(data = data.change(player) { it.copy(ready = true) })

    fun unreadyPlayer(player: Player) =
            copy(data = data.change(player) { it.copy(ready = false) })

    fun removePlayer(player: Player) =
            copy(data = data - player)
}