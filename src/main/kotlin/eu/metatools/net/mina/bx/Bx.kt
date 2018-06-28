package eu.metatools.net.mina.bx

/**
 * Binary communication request/response header.
 */
interface Bx {
    /**
     * The identity of the request/response.
     */
    var id: Id?
}