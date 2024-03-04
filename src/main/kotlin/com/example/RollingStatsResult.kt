package com.example

import java.math.BigDecimal
import java.math.RoundingMode

data class RollingStatsResult(val total: Int, val sumX: BigDecimal, val avgX: BigDecimal, val sumY: Long, val avgY: BigDecimal) {
    private fun BigDecimal.setScaleWithRounding(scale: Int = 10): BigDecimal = this.setScale(scale, RoundingMode.HALF_UP)

    fun toPlainTextString(): String = "${total},${sumX.setScaleWithRounding().toPlainString()},${avgX.setScaleWithRounding().toPlainString()},${sumY},${avgY.setScaleWithRounding(3).toPlainString()}"
}
