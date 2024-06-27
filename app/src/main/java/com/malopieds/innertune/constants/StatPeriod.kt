package com.malopieds.innertune.constants

import java.time.LocalDateTime
import java.time.ZoneOffset

enum class StatPeriod {
    `1_WEEK`, `1_MONTH`, `3_MONTH`, `6_MONTH`, `1_YEAR`, ALL;

    fun toTimeMillis(): Long =
        when (this) {
            `1_WEEK` -> LocalDateTime.now().minusWeeks(1).toInstant(ZoneOffset.UTC).toEpochMilli()
            `1_MONTH` -> LocalDateTime.now().minusMonths(1).toInstant(ZoneOffset.UTC).toEpochMilli()
            `3_MONTH` -> LocalDateTime.now().minusMonths(3).toInstant(ZoneOffset.UTC).toEpochMilli()
            `6_MONTH` -> LocalDateTime.now().minusMonths(6).toInstant(ZoneOffset.UTC).toEpochMilli()
            `1_YEAR` -> LocalDateTime.now().minusMonths(12).toInstant(ZoneOffset.UTC).toEpochMilli()
            ALL -> 0
        }
}