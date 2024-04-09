package kr.co.sbsolutions.newsoomirang.presenter.sensor

import android.annotation.SuppressLint
import android.bluetooth.BluetoothDevice
import android.os.Build
import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import kr.co.sbsolutions.newsoomirang.BuildConfig
import kr.co.sbsolutions.newsoomirang.common.Cons.TAG
import kr.co.sbsolutions.newsoomirang.common.getChangeDeviceName
import kr.co.sbsolutions.newsoomirang.databinding.AdapterBluetoothItemBinding

@SuppressLint("MissingPermission")
class SensorBluetoothAdapter(val bleClickListener : (BluetoothDevice) -> Unit) : ListAdapter<BluetoothDevice, SensorBluetoothAdapter.ViewHolder>(diffUtil) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(AdapterBluetoothItemBinding.inflate(LayoutInflater.from(parent.context), parent, false))
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(currentList[position])
    }

    inner class ViewHolder(private val binding: AdapterBluetoothItemBinding) : RecyclerView.ViewHolder(binding.root) {
        @SuppressLint("SetTextI18n")
        fun bind(item : BluetoothDevice) {
            binding.cvRoot.setOnClickListener {
                bleClickListener.invoke(item)
            }
            when(BuildConfig.DEBUG) {
                true -> binding.tvBleName.text = item.name
                else -> binding.tvBleName.text = item.name.getChangeDeviceName()
            }
        }
    }

    companion object {
        val diffUtil = object : DiffUtil.ItemCallback<BluetoothDevice>() {
            override fun areItemsTheSame(oldItem: BluetoothDevice, newItem: BluetoothDevice) = oldItem.address == newItem.address

            override fun areContentsTheSame(oldItem: BluetoothDevice, newItem: BluetoothDevice) = oldItem.address == newItem.address
        }
    }
}