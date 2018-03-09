package eu.metatools.net

import com.esotericsoftware.minlog.Log
import eu.metatools.common.firstOnly
import kotlinx.coroutines.experimental.delay
import kotlinx.coroutines.experimental.launch
import kotlinx.coroutines.experimental.runBlocking
import kotlinx.serialization.Serializable


@Serializable
data class Msg(val name: String, val value: String)

val config = Config(classMapper {
    with<Msg>()
})

fun main(args: Array<String>) {
    Log.set(Log.LEVEL_WARN)
    launch {
        val x = Peer(config)
        x.addListener(listener {
            received { c, v ->
                println("Received $v on $c")
            }
        })
        x.start()
        for (b in x.browse(coroutineContext))
            println(b)
        x.stop()
    }

    launch {
        while (isActive) {
            val h = Host<Unit>(config)
            h.start()
            h.bind()
            delay(5000)
            h.stop()
            h.close()
            delay(5000)
        }
    }

    readLine()

    if (true) return
    val h = Host<Unit>(config)
    h.start()
    h.bind()

    h.addListener(linkListener {
        connected { println("Connected $it") }
        disconnected { println("Disconnected $it") }
        idle { println("Idle on $it") }
        received { c, v ->
            println("Received $v on $c")
        }
    })

    runBlocking {
        val c = Peer(config)
        c.suspended.start()
        val a = c.produceDiscover().firstOnly()
        c.suspended.connect(a)
        c.suspended.sendTCP(Msg("Me", "Hello server"))
        c.close()
    }

    h.stop()
    h.close()
}