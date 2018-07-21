package eu.metatools.net.jgroups

import org.jgroups.Address
import org.jgroups.JChannel
import org.jgroups.conf.ProtocolStackConfigurator
import org.jgroups.stack.Protocol
import org.jgroups.util.ByteArrayDataOutputStream
import org.w3c.dom.Element
import java.io.File
import java.io.InputStream
import java.net.URL

/**
 * [JChannel] that uses [BindingCoder] to transform messages.
 */
class BindingChannel : JChannel {
    val bindingCoder: BindingCoder

    constructor(create_protocol_stack: Boolean, bindingCoder: BindingCoder) : super(create_protocol_stack) {
        this.bindingCoder = bindingCoder
    }

    constructor(bindingCoder: BindingCoder) : super() {
        this.bindingCoder = bindingCoder
    }

    constructor(file: File?, bindingCoder: BindingCoder) : super(file) {
        this.bindingCoder = bindingCoder
    }

    constructor(properties: Element?, bindingCoder: BindingCoder) : super(properties) {
        this.bindingCoder = bindingCoder
    }

    constructor(properties: URL?, bindingCoder: BindingCoder) : super(properties) {
        this.bindingCoder = bindingCoder
    }

    constructor(props: String?, bindingCoder: BindingCoder) : super(props) {
        this.bindingCoder = bindingCoder
    }

    constructor(input: InputStream?, bindingCoder: BindingCoder) : super(input) {
        this.bindingCoder = bindingCoder
    }

    constructor(configurator: ProtocolStackConfigurator?, bindingCoder: BindingCoder) : super(configurator) {
        this.bindingCoder = bindingCoder
    }

    constructor(vararg protocols: Protocol?, bindingCoder: BindingCoder) : super(*protocols) {
        this.bindingCoder = bindingCoder
    }

    constructor(protocols: MutableCollection<Protocol>?, bindingCoder: BindingCoder) : super(protocols) {
        this.bindingCoder = bindingCoder
    }

    constructor(protocols: MutableList<Protocol>?, bindingCoder: BindingCoder) : super(protocols) {
        this.bindingCoder = bindingCoder
    }

    constructor(ch: JChannel?, bindingCoder: BindingCoder) : super(ch) {
        this.bindingCoder = bindingCoder
    }

    var valueReceiver: ValueReceiver?
        get() = (receiver as? BindingCoderBridge)?.receiver
        set(value) {
            receiver = value?.let {
                BindingCoderBridge(bindingCoder, it)
            }
        }

    override fun send(dst: Address?, obj: Any?): JChannel {
        return ByteArrayDataOutputStream(bindingCoder.initialCapacity).let {
            bindingCoder.encode(it, obj)
            // Send to destination.
            super.send(dst, it.buffer(), 0, it.position())
        }
    }
}