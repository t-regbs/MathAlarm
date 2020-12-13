package com.timilehinaregbesola.mathalarm.screens.alarmlist

import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.timilehinaregbesola.mathalarm.R
import com.timilehinaregbesola.mathalarm.database.Alarm
import com.timilehinaregbesola.mathalarm.databinding.AlarmItemBinding
import com.timilehinaregbesola.mathalarm.utils.* // ktlint-disable no-wildcard-imports

class AlarmListAdapter(
    val viewModel: AlarmListViewModel,
    val clickListener: AlarmListener
) : ListAdapter<Alarm, AlarmListAdapter.ViewHolder>(AlarmDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder.from(parent)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position)!!, clickListener, viewModel)
    }

    fun deleteTask(position: Int) {
        viewModel.onDelete(getItem(position))
    }

    class ViewHolder private constructor(val binding: AlarmItemBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(
            item: Alarm,
            clickListener: AlarmListener,
            viewModel: AlarmListViewModel
        ) {
            binding.alarm = item
            binding.clickListener = clickListener
            val repeat: String = item.repeatDays
            val hour: Int = item.hour
            if (hour < 12) {
                binding.root.setBackgroundColor(
                    ContextCompat.getColor(itemView.context, R.color.colorSkyBlue)
                )
            } else {
                binding.root.setBackgroundColor(
                    ContextCompat
                        .getColor(itemView.context, R.color.colorNightBlue)
                )
            }

            binding.alarmSwitchButton.setOnClickListener {
                item.isOn = !item.isOn
                binding.alarmSwitchButton.isChecked = item.isOn
                if (item.isOn) {
                    if (item.scheduleAlarm(itemView.context, false)) {
                        Toast.makeText(
                            itemView.context, item.getTimeLeftMessage(itemView.context),
                            Toast.LENGTH_SHORT
                        ).show()
                    } else {
                        item.isOn = false
                        binding.alarmSwitchButton.isChecked = false
                    }
                } else {
                    item.cancelAlarm(itemView.context)
                }
                viewModel.onUpdate(item)
            }

            binding.alarmSwitchButton.isChecked = item.isOn

            binding.alarmTime.text = item.getFormatTime()
            val color: Int = if (item.repeat) {
                R.color.colorGold
            } else {
                R.color.colorWhite
            }
            binding.alarmTime.setTextColor(
                ContextCompat.getColor(itemView.context, color)
            )

            if (repeat[SUN] == 'T') {
                binding.alarmRepeatSun.setTextColor(
                    ContextCompat.getColor(itemView.context, R.color.colorGold)
                )
            } else binding.alarmRepeatSun.setTextColor(
                ContextCompat.getColor(itemView.context, R.color.colorWhite)
            )

            if (repeat[MON] == 'T') {
                binding.alarmRepeatMon.setTextColor(
                    ContextCompat.getColor(itemView.context, R.color.colorGold)
                )
            } else binding.alarmRepeatMon.setTextColor(
                ContextCompat.getColor(itemView.context, R.color.colorWhite)
            )

            if (repeat[TUE] == 'T') {
                binding.alarmRepeatTue.setTextColor(
                    ContextCompat.getColor(itemView.context, R.color.colorGold)
                )
            } else binding.alarmRepeatTue.setTextColor(
                ContextCompat.getColor(itemView.context, R.color.colorWhite)
            )

            if (repeat[WED] == 'T') {
                binding.alarmRepeatWed.setTextColor(
                    ContextCompat.getColor(itemView.context, R.color.colorGold)
                )
            } else binding.alarmRepeatWed.setTextColor(
                ContextCompat.getColor(itemView.context, R.color.colorWhite)
            )

            if (repeat[THU] == 'T') {
                binding.alarmRepeatThu.setTextColor(
                    ContextCompat.getColor(itemView.context, R.color.colorGold)
                )
            } else binding.alarmRepeatThu.setTextColor(
                ContextCompat.getColor(itemView.context, R.color.colorWhite)
            )

            if (repeat[FRI] == 'T') {
                binding.alarmRepeatFri.setTextColor(
                    ContextCompat.getColor(itemView.context, R.color.colorGold)
                )
            } else binding.alarmRepeatFri.setTextColor(
                ContextCompat.getColor(itemView.context, R.color.colorWhite)
            )

            if (repeat[SAT] == 'T') {
                binding.alarmRepeatSat.setTextColor(
                    ContextCompat.getColor(itemView.context, R.color.colorGold)
                )
            } else {
                binding.alarmRepeatSat.setTextColor(
                    ContextCompat.getColor(itemView.context, R.color.colorWhite)
                )
            }
        }

        companion object {
            fun from(parent: ViewGroup): ViewHolder {
                val layoutInflater = LayoutInflater.from(parent.context)
                val binding = AlarmItemBinding.inflate(layoutInflater, parent, false)
                return ViewHolder(binding)
            }
        }
    }
}

class AlarmDiffCallback : DiffUtil.ItemCallback<Alarm>() {
    override fun areItemsTheSame(oldItem: Alarm, newItem: Alarm): Boolean {
        return oldItem.alarmId == newItem.alarmId
    }

    override fun areContentsTheSame(oldItem: Alarm, newItem: Alarm): Boolean {
        return oldItem == newItem
    }
}

class AlarmListener(val clickListener: (alarmId: Long) -> Unit) {
    fun onclick(alarm: Alarm) = clickListener(alarm.alarmId)
}
