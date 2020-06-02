package com.android.example.mathalarm.screens

import android.app.Activity
import android.app.Dialog
import android.content.DialogInterface
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.TimePicker
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import com.android.example.mathalarm.R

class TimePickerFragment : DialogFragment() {
    private var mTimePicker: TimePicker? = null
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val argHour = requireArguments().getInt(ARG_HOUR)
        val argMin = requireArguments().getInt(ARG_MIN)
        val v: View =
            LayoutInflater.from(activity).inflate(R.layout.dialog_time, null)
        mTimePicker = v.findViewById(R.id.dialog_time_time_picker)
        if (Build.VERSION.SDK_INT < 23) {
            mTimePicker!!.currentHour = argHour
            mTimePicker!!.currentMinute = argMin
        } else {
            mTimePicker!!.hour = argHour
            mTimePicker!!.minute = argMin
        }
        return AlertDialog.Builder(requireActivity())
            .setView(v)
            .setTitle(R.string.time_picker_title).setPositiveButton(
                getString(R.string.ok)
            ) { _, _ ->
                val hour: Int
                val minute: Int
                if (Build.VERSION.SDK_INT < 23) {
                    hour = mTimePicker!!.currentHour
                    minute = mTimePicker!!.currentMinute
                } else {
                    hour = mTimePicker!!.hour
                    minute = mTimePicker!!.minute
                }
                sendResult(Activity.RESULT_OK, hour, minute)
            }
            .create()
    }

    private fun sendResult(resultCode: Int, hour: Int, min: Int) {
        if (targetFragment == null) {
            return
        }
        val intent = Intent()
        intent.putExtra(EXTRA_HOUR, hour)
        intent.putExtra(EXTRA_MIN, min)
        targetFragment!!.onActivityResult(targetRequestCode, resultCode, intent)
    }

    companion object {
        const val EXTRA_HOUR = "timilehin.com.chopalarm.hour"
        const val EXTRA_MIN = "timilehin.com.chopalarm.minute"
        private const val ARG_HOUR = "hour"
        private const val ARG_MIN = "min"
        fun newInstance(hour: Int, minute: Int): TimePickerFragment {
            val args = Bundle()
            args.putInt(ARG_HOUR, hour)
            args.putInt(ARG_MIN, minute)
            val fragment = TimePickerFragment()
            fragment.arguments = args
            return fragment
        }
    }
}

