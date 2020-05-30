package com.android.example.mathalarm.screens.alarmlist

import android.os.Bundle
import android.view.*
import android.widget.RelativeLayout
import android.widget.Toast
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProviders
import androidx.navigation.fragment.findNavController
import com.android.example.mathalarm.R
import com.android.example.mathalarm.database.AlarmDatabase
import com.android.example.mathalarm.databinding.FragmentAlarmListBinding

class AlarmFragment: Fragment() {

    private lateinit var binding: FragmentAlarmListBinding

    private lateinit var alarmListViewModel: AlarmListViewModel
    val sourceAdd = "add"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {

        // Get a reference to the binding object and inflate the fragment views.
        binding = DataBindingUtil.inflate(
            inflater, R.layout.fragment_alarm_list, container, false)

        val application = requireNotNull(this.activity).application

        //Creating an instance of the ViewModel Factory
        val dataSource = AlarmDatabase.getInstance(application).alarmDatabaseDao
        val viewModelFactory =
            AlarmListViewModelFactory(
                dataSource,
                application
            )

        alarmListViewModel = ViewModelProvider(
            this, viewModelFactory).get(AlarmListViewModel::class.java)

        binding.alarmListViewModel = alarmListViewModel

        val adapter = AlarmListAdapter(AlarmListener {alarmId ->
            alarmListViewModel.onAlarmClicked(alarmId)
        })
        val itemDecoration = VerticalSpacingItemDecoration(15)
        binding.alarmRecyclerView.addItemDecoration(itemDecoration)
        binding.alarmRecyclerView.adapter = adapter

        binding.lifecycleOwner = this

        alarmListViewModel.alarms.observe(viewLifecycleOwner, Observer {
            if (it.isNotEmpty()){
                binding.alarmEmptyView.visibility = View.GONE
                it.let{
                    adapter.submitList(it)
                }
            } else {
                binding.alarmEmptyView.visibility = View.VISIBLE
            }

        })

        alarmListViewModel.navigateToAlarmSettings.observe(viewLifecycleOwner, Observer { alarm ->
            alarm?.let {
                this.findNavController().navigate(AlarmFragmentDirections
                    .actionAlarmFragmentToAlarmSettingsFragment(alarm))
                alarmListViewModel.onAlarmSettingsNavigated()
            }
        })

        return binding.root
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)
        inflater.inflate(R.menu.alarm_fragment_menu, menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.fragment_add_alarm_menu -> {
                alarmListViewModel.onAdd()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }
}