package de.fraunhofer.aisec.glue.docker.unix

import java.net.Socket

class UnixSocket(path: String) : Socket(UnixSocketImpl(path))