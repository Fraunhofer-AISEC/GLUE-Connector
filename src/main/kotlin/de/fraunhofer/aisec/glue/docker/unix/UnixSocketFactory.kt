package de.fraunhofer.aisec.glue.docker.unix

import java.net.InetAddress
import java.net.Socket
import javax.net.SocketFactory

class UnixSocketFactory(private val path: String) : SocketFactory() {
    private fun createSocketImpl(): Socket {
        return UnixSocket(path)
    }

    override fun createSocket(): Socket {
        return createSocketImpl()
    }

    override fun createSocket(
        addr: String?,
        port: Int,
    ): Socket {
        return createSocketImpl()
    }

    override fun createSocket(
        addr: String?,
        port: Int,
        localAddr: InetAddress?,
        localPort: Int,
    ): Socket {
        return createSocketImpl()
    }

    override fun createSocket(
        addr: InetAddress?,
        port: Int,
    ): Socket {
        return createSocketImpl()
    }

    override fun createSocket(
        addr: InetAddress?,
        port: Int,
        localAddr: InetAddress?,
        localPort: Int,
    ): Socket {
        return createSocketImpl()
    }
}
