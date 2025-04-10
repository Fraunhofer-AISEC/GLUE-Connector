package de.fraunhofer.aisec.glue.config

import io.ktor.server.config.*
import kotlin.properties.ReadOnlyProperty

/**
 * The base class for all configuration types.
 * This class offers various combinators to work with configuration options.
 * Note that any validation and conversion is only done when accessing the configuration values, so errors are not
 * detected early.
 */
abstract class Config(val rawConfig: ApplicationConfig) {
    /**
     * Get a string property from the configuration object.
     * The name argument specifies the path relative to the configuration object separated by '.' characters.
     * If no name is given, the property name is used.
     * Dereferencing this property throws an [ApplicationConfigurationException] when the property in question is not
     * a string literal.
     */
    protected fun stringProp(name: String? = null): ReadOnlyProperty<Any, String?> {
        return ReadOnlyProperty { _, property -> rawConfig.propertyOrNull(name ?: property.name)?.let(ApplicationConfigValue::getString) }
    }

    /**
     * Get a (string) list property from the configuration object.
     * The name argument specifies the path relative to the configuration object separated by '.' characters.
     * If no name is given, the property name is used.
     * Dereferencing this property throws an [ApplicationConfigurationException] when the property in question is not
     * a list of strings.
     */
    protected fun listProp(name: String? = null): ReadOnlyProperty<Any, List<String>?> {
        return ReadOnlyProperty { _, property -> rawConfig.propertyOrNull(name ?: property.name)?.let(ApplicationConfigValue::getList) }
    }

    /**
     * Convert the type of a given property using [converter].
     * This is equivalent of the [let] receiver function, but for property delegates.
     */
    protected fun <T, R> convert(
        from: ReadOnlyProperty<Any, T?>,
        converter: (T) -> R,
    ): ReadOnlyProperty<Any, R?> {
        return ReadOnlyProperty { thisRef, property -> from.getValue(thisRef, property)?.let(converter) }
    }

    /**
     * Supply a default value for a given property.
     * If the property returns null, the default is returned instead, making the property non-null.
     * This is the equivalent of the elvis operator (`?:`).
     */
    protected fun <T> withDefault(
        from: ReadOnlyProperty<Any, T?>,
        default: T,
    ): ReadOnlyProperty<Any, T> {
        return ReadOnlyProperty { thisRef, property -> from.getValue(thisRef, property) ?: default }
    }

    /**
     * Require a property to not be null.
     * If the property is null, an exception is thrown with an optional custom [message].
     * This is equivalent to the non-null assertion operator (`!!`).
     */
    protected fun <T> required(
        from: ReadOnlyProperty<Any, T?>,
        message: String? = null,
    ): ReadOnlyProperty<Any, T> {
        return ReadOnlyProperty {
                thisRef,
                property,
            ->
            from.getValue(thisRef, property) ?: throw NullPointerException(message ?: "Property is required but not set")
        }
    }

    /**
     * Parse a string property into an int.
     * The getter can additionally throw a [NumberFormatException] if the property cannot be parsed.
     * @see stringProp
     */
    protected fun intProp(name: String? = null) = convert(stringProp(name), String::toInt)

    /**
     * Parse a string property into a boolean using [String.toBoolean].
     * @see stringProp
     */
    protected fun boolProp(name: String? = null) = convert(stringProp(name), String::toBoolean)

    /**
     * Parse a string property into a double.
     * The getter can additionally throw a [NumberFormatException] if the property cannot be parsed.
     * @see stringProp
     */
    protected fun doubleProp(name: String? = null) = convert(stringProp(name), String::toDouble)

    /**
     * Convert a list property into a list of integers.
     * The getter will throw a [NumberFormatException] if any list member cannot be parsed.
     * @see listProp
     * @see intProp
     */
    protected fun intListProp(name: String? = null) = convert(listProp(name)) { it.map(String::toInt) }

    /**
     * Convert a list property into a list of booleans.
     * @see listProp
     * @see boolProp
     */
    protected fun boolListProp(name: String? = null) = convert(listProp(name)) { it.map(String::toBoolean) }

    /**
     * Convert a list property into a list of doubles.
     * The getter will throw a [NumberFormatException] if any list member cannot be parsed.
     * @see listProp
     * @see doubleProp
     */
    protected fun doubleListProp(name: String? = null) = convert(listProp(name)) { it.map(String::toDouble) }
}
