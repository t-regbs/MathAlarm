package com.timilehinaregbesola.mathalarm.presentation.alarmmath

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class ChallengeAnswerTest {
    @Test fun nativeLocaleDigitsAndNegativeSignsHaveOneSharedMeaning() {
        for (value in listOf(" -12 ", "−१२", "−১২", "−੧੨", "－１２", "−١٢", "−۱۲")) {
            assertEquals(-12, parseChallengeAnswer(value), value)
        }
        assertEquals(12, parseChallengeAnswer("＋１２"))
        assertEquals(0, parseChallengeAnswer("০"))
    }
    @Test fun normalizationDoesNotAcceptFractionsMixedTextOrOverflow() {
        for (value in listOf("", "1.0", "१.२", "1 2", "12x", "1−2", "--1", "2147483648")) {
            assertNull(parseChallengeAnswer(value), value)
        }
    }
}
