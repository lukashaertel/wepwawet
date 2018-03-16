package eu.metatools.wege.net

import java.net.InetAddress


/**
 * Output of the net state machine.
 */
sealed class NetEvent

/**
 * Output to the user interface.
 */
sealed class NetEventUi : NetEvent()

object StageEnterBrowser : NetEventUi()
object StageLeaveBrowser : NetEventUi()
object StageEnterProcessing : NetEventUi()
object StageLeaveProcessing : NetEventUi()
object StageEnterHost : NetEventUi()
object StageLeaveHost : NetEventUi()
object StageEnterPeer : NetEventUi()
object StageLeavePeer : NetEventUi()
data class StageAddPlayer(val player: Player) : NetEventUi()
data class StageRemovePlayer(val player: Player) : NetEventUi()
data class StageMessage(val player: Player, val message: Message) : NetEventUi()
data class StageAddServer(val address: InetAddress, val description: Description) : NetEventUi()
data class StageRemoveServer(val address: InetAddress, val description: Description) : NetEventUi()
data class StageUnhandled(val item: Any) : NetEventUi()


/**
 * Output to the network control.
 */
sealed class NetEventControl : NetEvent()

object NetOutStartPeer : NetEventControl()
object NetOutStopPeer : NetEventControl()
object NetOutStartHost : NetEventControl()
object NetOutStopHost : NetEventControl()
class NetOutConnectPeer(val address: InetAddress) : NetEventControl()
object NetOutUpdateHosts : NetEventControl()
data class NetOutDescribe(val address: InetAddress) : NetEventControl()
data class NetOutAddPlayer(val address: InetAddress, val player: Player) : NetEventControl()
data class NetOutRemovePlayer(val address: InetAddress, val player: Player) : NetEventControl()
data class NetOutMessage(val address: InetAddress, val player: Player, val message: Message) : NetEventControl()
