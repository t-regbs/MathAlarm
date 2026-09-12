package com.timilehinaregbesola.mathalarm.utils.strings

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ChallengeStringsTest {
    @Test
    fun englishCountsUseSingularOnlyForOne() {
        assertEquals("0 questions", EnMathAlarmStrings.questionCount(0))
        assertEquals("1 question", EnMathAlarmStrings.questionCount(1))
        (2..10).forEach { count ->
            assertEquals("$count questions", EnMathAlarmStrings.questionCount(count))
        }
        assertEquals("1 question total · Maximum 10", EnMathAlarmStrings.mixedQuestionTotal(1, 10))
        assertEquals("Mixed difficulty · 2 questions", EnMathAlarmStrings.challengeSummary("Mixed difficulty", 2))
    }

    @Test
    fun russianCountsHandleOneFewManyAndTeenExceptions() {
        mapOf(
            0 to "вопросов", 1 to "вопрос", 2 to "вопроса", 4 to "вопроса", 5 to "вопросов",
            10 to "вопросов", 11 to "вопросов", 12 to "вопросов", 14 to "вопросов",
            21 to "вопрос", 22 to "вопроса", 25 to "вопросов",
        ).forEach { (count, noun) ->
            assertEquals("$count $noun", RuMathAlarmStrings.questionCount(count))
            assertEquals("Всего: $count $noun · Максимум: 10", RuMathAlarmStrings.mixedQuestionTotal(count, 10))
        }
    }

    @Test
    fun progressUsesLocaleWordOrderAndDoesNotPluralizeAnOrdinal() {
        assertEquals("Question 1 of 1", EnMathAlarmStrings.questionProgress(1, 1))
        assertEquals("第2题，共10题", ZhMathAlarmStrings.questionProgress(2, 10))
        assertEquals("10 में से सवाल 2", HiMathAlarmStrings.questionProgress(2, 10))
        assertEquals("1道题", ZhMathAlarmStrings.questionCount(1))
        assertEquals("10道题", ZhMathAlarmStrings.questionCount(10))
    }

    @Test
    fun everyLocaleSupportsAllAllowedQuestionCountsAndTheConfiguredLimit() {
        val locales = listOf(EnMathAlarmStrings, DeMathAlarmStrings, EsMathAlarmStrings,
            PtMathAlarmStrings, RuMathAlarmStrings, HiMathAlarmStrings, PaMathAlarmStrings,
            BnMathAlarmStrings, ZhMathAlarmStrings)
        locales.forEach { text ->
            assertEquals(4, text.mathDifficultyNames.size)
            (1..10).forEach { count ->
                assertTrue(text.challengeSummary(text.mathDifficultyNames[0], count).contains(text.questionCount(count)))
                assertTrue(text.mixedQuestionTotal(count, 10).contains(text.questionCount(count)))
            }
            assertTrue(text.questionCountHint(10).contains("10"))
            assertTrue(text.challengeAnnouncementDescription(10).contains("10"))
        }
    }
}
