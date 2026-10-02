package com.timilehinaregbesola.mathalarm.presentation.alarmmath

/** Native keyboards pass raw text; integer normalization and validation stay shared. */
internal fun parseChallengeAnswer(value: String): Int? {
    val normalized = buildString {
        for (character in value.trim()) {
            val digit = when (character) {
                in '0'..'9' -> character - '0'
                in '٠'..'٩' -> character - '٠'
                in '۰'..'۹' -> character - '۰'
                in '०'..'९' -> character - '०'
                in '০'..'৯' -> character - '০'
                in '੦'..'੯' -> character - '੦'
                in '０'..'９' -> character - '０'
                else -> null
            }
            append(if (digit != null) ('0'.code + digit).toChar() else when (character) {
                '−', '－' -> '-'
                '＋' -> '+'
                else -> character
            })
        }
    }
    return normalized.toIntOrNull()
}
