package com.android.example.mathalarm.screens.alarmlist

import android.os.Bundle
import android.view.*
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.ItemTouchHelper
import com.android.example.mathalarm.R
import com.android.example.mathalarm.database.AlarmDatabase
import com.android.example.mathalarm.databinding.FragmentAlarmListBinding
import org.koin.androidx.viewmodel.ext.android.viewModel


class AlarmFragment: Fragment() {

    private lateinit var binding: FragmentAlarmListBinding
    private val alarmListViewModel by viewModel<AlarmListViewModel>()
    private lateinit var adapter: AlarmListAdapter
    private var add: Boolean? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {

        // Get a reference to the binding object and inflate the fragment views.
        binding = FragmentAlarmListBinding.inflate(
            inflater, container, false)

        binding.alarmListViewModel = alarmListViewModel

        adapter = AlarmListAdapter(alarmListViewModel, AlarmListener {alarmId ->
            alarmListViewModel.onAlarmClicked(alarmId)
        })
        val itemDecoration = VerticalSpacingItemDecoration(15)
        binding.alarmRecyclerView.addItemDecoration(itemDecoration)
        binding.alarmRecyclerView.adapter = adapter
        val itemTouchHelper = ItemTouchHelper(SwipeToDeleteCallback(adapter))
        itemTouchHelper.attachToRecyclerView(binding.alarmRecyclerView)

        binding.lifecycleOwner = this
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        alarmListViewModel.getAlarms()
        setupObservers()
    }

    private fun setupObservers() {
        alarmListViewModel.addClicked.observe(viewLifecycleOwner, {
            if (it != null) {
                add = it
            }
        })

        alarmListViewModel.alarms.observe(viewLifecycleOwner, {
            if (it.isNotEmpty()){
                binding.alarmEmptyView.visibility = View.GONE
                it.let{ adapter.submitList(it) }
            } else {
                binding.alarmEmptyView.visibility = View.VISIBLE
            }
        })

        alarmListViewModel.navigateToAlarmSettings.observe(viewLifecycleOwner, { alarm ->
            alarm?.let {
                this.findNavController().navigate(AlarmFragmentDirections
                    .actionAlarmFragmentToAlarmSettingsFragment(alarm, add!!))
                alarmListViewModel.onAlarmSettingsNavigated()
            }
        })
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