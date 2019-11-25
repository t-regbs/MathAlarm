package com.android.example.mathalarm.screens

import android.app.Activity
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import com.android.example.mathalarm.R
import com.android.example.mathalarm.databinding.FragmentAlarmMathBinding

class AlarmMathFragment: Fragment() {

    private lateinit var binding: FragmentAlarmMathBinding

    private var sb: StringBuilder? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {

        binding = DataBindingUtil.inflate(
            inflater,
            R.layout.fragment_alarm_math, container, false)
        return binding.root
    }

    fun showCalc() {
        //Initialize the buttons and the on click actions
        sb = StringBuilder("")
//        binding.mathQuestion.setText(getMathString())
        binding.mathBtn1.setOnClickListener(View.OnClickListener {
            sb!!.append(1)
            binding.mathAnswer.text = sb
        })
        binding.mathBtn2.setOnClickListener(View.OnClickListener {
            sb!!.append(2)
            binding.mathAnswer.text = sb
        })
        binding.mathBtn3.setOnClickListener(View.OnClickListener {
            sb!!.append(3)
            binding.mathAnswer.text = sb
        })
        binding.mathBtn4.setOnClickListener(View.OnClickListener {
            sb!!.append(4)
            binding.mathAnswer.text = sb
        })
        binding.mathBtn5.setOnClickListener(View.OnClickListener {
            sb!!.append(5)
            binding.mathAnswer.text = sb
        })
        binding.mathBtn6.setOnClickListener(View.OnClickListener {
            sb!!.append(6)
            binding.mathAnswer.text = sb
        })
        binding.mathBtn7.setOnClickListener(View.OnClickListener {
            sb!!.append(7)
            binding.mathAnswer.text = sb
        })
        binding.mathBtn8.setOnClickListener(View.OnClickListener {
            sb!!.append(8)
            binding.mathAnswer.text = sb
        })
        binding.mathBtn9.setOnClickListener(View.OnClickListener {
            sb!!.append(9)
            binding.mathAnswer.text = sb
        })
        binding.mathBtn0.setOnClickListener(View.OnClickListener {
            sb!!.append(0)
            binding.mathAnswer.text = sb
        })
        binding.mathBtnDel.setOnClickListener(View.OnClickListener {
            if (sb!!.isNotEmpty()) {
                sb!!.deleteCharAt(sb!!.length - 1)
                binding.mathAnswer.text = sb
            }
        })
        binding.mathBtnClear.setOnClickListener(View.OnClickListener {
            sb!!.delete(0, sb!!.length)
            binding.mathAnswer.setText(sb)
        })
//        binding.mathBtnSet.setOnClickListener(View.OnClickListener {
//            if (sb.toString().toInt() != ans) {
//                Toast.makeText(activity, "Incorrect!", Toast.LENGTH_SHORT).show()
//                sb!!.setLength(0)
//                binding.mathAnswer.text = ""
//            } else {
//                mp.stop()
//                vibrateRunning = false
//                activity!!.setResult(Activity.RESULT_OK)
//                activity!!.finish()
//            }
//        })
//        binding.mathBtnSnooze.setOnClickListener(View.OnClickListener {
//            if (alarm.getSnooze() === 0) {
//                Toast.makeText(
//                    activity,
//                    getString(R.string.snooze_off), Toast.LENGTH_SHORT
//                ).show()
//            } else {
//                mp.stop()
//                vibrateRunning = false
//                alarm.scheduleSnooze(activity)
//                activity!!.finish()
//            }
//        })
    }

}