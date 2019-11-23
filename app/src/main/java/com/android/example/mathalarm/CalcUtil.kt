package com.android.example.mathalarm

import com.android.example.mathalarm.screens.AlarmMathFragment
import java.util.*

class CalcUtil {

    private var op = 0
    private  var num1 = 0
    private  var num2 = 0
    private  var ans = 0



    //Creates the math problem based on the user-set difficulty
    private fun getMathProblem(difficulty: Int) {
        val random = Random()
        op = random.nextInt(4)
        val add1: Int
        val add2: Int
        val mult1: Int
        val mult2: Int
        when (difficulty) {
            Alarm.EASY -> {
                add1 = 90
                add2 = 10
                mult1 = 10
                mult2 = 3
            }
            Alarm.HARD -> {
                add1 = 9000
                add2 = 1000
                mult1 = 14
                mult2 = 12
            }
            else -> {
                add1 = 900
                add2 = 100
                mult1 = 13
                mult2 = 3
            }
        }
        when (op) {
            AlarmMathFragment.ADD -> {
                num1 = random.nextInt(add1) + add2
                num2 = random.nextInt(add1) + add2
                ans = num1 + num2
            }
            AlarmMathFragment.SUBTRACT -> {
                num1 = random.nextInt(add1) + add2
                num2 = random.nextInt(add1) + add2
                if (num1 < num2) {
                    val temp: Int = num1
                    num1 = num2
                    num2 = temp
                }
                ans = num1 - num2
            }
            AlarmMathFragment.TIMES -> {
                num1 = random.nextInt(mult1) + mult2
                num2 = random.nextInt(mult1) + mult2
                ans = num1 * num2
            }
            AlarmMathFragment.DIVIDE -> {
                num1 = random.nextInt(mult1) + mult2
                num2 = random.nextInt(mult1) + mult2
                ans = num1 * num2
                val tmp: Int = ans
                ans = num1
                num1 = tmp
            }
            else -> {
            }
        }
    }

    private fun getMathString(): String? {
        return when (op) {
            AlarmMathFragment.ADD -> "$num1 + $num2 = "
            AlarmMathFragment.SUBTRACT -> "$num1 - $num2 = "
            AlarmMathFragment.TIMES -> "$num1 x $num2 = "
            AlarmMathFragment.DIVIDE -> "$num1 / $num2 = "
            else -> null
        }
    }
}