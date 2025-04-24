package de.fraunhofer.aisec.glue.config

import io.ktor.server.config.*

/**
 * The [ConfigManager] class creates and stores all configuration objects.
 * A configuration class must inherit from [Config].
 */
class ConfigManager(private val baseConfig: ApplicationConfig) {
    private val configCache = mutableMapOf<Class<out Config>, Config>()

    private fun <T : Config> deriveConfigName(cls: Class<T>): String {
        // Sometimes, we want to have a configuration object with a different name.
        // For example, if we have a nested name (`namespaced.config`) or a fragment of a larger configuration object.
        val basePath = cls.getDeclaredAnnotation(BasePath::class.java)
        if (basePath != null) {
            return basePath.path
        }

        // We also want to have a nice default, which is just the class name without an optional "Config" suffix and
        // without andy of the kotlin name mangling.
        return cls.simpleName.removeSuffix("Config").lowercase()
    }

    /**
     * Get or create a configuration object of type [T] using the provided [factory].
     * The [factory] is provided an [ApplicationConfig] with its contents derived from the type [T].
     * All configuration objects are cached and should therefore not be mutated.
     *
     * The supplied [ApplicationConfig] instance always contains a section of the configuration.
     * The path of that section is determined using the following mechanism:
     *
     * - If the type has a [BasePath] annotation, the section name is taken from that annotation.
     * - Otherwise, the name of the class without an optional `Config` suffix is converted to lower case and used as the
     *   section name
     */
    fun <T : Config> getConfig(
        cls: Class<T>,
        factory: (ApplicationConfig) -> T,
    ): T {
        @Suppress("UNCHECKED_CAST")
        return configCache.getOrPut(cls) {
            val baseName = deriveConfigName(cls)
            // Annoyingly, the ktor ApplicationConfig only has a throwing method to get subconfigs.
            // Since we build a class-based system on top, we just substitute an empty object instead and let the
            // property system figure out the rest.
            val rawConfig = runCatching { baseConfig.config(baseName) }.getOrElse { MapApplicationConfig() }
            factory(rawConfig)
        } as T
    }

    inline fun <reified T : Config> getConfig(noinline factory: (ApplicationConfig) -> T): T {
        return getConfig(T::class.java, factory)
    }
}
