package com.android.example.mathalarm.screens.alarmsettings

import android.app.Activity
import android.content.Intent
import android.media.RingtoneManager
import android.net.Uri
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.*
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import com.android.example.mathalarm.*
import com.android.example.mathalarm.database.Alarm
import com.android.example.mathalarm.database.AlarmDatabase
import com.android.example.mathalarm.databinding.FragmentAlarmSettingsBinding
import com.android.example.mathalarm.screens.TimePickerFragment
import java.util.*
import kotlin.collections.ArrayList


class AlarmSettingsFragment: Fragment() {

//    var mAlarm: Alarm? = null
    private lateinit var  binding: FragmentAlarmSettingsBinding

    private lateinit var alarmSettingsViewModel: AlarmSettingsViewModel

    var mTestAlarm: Alarm? = null

    var mAlarmTones: Array<Uri?> = emptyArray()

//    var mRepeat = ""

    private val REQUEST_TIME = 0
    private val DIALOG_TIME = "DialogTime"
    private val REQUEST_TEST = 1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {

        binding = DataBindingUtil.inflate(
            inflater,
            R.layout.fragment_alarm_settings, container, false)

        val application = requireNotNull(this.activity).application

        //Creating an instance of the ViewModel Factory
        val dataSource = AlarmDatabase.getInstance(application).alarmDatabaseDao
        val viewModelFactory =
            AlarmSettingsViewFactory(
                dataSource,
                application
            )

        alarmSettingsViewModel = ViewModelProviders.of(
            this, viewModelFactory).get(AlarmSettingsViewModel::class.java)

        binding.alarmSettingsViewModel = alarmSettingsViewModel


        val args = AlarmSettingsFragmentArgs.fromBundle(arguments!!)
        if (args.toString() == "add"){
//            alarmSettingsViewModel.createAlarm()
        }

//        mRepeat = mAlarm!!.repeatDays


        alarmSettingsViewModel.currentAlarm.observe(this, Observer {
            if (it != null){
                var mAlarm = alarmSettingsViewModel.currentAlarm.value!!
                var mRepeat = alarmSettingsViewModel.currentAlarm.value!!.repeatDays

                binding.settingsTime.text = getFormatTime(mAlarm)
                binding.settingsTime.setOnClickListener(View.OnClickListener {
                    val manager = fragmentManager
                    val dialog: TimePickerFragment = TimePickerFragment
                        .newInstance(alarmSettingsViewModel.currentAlarm.value!!.hour, alarmSettingsViewModel.currentAlarm.value!!.minute)
                    dialog.setTargetFragment(
                        this@AlarmSettingsFragment,
                        REQUEST_TIME
                    )
                    if (manager != null) {
                        dialog.show(manager, DIALOG_TIME)
                    }
                })

                binding.setRepeatSun.isChecked = mRepeat[SUN] == 'T'
                binding.setRepeatSun.setOnClickListener(View.OnClickListener {
                    binding.setRepeatSun.isChecked = mRepeat[SUN] != 'T'
                    val sb = StringBuilder(mRepeat)
                    if (mRepeat[SUN] == 'T') {
                        sb.setCharAt(SUN, 'F')
                        mRepeat = sb.toString()
                        mAlarm.repeatDays = mRepeat
                    } else {
                        sb.setCharAt(SUN, 'T')
                        mRepeat = sb.toString()
                        mAlarm.repeatDays = mRepeat
                    }
                })

                binding.setRepeatMon.setChecked(mRepeat.get(MON) == 'T')
                binding.setRepeatMon.setOnClickListener(View.OnClickListener {
                    binding.setRepeatMon.setChecked(mRepeat.get(MON) != 'T')
                    val sb = StringBuilder(mRepeat)
                    if (mRepeat.get(MON) == 'T') {
                        sb.setCharAt(MON, 'F')
                        mRepeat = sb.toString()
                        mAlarm.repeatDays = mRepeat
                    } else {
                        sb.setCharAt(MON, 'T')
                        mRepeat = sb.toString()
                        mAlarm.repeatDays = mRepeat
                    }
                })

                binding.setRepeatTue.setChecked(mRepeat.get(TUE) == 'T')
                binding.setRepeatTue.setOnClickListener(View.OnClickListener {
                    binding.setRepeatTue.setChecked(mRepeat.get(TUE) != 'T')
                    val sb = StringBuilder(mRepeat)
                    if (mRepeat.get(TUE) == 'T') {
                        sb.setCharAt(TUE, 'F')
                        mRepeat = sb.toString()
                        mAlarm.repeatDays = mRepeat
                    } else {
                        sb.setCharAt(MON, 'T')
                        mRepeat = sb.toString()
                        mAlarm.repeatDays = mRepeat
                    }
                })

                binding.setRepeatWed.setChecked(mRepeat.get(WED) == 'T')
                binding.setRepeatWed.setOnClickListener(View.OnClickListener {
                    binding.setRepeatWed.setChecked(mRepeat.get(WED) != 'T')
                    val sb = StringBuilder(mRepeat)
                    if (mRepeat.get(WED) == 'T') {
                        sb.setCharAt(WED, 'F')
                        mRepeat = sb.toString()
                        mAlarm.repeatDays = mRepeat
                    } else {
                        sb.setCharAt(WED, 'T')
                        mRepeat = sb.toString()
                        mAlarm.repeatDays = mRepeat
                    }
                })

                binding.setRepeatThu.setChecked(mRepeat.get(THU) == 'T')
                binding.setRepeatThu.setOnClickListener(View.OnClickListener {
                    binding.setRepeatMon.setChecked(mRepeat.get(THU) != 'T')
                    val sb = StringBuilder(mRepeat)
                    if (mRepeat.get(THU) == 'T') {
                        sb.setCharAt(THU, 'F')
                        mRepeat = sb.toString()
                        mAlarm.repeatDays = mRepeat
                    } else {
                        sb.setCharAt(THU, 'T')
                        mRepeat = sb.toString()
                        mAlarm.repeatDays = mRepeat
                    }
                })

                binding.setRepeatFri.setChecked(mRepeat.get(FRI) == 'T')
                binding.setRepeatFri.setOnClickListener(View.OnClickListener {
                    binding.setRepeatMon.setChecked(mRepeat.get(FRI) != 'T')
                    val sb = StringBuilder(mRepeat)
                    if (mRepeat.get(FRI) == 'T') {
                        sb.setCharAt(FRI, 'F')
                        mRepeat = sb.toString()
                        mAlarm.repeatDays = mRepeat
                    } else {
                        sb.setCharAt(FRI, 'T')
                        mRepeat = sb.toString()
                        mAlarm.repeatDays = mRepeat
                    }
                })

                binding.setRepeatSat.setChecked(mRepeat.get(SAT) == 'T')
                binding.setRepeatSat.setOnClickListener(View.OnClickListener {
                    binding.setRepeatMon.setChecked(mRepeat.get(SAT) != 'T')
                    val sb = StringBuilder(mRepeat)
                    if (mRepeat.get(SAT) == 'T') {
                        sb.setCharAt(SAT, 'F')
                        mRepeat = sb.toString()
                        mAlarm.repeatDays = mRepeat
                    } else {
                        sb.setCharAt(SAT, 'T')
                        mRepeat = sb.toString()
                        mAlarm.repeatDays = mRepeat
                    }
                })

                binding.settingsRepeatSwitch.setChecked(mAlarm.repeat)
                binding.settingsRepeatSwitch.setOnClickListener(View.OnClickListener {
                    binding.settingsRepeatSwitch.setChecked(!mAlarm.repeat)
                    mAlarm.repeat = (!mAlarm.repeat)
                })

                //Getting system sound files for tone and displaying in spinner
                //Getting system sound files for tone and displaying in spinner
                val toneItems: MutableList<String> =
                    ArrayList()
                val ringtoneMgr = RingtoneManager(activity)
                ringtoneMgr.setType(RingtoneManager.TYPE_ALARM)
                var alarmsCursor = ringtoneMgr.cursor
                var alarmsCount = alarmsCursor.count

                if (alarmsCount == 0) { //if there are no alarms, use notification sounds
                    ringtoneMgr.setType(RingtoneManager.TYPE_NOTIFICATION)
                    alarmsCursor = ringtoneMgr.cursor
                    alarmsCount = alarmsCursor.count
                    if (alarmsCount == 0) { //if no alarms and notification sounds, finally use ringtones
                        ringtoneMgr.setType(RingtoneManager.TYPE_RINGTONE)
                        alarmsCursor = ringtoneMgr.cursor
                        alarmsCount = alarmsCursor.count
                    }
                }

                if (alarmsCount == 0 && !alarmsCursor.moveToFirst()) {
                    Toast.makeText(activity, "No sound files available", Toast.LENGTH_SHORT).show()
                }

                var previousPosition = 0

                //If there are sound files, add them
                //If there are sound files, add them
                if (alarmsCount != 0) {
                    mAlarmTones = arrayOfNulls<Uri>(alarmsCount)
                    val currentTone: String = mAlarm.alarmTone
                    while (!alarmsCursor.isAfterLast && alarmsCursor.moveToNext()) {
                        val currentPosition = alarmsCursor.position
                        mAlarmTones[currentPosition] = ringtoneMgr.getRingtoneUri(currentPosition)
                        toneItems.add(
                            ringtoneMgr.getRingtone(currentPosition)
                                .getTitle(activity)
                        )
                        if (currentTone == mAlarmTones[currentPosition].toString()) {
                            previousPosition = currentPosition
                        }
                    }
                }

                if (toneItems.isEmpty()) {
                    toneItems.add("Empty")
                }


                val toneAdapter = ArrayAdapter(
                    activity!!,
                    android.R.layout.simple_spinner_dropdown_item, toneItems
                )
                binding.settingsToneSpinner.adapter = toneAdapter
                binding.settingsToneSpinner.setSelection(previousPosition)

                val difficultyItems =
                    arrayOf("Easy", "Medium", "Hard")
                val difficultyAdapter = ArrayAdapter(
                    activity!!,
                    android.R.layout.simple_spinner_dropdown_item, difficultyItems
                )
                binding.settingsMathDifficultySpinner.adapter = difficultyAdapter
                binding.settingsMathDifficultySpinner.setSelection(mAlarm.difficulty)

                binding.settingsSnoozeText.setText(java.lang.String.format(Locale.US, "%d", mAlarm.snooze))
                binding.settingsSnoozeText.addTextChangedListener(object : TextWatcher {
                    override fun beforeTextChanged(
                        s: CharSequence,
                        start: Int,
                        count: Int,
                        after: Int
                    ) {
                    }

                    override fun onTextChanged(
                        s: CharSequence,
                        start: Int,
                        before: Int,
                        count: Int
                    ) {
                        if (s.isNotEmpty()) {
                            mAlarm.snooze = (s.toString().toInt())
                        }
                    }

                    override fun afterTextChanged(s: Editable) {}
                })

                binding.settingsVibrateSwitch.isChecked = mAlarm.vibrate
                binding.settingsVibrateSwitch.setOnClickListener(View.OnClickListener {
                    binding.settingsVibrateSwitch.isChecked = !mAlarm.vibrate
                    mAlarm.vibrate = (!mAlarm.vibrate)
                })


            }
        })

        binding.settingsTestButton.setOnClickListener(View.OnClickListener {
            mTestAlarm = Alarm()
            mTestAlarm!!.difficulty = binding.settingsMathDifficultySpinner.selectedItemPosition
            if (mAlarmTones.isNotEmpty()) {
                mTestAlarm!!.alarmTone = (
                    mAlarmTones[binding.settingsToneSpinner
                        .selectedItemPosition].toString()
                )
            }
            mTestAlarm!!.vibrate = binding.settingsVibrateSwitch.isChecked
            mTestAlarm!!.snooze = 0
            alarmSettingsViewModel.onAdd(mTestAlarm!!)
//            AlarmViewModel.get(getActivity()).addAlarm(mTestAlarm);
//            val test = Intent(activity, AlarmMathActivity::class.java)
//            test.putExtra(Alarm.ALARM_EXTRA, mTestAlarm.getId())
//            startActivityForResult(test, AlarmSettingsFragment.REQUEST_TEST)
        })

        return binding.root
    }

//    fun scheduleAndMessage() { //schedule it and create a toast
//        if (alarmSettingsViewModel.currentAlarm.value!!.scheduleAlarm(activity)) {
//            Toast.makeText(
//                activity, mAlarm.getTimeLeftMessage(activity),
//                Toast.LENGTH_SHORT
//            ).show()
//            mAlarm.setIsOn(true)
//        } else {
//            mAlarm.setIsOn(false)
//        }
//    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (resultCode != Activity.RESULT_OK) {
            return
        }
        if (requestCode == REQUEST_TIME) {
            val hour = data?.getIntExtra(TimePickerFragment.EXTRA_HOUR, 0)
            val min = data?.getIntExtra(TimePickerFragment.EXTRA_MIN, 0)
            if (hour != null) {
                alarmSettingsViewModel.currentAlarm.value!!.hour = hour
            }
            if (min != null) {
                alarmSettingsViewModel.currentAlarm.value!!.minute = min
            }
            binding.settingsTime.text = getFormatTime(alarmSettingsViewModel.currentAlarm.value!!)
        } else {
            if (requestCode == REQUEST_TEST) {
//                alarmSettingsViewModel.onDelete(mTestAlarm)
                //AlarmViewModel.get(getActivity()).deleteAlarm(mTestAlarm);
            }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)
        inflater.inflate(R.menu.alarm_settings_menu, menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.fragment_settings_done -> {
                //Setting difficulty + alarm tone
                alarmSettingsViewModel.currentAlarm.value!!.difficulty =(binding.settingsMathDifficultySpinner.selectedItemPosition)
                if (mAlarmTones.isNotEmpty()) {
                    alarmSettingsViewModel.currentAlarm.value!!.alarmTone = (
                        mAlarmTones[binding.settingsToneSpinner
                            .selectedItemPosition].toString()
                    )
                }
                alarmSettingsViewModel.onUpdate(alarmSettingsViewModel.currentAlarm.value!!)
                //schedule alarm, update to database and close settings
//                if (mAdd) {
//                    scheduleAndMessage()
//                    alarmViewModel.addAlarm(mAlarm)
//                    //AlarmViewModel.get(getActivity()).addAlarm(mAlarm);
//                } else { // Alarm oldAlarm = AlarmViewModel.get(getActivity()).getAlarm(mAlarm.getId());
//                    alarmViewModel.getAlarm(mAlarm.getId())
//                    alarmViewModel.getAlarmResult().observe(
//                        viewLifecycleOwner,
//                        Observer<Alarm> { alarm ->
//                            //                            Alarm oldAlarm = alarm;
//                            if (alarm.isOn()) {
//                                alarm.cancelAlarm(activity)
//                            }
//                            scheduleAndMessage()
//                            alarmViewModel.updateAlarm(mAlarm)
//                            //AlarmViewModel.get(getActivity()).updateAlarm(mAlarm);
//                        })
//                    //                    Alarm oldAlarm = AlarmRepository.getInstance(getActivity()).getAlarm(mAlarm.getId());
//                }
                activity!!.finish()
                true
            }
            R.id.fragment_settings_delete -> {
                if (alarmSettingsViewModel.currentAlarm.value!!.isOn) {
                    alarmSettingsViewModel.cancelAlarm(context)
                }
                alarmSettingsViewModel.onDelete(alarmSettingsViewModel.currentAlarm.value!!)
                //AlarmViewModel.get(getActivity()).deleteAlarm(mAlarm);

                activity!!.finish()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }
}