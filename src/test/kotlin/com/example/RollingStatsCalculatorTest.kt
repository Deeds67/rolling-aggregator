package com.example

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import java.math.BigDecimal
import java.math.MathContext
import java.util.concurrent.ConcurrentLinkedQueue
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals

class RollingStatsCalculatorTest {
    @Test
    fun `addEventAndRemoveStaleEvents should add an event if it is within the specified rolling period`() {
        // Given
        val calculator = RollingStatsCalculator()
        val currentTime = System.currentTimeMillis()

        val xValue = Random.nextDouble().toBigDecimal()
        val yValue = Random.nextInt().toLong()
        val event = Event(currentTime, xValue, yValue)
        // When
        calculator.addEventAndRemoveStaleEvents(event, currentTime)

        // Then
        assertEquals(1, calculator.events.size)
        assertEquals(xValue, calculator.sumX.get())
        assertEquals(yValue, calculator.sumY.get())
        assertEquals(1, calculator.count.get())
    }

    @Test
    fun `addEventAndRemoveStaleEvents should add an event if it matches exactly the specified rolling period`() {
        // Given
        val rollingPeriod = 10000L
        val calculator = RollingStatsCalculator(rollingDurationMillis = rollingPeriod)
        val currentTime = System.currentTimeMillis()

        val xValue = Random.nextDouble().toBigDecimal()
        val yValue = Random.nextInt().toLong()
        val event = Event(currentTime - rollingPeriod, xValue, yValue)
        // When
        calculator.addEventAndRemoveStaleEvents(event, currentTime)

        // Then
        assertEquals(1, calculator.events.size)
        assertEquals(xValue, calculator.sumX.get())
        assertEquals(yValue, calculator.sumY.get())
        assertEquals(1, calculator.count.get())
    }

    @Test
    fun `addEventAndRemoveStaleEvents should not add an event if it is older than the rolling period`() {
        // Given
        val rollingPeriod = 60000L
        val calculator = RollingStatsCalculator(rollingDurationMillis = rollingPeriod)
        val currentTime = System.currentTimeMillis()
        val event = Event(currentTime - (rollingPeriod + 1), BigDecimal.valueOf(1.0), 1)
        // When
        calculator.addEventAndRemoveStaleEvents(event, currentTime)
        // Then
        assertEquals(0, calculator.events.size)
        assertEquals(BigDecimal.ZERO, calculator.sumX.get())
        assertEquals(0, calculator.sumY.get())
        assertEquals(0, calculator.count.get())
    }

    @Test
    fun `getRollingAverageAndRemoveStaleEvents should correctly calculate the RollingPeriodResult`() {
        // Given
        val rollingPeriod = 60000L
        val calculator = RollingStatsCalculator(rollingDurationMillis = rollingPeriod)
        val currentTime = System.currentTimeMillis()
        val event1 = Event(currentTime - (rollingPeriod - 20000), BigDecimal.valueOf(1.0), 1)
        val event2 = Event(currentTime - (rollingPeriod - 10000), BigDecimal.valueOf(0.5), 2)
        // When
        calculator.addEventAndRemoveStaleEvents(event1, currentTime)
        calculator.addEventAndRemoveStaleEvents(event2, currentTime)
        val stats = calculator.getRollingAverageAndRemoveStaleEvents(currentTime)
        // Then
        assertEquals(2, stats.total)
        assertEquals(BigDecimal.valueOf(1.5), stats.sumX)
        assertEquals(BigDecimal.valueOf(0.75), stats.avgX)
        assertEquals(3, stats.sumY)
        assertEquals(BigDecimal.valueOf(1.5), stats.avgY)
    }

    @Test
    fun `getRollingAverageAndRemoveStaleEvents should remove stale events before calculating the RollingPeriodResult`() {
        // Given
        val rollingPeriod = 60000L
        val calculator = RollingStatsCalculator(rollingDurationMillis = rollingPeriod)
        val currentTime = System.currentTimeMillis()
        val event1 = Event(currentTime - (rollingPeriod - 20000), BigDecimal.valueOf(1.0), 1)
        val event2 = Event(currentTime - (rollingPeriod - 10000), BigDecimal.valueOf(0.5), 2)
        // When
        calculator.addEventAndRemoveStaleEvents(event1, currentTime)
        calculator.addEventAndRemoveStaleEvents(event2, currentTime)
        val stats = calculator.getRollingAverageAndRemoveStaleEvents(currentTime + 15000)
        // Then
        assertEquals(1, stats.total)
        assertEquals(BigDecimal.valueOf(1.0), stats.sumX)
        assertEquals(BigDecimal.valueOf(1.0), stats.avgX)
        assertEquals(1, stats.sumY)
        assertEquals(BigDecimal.valueOf(1), stats.avgY)
    }

