package com.example

import junit.framework.TestCase.assertEquals
import org.junit.Test
import java.math.BigDecimal

class RollingStatsResultTest {
    @Test
    fun `fromPlainTextString should return an Event with the correctly rounded values`() {
        // Given
        val result = RollingStatsResult(1, BigDecimal.valueOf(0.0442672961999), BigDecimal.valueOf(0.04426729688999), 1282509067, BigDecimal.valueOf(1.0456111111119))
        // When
        val resultString = result.toPlainTextString()
        // Then
        assertEquals("1,0.0442672962,0.0442672969,1282509067,1.046", resultString)
    }
}