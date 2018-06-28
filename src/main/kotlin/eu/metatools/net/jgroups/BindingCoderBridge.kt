package eu.metatools.net.jgroups

import org.jgroups.Message
import org.jgroups.Receiver
import org.jgroups.util.MessageBatch
import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.InputStream
import java.io.OutputStream

/**
 * Bridges receiving messages and converting state using the binding coder.
 */
class BindingCoderBridge(val bindingCoder: BindingCoder, val receiver: ValueReceiver) : Receiver {
    override fun receive(msg: Message) {
        val value = bindingCoder.decode(msg)
        receiver.receive(msg, value)
    }

    override fun receive(batch: MessageBatch) {
        val values = batch.array().map { bindingCoder.decode(it) }
        receiver.receive(batch, values)
    }

    override fun getState(output: OutputStream) {
        bindingCoder.encode(DataOutputStream(output), receiver.getState())
    }

    override fun setState(input: InputStream) {
        receiver.setState(bindingCoder.decode(DataInputStream(input)))
    }
}