package eu.metatools.net.jgroups

import org.jgroups.Address

interface ValueSender {
    fun send(destination: Address?, message: Any?)

    fun sendAll(message: Any?) = Unit
}