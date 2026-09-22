package com.timilehinaregbesola.mathalarm.platform

/** Format a local calendar date without applying an instant/timezone conversion. */
expect fun formatAlarmDate(value: String, languageTag: String): String

expect fun formatAlarmWeekday(sundayIndex: Int, languageTag: String, abbreviated: Boolean = true): String
