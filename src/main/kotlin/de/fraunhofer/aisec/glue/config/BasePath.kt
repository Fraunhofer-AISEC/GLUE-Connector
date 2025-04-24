package de.fraunhofer.aisec.glue.config

/**
 * The base path annotation defines the root path to the config object that the annotated class is relative to.
 * If a class is annotated with `@BasePath("example"), then all properties are looked up relative to that.
 * If this annotation is absent, the name of the class is used as lower case without the optional Config suffix.
 */
annotation class BasePath(val path: String)
