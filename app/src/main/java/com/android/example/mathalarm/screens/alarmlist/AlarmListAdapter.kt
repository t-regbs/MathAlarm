package com.android.example.mathalarm.screens.alarmlist

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.android.example.mathalarm.*
import com.android.example.mathalarm.database.Alarm
import com.android.example.mathalarm.databinding.AlarmItemBinding

class AlarmListAdapter(val clickListener: AlarmListener):
    ListAdapter<Alarm, AlarmListAdapter.ViewHolder>(AlarmDiffCallback()) {


    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder.from(parent)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {

        holder.bind(getItem(position)!!, clickListener)
    }

    class ViewHolder private constructor(val binding: AlarmItemBinding): RecyclerView.ViewHolder(binding.root) {


        fun bind(
            item: Alarm,
            clickListener: AlarmListener
        ) {
            binding.alarm = item
            binding.clickListener = clickListener
            var repeat: String = item.repeatDays
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

            binding.alarmSwitchButton.isChecked = item.isOn

            binding.alarmTime.setText(
                getFormatTime(
                    item
                )
            )
            val color: Int
            color = if (item.repeat) {
                R.color.colorGold
            } else {
                R.color.colorWhite
            }
            binding.alarmTime.setTextColor(
                ContextCompat.getColor(
                    itemView.context,
                    color
                )
            )

            if (repeat.get(SUN) == 'T') {
                binding.alarmRepeatSun.setTextColor(
                    ContextCompat.getColor(
                        itemView.context,
                        R.color.colorGold
                    )
                )
            } else binding.alarmRepeatSun.setTextColor(
                ContextCompat.getColor(
                    itemView.context,
                    R.color.colorWhite
                )
            )

            if (repeat.get(MON) == 'T') {
                binding.alarmRepeatMon.setTextColor(
                    ContextCompat.getColor(
                        itemView.context,
                        R.color.colorGold
                    )
                )
            } else binding.alarmRepeatMon.setTextColor(
                ContextCompat.getColor(
                    itemView.context,
                    R.color.colorWhite
                )
            )

            if (repeat.get(TUE) == 'T') {
                binding.alarmRepeatTue.setTextColor(
                    ContextCompat.getColor(
                        itemView.context,
                        R.color.colorGold
                    )
                )
            } else binding.alarmRepeatTue.setTextColor(
                ContextCompat.getColor(
                    itemView.context,
                    R.color.colorWhite
                )
            )

            if (repeat.get(WED) == 'T') {
                binding.alarmRepeatWed.setTextColor(
                    ContextCompat.getColor(
                        itemView.context,
                        R.color.colorGold
                    )
                )
            } else binding.alarmRepeatWed.setTextColor(
                ContextCompat.getColor(
                    itemView.context,
                    R.color.colorWhite
                )
            )

            if (repeat.get(THU) == 'T') {
                binding.alarmRepeatThu.setTextColor(
                    ContextCompat.getColor(
                        itemView.context,
                        R.color.colorGold
                    )
                )
            } else binding.alarmRepeatThu.setTextColor(
                ContextCompat.getColor(
                    itemView.context,
                    R.color.colorWhite
                )
            )

            if (repeat.get(FRI) == 'T') {
                binding.alarmRepeatFri.setTextColor(
                    ContextCompat.getColor(
                        itemView.context,
                        R.color.colorGold
                    )
                )
            } else binding.alarmRepeatFri.setTextColor(
                ContextCompat.getColor(
                    itemView.context,
                    R.color.colorWhite
                )
            )

            if (repeat.get(SAT) == 'T') {
                binding.alarmRepeatSat.setTextColor(
                    ContextCompat.getColor(
                        itemView.context,
                        R.color.colorGold
                    )
                )
            } else {
                binding.alarmRepeatSat.setTextColor(
                    ContextCompat.getColor(
                        itemView.context,
                        R.color.colorWhite
                    )
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

class AlarmDiffCallback: DiffUtil.ItemCallback<Alarm>(){
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