    @Test
    fun `addEventAndRemoveStaleEvents should remove events older than the rolling period`() {
        // Given
        val rollingPeriod = 60000L
        val calculator = RollingStatsCalculator(rollingDurationMillis = rollingPeriod)
        val currentTime = System.currentTimeMillis()
        val oldEvents = listOf(
            Event(currentTime - 70000, BigDecimal.valueOf(1.0), 1),
            Event(currentTime - 70000, BigDecimal.valueOf(15.0), 12),
            Event(currentTime - 70000, BigDecimal.valueOf(150.0), 13)
        )
        val newEvent = Event(currentTime - 50000, BigDecimal.valueOf(0.5), 2)
        // When
        oldEvents.forEach { calculator.addEventAndRemoveStaleEvents(it, currentTime  - 70000) }
        val oldEventCount = calculator.events.size
        calculator.addEventAndRemoveStaleEvents(newEvent, currentTime)
        // Then
        assertEquals(oldEvents.size, oldEventCount)
        assertEquals(1, calculator.events.size)
        assertEquals(newEvent.x, calculator.sumX.get())
        assertEquals(newEvent.y, calculator.sumY.get())
        assertEquals(1, calculator.count.get())
    }

    @Test
    fun `addEventAndRemoveStaleEvents should be thread-safe`() {
        // Given
        val rollingPeriod = 1000L
        val calculator = RollingStatsCalculator(rollingDurationMillis = rollingPeriod)
        val currentTime = System.currentTimeMillis()
        val eventsSize = 1000
        val jobsSize = 100
        val expectedEvents = ConcurrentLinkedQueue<Event>()

        // When
        runBlocking {
            val jobs = List(jobsSize) { i ->
                launch(Dispatchers.Default) {
                    repeat(eventsSize) { j ->
                        val event = Event(currentTime - Random.nextLong(2000), (i * eventsSize + j).toBigDecimal(), (i * eventsSize + j).toLong())
                        calculator.addEventAndRemoveStaleEvents(event, currentTime)
                        if (currentTime - event.timestamp <= rollingPeriod) {
                            expectedEvents.add(event)
                        }
                    }
                }
            }

            jobs.forEach { it.join() }
        }

        val stats = calculator.getRollingAverageAndRemoveStaleEvents(currentTime)

        // Then
        val expectedSumX = expectedEvents.sumOf { it.x }
        val expectedSumY = BigDecimal.valueOf(expectedEvents.sumOf { it.y })
        val expectedEventsCount = expectedEvents.size
        assertEquals(expectedEventsCount, stats.total)
        assertEquals(expectedSumX, stats.sumX)
        assertEquals(expectedSumX.divide(expectedEventsCount.toBigDecimal(), MathContext.DECIMAL64), stats.avgX)
        assertEquals(expectedSumY.toLong(), stats.sumY)
        assertEquals(expectedSumY.divide(expectedEventsCount.toBigDecimal(), MathContext.DECIMAL64), stats.avgY)
    }

    @Test
    fun `getRollingAverageAndRemoveStaleEvents should not have floating point precision error`() {
        // Given
        val calculator = RollingStatsCalculator()
        val currentTime = System.currentTimeMillis()

        val event1 = Event.fromPlainTextString("${currentTime},0.1,1").filterIsInstance<EventParsingResult.Success>().first().event
        val event2 = Event.fromPlainTextString("${currentTime},0.2,2").filterIsInstance<EventParsingResult.Success>().first().event

        // When
        calculator.addEventAndRemoveStaleEvents(event1, currentTime)
        calculator.addEventAndRemoveStaleEvents(event2, currentTime)
        val result = calculator.getRollingAverageAndRemoveStaleEvents(currentTime)

        // Then
        val expectedSumX = "0.3".toBigDecimal()
        assertEquals(expectedSumX, result.sumX)
    }
}