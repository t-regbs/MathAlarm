package com.android.example.mathalarm.screens.alarmlist

import android.content.DialogInterface
import android.os.Bundle
import android.view.*
import androidx.appcompat.app.AlertDialog
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.ItemTouchHelper
import com.android.example.mathalarm.R
import com.android.example.mathalarm.database.AlarmDatabase
import com.android.example.mathalarm.databinding.FragmentAlarmListBinding


class AlarmFragment: Fragment() {

    companion object {
        fun newInstance(): AlarmFragment {
            return AlarmFragment()
        }
    }

    private lateinit var binding: FragmentAlarmListBinding

    private lateinit var alarmListViewModel: AlarmListViewModel

    private var add: Boolean? = null

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
        val viewModelFactory = AlarmListViewModelFactory(dataSource)

        alarmListViewModel = ViewModelProvider(
            this, viewModelFactory).get(AlarmListViewModel::class.java)

        binding.alarmListViewModel = alarmListViewModel

        val adapter = AlarmListAdapter(alarmListViewModel, AlarmListener {alarmId ->
            alarmListViewModel.onAlarmClicked(alarmId)
        })
        val itemDecoration = VerticalSpacingItemDecoration(15)
        binding.alarmRecyclerView.addItemDecoration(itemDecoration)
        binding.alarmRecyclerView.adapter = adapter
        val itemTouchHelper = ItemTouchHelper(SwipeToDeleteCallback(adapter))
        itemTouchHelper.attachToRecyclerView(binding.alarmRecyclerView)

        binding.lifecycleOwner = this


        alarmListViewModel.isFromAdd.observe(viewLifecycleOwner, Observer {
            if (it != null) {
                add = it
            }
        })

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
                    .actionAlarmFragmentToAlarmSettingsFragment(alarm, add!!))
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
            R.id.fragment_clear_alarm_menu -> {
                val builder = AlertDialog.Builder(requireContext())
                builder.setMessage("Are you sure you want to delete all the alarms?")
                builder.setCancelable(true)

                builder.setPositiveButton("Yes") { dialog, _ ->
                    alarmListViewModel.onClear()
                    dialog.cancel()
                }

                builder.setNegativeButton("No") { dialog, _ -> dialog.cancel() }

                builder.create().apply {
                    show()
                }
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }
}