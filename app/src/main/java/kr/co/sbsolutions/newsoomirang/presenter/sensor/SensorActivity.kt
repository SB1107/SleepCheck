package kr.co.sbsolutions.newsoomirang.presenter.sensor

import android.bluetooth.BluetoothDevice
import android.os.Bundle
import android.widget.Toast
import androidx.activity.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kr.co.sbsolutions.newsoomirang.databinding.ActivitySensorBinding
import kr.co.sbsolutions.newsoomirang.presenter.BaseActivity
import kr.co.sbsolutions.newsoomirang.presenter.BaseServiceActivity
import kr.co.sbsolutions.newsoomirang.presenter.BaseViewModel
import kr.co.sbsolutions.newsoomirang.presenter.splash.WHERE

class SensorActivity : BaseActivity() {
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

        bindViews()
    }

    private fun bindViews() {
        with(binding) {
            deviceRecyclerView.apply {
                layoutManager = LinearLayoutManager(this@SensorActivity, LinearLayoutManager.VERTICAL, false)
                adapter = bleAdapter
            }


            btSearch.setOnClickListener {
                lifecycleScope.launch {
                    lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
                        launch {
                            viewModel.isScanning.collectLatest {
                                if (it) {
                                    Toast.makeText(this@SensorActivity, "스캔중", Toast.LENGTH_SHORT).show()
                                }
                            }
                        }
                    }
                }
                viewModel.scanBLEDevices()
            }

        }

        lifecycleScope.launch {
            lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.isRegistered.collectLatest {
                        if (it) {
                            finish()
                        }else {
                            Toast.makeText(this@SensorActivity, "재연결이 필요합니다. ", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            }
        }
    }


}