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
        for (value in listOf("", "1.5", "१.२", "1 2", "12x", "1−2", "--1", "2147483648")) {
            assertNull(parseChallengeAnswer(value), value)
        }
    }
    @Test fun exactIntegerDecimalsAreAcceptedWithoutRounding() {
        for (value in listOf("30.0", "30.000", " ३०.० ", "３０.００", "+30.0")) {
            assertEquals(30, parseChallengeAnswer(value), value)
        }
        assertEquals(-12, parseChallengeAnswer("−१२.०"))
        assertEquals(Int.MAX_VALUE, parseChallengeAnswer("2147483647.0"))
        for (value in listOf("30.01", "30.", ".0", "30..0", "30,0", "3e1", "2147483648.0", "30.00000000000000001")) {
            assertNull(parseChallengeAnswer(value), value)
        }
    }

}
