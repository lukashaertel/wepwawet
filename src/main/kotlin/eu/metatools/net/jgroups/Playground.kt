package eu.metatools.net.jgroups

import eu.metatools.net.ImplicitBinding
import kotlinx.serialization.Serializable
import org.jgroups.Global


@Serializable
data class Chat(val name: String, val text: String)

@Serializable
data class History(val chat: List<Chat>)

var history = History(listOf())

fun main(args: Array<String>) {
    // Get coder.
    val coder = BindingCoder(listOf(
            ImplicitBinding(Chat::class),
            ImplicitBinding(History::class)))

    // Get and configure channel.
    val channel = BindingChannel(Global.DEFAULT_PROTOCOL_STACK, coder)
    channel.valueReceiver = object : ValueReceiver {
        override fun receive(source: org.jgroups.Message, message: Any?) {
            when (message) {
                is Chat -> {
                    history = history.copy(history.chat + message)
                    println(message)
                }
            }
        }

        override fun getState(): History {
            print("Generating state")
            return history
        }

        override fun setState(state: Any?) {
            history = state as History
            for (c in history.chat)
                println(c)
        }
    }

    val name = readLine() ?: return

    channel.connect("Chat cluster")
    channel.getState(null, 10000)

    for (s in generateSequence { readLine() })
        if (s != "exit")
            channel.send(null, Chat(name, s))
        else
            break

    channel.close()
}