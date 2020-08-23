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
import androidx.fragment.app.*
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.android.example.mathalarm.*
import com.android.example.mathalarm.database.Alarm
import com.android.example.mathalarm.database.AlarmDatabase
import com.android.example.mathalarm.databinding.FragmentAlarmSettingsBinding
import com.android.example.mathalarm.screens.TimePickerFragment
import java.util.*
import kotlin.collections.ArrayList


class AlarmSettingsFragment: Fragment() {

    private lateinit var  binding: FragmentAlarmSettingsBinding

    private lateinit var alarmSettingsViewModel: AlarmSettingsViewModel

    private lateinit var mAlarm: Alarm

    private val args: AlarmSettingsFragmentArgs by navArgs()
    var key: Long? = null

    private var isFromAdd: Boolean? = null

    var mTestAlarm: Alarm? = null

    var mAlarmTones: Array<Uri?> = emptyArray()

    private val REQUEST_TIME = 0
    private val DIALOG_TIME = "DialogTime"
    private val REQUEST_TEST = 1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)

        setFragmentResultListener("request_key") { requestKey: String, bundle: Bundle ->
            if(bundle.getString(ALARM_EXTRA) == "test"){
                alarmSettingsViewModel.onDelete(alarmSettingsViewModel.latestAlarm.value!!)
            }
        }
    }

    override fun onStart() {
        super.onStart()
//        alarmSettingsViewModel.getAlarm(key!!)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {

        binding = FragmentAlarmSettingsBinding.inflate(inflater, container, false)

        val application = requireNotNull(this.activity).application

        isFromAdd = args.add
        key = args.alarmKey

        //Creating an instance of the ViewModel Factory
        val dataSource = AlarmDatabase.getInstance(application).alarmDatabaseDao
        val viewModelFactory = AlarmSettingsViewFactory(args.alarmKey,dataSource)

        alarmSettingsViewModel = ViewModelProvider(
            this, viewModelFactory).get(AlarmSettingsViewModel::class.java)

        binding.alarmSettingsViewModel = alarmSettingsViewModel


        alarmSettingsViewModel.navigateToAlarmMath.observe(viewLifecycleOwner, Observer { alarmId ->
            alarmId?.let {
//                val test = Intent(activity, AlarmMathActivity::class.java)
//                test.putExtra(ALARM_EXTRA, alarm.toString())
//                startActivityForResult(test, REQUEST_TEST)
                val result = Bundle().apply {
                    putString(ALARM_EXTRA, "test")
                }
                findNavController().navigate(AlarmSettingsFragmentDirections.
                actionAlarmSettingsFragmentToAlarmMathFragment(alarmId.toString()))
                setFragmentResult("request_key", result)
                alarmSettingsViewModel.onAlarmMathNavigated()
            }
        })

        alarmSettingsViewModel.alarm.observe(viewLifecycleOwner, Observer {alarm ->
            if (alarm != null){
                mAlarm = alarmSettingsViewModel.alarm.value!!
                var mRepeat = alarmSettingsViewModel.alarm.value!!.repeatDays

                binding.settingsTime.text = getFormatTime(mAlarm)
                binding.settingsTime.setOnClickListener {
                    val manager = parentFragmentManager
                    val dialog: TimePickerFragment = TimePickerFragment
                        .newInstance(alarm.hour, alarm.minute)
                    dialog.setTargetFragment(
                        this@AlarmSettingsFragment,
                        REQUEST_TIME
                    )
                    dialog.show(manager, DIALOG_TIME)
                }

                binding.setRepeatSun.isChecked = mRepeat[SUN] == 'T'
                binding.setRepeatSun.setOnClickListener {
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
                }

                binding.setRepeatMon.isChecked = mRepeat[MON] == 'T'
                binding.setRepeatMon.setOnClickListener {
                    binding.setRepeatMon.isChecked = mRepeat[MON] != 'T'
                    val sb = StringBuilder(mRepeat)
                    if (mRepeat[MON] == 'T') {
                        sb.setCharAt(MON, 'F')
                        mRepeat = sb.toString()
                        mAlarm.repeatDays = mRepeat
                    } else {
                        sb.setCharAt(MON, 'T')
                        mRepeat = sb.toString()
                        mAlarm.repeatDays = mRepeat
                    }
                }

                binding.setRepeatTue.isChecked = mRepeat[TUE] == 'T'
                binding.setRepeatTue.setOnClickListener {
                    binding.setRepeatTue.isChecked = mRepeat[TUE] != 'T'
                    val sb = StringBuilder(mRepeat)
                    if (mRepeat[TUE] == 'T') {
                        sb.setCharAt(TUE, 'F')
                        mRepeat = sb.toString()
                        mAlarm.repeatDays = mRepeat
                    } else {
                        sb.setCharAt(MON, 'T')
                        mRepeat = sb.toString()
                        mAlarm.repeatDays = mRepeat
                    }
                }

                binding.setRepeatWed.isChecked = mRepeat[WED] == 'T'
                binding.setRepeatWed.setOnClickListener {
                    binding.setRepeatWed.isChecked = mRepeat[WED] != 'T'
                    val sb = StringBuilder(mRepeat)
                    if (mRepeat[WED] == 'T') {
                        sb.setCharAt(WED, 'F')
                        mRepeat = sb.toString()
                        mAlarm.repeatDays = mRepeat
                    } else {
                        sb.setCharAt(WED, 'T')
                        mRepeat = sb.toString()
                        mAlarm.repeatDays = mRepeat
                    }
                }

                binding.setRepeatThu.isChecked = mRepeat[THU] == 'T'
                binding.setRepeatThu.setOnClickListener {
                    binding.setRepeatMon.isChecked = mRepeat[THU] != 'T'
                    val sb = StringBuilder(mRepeat)
                    if (mRepeat[THU] == 'T') {
                        sb.setCharAt(THU, 'F')
                        mRepeat = sb.toString()
                        mAlarm.repeatDays = mRepeat
                    } else {
                        sb.setCharAt(THU, 'T')
                        mRepeat = sb.toString()
                        mAlarm.repeatDays = mRepeat
                    }
                }

                binding.setRepeatFri.isChecked = mRepeat[FRI] == 'T'
                binding.setRepeatFri.setOnClickListener {
                    binding.setRepeatMon.isChecked = mRepeat[FRI] != 'T'
                    val sb = StringBuilder(mRepeat)
                    if (mRepeat[FRI] == 'T') {
                        sb.setCharAt(FRI, 'F')
                        mRepeat = sb.toString()
                        mAlarm.repeatDays = mRepeat
                    } else {
                        sb.setCharAt(FRI, 'T')
                        mRepeat = sb.toString()
                        mAlarm.repeatDays = mRepeat
                    }
                }

                binding.setRepeatSat.isChecked = mRepeat[SAT] == 'T'
                binding.setRepeatSat.setOnClickListener {
                    binding.setRepeatMon.isChecked = mRepeat[SAT] != 'T'
                    val sb = StringBuilder(mRepeat)
                    if (mRepeat[SAT] == 'T') {
                        sb.setCharAt(SAT, 'F')
                        mRepeat = sb.toString()
                        mAlarm.repeatDays = mRepeat
                    } else {
                        sb.setCharAt(SAT, 'T')
                        mRepeat = sb.toString()
                        mAlarm.repeatDays = mRepeat
                    }
                }

                binding.settingsRepeatSwitch.isChecked = mAlarm.repeat
                binding.settingsRepeatSwitch.setOnClickListener {
                    binding.settingsRepeatSwitch.isChecked = !mAlarm.repeat
                    mAlarm.repeat = (!mAlarm.repeat)
                }

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
                    requireActivity(),
                    android.R.layout.simple_spinner_dropdown_item, toneItems
                )
                binding.settingsToneSpinner.adapter = toneAdapter
                binding.settingsToneSpinner.setSelection(previousPosition)

                val difficultyItems =
                    arrayOf("Easy", "Medium", "Hard")
                val difficultyAdapter = ArrayAdapter(
                    requireActivity(),
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
                binding.settingsVibrateSwitch.setOnClickListener {
                    binding.settingsVibrateSwitch.isChecked = !mAlarm.vibrate
                    mAlarm.vibrate = (!mAlarm.vibrate)
                }


            }
        })

        binding.settingsTestButton.setOnClickListener {
            mTestAlarm = Alarm()
            mTestAlarm!!.difficulty = binding.settingsMathDifficultySpinner.selectedItemPosition
            if (mAlarmTones.isNotEmpty()) {
                mTestAlarm!!.alarmTone = (
                        mAlarmTones[binding.settingsToneSpinner.selectedItemPosition].toString()
                        )
            }
            mTestAlarm!!.vibrate = binding.settingsVibrateSwitch.isChecked
            mTestAlarm!!.snooze = 0
            alarmSettingsViewModel.onAdd(mTestAlarm!!)
        }

        return binding.root
    }

    private fun scheduleAndMessage() { //schedule it and create a toast
        if (scheduleAlarm(requireActivity(), mAlarm)) {
            Toast.makeText(
                activity, getTimeLeftMessage(requireContext(), mAlarm),
                Toast.LENGTH_SHORT
            ).show()
            mAlarm.isOn = true
        } else {
            mAlarm.isOn = false
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (resultCode != Activity.RESULT_OK) {
            return
        }
        if (requestCode == REQUEST_TIME) {
            val hour = data?.getIntExtra(TimePickerFragment.EXTRA_HOUR, 0)
            val min = data?.getIntExtra(TimePickerFragment.EXTRA_MIN, 0)
            if (hour != null) {
                alarmSettingsViewModel.alarm.value!!.hour = hour
            }
            if (min != null) {
                alarmSettingsViewModel.alarm.value!!.minute = min
            }
            binding.settingsTime.text = getFormatTime(alarmSettingsViewModel.alarm.value!!)
        } else {
            if (requestCode == REQUEST_TEST) {
                alarmSettingsViewModel.onDelete(alarmSettingsViewModel.latestAlarm.value!!)
            }
        }
    }

    override fun onSaveInstanceState(savedInstanceState: Bundle) {
        super.onSaveInstanceState(savedInstanceState)
        savedInstanceState.putInt("hour", mAlarm.hour)
        savedInstanceState.putInt("minute", mAlarm.minute)
        savedInstanceState.putString("repeat", mAlarm.repeatDays)
        savedInstanceState.putBoolean("repeatweekly", mAlarm.repeat)
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)
        inflater.inflate(R.menu.alarm_settings_menu, menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.fragment_settings_done -> {
                //Setting difficulty + alarm tone
                mAlarm.difficulty =(binding.settingsMathDifficultySpinner.selectedItemPosition)
                if (mAlarmTones.isNotEmpty()) {
                    mAlarm.alarmTone = (
                        mAlarmTones[binding.settingsToneSpinner
                            .selectedItemPosition].toString()
                    )
                }
                //schedule alarm, update to database and close settings
                if (isFromAdd!!) {
                    scheduleAndMessage()
                    alarmSettingsViewModel.onUpdate(mAlarm)
                } else {
                    if (mAlarm.isOn) {
                        cancelAlarm(requireContext(), mAlarm)
                    }
                    scheduleAndMessage()
                    alarmSettingsViewModel.onUpdate(mAlarm)
                }
                findNavController().navigate(
                    AlarmSettingsFragmentDirections.actionAlarmSettingsFragmentToAlarmFragment()
                )
                true
            }
            R.id.fragment_settings_delete -> {
                if (mAlarm.isOn) {
                    cancelAlarm(requireContext(), mAlarm)
                }
                alarmSettingsViewModel.onDelete(alarmSettingsViewModel.alarm.value!!)

                findNavController().navigate(
                    AlarmSettingsFragmentDirections.actionAlarmSettingsFragmentToAlarmFragment()
                )
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }
}