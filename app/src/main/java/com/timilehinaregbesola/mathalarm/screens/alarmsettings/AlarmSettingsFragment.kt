package com.timilehinaregbesola.mathalarm.screens.alarmsettings

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
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.afollestad.materialdialogs.MaterialDialog
import com.afollestad.materialdialogs.datetime.timePicker
import com.timilehinaregbesola.mathalarm.R
import com.timilehinaregbesola.mathalarm.database.Alarm
import com.timilehinaregbesola.mathalarm.databinding.FragmentAlarmSettingsBinding
import com.timilehinaregbesola.mathalarm.screens.alarmmath.AlarmMathActivity
import com.timilehinaregbesola.mathalarm.utils.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.koin.android.ext.android.bind
import org.koin.androidx.viewmodel.ext.android.viewModel
import java.util.*
import kotlin.collections.ArrayList

class AlarmSettingsFragment : Fragment() {

    private lateinit var binding: FragmentAlarmSettingsBinding

    private val settingsViewModel by viewModel<AlarmSettingsViewModel>()

    private lateinit var mAlarm: Alarm

    private val args: AlarmSettingsFragmentArgs by navArgs()
    var key: Long? = null

    private var isFromAdd: Boolean? = null

    var mTestAlarm: Alarm? = null
    var mTestAlarmId: Long? = null

    var mAlarmTones: Array<Uri?> = emptyArray()
    private val REQUEST_TEST = 1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        isFromAdd = args.add
        key = args.alarmKey

        binding = FragmentAlarmSettingsBinding.inflate(inflater, container, false).apply {
            alarmSettingsViewModel = settingsViewModel
        }
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        settingsViewModel.getAlarm(key!!)
        setupObservers()
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
            settingsViewModel.onAdd(mTestAlarm!!)
        }
    }

    private suspend fun getTime(alarm: Alarm): CharSequence? = withContext(Dispatchers.IO) {
        alarm.getFormatTime()
    }

    private fun setupObservers() {
        settingsViewModel.removeSpinner.observe(viewLifecycleOwner) {
            binding.timeProgress.visibility = View.GONE
        }
        settingsViewModel.alarm.observe(viewLifecycleOwner) {
            it?.let { alarm ->
                mAlarm = alarm
                var mRepeat = alarm.repeatDays
                viewLifecycleOwner.lifecycleScope.launch {
                    binding.settingsTime.text = getTime(mAlarm)
                    settingsViewModel.stopLoading()
                }
                binding.settingsTime.setOnClickListener {
                    val timeCal = Calendar.getInstance()
                    if (!isFromAdd!!) {
                        timeCal.set(0, 0, 0, alarm.hour, alarm.minute)
                    }
                    MaterialDialog(requireContext()).show {
                        timePicker(currentTime = timeCal, show24HoursView = false) { _, datetime ->
                            mAlarm.hour = datetime.get(Calendar.HOUR_OF_DAY)
                            mAlarm.minute = datetime.get(Calendar.MINUTE)
                            settingsViewModel.onUpdate(mAlarm)
                            binding.settingsTime.text = mAlarm.getFormatTime()
                        }
                    }
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
                        sb.setCharAt(TUE, 'T')
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
                    binding.setRepeatThu.isChecked = mRepeat[THU] != 'T'
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
                    binding.setRepeatFri.isChecked = mRepeat[FRI] != 'T'
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
                    binding.setRepeatSat.isChecked = mRepeat[SAT] != 'T'
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

                // Getting system sound files for tone and displaying in spinner
                // Getting system sound files for tone and displaying in spinner
                val toneItems: MutableList<String> =
                    ArrayList()
                val ringtoneMgr = RingtoneManager(activity)
                ringtoneMgr.setType(RingtoneManager.TYPE_ALARM)
                var alarmsCursor = ringtoneMgr.cursor
                var alarmsCount = alarmsCursor.count

                if (alarmsCount == 0) { // if there are no alarms, use notification sounds
                    ringtoneMgr.setType(RingtoneManager.TYPE_NOTIFICATION)
                    alarmsCursor = ringtoneMgr.cursor
                    alarmsCount = alarmsCursor.count
                    if (alarmsCount == 0) { // if no alarms and notification sounds, finally use ringtones
                        ringtoneMgr.setType(RingtoneManager.TYPE_RINGTONE)
                        alarmsCursor = ringtoneMgr.cursor
                        alarmsCount = alarmsCursor.count
                    }
                }

                if (alarmsCount == 0 && !alarmsCursor.moveToFirst()) {
                    Toast.makeText(activity, "No sound files available", Toast.LENGTH_SHORT).show()
                }

                var previousPosition = 0

                // If there are sound files, add them
                if (alarmsCount != 0) {
                    mAlarmTones = arrayOfNulls(alarmsCount)
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

                binding.settingsSnoozeText.setText(
                    java.lang.String.format(
                        Locale.US,
                        "%d",
                        mAlarm.snooze
                    )
                )
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
        }

        settingsViewModel.navigateToAlarmMath.observe(
            viewLifecycleOwner,
            { alarmId ->
                alarmId?.let {
                    mTestAlarmId = alarmId
                    val test = Intent(requireContext(), AlarmMathActivity::class.java)
                    test.putExtra(ALARM_EXTRA, alarmId.toString())
                    startActivityForResult(test, REQUEST_TEST)
                    settingsViewModel.onAlarmMathNavigated()
                }
            }
        )
    }

    private fun scheduleAndMessage() { // schedule it and create a toast
        if (mAlarm.scheduleAlarm(requireActivity(), false)) {
            Toast.makeText(
                activity, mAlarm.getTimeLeftMessage(requireContext()),
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
        if (requestCode == REQUEST_TEST) {
            settingsViewModel.onDeleteFromId(mTestAlarmId)
        }
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)
        inflater.inflate(R.menu.alarm_settings_menu, menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.fragment_settings_done -> {
                // Setting difficulty + alarm tone
                mAlarm.difficulty = (binding.settingsMathDifficultySpinner.selectedItemPosition)
                if (mAlarmTones.isNotEmpty()) {
                    mAlarm.alarmTone = (
                        mAlarmTones[binding.settingsToneSpinner.selectedItemPosition].toString()
                        )
                }
                // schedule alarm, update to database and close settings
                if (isFromAdd!!) {
                    scheduleAndMessage()
                    settingsViewModel.onUpdate(mAlarm)
                } else {
                    if (mAlarm.isOn) {
                        mAlarm.cancelAlarm(requireContext())
                    }
                    scheduleAndMessage()
                    settingsViewModel.onUpdate(mAlarm)
                }
                findNavController().popBackStack()
                true
            }
            R.id.fragment_settings_delete -> {
                if (mAlarm.isOn) {
                    mAlarm.cancelAlarm(requireContext())
                }
                settingsViewModel.onDeleteAlarm(mAlarm)

                findNavController().popBackStack()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }
}
