package de.fraunhofer.aisec.glue.logging

import ch.qos.logback.classic.Level
import ch.qos.logback.classic.spi.ILoggingEvent
import ch.qos.logback.core.pattern.color.ANSIConstants
import ch.qos.logback.core.pattern.color.ForegroundCompositeConverterBase
import java.util.*

class LevelHighlightingCompositeConverter : ForegroundCompositeConverterBase<ILoggingEvent>() {
    private val levelToColor: MutableMap<Int, String> = mutableMapOf()

    override fun start() {
        levelToColor[Level.ERROR_INT] = "${ANSIConstants.BOLD}${ANSIConstants.RED_FG}"
        levelToColor[Level.WARN_INT] = ANSIConstants.YELLOW_FG
        levelToColor[Level.INFO_INT] = ANSIConstants.WHITE_FG
        levelToColor[Level.DEBUG_INT] = ANSIConstants.CYAN_FG
        levelToColor[Level.TRACE_INT] = ANSIConstants.BLUE_FG

        val options = optionList ?: emptyList()
        for (option in options) {
            val parts = option.split("=", limit = 2)
            if (parts.size != 2) {
                addWarn("Ignoring malformed level color option '$option'. Expected LEVEL=COLOR.")
                continue
            }
            val levelKey = parts[0].trim()
            val colorKey = parts[1].trim()
            val levelInt = parseLevel(levelKey)
            val colorCode = parseColor(colorKey)
            if (levelInt == null) {
                addWarn("Ignoring unknown level '$levelKey' in level color map.")
                continue
            }
            if (colorCode == null) {
                addWarn("Ignoring unknown color '$colorKey' in level color map.")
                continue
            }
            levelToColor[levelInt] = colorCode
        }

        super.start()
    }

    override fun getForegroundColorCode(event: ILoggingEvent): String {
        return levelToColor[event.level.toInt()] ?: ANSIConstants.DEFAULT_FG
    }

    private fun parseLevel(raw: String): Int? {
        val normalized = raw.trim().uppercase(Locale.ROOT)
        return when (normalized) {
            "ERROR", "SEVERE" -> Level.ERROR_INT
            "WARN", "WARNING" -> Level.WARN_INT
            "INFO" -> Level.INFO_INT
            "DEBUG" -> Level.DEBUG_INT
            "TRACE" -> Level.TRACE_INT
            "FATAL" -> Level.ERROR_INT
            else -> null
        }
    }

    private fun parseColor(raw: String): String? {
        val normalized = raw.trim().lowercase(Locale.ROOT)
        if (normalized.isEmpty()) {
            return null
        }

        if (normalized.startsWith("bg_")) {
            return when (normalized.removePrefix("bg_")) {
                "black" -> "40"
                "red" -> "41"
                "green" -> "42"
                "yellow" -> "43"
                "blue" -> "44"
                "magenta" -> "45"
                "cyan" -> "46"
                "white" -> "47"
                "default" -> "49"
                else -> null
            }
        }

        return when (normalized) {
            "black" -> ANSIConstants.BLACK_FG
            "red" -> ANSIConstants.RED_FG
            "green" -> ANSIConstants.GREEN_FG
            "yellow" -> ANSIConstants.YELLOW_FG
            "blue" -> ANSIConstants.BLUE_FG
            "magenta" -> ANSIConstants.MAGENTA_FG
            "cyan" -> ANSIConstants.CYAN_FG
            "white" -> ANSIConstants.WHITE_FG
            "default" -> ANSIConstants.DEFAULT_FG
            else -> null
        }
    }
}
