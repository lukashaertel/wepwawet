package eu.metatools.net.jgroups

import org.jgroups.Message
import org.jgroups.util.MessageBatch

interface ValueReceiver {
    fun receive(source: Message, message: Any?)

    fun receive(source: MessageBatch, messages: List<Any?>) {
        for ((s, m) in source.array().zip(messages))
            receive(s, m)
    }

    fun getState(): Any? = null

    fun setState(state: Any?) = Unit
}