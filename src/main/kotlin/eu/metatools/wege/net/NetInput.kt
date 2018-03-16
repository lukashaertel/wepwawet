package eu.metatools.wege.net

import java.net.InetAddress


/**
 * Input to the net state machine.
 */
sealed class NetInput

/**
 * Input originating from user interface.
 */
sealed class NetInputUi : NetInput()

object ActionPeriodic : NetInputControl()
object ActionBack : NetInputUi()
object ActionBrowse : NetInputUi()
object ActionHost : NetInputUi()
data class ActionJoin(val address: InetAddress) : NetInputUi()
data class ActionMessage(val message: Message) : NetInputUi()

/**
 * Input originating from network control.
 */
sealed class NetInputControl : NetInput()

object NetInBound : NetInputControl()
object NetInConnected : NetInputControl()
object NetInFail : NetInputControl()
data class NetInInquire(val address: InetAddress) : NetInputControl()
data class NetInAddPlayer(val address: InetAddress, val player: Player) : NetInputControl()
data class NetInRemovePlayer(val address: InetAddress, val player: Player) : NetInputControl()
data class NetInMessage(val address: InetAddress, val player: Player, val message: Message) : NetInputControl()

data class NetInHostAdd(val address: InetAddress, val description: Description) : NetInputControl()
data class NetInHostRemove(val address: InetAddress, val description: Description) : NetInputControl()