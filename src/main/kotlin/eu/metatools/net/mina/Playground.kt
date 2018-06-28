package eu.metatools.net.mina

import eu.metatools.common.await
import eu.metatools.net.ImplicitBinding
import eu.metatools.net.mina.bx.*
import kotlinx.coroutines.experimental.*
import kotlinx.serialization.Serializable
import org.apache.mina.filter.codec.ProtocolCodecFilter
import org.apache.mina.filter.logging.LoggingFilter
import org.apache.mina.transport.socket.nio.NioSocketAcceptor
import org.apache.mina.transport.socket.nio.NioSocketConnector
import java.net.InetSocketAddress


@Serializable
data class GetTime(val n: Int, override var id: Id? = null) : Bx

@Serializable
data class GetTimeWithDelay(val n: Int, override var id: Id? = null) : Bx

@Serializable
data class GetData(override var id: Id? = null) : Bx

@Serializable
data class Response(val n: Int, val value: Long, override var id: Id? = null) : Bx


fun main(args: Array<String>) = runBlocking {
    val codecs = BindingCodecFactory(listOf(
            ImplicitBinding(BxException::class),
            ImplicitBinding(GetTime::class),
            ImplicitBinding(GetTimeWithDelay::class),
            ImplicitBinding(GetData::class),
            ImplicitBinding(Response::class)))

    val host = launch {
        val acceptor = NioSocketAcceptor()
        acceptor.filterChain.addLast("logging", LoggingFilter())
        acceptor.filterChain.addLast("codecs", ProtocolCodecFilter(codecs))
        acceptor.handler = object : BxHost(null) {
            override suspend fun process(bx: Bx) = when (bx) {
                is GetTime -> Response(bx.n, System.currentTimeMillis())
                is GetTimeWithDelay -> {
                    delay(2000)
                    Response(bx.n, System.currentTimeMillis())
                }
                is GetData -> throw UnsupportedOperationException("Dunno how to do that yet")
                else -> throw UnsupportedOperationException()
            }
        }
        acceptor.bind(InetSocketAddress(14400))
    }

    val client = launch {
        val connector = NioSocketConnector()
        connector.filterChain.addLast("logging", LoggingFilter())
        connector.filterChain.addLast("codecs", ProtocolCodecFilter(codecs))
        connector.handler = BxClient(null)

        val session = await(connector.connect(InetSocketAddress("localhost", 14400))).session


        (0..10).map { n ->
            launch { println(request(session, GetTime(n))) }
        }.joinAll()

        await(session.closeNow())

        connector.dispose()
    }
    client.join()
    host.cancelAndJoin()
}