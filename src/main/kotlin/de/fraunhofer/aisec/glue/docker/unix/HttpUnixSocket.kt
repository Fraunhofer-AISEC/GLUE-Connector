package de.fraunhofer.aisec.glue.docker.unix

import jnr.unixsocket.UnixSocket
import jnr.unixsocket.UnixSocketAddress
import jnr.unixsocket.UnixSocketChannel
import java.net.InetSocketAddress
import java.net.SocketAddress

class HttpUnixSocket(private val path: String, private var inetAddress: InetSocketAddress? = null) :
    UnixSocket(UnixSocketChannel.create()) {
    override fun connect(addr: SocketAddress?) {
        inetAddress = addr as? InetSocketAddress
        return super.connect(UnixSocketAddress(path))
    }

    override fun connect(
        addr: SocketAddress?,
        timeout: Int,
    ) {
        inetAddress = addr as? InetSocketAddress
        return super.connect(UnixSocketAddress(path), timeout)
    }

    override fun getInetAddress() = inetAddress?.address
}
