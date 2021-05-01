package com.timilehinaregbesola.mathalarm.presentation.alarmlist

import android.os.Bundle
import android.view.* // ktlint-disable no-wildcard-imports
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.platform.ComposeView
import androidx.fragment.app.Fragment
import com.timilehinaregbesola.mathalarm.R
import com.timilehinaregbesola.mathalarm.presentation.ui.ComposeAlarmTheme
import org.koin.androidx.viewmodel.ext.android.viewModel

class AlarmFragment : Fragment() {
    private val alarmListViewModel by viewModel<AlarmListViewModel>()
    private var add: Boolean? = false

    @ExperimentalFoundationApi
    @ExperimentalMaterialApi
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return ComposeView(requireContext()).apply {
            // In order for savedState to work, the same ID needs to be used for all instances.
            id = R.id.alarmFragment

            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
            setContent {
                var alarmId: Long? = null
                val alarm = alarmListViewModel.alarm.value
                alarmListViewModel.addClicked.observe(
                    viewLifecycleOwner,
                    {
                        if (it != null) {
                            add = it
                        }
                    }
                )
                ComposeAlarmTheme {
                    alarmListViewModel._alarms.observeAsState().value?.let { alarms ->
                        alarmListViewModel.navigateToAlarmSettings.observeAsState().value.let { id ->
                            if (id != null) {
                                alarmId = id
                                alarmListViewModel.getAlarm(alarmId!!)
                            }
//                                navigate(Screen.AlarmSettings, Screen.AlarmList, id, add!!)

                            if (alarms.isEmpty()) {
                                EmptyScreen(alarmListViewModel)
                            } else {
                                ListDisplayScreen(alarms, alarmListViewModel, alarmId, add!!, alarm)
                            }
                        }
                    }
                }
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        alarmListViewModel.getAlarms()
    }
}
