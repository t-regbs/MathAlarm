package com.timilehinaregbesola.mathalarm.platform

import platform.Foundation.*

actual fun formatAlarmDate(value: String, languageTag: String): String {
    val parser = NSDateFormatter().apply {
        locale = NSLocale(localeIdentifier = "en_US_POSIX")
        timeZone = NSTimeZone.timeZoneForSecondsFromGMT(0)
        dateFormat = "yyyy-MM-dd"
    }
    val date = parser.dateFromString(value) ?: return value
    return NSDateFormatter().apply {
        locale = NSLocale(localeIdentifier = languageTag)
        timeZone = NSTimeZone.timeZoneForSecondsFromGMT(0)
        dateStyle = NSDateFormatterMediumStyle
        timeStyle = NSDateFormatterNoStyle
    }.stringFromDate(date)
}

actual fun formatAlarmWeekday(sundayIndex: Int, languageTag: String, abbreviated: Boolean): String {
    val formatter = NSDateFormatter().apply { locale = NSLocale(localeIdentifier = languageTag) }
    val names = if (abbreviated) formatter.shortWeekdaySymbols else formatter.weekdaySymbols
    return names[sundayIndex] as String
}
