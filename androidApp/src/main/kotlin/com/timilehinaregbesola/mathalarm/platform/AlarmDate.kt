package com.timilehinaregbesola.mathalarm.platform

import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale

fun formatAlarmDate(value: String, languageTag: String): String = runCatching {
    LocalDate.parse(value).format(DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM)
        .withLocale(Locale.forLanguageTag(languageTag)))
}.getOrDefault(value)

fun formatAlarmWeekday(sundayIndex: Int, languageTag: String, abbreviated: Boolean = true): String =
    java.time.DayOfWeek.of(if (sundayIndex == 0) 7 else sundayIndex)
        .getDisplayName(
            if (abbreviated) java.time.format.TextStyle.SHORT else java.time.format.TextStyle.FULL,
            Locale.forLanguageTag(languageTag),
        )
