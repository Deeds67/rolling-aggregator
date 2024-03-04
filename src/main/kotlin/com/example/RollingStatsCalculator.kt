package com.example

import java.math.BigDecimal
import java.math.MathContext
import java.math.RoundingMode
import java.util.concurrent.PriorityBlockingQueue
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicLong
import java.util.concurrent.atomic.AtomicReference
import java.util.concurrent.locks.ReentrantReadWriteLock
import kotlin.concurrent.read
import kotlin.concurrent.write

/*
 * RollingStatsCalculator is a class that calculates the rolling (total, sum, average) of `Events` in a thread-safe way.
 * @property rollingDurationMillis The duration of the rolling period in milliseconds.
 */
class RollingStatsCalculator(private val rollingDurationMillis: Long = 60000) {
    internal val events = PriorityBlockingQueue<Event>(100, compareBy { it.timestamp })
    internal val sumX = AtomicReference(BigDecimal.ZERO)
    internal val sumY = AtomicLong(0)
    internal val count = AtomicInteger(0)
    private val lock = ReentrantReadWriteLock()

    fun addEventAndRemoveStaleEvents(event: Event, currentTime: Long) {
        addEventsAndRemoveStaleEvents(listOf(event), currentTime)
    }

    fun addEventsAndRemoveStaleEvents(events: List<Event>, currentTime: Long) {
        // Read lock allows for multiple threads to add events concurrently
        lock.read {
            events.map { addEvent(it, currentTime) }
        }
        // Write lock on removing stale events, to avoid race conditions.
        lock.write {
            removeStaleEvents(currentTime)
        }
    }

    fun getRollingAverageAndRemoveStaleEvents(currentTime: Long): RollingStatsResult =
        // Write lock to avoid race conditions when removing stale events,
        // as well as to avoid new events being added while the rolling average is being calculated.
        lock.write {
            removeStaleEvents(currentTime)
            getRollingAverage()
        }

    private fun addEvent(event: Event, currentTime: Long) {
        if (currentTime - event.timestamp <= rollingDurationMillis) {
            events.add(event)
            sumX.updateAndGet { it.add(event.x) }
            sumY.addAndGet(event.y)
            count.incrementAndGet()
        }
    }

    private fun getRollingAverage(): RollingStatsResult {
        val total = count.get()
        val sumXValue = sumX.get()
        val sumYValue = sumY.get()
        if (total > 0) {
            val avgX = sumXValue.divide(total.toBigDecimal(), MathContext.DECIMAL64)
            val avgY = BigDecimal.valueOf(sumYValue).divide(total.toBigDecimal(), MathContext.DECIMAL64)
            return RollingStatsResult(total, sumXValue, avgX, sumYValue, avgY)
        }

        throw EmptyPeriodError
    }

    private fun removeStaleEvents(currentTime: Long) {
        while (events.isNotEmpty() && currentTime - events.peek().timestamp > rollingDurationMillis) {
            val event = events.poll()
            sumX.updateAndGet { it.subtract(event.x) }
            sumY.addAndGet(-event.y)
            count.decrementAndGet()
        }
    }
}