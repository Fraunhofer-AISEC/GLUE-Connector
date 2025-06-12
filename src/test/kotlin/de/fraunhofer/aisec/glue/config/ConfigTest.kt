package de.fraunhofer.aisec.glue.config

import io.ktor.server.config.*
import org.junit.jupiter.api.assertThrows
import kotlin.test.Test
import kotlin.test.assertEquals

class ConfigTest {
    @Test
    fun `Typed property accessors correctly convert values`() {
        val rawConfig =
            MapApplicationConfig(
                "stringValue" to "string",
                "intValue" to "1",
                "doubleValue" to "1.1",
                "boolValue" to "true",
                // The int list will just be the representative for the other list properties.
                // The MapApplicationConfig syntax here is a bit awkward.
                "intListValue.size" to "3",
                "intListValue.0" to "1",
                "intListValue.1" to "2",
                "intListValue.2" to "3",
            )

        val testConfig =
            object : Config(rawConfig) {
                val stringValue by required(stringProp())
                val intValue by required(intProp())
                val doubleValue by required(doubleProp())
                val boolValue by required(boolProp())
                val intListValue by required(intListProp())
                val absentValue by withDefault(stringProp(), "default")

                // TODO: If we move to eager evaluation, remove the following key
                val absentLazyValue by required(stringProp())
            }

        assertEquals("string", testConfig.stringValue)
        assertEquals(1, testConfig.intValue)
        assertEquals(1.1, testConfig.doubleValue)
        assertEquals(true, testConfig.boolValue)
        assertEquals(listOf(1, 2, 3), testConfig.intListValue)
        assertEquals("default", testConfig.absentValue)
        assertThrows<NullPointerException> { testConfig.absentLazyValue }
    }

    @Test
    fun `ConfigManager resolves the BasePath annotation correctly`() {
        val configManager =
            ConfigManager(
                MapApplicationConfig(
                    "path.testKey" to "present",
                    "nested.path.testKey" to "present",
                ),
            )

        @BasePath("path")
        class ExplicitlyNamedConfig(cfg: ApplicationConfig) : Config(cfg) {
            val testKey by required(stringProp())
        }

        val annotatedConfig = configManager.getConfig(::ExplicitlyNamedConfig)
        assertEquals("present", annotatedConfig.testKey)

        @BasePath("nested.path")
        class NestedConfig(cfg: ApplicationConfig) : Config(cfg) {
            val testKey by required(stringProp())
        }

        val nestedAnnotatedConfig = configManager.getConfig(::NestedConfig)
        assertEquals("present", nestedAnnotatedConfig.testKey)
    }

    @Test
    fun `ConfigManager derives names from class names correctly`() {
        val configManager =
            ConfigManager(
                MapApplicationConfig(
                    "toplevel.testKey" to "present",
                    "nested.testKey" to "present",
                    "inner.testKey" to "present",
                    "functionlocal.testKey" to "present",
                    "noconfigsuffix.testKey" to "present",
                ),
            )

        class FunctionLocalConfig(cfg: ApplicationConfig) : Config(cfg) {
            val testKey by required(stringProp())
        }

        val topLevelConfig = configManager.getConfig(::TopLevelConfig)
        assertEquals("present", topLevelConfig.testKey)

        val nestedConfig = configManager.getConfig(::NestedConfig)
        assertEquals("present", nestedConfig.testKey)

        val innerConfig = configManager.getConfig(::InnerConfig)
        assertEquals("present", innerConfig.testKey)

        val functionLevelConfig = configManager.getConfig(::FunctionLocalConfig)
        assertEquals("present", functionLevelConfig.testKey)

        val noConfigSuffix = configManager.getConfig(::NoConfigSuffix)
        assertEquals("present", noConfigSuffix.testKey)
    }

    class NestedConfig(cfg: ApplicationConfig) : Config(cfg) {
        val testKey by required(stringProp())
    }

    inner class InnerConfig(cfg: ApplicationConfig) : Config(cfg) {
        val testKey by required(stringProp())
    }
}
