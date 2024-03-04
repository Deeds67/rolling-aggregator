package com.example

import java.math.BigDecimal

sealed class EventParsingResult {
    data class Success(val event: Event): EventParsingResult()
    data class Failure(val error: Errors): EventParsingResult()
}
data class Event(val timestamp: Long, val x: BigDecimal, val y: Long) {
    companion object {
        fun fromPlainTextString(text: String): List<EventParsingResult> {
            val lines = text.split("\n")
            return lines.map { parseEvent(it) }
        }
        private fun parseEvent(text: String): EventParsingResult {
            val split = text.split(",")
            if (split.size != 3) return EventParsingResult.Failure(InvalidEventError(text))

            return try {
                EventParsingResult.Success(Event(split[0].toLong(), split[1].toBigDecimal(), split[2].toLong()))
            } catch (e: NumberFormatException) {
                EventParsingResult.Failure(InvalidEventError(text))
            }
        }
    }
}