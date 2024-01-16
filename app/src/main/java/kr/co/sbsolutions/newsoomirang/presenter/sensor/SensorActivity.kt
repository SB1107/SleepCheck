package kr.co.sbsolutions.newsoomirang.presenter.sensor

import android.bluetooth.BluetoothDevice
import android.os.Bundle
import androidx.activity.viewModels
import kr.co.sbsolutions.newsoomirang.databinding.ActivitySensorBinding
import kr.co.sbsolutions.newsoomirang.presenter.BaseServiceActivity
import kr.co.sbsolutions.newsoomirang.presenter.BaseViewModel

class SensorActivity : BaseServiceActivity() {
    private val viewModel: SensorViewModel by viewModels()
    private val binding: ActivitySensorBinding by lazy {
        ActivitySensorBinding.inflate(layoutInflater)
    }
    private val bleAdapter : BluetoothAdapter by lazy {
        BluetoothAdapter(bleClickListener)
    }

    private val bleClickListener: (BluetoothDevice) -> Unit = { bluetoothDevice ->
        viewModel.registerDevice( bluetoothDevice)
    }
    override fun newBackPressed() {
         finish()
    }
    override fun injectViewModel(): BaseViewModel {
        return  viewModel
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)
    }
}