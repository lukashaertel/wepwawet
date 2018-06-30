package eu.metatools.net.jgroups

import eu.metatools.net.ImplicitBinding
import eu.metatools.wepwawet.Container
import eu.metatools.wepwawet.Entity
import eu.metatools.wepwawet.Revision
import kotlinx.coroutines.experimental.channels.produce
import kotlinx.coroutines.experimental.runBlocking
import kotlinx.coroutines.experimental.withTimeoutOrNull
import kotlinx.serialization.Serializable
import org.jgroups.Global
import org.jgroups.Message


@Serializable
data class Call(val time: Int,
                val inner: Short,
                val author: Byte,
                val ids: List<Any?>,
                val call: Byte,
                val arg: Any?)

@Serializable
data class History(val calls: List<Call>)

var history = History(listOf())

fun Container.receive(call: Call) {
    receive(Revision(call.time, call.inner, call.author), call.ids, call.call, call.arg)
}

class Ex(container: Container) : Entity(container) {
    var r by prop(0)

    val right by impulse { ->
        r += 1
    }

    val left by impulse { ->
        r -= 1
    }

    fun display() {
        if (r >= 0) {
            print(" ".repeat(r))
            println("x")
        }
    }
}

fun main(args: Array<String>) = runBlocking {
    // Get coder.
    val coder = BindingCoder(listOf(
            ImplicitBinding(Call::class),
            ImplicitBinding(History::class))) and BindingCoder.PRIMITIVE

    // Get and configure channel.
    val channel = BindingChannel(Global.DEFAULT_PROTOCOL_STACK, coder)

    val author = readLine()?.toIntOrNull()?.toByte() ?: return@runBlocking
    val container = object : Container(author) {
        override fun dispatch(time: Revision, id: List<Any?>, call: Byte, arg: Any?) {
            val message = Call(time.time, time.inner, time.author, id, call, arg)
            history = history.copy(history.calls + message)
            channel.send(null, message)
            println("S>$message")
        }
    }

    channel.valueReceiver = object : ValueReceiver {
        override fun receive(source: Message, message: Any?) {
            when (message) {
                is Call -> {
                    if (message.author != author) {
                        history = history.copy(history.calls + message)
                        container.receive(message)
                        println("R>$message")
                    } else {
                        println("L>$message")
                    }
                }
            }
        }

        override fun getState(): History {
            return history
        }

        override fun setState(state: Any?) {
            history = state as History
            for (call in history.calls) {
                container.receive(call)
                println("H>$call")
            }
        }
    }

    channel.connect("Chat cluster")

    val ex = container.init(::Ex)

    channel.getState(null, 10000)


    val cmds = produce {
        while (true) {
            val line = readLine()
            if (line == null)
                break
            send(line)
        }
    }

    loop@ while (true) {

        container.time = System.currentTimeMillis().toInt()
        container.repo.softUpper = container.rev()
        ex.display()

        val cmd = withTimeoutOrNull(500) { cmds.receive() }
        if (cmd == null)
            continue

        when (cmd) {
            "l" -> ex.left()
            "r" -> ex.right()
            "e" -> break@loop
        }
    }

    channel.close()
}