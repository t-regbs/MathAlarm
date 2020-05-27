package com.android.example.mathalarm.screens.alarmmath

import android.app.Activity
import android.content.Context
import android.media.AudioManager
import android.media.MediaPlayer
import android.net.Uri
import android.os.Bundle
import android.os.Vibrator
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.Toast
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProviders
import androidx.navigation.fragment.findNavController
import com.android.example.mathalarm.*
import com.android.example.mathalarm.database.AlarmDatabase
import com.android.example.mathalarm.databinding.FragmentAlarmMathBinding
import java.io.IOException
import java.util.*

class AlarmMathFragment: Fragment() {

    companion object {
        private const val ADD = 0
        private const val SUBTRACT = 1
        private const val TIMES = 2
        private const val DIVIDE = 3
        fun newInstance(): AlarmMathFragment{
            return AlarmMathFragment()
        }
    }
    private lateinit var binding: FragmentAlarmMathBinding
    private lateinit var alarmMathViewModel: AlarmMathViewModel

    private var sb: StringBuilder? = null
    private val mp = MediaPlayer()
    private var vibrateRunning = false

    private var op = 0
    private  var num1 = 0
    private  var num2 = 0
    private var ans = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requireActivity().window.addFlags(
            WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED
                    or WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD
                    or WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
                    or WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
                    or WindowManager.LayoutParams.FLAG_ALLOW_LOCK_WHILE_SCREEN_ON
        )
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {

        binding = DataBindingUtil.inflate(
            inflater,
            R.layout.fragment_alarm_math, container, false)

        val intent = requireActivity().intent
        val extra = intent.extras

        val application = requireNotNull(this.activity).application

        val alarmId: Long = extra!![ALARM_EXTRA] as Long

        val dataSource = AlarmDatabase.getInstance(application).alarmDatabaseDao

        val viewModelFactory = AlarmMathViewFactory(alarmId, dataSource)

        alarmMathViewModel = ViewModelProvider(
            this, viewModelFactory).get(AlarmMathViewModel::class.java)
        binding.alarmMathViewModel = alarmMathViewModel


        alarmMathViewModel.alarm.observe(viewLifecycleOwner, Observer{ alarm ->
            if (alarm != null) {
                val dayOfTheWeek = getDayOfWeek(
                    Calendar.getInstance()[Calendar.DAY_OF_WEEK]
                )
                if (!alarm.repeat) {
                    val repeat = StringBuilder(alarm.repeatDays)
                    repeat.setCharAt(dayOfTheWeek, 'F')
                    alarm.repeatDays = repeat.toString()
                    if (alarm.repeatDays == "FFFFFFF") {
                        alarm.isOn = false
                    }
//                alarmMathViewModel.onUpdate(alarm)
                }

                //Play alarm tone
                if (alarm.alarmTone.isNotEmpty()) {
                    val alarmUri = Uri.parse(alarm.alarmTone)
                    try {
                        mp.reset()
                        mp.setDataSource(requireContext(), alarmUri)
                        mp.setAudioStreamType(AudioManager.STREAM_MUSIC)
                        mp.prepare()
                        mp.isLooping = true
                        mp.start()
                    } catch (e: IOException) {
                        e.printStackTrace()
                    }
                } else {
                    Toast.makeText(
                        activity, getString(R.string.tone_not_available),
                        Toast.LENGTH_SHORT
                    ).show()
                }

                //Vibrate phone
                if (alarm.vibrate) {
                    vibrateRunning = true
                    val thread = Thread(Runnable {
                        while (vibrateRunning) {
                            val v =
                                requireActivity().getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
                            v.vibrate(1000)
                            try {
                                Thread.sleep(5000)
                            } catch (e: InterruptedException) {
                            }
                        }
                        if (!vibrateRunning) {
                            return@Runnable
                        }
                    })
                    thread.start()
                }

                //Get difficulty
                getMathProblem(alarm.difficulty)

                //Initialize the buttons and the on click actions
                sb = StringBuilder("")
                binding.mathQuestion.text = getMathString()
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
                    binding.mathAnswer.text = sb
                })
                binding.mathBtnSet.setOnClickListener(View.OnClickListener {
                    if (sb.toString().toInt() != ans) {
                        Toast.makeText(activity, "Incorrect!", Toast.LENGTH_SHORT).show()
                        sb!!.setLength(0)
                        binding.mathAnswer.text = ""
                    } else {
                        mp.stop()
                        vibrateRunning = false
                        requireActivity().setResult(Activity.RESULT_OK)
                        requireActivity().finish()
                    }
                })
                binding.mathBtnSnooze.setOnClickListener(View.OnClickListener {
                    if (alarm.snooze == 0) {
                        Toast.makeText(
                            activity,
                            getString(R.string.snooze_off), Toast.LENGTH_SHORT
                        ).show()
                    } else {
                        mp.stop()
                        vibrateRunning = false
                        scheduleSnooze(requireContext(), alarm)
                        requireActivity().finish()
                    }
                })
            }
        })
        return binding.root
    }

    //Creates the math problem based on the user-set difficulty
    private fun getMathProblem(difficulty: Int) {
        val random = Random()
        op = random.nextInt(4)
        val add1: Int
        val add2: Int
        val mult1: Int
        val mult2: Int
        when (difficulty) {
            EASY -> {
                add1 = 90
                add2 = 10
                mult1 = 10
                mult2 = 3
            }
            HARD -> {
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
            ADD -> {
                num1 = random.nextInt(add1) + add2
                num2 = random.nextInt(add1) + add2
                ans = num1 + num2
            }
            SUBTRACT -> {
                num1 = random.nextInt(add1) + add2
                num2 = random.nextInt(add1) + add2
                if (num1 < num2) {
                    val temp: Int = num1
                    num1 = num2
                    num2 = temp
                }
                ans = num1 - num2
            }
            TIMES -> {
                num1 = random.nextInt(mult1) + mult2
                num2 = random.nextInt(mult1) + mult2
                ans = num1 * num2
            }
            DIVIDE -> {
                num1 = random.nextInt(mult1) + mult2
                num2 = random.nextInt(mult1) + mult2
                ans = num1 * num2
                val tmp: Int = ans
                ans = num1
                num1 = tmp
            }
        }
    }

    private fun getMathString(): String? {
        return when (op) {
            ADD -> "$num1 + $num2 = "
            SUBTRACT -> "$num1 - $num2 = "
            TIMES -> "$num1 x $num2 = "
            DIVIDE -> "$num1 / $num2 = "
            else -> null
        }
    }
}