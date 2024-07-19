package com.moxxaxx.music.constants

import java.time.LocalDateTime
import java.time.ZoneOffset

enum class StatPeriod {
    WEEK_1,
    MONTH_1,
    MONTH_3,
    MONTH_6,
    YEAR_1,
    ALL,
    ;

    fun toTimeMillis(): Long =
        when (this) {
            WEEK_1 ->
                LocalDateTime
                    .now()
                    .minusWeeks(1)
                    .toInstant(ZoneOffset.UTC)
                    .toEpochMilli()
            MONTH_1 ->
                LocalDateTime
                    .now()
                    .minusMonths(1)
                    .toInstant(ZoneOffset.UTC)
                    .toEpochMilli()
            MONTH_3 ->
                LocalDateTime
                    .now()
                    .minusMonths(3)
                    .toInstant(ZoneOffset.UTC)
                    .toEpochMilli()
            MONTH_6 ->
                LocalDateTime
                    .now()
                    .minusMonths(6)
                    .toInstant(ZoneOffset.UTC)
                    .toEpochMilli()
            YEAR_1 ->
                LocalDateTime
                    .now()
                    .minusMonths(12)
                    .toInstant(ZoneOffset.UTC)
                    .toEpochMilli()
            ALL -> 0
        }
}
