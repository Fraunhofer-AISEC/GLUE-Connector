package de.fraunhofer.aisec.glue.docker.unix

import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.Socket
import javax.net.SocketFactory

class UnixSocketFactory(private val path: String) : SocketFactory() {
    override fun createSocket(): Socket {
        return HttpUnixSocket(path)
    }

    override fun createSocket(
        addr: String?,
        port: Int,
    ): Socket {
        return HttpUnixSocket(path, InetSocketAddress(addr, port))
    }

    override fun createSocket(
        addr: String?,
        port: Int,
        localAddr: InetAddress?,
        localPort: Int,
    ): Socket {
        return HttpUnixSocket(path, InetSocketAddress(addr, port))
    }

    override fun createSocket(
        addr: InetAddress?,
        port: Int,
    ): Socket {
        return HttpUnixSocket(path, InetSocketAddress(addr, port))
    }

    override fun createSocket(
        addr: InetAddress?,
        port: Int,
        localAddr: InetAddress?,
        localPort: Int,
    ): Socket {
        return HttpUnixSocket(path, InetSocketAddress(addr, port))
    }
}
