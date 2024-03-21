package de.fraunhofer.aisec.glue.docker.unix

import java.io.InputStream
import java.io.OutputStream
import java.net.*
import java.nio.channels.Channels
import java.nio.channels.SocketChannel

class UnixSocketImpl(private val path: String) : SocketImpl() {
    private lateinit var channel: SocketChannel

    private fun connectImpl() {
        val address = UnixDomainSocketAddress.of(path)
        channel = SocketChannel.open(address)
    }

    override fun create(p0: Boolean) {}

    override fun connect(
        p0: String?,
        p1: Int,
    ) {
        connectImpl()
    }

    override fun connect(
        p0: InetAddress?,
        p1: Int,
    ) {
        connectImpl()
    }

    override fun connect(
        p0: SocketAddress?,
        p1: Int,
    ) {
        connectImpl()
    }

    override fun bind(
        p0: InetAddress?,
        p1: Int,
    ) {
        throw UnsupportedOperationException("Only client-side sockets are supported")
    }

    override fun listen(p0: Int) {
        throw UnsupportedOperationException("Only client-side sockets are supported")
    }

    override fun accept(p0: SocketImpl?) {
        throw UnsupportedOperationException("Only client-side sockets are supported")
    }

    override fun getInputStream(): InputStream {
        return Channels.newInputStream(channel)
    }

    override fun getOutputStream(): OutputStream {
        return Channels.newOutputStream(channel)
    }

    override fun available(): Int {
        return 0
    }

    override fun close() {
        channel.close()
    }

    override fun supportsUrgentData(): Boolean {
        return false
    }

    override fun sendUrgentData(p0: Int) {
        throw UnsupportedOperationException()
    }

    override fun supportedOptions(): Set<SocketOption<*>> {
        return setOf()
    }

    override fun setOption(
        opt: Int,
        arg: Any?,
    ) {
    }

    override fun getOption(p0: Int): Any? {
        return null
    }
}