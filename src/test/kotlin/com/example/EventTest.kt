package com.example

import junit.framework.TestCase.assertEquals
import org.junit.Test
import java.math.BigDecimal

class EventTest {
    @Test
    fun `fromPlainTextString should return an SuccessResult with the correct values parsed into an event`() {
        // Given
        val text = "1607341341814,0.0442672968,1282509067"
        // When
        val event = Event.fromPlainTextString(text).filterIsInstance<EventParsingResult.Success>().first().event
        // Then
        assertEquals(1607341341814, event.timestamp)
        assertEquals(BigDecimal.valueOf(0.0442672968), event.x)
        assertEquals(1282509067, event.y)
    }

    @Test
    fun `fromPlainTextString should return Failure results if an event is invalid`() {
        // Given
        val empty = ""
        val notEnoughInputs = "1,1"
        val tooManyInputs = "1,1,1,1"
        val invalidX = "1,1a,1"

        val invalidInputs = listOf(
            empty,
            notEnoughInputs,
            tooManyInputs,
            invalidX,
        )

        val inputString = invalidInputs.joinToString("\n")

        // Then
        Event.fromPlainTextString(inputString).forEach {
            assert(it is EventParsingResult.Failure)
        }
    }
}